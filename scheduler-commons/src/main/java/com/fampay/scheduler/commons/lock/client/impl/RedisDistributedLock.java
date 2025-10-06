package com.fampay.scheduler.commons.lock.client.impl;

import com.fampay.scheduler.commons.exception.GenericException;
import com.fampay.scheduler.commons.exception.InternalLibraryException;
import com.fampay.scheduler.commons.lock.client.CustomDistributedLock;
import com.fampay.scheduler.commons.lock.exception.ParallelLockException;
import com.fampay.scheduler.commons.redis.client.CustomRedisClient;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static com.fampay.scheduler.commons.exception.LibraryErrorMessages.ACQUIRE_LOCK_FAILED;
import static com.fampay.scheduler.commons.exception.LibraryErrorMessages.SYSTEM_ERROR;


@Slf4j
@Component
public class RedisDistributedLock implements CustomDistributedLock {

    private final CustomRedisClient redisClient;

    @Autowired
    public RedisDistributedLock(CustomRedisClient redisClient) {
        this.redisClient = redisClient;
    }

    @Override
    public Lock acquireLock(@NonNull String lockUniqueId) throws InternalLibraryException {
        Optional<Lock> lock = acquireLock(lockUniqueId, false);
        return lock.orElseThrow(() -> ParallelLockException.childBuilder().message(ACQUIRE_LOCK_FAILED).build());
    }

    @Override
    public Optional<Lock> acquireLock(@NonNull String lockUniqueId,@NonNull Boolean suppressError) throws InternalLibraryException {
        return acquireLockWithWait(lockUniqueId, -1L, 0L, suppressError);
    }

    @Override
    public Lock acquireLockWithWait(@NonNull String lockUniqueId,@NonNull Long waitTimeInMillis) throws InternalLibraryException {
        Optional<Lock> lock = acquireLockWithWait(lockUniqueId, waitTimeInMillis, false);
        return lock.orElseThrow(() -> ParallelLockException.childBuilder().message(ACQUIRE_LOCK_FAILED).build());
    }

    @Override
    public Optional<Lock> acquireLockWithWait(@NonNull String lockUniqueId,@NonNull Long waitTimeInMillis,@NonNull Boolean suppressError) throws InternalLibraryException {
        return acquireLockWithWait(lockUniqueId, -1L, waitTimeInMillis, suppressError);
    }

    @Override
    public Lock acquireLock(@NonNull String lockUniqueId,@NonNull Long lockExpiryInMillis) throws InternalLibraryException {
        Optional<Lock> lock = acquireLock(lockUniqueId, lockExpiryInMillis, false);
        return lock.orElseThrow(() -> ParallelLockException.childBuilder().message(ACQUIRE_LOCK_FAILED).build());
    }

    @Override
    public Optional<Lock> acquireLock(@NonNull String lockUniqueId,@NonNull Long lockExpiryInMillis,@NonNull Boolean suppressError) throws InternalLibraryException {
        return acquireLockWithWait(lockUniqueId, lockExpiryInMillis, 0L, suppressError);
    }

    @Override
    public Optional<Lock> acquireLockWithWait(@NonNull String lockUniqueId, @NonNull Long lockExpiryInMillis, @NonNull Long waitTimeInMillis, @NonNull Boolean suppressError) throws InternalLibraryException {
        log.debug("RedisDistributedLock.achieveLock() - lockId={}, expiry={}, wait={} ", lockUniqueId, lockExpiryInMillis, waitTimeInMillis);
        try {
            RLock lock = redisClient.getRedissonClient().getLock(lockUniqueId);
            if (lock.tryLock(waitTimeInMillis, lockExpiryInMillis, TimeUnit.MILLISECONDS)) {
                log.debug("Achieved lock for lockUniqueId={}", lockUniqueId);
                return Optional.of(lock);
            } else if (BooleanUtils.isNotTrue(suppressError)) {
                throw ParallelLockException.childBuilder().message(ACQUIRE_LOCK_FAILED).build();
            } else {
                return Optional.empty();
            }
        } catch (GenericException e) {
            log.error("Error in acquiring lock: ", e);
            throw e;
        } catch (Exception ex) {
            log.error("Exception while acquiring lock", ex);
            throw InternalLibraryException.childBuilder().message(SYSTEM_ERROR).build();
        }
    }

    @Override
    public void releaseLock(@NonNull Lock lock,@NonNull String lockUniqueId) {
        try {
            if (((RLock) lock).isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Released lock for lockUniqueId={}", lockUniqueId);
            }
        } catch (Exception ex) {
            log.error("Exception occurred while releasing lock on lockUniqueId={}", lockUniqueId, ex);
        }
    }
}
