/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.hds.ComponentDetailsDTO;
import com.sonatype.insight.brain.hds.ComponentInfoResource;
import com.sonatype.insight.brain.hds.ComponentVersionInfoDTO;
import com.sonatype.insight.brain.hds.TestComponentDetailsList;
import com.sonatype.insight.brain.hds.TestNamedComponentDetails;
import com.sonatype.insight.brain.hds.VersionScoringService;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.hds.ComponentInfoResourceTestUtils.convertToHdsUrl;
import static com.sonatype.insight.brain.hds.ComponentInfoResourceTestUtils.newComponentDetails;
import static com.sonatype.insight.brain.hds.VersionScoringService.HDS_BULK_SCORE_VERSIONING_PATH;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * HRC-scoped sibling of {@code ComponentInfoResourceRepositoryTest}. Exercises the polymorphic
 * {@code {ownerType}/{ownerId}} paths on {@link ComponentInfoResource} keyed on
 * {@code hosted_repository_component}. Folds in the pieces actually exercised (by this class and by the
 * {@code @Test} methods it inherits from the legacy {@code AbstractComponentInfoResourceTest} base) from both.
 */
@IqH2Test
class IqH2ComponentInfoResourceHostedRepositoryComponentTest
{
  private static final ComponentIdentifier MAVEN_COORDINATES =
      ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "", "jar");

  private IqTestContext ctx;

  private MultiLicenseDAO multiLicenseDAO;

  private HostedRepositoryComponent hrc;

  @BeforeEach
  void setUp() throws Exception {
    /*
     * License restrictions on enforcement points are checked when uploading scan data, report data retrieval is
     * permitted with most valid licenses.
     */
    ctx.setFeatures(LicensedFeature.COMPONENT_EVALUATION);

    multiLicenseDAO = ctx.lookup(MultiLicenseDAO.class);
    ctx.tempEntity().newApplicationWithParent("AbstractComponentInfoResourceTest");

    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
    Repository repository = ctx.tempEntity().newRepository();
    hrc = ctx.tempEntity().newHostedRepositoryComponent(repository);
  }

  @AfterEach
  void tearDown() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);
  }

  private Owner getOwner() {
    return hrc;
  }

  private String getOwnerId() {
    return hrc.getId();
  }

  private String getResourcePath() {
    return ComponentInfoResource.RESOURCE_PATH;
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(getResourcePath());
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
    return detailsRequest(ownerId, componentIdentifier, hash, matchState, proprietary, identificationSource, null);
  }

  private HttpRequest detailsRequest(
      String ownerId,
      ComponentIdentifier componentIdentifier,
      String hash,
      MatchState matchState,
      Boolean proprietary,
      String identificationSource,
      String scanId)
  {
    return restRequest().path(getOwner().getType().toString(), ownerId)
        .query("componentIdentifier", componentIdentifier)
        .query("hash", hash)
        .query("matchState", matchState != null ? matchState.getId() : null)
        .query("proprietary", proprietary)
        .query("identificationSource", identificationSource)
        .query("scanId", scanId);
  }

  private HttpRequest listRequest(String ownerId, ComponentIdentifier componentIdentifier) {
    return restRequest().path(getOwner().getType().toString(), ownerId, "list")
        .query("componentIdentifier",
            componentIdentifier);
  }

  private HttpRequest allVersionsRequest(String ownerId, ComponentIdentifier componentIdentifier) {
    return restRequest().path(getOwner().getType().toString(), ownerId, "allVersions")
        .query("componentIdentifier",
            componentIdentifier);
  }

  private void assertRemediation(ApiComponentRemediationValueDTO remediationValue) {
    assertThat(remediationValue).isNotNull();
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

  @Test
  void testGetComponentDetails() throws Exception {
    String hash = "hash";

    final HttpRequest request = detailsRequest(getOwnerId(), MAVEN_COORDINATES, hash, MatchState.SIMILAR, false);
    HttpResponse response = request.get();
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
}
