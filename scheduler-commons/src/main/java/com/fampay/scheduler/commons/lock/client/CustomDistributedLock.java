package com.fampay.scheduler.commons.lock.client;

import com.fampay.scheduler.commons.exception.InternalLibraryException;
import lombok.NonNull;

import java.util.Optional;
import java.util.concurrent.locks.Lock;

public interface CustomDistributedLock {

    /**
     * This method tries to get lock. If achieved, it returns Lock object or else returns null.
     * If acquired the lock will keep renewing lease every (watchdogTimeout / 3)ms if thread is active.
     * If the thread is not active it will auto released after watchdogTimeout.
     * @param lockUniqueId String
     * @return Lock
     */
    Lock acquireLock(@NonNull String lockUniqueId) throws InternalLibraryException;

    /**
     * This method tries to get lock. If achieved, it returns Lock object or else returns null.
     * If acquired the lock will keep renewing lease every (watchdogTimeout / 3)ms if thread is active.
     * If the thread is not active it will auto released after watchdogTimeout.
     * If the suppressError is true then it will not throw ParallelLockException
     * @param lockUniqueId String
     * @param suppressError Boolean
     * @return Lock
     */
    Optional<Lock> acquireLock(@NonNull String lockUniqueId,@NonNull Boolean suppressError) throws InternalLibraryException;

    /**
     * This method tries to get lock. If achieved within the wait time, it returns Lock object or else returns null.
     * If acquired the lock will keep renewing lease every (watchdogTimeout / 3)ms if thread is active.
     * If the thread is not active it will auto released after watchdogTimeout.
     * @param lockUniqueId String
     * @param waitTimeInMillis Long
     * @return Lock
     */
    Lock acquireLockWithWait(@NonNull String lockUniqueId,@NonNull Long waitTimeInMillis) throws InternalLibraryException;

    /**
     * This method tries to get lock. If achieved within the wait time, it returns Lock object or else returns null.
     * If acquired the lock will keep renewing lease every (watchdogTimeout / 3)ms if thread is active.
     * If the thread is not active it will auto released after watchdogTimeout.
     * If the suppressError is true then it will not throw ParallelLockException
     * @param lockUniqueId String
     * @param waitTimeInMillis Long
     * @param suppressError Boolean
     * @return Lock
     */
    Optional<Lock> acquireLockWithWait(@NonNull String lockUniqueId,@NonNull Long waitTimeInMillis, Boolean suppressError) throws InternalLibraryException;


    /**
     * This method tries to get lock. If achieved, it returns Lock object or else returns null.
     * If acquired the lock will be auto-released after expiry.
     * @param lockUniqueId String
     * @param lockExpiryInMillis Long
     * @return Lock
     */
    Lock acquireLock(@NonNull String lockUniqueId,@NonNull Long lockExpiryInMillis) throws InternalLibraryException;

    /**
     * This method tries to get lock. If achieved, it returns Lock object or else returns null.
     * If acquired the lock will be auto-released after expiry.
     * If the suppressError is true then it will not throw ParallelLockException
     * @param lockUniqueId String
     * @param lockExpiryInMillis Long
     * @param suppressError Boolean
     * @return Lock
     */
    Optional<Lock> acquireLock(@NonNull String lockUniqueId,@NonNull Long lockExpiryInMillis,@NonNull Boolean suppressError) throws InternalLibraryException;

//    /**
//     * This method tries to get lock. If achieved within the wait time, it returns Lock object or else returns null.
//     * If acquired the lock will be auto-released after expiry.
//     * If the suppressError is true then it will not throw ParallelLockException
//     * @param lockUniqueId String
//     * @param lockExpiryInMillis Long
//     * @param waitTimeInMillis Long
//     * @return Lock
//     */
//    Lock acquireLockWithWait(String lockUniqueId, Long lockExpiryInMillis, Long waitTimeInMillis) throws InternalLibraryException;

    /**
     * This method tries to get lock. If achieved within the wait time, it returns Lock object or else returns null.
     * If acquired the lock will be auto-released after expiry.
     * If the suppressError is true then it will not throw ParallelLockException
     * @param lockUniqueId String
     * @param lockExpiryInMillis Long
     * @param waitTimeInMillis Long
     * @param suppressError Boolean
     * @return Lock
     */
    Optional<Lock> acquireLockWithWait(@NonNull String lockUniqueId,@NonNull Long lockExpiryInMillis,@NonNull Long waitTimeInMillis,@NonNull Boolean suppressError) throws InternalLibraryException;

    /**
     * Explicitly release lock before expiry
     * @param lock Lock
     * @param lockUniqueId String
     */
    void releaseLock(@NonNull Lock lock,@NonNull String lockUniqueId);
}