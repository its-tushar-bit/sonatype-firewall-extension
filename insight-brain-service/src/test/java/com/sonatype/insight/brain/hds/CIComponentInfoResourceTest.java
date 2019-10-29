/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.hds.ComponentInfoService.ComponentLicenses;
import com.sonatype.insight.brain.hds.ComponentInfoService.ComponentSecurityVulnerabilities;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.service.InsightWork;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.hds.ComponentInfoResourceTestUtils.convertToHdsUrl;
import static com.sonatype.insight.brain.hds.ComponentInfoResourceTestUtils.toLicenseDTO;
import static org.assertj.core.api.Assertions.assertThat;

public class CIComponentInfoResourceTest
    extends AbstractComponentInfoResourceTest
{
  private Repository repository;

  protected HttpRequest vulnerabilitiesRequest(final OwnerType ownerType,
                                               final String ownerId,
                                               final String hash,
                                               final ComponentIdentifier componentIdentifier,
                                               final String identificationSource,
                                               final String scanId)
  {
    return restRequest().path(CIComponentInfoResource.VULNERABILITIES_PATH).parameter(ownerType, ownerId)
        .query("hash", hash).query("componentIdentifier", componentIdentifier)
        .query("identificationSource", identificationSource).query("scanId", scanId);
  }

  protected HttpRequest vulnerabilitiesRequest(final OwnerType ownerType,
                                               final String ownerId,
                                               final String hash,
                                               final ComponentIdentifier componentIdentifier)
  {
    return vulnerabilitiesRequest(ownerType, ownerId, hash, componentIdentifier, null, null);
  }

  protected HttpRequest licensesRequest(
      ComponentIdentifier componentIdentifier,
      String identificationSource,
      String scanId)
  {
    return restRequest().path(CIComponentInfoResource.LICENSES_PATH).parameter(getOwner().getType(), getOwnerId())
        .query("componentIdentifier", componentIdentifier).query("identificationSource", identificationSource)
        .query("scanId", scanId);
  }

  protected HttpRequest licensesRequest(ComponentIdentifier componentIdentifier) {
    return licensesRequest(componentIdentifier, null, null);
  }

  @Before
  public void createRepository() {
    repository = tempEntity.newRepository();
  }

  @Override
  protected String getResourcePath() {
    return CIComponentInfoResource.RESOURCE_PATH;
  }

  @Test
  public void testGetComponentDetails() throws Exception {
    testGetComponentDetails_ReadPermission();
  }

  @Test
  public void testGetComponentDetailsList() throws Exception {
    testGetComponentDetailsList_ReadPermission();
  }

  @Test
  public void testGetComponentDetails_ThirdParty() throws Exception {
    final String scanId = "ScanId";
    createReportFile(getOwner().getId(), scanId, "/CIComponentInfoResourceTest/report");
    final ComponentIdentifier tpComponentIdentifier = componentIdentifierFrom("debian", "glibc", "2.24-11+deb9u3");

    HttpRequest request = detailsRequest(getOwnerId(), tpComponentIdentifier, null, MatchState.EXACT, false,
        IdentificationSource.CLAIR.getId(), scanId);
    HttpResponse response = request.get();
    assertResponseStatus(200, response);

    ComponentDetails componentDetails = response.getBody(TestNamedComponentDetails.class);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(tpComponentIdentifier);
    assertThat(componentDetails.getMatchState()).isEqualTo(MatchState.EXACT.getId());
    assertThat(componentDetails.getIdentificationSource()).isEqualTo(IdentificationSource.CLAIR.getId());
  }

  @Test
  public void testGetComponentDetailsForAllVersions_ThirdParty() throws Exception {
    final String scanId = "ScanId";
    createReportFile(getOwner().getId(), scanId, "/CIComponentInfoResourceTest/report");
    final ComponentIdentifier tpComponentIdentifier = componentIdentifierFrom("debian", "glibc", "2.24-11+deb9u3");

    HttpRequest request = allVersionsRequest(getOwnerId(), tpComponentIdentifier).query("identificationSource", "Clair")
        .query("scanId", scanId);
    HttpResponse response = request.get();
    assertResponseStatus(200, response);

    ComponentDetailsDTO[] tpAllVersions = response.getBody(ComponentDetailsDTO[].class);
    assertThat(tpAllVersions).hasSize(1);
    ComponentDetailsDTO componentDetailsDTO = tpAllVersions[0];
    assertThat(componentDetailsDTO.identificationSource).isEqualTo("Clair");
    assertThat(componentDetailsDTO.matchState).isEqualTo("exact");
    assertThat(componentDetailsDTO.componentIdentifier).isEqualTo(tpComponentIdentifier);
    assertThat(componentDetailsDTO.highestSecurityVulnerabilitySeverity).isEqualTo(10.0f);
    assertThat(componentDetailsDTO.securityVulnerabilityCount).isEqualTo(2);
  }

  @Test
  public void testGetLicenses_ThirdParty() throws Exception {
    final String scanId = "ScanId";
    createReportFile(getOwner().getId(), scanId, "/CIComponentInfoResourceTest/report");
    final ComponentIdentifier tpComponentIdentifier = componentIdentifierFrom("debian", "glibc", "2.24-11+deb9u3");

    HttpResponse response = licensesRequest(tpComponentIdentifier, "Clair", scanId).get();
    assertResponseStatus(200, response);
    ComponentLicenses licenses = response.getBody(ComponentLicenses.class);
    assertThat(licenses.declaredlicenses).isEmpty();
    assertThat(licenses.observedlicenses).isEmpty();
  }

  @Test
  public void testGetLicenses_Unlicensed() throws Exception {
    uninstallLicense();
    HttpResponse response = licensesRequest(ComponentIdentifier.createMavenCoordinates("ulg", "ula", "ulv")).get();
    assertResponseStatus(402, response);
  }

  @Test
  public void testGetLicenses() throws Exception {
    ComponentDetails hdsComponentDetails = new ComponentDetails(MAVEN_COORDINATES);
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-2.0"));
    hdsRespondWith(hdsComponentDetails)
        .atUri(convertToHdsUrl(detailsRequest(getOwnerId(), MAVEN_COORDINATES, null, null, null).getUrl()));

    HttpResponse response = licensesRequest(MAVEN_COORDINATES).get();
    assertResponseStatus(200, response);
    ComponentLicenses licenses = response.getBody(ComponentLicenses.class);
    assertThat(licenses.declaredlicenses).extracting(license -> license.license.getLicenseId())
        .containsExactlyInAnyOrder("Apache-2.0");
    assertThat(licenses.observedlicenses).isEmpty();
  }

  private Set<License> toLicenseSet(String... licenseIds) {
    Set<License> result = new LinkedHashSet<>();
    MultiLicenseDAO dao = new MultiLicenseDAO();
    for (String licenseId : licenseIds) {
      MultiLicense multiLicense = dao.getByIdNotNull(licenseId);
      result.add(toLicenseDTO(multiLicense));
    }
    return result;
  }

  @Test
  public void testGetSecurityVulnerabilities() throws Exception {
    ComponentDetails hdsComponentDetails = new ComponentDetails(MAVEN_COORDINATES);
    String hash = "hash";
    hdsComponentDetails.setHash(hash);
    SecurityVulnerability vulnerability = new SecurityVulnerability("refId", "source", 5.0f, "summary");
    hdsComponentDetails.setSecurityVulnerabilities(Collections.singletonList(vulnerability));

    hdsRespondWith(hdsComponentDetails)
        .atUri(convertToHdsUrl(detailsRequest(repository.getId(), MAVEN_COORDINATES, hash, null, null).getUrl()));

    HttpResponse response = vulnerabilitiesRequest(OwnerType.REPOSITORY, repository.getId(), hash, MAVEN_COORDINATES)
        .get();
    assertResponseStatus(200, response);
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
  public void testGetSecurityVulnerabilities_ThirdParty() throws Exception {
    final String scanId = "ScanId";
    String hash = "hash";
    createReportFile(getOwner().getId(), scanId, "/CIComponentInfoResourceTest/report");
    final ComponentIdentifier tpComponentIdentifier = componentIdentifierFrom("debian", "apt", "1.4.8");

    HttpResponse response =
        vulnerabilitiesRequest(getOwner().getType(), getOwnerId(), hash, tpComponentIdentifier,
            IdentificationSource.CLAIR.getId(), scanId).get();
    assertResponseStatus(200, response);
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
  public void testGetSecurityVulnerabilities_NoRepository() throws Exception {
    HttpResponse response = vulnerabilitiesRequest(OwnerType.REPOSITORY, "repositoryDoesNotExist", "hash",
        MAVEN_COORDINATES).get();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Cannot find a repository with ID repositoryDoesNotExist.");
  }

  private File createReportFile(String appId, String scanId, String sourceReportDir) throws IOException {
    File reportFile = new InsightWork(getCLMServer().getConfiguration()).getReportFile(appId, scanId);
    FileUtils.copyFile(zipResourceDir(sourceReportDir), reportFile);
    return reportFile;
  }

  private ComponentIdentifier componentIdentifierFrom(final String format, final String name, final String version) {
    final HashMap<String, String> coords = new HashMap<>();
    coords.put("name", name);
    coords.put(ComponentIdentifier.VERSION, version);
    return new ComponentIdentifier(format, coords);
  }
}
