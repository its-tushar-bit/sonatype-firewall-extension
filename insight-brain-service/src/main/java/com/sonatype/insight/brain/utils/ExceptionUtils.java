/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.sql.SQLException;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.RollbackException;

import org.jooq.exception.DataAccessException;

public final class ExceptionUtils
{
  private ExceptionUtils() {
  }

  /**
   * Checks if an exception represents a database duplicate key constraint violation.
   * <p>
   * Handles both JPA exceptions (for legacy code) and jOOQ exceptions:
   * <ul>
   * <li>Direct {@link EntityExistsException} (legacy JPA)</li>
   * <li>{@link RollbackException} wrapping EntityExistsException (legacy JPA with PostgreSQL)</li>
   * <li>{@link DataAccessException} from jOOQ containing "Unique" in message</li>
   * <li>{@link SQLException} with SQLState 23505 for unique violations (PostgreSQL and H2)</li>
   * </ul>
   *
   * @param e the exception to check
   * @return true if the exception represents a duplicate key violation, false otherwise
   */
  public static boolean isDuplicateKeyException(Exception e) {
    // Legacy JPA exceptions
    if (e instanceof EntityExistsException) {
      return true;
    }
    if (e instanceof RollbackException) {
      Throwable cause = e.getCause();
      return cause instanceof EntityExistsException;
    }

    // jOOQ exceptions
    if (e instanceof DataAccessException) {
      String message = e.getMessage();
      if (message != null && message.contains("Unique")) {
        return true;
      }
      // Check for SQLException cause with duplicate key SQLState
      Throwable cause = e.getCause();
      if (cause instanceof SQLException) {
        String sqlState = ((SQLException) cause).getSQLState();
        // 23505 = unique_violation (PostgreSQL and H2)
        return "23505".equals(sqlState);
      }
    }

    return false;
  }
}
