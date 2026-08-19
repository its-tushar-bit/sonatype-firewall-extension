/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.error;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GuidePurlValidatorTest
{
  @Test
  public void validate_acceptsValidPurl() {
    assertThatCode(() -> GuidePurlValidator.validate("pkg:maven/org.apache.logging.log4j/log4j-core@2.14.0"))
        .doesNotThrowAnyException();
  }

  @Test
  public void validate_rejectsMalformedPurl_withSaasPrefixedMessage() {
    assertThatThrownBy(() -> GuidePurlValidator.validate("not-a-purl"))
        .isInstanceOf(GuideApiException.class)
        .hasMessageStartingWith("Invalid PURL format: ")
        .extracting(e -> ((GuideApiException) e).getResponse().getStatus())
        .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void validate_rejectsNullPurl() {
    // Defensive: callers are expected to null-check before calling validate(), but if a null
    // does slip through, we want a structured 400 rather than a NullPointerException flying
    // up to the framework default handler.
    assertThatThrownBy(() -> GuidePurlValidator.validate(null))
        .isInstanceOf(GuideApiException.class)
        .hasMessageStartingWith("Invalid PURL format: ");
  }

  @Test
  public void validate_rejectsBlankPurl() {
    assertThatThrownBy(() -> GuidePurlValidator.validate(""))
        .isInstanceOf(GuideApiException.class)
        .hasMessageStartingWith("Invalid PURL format: ");
  }
}
