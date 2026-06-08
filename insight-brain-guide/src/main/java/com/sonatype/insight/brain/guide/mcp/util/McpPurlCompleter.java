/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.mcp.util;

import java.util.HashMap;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds sensible defaults to PURL coordinates so that bare PURLs sent by LLM clients are completed
 * with the fields downstream services (HDS, search-server, policy evaluation) require.
 *
 * <p>
 * Defaults applied when the corresponding coordinate is absent:
 * <ul>
 * <li>{@code maven}: {@code extension=jar} (PURL {@code ?type=jar})</li>
 * <li>{@code pypi}: {@code extension=tar.gz}</li>
 * <li>{@code gem} (RubyGems): {@code platform=ruby}</li>
 * </ul>
 *
 * <p>
 * Other formats and unparseable input are returned unchanged so the downstream caller produces
 * its own diagnostic.
 */
public final class McpPurlCompleter
{
  private static final Logger log = LoggerFactory.getLogger(McpPurlCompleter.class);

  private McpPurlCompleter() {
  }

  /**
   * Returns a PURL string with sensible defaults applied for ecosystems that require additional
   * coordinates beyond the bare {@code pkg:type/namespace/name@version} form. If the input cannot
   * be parsed, the original string is returned unchanged.
   */
  public static String complete(String purl) {
    if (purl == null || purl.isBlank()) {
      return purl;
    }
    try {
      ComponentIdentifier identifier = new PackageUrlIdentifier(purl).toComponentIdentifier();
      String format = identifier.getFormat();
      if (format == null) {
        return purl;
      }
      Map<String, String> coords = new HashMap<>(identifier.getCoordinates());
      if (!applyDefaults(format, coords)) {
        return purl;
      }
      return PackageUrlIdentifier.toPackageUrl(new ComponentIdentifier(format, coords));
    }
    catch (Exception e) {
      log.debug("Failed to complete PURL '{}': {}", purl, e.getMessage());
      return purl;
    }
  }

  private static boolean applyDefaults(String format, Map<String, String> coords) {
    switch (format) {
      case ComponentIdentifier.FORMAT_MAVEN:
        return putIfMissing(coords, ComponentIdentifier.MAVEN_EXTENSION, "jar");
      case ComponentIdentifier.FORMAT_PYPI:
        return putIfMissing(coords, ComponentIdentifier.PYPI_EXTENSION, "tar.gz");
      case ComponentIdentifier.FORMAT_RUBYGEMS:
        return putIfMissing(coords, ComponentIdentifier.RUBYGEMS_PLATFORM, "ruby");
      default:
        return false;
    }
  }

  private static boolean putIfMissing(Map<String, String> coords, String key, String value) {
    String existing = coords.get(key);
    if (existing == null || existing.isBlank()) {
      coords.put(key, value);
      return true;
    }
    return false;
  }
}
