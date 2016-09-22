/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils;

import javax.ws.rs.core.UriBuilder;

import com.codeborne.selenide.Configuration;

public class BaseUrl
{
  public static UriBuilder uriBuilder() {
    return rootUriBuilder().path("assets/index.html");
  }

  public static UriBuilder rootUriBuilder() {
    return UriBuilder.fromUri(Configuration.baseUrl);
  }
}
