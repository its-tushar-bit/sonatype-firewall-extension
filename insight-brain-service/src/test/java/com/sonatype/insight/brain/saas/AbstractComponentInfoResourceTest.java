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

import javax.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.ide.LicenseStatus;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityStatus;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.saas.ComponentInfoService.ComponentLicenses;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.jetty.util.UrlEncoded;
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
    Response response = AuthedRestAccess.get(getLicensesServiceURL(applicationPublicId, "ulg", "ula", "ulv"));
    assertResponseStatus(402, response);
  }

  @Test
  public void testGetComponentDetailsList_Unlicensed() throws Exception {
    uninstallLicense();
    Response response = AuthedRestAccess.get(getComponentDetailsListUrl(applicationPublicId, "ulg", "ula", "ulv"));
    assertResponseStatus(402, response);
  }

  @Test
  public void testGetComponentDetails_Unlicensed() throws Exception {
    uninstallLicense();
    Response response = AuthedRestAccess.get(getComponentDetailsUrl(applicationPublicId, "ulg", "ula", "ulv", "ulh",
        "unknown"));
    assertResponseStatus(402, response);
  }

  private String getComponentDetailsUrl(String applicationPublicId, String groupId, String artifactId, String version,
      String hash, String matchState)
  {
    return getComponentDetailsUrl(applicationPublicId, groupId, artifactId, version, hash, matchState, null);
  }

  private String getComponentDetailsUrl(String applicationPublicId, String groupId, String artifactId, String version,
      String hash, String matchState, String proprietary)
  {
    return getComponentDetailsUrl(applicationPublicId, getComponentIdentifierParam(groupId, artifactId, version), hash,
        matchState, proprietary);
  }

  private String getComponentDetailsUrl(String applicationPublicId, ComponentIdentifier componentIdentifier,
      String hash, String matchState, String proprietary)
  {
    return getComponentDetailsUrl(applicationPublicId, getComponentIdentifierParam(componentIdentifier), hash,
        matchState, proprietary);
  }

  private String getComponentDetailsUrl(String applicationPublicId, String identifier, String hash,
      String matchState, String proprietary)
  {
    UriBuilder builder = UriBuilder.fromUri(getServiceURL());
    builder.path("{appId}");
    if (identifier != null) {
      builder.queryParam("componentIdentifier", identifier);
    }
    if (hash != null) {
      builder.queryParam("hash", hash);
    }
    if (matchState != null) {
      builder.queryParam("matchState", matchState);
    }
    if (proprietary != null) {
      builder.queryParam("proprietary", proprietary);
    }
    return builder.build(applicationPublicId).toString();
  }

  private String getComponentDetailsListUrl(String applicationPublicId, String g, String a, String v) {
    return getServiceURL() + "/" + applicationPublicId + "/list?componentIdentifier="
        + getComponentIdentifierParam(g, a, v);
  }

  private String getComponentDetailsListUrl(String applicationPublicId, ComponentIdentifier componentIdentifier) {
    return getServiceURL() + "/" + applicationPublicId + "/list?componentIdentifier="
        + getComponentIdentifierParam(componentIdentifier);
  }

  private String getLicensesServiceURL(String applicationPublicId, String g, String a, String v) {
    return getLicensesServiceURL(applicationPublicId) + "?componentIdentifier=" + getComponentIdentifierParam(g, a, v);
  }

  private String getLicensesServiceURL(String applicationPublicId, ComponentIdentifier componentIdentifier) {
    return getLicensesServiceURL(applicationPublicId) + "?componentIdentifier="
        + getComponentIdentifierParam(componentIdentifier);
  }

  private String getLicensesServiceURL(String applicationPublicId) {
    return getServiceURL() + "/licenses/" + applicationPublicId;
  }

  private String getComponentIdentifierParam(String g, String a, String v) {
    return getComponentIdentifierParam(ComponentIdentifier.createMavenCoordinates(g, a, v));
  }

  private String getComponentIdentifierParam(ComponentIdentifier componentIdentifier) {
    return UrlEncoded.encodeString(toJson(componentIdentifier));
  }

  private String getServiceURL() {
    return getRestBaseUrl() + getResourcePath();
  }

  protected abstract String getToolName();

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
    setSaasResponseForURI(getSaasComponentDetailsUrl(MAVEN_COORDINATES), toJson(saasComponentDetails), 200);

    Response response = AuthedRestAccess.get(getLicensesServiceURL(applicationPublicId, MAVEN_COORDINATES));
    assertResponseStatus(200, response);
    ComponentLicenses licenses = fromJson(response, ComponentLicenses.class);
    assertThat(licenses.declaredlicenses, hasSize(1));
    assertThat(licenses.declaredlicenses.get(0).license.getLicenseId(), is("Apache-2.0"));
    assertThat(licenses.observedlicenses, hasSize(0));
  }

  protected void testGetComponentDetails_EvaluateComponentPermission() throws Exception {
    String hash = "01234567890123456789";

    String serviceUrl = getComponentDetailsUrl(applicationPublicId, MAVEN_COORDINATES, hash,
        MatchState.SIMILAR.getId(), "false" /* proprietary */);
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);

    ComponentDetails componentDetails = fromJson(response, TestNamedComponentDetails.class);
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
    setSaasResponseForURI(
        convertToSaasUrl(getComponentDetailsListUrl(applicationPublicId, MAVEN_COORDINATES), applicationPublicId),
        toJson(saasComponentDetailsList), 200);

    String serviceUrl = getComponentDetailsListUrl(applicationPublicId, MAVEN_COORDINATES);
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);

    ComponentDetailsList componentDetailsList = fromJson(response, TestComponentDetailsList.class);
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

    String serviceUrl = getComponentDetailsUrl(applicationPublicId, MAVEN_COORDINATES, hash,
        MatchState.SIMILAR.getId(), "false" /* proprietary */) + "&reportId=" + reportId;
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);

    ComponentDetails componentDetails = fromJson(response, TestNamedComponentDetails.class);
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
    setSaasResponseForURI(
        convertToSaasUrl(getComponentDetailsListUrl(applicationPublicId, MAVEN_COORDINATES), applicationPublicId),
        toJson(saasComponentDetailsList), 200);

    String serviceUrl = getComponentDetailsListUrl(applicationPublicId, MAVEN_COORDINATES) + "&reportId=" + reportId;
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);

    ComponentDetailsList componentDetailsList = fromJson(response, TestComponentDetailsList.class);
    assertThat(componentDetailsList, is(notNullValue()));
    assertThat(componentDetailsList.getList(), hasSize(1));
    ComponentDetails componentDetails = componentDetailsList.getList().get(0);
    assertComponentDetails(componentDetails, saasComponentDetails);
  }

  private String getSaasComponentDetailsUrl(ComponentIdentifier componentIdentifier) {
    return "rest/" + getToolName() + "/componentDetails?componentIdentifier="
        + getComponentIdentifierParam(componentIdentifier);
  }

  private String convertToSaasUrl(String brainUrl, String applicationId) {
    return brainUrl.replaceFirst("/rest/[^/]+/", "/rest/" + getToolName() + "/").substring(getRestBaseUrl().length())
        .replace("/" + applicationId, "").replace("component/details", "componentDetails");
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
