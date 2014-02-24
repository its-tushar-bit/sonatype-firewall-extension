/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.ide.ComponentDetails;
import com.sonatype.clm.dto.model.ide.ComponentDetailsList;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.HashGAVDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.HashGAV;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ProprietaryConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.saas.AbstractComponentInfoResource.ComponentLicenses;
import com.sonatype.insight.brain.saas.AbstractComponentInfoResource.LicenseWithThreatLevel;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public abstract class AbstractComponentInfoResourceTest
    extends AbstractResourceTest
{
  protected abstract String getResourcePath();

  @Before
  public void clearEnforcementPointsFromLicense() throws Exception {
    /*
     * License restrictions on enforcement points are checked when uploading scan data, report data retrieval is
     * permitted with any valid license, so these tests should not require any enforcement point in the license.
     */
    setEnforcementPoints();
  }

  @Test
  public void testGetSelectableLicenses_Unlicensed() throws Exception {
    uninstallLicense();
    Response response = AuthedRestAccess.get(getSelectableLicensesServiceURL("unlicensedappid", "ulg", "ula", "ulv"));
    assertResponseStatus(402, response);
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
    setSaasResponseForURI(getSaasComponentDetailsUrl(groupId, artifactId, version), toJson(saasComponentDetails), 200);
    Response response = AuthedRestAccess.get(getSelectableLicensesServiceURL(applicationPublicId, groupId, artifactId,
        version));
    assertResponseStatus(200, response);
    License[] licenses = fromJson(response, License[].class);
    assertEquals(1, licenses.length);
    assertEquals("EPL-1.0", licenses[0].getLicenseId());

    // Verify that a versionless license is resolved to versioned licenses
    saasComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-UNSPECIFIED"));
    setSaasResponseForURI(getSaasComponentDetailsUrl(groupId, artifactId, version), toJson(saasComponentDetails), 200);
    response = AuthedRestAccess.get(getSelectableLicensesServiceURL(applicationPublicId, groupId, artifactId, version));
    assertResponseStatus(200, response);
    licenses = fromJson(response, License[].class);
    assertEquals(Arrays.asList(licenses).toString(), 4, licenses.length);
    assertContainsLicenseId("Apache-UNSPECIFIED", licenses);
    assertContainsLicenseId("Apache-1.0", licenses);
    assertContainsLicenseId("Apache-1.1", licenses);
    assertContainsLicenseId("Apache-2.0", licenses);

    // Verify that declared and observed licenses are merged
    saasComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-2.0", "EPL-1.0"));
    saasComponentDetails.setObservedLicenses(toLicenseSet("EPL-1.0", "GPL-2.0"));
    setSaasResponseForURI(getSaasComponentDetailsUrl(groupId, artifactId, version), toJson(saasComponentDetails), 200);
    response = AuthedRestAccess.get(getSelectableLicensesServiceURL(applicationPublicId, groupId, artifactId, version));
    assertResponseStatus(200, response);
    licenses = fromJson(response, License[].class);
    assertEquals(Arrays.asList(licenses).toString(), 3, licenses.length);
    assertContainsLicenseId("Apache-2.0", licenses);
    assertContainsLicenseId("EPL-1.0", licenses);
    assertContainsLicenseId("GPL-2.0", licenses);
  }

  @Test
  public void testGetLicenses_Unlicensed() throws Exception {
    uninstallLicense();
    Response response = AuthedRestAccess.get(getLicensesServiceURL("unlicensedappid", "ulg", "ula", "ulv"));
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
    setSaasResponseForURI(getSaasComponentDetailsUrl(groupId, artifactId, version), toJson(saasComponentDetails), 200);
    Response response = AuthedRestAccess.get(getLicensesServiceURL(applicationPublicId, groupId, artifactId, version));
    assertResponseStatus(200, response);
    ComponentLicenses licenses = fromJson(response, ComponentLicenses.class);
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
    setSaasResponseForURI(getSaasComponentDetailsUrl(groupId, artifactId, version), toJson(saasComponentDetails), 200);
    response = AuthedRestAccess.get(getLicensesServiceURL(applicationPublicId, groupId, artifactId, version));
    assertResponseStatus(200, response);
    licenses = fromJson(response, ComponentLicenses.class);
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
    ComponentLicenses licenses = fromJson(response, ComponentLicenses.class);
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
  public void testGetComponentDetailsList_Unlicensed() throws Exception {
    uninstallLicense();
    Response response = AuthedRestAccess.get(getComponentDetailsListUrl("unlicensedappid", "ulg", "ula", "ulv"));
    assertResponseStatus(402, response);
  }

  @Test
  public void testGetComponentDetailsList() throws Exception {
    // Create an application
    Organization organization = tempEntity
        .newOrganization("testGetComponentDetailsList", false /* createLicenseThreatGroups */);
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
        + "&version=" + version, toJson(saasComponentDetailsList), 200);

    String serviceUrl = getComponentDetailsListUrl(applicationPublicId, groupId, artifactId, version);
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);

    ComponentDetailsList componentDetailsList = fromJson(response, ComponentDetailsList.class);
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

  @Test
  public void testGetComponentDetails_Unlicensed() throws Exception {
    uninstallLicense();
    Response response = AuthedRestAccess.get(getComponentDetailsUrl("unlicensedappId", "ulg", "ula", "ulv", "ulh",
        "unknown"));
    assertResponseStatus(402, response);
  }

  @Test
  public void testGetComponentDetails_PolicyAlerts() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(applicationPublicId);
    Label label = new Label(application.getId(), "white", null);
    new LabelDAO().insert(label);
    new ComponentLabelDAO().insert(new ComponentLabel(application.getId(), label.getId(), "01234567890123456789"));

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    Condition condition1 = new Condition(SecurityVulnerabilityConditionType.ID, "present");
    constraint1.addCondition(condition1);
    Policy policy1 = new Policy("PolicyId1", "Policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    Action failAction = new Action(FailActionType.ID);
    policy1.addAction(BuildStageType.ID, failAction);
    addPolicy(applicationPublicId, policy1);

    Constraint constraint2 = new Constraint("C2", "Constraint 2", LogicalOperator.AND);
    constraint2.addCondition(new Condition(MatchStateConditionType.ID, "is not", "similar"));
    Constraint constraint3 = new Constraint("C3", "Constraint 3", LogicalOperator.AND);
    constraint3.addCondition(new Condition(LabelConditionType.ID, "is not", label.getId()));
    Policy policy2 = new Policy("PolicyId2", "Policy2");
    policy2.setThreatLevel(8);
    policy2.addConstraint(constraint2);
    policy2.addConstraint(constraint3);
    policy2.addAction(BuildStageType.ID, failAction);
    addPolicy(applicationPublicId, policy2);

    String groupId = "g1";
    String artifactId = "a1";
    String version = "v1";
    String serviceUrl = getComponentDetailsUrl(applicationPublicId, groupId, artifactId, version,
        "01234567890123456789", "similar");
    String saasUrl = convertToSaasUrl(serviceUrl, applicationPublicId);
    ComponentDetails saasComponentDetails = new ComponentDetails(groupId, artifactId, version);
    saasComponentDetails.addSecurityVulnerability(new SecurityVulnerability("Test Ref Id", "Test Source", 7.5F));
    setSaasResponseForURI(saasUrl, toJson(saasComponentDetails), 200);
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);

    ComponentDetails componentDetails = fromJson(response, ComponentDetails.class);
    Assert.assertNotNull(componentDetails);
    Assert.assertEquals(groupId, componentDetails.getGroupId());
    Assert.assertEquals(artifactId, componentDetails.getArtifactId());
    Assert.assertEquals(version, componentDetails.getVersion());
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    Assert.assertEquals("Policy1", policyAlerts.get(0).getTrigger().getPolicyName());
  }

  @Test
  public void testGetComponentDetails_OverriddenLicense() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(applicationPublicId);

    LicenseOverride licenseOverride = new LicenseOverride(application.getId(), "g1", "a1", "v1",
        LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0", null /* comment */);
    new LicenseOverrideDAO().insert(licenseOverride);

    String groupId = "g1";
    String artifactId = "a1";
    String version = "v1";
    String serviceUrl = getComponentDetailsUrl(applicationPublicId, groupId, artifactId, version, null, null);
    String saasUrl = convertToSaasUrl(serviceUrl, applicationPublicId);
    ComponentDetails saasComponentDetails = new ComponentDetails(groupId, artifactId, version);
    setSaasResponseForURI(saasUrl, toJson(saasComponentDetails), 200);
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);

    ComponentDetails componentDetails = fromJson(response, ComponentDetails.class);
    Assert.assertNotNull(componentDetails);
    Assert.assertEquals(groupId, componentDetails.getGroupId());
    Assert.assertEquals(artifactId, componentDetails.getArtifactId());
    Assert.assertEquals(version, componentDetails.getVersion());
    Assert.assertEquals(1, componentDetails.getOverriddenLicenses().size());
    License overriddenLicense = componentDetails.getOverriddenLicenses().iterator().next();
    Assert.assertNotNull(overriddenLicense);
    Assert.assertEquals("GPL-2.0", overriddenLicense.getLicenseId());
    Assert.assertEquals("GPL-2.0", overriddenLicense.getLicenseName());
    Assert.assertEquals(new Integer(9), componentDetails.getLicenseThreatLevel());
  }

  @Test
  public void testGetComponentDetails_OverriddenSecurityVulnerabilityStatus() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(applicationPublicId);

    setSecurityAuditLog(application.getId(),
        "/AbstractComponentInfoResourceTest/SecurityOverride_abababababababababab.json");

    String groupId = "g1";
    String artifactId = "a1";
    String version = "v1";
    String serviceUrl = getComponentDetailsUrl(applicationPublicId, groupId, artifactId, version, null, null);
    String saasUrl = convertToSaasUrl(serviceUrl, applicationPublicId);
    ComponentDetails saasComponentDetails = new ComponentDetails(groupId, artifactId, version);
    saasComponentDetails.addSecurityVulnerability(new SecurityVulnerability("36079", "osvdb", 7.5F, "Summary"));
    setSaasResponseForURI(saasUrl, toJson(saasComponentDetails), 200);
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);

    ComponentDetails componentDetails = fromJson(response, ComponentDetails.class);
    Assert.assertNotNull(componentDetails);
    Assert.assertEquals(groupId, componentDetails.getGroupId());
    Assert.assertEquals(artifactId, componentDetails.getArtifactId());
    Assert.assertEquals(version, componentDetails.getVersion());
    Assert.assertEquals(1, componentDetails.getSecurityVulnerabilities().size());
    Assert.assertEquals("36079", componentDetails.getSecurityVulnerabilities().get(0).getRefId());
    Assert.assertEquals("osvdb", componentDetails.getSecurityVulnerabilities().get(0).getSource());
    Assert.assertEquals(7.5F, componentDetails.getSecurityVulnerabilities().get(0).getSeverity(), 0.1);
    Assert.assertEquals("Summary", componentDetails.getSecurityVulnerabilities().get(0).getSummary());
    Assert.assertEquals("Acknowledged", componentDetails.getSecurityVulnerabilities().get(0).getStatus());
  }

  @Test
  public void testGetComponentDetails_UnknownComponent() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    tempEntity.newApplicationWithParent(applicationPublicId);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(MatchStateConditionType.ID, "is", "unknown"));
    Policy policy1 = new Policy("PolicyId1", "Policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    Action failAction = new Action(FailActionType.ID);
    policy1.addAction(BuildStageType.ID, failAction);
    addPolicy(applicationPublicId, policy1);

    String groupId = "ug1";
    String artifactId = "ua1";
    String version = "uv1";
    String serviceUrl = getComponentDetailsUrl(applicationPublicId, groupId, artifactId, version,
        "01234567890123456789", "unknown");
    setSaasResponseForURI(convertToSaasUrl(serviceUrl, applicationPublicId), "unknown GAV", 404);
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);

    ComponentDetails componentDetails = fromJson(response, ComponentDetails.class);
    Assert.assertNotNull(componentDetails);
    Assert.assertEquals(groupId, componentDetails.getGroupId());
    Assert.assertEquals(artifactId, componentDetails.getArtifactId());
    Assert.assertEquals(version, componentDetails.getVersion());
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    Assert.assertEquals("Policy1", policyAlerts.get(0).getTrigger().getPolicyName());

    serviceUrl = getComponentDetailsUrl(applicationPublicId, "", "", "", "01234567890123456789", "unknown");
    setSaasResponseForURI(convertToSaasUrl(serviceUrl, applicationPublicId), "unknown GAV", 404);
    response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);

    componentDetails = fromJson(response, ComponentDetails.class);
    Assert.assertNotNull(componentDetails);
    Assert.assertEquals("", componentDetails.getGroupId());
    Assert.assertEquals("", componentDetails.getArtifactId());
    Assert.assertEquals("", componentDetails.getVersion());
    policyAlerts = componentDetails.getPolicyAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    Assert.assertEquals("Policy1", policyAlerts.get(0).getTrigger().getPolicyName());
  }

  @Test
  public void testGetComponentDetails_AppIdWithUnsafeCharacters() throws Exception {
    String applicationPublicId = "bom 1&2%20?";
    tempEntity.newApplicationWithParent(applicationPublicId);

    String groupId = "ug1";
    String artifactId = "ua1";
    String version = "uv1";
    String serviceUrl = getComponentDetailsUrl(applicationPublicId, groupId, artifactId, version,
        "01234567890123456789", "unknown");
    ComponentDetails saasComponentDetails = new ComponentDetails(groupId, artifactId, version);
    setSaasResponseForURI(convertToSaasUrl(serviceUrl, applicationPublicId), toJson(saasComponentDetails), 200);
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);

    ComponentDetails componentDetails = fromJson(response, ComponentDetails.class);
    Assert.assertNotNull(componentDetails);
    Assert.assertEquals(groupId, componentDetails.getGroupId());
    Assert.assertEquals(artifactId, componentDetails.getArtifactId());
    Assert.assertEquals(version, componentDetails.getVersion());
  }

  @Test
  public void testGetComponentDetails_ProprietaryComponent() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    tempEntity.newApplicationWithParent(applicationPublicId);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(ProprietaryConditionType.ID, "is true"));
    Policy policy1 = new Policy("PolicyId1", "Policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.addAction(BuildStageType.ID, new Action(FailActionType.ID));
    addPolicy(applicationPublicId, policy1);

    String groupId = "g1";
    String artifactId = "a1";
    String version = "v1";
    String serviceUrl = getComponentDetailsUrl(applicationPublicId, groupId, artifactId, version,
        "01234567890123456789", "similar", "true");
    String saasUrl = convertToSaasUrl(serviceUrl, applicationPublicId);
    ComponentDetails saasComponentDetails = new ComponentDetails(groupId, artifactId, version);
    setSaasResponseForURI(saasUrl, toJson(saasComponentDetails), 200);
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);

    ComponentDetails componentDetails = fromJson(response, ComponentDetails.class);
    Assert.assertNotNull(componentDetails);
    Assert.assertEquals(groupId, componentDetails.getGroupId());
    Assert.assertEquals(artifactId, componentDetails.getArtifactId());
    Assert.assertEquals(version, componentDetails.getVersion());
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    Assert.assertEquals("Policy1", policyAlerts.get(0).getTrigger().getPolicyName());

    serviceUrl = getComponentDetailsUrl(applicationPublicId, groupId, artifactId, version, "01234567890123456789",
        "similar", "false");
    response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);
    componentDetails = fromJson(response, ComponentDetails.class);
    Assert.assertNotNull(componentDetails);
    Assert.assertEquals(groupId, componentDetails.getGroupId());
    Assert.assertEquals(artifactId, componentDetails.getArtifactId());
    Assert.assertEquals(version, componentDetails.getVersion());
    policyAlerts = componentDetails.getPolicyAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(0, policyAlerts.size());
  }

  @Test
  public void testGetComponentDetails_ManuallyIdentifiedComponent() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    tempEntity.newApplicationWithParent(applicationPublicId);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(MatchStateConditionType.ID, "is", "exact"));
    constraint1.addCondition(new Condition(AgeInDaysConditionType.ID, "younger than", "30"));
    Policy policy1 = new Policy("PolicyId1", "Policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.addAction(BuildStageType.ID, new Action(FailActionType.ID));
    addPolicy(applicationPublicId, policy1);

    String hash = "01234567890123456789";
    String groupId = "g1";
    String artifactId = "a1";
    String version = "v1";
    String serviceUrl = getComponentDetailsUrl(applicationPublicId, groupId, artifactId, version, hash,
        MatchState.SIMILAR.getId(), "false" /* proprietary */);
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);

    ComponentDetails componentDetails = fromJson(response, ComponentDetails.class);
    Assert.assertNotNull(componentDetails);
    Assert.assertEquals(hash, componentDetails.getHash());
    Assert.assertEquals(groupId, componentDetails.getGroupId());
    Assert.assertEquals(artifactId, componentDetails.getArtifactId());
    Assert.assertEquals(version, componentDetails.getVersion());
    Assert.assertEquals(MatchState.SIMILAR.getId(), componentDetails.getMatchState());
    Assert.assertEquals(IdentificationSource.SONATYPE.getId(), componentDetails.getIdentificationSource());
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(0, policyAlerts.size());

    HashGAV hashGAV = new HashGAV(hash, "Claimed" + groupId, "Claimed" + artifactId, "Claimed" + version,
        null /* extension */, null /* classifier */);
    hashGAV.setComment("ClaimedComment");
    hashGAV.setCreateTime(new Date());
    HashGAVDAO hashGAVDAO = new HashGAVDAO();
    hashGAVDAO.insert(hashGAV);
    response = AuthedRestAccess.get(serviceUrl);
    hashGAVDAO.delete(hashGAV);
    assertResponseStatus(200, response);
    componentDetails = fromJson(response, ComponentDetails.class);
    Assert.assertNotNull(componentDetails);
    Assert.assertEquals(hash, componentDetails.getHash());
    Assert.assertEquals("Claimed" + groupId, componentDetails.getGroupId());
    Assert.assertEquals("Claimed" + artifactId, componentDetails.getArtifactId());
    Assert.assertEquals("Claimed" + version, componentDetails.getVersion());
    Assert.assertEquals(MatchState.EXACT.getId(), componentDetails.getMatchState());
    Assert.assertEquals(IdentificationSource.MANUAL.getId(), componentDetails.getIdentificationSource());
    Assert.assertEquals("ClaimedComment", componentDetails.getIdentificationSourceComment());
    policyAlerts = componentDetails.getPolicyAlerts();
    Assert.assertEquals(1, policyAlerts.size());
    Assert.assertEquals("Policy1", policyAlerts.get(0).getTrigger().getPolicyName());
  }

  @Test
  public void testGetComponentDetails_Label_DefinedAtAppLevel() throws Exception {
    testGetComponentDetails_Label(false, false);
  }

  @Test
  public void testGetComponentDetails_Label_DefinedAtOrgLevel_AppliedAtOrgLevel() throws Exception {
    testGetComponentDetails_Label(true, true);
  }

  @Test
  public void testGetComponentDetails_Label_DefinedAtOrgLevel_AppliedAtAppLevel() throws Exception {
    testGetComponentDetails_Label(true, false);
  }

  private void testGetComponentDetails_Label(boolean orgLabel, boolean orgComponentLabel) throws Exception {
    String hash = "01234567890123456789";
    String applicationPublicId = "IdeResourceTest_AppId";
    Application app = tempEntity.newApplicationWithParent(applicationPublicId);
    Label label = new Label(orgLabel ? app.getOrganizationId() : app.getId(), "red", null);
    new LabelDAO().insert(label);
    new ComponentLabelDAO().insert(new ComponentLabel(orgComponentLabel ? app.getOrganizationId() : app.getId(), label
        .getId(), hash));

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(LabelConditionType.ID, "is", label.getId()));
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    Action failAction = new Action(FailActionType.ID);
    policy1.addAction(BuildStageType.ID, failAction);
    addPolicy(applicationPublicId, policy1);

    String groupId = "g1";
    String artifactId = "a1";
    String version = "v1";
    String serviceUrl = getComponentDetailsUrl(applicationPublicId, groupId, artifactId, version, hash,
        MatchState.SIMILAR.getId(), "false" /* proprietary */);
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);

    ComponentDetails componentDetails = fromJson(response, ComponentDetails.class);
    Assert.assertThat(componentDetails, is(notNullValue()));
    Assert.assertThat(componentDetails.getHash(), is(hash));
    Assert.assertThat(componentDetails.getGroupId(), is(groupId));
    Assert.assertThat(componentDetails.getArtifactId(), is(artifactId));
    Assert.assertThat(componentDetails.getVersion(), is(version));
    Assert.assertThat(componentDetails.getMatchState(), is(MatchState.SIMILAR.getId()));
    Assert.assertThat(componentDetails.getIdentificationSource(), is(IdentificationSource.SONATYPE.getId()));
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    Assert.assertThat(policyAlerts, is(notNullValue()));
    Assert.assertThat(policyAlerts.size(), is(1));
  }

  private void addPolicy(String applicationPublicId, Policy policy) throws Exception {
    String appId = new ApplicationDAO().getByPublicIdNotNull(applicationPublicId).getId();
    PolicyDAO policyDAO = new PolicyDAO();
    policy.setOwnerId(appId);
    policyDAO.insert(policy);
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

  private String convertToSaasUrl(String brainUrl, String applicationId) {
    return brainUrl.replaceFirst("/rest/[^/]+/", "/rest/ide/").substring(getRestBaseUrl().length())
        .replace("/" + applicationId, "");
  }

  private String getComponentDetailsUrl(String applicationPublicId, String groupId, String artifactId, String version,
      String hash, String matchState)
  {
    return getComponentDetailsUrl(applicationPublicId, groupId, artifactId, version, hash, matchState, null);
  }

  private String getComponentDetailsUrl(String applicationPublicId, String groupId, String artifactId, String version,
      String hash, String matchState, String proprietary)
  {
    UriBuilder builder = UriBuilder.fromUri(getServiceURL());
    builder.path("{appId}");
    builder.queryParam("groupId", groupId);
    builder.queryParam("artifactId", artifactId);
    builder.queryParam("version", version);
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
    return getRestBaseUrl() + getResourcePath();
  }
}
