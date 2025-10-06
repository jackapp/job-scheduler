package com.fampay.scheduler.commons.lock.annotation;

import com.fampay.scheduler.commons.exception.InternalLibraryException;
import com.fampay.scheduler.commons.helper.utils.CommonSerializationUtil;
import com.fampay.scheduler.commons.lock.client.CustomDistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

import static com.fampay.scheduler.commons.exception.LibraryErrorMessages.*;

@Aspect
@Component
@Slf4j
public class ExecuteInLockMethodAspect {

    private static final String ERROR_FOR_LOCKIDKEY = "Error in fetching value for lockIdKey: {}";
    public static final String ERROR_FOR_LOCK_ID_KEYS = "Object {} is not of valid data types {}";
    private static final List<Class<?>> ALLOWED_DATA_TYPES_FOR_LOCK_ID_KEYS = List.of(String.class, Enum.class, Long.class, Integer.class);
    private final CustomDistributedLock distributedLock;

    @Autowired
    public ExecuteInLockMethodAspect(CustomDistributedLock distributedLock) {
        this.distributedLock = distributedLock;
    }

    @Pointcut("@annotation(executeInLock)")
    public void executeInLockMethod(ExecuteInLock executeInLock) {}

    @Around("executeInLockMethod(executeInLock)")
    public Object around(ProceedingJoinPoint pjp, ExecuteInLock executeInLock) throws Throwable {
        log.debug("Locking request received: {}", executeInLock);

        if(StringUtils.isEmpty(executeInLock.prefix())) {
            throw InternalLibraryException.childBuilder().message(PREFIX_MISSING)
                    .displayMessage("Please provide application prefix in the annote").build();
        }

        String lockId = "DL-" + executeInLock.prefix();

        if (ArrayUtils.isNotEmpty(executeInLock.lockIdKeys())) {
            lockId += calculateLockId(pjp, executeInLock.lockIdKeys());
        } else {
            lockId += calculateLockId(pjp, executeInLock.lockIdKey());
        }

        Optional<Lock> lock;
        if(executeInLock.waitingTime() == 0) {
            lock = distributedLock.acquireLock(lockId, executeInLock.suppressError());
        } else {
            lock = distributedLock.acquireLockWithWait(lockId, executeInLock.waitingTime(), executeInLock.suppressError());
        }
        Object obj = null;
        try {
            if (lock.isPresent()) {
                log.debug("Acquired lock for: {}", lockId);
                obj = pjp.proceed();
            } else {
                log.debug("Unable to acquire lock for: {}", lockId);
            }
        } finally {
            if(lock.isPresent()) {
                distributedLock.releaseLock(lock.get(), lockId);
            }
        }
        return obj;
    }

    private String calculateLockId(ProceedingJoinPoint pjp, String lockIdKey) {
        String lockId;
        List<Object> args = Arrays.asList(pjp.getArgs());
        if (StringUtils.isEmpty(lockIdKey)) {
            lockId = pjp.getSignature().toString() + CommonSerializationUtil.writeString(args);
        } else {
            try {
                List<String> path = Arrays.asList(lockIdKey.split("\\."));
                String className = path.get(0);
                Object obj;
                if (className.equals("arg")) {
                    int idx = Integer.parseInt(path.get(1));
                    obj = args.get(idx);
                } else {
                    obj = args.stream().filter(arg -> StringUtils.substringAfterLast(arg.getClass().getName(), ".").toLowerCase()
                            .equals(className.toLowerCase())).findFirst().orElseThrow();
                    for (int i = 1; i < path.size(); i++) {
                        obj = PropertyUtils.getProperty(obj, path.get(i));
                    }
                }
                lockId = CommonSerializationUtil.writeString(obj);
            } catch (Exception ex) {
                log.error(ERROR_FOR_LOCKIDKEY, lockIdKey, ex);
                throw InternalLibraryException.childBuilder().message(KEY_PARSING_ERROR).build();
            }
        }
        log.debug("LockId to be used: {}", lockId);
        return lockId;
    }

    private String calculateLockId(ProceedingJoinPoint pjp, String[] lockIdKeys) {
        final String[] parameterNames = ((MethodSignature) pjp.getSignature()).getParameterNames();
        Object[] arguments = pjp.getArgs();
        List<String> data = calculateLockId(lockIdKeys, parameterNames, arguments);
        return "_" + String.join("_", data);
    }

    private List<String> calculateLockId(String[] lockIdKeys, String[] parameterNames, Object[] arguments) {
        List<String> data = new ArrayList<>();
        for (String lockIdKey : lockIdKeys) {
            String[] params = lockIdKey.split("\\.");
            Integer position = getPositionForKey(params, parameterNames, lockIdKey);
            Object object = arguments[position];
            for (int j = 1; j < params.length; ++j) {
                if (Objects.nonNull(object)) {
                    try {
                        Map<String, Object> map = CommonSerializationUtil.convertObjectToMap(object);
                        object = map.get(params[j]);
                    } catch (Exception ex) {
                        throwError(lockIdKey, ex);
                    }
                } else {
                    throwError(lockIdKey);
                }
            }
            if (Objects.nonNull(object)) {
                if (!isAllowedDataTypeForLockKey(object)) {
                    throwError(object);
                }
                data.add(String.valueOf(object));
            } else {
                throwError(lockIdKey);
            }
        }
        return data;
    }

    private  boolean isAllowedDataTypeForLockKey(Object obj) {
        return ALLOWED_DATA_TYPES_FOR_LOCK_ID_KEYS.stream().anyMatch(clazz -> clazz.isInstance(obj));
    }

    private Integer getPositionForKey(String[] params, String[] parameterNames, String lockIdKey) {
        Integer position = null;
        for (int j = 0; j < parameterNames.length; ++j) {
            if (StringUtils.equals(parameterNames[j], params[0])) {
                position = j;
                break;
            }
        }
        if (position == null) {
            throwError(lockIdKey);
        }
        return position;
    }

    private void throwError(String lockIdKey, Exception ex) {
        log.error(ERROR_FOR_LOCKIDKEY, lockIdKey, ex);
        throw InternalLibraryException.childBuilder().message(KEY_PARSING_ERROR).build();
    }

    private void throwError(String lockIdKey) {
        log.error(ERROR_FOR_LOCKIDKEY, lockIdKey);
        throw InternalLibraryException.childBuilder().message(KEY_PARSING_ERROR).build();
    }

    private void throwError(Object object) {
        log.error(ERROR_FOR_LOCK_ID_KEYS, object, ALLOWED_DATA_TYPES_FOR_LOCK_ID_KEYS);
        throw InternalLibraryException.childBuilder().message(INVALID_LOCK_ID_KEYS).build();
    }
}
