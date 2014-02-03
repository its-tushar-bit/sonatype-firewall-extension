/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.ide.ComponentDetails;
import com.sonatype.clm.dto.model.ide.ComponentDetailsList;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.saas.AbstractComponentInfoResource.ComponentLicenses;
import com.sonatype.insight.brain.saas.AbstractComponentInfoResource.LicenseWithThreatLevel;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class CIComponentInfoResourceTest
    extends AbstractResourceTest
{
  @Before
  public void clearEnforcementPointsFromLicense() throws Exception {
    /*
     * License restrictions on enforcement points are checked when uploading scan data, report data retrieval is
     * permitted with any valid license, so these tests should not require any enforcement point in the license.
     */
    setEnforcementPoints();
  }

  @Test
  public void testGetSelectableLicenses() throws Exception {
    String applicationPublicId = "ComponentInfoResourceTest";
    tempEntity.newApplicationWithParent(applicationPublicId);

    String groupId = "g1";
    String artifactId = "a1";
    String version = "v1";
    ComponentDetails saasComponentDetails = new ComponentDetails(groupId, artifactId, version);

    // Verify that UNSPECIFIED is removed from the result
    saasComponentDetails.setDeclaredLicenses(toLicenseSet("EPL-1.0", "UNSPECIFIED"));
    setSaasResponseForURI(getSaasComponentDetailsUrl(groupId, artifactId, version),
        JsonHelpers.asJson(saasComponentDetails), 200);
    Response response = AuthedRestAccess.get(getSelectableLicensesServiceURL(applicationPublicId, groupId, artifactId,
        version));
    assertResponseStatus(200, response);
    License[] licenses = JsonHelpers.fromJson(response.getResponseBody(), License[].class);
    assertEquals(1, licenses.length);
    assertEquals("EPL-1.0", licenses[0].getLicenseId());

    // Verify that a versionless license is resolved to versioned licenses
    saasComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-UNSPECIFIED"));
    setSaasResponseForURI(getSaasComponentDetailsUrl(groupId, artifactId, version),
        JsonHelpers.asJson(saasComponentDetails), 200);
    response = AuthedRestAccess.get(getSelectableLicensesServiceURL(applicationPublicId, groupId, artifactId, version));
    assertResponseStatus(200, response);
    licenses = JsonHelpers.fromJson(response.getResponseBody(), License[].class);
    assertEquals(Arrays.asList(licenses).toString(), 4, licenses.length);
    assertContainsLicenseId("Apache-UNSPECIFIED", licenses);
    assertContainsLicenseId("Apache-1.0", licenses);
    assertContainsLicenseId("Apache-1.1", licenses);
    assertContainsLicenseId("Apache-2.0", licenses);

    // Verify that declared and observed licenses are merged
    saasComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-2.0", "EPL-1.0"));
    saasComponentDetails.setObservedLicenses(toLicenseSet("EPL-1.0", "GPL-2.0"));
    setSaasResponseForURI(getSaasComponentDetailsUrl(groupId, artifactId, version),
        JsonHelpers.asJson(saasComponentDetails), 200);
    response = AuthedRestAccess.get(getSelectableLicensesServiceURL(applicationPublicId, groupId, artifactId, version));
    assertResponseStatus(200, response);
    licenses = JsonHelpers.fromJson(response.getResponseBody(), License[].class);
    assertEquals(Arrays.asList(licenses).toString(), 3, licenses.length);
    assertContainsLicenseId("Apache-2.0", licenses);
    assertContainsLicenseId("EPL-1.0", licenses);
    assertContainsLicenseId("GPL-2.0", licenses);
  }

  @Test
  public void testGetSelectableLicenses_Unlicensed() throws Exception {
    uninstallLicense();
    Response response = AuthedRestAccess.get(getSelectableLicensesServiceURL("unlicensedappid", "ulg", "ula", "ulv"));
    assertResponseStatus(402, response);
  }

  @Test
  public void testGetLicenses() throws Exception {
    String applicationPublicId = "ComponentInfoResourceTest";
    Application application = tempEntity.newApplicationWithParent(applicationPublicId);

    String groupId = "g1";
    String artifactId = "a1";
    String version = "v1";
    ComponentDetails saasComponentDetails = new ComponentDetails(groupId, artifactId, version);

    // Verify component without licenses
    setSaasResponseForURI(getSaasComponentDetailsUrl(groupId, artifactId, version),
        JsonHelpers.asJson(saasComponentDetails), 200);
    Response response = AuthedRestAccess.get(getLicensesServiceURL(applicationPublicId, groupId, artifactId, version));
    assertResponseStatus(200, response);
    ComponentLicenses licenses = JsonHelpers.fromJson(response.getResponseBody(), ComponentLicenses.class);
    assertThat(licenses.declaredlicenses, empty());
    assertThat(licenses.observedlicenses, empty());

    // Verify component with licenses
    LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();
    LicenseThreatGroup licenseThreatGroup = new LicenseThreatGroup(application.getId(), "ComponentInfoResourceTest", 5 /* threatLevel */);
    licenseThreatGroupDAO.insert(licenseThreatGroup);
    LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();
    licenseThreatGroupLicenseDAO.setLicenses(licenseThreatGroup.getId(), toLicenseIdSet("LGPL-2.0", "BSD-3-Clause"));

    saasComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-2.0", "LGPL-2.0-MPL-1.1"));
    saasComponentDetails.setObservedLicenses(toLicenseSet("GPL-2.0", "AFL-2.1-BSD-3-Clause"));
    setSaasResponseForURI(getSaasComponentDetailsUrl(groupId, artifactId, version),
        JsonHelpers.asJson(saasComponentDetails), 200);
    response = AuthedRestAccess.get(getLicensesServiceURL(applicationPublicId, groupId, artifactId, version));
    assertResponseStatus(200, response);
    licenses = JsonHelpers.fromJson(response.getResponseBody(), ComponentLicenses.class);
    assertThat(licenses.declaredlicenses, hasSize(3));
    assertContainsLicenseWithThreatLevel("Apache-2.0", "Apache-2.0", 0, licenses.declaredlicenses);
    assertContainsLicenseWithThreatLevel("LGPL-2.0", "LGPL-2.0", 5, licenses.declaredlicenses);
    assertContainsLicenseWithThreatLevel("MPL-1.1", "MPL-1.1", 2, licenses.declaredlicenses);
    assertThat(licenses.observedlicenses, hasSize(3));
    assertContainsLicenseWithThreatLevel("GPL-2.0", "GPL-2.0", 9, licenses.observedlicenses);
    assertContainsLicenseWithThreatLevel("AFL-2.1", "AFL-2.1", 2, licenses.observedlicenses);
    assertContainsLicenseWithThreatLevel("BSD-3-Clause", "BSD-3-Clause", 5, licenses.observedlicenses);
  }

  @Test
  public void testGetLicenses_claimedComponent() throws Exception {
    String applicationPublicId = "ComponentInfoResourceTest";
    tempEntity.newApplicationWithParent(applicationPublicId);

    String groupId = "g1";
    String artifactId = "a1";
    String version = "v1";

    // Verify exception is not thrown if component is not known to SaaS
    Response response = AuthedRestAccess.get(getLicensesServiceURL(applicationPublicId, groupId, artifactId, version));
    assertResponseStatus(200, response);
    // if we got here, we are good, but let's do some sanity check 
    ComponentLicenses licenses = JsonHelpers.fromJson(response.getResponseBody(), ComponentLicenses.class);
    assertThat(licenses.declaredlicenses, empty());
    assertThat(licenses.observedlicenses, empty());
  }

  private void assertContainsLicenseWithThreatLevel(String licenseId, String licenseName, Integer threatLevel,
      List<LicenseWithThreatLevel> actual)
  {
    for (LicenseWithThreatLevel licenseWithThreatLevel : actual) {
      if (licenseId.equals(licenseWithThreatLevel.license.getLicenseId())) {
        assertEquals(licenseName, licenseWithThreatLevel.license.getLicenseName());
        assertEquals(threatLevel, licenseWithThreatLevel.threatLevel);
        return;
      }
    }
    fail("Expected license id " + licenseId);
  }

  @Test
  public void testGetLicenses_Unlicensed() throws Exception {
    uninstallLicense();
    Response response = AuthedRestAccess.get(getLicensesServiceURL("unlicensedappid", "ulg", "ula", "ulv"));
    assertResponseStatus(402, response);
  }

  @Test
  public void testGetComponentDetailsList_Unlicensed() throws Exception {
    uninstallLicense();
    Response response = AuthedRestAccess.get(getComponentDetailsListUrl("unlicensedappid", "ulg", "ula", "ulv"));
    assertResponseStatus(402, response);
  }

  @Test
  public void testGetComponentDetailsList() throws Exception {
    // Create an application
    Organization organization = tempEntity.newOrganization("testGetComponentDetailsList", false /* createLicenseThreatGroups */);
    String applicationPublicId = "testGetComponentDetailsList";
    Application application = tempEntity.newApplication(applicationPublicId, applicationPublicId, organization.getId());
    String appId = application.getId();
    // Create license threat groups
    LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();
    LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();
    LicenseThreatGroup licenseThreatGroup = new LicenseThreatGroup(appId, "Group1", 9);
    licenseThreatGroupDAO.insert(licenseThreatGroup);
    Set<String> licenseIds = new LinkedHashSet<String>();
    licenseIds.add("Apache-2.0");
    licenseThreatGroupLicenseDAO.setLicenses(licenseThreatGroup.getId(), licenseIds);
    licenseThreatGroup = new LicenseThreatGroup(appId, "Group2", 1);
    licenseThreatGroupDAO.insert(licenseThreatGroup);
    licenseIds.clear();
    licenseIds.add("GPL-2.0");
    licenseThreatGroupLicenseDAO.setLicenses(licenseThreatGroup.getId(), licenseIds);

    // Create the mocked saas response
    String groupId = "g1";
    String artifactId = "a1";
    String version = "1.0.0";
    ComponentDetails saasComponentDetails1 = new ComponentDetails(groupId, artifactId, version);
    Set<License> licenses1 = new LinkedHashSet<License>();
    licenses1.add(new License("Apache-2.0", "Apache-2.0"));
    saasComponentDetails1.setDeclaredLicenses(licenses1);
    ComponentDetails saasComponentDetails2 = new ComponentDetails(groupId, artifactId, "2.0.0");
    Set<License> licenses2 = new LinkedHashSet<License>();
    licenses2.add(new License("GPL-2.0", "GPL-2.0"));
    saasComponentDetails2.setDeclaredLicenses(licenses2);
    ComponentDetailsList saasComponentDetailsList = new ComponentDetailsList();
    saasComponentDetailsList.setList(Arrays.asList(new ComponentDetails[] { saasComponentDetails1,
        saasComponentDetails2 }));
    setSaasResponseForURI("rest/ide/component/details/list?groupId=" + groupId + "&artifactId=" + artifactId
        + "&version=" + version, JsonHelpers.asJson(saasComponentDetailsList), 200);

    String serviceUrl = getComponentDetailsListUrl(applicationPublicId, groupId, artifactId, version);
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);

    ComponentDetailsList componentDetailsList = JsonHelpers.fromJson(response.getResponseBody(),
        ComponentDetailsList.class);
    Assert.assertNotNull(componentDetailsList);
    Assert.assertEquals(2, componentDetailsList.getList().size());
    ComponentDetails componentDetails = componentDetailsList.getList().get(0);
    Assert.assertEquals(groupId, componentDetails.getGroupId());
    Assert.assertEquals(artifactId, componentDetails.getArtifactId());
    Assert.assertEquals(version, componentDetails.getVersion());
    Assert.assertEquals(new Integer(9), componentDetails.getLicenseThreatLevel());
    componentDetails = componentDetailsList.getList().get(1);
    Assert.assertEquals(groupId, componentDetails.getGroupId());
    Assert.assertEquals(artifactId, componentDetails.getArtifactId());
    Assert.assertEquals("2.0.0", componentDetails.getVersion());
    Assert.assertEquals(new Integer(1), componentDetails.getLicenseThreatLevel());
  }

  private void assertContainsLicenseId(String licenseId, License[] licenses) {
    for (License license : licenses) {
      if (licenseId.equals(license.getLicenseId())) {
        return;
      }
    }
    fail("Expected license id " + licenseId);
  }

  private Set<License> toLicenseSet(String... licenseIds) {
    Set<License> result = new LinkedHashSet<License>();
    MultiLicenseDAO dao = new MultiLicenseDAO();
    for (String licenseId : licenseIds) {
      MultiLicense multiLicense = dao.getByIdNotNull(licenseId);
      result.add(new License(multiLicense.getId(), multiLicense.getShortDisplayName()));
    }
    return result;
  }

  private Set<String> toLicenseIdSet(String... licenseIds) {
    Set<String> result = new LinkedHashSet<String>();
    for (String licenseId : licenseIds) {
      result.add(licenseId);
    }
    return result;
  }

  private String getSaasComponentDetailsUrl(String g, String a, String v) {
    return "/rest/ide/component/details?groupId=" + g + "&artifactId=" + a + "&version=" + v;
  }

  private String getComponentDetailsListUrl(String applicationPublicId, String g, String a, String v) {
    return getServiceURL() + "/list/" + applicationPublicId + "?groupId=" + g + "&artifactId=" + a + "&version=" + v;
  }

  private String getLicensesServiceURL(String applicationPublicId, String g, String a, String v) {
    return getServiceURL() + "/licenses/" + applicationPublicId + "?groupId=" + g + "&artifactId=" + a + "&version="
        + v;
  }

  private String getSelectableLicensesServiceURL(String applicationPublicId, String g, String a, String v) {
    return getServiceURL() + "/selectableLicenses/" + applicationPublicId + "?groupId=" + g + "&artifactId=" + a
        + "&version=" + v;
  }

  private String getServiceURL() {
    return getRestBaseUrl() + CIComponentInfoResource.SERVICE_PATH;
  }
}
