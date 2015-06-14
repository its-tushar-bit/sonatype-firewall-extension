/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.ide.LicenseStatus;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityStatus;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.saas.ComponentInfoService.ComponentLicenses;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public abstract class AbstractComponentInfoResourceTest
    extends AbstractResourceTest
{
  private static final ComponentIdentifier MAVEN_COORDINATES = ComponentIdentifier.createMavenCoordinates("g1", "a1",
      "v1", "", "jar");

  protected abstract String getResourcePath();

  private String applicationPublicId = "AbstractComponentInfoResourceTest";

  private Application application;

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(getResourcePath());
  }

  protected HttpRequest detailsRequest(String appId, ComponentIdentifier componentIdentifier, String hash,
      MatchState matchState, Boolean proprietary)
  {
    return restRequest().path(appId).query("componentIdentifier", componentIdentifier).query("hash", hash)
        .query("matchState", matchState != null ? matchState.getId() : null).query("proprietary", proprietary);
  }

  protected HttpRequest listRequest(String appId, ComponentIdentifier componentIdentifier) {
    return restRequest().path(appId, "list").query("componentIdentifier", componentIdentifier);
  }

  protected HttpRequest licensesRequest(String appId, ComponentIdentifier componentIdentifier) {
    return restRequest().path("licenses", appId).query("componentIdentifier", componentIdentifier);
  }

  @Before
  public void clearEnforcementPointsFromLicense() throws Exception {
    /*
     * License restrictions on enforcement points are checked when uploading scan data, report data retrieval is
     * permitted with any valid license, so these tests should not require any enforcement point in the license.
     */
    setEnforcementPoints();

    application = tempEntity.newApplicationWithParent(applicationPublicId);
  }

  @Test
  public void testGetLicenses_Unlicensed() throws Exception {
    uninstallLicense();
    HttpResponse response = licensesRequest(applicationPublicId,
        ComponentIdentifier.createMavenCoordinates("ulg", "ula", "ulv")).get();
    assertResponseStatus(402, response);
  }

  @Test
  public void testGetComponentDetailsList_Unlicensed() throws Exception {
    uninstallLicense();
    HttpResponse response = listRequest(applicationPublicId,
        ComponentIdentifier.createMavenCoordinates("ulg", "ula", "ulv")).get();
    assertResponseStatus(402, response);
  }

  @Test
  public void testGetComponentDetails_Unlicensed() throws Exception {
    uninstallLicense();
    HttpResponse response = detailsRequest(applicationPublicId,
        ComponentIdentifier.createMavenCoordinates("ulg", "ula", "ulv"), "ulh", MatchState.UNKNOWN, null).get();
    assertResponseStatus(402, response);
  }

  private ComponentDetails newComponentDetails(ComponentIdentifier componentIdentifier) {
    MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();
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
    sv.setStatus(SecurityVulnerabilityStatus.OPEN.getName());
    componentDetails.setSecurityVulnerabilities(Collections.singletonList(sv));
    componentDetails.setCatalogDate(new Date().getTime());
    componentDetails.setWebsite("http://www.example.com");
    componentDetails.setLicenseThreatLevel(2);
    componentDetails.setIdentificationSource("SONATYPE");
    componentDetails.setIdentificationSourceComment("No comments");
    return componentDetails;
  }

  private License toLicenseDTO(MultiLicense multiLicense) {
    return new License(multiLicense.getId(), multiLicense.getShortDisplayName());
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
  public void testGetLicenses() throws Exception {
    ComponentDetails saasComponentDetails = new ComponentDetails(MAVEN_COORDINATES);
    saasComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-2.0"));
    setSaasResponseForURI(
        convertToSaasUrl(detailsRequest(applicationPublicId, MAVEN_COORDINATES, null, null, null).getUrl(), "foo"),
        toJson(saasComponentDetails), 200);

    HttpResponse response = licensesRequest(applicationPublicId, MAVEN_COORDINATES).get();
    assertResponseStatus(200, response);
    ComponentLicenses licenses = response.getBody(ComponentLicenses.class);
    assertThat(licenses.declaredlicenses, hasSize(1));
    assertThat(licenses.declaredlicenses.get(0).license.getLicenseId(), is("Apache-2.0"));
    assertThat(licenses.observedlicenses, hasSize(0));
  }

  protected void testGetComponentDetails_EvaluateComponentPermission() throws Exception {
    String hash = "01234567890123456789";

    HttpResponse response = detailsRequest(applicationPublicId, MAVEN_COORDINATES, hash, MatchState.SIMILAR, false).get();
    assertResponseStatus(200, response);

    ComponentDetails componentDetails = response.getBody(TestNamedComponentDetails.class);
    assertThat(componentDetails, is(notNullValue()));
    assertThat(componentDetails.getHash(), is(hash));
    assertThat(componentDetails.getComponentIdentifier(), is(MAVEN_COORDINATES));
    assertThat(componentDetails.getMatchState(), is(MatchState.SIMILAR.getId()));
    assertThat(componentDetails.getIdentificationSource(), is(IdentificationSource.SONATYPE.getId()));
  }

  protected void testGetComponentDetailsList_EvaluateComponentPermission() throws Exception {
    ComponentDetails saasComponentDetails = newComponentDetails(MAVEN_COORDINATES);
    ComponentDetailsList saasComponentDetailsList = new ComponentDetailsList();
    saasComponentDetailsList.setList(Arrays.asList(saasComponentDetails));
    HttpRequest request = listRequest(applicationPublicId, MAVEN_COORDINATES);
    setSaasResponseForURI(convertToSaasUrl(request.getUrl(), applicationPublicId), toJson(saasComponentDetailsList),
        200);

    HttpResponse response = request.get();
    assertResponseStatus(200, response);

    ComponentDetailsList componentDetailsList = response.getBody(TestComponentDetailsList.class);
    assertThat(componentDetailsList, is(notNullValue()));
    assertThat(componentDetailsList.getList(), hasSize(1));
    ComponentDetails componentDetails = componentDetailsList.getList().get(0);
    assertComponentDetails(componentDetails, saasComponentDetails);
  }

  protected void testGetComponentDetails_ReadPermission() throws Exception {
    String reportId = "4cabb3f39eb945158c240f36aedf05e8";
    FileUtils.copyDirectoryStructure(new File(
        "target/test-classes/AbstractComponentInfoResourceTest/GetComponentDetailsWithReadPermission", reportId),
        getCLMServer().getReportDir(application.getId(), reportId));

    String hash = "a235ba8b489512805ac1";

    HttpResponse response = detailsRequest(applicationPublicId, MAVEN_COORDINATES, hash, MatchState.SIMILAR, false).query(
        "reportId", reportId).get();
    assertResponseStatus(200, response);

    ComponentDetails componentDetails = response.getBody(TestNamedComponentDetails.class);
    assertThat(componentDetails, is(notNullValue()));
    assertThat(componentDetails.getHash(), is(hash));
    assertThat(componentDetails.getComponentIdentifier(), is(MAVEN_COORDINATES));
    assertThat(componentDetails.getMatchState(), is(MatchState.SIMILAR.getId()));
    assertThat(componentDetails.getIdentificationSource(), is(IdentificationSource.SONATYPE.getId()));
  }

  protected void testGetComponentDetailsList_ReadPermission() throws Exception {
    String reportId = "4cabb3f39eb945158c240f36aedf05e8";
    FileUtils.copyDirectoryStructure(new File(
        "target/test-classes/AbstractComponentInfoResourceTest/GetComponentDetailsWithReadPermission", reportId),
        getCLMServer().getReportDir(application.getId(), reportId));

    ComponentDetails saasComponentDetails = newComponentDetails(MAVEN_COORDINATES);
    ComponentDetailsList saasComponentDetailsList = new ComponentDetailsList();
    saasComponentDetailsList.setList(Arrays.asList(saasComponentDetails));
    HttpRequest request = listRequest(applicationPublicId, MAVEN_COORDINATES);
    setSaasResponseForURI(convertToSaasUrl(request.getUrl(), applicationPublicId), toJson(saasComponentDetailsList),
        200);

    HttpResponse response = request.query("reportId", reportId).get();
    assertResponseStatus(200, response);

    ComponentDetailsList componentDetailsList = response.getBody(TestComponentDetailsList.class);
    assertThat(componentDetailsList, is(notNullValue()));
    assertThat(componentDetailsList.getList(), hasSize(1));
    ComponentDetails componentDetails = componentDetailsList.getList().get(0);
    assertComponentDetails(componentDetails, saasComponentDetails);
  }

  private String convertToSaasUrl(String brainUrl, String applicationId) {
    return brainUrl.replaceFirst("(.*/)(rest/[^/]+)/componentDetails(/[^/]+)(.*)", "$2/componentDetails$4");
  }

  private void assertComponentDetails(ComponentDetails actual, ComponentDetails expected) {
    assertThat(actual.getComponentIdentifier(), is(expected.getComponentIdentifier()));
    assertThat(actual.getHash(), is(expected.getHash()));
    assertThat(actual.getMatchState(), is(expected.getMatchState()));
    assertThat(actual.getDeclaredLicenses(), is(expected.getDeclaredLicenses()));
    assertThat(actual.getObservedLicenses(), is(expected.getObservedLicenses()));
    assertThat(actual.getOverriddenLicenses(), is(expected.getOverriddenLicenses()));
    assertThat(actual.getEffectiveLicenses(), is(expected.getEffectiveLicenses()));
    assertThat(actual.getEffectiveLicenseStatus(), is(expected.getEffectiveLicenseStatus()));
    assertThat(actual.getCatalogDate(), is(expected.getCatalogDate()));
    assertThat(actual.getSecurityVulnerabilities().size(), is(expected.getSecurityVulnerabilities().size()));
    for (int i = 0; i < expected.getSecurityVulnerabilities().size(); i++) {
      assertSecurityVulnerability(actual.getSecurityVulnerabilities().get(i), expected.getSecurityVulnerabilities()
          .get(i));
    }
    assertThat(actual.getWebsite(), is(expected.getWebsite()));
    assertThat(actual.getLicenseThreatLevel(), is(expected.getLicenseThreatLevel()));
    assertThat(actual.getLicenseThreatGroupNames(), is(Collections.singletonList("Weak Copyleft")));
    assertThat(actual.getIdentificationSource(), is(expected.getIdentificationSource()));
    assertThat(actual.getIdentificationSourceComment(), is(expected.getIdentificationSourceComment()));
  }

  private void assertSecurityVulnerability(SecurityVulnerability actual, SecurityVulnerability expected) {
    assertThat(actual.getRefId(), is(expected.getRefId()));
    assertThat(actual.getSeverity(), is(expected.getSeverity()));
    assertThat(actual.getSource(), is(expected.getSource()));
    assertThat(actual.getSummary(), is(expected.getSummary()));
    assertThat(actual.getStatus(), is(expected.getStatus()));
    assertThat(actual.getUrl(), is(expected.getUrl()));
  }
}
