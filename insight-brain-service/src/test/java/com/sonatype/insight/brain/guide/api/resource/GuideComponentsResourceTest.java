/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.resource;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.guide.api.dto.ApiSearchResponse;
import com.sonatype.guide.api.dto.ComponentDetailDocument;
import com.sonatype.guide.api.dto.ComponentDocument;
import com.sonatype.guide.api.request.LatestVersionRequest;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDependenciesRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDetailDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDetailSearchResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentSearchResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentVersionsRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentVulnerabilitiesRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilitySearchResponse;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyCompliance;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyComplianceLevel;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyComplianceSummary;
import com.sonatype.insight.brain.guide.api.error.GuideApiException;
import com.sonatype.insight.brain.guide.core.SearchApiClient;
import com.sonatype.insight.brain.guide.policy.GuidePolicyEvaluator;
import com.sonatype.insight.brain.guide.policy.GuidePolicyService;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.PermissionService;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GuideComponentsResourceTest
{
  @Mock
  private SearchApiClient searchApiClient;

  @Mock
  private GuidePolicyEvaluator guidePolicyEvaluator;

  @Mock
  private ApplicationDAO applicationDAO;

  @Mock
  private OwnerDAO ownerDAO;

  @Mock
  private PermissionService permissionService;

  private GuideComponentsResource underTest;

  @BeforeEach
  public void setUp() {
    // The /detail and /latest-version surfaces gate the full card on EVALUATE_COMPONENT, which calls
    // SecurityUtils.getSubject(); bind a subject and grant the permission so those tests see the card.
    SecurityManager securityManager = mock(SecurityManager.class);
    ThreadContext.bind(securityManager);
    ThreadContext.bind(new Subject.Builder(securityManager).buildSubject());
    lenient().when(permissionService.validatePermission(any(), any(), any(), any()))
        .thenReturn(EnumSet.of(Permission.EVALUATE_COMPONENT));
    underTest = new GuideComponentsResource(
        searchApiClient,
        new GuidePolicyService(guidePolicyEvaluator, applicationDAO, ownerDAO, permissionService));
  }

  @AfterEach
  public void tearDown() {
    ThreadContext.unbindSubject();
    ThreadContext.unbindSecurityManager();
  }

  @Test
  public void getComponentDetail_byCoords_missingVersion_returns400() {
    assertThatThrownBy(
        () -> underTest.getComponentDetail(
            null, "maven", "org.apache.logging.log4j", "log4j-core", null, null, null, null, null))
                .isInstanceOf(GuideApiException.class)
                .hasMessageContaining("'format', 'name', 'version'")
                .extracting(e -> ((GuideApiException) e).getResponse().getStatus())
                .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    verifyNoInteractions(searchApiClient);
  }

  @Test
  public void getComponentDetail_byCoords_blankVersion_returns400() {
    assertThatThrownBy(
        () -> underTest.getComponentDetail(
            null, "maven", "org.apache.logging.log4j", "log4j-core", "  ", null, null, null, null))
                .isInstanceOf(GuideApiException.class)
                .hasMessageContaining("'format', 'name', 'version'");
    verifyNoInteractions(searchApiClient);
  }

  @Test
  public void getComponentDetail_byCoords_missingFormat_returns400() {
    assertThatThrownBy(
        () -> underTest.getComponentDetail(null, null, null, "log4j-core", "2.14.0", null, null, null, null))
            .isInstanceOf(GuideApiException.class)
            .hasMessageContaining("'format', 'name', 'version'");
    verifyNoInteractions(searchApiClient);
  }

  @Test
  public void getComponentDetail_byCoords_missingName_returns400() {
    assertThatThrownBy(() -> underTest.getComponentDetail(null, "maven", null, null, "2.14.0", null, null, null, null))
        .isInstanceOf(GuideApiException.class)
        .hasMessageContaining("'format', 'name', 'version'");
    verifyNoInteractions(searchApiClient);
  }

  @Test
  public void getComponentDetail_noPurlAndNoCoords_returns400() {
    assertThatThrownBy(() -> underTest.getComponentDetail(null, null, null, null, null, null, null, null, null))
        .isInstanceOf(GuideApiException.class)
        .hasMessageContaining("'format', 'name', 'version'");
    verifyNoInteractions(searchApiClient);
  }

  // The same buildPurl helper is used by /versions, /vulnerabilities, /dependencies; covering
  // /detail here is sufficient to lock in the validation contract for all four endpoints.

  @Test
  public void getComponentDetail_byPurl_invalidPurl_returns400() {
    assertThatThrownBy(() -> underTest.getComponentDetail("not-a-purl", null, null, null, null, null, null, null, null))
        .isInstanceOf(GuideApiException.class)
        .hasMessageStartingWith("Invalid PURL format: ")
        .extracting(e -> ((GuideApiException) e).getResponse().getStatus())
        .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    verifyNoInteractions(searchApiClient);
  }

  @Test
  public void getLatestVersion_invalidPurl_returns400() {
    var request = new LatestVersionRequest("not-a-purl");
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
    when(searchApiClient.getComponentDetailByPurl(any())).thenReturn(
        new GuideComponentDetailDocument(
            "npm", null, null, "@types/node", "25.9.2", null, null, null,
            null, null, null, null, null, null, null, null, null));
    when(guidePolicyEvaluator.evaluate(any(List.class))).thenReturn(Map.of());

    underTest.getComponentDetail(null, "npm", null, "@types/node", "25.9.2", null, null, null, null);

    ArgumentCaptor<String> purl = ArgumentCaptor.forClass(String.class);
    verify(searchApiClient).getComponentDetailByPurl(purl.capture());
    assertThat(purl.getValue()).isEqualTo("pkg:npm/%40types%2Fnode@25.9.2");
  }

  @Test
  public void searchComponents_attachesPolicyComplianceWhenEvaluatorReturnsIt() throws Exception {
    GuideComponentDocument hit = new GuideComponentDocument(
        "maven", null, "org.example", "lib", "1.0", null,
        null, null, null, null, null, null, null, null, null);
    GuideComponentSearchResponse upstream = new GuideComponentSearchResponse(
        List.of(hit), 1L, 0, 20, Map.of());
    when(searchApiClient.searchComponents(any())).thenReturn(upstream);
    GuidePolicyCompliance compliance = compliantOf();
    when(guidePolicyEvaluator.evaluate(List.of("pkg:maven/org.example/lib@1.0?type=jar")))
        .thenReturn(Map.of("pkg:maven/org.example/lib@1.0?type=jar", compliance));

    ApiSearchResponse<ComponentDocument> result = underTest.searchComponents(
        null, 0, 20, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null);

    assertThat(result.hits()).hasSize(1);
    GuideComponentDocument enriched = (GuideComponentDocument) result.hits().get(0);
    // List endpoint: only the compliant flag is attached, not the full shape.
    assertSlim(enriched.policyCompliance(), true);
  }

  @Test
  public void searchComponents_softFailsWhenEvaluatorReturnsEmpty() throws Exception {
    GuideComponentDocument hit = new GuideComponentDocument(
        "maven", null, "org.example", "lib", "1.0", null,
        null, null, null, null, null, null, null, null, null);
    GuideComponentSearchResponse upstream = new GuideComponentSearchResponse(
        List.of(hit), 1L, 0, 20, Map.of());
    when(searchApiClient.searchComponents(any())).thenReturn(upstream);
    when(guidePolicyEvaluator.evaluate(any(List.class))).thenReturn(Map.of());

    ApiSearchResponse<ComponentDocument> result = underTest.searchComponents(
        null, 0, 20, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null);

    GuideComponentDocument enriched = (GuideComponentDocument) result.hits().get(0);
    assertThat(enriched.policyCompliance()).isNull();
  }

  @Test
  public void getComponentDetail_byPurl_attachesPolicyCompliance() throws Exception {
    GuideComponentDetailDocument upstream = new GuideComponentDetailDocument(
        "maven", null, "org.example", "lib", "1.0", null, null, null,
        null, null, null, null, null, null, null, null, null);
    when(searchApiClient.getComponentDetailByPurl("pkg:maven/org.example/lib@1.0")).thenReturn(upstream);
    GuidePolicyCompliance compliance = compliantOf();
    when(guidePolicyEvaluator.evaluate(List.of("pkg:maven/org.example/lib@1.0?type=jar")))
        .thenReturn(Map.of("pkg:maven/org.example/lib@1.0?type=jar", compliance));

    ComponentDetailDocument result = underTest.getComponentDetail(
        "pkg:maven/org.example/lib@1.0", null, null, null, null, null, null, null, null);

    GuideComponentDetailDocument enriched = (GuideComponentDetailDocument) result;
    assertThat(enriched.policyCompliance()).isSameAs(compliance);
  }

  @Test
  public void getComponentVersions_byPurl_attachesPolicyCompliance() throws Exception {
    GuideComponentDetailDocument hit = new GuideComponentDetailDocument(
        "maven", null, "org.example", "lib", "1.0", null, null, null,
        null, null, null, null, null, null, null, null, null);
    GuideComponentDetailSearchResponse upstream = new GuideComponentDetailSearchResponse(
        List.of(hit), 1L, 0, 20, Map.of());
    when(searchApiClient.getComponentVersions(any())).thenReturn(upstream);
    GuidePolicyCompliance compliance = compliantOf();
    when(guidePolicyEvaluator.evaluate(List.of("pkg:maven/org.example/lib@1.0?type=jar")))
        .thenReturn(Map.of("pkg:maven/org.example/lib@1.0?type=jar", compliance));

    ApiSearchResponse<ComponentDetailDocument> result = underTest.getComponentVersions(
        "pkg:maven/org.example/lib@1.0", null, null, null, null, 0, 20, null, null,
        null, null, null, null, null, null, null, null, null, null, null, null, null);

    GuideComponentDetailDocument enriched = (GuideComponentDetailDocument) result.hits().get(0);
    // /versions is a list endpoint (same DTO as /detail, but list context) → slim.
    assertSlim(enriched.policyCompliance(), true);
  }

  @Test
  public void getComponentDependencies_byPurl_attachesPolicyCompliance() throws Exception {
    GuideComponentDocument hit = new GuideComponentDocument(
        "maven", null, "org.example", "dep", "2.0", null,
        null, null, null, null, null, null, null, null, null);
    GuideComponentSearchResponse upstream = new GuideComponentSearchResponse(
        List.of(hit), 1L, 0, 20, Map.of());
    when(searchApiClient.getComponentDependencies(any())).thenReturn(upstream);
    GuidePolicyCompliance compliance = compliantOf();
    when(guidePolicyEvaluator.evaluate(List.of("pkg:maven/org.example/dep@2.0?type=jar")))
        .thenReturn(Map.of("pkg:maven/org.example/dep@2.0?type=jar", compliance));

    ApiSearchResponse<ComponentDocument> result = underTest.getComponentDependencies(
        "pkg:maven/org.example/lib@1.0", null, null, null, null,
        null, 0, 20, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

    GuideComponentDocument enriched = (GuideComponentDocument) result.hits().get(0);
    // /dependencies is a list endpoint → slim.
    assertSlim(enriched.policyCompliance(), true);
  }

  @Test
  public void getLatestVersion_byPurl_attachesPolicyCompliance() throws Exception {
    GuideComponentDetailDocument upstream = new GuideComponentDetailDocument(
        "maven", null, "org.example", "lib", "2.0", null, null, null,
        null, null, null, null, null, null, null, null, null);
    when(searchApiClient.getLatestVersionDetail("pkg:maven/org.example/lib@1.0")).thenReturn(upstream);
    GuidePolicyCompliance compliance = compliantOf();
    when(guidePolicyEvaluator.evaluate(List.of("pkg:maven/org.example/lib@2.0?type=jar")))
        .thenReturn(Map.of("pkg:maven/org.example/lib@2.0?type=jar", compliance));

    ComponentDetailDocument result = underTest.getLatestVersion(
        new LatestVersionRequest("pkg:maven/org.example/lib@1.0"));

    GuideComponentDetailDocument enriched = (GuideComponentDetailDocument) result;
    assertThat(enriched.policyCompliance()).isSameAs(compliance);
  }

  @Test
  public void getComponentDetail_byCoords_forwardsExtensionAndClassifierAsPurlQualifiers() throws Exception {
    when(searchApiClient.getComponentDetailByPurl(any())).thenReturn(
        new GuideComponentDetailDocument(
            "maven", null, "org.apache.commons", "commons-lang3", "3.12.0", null, null, null,
            null, null, null, null, null, null, null, null, null));
    when(guidePolicyEvaluator.evaluate(any(List.class))).thenReturn(Map.of());

    underTest.getComponentDetail(
        null, "maven", "org.apache.commons", "commons-lang3", "3.12.0",
        null, null, "jar", "sources");

    ArgumentCaptor<String> purlCaptor = ArgumentCaptor.forClass(String.class);
    verify(searchApiClient).getComponentDetailByPurl(purlCaptor.capture());
    // Canonical qualifier order is alphabetical, so `classifier` precedes `type`.
    assertThat(purlCaptor.getValue())
        .isEqualTo("pkg:maven/org.apache.commons/commons-lang3@3.12.0?classifier=sources&type=jar");
  }

  @Test
  public void getComponentDetail_byPurl_forwardsExtensionAndClassifierAsPurlQualifiers() throws Exception {
    when(searchApiClient.getComponentDetailByPurl(any())).thenReturn(
        new GuideComponentDetailDocument(
            "maven", null, "org.apache.commons", "commons-lang3", "3.12.0", null, null, null,
            null, null, null, null, null, null, null, null, null));

    // When purl is provided directly, qualifiers are applied to that purl rather than building from coords.
    underTest.getComponentDetail(
        "pkg:maven/org.apache.commons/commons-lang3@3.12.0", null, null, null, null,
        null, null, "jar", "sources");

    ArgumentCaptor<String> purlCaptor = ArgumentCaptor.forClass(String.class);
    verify(searchApiClient).getComponentDetailByPurl(purlCaptor.capture());
    assertThat(purlCaptor.getValue())
        .isEqualTo("pkg:maven/org.apache.commons/commons-lang3@3.12.0?classifier=sources&type=jar");
  }

  @Test
  public void getComponentVersions_byCoords_forwardsExtensionAndClassifier() throws Exception {
    when(searchApiClient.getComponentVersions(any()))
        .thenReturn(new GuideComponentDetailSearchResponse(List.of(), 0L, 0, 20, Map.of()));

    ArgumentCaptor<GuideComponentVersionsRequest> captor =
        ArgumentCaptor.forClass(GuideComponentVersionsRequest.class);

    underTest.getComponentVersions(
        null, "maven", "org.apache.commons", "commons-lang3", "3.12.0",
        null, null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, "jar", "sources");

    verify(searchApiClient).getComponentVersions(captor.capture());
    assertThat(captor.getValue().extension()).isEqualTo("jar");
    assertThat(captor.getValue().classifier()).isEqualTo("sources");
  }

  @Test
  public void getComponentVulnerabilities_byCoords_forwardsExtensionAndClassifier() throws Exception {
    when(searchApiClient.getComponentVulnerabilities(any()))
        .thenReturn(new GuideVulnerabilitySearchResponse(List.of(), 0L, 0, 20, Map.of()));

    ArgumentCaptor<GuideComponentVulnerabilitiesRequest> captor =
        ArgumentCaptor.forClass(GuideComponentVulnerabilitiesRequest.class);

    underTest.getComponentVulnerabilities(
        null, "maven", "org.apache.commons", "commons-lang3", "3.12.0",
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
        "jar", "sources");

    verify(searchApiClient).getComponentVulnerabilities(captor.capture());
    assertThat(captor.getValue().extension()).isEqualTo("jar");
    assertThat(captor.getValue().classifier()).isEqualTo("sources");
  }

  @Test
  public void getComponentDependencies_byCoords_forwardsExtensionAndClassifier() throws Exception {
    when(searchApiClient.getComponentDependencies(any()))
        .thenReturn(new GuideComponentSearchResponse(List.of(), 0L, 0, 20, Map.of()));

    ArgumentCaptor<GuideComponentDependenciesRequest> captor =
        ArgumentCaptor.forClass(GuideComponentDependenciesRequest.class);

    underTest.getComponentDependencies(
        null, "maven", "org.apache.commons", "commons-lang3", "3.12.0",
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, "jar", "sources");

    verify(searchApiClient).getComponentDependencies(captor.capture());
    assertThat(captor.getValue().extension()).isEqualTo("jar");
    assertThat(captor.getValue().classifier()).isEqualTo("sources");
  }

  private static GuidePolicyCompliance compliantOf() {
    Map<String, Integer> counts = new LinkedHashMap<>();
    counts.put("SECURITY", 0);
    counts.put("LICENSE", 0);
    counts.put("QUALITY", 0);
    counts.put("OTHER", 0);
    return new GuidePolicyCompliance(true, GuidePolicyComplianceLevel.PASS, "release", "ROOT_ORGANIZATION_ID",
        new GuidePolicyComplianceSummary(0, "none", 0, 0, counts), List.of());
  }

  private static void assertSlim(GuidePolicyCompliance pc, boolean expectedCompliant) {
    assertThat(pc).isNotNull();
    assertThat(pc.compliant()).isEqualTo(expectedCompliant);
    assertThat(pc.stage()).isNull();
    assertThat(pc.ownerId()).isNull();
    assertThat(pc.summary()).isNull();
    assertThat(pc.violations()).isNull();
  }
}
