/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.codeborne.selenide.SelenideElement;
import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import jakarta.ws.rs.core.UriBuilder;

/**
 * Page object for the Nexus One SPA served at /assets/nexus-one/index.html
 */
public class NexusOnePage
    extends BasicElement<NexusOnePage>
{
  public static final String ROOT = ".radix-themes";

  public static String url() {
    return url("/hello1");
  }

  public static String url(String hashRoute) {
    return UriBuilder.fromUri(BaseUrl.rootUriBuilder().build())
        .path("assets/nexus-one/index.html")
        .fragment(hashRoute)
        .build()
        .toString();
  }

  public NexusOnePage() {
    super(ROOT);
  }

  public SelenideElement heading() {
    return child("h1");
  }
}
