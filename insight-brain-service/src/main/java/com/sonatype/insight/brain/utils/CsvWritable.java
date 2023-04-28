/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;

/**
 * @since 1.24.0
 */
public interface CsvWritable
{
  Joiner joiner = Joiner.on(",");

  Pattern pattern = Pattern.compile("[,\n\r\"]", Pattern.CASE_INSENSITIVE);

  DateTimeFormatter dateFormatter =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(TimeZone.getTimeZone("UTC").toZoneId());

  String toCsvLine();

  default String formatDate(Date date) {
    if (date == null) {
      return "";
    }
    return dateFormatter.format(date.toInstant());
  }

  static String escapeDoubleQuotes(String field) {
    return field.isEmpty() ? field : field.replace("\"", "\"\"");
  }

  static String quoteFieldWhenSpecialCsvCharactersPresent(String field) {
    if (field.isEmpty()) {
      return field;
    }
    Matcher matcher = pattern.matcher(field);
    if (matcher.find()) {
      return "\"" + field + "\"";
    }

    return field;
  }
}
