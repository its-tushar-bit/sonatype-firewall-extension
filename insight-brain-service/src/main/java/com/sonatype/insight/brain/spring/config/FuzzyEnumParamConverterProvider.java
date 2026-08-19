/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Case-insensitive enum parameter converter for JAX-RS, replicating the behavior of
 * Dropwizard's FuzzyEnumParamConverterProvider. Converts query/path parameters to enum
 * values using case-insensitive matching with dash/period/whitespace-to-underscore normalization.
 * If the enum has a static {@code fromString(String)} or {@code fromValue(String)} method,
 * it is tried first.
 */
public class FuzzyEnumParamConverterProvider
    implements ParamConverterProvider
{
  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
    if (!rawType.isEnum()) {
      return null;
    }
    Class<? extends Enum> enumType = (Class<? extends Enum>) rawType;
    Enum<?>[] constants = enumType.getEnumConstants();
    Method fromStringMethod = findFromStringMethod(rawType);
    return (ParamConverter<T>) new FuzzyEnumParamConverter<>(enumType, constants, fromStringMethod);
  }

  private static Method findFromStringMethod(Class<?> enumType) {
    for (String methodName : new String[]{"fromString", "fromValue"}) {
      try {
        return enumType.getMethod(methodName, String.class);
      }
      catch (NoSuchMethodException ignored) {
      }
    }
    return null;
  }

  private static class FuzzyEnumParamConverter<E extends Enum<E>>
      implements ParamConverter<E>
  {
    private final Class<E> enumType;

    private final E[] constants;

    private final Method fromStringMethod;

    @SuppressWarnings("unchecked")
    FuzzyEnumParamConverter(Class<E> enumType, Enum<?>[] constants, Method fromStringMethod) {
      this.enumType = enumType;
      this.constants = (E[]) constants;
      this.fromStringMethod = fromStringMethod;
    }

    @Override
    @SuppressWarnings("unchecked")
    public E fromString(String value) {
      if (value == null || value.isEmpty()) {
        return null;
      }
      if (fromStringMethod != null) {
        try {
          E result = (E) fromStringMethod.invoke(null, value);
          if (result != null) {
            return result;
          }
        }
        catch (InvocationTargetException e) {
          if (e.getCause() instanceof WebApplicationException wae) {
            throw wae;
          }
        }
        catch (ReflectiveOperationException e) {
          throw new BadRequestException("Failed to convert '" + value + "' to " + enumType.getSimpleName(), e);
        }
      }
      return fromStringFuzzy(value);
    }

    private E fromStringFuzzy(String value) {
      String normalized = value
          .replace(" ", "")
          .replace("\n", "")
          .replace("\r", "")
          .replace("\t", "")
          .replace('-', '_')
          .replace('.', '_');
      for (E constant : constants) {
        if (constant.name().equalsIgnoreCase(normalized)) {
          return constant;
        }
      }
      for (E constant : constants) {
        if (constant.toString().equalsIgnoreCase(value)) {
          return constant;
        }
      }
      throw new BadRequestException("Invalid value '" + value + "' for " + enumType.getSimpleName());
    }

    @Override
    public String toString(E value) {
      return value == null ? null : value.name();
    }
  }
}
