/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.error;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import jakarta.ws.rs.core.Response;

/**
 * Parses a PURL string to confirm it is syntactically valid before forwarding to the upstream
 * search-server. Mirrors Guide SaaS's controller-layer check: SaaS calls {@code new
 * PackageURL(purl)} at the start of every PURL-accepting handler and returns a 400 with
 * {@code "Invalid PURL format: " + reason} when parsing fails. Without this check, IQ
 * self-hosted would forward the raw string to the upstream and surface its 5xx (or, worse,
 * unrelated results) — both contract violations vs SaaS.
 *
 * <p>
 * The parsed result is intentionally discarded; we only care about the side effect of the
 * constructor throwing on malformed input.
 *
 * <p>
 * Shared between the {@code GuideComponentsResource} and {@code GuideRecommendationsResource}
 * PURL-accepting handlers so the validation contract stays consistent across endpoints.
 */
public final class GuidePurlValidator
{
  private GuidePurlValidator() {
  }

  /**
   * @throws GuideApiException with status 400 if {@code purl} is not a syntactically valid
   *           PackageURL string.
   */
  public static void validate(String purl) {
    try {
      new PackageURL(purl);
    }
    catch (MalformedPackageURLException e) {
      throw new GuideApiException(Response.Status.BAD_REQUEST, "Invalid PURL format: " + e.getMessage());
    }
  }
}
