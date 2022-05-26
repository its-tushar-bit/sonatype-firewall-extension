/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.net.URL;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationUtils
{
  private static final Logger log = LoggerFactory.getLogger(ConfigurationUtils.class);

  private ConfigurationUtils() { }

  public static String urlValueToString(Object value) {
    if (value == null) {
      return null;
    }
    String url = addEndingForwardSlashIfNeeded(value.toString());
    validateUrl(url);
    return url;
  }

  private static String addEndingForwardSlashIfNeeded(String url) {
    if (url != null && !url.endsWith("/")) {
      url += '/';
    }
    return url;
  }

  private static void validateUrl(String url) {
    try {
      new URL(url);
    }
    catch (Exception e) {
      throw new BadRequestException("Invalid URL: " + url, e);
    }
  }

  public static String forceBaseUrlToString(TransactionContext tx, Object value) {
    Boolean booleanValue = (Boolean) value;
    String baseUrl = new SystemConfigurationPropertyDAO().get(tx, "baseUrl");
    if (booleanValue != null && booleanValue) {
      log.error("DEPRECATION NOTICE: Forcing use of server base URL: {}, any 'X-Forwarded-*' headers will be " +
          "ignored. More information at http://links.sonatype.com/products/clm/docs/base-url", baseUrl);
    }
    else {
      log.info("Server base URL: {}", baseUrl);
    }
    return booleanValue == null ? null : Boolean.toString(booleanValue);
  }
}
