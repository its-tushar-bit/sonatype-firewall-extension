/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.purl;

import com.sonatype.insight.brain.guide.api.error.GuideApiException;
import jakarta.ws.rs.core.Response;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PurlArtifactQualifiersTest
{
  private static final String BASE_PURL = "pkg:maven/org.apache.commons/commons-lang3@3.12.0";

  @Test
  public void withArtifactQualifiers_bothNull_returnsInputUnchanged() {
    assertThat(PurlArtifactQualifiers.withArtifactQualifiers(BASE_PURL, null, null))
        .isEqualTo(BASE_PURL);
  }

  @Test
  public void withArtifactQualifiers_bothBlank_returnsInputUnchanged() {
    assertThat(PurlArtifactQualifiers.withArtifactQualifiers(BASE_PURL, "", "  "))
        .isEqualTo(BASE_PURL);
  }

  @Test
  public void withArtifactQualifiers_nullPurl_returnsNull() {
    assertThat(PurlArtifactQualifiers.withArtifactQualifiers(null, "jar", "sources")).isNull();
  }

  @Test
  public void withArtifactQualifiers_extensionOnly_addsTypeQualifier() {
    assertThat(PurlArtifactQualifiers.withArtifactQualifiers(BASE_PURL, "jar", null))
        .isEqualTo(BASE_PURL + "?type=jar");
  }

  @Test
  public void withArtifactQualifiers_classifierOnly_addsClassifierQualifier() {
    assertThat(PurlArtifactQualifiers.withArtifactQualifiers(BASE_PURL, null, "sources"))
        .isEqualTo(BASE_PURL + "?classifier=sources");
  }

  @Test
  public void withArtifactQualifiers_bothPresent_addsBothInCanonicalOrder() {
    // PackageURL.canonicalize() sorts qualifiers alphabetically, so "classifier" precedes "type"
    assertThat(PurlArtifactQualifiers.withArtifactQualifiers(BASE_PURL, "jar", "sources"))
        .isEqualTo(BASE_PURL + "?classifier=sources&type=jar");
  }

  @Test
  public void withArtifactQualifiers_extensionBlankClassifierPresent_addsClassifierOnly() {
    assertThat(PurlArtifactQualifiers.withArtifactQualifiers(BASE_PURL, "  ", "sources"))
        .isEqualTo(BASE_PURL + "?classifier=sources");
  }

  @Test
  public void withArtifactQualifiers_preservesExistingQualifiers() {
    // pkg:pypi/Django@5.1.5?extension=tar.gz — extension is a pre-existing qualifier that must
    // survive when we add a classifier. Note: PackageURL canonicalizes PyPI package names to
    // lowercase, so "Django" becomes "django" in the output.
    String pypiPurl = "pkg:pypi/Django@5.1.5?extension=tar.gz";
    assertThat(PurlArtifactQualifiers.withArtifactQualifiers(pypiPurl, null, "wheel"))
        .isEqualTo("pkg:pypi/django@5.1.5?classifier=wheel&extension=tar.gz");
  }

  @Test
  public void withArtifactQualifiers_requestExtensionOverridesExistingType() {
    String purlWithType = BASE_PURL + "?type=pom";
    assertThat(PurlArtifactQualifiers.withArtifactQualifiers(purlWithType, "jar", null))
        .isEqualTo(BASE_PURL + "?type=jar");
  }

  @Test
  public void withArtifactQualifiers_malformedPurl_throwsBadRequest() {
    assertThatThrownBy(
        () -> PurlArtifactQualifiers.withArtifactQualifiers("not-a-purl", "jar", null))
            .isInstanceOf(GuideApiException.class)
            .extracting(e -> ((GuideApiException) e).getResponse().getStatus())
            .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void withArtifactQualifiers_scopedNpmPurl_preservesEncoding() {
    // Regression guard: adding qualifiers to a scoped-npm PURL must not break the %-encoding
    // that keeps the namespace/name split intact.
    String scopedPurl = "pkg:npm/%40acceleratxr%2Fclient_sdk@1.14.0";
    assertThat(PurlArtifactQualifiers.withArtifactQualifiers(scopedPurl, "tgz", null))
        .isEqualTo(scopedPurl + "?type=tgz");
  }
}
