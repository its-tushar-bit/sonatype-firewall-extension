/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.sonatype.insight.brain.utils.CsvWritable;

public class ApiUserActivityDTO
    implements CsvWritable
{
  public String username;

  public Integer loginCount;

  public String lastActive;

  public static String getCsvHeader() {
    return "Username,Login Count,Last Active";
  }

  @Override
  public String toCsvLine() {
    String escapedUsername = username != null
        ? CsvWritable.quoteFieldWhenSpecialCsvCharactersPresent(CsvWritable.escapeDoubleQuotes(username))
        : "";
    String loginCountStr = loginCount != null ? loginCount.toString() : "0";
    String escapedLastActive = lastActive != null
        ? CsvWritable.quoteFieldWhenSpecialCsvCharactersPresent(CsvWritable.escapeDoubleQuotes(lastActive))
        : "";

    return joiner.join(escapedUsername, loginCountStr, escapedLastActive);
  }
}
