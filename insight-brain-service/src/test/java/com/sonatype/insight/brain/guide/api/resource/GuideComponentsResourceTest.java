/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.resource;

import com.sonatype.insight.brain.guide.api.error.GuideApiException;
import com.sonatype.insight.brain.guide.core.SearchApiClient;
import jakarta.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@RunWith(MockitoJUnitRunner.class)
public class GuideComponentsResourceTest
{
  @Mock
  private SearchApiClient searchApiClient;

  private GuideComponentsResource underTest;

  @Before
  public void setUp() {
    underTest = new GuideComponentsResource(searchApiClient);
  }

  @Test
  public void getComponentDetail_byCoords_missingVersion_returns400() {
    assertThatThrownBy(
        () -> underTest.getComponentDetail(null, "maven", "org.apache.logging.log4j", "log4j-core", null))
            .isInstanceOf(GuideApiException.class)
            .hasMessageContaining("'format', 'name', 'version'")
            .extracting(e -> ((GuideApiException) e).getResponse().getStatus())
            .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    verifyNoInteractions(searchApiClient);
  }

  @Test
  public void getComponentDetail_byCoords_blankVersion_returns400() {
    assertThatThrownBy(
        () -> underTest.getComponentDetail(null, "maven", "org.apache.logging.log4j", "log4j-core", "  "))
            .isInstanceOf(GuideApiException.class)
            .hasMessageContaining("'format', 'name', 'version'");
    verifyNoInteractions(searchApiClient);
  }

  @Test
  public void getComponentDetail_byCoords_missingFormat_returns400() {
    assertThatThrownBy(() -> underTest.getComponentDetail(null, null, null, "log4j-core", "2.14.0"))
        .isInstanceOf(GuideApiException.class)
        .hasMessageContaining("'format', 'name', 'version'");
    verifyNoInteractions(searchApiClient);
  }

  @Test
  public void getComponentDetail_byCoords_missingName_returns400() {
    assertThatThrownBy(() -> underTest.getComponentDetail(null, "maven", null, null, "2.14.0"))
        .isInstanceOf(GuideApiException.class)
        .hasMessageContaining("'format', 'name', 'version'");
    verifyNoInteractions(searchApiClient);
  }

  @Test
  public void getComponentDetail_noPurlAndNoCoords_returns400() {
    assertThatThrownBy(() -> underTest.getComponentDetail(null, null, null, null, null))
        .isInstanceOf(GuideApiException.class)
        .hasMessageContaining("'format', 'name', 'version'");
    verifyNoInteractions(searchApiClient);
  }

  // The same buildPurl helper is used by /versions, /vulnerabilities, /dependencies; covering
  // /detail here is sufficient to lock in the validation contract for all four endpoints.

  @Test
  public void getComponentDetail_byPurl_invalidPurl_returns400() {
    assertThatThrownBy(() -> underTest.getComponentDetail("not-a-purl", null, null, null, null))
        .isInstanceOf(GuideApiException.class)
        .hasMessageStartingWith("Invalid PURL format: ")
        .extracting(e -> ((GuideApiException) e).getResponse().getStatus())
        .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    verifyNoInteractions(searchApiClient);
  }

  @Test
  public void getLatestVersion_invalidPurl_returns400() {
    var request = new com.sonatype.guide.api.request.LatestVersionRequest("not-a-purl");
    assertThatThrownBy(() -> underTest.getLatestVersion(request))
        .isInstanceOf(GuideApiException.class)
        .hasMessageStartingWith("Invalid PURL format: ");
    verifyNoInteractions(searchApiClient);
  }

  // validatePurl is the single chokepoint shared by all five PURL-accepting paths
  // (detail, versions, vulnerabilities, dependencies, latest-version), so covering
  // /detail and /latest-version here exercises the same code path the others use.

  @Test
  public void getComponentDetail_byCoords_scopedNpm_buildsCanonicalPurl() throws Exception {
    // The Guide SPA's component detail page splits a URL like
    // /component/npm/%40types%2Fnode/25.9.2 and forwards name="@types/node" verbatim — the
    // shared @guide/ui-core helper splits on ":" rather than "/", so the scope+slash stays
    // inside name. Before this PR's PackageURL-constructor fix, naive concatenation here
    // produced the literal "pkg:npm/@types/node@25.9.2", which the PackageURL parser
    // misread as (namespace=@types, name=node) and HDS returned 404. The typed constructor
    // URL-encodes the "@" and "/" inside name, producing the canonical
    // "pkg:npm/%40types%2Fnode@25.9.2" that round-trips to (namespace=null, name="@types/node").
    underTest.getComponentDetail(null, "npm", null, "@types/node", "25.9.2");

    ArgumentCaptor<String> purl = ArgumentCaptor.forClass(String.class);
    verify(searchApiClient).getComponentDetailByPurl(purl.capture());
    assertThat(purl.getValue()).isEqualTo("pkg:npm/%40types%2Fnode@25.9.2");
  }
}
