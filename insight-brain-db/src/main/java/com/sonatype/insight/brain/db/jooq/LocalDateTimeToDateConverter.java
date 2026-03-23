/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.jooq;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.jooq.Converter;

/**
 * jOOQ converter that converts between database TIMESTAMP (LocalDateTime) and entity Date fields.
 * This allows entity models to use java.util.Date while jOOQ handles the conversion automatically.
 *
 * @since 1.201
 */
public class LocalDateTimeToDateConverter
    implements Converter<LocalDateTime, Date>
{
  private static final long serialVersionUID = 1L;

  @Override
  public Date from(final LocalDateTime databaseObject) {
    if (databaseObject == null) {
      return null;
    }
    return Date.from(databaseObject.atZone(ZoneId.systemDefault()).toInstant());
  }

  @Override
  public LocalDateTime to(final Date userObject) {
    if (userObject == null) {
      return null;
    }
    // Use getTime() instead of toInstant() because java.sql.Date.toInstant() throws UnsupportedOperationException
    return Instant.ofEpochMilli(userObject.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
  }

  @Override
  public Class<LocalDateTime> fromType() {
    return LocalDateTime.class;
  }

  @Override
  public Class<Date> toType() {
    return Date.class;
  }
}
