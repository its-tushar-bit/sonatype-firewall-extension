/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.error;

import jakarta.ws.rs.core.Response;

/**
 * Marker subclass of {@link GuideApiException} for "the Guide license is unavailable" errors.
 *
 * <p>
 * Two emission paths use this type:
 * <ul>
 * <li>The immediate 402 from {@link
 * com.sonatype.insight.brain.guide.core.SearchApiClientImpl} on the first request after
 * HDS revokes the Guide entitlement and IQ refreshes its license.</li>
 * <li>The 403 from {@link com.sonatype.insight.brain.security.SearchLicenseFilter} on
 * subsequent requests once the in-memory feature set lacks {@code GUIDE_SEARCH}, or on any
 * request in a multi-tenant deployment (where Guide is unsupported).</li>
 * </ul>
 *
 * <p>
 * The Guide SPA matches on the {@code X-Sonatype-Guide-License: unavailable} response
 * header (constants below) rather than parsing English text. {@link GuideErrorResponses}
 * attaches that header when the thrown exception is an instance of this subclass. The
 * header means only "Guide is off"; the SPA gates on it regardless of the underlying
 * reason. The human-readable cause (no license vs. multi-tenant) is carried in the
 * response body message, not the header.
 */
public class GuideLicenseUnavailableException
    extends GuideApiException
{
  /** Response header that flags Guide-license-unavailable errors for the SPA. */
  public static final String LICENSE_HEADER = "X-Sonatype-Guide-License";

  /** Header value emitted when Guide is not currently licensed. */
  public static final String LICENSE_UNAVAILABLE = "unavailable";

  public GuideLicenseUnavailableException(Response.Status status, String message) {
    super(status, message);
  }
}
