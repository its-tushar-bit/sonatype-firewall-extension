/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.jooq;

import com.sonatype.insight.brain.db.datastore.DataStore;

import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

/**
 * Utility class for handling database dialect-specific operations.
 *
 * <p>
 * This class provides helper methods for operations that behave differently
 * between H2 and PostgreSQL, such as:
 * <ul>
 * <li>Array operations (PostgreSQL's ANY operator)</li>
 * <li>Advisory locks</li>
 * <li>Window functions with dialect-specific syntax</li>
 * <li>Type casting differences</li>
 * </ul>
 * </p>
 */
public final class DialectHelper
{
  public static final String POSTGRES_UNIQUE_CONSTRAINT_VIOLATION = "23505";

  private DialectHelper() {
    // Utility class
  }

  /**
   * Get the current date in a dialect-appropriate way.
   *
   * @return a Field representing the current date
   */
  public static Field<java.time.LocalDate> currentDate() {
    return DSL.currentLocalDate();
  }

  /**
   * Create a COALESCE expression.
   *
   * @param field the field to check
   * @param defaultValue the default value if field is null
   * @param <T> the field type
   * @return a Field representing COALESCE(field, defaultValue)
   */
  public static <T> Field<T> coalesce(final Field<T> field, final T defaultValue) {
    return DSL.coalesce(field, defaultValue);
  }

  /**
   * Get the string concatenation operator for the dialect.
   *
   * <p>
   * PostgreSQL uses || while H2 supports both || and CONCAT().
   * </p>
   *
   * @param fields the fields to concatenate
   * @return a Field representing the concatenated string
   */
  public static Field<String> concat(final Field<?>... fields) {
    return DSL.concat(fields);
  }

  /**
   * Detect the SQL dialect for the given DataStore.
   *
   * @param dataStore the DataStore to detect the dialect for
   * @return the appropriate SQLDialect (H2 or POSTGRES)
   */
  public static SQLDialect detectDialect(final DataStore dataStore) {
    if (dataStore.isDatabaseEmbedded()) {
      return SQLDialect.H2;
    }
    return SQLDialect.POSTGRES;
  }
}
