/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.clm.dto.model.ide.LicenseStatus;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.hds.ComponentDetailsDTO;
import com.sonatype.insight.brain.hds.ComponentVersionInfoDTO;
import com.sonatype.insight.brain.hds.RepoManComponentInfoResource;
import com.sonatype.insight.brain.hds.VersionScoringService;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.license.model.LicensedFeature;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.hds.VersionScoringService.HDS_BULK_SCORE_VERSIONING_PATH;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * IQ Server on PostgreSQL — converted from the legacy
 * {@code com.sonatype.insight.brain.hds.RepoManComponentInfoResourceTest}, exercising the component-info
 * paths inherited from {@code AbstractComponentInfoResourceTest} for the repository-manager owner. No base class.
 */
@IqPostgresTest
class IqPostgresRepoManComponentInfoResourceTest
{
  private static final ComponentIdentifier MAVEN_COORDINATES =
      ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "", "jar");

  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private Application application;

  private MultiLicenseDAO multiLicenseDAO;

  private HttpRequest restRequest() {
    return ctx.restRequest().path(RepoManComponentInfoResource.RESOURCE_PATH);
  }

  @BeforeEach
  void before() throws Exception {
    /*
     * License restrictions on enforcement points are checked when uploading scan data, report data retrieval is
     * permitted with most valid licenses.
     */
    ctx.setFeatures(LicensedFeature.COMPONENT_EVALUATION);

    multiLicenseDAO = ctx.lookup(MultiLicenseDAO.class);
    application = ctx.tempEntity().newApplicationWithParent("AbstractComponentInfoResourceTest");
  }

  private String getOwnerId() {
    return application.getPublicId();
  }

  private Owner getOwner() {
    return application;
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
    return restRequest().path(getOwner().getType().toString(), ownerId)
        .query("componentIdentifier", componentIdentifier)
        .query("hash", hash)
        .query("matchState", matchState != null ? matchState.getId() : null)
        .query("proprietary", proprietary)
        .query("identificationSource", identificationSource)
        .query("scanId", (String) null);
  }

  private HttpRequest listRequest(String ownerId, ComponentIdentifier componentIdentifier) {
    return restRequest().path(getOwner().getType().toString(), ownerId, "list")
        .query("componentIdentifier", componentIdentifier);
  }

  private HttpRequest allVersionsRequest(String ownerId, ComponentIdentifier componentIdentifier) {
    return restRequest().path(getOwner().getType().toString(), ownerId, "allVersions")
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
    String hash = "01234567890123456789";

    HttpResponse response = detailsRequest(getOwnerId(), MAVEN_COORDINATES, hash, MatchState.SIMILAR, false).get();
    ctx.assertResponseStatus(200, response);

    ComponentDetails componentDetails = response.getBody(LocalTestNamedComponentDetails.class);
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

    ComponentDetailsList componentDetailsList = response.getBody(LocalTestComponentDetailsList.class);
    assertThat(componentDetailsList).isNotNull();
    assertThat(componentDetailsList.getList()).hasSize(1);
    ComponentDetails componentDetails = componentDetailsList.getList().get(0);
    assertComponentDetails(componentDetails, hdsComponentDetails);
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

  private void assertSecurityVulnerability(SecurityVulnerability actual, SecurityVulnerability expected) {
    assertThat(actual.getRefId()).isEqualTo(expected.getRefId());
    assertThat(actual.getSeverity()).isEqualTo(expected.getSeverity());
    assertThat(actual.getSource()).isEqualTo(expected.getSource());
    assertThat(actual.getSummary()).isEqualTo(expected.getSummary());
    assertThat(actual.getStatus()).isEqualTo(expected.getStatus());
    assertThat(actual.getUrl()).isEqualTo(expected.getUrl());
  }

  // --- inlined copies of insight-brain-service test-scoped helpers (not on this module's classpath) ---

  private static ComponentDetails newComponentDetails(
      ComponentIdentifier componentIdentifier,
      MultiLicenseDAO multiLicenseDAO)
  {
    ComponentDetails componentDetails = new ComponentDetails(componentIdentifier);
    componentDetails.setHash("somehash");
    componentDetails.setMatchState(MatchState.EXACT.getId());
    componentDetails.setDeclaredLicenses(Collections.singleton(toLicenseDTO(multiLicenseDAO
        .getByIdNotNull("Apache-2.0"))));
    componentDetails
        .setObservedLicenses(Collections.singleton(toLicenseDTO(multiLicenseDAO.getByIdNotNull("EPL-1.0"))));
    componentDetails
        .setOverriddenLicenses(Collections.singleton(toLicenseDTO(multiLicenseDAO.getByIdNotNull("GPL-1.0"))));
    componentDetails
        .setEffectiveLicenses(Collections.singleton(toLicenseDTO(multiLicenseDAO.getByIdNotNull("GPL-1.0"))));
    componentDetails.setEffectiveLicenseStatus(LicenseStatus.Overridden);
    SecurityVulnerability sv = new SecurityVulnerability("refid", "source", 1F);
    sv.setStatus(SecurityVulnerabilityOverrideStatus.OPEN.getName());
    componentDetails.setSecurityVulnerabilities(Collections.singletonList(sv));
    componentDetails.setCatalogDate(new Date().getTime());
    componentDetails.setWebsite("http://www.example.com");
    componentDetails.setLicenseThreatLevel(2);
    componentDetails.setIdentificationSource(IdentificationSource.SONATYPE.getId());
    componentDetails.setIdentificationSourceComment("No comments");
    return componentDetails;
  }

  private static String convertToHdsUrl(String brainUrl) {
    return brainUrl.replaceFirst("(.*/)(rest/[^/]+)/componentDetails(/[^/]+/[^/]+)(.*)", "$2/componentDetails$4")
        .replace("allVersions", "list");
  }

  private static License toLicenseDTO(MultiLicense multiLicense) {
    return new License(multiLicense.getId(), multiLicense.getShortDisplayName());
  }

  private static final class LocalTestNamedComponentDetails
      extends NamedComponentDetails
  {
    private ComponentDisplayName displayName;

    @Override
    public ComponentDisplayName getDisplayName() {
      return displayName;
    }

    public void setDisplayName(ComponentDisplayName displayName) {
      this.displayName = displayName;
    }
  }

  private static final class LocalTestComponentDetailsList
      extends ComponentDetailsList
  {
    @Override
    @JsonDeserialize(contentAs = LocalTestComponentDetails.class)
    public void setList(java.util.List<ComponentDetails> list) {
      super.setList(list);
    }
  }

  private static final class LocalTestComponentDetails
      extends ComponentDetails
  {
    private String groupId;

    private String artifactId;

    private String version;

    LocalTestComponentDetails() {
    }

    @Override
    public String getGroupId() {
      return groupId;
    }

    public void setGroupId(String groupId) {
      this.groupId = groupId;
    }

    @Override
    public String getArtifactId() {
      return artifactId;
    }

    public void setArtifactId(String artifactId) {
      this.artifactId = artifactId;
    }

    @Override
    public String getVersion() {
      return version;
    }

    public void setVersion(String version) {
      this.version = version;
    }
  }
}
