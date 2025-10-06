package com.fampay.scheduler.commons.helper.utils;


import com.fampay.scheduler.commons.exception.InternalLibraryException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommonSerializationUtil {
    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();


    static {
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
        objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    /**
     * Method to convert object to JSON string.
     *
     * @param value - value to convert
     * @return JSON string for the object
     * @throws RuntimeException error on some issue
     */
    public static String writeString(Object value) throws InternalLibraryException {
        try {
            return (value instanceof String) ? (String) value : objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.error("Failed read write string: ", e);
            throw InternalLibraryException.childBuilder().message(e.getMessage()).build();
        }
    }

    /**
     * Convert map to object
     */
    public static <T> T convertMapToObject(Map<String, Object> map, Class<T> cls) {
        return objectMapper.convertValue(map, cls);
    }

    /**
     * Helper to get a Map<String, Object> from String
     */
    public static Map<String, Object> readMapFromString(String str) throws InternalLibraryException {
        return readMapFromString(str, true);
    }

    /**
     * Helper to convert Object to Map
     */
    public static Map<String, Object> convertObjectToMap(Object object) {
        if (object instanceof Map) {
            return (Map<String, Object>) object;
        } else {
            return objectMapper.convertValue(object, TypeFactory.defaultInstance().constructMapLikeType(HashMap.class, String.class, Object.class));
        }
    }

    /**
     * Helper to get a Map<String, Object> from String - with option to suppress error
     */
    public static Map<String, Object> readMapFromString(String str, boolean suppressError) throws InternalLibraryException {
        try {
            return objectMapper.readValue(str, TypeFactory.defaultInstance().constructMapLikeType(HashMap.class, String.class, Object.class));
        } catch (IOException e) {
            log.error("Failed read map from string: ", e);
            if (suppressError) {
                return new HashMap<>();
            }
            throw InternalLibraryException.childBuilder().message(e.getMessage()).build();
        }
    }

    /**
     * Read a object from string
     */
    public static <T> T readObject(String str, Class<T> cls) throws InternalLibraryException {
        T res = null;
        if(!Strings.isNullOrEmpty(str)) {
            try {
                res = (cls == String.class) ? (T) str : objectMapper.readValue(str, cls);
            } catch (IOException e) {
                log.error("Failed read Object to class: ", e);
                throw InternalLibraryException.childBuilder().message(e.getMessage()).build();
            }
        }
        return res;
    }

    /**
     * Read parameterised object from string
     */
    public static <T> T readObject(String str, TypeReference<T> cls) throws InternalLibraryException {
        T res = null;
        if(!Strings.isNullOrEmpty(str)) {
            try {
                res = objectMapper.readValue(str, cls);
            } catch (IOException e) {
                log.error("Failed read Object to class: ", e);
                throw InternalLibraryException.childBuilder().message(e.getMessage()).build();
            }
        }
        return res;
    }


    /**
     * Read a object from string for java types
     */
    public static <T> T readObject(String str, JavaType cls) throws InternalLibraryException {
        T res = null;
        if(!Strings.isNullOrEmpty(str)) {
            try {
                res = objectMapper.readValue(str, cls);
            } catch (IOException e) {
                log.error("Failed read Object to JavaType: ", e);
                throw InternalLibraryException.childBuilder().message(e.getMessage()).build();
            }
        }
        return res;
    }

    /**
     * Read list from string
     */
    public static <T> List<T> readListFromString(String str, Class<T> cls) throws InternalLibraryException {
        try {
            return objectMapper.readValue(str, TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, cls));
        } catch (IOException e) {
            log.error("Failed to read list from string: ", e);
            throw InternalLibraryException.childBuilder().message(e.getMessage()).build();
        }
    }

    /**
     * Convert list from TypedList
     */
    public static <T> List<T> convertListToTypedList(List input, Class<T> cls) {
        return objectMapper.convertValue(input, TypeFactory.defaultInstance().constructCollectionLikeType(List.class, cls));
    }

}
