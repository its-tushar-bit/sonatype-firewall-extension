/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.jooq;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.persistence.Column;
import org.jooq.Configuration;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.RecordType;
import org.jooq.RecordUnmapper;
import org.jooq.RecordUnmapperProvider;
import org.jooq.impl.DefaultRecordUnmapper;

/**
 * A {@link RecordUnmapperProvider} that fixes jOOQ's asymmetric enum handling.
 * <p>
 * jOOQ's {@code record.into()} converts String to Enum using {@code Enum.valueOf()} (which expects {@code name()}),
 * but {@code record.from()} converts Enum to String using {@code toString()}. When an enum overrides
 * {@code toString()} to differ from {@code name()}, this causes a mismatch.
 * </p>
 * <p>
 * This provider wraps the default unmapper and corrects enum fields to use {@code name()}, mirroring
 * what jOOQ does on the read side ({@code Convert.java:1235}).
 * </p>
 */
public class EnumAwareRecordUnmapperProvider
    implements RecordUnmapperProvider
{
  /**
   * Caches the reflective lookup of enum fields per entity class. Key is the entity class, value is the list of
   * enum fields with their corresponding database column names (resolved via {@code @Column} or field name).
   * This avoids repeated reflection on every {@code record.from()} call.
   */
  private static final Map<Class<?>, List<EnumFieldMapping>> ENUM_FIELD_CACHE = new ConcurrentHashMap<>();

  private final Configuration configuration;

  public EnumAwareRecordUnmapperProvider(final Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  public <E, R extends Record> RecordUnmapper<E, R> provide(
      final Class<? extends E> type,
      final RecordType<R> rowType)
  {
    RecordUnmapper<E, R> delegate = new DefaultRecordUnmapper<>(type, rowType, configuration);
    List<EnumFieldMapping> enumFields = ENUM_FIELD_CACHE.computeIfAbsent(
        type, EnumAwareRecordUnmapperProvider::findEnumFields);

    if (enumFields.isEmpty()) {
      return delegate;
    }

    return source -> {
      R record = delegate.unmap(source);
      replaceEnumWithName(record, source, enumFields);
      return record;
    };
  }

  @SuppressWarnings("unchecked")
  private static <R extends Record> void replaceEnumWithName(
      final R record,
      final Object source,
      final List<EnumFieldMapping> enumFields)
  {
    for (EnumFieldMapping mapping : enumFields) {
      try {
        Enum<?> value = (Enum<?>) mapping.entityField.get(source);
        Field<String> recordField = (Field<String>) record.field(mapping.columnName);
        if (recordField != null) {
          record.set(recordField, value != null ? value.name() : null);
        }
      }
      catch (IllegalAccessException e) {
        throw new RuntimeException("Failed to read enum field: " + mapping.entityField.getName(), e);
      }
    }
  }

  /**
   * Discovers enum fields in the entity class hierarchy using reflection. This is the same approach
   * jOOQ's {@code DefaultRecordUnmapper#PojoUnmapper} uses to read POJO fields. Results are cached
   * in {@link #ENUM_FIELD_CACHE} so the reflective lookup only happens once per entity class.
   */
  private static List<EnumFieldMapping> findEnumFields(final Class<?> type) {
    List<EnumFieldMapping> result = new ArrayList<>();
    for (Class<?> clazz = type; clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
      for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
        if (field.getType().isEnum()) {
          field.setAccessible(true);
          result.add(new EnumFieldMapping(field, resolveColumnName(field)));
        }
      }
    }
    return result;
  }

  private static String resolveColumnName(final java.lang.reflect.Field field) {
    Column column = field.getAnnotation(Column.class);
    return (column != null && !column.name().isEmpty()) ? column.name() : field.getName();
  }

  private record EnumFieldMapping(java.lang.reflect.Field entityField, String columnName)
  {
  }
}
