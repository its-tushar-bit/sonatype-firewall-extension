/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.core;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sonatype.guide.api.dto.ApiSearchResponse;
import com.sonatype.guide.api.dto.ComponentDocument;
import com.sonatype.guide.api.dto.RecommendationResponse.Outcome;
import com.sonatype.guide.api.dto.SearchResult;
import com.sonatype.guide.api.dto.VulnerabilityDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDependenciesRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDetailDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDetailSearchResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentSearchRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentVersionsRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentVulnerabilitiesRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentSearchResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideGlobalSearchRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideGlobalSearchResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideRecommendationResult;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilityDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilitySearchRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilitySearchResponse;
import com.sonatype.insight.brain.guide.api.dto.RecommendedVersionInfo;
import com.sonatype.insight.brain.guide.api.error.GuideApiException;
import com.sonatype.insight.brain.guide.api.error.GuideLicenseUnavailableException;
import com.sonatype.insight.brain.guide.api.error.GuideNotFoundException;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.error.exception.PaymentRequiredException;

import com.sonatype.insight.brain.security.SecurityAspectControl;
import jakarta.ws.rs.InternalServerErrorException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SearchApiClientImplTest
{
  private static final String PURL = "pkg:maven/org.example/lib@1.0.0";

  @Mock
  private HdsClient hdsClient;

  @Mock
  private GuideLicenseRevocationHandler revocationHandler;

  private SearchApiClientImpl underTest;

  @Before
  public void setUp() {
    SecurityAspectControl.disableEnforcement();
    underTest = new SearchApiClientImpl(hdsClient, revocationHandler);
  }

  @After
  public void tearDown() {
    SecurityAspectControl.enableEnforcement();
  }

  @Test
  public void testGetComponentByPurl_delegatesToHdsGet() {
    when(hdsClient.get(String.class, "rest/search/components/detail", Map.of("purl", PURL)))
        .thenReturn("{\"component\":\"data\"}");

    String result = underTest.getComponentByPurl(PURL);

    assertThat(result).isEqualTo("{\"component\":\"data\"}");
    verify(hdsClient).get(String.class, "rest/search/components/detail", Map.of("purl", PURL));
  }

  @Test
  public void testGetComponentByPurl_scopedNpm_preEncodesPercentSigns() {
    // jakarta.ws.rs UriBuilder.queryParam treats existing %XX in a value as already-encoded
    // and won't double-encode it, which means a canonical PURL like
    // pkg:npm/%40acceleratxr%2Fclient_sdk@1.14.0 reaches the upstream search-server (after
    // one URL-decode) as pkg:npm/@acceleratxr/client_sdk@1.14.0 — the "slash" form, which
    // PackageURL parses as (namespace=@acceleratxr, name=client_sdk) instead of the
    // (namespace=null, name=@acceleratxr/client_sdk) form OpenSearch indexes by.
    //
    // Guide SaaS goes through Spring's UriComponentsBuilder, which DOES double-encode the
    // existing %XX, so search-server sees the canonical form and parses it correctly. This
    // test locks in that SearchApiClientImpl pre-encodes % so the IQ→HDS wire format
    // matches the SaaS→search-server wire format. Without the pre-encoding, scoped npm
    // packages 404 in self-hosted Guide while working on SaaS.
    String canonicalScopedPurl = "pkg:npm/%40acceleratxr%2Fclient_sdk@1.14.0";
    String expectedWirePurl = "pkg:npm/%2540acceleratxr%252Fclient_sdk@1.14.0";

    underTest.getComponentByPurl(canonicalScopedPurl);

    verify(hdsClient).get(String.class, "rest/search/components/detail",
        Map.of("purl", expectedWirePurl));
  }

  // The five tests below mirror testGetComponentByPurl_scopedNpm_preEncodesPercentSigns
  // for every other code path that forwards a PURL through purlForUpstream(). Each one
  // pins the IQ→HDS wire format so a regression that drops the % → %25 pre-encoding
  // (or skips one of these call sites) trips a test instead of silently 404'ing scoped
  // npm packages on self-hosted Guide while SaaS keeps working.
  private static final String SCOPED_NPM_PURL = "pkg:npm/%40acceleratxr%2Fclient_sdk@1.14.0";

  private static final String SCOPED_NPM_PURL_WIRE =
      "pkg:npm/%2540acceleratxr%252Fclient_sdk@1.14.0";

  @Test
  public void testGetComponentDetailByPurl_scopedNpm_preEncodesPercentSigns() {
    underTest.getComponentDetailByPurl(SCOPED_NPM_PURL);

    verify(hdsClient).get(GuideComponentDetailDocument.class, "rest/search/components/detail",
        Map.of("purl", SCOPED_NPM_PURL_WIRE));
  }

  @Test
  public void testGetComponentVersions_scopedNpm_preEncodesPercentSigns() {
    GuideComponentVersionsRequest request = new GuideComponentVersionsRequest(
        SCOPED_NPM_PURL, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

    Multimap<String, String> expectedParams = ArrayListMultimap.create();
    expectedParams.put("purl", SCOPED_NPM_PURL_WIRE);

    underTest.getComponentVersions(request);

    verify(hdsClient).getWithMultimap(GuideComponentDetailSearchResponse.class,
        "rest/search/components/versions", expectedParams);
  }

  @Test
  public void testGetComponentVulnerabilities_scopedNpm_preEncodesPercentSigns() {
    GuideComponentVulnerabilitiesRequest request = new GuideComponentVulnerabilitiesRequest(
        SCOPED_NPM_PURL, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null);

    Multimap<String, String> expectedParams = ArrayListMultimap.create();
    expectedParams.put("purl", SCOPED_NPM_PURL_WIRE);

    underTest.getComponentVulnerabilities(request);

    verify(hdsClient).getWithMultimap(GuideVulnerabilitySearchResponse.class,
        "rest/search/components/vulnerabilities", expectedParams);
  }

  @Test
  public void testGetComponentDependencies_scopedNpm_preEncodesPercentSigns() {
    GuideComponentDependenciesRequest request = new GuideComponentDependenciesRequest(
        SCOPED_NPM_PURL, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null);

    Multimap<String, String> expectedParams = ArrayListMultimap.create();
    expectedParams.put("purl", SCOPED_NPM_PURL_WIRE);

    underTest.getComponentDependencies(request);

    verify(hdsClient).getWithMultimap(GuideComponentSearchResponse.class,
        "rest/search/components/dependencies", expectedParams);
  }

  @Test
  public void testGetLatestVersionDetail_scopedNpm_preEncodesPercentSigns() {
    underTest.getLatestVersionDetail(SCOPED_NPM_PURL);

    verify(hdsClient).post(GuideComponentDetailDocument.class, "rest/search/components/latest-version",
        Map.of("purl", SCOPED_NPM_PURL_WIRE));
  }

  // ---- Artifact selector (extension/classifier) wire-format tests ----
  //
  // Search-server (HDS) reads artifact selectors from the PURL's `type` and `classifier`
  // qualifiers on the by-PURL routes, not from sibling query params. These tests lock in
  // that SearchApiClientImpl folds request.extension() / request.classifier() into the
  // outgoing PURL BEFORE the % → %25 pre-encoding runs.

  private static final String QUALIFIED_MAVEN_PURL =
      "pkg:maven/org.apache.commons/commons-lang3@3.12.0";

  @Test
  public void testGetComponentVersions_extensionAndClassifier_foldedIntoPurlQualifiers() {
    GuideComponentVersionsRequest request = new GuideComponentVersionsRequest(
        QUALIFIED_MAVEN_PURL, "jar", "sources",
        null, null, null, null, null, null, null, null, null, null, null, null, null);

    Multimap<String, String> expectedParams = ArrayListMultimap.create();
    // Canonical qualifier order is alphabetical, so `classifier` precedes `type`.
    expectedParams.put("purl", QUALIFIED_MAVEN_PURL + "?classifier=sources&type=jar");

    underTest.getComponentVersions(request);

    verify(hdsClient).getWithMultimap(GuideComponentDetailSearchResponse.class,
        "rest/search/components/versions", expectedParams);
  }

  @Test
  public void testGetComponentVersions_nullExtensionAndClassifier_wireFormatUnchanged() {
    GuideComponentVersionsRequest request = new GuideComponentVersionsRequest(
        QUALIFIED_MAVEN_PURL, null, null,
        null, null, null, null, null, null, null, null, null, null, null, null, null);

    Multimap<String, String> expectedParams = ArrayListMultimap.create();
    expectedParams.put("purl", QUALIFIED_MAVEN_PURL);

    underTest.getComponentVersions(request);

    verify(hdsClient).getWithMultimap(GuideComponentDetailSearchResponse.class,
        "rest/search/components/versions", expectedParams);
  }

  @Test
  public void testGetComponentVersions_blankExtensionAndClassifier_wireFormatUnchanged() {
    GuideComponentVersionsRequest request = new GuideComponentVersionsRequest(
        QUALIFIED_MAVEN_PURL, "", "   ",
        null, null, null, null, null, null, null, null, null, null, null, null, null);

    Multimap<String, String> expectedParams = ArrayListMultimap.create();
    expectedParams.put("purl", QUALIFIED_MAVEN_PURL);

    underTest.getComponentVersions(request);

    verify(hdsClient).getWithMultimap(GuideComponentDetailSearchResponse.class,
        "rest/search/components/versions", expectedParams);
  }

  @Test
  public void testGetComponentVulnerabilities_extensionOnly_foldedIntoPurl() {
    GuideComponentVulnerabilitiesRequest request = new GuideComponentVulnerabilitiesRequest(
        QUALIFIED_MAVEN_PURL, "jar", null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null);

    Multimap<String, String> expectedParams = ArrayListMultimap.create();
    expectedParams.put("purl", QUALIFIED_MAVEN_PURL + "?type=jar");

    underTest.getComponentVulnerabilities(request);

    verify(hdsClient).getWithMultimap(GuideVulnerabilitySearchResponse.class,
        "rest/search/components/vulnerabilities", expectedParams);
  }

  @Test
  public void testGetComponentDependencies_classifierOnly_foldedIntoPurl() {
    GuideComponentDependenciesRequest request = new GuideComponentDependenciesRequest(
        QUALIFIED_MAVEN_PURL, null, "sources", null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null, null);

    Multimap<String, String> expectedParams = ArrayListMultimap.create();
    expectedParams.put("purl", QUALIFIED_MAVEN_PURL + "?classifier=sources");

    underTest.getComponentDependencies(request);

    verify(hdsClient).getWithMultimap(GuideComponentSearchResponse.class,
        "rest/search/components/dependencies", expectedParams);
  }

  @Test
  public void testGetComponentVersions_scopedNpmWithExtension_preservesPercentEncoding() {
    // Regression guard: extension folding must happen BEFORE the % → %25 pre-encoding.
    // Canonical scoped-npm PURL + type=tgz qualifier, then the pre-encoding runs.
    GuideComponentVersionsRequest request = new GuideComponentVersionsRequest(
        SCOPED_NPM_PURL, "tgz", null,
        null, null, null, null, null, null, null, null, null, null, null, null, null);

    Multimap<String, String> expectedParams = ArrayListMultimap.create();
    expectedParams.put("purl", SCOPED_NPM_PURL_WIRE + "?type=tgz");

    underTest.getComponentVersions(request);

    verify(hdsClient).getWithMultimap(GuideComponentDetailSearchResponse.class,
        "rest/search/components/versions", expectedParams);
  }

  @Test
  public void testGetLatestComponentVersion_delegatesToHdsPost() {
    when(hdsClient.post(String.class, "rest/search/components/latest-version", Map.of("purl", PURL)))
        .thenReturn("{\"latestVersion\":\"2.0.0\"}");

    String result = underTest.getLatestComponentVersion(PURL);

    assertThat(result).isEqualTo("{\"latestVersion\":\"2.0.0\"}");
    verify(hdsClient).post(String.class, "rest/search/components/latest-version", Map.of("purl", PURL));
  }

  @Test
  public void testGetRecommendations_noSelector_sendsPurlOnly() {
    GuideRecommendationResult expected = new GuideRecommendationResult(
        Outcome.FOUND_RECOMMENDATIONS,
        new RecommendedVersionInfo("1.0.0", null, Map.of(), Map.of(), Map.of(), List.of(), 80, null, null),
        List.of(new RecommendedVersionInfo("2.0.0", null, Map.of(), Map.of(), Map.of(), List.of(), 95, null, null)));
    when(hdsClient.post(GuideRecommendationResult.class, "rest/search/recommendations", Map.of("purl", PURL)))
        .thenReturn(expected);

    GuideRecommendationResult result = underTest.getRecommendations(PURL, null, null);

    assertThat(result).isSameAs(expected);
    verify(hdsClient).post(GuideRecommendationResult.class, "rest/search/recommendations", Map.of("purl", PURL));
  }

  @Test
  public void testGetRecommendations_withExtension_sendsBothFields() {
    GuideRecommendationResult expected = new GuideRecommendationResult(
        Outcome.FOUND_RECOMMENDATIONS,
        new RecommendedVersionInfo("1.0.0", null, Map.of(), Map.of(), Map.of(), List.of(), 80, null, null),
        List.of());
    Map<String, String> expectedBody = new LinkedHashMap<>();
    expectedBody.put("purl", PURL);
    expectedBody.put("extension", "war");
    when(hdsClient.post(GuideRecommendationResult.class, "rest/search/recommendations", expectedBody))
        .thenReturn(expected);

    GuideRecommendationResult result = underTest.getRecommendations(PURL, "war", null);

    assertThat(result).isSameAs(expected);
    verify(hdsClient).post(GuideRecommendationResult.class, "rest/search/recommendations", expectedBody);
  }

  @Test
  public void testGetRecommendations_withClassifier_sendsBothFields() {
    GuideRecommendationResult expected = new GuideRecommendationResult(
        Outcome.FOUND_RECOMMENDATIONS,
        new RecommendedVersionInfo("1.0.0", null, Map.of(), Map.of(), Map.of(), List.of(), 80, null, null),
        List.of());
    Map<String, String> expectedBody = new LinkedHashMap<>();
    expectedBody.put("purl", PURL);
    expectedBody.put("classifier", "sources");
    when(hdsClient.post(GuideRecommendationResult.class, "rest/search/recommendations", expectedBody))
        .thenReturn(expected);

    GuideRecommendationResult result = underTest.getRecommendations(PURL, null, "sources");

    assertThat(result).isSameAs(expected);
    verify(hdsClient).post(GuideRecommendationResult.class, "rest/search/recommendations", expectedBody);
  }

  @Test
  public void testGetRecommendations_withExtensionAndClassifier_sendsAllFields() {
    GuideRecommendationResult expected = new GuideRecommendationResult(
        Outcome.FOUND_RECOMMENDATIONS,
        new RecommendedVersionInfo("1.0.0", null, Map.of(), Map.of(), Map.of(), List.of(), 80, null, null),
        List.of());
    Map<String, String> expectedBody = new LinkedHashMap<>();
    expectedBody.put("purl", PURL);
    expectedBody.put("extension", "war");
    expectedBody.put("classifier", "sources");
    when(hdsClient.post(GuideRecommendationResult.class, "rest/search/recommendations", expectedBody))
        .thenReturn(expected);

    GuideRecommendationResult result = underTest.getRecommendations(PURL, "war", "sources");

    assertThat(result).isSameAs(expected);
    verify(hdsClient).post(GuideRecommendationResult.class, "rest/search/recommendations", expectedBody);
  }

  @Test
  public void testGetRecommendations_blankSelector_omittedFromBody() {
    GuideRecommendationResult expected = new GuideRecommendationResult(
        Outcome.FOUND_RECOMMENDATIONS,
        new RecommendedVersionInfo("1.0.0", null, Map.of(), Map.of(), Map.of(), List.of(), 80, null, null),
        List.of());
    // Blank extension/classifier should be treated as null -> omitted from body
    when(hdsClient.post(GuideRecommendationResult.class, "rest/search/recommendations", Map.of("purl", PURL)))
        .thenReturn(expected);

    GuideRecommendationResult result = underTest.getRecommendations(PURL, "  ", "");

    assertThat(result).isSameAs(expected);
    verify(hdsClient).post(GuideRecommendationResult.class, "rest/search/recommendations", Map.of("purl", PURL));
  }

  @Test
  public void testGetRecommendations_whitespaceSelector_trimmed() {
    GuideRecommendationResult expected = new GuideRecommendationResult(
        Outcome.FOUND_RECOMMENDATIONS,
        new RecommendedVersionInfo("1.0.0", null, Map.of(), Map.of(), Map.of(), List.of(), 80, null, null),
        List.of());
    Map<String, String> expectedBody = new LinkedHashMap<>();
    expectedBody.put("purl", PURL);
    expectedBody.put("extension", "war"); // trimmed
    expectedBody.put("classifier", "sources"); // trimmed
    when(hdsClient.post(GuideRecommendationResult.class, "rest/search/recommendations", expectedBody))
        .thenReturn(expected);

    GuideRecommendationResult result = underTest.getRecommendations(PURL, " war ", " sources ");

    assertThat(result).isSameAs(expected);
    verify(hdsClient).post(GuideRecommendationResult.class, "rest/search/recommendations", expectedBody);
  }

  @Test
  public void testGetRecommendations_throwsNotFound_propagatesUpstreamMessage() {
    when(hdsClient.post(GuideRecommendationResult.class, "rest/search/recommendations", Map.of("purl", PURL)))
        .thenThrow(new NotFoundException("Recommendations not found for PURL: " + PURL + " (no upgrade path)"));

    assertThatThrownBy(() -> underTest.getRecommendations(PURL, null, null))
        .isInstanceOf(GuideNotFoundException.class)
        .hasMessage("Recommendations not found for PURL: " + PURL + " (no upgrade path)");
  }

  @Test
  public void testGetRecommendations_throwsNotFound_fallsBackWhenUpstreamMessageBlank() {
    when(hdsClient.post(GuideRecommendationResult.class, "rest/search/recommendations", Map.of("purl", PURL)))
        .thenThrow(new NotFoundException(""));

    assertThatThrownBy(() -> underTest.getRecommendations(PURL, null, null))
        .isInstanceOf(GuideNotFoundException.class)
        .hasMessage("No recommendations found for purl: " + PURL);
  }

  @Test
  public void testGetRecommendations_throwsNotFound_fallsBackWhenUpstreamMessageIsReasonPhrase() {
    // HdsClient.getErrorMessage returns the bare HTTP reason phrase ("Not Found") when the
    // upstream response body is not text/plain — which is the actual production behavior for
    // the search-server's JSON 404s. Treat that as no useful upstream message.
    when(hdsClient.post(GuideRecommendationResult.class, "rest/search/recommendations", Map.of("purl", PURL)))
        .thenThrow(new NotFoundException("Not Found"));

    assertThatThrownBy(() -> underTest.getRecommendations(PURL, null, null))
        .isInstanceOf(GuideNotFoundException.class)
        .hasMessage("No recommendations found for purl: " + PURL);
  }

  @Test
  public void testGetRecommendations_throwsGuideApiExceptionOnBadGateway() {
    when(hdsClient.post(GuideRecommendationResult.class, "rest/search/recommendations", Map.of("purl", PURL)))
        .thenThrow(new BadGatewayException("upstream unavailable"));

    assertThatThrownBy(() -> underTest.getRecommendations(PURL, null, null))
        .isInstanceOf(GuideApiException.class)
        .hasMessageContaining("Failed to retrieve recommendations");
  }

  @Test
  public void testGetRecommendations_throwsGuideApiExceptionOnInternalServerError() {
    when(hdsClient.post(GuideRecommendationResult.class, "rest/search/recommendations", Map.of("purl", PURL)))
        .thenThrow(new InternalServerErrorException("hds error"));

    assertThatThrownBy(() -> underTest.getRecommendations(PURL, null, null))
        .isInstanceOf(GuideApiException.class)
        .hasMessageContaining("Failed to retrieve recommendations");
  }

  @Test
  public void testGetComponentByPurl_propagatesHdsExceptions() {
    when(hdsClient.get(String.class, "rest/search/components/detail", Map.of("purl", PURL)))
        .thenThrow(new RuntimeException("Connection refused"));

    assertThatThrownBy(() -> underTest.getComponentByPurl(PURL))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Connection refused");
  }

  @Test
  public void testSearchComponents_delegatesToHdsGetWithMultimap() {
    GuideComponentSearchRequest request = new GuideComponentSearchRequest(
        "log4j", 0, 10, "name", "asc",
        List.of("maven"), List.of("Logging"), List.of("critical"),
        7.0, 10.0, null, null,
        List.of("Apache"), List.of("Apache-2.0"),
        50, 100, "true", "30d", false, 5);

    GuideComponentSearchResponse expected = new GuideComponentSearchResponse(
        List.of(new GuideComponentDocument(
            "maven", null, "org.apache.logging.log4j", "log4j-core", "2.21.1",
            null, List.of(), List.of("Logging"), true, 99, 0.0, null, false, null, null)),
        1, 0, 10, null);

    Multimap<String, String> expectedParams = ArrayListMultimap.create();
    expectedParams.put("query", "log4j");
    expectedParams.put("offset", "0");
    expectedParams.put("limit", "10");
    expectedParams.put("sortField", "name");
    expectedParams.put("sortOrder", "asc");
    expectedParams.put("formats", "maven");
    expectedParams.put("categories", "Logging");
    expectedParams.put("severities", "critical");
    expectedParams.put("minCvss", "7.0");
    expectedParams.put("maxCvss", "10.0");
    expectedParams.put("licenseFamilies", "Apache");
    expectedParams.put("licenses", "Apache-2.0");
    expectedParams.put("minVersionScore", "50");
    expectedParams.put("maxVersionScore", "100");
    expectedParams.put("latestStable", "true");
    expectedParams.put("publishedWindow", "30d");
    expectedParams.put("hasMalware", "false");
    expectedParams.put("minDocCount", "5");

    when(hdsClient.getWithMultimap(GuideComponentSearchResponse.class, "rest/search/components",
        expectedParams)).thenReturn(expected);

    ApiSearchResponse<ComponentDocument> result = underTest.searchComponents(request);

    assertThat(result).isSameAs(expected);
  }

  @Test
  public void testSearchComponents_returnsEmptyOnNotFound() {
    GuideComponentSearchRequest request = new GuideComponentSearchRequest(
        "nonexistent", null, null, null, null,
        null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null);

    Multimap<String, String> expectedParams = ArrayListMultimap.create();
    expectedParams.put("query", "nonexistent");

    when(hdsClient.getWithMultimap(GuideComponentSearchResponse.class, "rest/search/components",
        expectedParams)).thenThrow(new NotFoundException("Not found"));

    ApiSearchResponse<ComponentDocument> result = underTest.searchComponents(request);

    assertThat(result.hits()).isEmpty();
    assertThat(result.total()).isEqualTo(0);
  }

  @Test
  public void testSearchComponents_throwsGuideApiExceptionOnBadGateway() {
    GuideComponentSearchRequest request = new GuideComponentSearchRequest(
        "test", null, null, null, null,
        null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null);

    Multimap<String, String> expectedParams = ArrayListMultimap.create();
    expectedParams.put("query", "test");

    when(hdsClient.getWithMultimap(GuideComponentSearchResponse.class, "rest/search/components",
        expectedParams)).thenThrow(new BadGatewayException("upstream unavailable"));

    assertThatThrownBy(() -> underTest.searchComponents(request))
        .isInstanceOf(GuideApiException.class)
        .hasMessageContaining("Failed to retrieve component search results");
  }

  @Test
  public void testSearchComponents_throwsGuideApiExceptionOnInternalServerError() {
    GuideComponentSearchRequest request = new GuideComponentSearchRequest(
        "test", null, null, null, null,
        null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null);

    Multimap<String, String> expectedParams = ArrayListMultimap.create();
    expectedParams.put("query", "test");

    when(hdsClient.getWithMultimap(GuideComponentSearchResponse.class, "rest/search/components",
        expectedParams)).thenThrow(new InternalServerErrorException("hds error"));

    assertThatThrownBy(() -> underTest.searchComponents(request))
        .isInstanceOf(GuideApiException.class)
        .hasMessageContaining("Failed to retrieve component search results");
  }

  @Test
  public void testSearchVulnerabilities_delegatesToHdsGetWithMultimap() {
    GuideVulnerabilitySearchRequest request = new GuideVulnerabilitySearchRequest(
        "log4j", 0, 10, "cvssSeverity", "desc",
        List.of("critical", "high"), 7.0, 10.0, null, null,
        null, null, null, null, null, null, null);

    GuideVulnerabilitySearchResponse expected = new GuideVulnerabilitySearchResponse(
        List.of(new GuideVulnerabilityDocument(
            "CVE-2021-44228", List.of("GHSA-jfh8-c2jp-5v3q"), "Log4Shell RCE",
            10.0, 10.0, List.of("CWE-502"), List.of(), List.of("maven"),
            false, true, 0.975, "NVD", null, null)),
        1, 0, 10, null);

    Multimap<String, String> expectedParams = ArrayListMultimap.create();
    expectedParams.put("query", "log4j");
    expectedParams.put("offset", "0");
    expectedParams.put("limit", "10");
    expectedParams.put("sortField", "cvssSeverity");
    expectedParams.put("sortOrder", "desc");
    expectedParams.put("severities", "critical");
    expectedParams.put("severities", "high");
    expectedParams.put("minCvss", "7.0");
    expectedParams.put("maxCvss", "10.0");

    when(hdsClient.getWithMultimap(GuideVulnerabilitySearchResponse.class, "rest/search/vulnerabilities",
        expectedParams)).thenReturn(expected);

    ApiSearchResponse<VulnerabilityDocument> result = underTest.searchVulnerabilities(request);

    assertThat(result).isSameAs(expected);
  }

  @Test
  public void testSearchVulnerabilities_returnsEmptyOnNotFound() {
    GuideVulnerabilitySearchRequest request = new GuideVulnerabilitySearchRequest(
        "nonexistent", null, null, null, null,
        null, null, null, null, null,
        null, null, null, null, null, null, null);

    Multimap<String, String> expectedParams = ArrayListMultimap.create();
    expectedParams.put("query", "nonexistent");

    when(hdsClient.getWithMultimap(GuideVulnerabilitySearchResponse.class, "rest/search/vulnerabilities",
        expectedParams)).thenThrow(new NotFoundException("Not found"));

    ApiSearchResponse<VulnerabilityDocument> result = underTest.searchVulnerabilities(request);

    assertThat(result.hits()).isEmpty();
    assertThat(result.total()).isEqualTo(0);
  }

  @Test
  public void testSearchVulnerabilities_throwsGuideApiExceptionOnBadGateway() {
    GuideVulnerabilitySearchRequest request = new GuideVulnerabilitySearchRequest(
        "test", null, null, null, null,
        null, null, null, null, null,
        null, null, null, null, null, null, null);

    Multimap<String, String> expectedParams = ArrayListMultimap.create();
    expectedParams.put("query", "test");

    when(hdsClient.getWithMultimap(GuideVulnerabilitySearchResponse.class, "rest/search/vulnerabilities",
        expectedParams)).thenThrow(new BadGatewayException("upstream unavailable"));

    assertThatThrownBy(() -> underTest.searchVulnerabilities(request))
        .isInstanceOf(GuideApiException.class)
        .hasMessageContaining("Failed to retrieve vulnerability search results");
  }

  @Test
  public void testSearchVulnerabilities_throwsGuideApiExceptionOnInternalServerError() {
    GuideVulnerabilitySearchRequest request = new GuideVulnerabilitySearchRequest(
        "test", null, null, null, null,
        null, null, null, null, null,
        null, null, null, null, null, null, null);

    Multimap<String, String> expectedParams = ArrayListMultimap.create();
    expectedParams.put("query", "test");

    when(hdsClient.getWithMultimap(GuideVulnerabilitySearchResponse.class, "rest/search/vulnerabilities",
        expectedParams)).thenThrow(new InternalServerErrorException("hds error"));

    assertThatThrownBy(() -> underTest.searchVulnerabilities(request))
        .isInstanceOf(GuideApiException.class)
        .hasMessageContaining("Failed to retrieve vulnerability search results");
  }

  @Test
  public void testGlobalSearch_delegatesToHdsGetWithMultimap() {
    GuideGlobalSearchRequest request = new GuideGlobalSearchRequest(
        "log4j", 0, 10, "name", "asc", "true", List.of("maven", "npm"), "30d");

    GuideGlobalSearchResponse expected = new GuideGlobalSearchResponse(
        List.of(new GuideComponentDocument(
            "maven", null, "org.apache.logging.log4j", "log4j-core", "2.21.1",
            null, List.of(), List.of("library"), true, 99, 0.0, null, false, null, null)),
        1, 0, 10, null);

    Multimap<String, String> expectedParams = ArrayListMultimap.create();
    expectedParams.put("query", "log4j");
    expectedParams.put("offset", "0");
    expectedParams.put("limit", "10");
    expectedParams.put("sortField", "name");
    expectedParams.put("sortOrder", "asc");
    expectedParams.put("latestStable", "true");
    expectedParams.put("formats", "maven");
    expectedParams.put("formats", "npm");
    expectedParams.put("publishedWindow", "30d");

    when(hdsClient.getWithMultimap(GuideGlobalSearchResponse.class, "rest/search/global",
        expectedParams)).thenReturn(expected);

    ApiSearchResponse<SearchResult> result = underTest.globalSearch(request);

    assertThat(result).isSameAs(expected);
    verify(hdsClient).getWithMultimap(GuideGlobalSearchResponse.class, "rest/search/global", expectedParams);
  }

  @Test
  public void testGlobalSearch_returnsEmptyOnNotFound() {
    GuideGlobalSearchRequest request = new GuideGlobalSearchRequest(
        "nonexistent", 5, 25, null, null, null, null, null);

    Multimap<String, String> expectedParams = ArrayListMultimap.create();
    expectedParams.put("query", "nonexistent");
    expectedParams.put("offset", "5");
    expectedParams.put("limit", "25");

    when(hdsClient.getWithMultimap(GuideGlobalSearchResponse.class, "rest/search/global",
        expectedParams)).thenThrow(new NotFoundException("Not found"));

    ApiSearchResponse<SearchResult> result = underTest.globalSearch(request);

    assertThat(result.hits()).isEmpty();
    assertThat(result.total()).isEqualTo(0);
    assertThat(result.offset()).isEqualTo(5);
    assertThat(result.limit()).isEqualTo(25);
  }

  @Test
  public void testGlobalSearch_throwsGuideApiExceptionOnBadGateway() {
    GuideGlobalSearchRequest request = new GuideGlobalSearchRequest(
        "test", null, null, null, null, null, null, null);

    Multimap<String, String> expectedParams = ArrayListMultimap.create();
    expectedParams.put("query", "test");

    when(hdsClient.getWithMultimap(GuideGlobalSearchResponse.class, "rest/search/global",
        expectedParams)).thenThrow(new BadGatewayException("upstream unavailable"));

    assertThatThrownBy(() -> underTest.globalSearch(request))
        .isInstanceOf(GuideApiException.class)
        .hasMessageContaining("Failed to retrieve global search results");
  }

  @Test
  public void testGlobalSearch_throwsGuideApiExceptionOnInternalServerError() {
    GuideGlobalSearchRequest request = new GuideGlobalSearchRequest(
        "test", null, null, null, null, null, null, null);

    Multimap<String, String> expectedParams = ArrayListMultimap.create();
    expectedParams.put("query", "test");

    when(hdsClient.getWithMultimap(GuideGlobalSearchResponse.class, "rest/search/global",
        expectedParams)).thenThrow(new InternalServerErrorException("hds error"));

    assertThatThrownBy(() -> underTest.globalSearch(request))
        .isInstanceOf(GuideApiException.class)
        .hasMessageContaining("Failed to retrieve global search results");
  }

  @Test
  public void testGetComponentVulnerabilities_delegatesToHdsGetWithMultimap() {
    GuideComponentVulnerabilitiesRequest request = new GuideComponentVulnerabilitiesRequest(
        PURL, null, null, "maven", "org.example", "lib", "1.0.0",
        0, 10, "cvssSeverity", "desc",
        List.of("critical"), 7.0, 10.0, null, null, null, null, null, null, null);

    GuideVulnerabilitySearchResponse expected = new GuideVulnerabilitySearchResponse(
        List.of(new GuideVulnerabilityDocument(
            "CVE-2021-44228", List.of("GHSA-jfh8-c2jp-5v3q"), "Log4Shell RCE",
            10.0, 10.0, List.of("CWE-502"), List.of(), List.of("maven"),
            false, true, 0.975, "NVD", null, null)),
        1, 0, 10, null);

    Multimap<String, String> expectedParams = ArrayListMultimap.create();
    expectedParams.put("purl", PURL);
    expectedParams.put("format", "maven");
    expectedParams.put("namespace", "org.example");
    expectedParams.put("name", "lib");
    expectedParams.put("version", "1.0.0");
    expectedParams.put("offset", "0");
    expectedParams.put("limit", "10");
    expectedParams.put("sortField", "cvssSeverity");
    expectedParams.put("sortOrder", "desc");
    expectedParams.put("severities", "critical");
    expectedParams.put("minCvss", "7.0");
    expectedParams.put("maxCvss", "10.0");

    when(hdsClient.getWithMultimap(GuideVulnerabilitySearchResponse.class,
        "rest/search/components/vulnerabilities", expectedParams)).thenReturn(expected);

    ApiSearchResponse<VulnerabilityDocument> result = underTest.getComponentVulnerabilities(request);

    assertThat(result).isSameAs(expected);
    verify(hdsClient).getWithMultimap(GuideVulnerabilitySearchResponse.class,
        "rest/search/components/vulnerabilities", expectedParams);
  }

  @Test
  public void testGetComponentVulnerabilities_returnsEmptyOnNotFound() {
    GuideComponentVulnerabilitiesRequest request = new GuideComponentVulnerabilitiesRequest(
        PURL, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, null);

    Multimap<String, String> expectedParams = ArrayListMultimap.create();
    expectedParams.put("purl", PURL);

    when(hdsClient.getWithMultimap(GuideVulnerabilitySearchResponse.class,
        "rest/search/components/vulnerabilities", expectedParams))
            .thenThrow(new NotFoundException("Not found"));

    ApiSearchResponse<VulnerabilityDocument> result = underTest.getComponentVulnerabilities(request);

    assertThat(result.hits()).isEmpty();
    assertThat(result.total()).isEqualTo(0);
  }

  @Test
  public void testGetComponentVulnerabilities_throwsGuideApiExceptionOnBadGateway() {
    GuideComponentVulnerabilitiesRequest request = new GuideComponentVulnerabilitiesRequest(
        PURL, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, null);

    Multimap<String, String> expectedParams = ArrayListMultimap.create();
    expectedParams.put("purl", PURL);

    when(hdsClient.getWithMultimap(GuideVulnerabilitySearchResponse.class,
        "rest/search/components/vulnerabilities", expectedParams))
            .thenThrow(new BadGatewayException("upstream unavailable"));

    assertThatThrownBy(() -> underTest.getComponentVulnerabilities(request))
        .isInstanceOf(GuideApiException.class)
        .hasMessageContaining("Failed to retrieve component vulnerabilities");
  }

  @Test
  public void testGetComponentDependencies_delegatesToHdsGetWithMultimap() {
    GuideComponentDependenciesRequest request = new GuideComponentDependenciesRequest(
        PURL, null, null, "maven", "org.example", "lib", "1.0.0",
        "log4j", 0, 10, "name", "asc",
        List.of("maven"), null, null, null, null, null, null, null, null, null);

    GuideComponentSearchResponse expected = new GuideComponentSearchResponse(
        List.of(new GuideComponentDocument(
            "maven", null, "org.apache.logging.log4j", "log4j-core", "2.21.1",
            null, List.of(), List.of("Logging"), true, 99, 0.0, null, false, null, null)),
        1, 0, 10, null);

    Multimap<String, String> expectedParams = ArrayListMultimap.create();
    expectedParams.put("purl", PURL);
    expectedParams.put("format", "maven");
    expectedParams.put("namespace", "org.example");
    expectedParams.put("name", "lib");
    expectedParams.put("version", "1.0.0");
    expectedParams.put("query", "log4j");
    expectedParams.put("offset", "0");
    expectedParams.put("limit", "10");
    expectedParams.put("sortField", "name");
    expectedParams.put("sortOrder", "asc");
    expectedParams.put("formats", "maven");

    when(hdsClient.getWithMultimap(GuideComponentSearchResponse.class,
        "rest/search/components/dependencies", expectedParams)).thenReturn(expected);

    ApiSearchResponse<ComponentDocument> result = underTest.getComponentDependencies(request);

    assertThat(result).isSameAs(expected);
    verify(hdsClient).getWithMultimap(GuideComponentSearchResponse.class,
        "rest/search/components/dependencies", expectedParams);
  }

  @Test
  public void testGetComponentDependencies_returnsEmptyOnNotFound() {
    GuideComponentDependenciesRequest request = new GuideComponentDependenciesRequest(
        PURL, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null);

    Multimap<String, String> expectedParams = ArrayListMultimap.create();
    expectedParams.put("purl", PURL);

    when(hdsClient.getWithMultimap(GuideComponentSearchResponse.class,
        "rest/search/components/dependencies", expectedParams))
            .thenThrow(new NotFoundException("Not found"));

    ApiSearchResponse<ComponentDocument> result = underTest.getComponentDependencies(request);

    assertThat(result.hits()).isEmpty();
    assertThat(result.total()).isEqualTo(0);
  }

  @Test
  public void testGetComponentDependencies_throwsGuideApiExceptionOnBadGateway() {
    GuideComponentDependenciesRequest request = new GuideComponentDependenciesRequest(
        PURL, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null);

    Multimap<String, String> expectedParams = ArrayListMultimap.create();
    expectedParams.put("purl", PURL);

    when(hdsClient.getWithMultimap(GuideComponentSearchResponse.class,
        "rest/search/components/dependencies", expectedParams))
            .thenThrow(new BadGatewayException("upstream unavailable"));

    assertThatThrownBy(() -> underTest.getComponentDependencies(request))
        .isInstanceOf(GuideApiException.class)
        .hasMessageContaining("Failed to retrieve component dependencies");
  }

  @Test
  public void getComponentByPurl_paymentRequired_triggersHandlerAndThrowsLicenseUnavailable() {
    when(hdsClient.get(String.class, "rest/search/components/detail", Map.of("purl", PURL)))
        .thenThrow(new PaymentRequiredException("HDS gated"));

    assertThatThrownBy(() -> underTest.getComponentByPurl(PURL))
        .isInstanceOfSatisfying(GuideLicenseUnavailableException.class,
            e -> assertThat(e.getResponse().getStatus()).isEqualTo(402));

    verify(revocationHandler, times(1)).onPaymentRequired("rest/search/components/detail");
  }

  @Test
  public void getLatestComponentVersion_paymentRequired_triggersHandlerAndThrowsLicenseUnavailable() {
    when(hdsClient.post(String.class, "rest/search/components/latest-version", Map.of("purl", PURL)))
        .thenThrow(new PaymentRequiredException("HDS gated"));

    assertThatThrownBy(() -> underTest.getLatestComponentVersion(PURL))
        .isInstanceOfSatisfying(GuideLicenseUnavailableException.class,
            e -> assertThat(e.getResponse().getStatus()).isEqualTo(402));

    verify(revocationHandler, times(1)).onPaymentRequired("rest/search/components/latest-version");
  }

  @Test
  public void badGateway_doesNotInvokeRevocationHandler() {
    when(hdsClient.get(String.class, "rest/search/components/detail", Map.of("purl", PURL)))
        .thenThrow(new BadGatewayException("upstream"));

    assertThatThrownBy(() -> underTest.getComponentByPurl(PURL))
        .isInstanceOf(BadGatewayException.class);

    verify(revocationHandler, never()).onPaymentRequired(anyString());
  }

  @Test
  public void paymentRequired_refreshHandlerThrows_stillThrowsLicenseUnavailable() {
    // Refresh failures must not mask the deterministic 402 marker response — HDS already told
    // us the license no longer grants this feature, so the client must see 402 +
    // X-Sonatype-Guide-License: unavailable regardless of whether the in-process refresh
    // attempt succeeded. Covers the originating thread's RuntimeException from loadLicense()
    // and concurrent threads' CompletionException from CompletableFuture.join().
    when(hdsClient.get(String.class, "rest/search/components/detail", Map.of("purl", PURL)))
        .thenThrow(new PaymentRequiredException("HDS gated"));
    doThrow(new RuntimeException("HDS unreachable while refreshing license"))
        .when(revocationHandler)
        .onPaymentRequired("rest/search/components/detail");

    assertThatThrownBy(() -> underTest.getComponentByPurl(PURL))
        .isInstanceOfSatisfying(GuideLicenseUnavailableException.class,
            e -> assertThat(e.getResponse().getStatus()).isEqualTo(402));
  }

  @Test
  public void paymentRequired_concurrentJoinFailure_stillThrowsLicenseUnavailable() {
    // CompletableFuture.join() wraps the original cause in CompletionException, which Thread B
    // sees when Thread A's loadLicense() throws. withLicenseRefreshOn402 must catch this too.
    when(hdsClient.get(String.class, "rest/search/components/detail", Map.of("purl", PURL)))
        .thenThrow(new PaymentRequiredException("HDS gated"));
    doThrow(new CompletionException(new RuntimeException("Thread A's loadLicense failed")))
        .when(revocationHandler)
        .onPaymentRequired("rest/search/components/detail");

    assertThatThrownBy(() -> underTest.getComponentByPurl(PURL))
        .isInstanceOfSatisfying(GuideLicenseUnavailableException.class,
            e -> assertThat(e.getResponse().getStatus()).isEqualTo(402));
  }
}
