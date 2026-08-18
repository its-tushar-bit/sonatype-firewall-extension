/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.aideveloper;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Opt-in state of AI Developer for this server.
 *
 * @param optedIn whether AI Developer has been unlocked by an opt-in. The unlock applies to everyone on this server,
 *          so this is the same answer for every caller
 * @param optedInBy internal name of the user who opted in, {@code null} when nobody has or the record carries no name
 * @param optedInAt ISO-8601 instant the opt-in was recorded, {@code null} when nobody has opted in or the record
 *          carries no instant
 * @param externalDatabaseRequired whether the opt-in is inert because the server runs on the embedded database.
 *          AI Developer needs an external database, so an opt-in recorded here grants nothing until the server is
 *          moved to PostgreSQL
 * @param message human-readable reason the opt-in grants nothing, {@code null} when it does grant access
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiDeveloperOptInStatus(
    boolean optedIn,
    String optedInBy,
    String optedInAt,
    boolean externalDatabaseRequired,
    String message)
{

  static final String EMBEDDED_DATABASE_MESSAGE =
      "AI Developer requires an external PostgreSQL database. This server uses the embedded database, "
          + "so opting in does not grant access until the server is migrated.";

  /**
   * Reads the {@code aiDeveloperOptIn} property value, which holds the recording user and the instant separated by a
   * comma. The instant carries no comma, so the last one separates the two fields and usernames may contain commas
   * themselves (LDAP distinguished names do). Any non-blank value counts as opted in, matching the license check,
   * even when it does not parse into both fields.
   */
  static AiDeveloperOptInStatus from(String propertyValue, boolean embeddedDatabase) {
    String value = propertyValue != null ? propertyValue.trim() : null;
    if (value == null || value.isBlank()) {
      return new AiDeveloperOptInStatus(false, null, null, embeddedDatabase, null);
    }
    int separator = value.lastIndexOf(',');
    String by = blankToNull(separator < 0 ? value : value.substring(0, separator));
    String at = separator < 0 ? null : blankToNull(value.substring(separator + 1));
    return new AiDeveloperOptInStatus(true, by, at, embeddedDatabase,
        embeddedDatabase ? EMBEDDED_DATABASE_MESSAGE : null);
  }

  /**
   * Reports a field the record does not carry as absent rather than as an empty string, so a reader parsing
   * {@code optedInAt} sees either an ISO-8601 instant or nothing at all.
   */
  private static String blankToNull(String field) {
    String trimmed = field.trim();
    return trimmed.isBlank() ? null : trimmed;
  }
}
