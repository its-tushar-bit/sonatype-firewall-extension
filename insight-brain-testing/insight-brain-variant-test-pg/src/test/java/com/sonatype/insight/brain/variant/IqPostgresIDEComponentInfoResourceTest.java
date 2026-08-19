/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.insight.brain.hds.TestComponentDetailsList;
import com.sonatype.insight.brain.hds.TestNamedComponentDetails;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.hds.ComponentDetailsDTO;
import com.sonatype.insight.brain.hds.ComponentVersionInfoDTO;
import com.sonatype.insight.brain.hds.IdeComponentDetailsHdsClient;
import com.sonatype.insight.brain.hds.VersionScoringService;
import com.sonatype.insight.brain.ide.IDEComponentInfoResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.license.model.LicensedFeature;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.hds.ComponentInfoResourceTestUtils.convertToHdsUrl;
import static com.sonatype.insight.brain.hds.ComponentInfoResourceTestUtils.newComponentDetails;
import static com.sonatype.insight.brain.hds.VersionScoringService.HDS_BULK_SCORE_VERSIONING_PATH;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * IQ Server on PostgreSQL — converted from the legacy {@code IDEComponentInfoResourceTest} /
 * {@code AbstractComponentInfoResourceTest} pair. No base class; uses the injected {@link IqTestContext}.
 */
@IqPostgresTest
class IqPostgresIDEComponentInfoResourceTest
{
  private static final ComponentIdentifier MAVEN_COORDINATES =
      ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "", "jar");

  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private MultiLicenseDAO multiLicenseDAO;

  private Application application;

  @BeforeEach
  void setUp() throws Exception {
    /*
     * License restrictions on enforcement points are checked when uploading scan data, report data retrieval is
     * permitted with most valid licenses.
     */
    ctx.setFeatures(LicensedFeature.COMPONENT_EVALUATION);

    multiLicenseDAO = ctx.lookup(MultiLicenseDAO.class);
    application = ctx.tempEntity().newApplicationWithParent("AbstractComponentInfoResourceTest");
  }

  @BeforeEach
  @AfterEach
  void resetCircuitBreaker() {
    // The circuit breaker is a field on a singleton IdeComponentDetailsHdsClient bean, shared across
    // all tests in this JVM fork (reused server). Reset it to closed before/after each test so a
    // failure in one test doesn't poison subsequent ones.
    IdeComponentDetailsHdsClient ideHdsClient = ctx.lookup(IdeComponentDetailsHdsClient.class);
    if (ideHdsClient != null) {
      ideHdsClient.getCircuitBreaker().recordSuccess();
    }
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(IDEComponentInfoResource.RESOURCE_PATH);
  }

  private String getOwnerId() {
    return application.getPublicId();
  }

  private HttpRequest detailsRequest(
      String ownerId,
      ComponentIdentifier componentIdentifier,
      String hash,
      MatchState matchState,
      Boolean proprietary)
  {
    String identificationSource =
        MatchState.UNKNOWN == matchState || matchState == null ? null : IdentificationSource.SONATYPE.getId();
    return restRequest().path(application.getType().toString(), ownerId)
        .query("componentIdentifier", componentIdentifier)
        .query("hash", hash)
        .query("matchState", matchState != null ? matchState.getId() : null)
        .query("proprietary", proprietary)
        .query("identificationSource", identificationSource)
        .query("scanId", (String) null);
  }

  private HttpRequest listRequest(String ownerId, ComponentIdentifier componentIdentifier) {
    return restRequest().path(application.getType().toString(), ownerId, "list")
        .query("componentIdentifier", componentIdentifier);
  }

  private HttpRequest allVersionsRequest(String ownerId, ComponentIdentifier componentIdentifier) {
    return restRequest().path(application.getType().toString(), ownerId, "allVersions")
        .query("componentIdentifier", componentIdentifier);
  }

  @Test
  void testGetComponentDetailsList_Unlicensed() throws Exception {
    ctx.uninstallLicense();
    HttpResponse response = listRequest(getOwnerId(), ComponentIdentifier.createMavenCoordinates("ulg", "ula", "ulv"))
        .get();
    ctx.assertResponseStatus(402, response);
  }

  @Test
  void testGetComponentDetails_Unlicensed() throws Exception {
    ctx.uninstallLicense();
    HttpResponse response = detailsRequest(getOwnerId(),
        ComponentIdentifier.createMavenCoordinates("ulg", "ula", "ulv"), "ulh", MatchState.UNKNOWN, null).get();
    ctx.assertResponseStatus(402, response);
  }

  @Test
  void testGetComponentDetails() throws Exception {
    String hash = "01234567890123456789";

    HttpResponse response = detailsRequest(getOwnerId(), MAVEN_COORDINATES, hash, MatchState.SIMILAR, false).get();
    ctx.assertResponseStatus(200, response);

    ComponentDetails componentDetails = response.getBody(TestNamedComponentDetails.class);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getHash()).isEqualTo(hash);
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_COORDINATES);
    assertThat(componentDetails.getMatchState()).isEqualTo(MatchState.SIMILAR.getId());
    assertThat(componentDetails.getIdentificationSource()).isEqualTo(IdentificationSource.SONATYPE.getId());
  }

  @Test
  void testGetComponentDetailsList() throws Exception {
    ComponentDetails hdsComponentDetails = newComponentDetails(MAVEN_COORDINATES, multiLicenseDAO);
    ComponentDetailsList hdsComponentDetailsList = new ComponentDetailsList();
    hdsComponentDetailsList.setList(Collections.singletonList(hdsComponentDetails));
    HttpRequest request = listRequest(getOwnerId(), MAVEN_COORDINATES);
    ctx.hdsRespondWith(hdsComponentDetailsList).atUri(convertToHdsUrl(request.getUrl()));

    HttpResponse response = request.get();
    ctx.assertResponseStatus(200, response);

    ComponentDetailsList componentDetailsList = response.getBody(TestComponentDetailsList.class);
    assertThat(componentDetailsList).isNotNull();
    assertThat(componentDetailsList.getList()).hasSize(1);
    ComponentDetails componentDetails = componentDetailsList.getList().get(0);
    assertComponentDetails(componentDetails, hdsComponentDetails);
  }

  @Test
  void testGetComponentVersionInfo() throws Exception {
    ComponentDetails hdsComponentDetails = newComponentDetails(MAVEN_COORDINATES, multiLicenseDAO);
    ComponentDetailsList hdsComponentDetailsList = new ComponentDetailsList();
    hdsComponentDetailsList.setList(Collections.singletonList(hdsComponentDetails));
    HttpRequest request = allVersionsRequest(getOwnerId(), MAVEN_COORDINATES);
    ctx.hdsRespondWith(hdsComponentDetailsList).atUri(convertToHdsUrl(request.getUrl()));
    ctx.hdsRespondWith(new ComponentDependenciesDTO(new HashMap<>(), new HashMap<>()))
        .atUri("rest/component/dependencies");
    ctx.hdsRespondWith(new VersionScoringService[]{}).atUri(HDS_BULK_SCORE_VERSIONING_PATH);

    HttpResponse response = request.get();
    ctx.assertResponseStatus(200, response);

    ComponentVersionInfoDTO responseDto = response.getBody(ComponentVersionInfoDTO.class);
    List<ComponentDetailsDTO> componentDetailsForAllVersions = responseDto.allVersions;

    assertThat(componentDetailsForAllVersions).hasSize(1);
    ComponentDetailsDTO componentDetailsDTO = componentDetailsForAllVersions.get(0);
    assertComponentDetails(componentDetailsDTO, hdsComponentDetails);
    assertRemediation(responseDto.remediation);
  }

  /**
   * Fast test proving the resource is wired to the breaker-protected client (runs on PR builds).
   * A wiring regression (e.g., constructor no longer sets @Named("ideComponentDetails") client) would let
   * this test fail: 5+ HDS failures would not trip the breaker because the resource would be using the
   * default unprotected HdsClient instead.
   */
  @Test
  void testGetComponentDetails_hdsFailuresTripCircuitBreaker() throws Exception {
    HttpRequest request = detailsRequest(getOwnerId(), MAVEN_COORDINATES, "hash", MatchState.SIMILAR, false);
    ctx.hdsRespondWith("service unavailable").atUri(convertToHdsUrl(request.getUrl())).andStatus(503);

    // 5 requests (each retried once, so 10 servlet hits) trip the 5-consecutive-failure threshold.
    // BadGatewayException surfaces as 502.
    for (int i = 0; i < 5; i++) {
      HttpResponse response = request.get();
      ctx.assertResponseStatus(HttpStatus.BAD_GATEWAY_502, response);
    }

    // Breaker is now open: the next call fails fast with 504.
    HttpResponse response = request.get();
    ctx.assertResponseStatus(HttpStatus.GATEWAY_TIMEOUT_504, response);
    // Note: breaker will be reset in @AfterEach
  }

  /**
   * Full recovery test: proves the breaker closes after cooldown + successful probe.
   * Waits 35s for the 30s cooldown.
   */
  @Test
  void testGetComponentDetails_hdsFailuresOpenCircuitBreakerAndRecover() throws Exception {
    HttpRequest request = detailsRequest(getOwnerId(), MAVEN_COORDINATES, "hash", MatchState.SIMILAR, false);
    ctx.hdsRespondWith("service unavailable").atUri(convertToHdsUrl(request.getUrl())).andStatus(503);

    // 5 requests trip the breaker (same as fast test above).
    for (int i = 0; i < 5; i++) {
      HttpResponse response = request.get();
      ctx.assertResponseStatus(HttpStatus.BAD_GATEWAY_502, response);
    }

    // Breaker is open.
    HttpResponse response = request.get();
    ctx.assertResponseStatus(HttpStatus.GATEWAY_TIMEOUT_504, response);

    // Wait out the 30s cooldown, then let a successful probe through.
    ctx.hdsRespondWith(new NamedComponentDetails()).atUri(convertToHdsUrl(request.getUrl()));
    Thread.sleep(Duration.ofSeconds(35).toMillis());
    ctx.assertResponseStatus(HttpStatus.OK_200, request.get());
  }

  private void assertRemediation(ApiComponentRemediationValueDTO remediationValue) {
    assertThat(remediationValue.componentOverrides).hasSize(0);
    assertThat(remediationValue.policyWaivers).hasSize(0);
    assertThat(remediationValue.versionChanges).hasSize(1);

    ApiVersionChangeOptionDTO versionChange = remediationValue.versionChanges.get(0);
    assertThat(versionChange.getType()).isEqualTo(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS);
    assertThat(versionChange.getData().getComponent().packageUrl).isEqualTo("pkg:maven/g1/a1@v1?type=jar");
    assertThat(versionChange.getData().getComponent().displayName).isEqualTo(
        ComponentDisplayNameUtil
            .fromIdentifier(versionChange.getData().getComponent().componentIdentifier.toComponentIdentifier())
            .toString());
  }

  private void assertComponentDetails(ComponentDetails actual, ComponentDetails expected) {
    assertThat(actual.getComponentIdentifier()).isEqualTo(expected.getComponentIdentifier());
    assertThat(actual.getHash()).isEqualTo(expected.getHash());
    assertThat(actual.getMatchState()).isEqualTo(expected.getMatchState());
    assertThat(actual.getDeclaredLicenses()).isEqualTo(expected.getDeclaredLicenses());
    assertThat(actual.getObservedLicenses()).isEqualTo(expected.getObservedLicenses());
    assertThat(actual.getOverriddenLicenses()).isEqualTo(expected.getOverriddenLicenses());
    assertThat(actual.getEffectiveLicenses()).isEqualTo(expected.getEffectiveLicenses());
    assertThat(actual.getEffectiveLicenseStatus()).isEqualTo(expected.getEffectiveLicenseStatus());
    assertThat(actual.getCatalogDate()).isEqualTo(expected.getCatalogDate());
    assertThat(actual.getSecurityVulnerabilities()).hasSameSizeAs(expected.getSecurityVulnerabilities());
    for (int i = 0; i < expected.getSecurityVulnerabilities().size(); i++) {
      assertSecurityVulnerability(actual.getSecurityVulnerabilities().get(i),
          expected.getSecurityVulnerabilities().get(i));
    }
    assertThat(actual.getWebsite()).isEqualTo(expected.getWebsite());
    assertThat(actual.getLicenseThreatLevel()).isEqualTo(expected.getLicenseThreatLevel());
    assertThat(actual.getLicenseThreatGroupNames()).isEqualTo(Collections.singletonList("Weak Copyleft"));
    assertThat(actual.getIdentificationSource()).isEqualTo(expected.getIdentificationSource());
    assertThat(actual.getIdentificationSourceComment()).isEqualTo(expected.getIdentificationSourceComment());
  }

  private void assertComponentDetails(ComponentDetailsDTO actual, ComponentDetails expected) {
    assertThat(actual.catalogDate).isEqualTo(expected.getCatalogDate());
    assertThat(actual.componentIdentifier).isEqualTo(expected.getComponentIdentifier());
    assertThat(actual.displayName)
        .hasToString(ComponentDisplayNameUtil.fromIdentifier(expected.getComponentIdentifier()).toString());
    assertThat(actual.declaredLicenses).isEqualTo(expected.getDeclaredLicenses());
    assertThat(actual.observedLicenses).isEqualTo(expected.getObservedLicenses());
    assertThat(actual.overriddenLicenses).isEqualTo(expected.getOverriddenLicenses());
    assertThat(actual.effectiveLicenses).isEqualTo(expected.getEffectiveLicenses());
    assertThat(actual.effectiveLicenseStatus).isEqualTo(expected.getEffectiveLicenseStatus());
    assertThat(actual.highestSecurityVulnerabilitySeverity).isEqualTo(expected.getSecurityVulnerabilities()
        .stream()
        .map(SecurityVulnerability::getSeverity)
        .max(Float::compareTo)
        .get());
    assertThat(actual.identificationSource).isEqualTo(expected.getIdentificationSource());
    assertThat(actual.identificationSourceComment).isEqualTo(expected.getIdentificationSourceComment());
    assertThat(actual.majorRevisionStep).isEqualTo(expected.isMajorRevisionStep());
    assertThat(actual.matchState).isEqualTo(expected.getMatchState());
    assertThat(actual.relativePopularity).isEqualTo(expected.getRelativePopularity());
    assertThat(actual.securityVulnerabilityCount).isEqualTo(expected.getSecurityVulnerabilities().size());
    assertThat(actual.website).isEqualTo(expected.getWebsite());
  }

  private void assertSecurityVulnerability(SecurityVulnerability actual, SecurityVulnerability expected) {
    assertThat(actual.getRefId()).isEqualTo(expected.getRefId());
    assertThat(actual.getSeverity()).isEqualTo(expected.getSeverity());
    assertThat(actual.getSource()).isEqualTo(expected.getSource());
    assertThat(actual.getSummary()).isEqualTo(expected.getSummary());
    assertThat(actual.getStatus()).isEqualTo(expected.getStatus());
    assertThat(actual.getUrl()).isEqualTo(expected.getUrl());
  }
}
