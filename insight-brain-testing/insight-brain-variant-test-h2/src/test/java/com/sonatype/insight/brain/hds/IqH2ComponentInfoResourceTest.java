/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.hds.ComponentInfoService.ComponentLicenses;
import com.sonatype.insight.brain.hds.ComponentInfoService.ComponentMultiLicenses;
import com.sonatype.insight.brain.hds.ComponentInfoService.ComponentSecurityVulnerabilities;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.DependencyType;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.repository.RepositoryQueryService;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.license.model.LicensedFeature;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.sonatype.insight.brain.hds.ComponentInfoResourceTestUtils.convertToHdsUrl;
import static com.sonatype.insight.brain.hds.ComponentInfoResourceTestUtils.toLicenseDTO;
import static com.sonatype.insight.brain.hds.VersionScoringService.HDS_BULK_SCORE_VERSIONING_PATH;
import static com.sonatype.insight.brain.model.license.License.UNSPECIFIED_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Kept in the {@code com.sonatype.insight.brain.hds} package because
 * {@link ComponentInfoResource#COMPONENT_DETAILS_PATH}/{@link ComponentInfoResource#MULTI_LICENSES_LEGAL_REVIEWER_PATH}
 * are package-private. Reproduces the {@code AbstractComponentInfoResourceTest} fixture that the legacy
 * {@code ComponentInfoResourceTest} inherited.
 */
@IqH2Test
class IqH2ComponentInfoResourceTest
{
  private IqTestContext ctx;

  private final WireMockServer nxrm3MockSever = new WireMockServer(wireMockConfig().dynamicPort());

  private static final ComponentIdentifier MAVEN_COORDINATES = ComponentIdentifier.createMavenCoordinates("g1", "a1",
      "v1", "", "jar");

  private Repository repository;

  private MultiLicenseDAO multiLicenseDAO;

  private ApplicationDAO applicationDAO;

  private Configuration configurationService;

  private Application application;

  private Owner getOwner() {
    return application;
  }

  private String getOwnerId() {
    return application.getPublicId();
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(ComponentInfoResource.RESOURCE_PATH);
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

  private HttpRequest vulnerabilitiesRequest(
      final OwnerType ownerType,
      final String ownerId,
      final String hash,
      final ComponentIdentifier componentIdentifier,
      final String identificationSource,
      final String scanId)
  {
    return restRequest().path(ComponentInfoResource.VULNERABILITIES_PATH)
        .parameter(ownerType, ownerId)
        .query("hash", hash)
        .query("componentIdentifier", componentIdentifier)
        .query("identificationSource", identificationSource)
        .query("scanId", scanId);
  }

  private HttpRequest vulnerabilitiesRequest(
      final OwnerType ownerType,
      final String ownerId,
      final String hash,
      final ComponentIdentifier componentIdentifier)
  {
    return vulnerabilitiesRequest(ownerType, ownerId, hash, componentIdentifier, null, null);
  }

  private HttpRequest licensesRequest(
      ComponentIdentifier componentIdentifier,
      String identificationSource,
      String scanId)
  {
    return restRequest().path(ComponentInfoResource.LICENSES_PATH)
        .parameter(getOwner().getType(), getOwnerId())
        .query("componentIdentifier", componentIdentifier)
        .query("identificationSource", identificationSource)
        .query("scanId", scanId);
  }

  private HttpRequest multiLicensesRequest(
      ComponentIdentifier componentIdentifier,
      String identificationSource,
      String scanId,
      String path)
  {
    return restRequest().path(path)
        .parameter(getOwner().getType(), getOwnerId())
        .query("componentIdentifier", componentIdentifier)
        .query("identificationSource", identificationSource)
        .query("scanId", scanId);
  }

  private HttpRequest licensesRequest(ComponentIdentifier componentIdentifier) {
    return licensesRequest(componentIdentifier, null, null);
  }

  private HttpRequest multiLicensesRequest(ComponentIdentifier componentIdentifier, String path) {
    return multiLicensesRequest(componentIdentifier, null, null, path);
  }

  @BeforeEach
  void before() throws Exception {
    nxrm3MockSever.start();

    /*
     * License restrictions on enforcement points are checked when uploading scan data, report data retrieval is
     * permitted with most valid licenses.
     */
    ctx.setFeatures(LicensedFeature.COMPONENT_EVALUATION);

    multiLicenseDAO = ctx.lookup(MultiLicenseDAO.class);
    application = ctx.tempEntity().newApplicationWithParent("AbstractComponentInfoResourceTest");

    applicationDAO = ctx.lookup(ApplicationDAO.class);
    configurationService = ctx.lookup(Configuration.class);

    assertThat(configurationService.isALPObservedLicenseDetectionEnabled()).isTrue();

    repository = ctx.tempEntity().newRepository();
  }

  @org.junit.jupiter.api.AfterEach
  void after() {
    nxrm3MockSever.stop();
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

  private void testGetComponentDetails_EvaluateComponentPermission() throws Exception {
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

  private void testGetComponentDetailsList_EvaluateComponentPermission() throws Exception {
    ComponentDetails hdsComponentDetails = ComponentInfoResourceTestUtils.newComponentDetails(MAVEN_COORDINATES,
        multiLicenseDAO);
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

  void testGetComponentDetails_ReadPermission() throws Exception {
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

  void testGetComponentDetailsList_ReadPermission() throws Exception {
    ComponentDetails hdsComponentDetails = ComponentInfoResourceTestUtils.newComponentDetails(MAVEN_COORDINATES,
        multiLicenseDAO);
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
  void testGetComponentDetails() throws Exception {
    testGetComponentDetails_ReadPermission();
  }

  @Test
  void testGetComponentDetailsList() throws Exception {
    testGetComponentDetailsList_ReadPermission();
  }

  @Test
  void testGetComponentVersionInfo_FromInnerSourceRepository() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    app.setRepositoryConnectionEnabled(true);
    applicationDAO.update(app);
    ctx.tempEntity().newRepositoryConnection(app.getId(), nxrm3MockSever.baseUrl(), null, null);
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "1.2.0", "", "jar");
    nxrm3MockSever.stubFor(get(urlPathMatching("/service/rest/v1/search/assets"))
        .withQueryParam(RepositoryQueryService.NEXUS3_QUERY_MAVEN_GROUP_KEY,
            equalTo(identifier.get(ComponentIdentifier.MAVEN_GROUP_ID)))
        .withQueryParam(RepositoryQueryService.NEXUS3_QUERY_NAME_KEY,
            equalTo(identifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID)))
        .withQueryParam(RepositoryQueryService.NEXUS3_QUERY_MAVEN_EXTENSION_KEY,
            equalTo(identifier.get(ComponentIdentifier.MAVEN_EXTENSION)))
        .withQueryParam(RepositoryQueryService.NEXUS3_QUERY_MAVEN_CLASSIFIER_KEY,
            equalTo(""))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(getCannedResponse("maven_nopaging.json"))));

    ComponentDetailsList hdsComponentDetailsList = new ComponentDetailsList();
    HttpRequest requestMock = listRequest(getOwnerId(), identifier);
    ctx.hdsRespondWith(hdsComponentDetailsList).atUri(convertToHdsUrl(requestMock.getUrl()));
    ctx.hdsRespondWith(new VersionScoringService[]{}).atUri(HDS_BULK_SCORE_VERSIONING_PATH);

    HttpRequest request = allVersionsRequest(app.getPublicId(), identifier)
        .query("dependencyType", DependencyType.INNER_SOURCE.getId())
        .query("identificationSource", IdentificationSource.PACKAGE_MANIFEST.getId());

    HttpResponse response = request.get();
    ctx.assertResponseStatus(200, response);

    ComponentVersionInfoDTO responseDto = response.getBody(ComponentVersionInfoDTO.class);
    List<ComponentDetailsDTO> resultDto = responseDto.allVersions;

    assertThat(resultDto).hasSize(3);
    assertInnerSourceRepositoryVersionInfo(resultDto.get(0), identifier.createAlternativeVersion("1.1.0"),
        IdentificationSource.PACKAGE_MANIFEST);
    assertInnerSourceRepositoryVersionInfo(resultDto.get(1), identifier, IdentificationSource.PACKAGE_MANIFEST);
    assertInnerSourceRepositoryVersionInfo(resultDto.get(2), identifier.createAlternativeVersion("1.3.0"),
        IdentificationSource.PACKAGE_MANIFEST);
  }

  @Test
  void testGetComponentDetails_ThirdParty() throws Exception {
    final String scanId = "ScanId";
    ctx.createReportFile(getOwner().getId(), scanId, "/CIComponentInfoResourceTest/report");
    final ComponentIdentifier tpComponentIdentifier = componentIdentifierFrom("debian", "glibc", "2.24-11+deb9u3");

    HttpRequest request = detailsRequest(getOwnerId(), tpComponentIdentifier, null, MatchState.EXACT, false,
        IdentificationSource.CLAIR.getId(), scanId);
    HttpResponse response = request.get();
    ctx.assertResponseStatus(200, response);

    ComponentDetails componentDetails = response.getBody(TestNamedComponentDetails.class);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(tpComponentIdentifier);
    assertThat(componentDetails.getMatchState()).isEqualTo(MatchState.EXACT.getId());
    assertThat(componentDetails.getIdentificationSource()).isEqualTo(IdentificationSource.CLAIR.getId());
  }

  @Test
  void testGetComponentVersionInfo_ThirdParty() throws Exception {
    final String scanId = "ScanId";
    ctx.createReportFile(getOwner().getId(), scanId, "/CIComponentInfoResourceTest/report");
    final ComponentIdentifier tpComponentIdentifier = componentIdentifierFrom("debian", "glibc", "2.24-11+deb9u3");

    HttpRequest request = allVersionsRequest(getOwnerId(), tpComponentIdentifier).query("identificationSource", "Clair")
        .query("scanId", scanId);
    HttpResponse response = request.get();
    ctx.assertResponseStatus(200, response);

    ComponentVersionInfoDTO responseDto = response.getBody(ComponentVersionInfoDTO.class);
    List<ComponentDetailsDTO> tpAllVersions = responseDto.allVersions;
    assertThat(tpAllVersions).hasSize(1);
    ComponentDetailsDTO componentDetailsDTO = tpAllVersions.get(0);
    assertThat(componentDetailsDTO.identificationSource).isEqualTo("Clair");
    assertThat(componentDetailsDTO.matchState).isEqualTo("exact");
    assertThat(componentDetailsDTO.componentIdentifier).isEqualTo(tpComponentIdentifier);
    assertThat(componentDetailsDTO.highestSecurityVulnerabilitySeverity).isEqualTo(10.0f);
    assertThat(componentDetailsDTO.securityVulnerabilityCount).isEqualTo(2);
    assertThat(responseDto.remediation.versionChanges).hasSize(1);
    ApiVersionChangeOptionDTO versionChangeDTO = responseDto.remediation.versionChanges.get(0);
    assertThat(versionChangeDTO.getType()).isEqualTo(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS);
    assertThat(versionChangeDTO.getData().getComponent().packageUrl).isEqualTo("pkg:debian/glibc@2.24-11%2Bdeb9u4");
    assertThat(versionChangeDTO.getData().getComponent().displayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(versionChangeDTO.getData().getComponent().componentIdentifier
            .toComponentIdentifier()).toString());
  }

  @Test
  void testGetLicenses_ThirdParty() throws Exception {
    final String scanId = "ScanId";
    ctx.createReportFile(getOwner().getId(), scanId, "/CIComponentInfoResourceTest/report");
    final ComponentIdentifier tpComponentIdentifier = componentIdentifierFrom("debian", "glibc", "2.24-11+deb9u3");

    HttpResponse response = licensesRequest(tpComponentIdentifier, "Clair", scanId).get();
    ctx.assertResponseStatus(200, response);
    ComponentLicenses licenses = response.getBody(ComponentLicenses.class);
    assertThat(licenses.declaredlicenses).hasSize(1);
    assertThat(licenses.declaredlicenses).extracting(license -> license.license.getLicenseId())
        .containsExactlyInAnyOrder("Apache-2.0");
    assertThat(licenses.observedlicenses).hasSize(1);
    assertThat(licenses.observedlicenses)
        .extracting(licenseWithThreatLevel -> licenseWithThreatLevel.license.getLicenseId(),
            licenseWithThreatLevel -> licenseWithThreatLevel.license.getLicenseName())
        .containsExactlyInAnyOrder(tuple(UNSPECIFIED_ID, "Not Provided"));
  }

  @Test
  void testGetMultiLicenses_ThirdParty() throws Exception {
    doTestGetMultiLicenses_ThirdParty(ComponentInfoResource.MULTI_LICENSES_PATH);
  }

  @Test
  void testGetMultiLicensesForLegalReviewer_ThirdParty() throws Exception {
    doTestGetMultiLicenses_ThirdParty(ComponentInfoResource.MULTI_LICENSES_LEGAL_REVIEWER_PATH);
  }

  private void doTestGetMultiLicenses_ThirdParty(String path) throws Exception {
    final String scanId = "ScanId";
    ctx.createReportFile(getOwner().getId(), scanId, "/CIComponentInfoResourceTest/report");
    final ComponentIdentifier tpComponentIdentifier = componentIdentifierFrom("debian", "glibc", "2.24-11+deb9u3");

    HttpResponse response = multiLicensesRequest(tpComponentIdentifier, "Clair", scanId, path).get();
    ctx.assertResponseStatus(200, response);
    ComponentMultiLicenses licenses = response.getBody(ComponentMultiLicenses.class);
    assertThat(licenses.declaredLicenses)
        .flatExtracting(multiLicenses -> multiLicenses.licenses)
        .extracting(license -> license.license.getLicenseId())
        .containsExactly("Apache-2.0");
    assertThat(licenses.observedLicenses).flatExtracting(multiLicenses -> multiLicenses.licenses)
        .extracting(licenseWithThreatLevel -> licenseWithThreatLevel.license.getLicenseId(),
            licenseWithThreatLevel -> licenseWithThreatLevel.license.getLicenseName())
        .containsExactly(tuple(UNSPECIFIED_ID, "Not Provided"));
  }

  @Test
  void testGetLicenses_Unlicensed() throws Exception {
    ctx.uninstallLicense();
    HttpResponse response = licensesRequest(ComponentIdentifier.createMavenCoordinates("ulg", "ula", "ulv")).get();
    ctx.assertResponseStatus(402, response);
  }

  @Test
  void testGetMultiLicenses_Unlicensed() throws Exception {
    doTestGetMultiLicenses_Unlicensed(ComponentInfoResource.MULTI_LICENSES_PATH);
  }

  @Test
  void testGetMultiLicensesForLegalReviewer_Unlicensed() throws Exception {
    doTestGetMultiLicenses_Unlicensed(ComponentInfoResource.MULTI_LICENSES_LEGAL_REVIEWER_PATH);
  }

  private void doTestGetMultiLicenses_Unlicensed(String path) throws Exception {
    ctx.uninstallLicense();
    HttpResponse response =
        multiLicensesRequest(ComponentIdentifier.createMavenCoordinates("ulg", "ula", "ulv"), path).get();
    ctx.assertResponseStatus(402, response);
  }

  @Test
  void testGetLicenses() throws Exception {
    ComponentDetails hdsComponentDetails = new ComponentDetails(MAVEN_COORDINATES);
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-2.0"));
    ctx.hdsRespondWith(hdsComponentDetails)
        .atUri(convertToHdsUrl(detailsRequest(getOwnerId(), MAVEN_COORDINATES, null, null, null).getUrl()));

    HttpResponse response = licensesRequest(MAVEN_COORDINATES).get();
    ctx.assertResponseStatus(200, response);
    ComponentLicenses licenses = response.getBody(ComponentLicenses.class);
    assertThat(licenses.declaredlicenses).extracting(license -> license.license.getLicenseId())
        .containsExactlyInAnyOrder("Apache-2.0");
    assertThat(licenses.observedlicenses)
        .extracting(licenseWithThreatLevel -> licenseWithThreatLevel.license.getLicenseId(),
            licenseWithThreatLevel -> licenseWithThreatLevel.license.getLicenseName())
        .containsExactlyInAnyOrder(tuple(UNSPECIFIED_ID, "Not Provided"));

  }

  @Test
  void testGetMultiLicenses() throws Exception {
    doTestGetMultiLicenses(ComponentInfoResource.MULTI_LICENSES_PATH);
  }

  @Test
  void testGetMultiLicensesForLegalReviewer() throws Exception {
    doTestGetMultiLicenses(ComponentInfoResource.MULTI_LICENSES_PATH);
  }

  private void doTestGetMultiLicenses(String path) throws Exception {
    ComponentDetails hdsComponentDetails = new ComponentDetails(MAVEN_COORDINATES);
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-2.0"));
    ctx.hdsRespondWith(hdsComponentDetails)
        .atUri(convertToHdsUrl(detailsRequest(getOwnerId(), MAVEN_COORDINATES, null, null, null).getUrl()));

    HttpResponse response = multiLicensesRequest(MAVEN_COORDINATES, path).get();
    ctx.assertResponseStatus(200, response);
    ComponentMultiLicenses licenses = response.getBody(ComponentMultiLicenses.class);
    assertThat(licenses.declaredLicenses)
        .flatExtracting(multiLicenses -> multiLicenses.licenses)
        .extracting(license -> license.license.getLicenseId())
        .containsExactlyInAnyOrder("Apache-2.0");
    assertThat(licenses.observedLicenses)
        .flatExtracting(multiLicenses -> multiLicenses.licenses)
        .extracting(licenseWithThreatLevel -> licenseWithThreatLevel.license.getLicenseId(),
            licenseWithThreatLevel -> licenseWithThreatLevel.license.getLicenseName())
        .containsExactlyInAnyOrder(tuple(UNSPECIFIED_ID, "Not Provided"));
  }

  private Set<License> toLicenseSet(String... licenseIds) {
    Set<License> result = new LinkedHashSet<>();
    for (String licenseId : licenseIds) {
      MultiLicense multiLicense = multiLicenseDAO.getByIdNotNull(licenseId);
      result.add(toLicenseDTO(multiLicense));
    }
    return result;
  }

  @Test
  void testGetSecurityVulnerabilities() throws Exception {
    ComponentDetails hdsComponentDetails = new ComponentDetails(MAVEN_COORDINATES);
    String hash = "hash";
    hdsComponentDetails.setHash(hash);
    SecurityVulnerability vulnerability = new SecurityVulnerability("refId", "source", 5.0f, "summary");
    hdsComponentDetails.setSecurityVulnerabilities(Collections.singletonList(vulnerability));

    ctx.hdsRespondWith(hdsComponentDetails)
        .atUri(convertToHdsUrl(detailsRequest(repository.getId(), MAVEN_COORDINATES, hash, null, null).getUrl()));

    HttpResponse response = vulnerabilitiesRequest(OwnerType.REPOSITORY, repository.getId(), hash, MAVEN_COORDINATES)
        .get();
    ctx.assertResponseStatus(200, response);
    ComponentSecurityVulnerabilities retrievedVulnerabilities = response
        .getBody(ComponentSecurityVulnerabilities.class);
    assertThat(retrievedVulnerabilities.securityVulnerabilities).hasSize(1);
    SecurityVulnerability retrievedVulnerability = retrievedVulnerabilities.securityVulnerabilities.get(0);
    assertThat(retrievedVulnerability.getRefId()).isEqualTo(vulnerability.getRefId());
    assertThat(retrievedVulnerability.getSource()).isEqualTo(vulnerability.getSource());
    assertThat(retrievedVulnerability.getSeverity()).isEqualTo(vulnerability.getSeverity());
    assertThat(retrievedVulnerability.getSummary()).isEqualTo(vulnerability.getSummary());
    assertThat(retrievedVulnerability.getStatus()).isEqualTo(SecurityVulnerabilityOverrideStatus.OPEN.getName());
  }

  @Test
  void testGetSecurityVulnerabilities_ThirdParty() throws Exception {
    final String scanId = "ScanId";
    String hash = "hash";
    ctx.createReportFile(getOwner().getId(), scanId, "/CIComponentInfoResourceTest/report");
    final ComponentIdentifier tpComponentIdentifier = componentIdentifierFrom("debian", "apt", "1.4.8");

    HttpResponse response =
        vulnerabilitiesRequest(getOwner().getType(), getOwnerId(), hash, tpComponentIdentifier,
            IdentificationSource.CLAIR.getId(), scanId).get();
    ctx.assertResponseStatus(200, response);
    ComponentSecurityVulnerabilities retrievedVulnerabilities = response
        .getBody(ComponentSecurityVulnerabilities.class);
    assertThat(retrievedVulnerabilities.securityVulnerabilities).hasSize(1);
    SecurityVulnerability retrievedVulnerability = retrievedVulnerabilities.securityVulnerabilities.get(0);
    assertThat(retrievedVulnerability.getRefId()).isEqualTo("CVE-2019-3462");
    assertThat(retrievedVulnerability.getSource()).isNull();
    assertThat(retrievedVulnerability.getSeverity()).isEqualTo(10.0f);
    assertThat(retrievedVulnerability.getSummary()).isEqualTo("description CVE-2019-3462");
    assertThat(retrievedVulnerability.getStatus()).isEqualTo(SecurityVulnerabilityOverrideStatus.OPEN.getName());
  }

  @Test
  void testGetSecurityVulnerabilities_NoRepository() throws Exception {
    HttpResponse response = vulnerabilitiesRequest(OwnerType.REPOSITORY, "repositoryDoesNotExist", "hash",
        MAVEN_COORDINATES).get();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Repository with ID repositoryDoesNotExist does not exist.");
  }

  private ComponentIdentifier componentIdentifierFrom(final String format, final String name, final String version) {
    final HashMap<String, String> coords = new HashMap<>();
    coords.put("name", name);
    coords.put(ComponentIdentifier.VERSION, version);
    return new ComponentIdentifier(format, coords);
  }

  private void assertInnerSourceRepositoryVersionInfo(
      final ComponentDetailsDTO cp,
      final ComponentIdentifier expectedId,
      final IdentificationSource identificationSource)
  {
    assertThat(cp.componentIdentifier).isEqualTo(expectedId);
    assertThat(cp.matchState).isEqualTo(MatchState.EXACT.getId());
    assertThat(cp.identificationSource).isEqualTo(identificationSource.getId());
    assertThat(cp.declaredLicenses).hasSize(1).extracting("licenseId").containsExactly("UNSPECIFIED");
    assertThat(cp.observedLicenses).hasSize(1).extracting("licenseId").containsExactly("UNSPECIFIED");
    assertThat(cp.effectiveLicenses).hasSize(1).extracting("licenseId").containsExactly("UNSPECIFIED");
  }

  private String getCannedResponse(final String path) throws IOException {
    return IOUtils.toString(getClass().getResource("/CIComponentInfoResourceTest/repositoryResponses/" + path),
        StandardCharsets.UTF_8);
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

  private void assertSecurityVulnerability(SecurityVulnerability actual, SecurityVulnerability expected) {
    assertThat(actual.getRefId()).isEqualTo(expected.getRefId());
    assertThat(actual.getSeverity()).isEqualTo(expected.getSeverity());
    assertThat(actual.getSource()).isEqualTo(expected.getSource());
    assertThat(actual.getSummary()).isEqualTo(expected.getSummary());
    assertThat(actual.getStatus()).isEqualTo(expected.getStatus());
    assertThat(actual.getUrl()).isEqualTo(expected.getUrl());
  }
}
