/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.purl;

import java.util.TreeMap;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.sonatype.insight.brain.guide.api.error.GuideApiException;
import jakarta.ws.rs.core.Response;

/**
 * Decorates an already-built PURL string with artifact-selecting qualifiers ({@code type} and
 * {@code classifier}) from the Guide API's {@code extension} / {@code classifier} query
 * parameters, applying the {@code extension} &harr; {@code type} mapping used by search-server.
 *
 * <p>
 * Mirrors seaworthy's {@code PurlArtifactQualifiers.buildArtifactQualifiers} in behavior:
 * blank inputs are treated identically to {@code null} and omitted from the qualifiers map. That
 * matters because Spring (SaaS) and Jersey (self-hosted) both bind an absent or empty form field
 * as the empty string, and {@link PackageURL} rejects qualifier entries with empty values at
 * construction time — collapsing both forms here prevents callers from accidentally triggering
 * {@code MalformedPackageURLException} on a request like {@code ?extension=&classifier=}.
 *
 * <p>
 * Pre-existing qualifiers on the input PURL are preserved. When the request supplies an
 * {@code extension} for a PURL that already carries a {@code type} qualifier, the request value
 * wins.
 *
 * <p>
 * Companion to {@link com.sonatype.insight.brain.guide.api.purl.GuidePurlAssembler}: the
 * assembler builds PURLs from {@code (format, namespace, name, version)}; this helper decorates
 * an already-built PURL. The two responsibilities are intentionally split — each function has a
 * narrow contract.
 */
public final class PurlArtifactQualifiers
{
  /**
   * PURL qualifier name that carries the artifact's file extension. Guide uses {@code "type"}
   * uniformly for every format (Maven, npm, PyPI, RubyGems, …) because search-server's
   * {@code PurlArtifactKeyExtractor} reads this single key on the wire, and the v6 OpenSearch
   * index stores the value under {@code artifacts[].extension} regardless of format.
   */
  public static final String TYPE_QUALIFIER = "type";

  /** PURL qualifier name for the artifact classifier (Maven {@code sources}/{@code javadoc}, etc.). */
  public static final String CLASSIFIER_QUALIFIER = "classifier";

  private PurlArtifactQualifiers() {
    // Utility class
  }

  /**
   * Returns a canonical PURL string with the given artifact selectors merged into its
   * qualifiers. Blank (null, empty, or whitespace-only) selectors are omitted. Returns the
   * input unchanged when both selectors are blank. Returns {@code null} when the input PURL is
   * {@code null}.
   *
   * @throws GuideApiException with HTTP 400 if the input PURL is malformed
   */
  public static String withArtifactQualifiers(String purl, String extension, String classifier) {
    if (purl == null) {
      return null;
    }
    boolean hasExt = extension != null && !extension.isBlank();
    boolean hasCls = classifier != null && !classifier.isBlank();
    if (!hasExt && !hasCls) {
      return purl;
    }
    try {
      PackageURL parsed = new PackageURL(purl);
      TreeMap<String, String> qualifiers = parsed.getQualifiers() != null
          ? new TreeMap<>(parsed.getQualifiers())
          : new TreeMap<>();
      if (hasExt) {
        qualifiers.put(TYPE_QUALIFIER, extension);
      }
      if (hasCls) {
        qualifiers.put(CLASSIFIER_QUALIFIER, classifier);
      }
      return new PackageURL(
          parsed.getType(),
          parsed.getNamespace(),
          parsed.getName(),
          parsed.getVersion(),
          qualifiers,
          parsed.getSubpath())
              .canonicalize();
    }
    catch (MalformedPackageURLException e) {
      throw new GuideApiException(Response.Status.BAD_REQUEST, "Invalid PURL: " + e.getMessage());
    }
  }
}
