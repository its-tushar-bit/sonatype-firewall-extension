/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.render;

import java.net.URISyntaxException;

import com.sonatype.nexus.scm.SourceControlProvider;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UTMSourceUtil
{
  private static final Logger log = LoggerFactory.getLogger(UTMSourceUtil.class);

  public UTMSourceUtil() {
  }

  public static String maybeAppendUTMSourceParam(final String url, final SourceControlProvider provider) {
    try {
      final URIBuilder uriBuilder = new URIBuilder(url);
      if (provider == SourceControlProvider.GITHUB || provider == SourceControlProvider.GITLAB) {
        uriBuilder.addParameter("utm_source", provider.toString());
      }
      return uriBuilder.toString();
    }
    catch (URISyntaxException e) {
      log.error("Error parsing url {}", url);
    }
    return url;
  }
}
