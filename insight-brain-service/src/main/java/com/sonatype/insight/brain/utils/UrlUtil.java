/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

public class UrlUtil
{
  public static String getDomainWithProtocol(String baseUrl) {
    try {
      URL url = new URL(baseUrl);
      return StringUtils.substring(url.toURI().resolve("/").toString(),0,-1);
    }
    catch (MalformedURLException | URISyntaxException e) {
      throw new BadRequestException("'baseUrl' is not valid", e.getCause());
    }
  }
}
