/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.sonatype.insight.brain.utils.CsvWritable;

public class ApiActivityEventDTO
    implements CsvWritable
{
  public String username;

  public String timestamp;

  public String domain;

  public String type;

  public String method;

  public String uri;

  public String ipAddress;

  public String userAgent;

  public String errorType;

  public static String getCsvHeader() {
    return "Username,Timestamp,Domain,Type,Method,URI,IP Address,User Agent,Error";
  }

  @Override
  public String toCsvLine() {
    String escapedUsername = username != null
        ? CsvWritable.quoteFieldWhenSpecialCsvCharactersPresent(CsvWritable.escapeDoubleQuotes(username))
        : "";
    String escapedTimestamp = timestamp != null
        ? CsvWritable.quoteFieldWhenSpecialCsvCharactersPresent(CsvWritable.escapeDoubleQuotes(timestamp))
        : "";
    String escapedDomain = domain != null
        ? CsvWritable.quoteFieldWhenSpecialCsvCharactersPresent(CsvWritable.escapeDoubleQuotes(domain))
        : "";
    String escapedType = type != null
        ? CsvWritable.quoteFieldWhenSpecialCsvCharactersPresent(CsvWritable.escapeDoubleQuotes(type))
        : "";
    String escapedMethod = method != null
        ? CsvWritable.quoteFieldWhenSpecialCsvCharactersPresent(CsvWritable.escapeDoubleQuotes(method))
        : "";
    String escapedUri = uri != null
        ? CsvWritable.quoteFieldWhenSpecialCsvCharactersPresent(CsvWritable.escapeDoubleQuotes(uri))
        : "";
    String escapedIpAddress = ipAddress != null
        ? CsvWritable.quoteFieldWhenSpecialCsvCharactersPresent(CsvWritable.escapeDoubleQuotes(ipAddress))
        : "";
    String escapedUserAgent = userAgent != null
        ? CsvWritable.quoteFieldWhenSpecialCsvCharactersPresent(CsvWritable.escapeDoubleQuotes(userAgent))
        : "";
    String escapedErrorType = errorType != null
        ? CsvWritable.quoteFieldWhenSpecialCsvCharactersPresent(CsvWritable.escapeDoubleQuotes(errorType))
        : "";

    return joiner.join(escapedUsername, escapedTimestamp, escapedDomain, escapedType, escapedMethod, escapedUri,
        escapedIpAddress, escapedUserAgent, escapedErrorType);
  }
}
