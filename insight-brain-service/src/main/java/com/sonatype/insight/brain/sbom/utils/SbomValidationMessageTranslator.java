/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.utils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Translates raw JSON-schema / XSD / scanner error strings into human-readable
 * equivalents while preserving any {@code "Line: L, Column: C, Path: P, Error: "}
 * prefix. Applied per-row so scoping is inherently local. Unknown patterns pass
 * through unchanged. Shared by {@code SbomFileDetector} (SBOM Manager upload path)
 * and {@code ApiThirdPartyScanService} (Lifecycle scan-source path). CLM-40052.
 */
public final class SbomValidationMessageTranslator
{
  private SbomValidationMessageTranslator() {
  }

  private record Rewrite(Pattern pattern, String replacement)
  {
  }

  // These patterns are intentionally unanchored to handle validator prefixes
  // like "Line: 16, Column: 6, Path: $.components[0], Error: ..." except for
  // the two "not supported" patterns which are anchored (^...$) because they
  // should only match exact versions and not substrings.
  private static final List<Rewrite> REWRITES = List.of(
      new Rewrite(
          Pattern.compile("must not be valid to the schema \\{\"required\":\\[\"versionRange\"\\]\\}"),
          "versionRange is only allowed when isExternal=true. Set isExternal: true on this component, or remove versionRange."),
      new Rewrite(
          Pattern.compile("must not be valid to the schema \\{\"required\":\\[\"version\",\"versionRange\"\\]\\}"),
          "version and versionRange are mutually exclusive. Set one, not both."),
      new Rewrite(
          Pattern.compile("required property '([^']+)' not found"),
          "Missing required field \"$1\"."),
      new Rewrite(
          Pattern.compile(
              "property '([^']+)' is not defined in the schema and the schema does not allow additional properties"),
          "Field \"$1\" is not defined in this CycloneDX schema. Remove it or use a properties[] entry."),
      new Rewrite(
          Pattern.compile("does not match the date-time pattern must be a valid RFC 3339 date-time"),
          "Not a valid RFC 3339 date-time (expected e.g. \"2026-06-30T12:00:00Z\")."),
      new Rewrite(
          Pattern.compile("^CycloneDX XML namespace is not supported$"),
          "This SBOM's XML namespace isn't a recognized CycloneDX version. IQ supports CycloneDX 1.1 through 1.7."),
      new Rewrite(
          Pattern.compile("^CycloneDX JSON (\\d+\\.\\d+) version is not supported$"),
          "CycloneDX $1 isn't supported. IQ supports CycloneDX 1.1 through 1.7."));

  public static String translate(final String raw) {
    if (raw == null) {
      return null;
    }
    for (Rewrite rw : REWRITES) {
      Matcher m = rw.pattern.matcher(raw);
      if (m.find()) {
        return m.replaceFirst(rw.replacement);
      }
    }
    return raw;
  }
}
