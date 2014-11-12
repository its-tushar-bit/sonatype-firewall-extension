/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.util.ArrayList;
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
import com.sonatype.clm.dto.model.ide.ComponentIdentifier;
import com.sonatype.clm.dto.model.ide.LicenseStatus;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
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
import com.sonatype.insight.mock.UriParamRequestMatcher;

import com.ning.http.client.Response;
import org.eclipse.jetty.util.UrlEncoded;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.clm.dto.model.ide.ComponentIdentifier.MAVEN_ARTIFACT_ID;
import static com.sonatype.clm.dto.model.ide.ComponentIdentifier.MAVEN_GROUP_ID;
import static com.sonatype.clm.dto.model.ide.ComponentIdentifier.VERSION;
import static org.hamcrest.Matchers.contains;
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

  private static final ComponentIdentifier MAVEN_COORDINATES = ComponentIdentifier
    .createMavenCoordinates("g1", "a1", "v1");

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

    String groupId = "g1.testGetSelectableLicenses";
    String artifactId = "a1";
    String version = "v1";
    ComponentDetails saasComponentDetails = newComponentDetailsForMaven(groupId, artifactId, version);

    // Verify that UNSPECIFIED is removed from the result
    saasComponentDetails.setDeclaredLicenses(toLicenseSet("EPL-1.0", "UNSPECIFIED"));
    setSaasResponse(new UriParamRequestMatcher(getSaasComponentDetailsUrl(groupId, artifactId, version),
        toJson(saasComponentDetails), 200));
    Response response = AuthedRestAccess.get(getSelectableLicensesServiceURL(applicationPublicId, groupId, artifactId,
        version));
    assertResponseStatus(200, response);
    License[] licenses = fromJson(response, License[].class);
    assertEquals(1, licenses.length);
    assertEquals("EPL-1.0", licenses[0].getLicenseId());

    // Verify that a versionless license is resolved to versioned licenses
    version = "v2";
    saasComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-UNSPECIFIED"));
    setSaasResponse(new UriParamRequestMatcher(getSaasComponentDetailsUrl(groupId, artifactId, version),
        toJson(saasComponentDetails), 200));
    response = AuthedRestAccess.get(getSelectableLicensesServiceURL(applicationPublicId, groupId, artifactId, version));
    assertResponseStatus(200, response);
    licenses = fromJson(response, License[].class);
    assertEquals(Arrays.asList(licenses).toString(), 4, licenses.length);
    assertContainsLicenseId("Apache-UNSPECIFIED", licenses);
    assertContainsLicenseId("Apache-1.0", licenses);
    assertContainsLicenseId("Apache-1.1", licenses);
    assertContainsLicenseId("Apache-2.0", licenses);

    // Verify that declared and observed licenses are merged
    version = "v3";
    saasComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-2.0", "EPL-1.0"));
    saasComponentDetails.setObservedLicenses(toLicenseSet("EPL-1.0", "GPL-2.0"));
    setSaasResponse(new UriParamRequestMatcher(getSaasComponentDetailsUrl(groupId, artifactId, version),
        toJson(saasComponentDetails), 200));
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
    ComponentDetails saasComponentDetails = newComponentDetailsForMaven(groupId, artifactId, version);

    // Verify component without licenses
    setSaasResponse(new UriParamRequestMatcher(getSaasComponentDetailsUrl(groupId, artifactId, version),
        toJson(saasComponentDetails), 200));
    Response response = AuthedRestAccess.get(getLicensesServiceURL(applicationPublicId, groupId, artifactId, version));
    assertResponseStatus(200, response);
    ComponentLicenses licenses = fromJson(response, ComponentLicenses.class);
    assertThat(licenses.declaredlicenses, empty());
    assertThat(licenses.observedlicenses, empty());
    assertThat(licenses.effectiveLicenses, empty());

    // Verify component with licenses
    tempEntity.newLicenseThreatGroup(application.getId(), "ComponentInfoResourceTest", 5, "LGPL-2.0", "BSD-3-Clause");

    version = "v2"; // avoid matching parameters
    saasComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-2.0", "LGPL-2.0-MPL-1.1"));
    saasComponentDetails.setObservedLicenses(toLicenseSet("GPL-2.0", "AFL-2.1-BSD-3-Clause"));
    setSaasResponse(new UriParamRequestMatcher(getSaasComponentDetailsUrl(groupId, artifactId, version),
        toJson(saasComponentDetails), 200));
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
    assertThat(licenses.effectiveLicenses, hasSize(6));
    List<LicenseWithThreatLevel> effectiveLicensesList = new ArrayList<>(licenses.effectiveLicenses);
    for (LicenseWithThreatLevel licenseWithThreatLevel : licenses.declaredlicenses) {
      assertContainsLicenseWithThreatLevel(licenseWithThreatLevel.license.getLicenseId(),
          licenseWithThreatLevel.license.getLicenseName(), licenseWithThreatLevel.threatLevel, effectiveLicensesList);
    }
    for (LicenseWithThreatLevel licenseWithThreatLevel : licenses.observedlicenses) {
      assertContainsLicenseWithThreatLevel(licenseWithThreatLevel.license.getLicenseId(),
          licenseWithThreatLevel.license.getLicenseName(), licenseWithThreatLevel.threatLevel, effectiveLicensesList);
    }
  }

  @Test
  public void testGetLicenses_withOverride() throws Exception {
    String applicationPublicId = "ComponentInfoResourceTest";
    Application application = tempEntity.newApplicationWithParent(applicationPublicId);

    String groupId = "g1.testGetLicenses_withOverride";
    String artifactId = "a1";
    String version = "v1";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version);

    // Verify component with licenses
    tempEntity.newLicenseThreatGroup(application.getId(), "ComponentInfoResourceTest", 5, "LGPL-2.0", "BSD-3-Clause");
    tempEntity.newLicenseOverride(application.getId(), componentIdentifier, LicenseOverrideStatus.SELECTED,
      "BSD-3-Clause");

    ComponentDetails saasComponentDetails = new ComponentDetails(componentIdentifier);
    saasComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-2.0", "LGPL-2.0-MPL-1.1"));
    saasComponentDetails.setObservedLicenses(toLicenseSet("GPL-2.0", "AFL-2.1-BSD-3-Clause"));
    setSaasResponse(new UriParamRequestMatcher(getSaasComponentDetailsUrl(groupId, artifactId, version),
        toJson(saasComponentDetails), 200));
    Response response = AuthedRestAccess.get(getLicensesServiceURL(applicationPublicId, groupId, artifactId, version));
    assertResponseStatus(200, response);
    ComponentLicenses licenses = fromJson(response, ComponentLicenses.class);
    assertThat(licenses.declaredlicenses, hasSize(3));
    assertContainsLicenseWithThreatLevel("Apache-2.0", "Apache-2.0", 0, licenses.declaredlicenses);
    assertContainsLicenseWithThreatLevel("LGPL-2.0", "LGPL-2.0", 5, licenses.declaredlicenses);
    assertContainsLicenseWithThreatLevel("MPL-1.1", "MPL-1.1", 2, licenses.declaredlicenses);
    assertThat(licenses.observedlicenses, hasSize(3));
    assertContainsLicenseWithThreatLevel("GPL-2.0", "GPL-2.0", 9, licenses.observedlicenses);
    assertContainsLicenseWithThreatLevel("AFL-2.1", "AFL-2.1", 2, licenses.observedlicenses);
    assertContainsLicenseWithThreatLevel("BSD-3-Clause", "BSD-3-Clause", 5, licenses.observedlicenses);
    assertThat(licenses.effectiveLicenses, hasSize(1));
    assertThat(licenses.effectiveLicenses.iterator().next().license.getLicenseId(), is("BSD-3-Clause"));
    assertThat(licenses.effectiveLicenses.iterator().next().license.getLicenseName(), is("BSD-3-Clause"));
  }

  @Test
  public void testGetLicenses_withNotDeclaredForDeclaredLicenses() throws Exception {
    String applicationPublicId = "ComponentInfoResourceTest";
    tempEntity.newApplicationWithParent(applicationPublicId);

    String groupId = "g1.testGetLicenses_withNotDeclaredForDeclaredLicenses";
    String artifactId = "a1";
    String version = "v1";

    ComponentDetails saasComponentDetails = newComponentDetailsForMaven(groupId, artifactId, version);
    saasComponentDetails.setDeclaredLicenses(toLicenseSet("Not-Declared"));
    saasComponentDetails.setObservedLicenses(toLicenseSet("GPL-2.0"));
    setSaasResponse(new UriParamRequestMatcher(getSaasComponentDetailsUrl(groupId, artifactId, version),
        toJson(saasComponentDetails), 200));
    Response response = AuthedRestAccess.get(getLicensesServiceURL(applicationPublicId, groupId, artifactId, version));
    assertResponseStatus(200, response);
    ComponentLicenses licenses = fromJson(response, ComponentLicenses.class);
    assertThat(licenses.declaredlicenses, hasSize(1));
    assertContainsLicenseWithThreatLevel("Not-Declared", "Not Declared", null, licenses.declaredlicenses);
    assertThat(licenses.observedlicenses, hasSize(1));
    assertContainsLicenseWithThreatLevel("GPL-2.0", "GPL-2.0", 9, licenses.observedlicenses);
    assertThat(licenses.effectiveLicenses, hasSize(1));
    List<LicenseWithThreatLevel> effectiveList = new ArrayList<>(licenses.effectiveLicenses);
    assertContainsLicenseWithThreatLevel("GPL-2.0", "GPL-2.0", 9, effectiveList);
  }

  @Test
  public void testGetLicenses_withNoSourcesForObservedLicenses() throws Exception {
    String applicationPublicId = "ComponentInfoResourceTest";
    tempEntity.newApplicationWithParent(applicationPublicId);

    String groupId = "g1.testGetLicenses_withNoSourcesForObservedLicenses";
    String artifactId = "a1";
    String version = "v1";

    ComponentDetails saasComponentDetails = newComponentDetailsForMaven(groupId, artifactId, version);
    saasComponentDetails.setDeclaredLicenses(toLicenseSet("GPL-2.0"));
    saasComponentDetails.setObservedLicenses(toLicenseSet("No-Sources"));
    setSaasResponse(new UriParamRequestMatcher(getSaasComponentDetailsUrl(groupId, artifactId, version),
        toJson(saasComponentDetails), 200));
    Response response = AuthedRestAccess.get(getLicensesServiceURL(applicationPublicId, groupId, artifactId, version));
    assertResponseStatus(200, response);
    ComponentLicenses licenses = fromJson(response, ComponentLicenses.class);
    assertThat(licenses.declaredlicenses, hasSize(1));
    assertContainsLicenseWithThreatLevel("GPL-2.0", "GPL-2.0", 9, licenses.declaredlicenses);
    assertThat(licenses.observedlicenses, hasSize(1));
    assertContainsLicenseWithThreatLevel("No-Sources", "No Sources", null, licenses.observedlicenses);
    assertThat(licenses.effectiveLicenses, hasSize(1));
    List<LicenseWithThreatLevel> effectiveList = new ArrayList<>(licenses.effectiveLicenses);
    assertContainsLicenseWithThreatLevel("GPL-2.0", "GPL-2.0", 9, effectiveList);
  }

  @Test
  public void testGetLicenses_withNoSourceLicenseForObservedLicenses() throws Exception {
    String applicationPublicId = "ComponentInfoResourceTest";
    tempEntity.newApplicationWithParent(applicationPublicId);

    String groupId = "g1.testGetLicenses_withNoSourceLicenseForObservedLicenses";
    String artifactId = "a1";
    String version = "v1";

    ComponentDetails saasComponentDetails = newComponentDetailsForMaven(groupId, artifactId, version);
    saasComponentDetails.setDeclaredLicenses(toLicenseSet("GPL-2.0"));
    saasComponentDetails.setObservedLicenses(toLicenseSet("No-Source-License"));
    setSaasResponse(new UriParamRequestMatcher(getSaasComponentDetailsUrl(groupId, artifactId, version),
        toJson(saasComponentDetails), 200));
    Response response = AuthedRestAccess.get(getLicensesServiceURL(applicationPublicId, groupId, artifactId, version));
    assertResponseStatus(200, response);
    ComponentLicenses licenses = fromJson(response, ComponentLicenses.class);
    assertThat(licenses.declaredlicenses, hasSize(1));
    assertContainsLicenseWithThreatLevel("GPL-2.0", "GPL-2.0", 9, licenses.declaredlicenses);
    assertThat(licenses.observedlicenses, hasSize(1));
    assertContainsLicenseWithThreatLevel("No-Source-License", "No Source License", null, licenses.observedlicenses);
    assertThat(licenses.effectiveLicenses, hasSize(1));
    List<LicenseWithThreatLevel> effectiveList = new ArrayList<>(licenses.effectiveLicenses);
    assertContainsLicenseWithThreatLevel("GPL-2.0", "GPL-2.0", 9, effectiveList);
  }

  @Test
  public void testGetLicenses_withNotDeclaredForDeclaredLicensesAndNoSourcesForObservedLicenses() throws Exception {
    String applicationPublicId = "ComponentInfoResourceTest";
    tempEntity.newApplicationWithParent(applicationPublicId);

    String groupId = "g1.testGetLicenses_withNotDeclaredForDeclaredLicensesAndNoSourcesForObservedLicenses";
    String artifactId = "a1";
    String version = "v1";

    ComponentDetails saasComponentDetails = newComponentDetailsForMaven(groupId, artifactId, version);
    saasComponentDetails.setDeclaredLicenses(toLicenseSet("Not-Declared"));
    saasComponentDetails.setObservedLicenses(toLicenseSet("No-Source-License"));
    setSaasResponse(new UriParamRequestMatcher(getSaasComponentDetailsUrl(groupId, artifactId, version),
        toJson(saasComponentDetails), 200));
    Response response = AuthedRestAccess.get(getLicensesServiceURL(applicationPublicId, groupId, artifactId, version));
    assertResponseStatus(200, response);
    ComponentLicenses licenses = fromJson(response, ComponentLicenses.class);
    assertThat(licenses.declaredlicenses, hasSize(1));
    assertContainsLicenseWithThreatLevel("Not-Declared", "Not Declared", null, licenses.declaredlicenses);
    assertThat(licenses.observedlicenses, hasSize(1));
    assertContainsLicenseWithThreatLevel("No-Source-License", "No Source License", null, licenses.observedlicenses);
    assertThat(licenses.effectiveLicenses, hasSize(2));
    List<LicenseWithThreatLevel> effectiveList = new ArrayList<>(licenses.effectiveLicenses);
    assertContainsLicenseWithThreatLevel("Not-Declared", "Not Declared", null, effectiveList);
    assertContainsLicenseWithThreatLevel("No-Source-License", "No Source License", null, effectiveList);
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

  @SuppressWarnings("deprecation")
  private void assertGavInComponentDetails(String groupId, String artifactId, String version,
      ComponentDetails componentDetails)
  {
    assertThat(componentDetails.getGroupId(), is(groupId));
    assertThat(componentDetails.getArtifactId(), is(artifactId));
    assertThat(componentDetails.getVersion(), is(version));
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
    tempEntity.newLicenseThreatGroup(appId, "Group1", 9, "Apache-2.0");
    // Various LTG groups to test case insensitive ordering
    tempEntity.newLicenseThreatGroup(appId, "groupA", 1, "GPL-2.0");
    tempEntity.newLicenseThreatGroup(appId, "Groupb", 1, "GPL-2.0");
    tempEntity.newLicenseThreatGroup(appId, "GroupC", 1, "GPL-2.0");

    // Create the mocked saas response
    String groupId = "g1";
    String artifactId = "a1";
    String version = "1.0.0";
    ComponentDetails saasComponentDetails1 = newComponentDetailsForMaven(groupId, artifactId, version);
    Set<License> licenses1 = new LinkedHashSet<License>();
    licenses1.add(new License("Apache-2.0", "Apache-2.0"));
    saasComponentDetails1.setDeclaredLicenses(licenses1);
    ComponentDetails saasComponentDetails2 = newComponentDetailsForMaven(groupId, artifactId, "2.0.0");
    Set<License> licenses2 = new LinkedHashSet<License>();
    licenses2.add(new License("GPL-2.0", "GPL-2.0"));
    saasComponentDetails2.setDeclaredLicenses(licenses2);
    ComponentDetailsList saasComponentDetailsList = new ComponentDetailsList();
    saasComponentDetailsList.setList(Arrays.asList(saasComponentDetails1, saasComponentDetails2));
    setSaasResponse(new UriParamRequestMatcher(convertToSaasUrl(
        getComponentDetailsListUrl(applicationPublicId, groupId, artifactId, version), applicationPublicId),
        toJson(saasComponentDetailsList), 200));

    String serviceUrl = getComponentDetailsListUrl(applicationPublicId, groupId, artifactId, version);
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);

    ComponentDetailsList componentDetailsList = fromJson(response, ComponentDetailsList.class);
    Assert.assertNotNull(componentDetailsList);
    Assert.assertEquals(2, componentDetailsList.getList().size());
    ComponentDetails componentDetails = componentDetailsList.getList().get(0);
    assertGavInComponentDetails(groupId, artifactId, version, componentDetails);
    Assert.assertEquals(new Integer(9), componentDetails.getLicenseThreatLevel());
    Assert.assertEquals(1, componentDetails.getLicenseThreatGroupNames().size());
    Assert.assertEquals("Group1", componentDetails.getLicenseThreatGroupNames().get(0));
    Assert.assertEquals(1, componentDetails.getDeclaredLicenses().size());
    Assert.assertEquals("Apache-2.0", componentDetails.getDeclaredLicenses().iterator().next().getLicenseName());
    Assert.assertEquals("Apache-2.0", componentDetails.getDeclaredLicenses().iterator().next().getLicenseId());
    Assert.assertEquals(0, componentDetails.getObservedLicenses().size());
    Assert.assertEquals(1, componentDetails.getEffectiveLicenses().size());
    Assert.assertEquals("Apache-2.0", componentDetails.getEffectiveLicenses().iterator().next().getLicenseName());
    Assert.assertEquals("Apache-2.0", componentDetails.getEffectiveLicenses().iterator().next().getLicenseId());
    Assert.assertNull(componentDetails.getEffectiveLicenseStatus());
    componentDetails = componentDetailsList.getList().get(1);
    assertGavInComponentDetails(groupId, artifactId, "2.0.0", componentDetails);
    Assert.assertEquals(new Integer(1), componentDetails.getLicenseThreatLevel());
    Assert.assertEquals(3, componentDetails.getLicenseThreatGroupNames().size());
    Assert.assertThat(componentDetails.getLicenseThreatGroupNames(), contains("groupA", "Groupb", "GroupC"));
    Assert.assertEquals(1, componentDetails.getDeclaredLicenses().size());
    Assert.assertEquals("GPL-2.0", componentDetails.getDeclaredLicenses().iterator().next().getLicenseName());
    Assert.assertEquals("GPL-2.0", componentDetails.getDeclaredLicenses().iterator().next().getLicenseId());
    Assert.assertEquals(0, componentDetails.getObservedLicenses().size());
    Assert.assertEquals(1, componentDetails.getEffectiveLicenses().size());
    Assert.assertEquals("GPL-2.0", componentDetails.getEffectiveLicenses().iterator().next().getLicenseName());
    Assert.assertEquals("GPL-2.0", componentDetails.getEffectiveLicenses().iterator().next().getLicenseId());
    Assert.assertNull(componentDetails.getEffectiveLicenseStatus());
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
    String hash = "01234567890123456789";

    Application application = tempEntity.newApplicationWithParent(applicationPublicId);
    Label label = tempEntity.newLabel(application.getId(), "white");
    tempEntity.newComponentLabel(application.getId(), label.getId(), "01234567890123456789");

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

    String groupId = "g1.testGetComponentDetails_PolicyAlerts";
    String artifactId = "a1";
    String version = "v1";
    ComponentDetails saasComponentDetails = newComponentDetailsForMaven(groupId, artifactId, version);
    saasComponentDetails.addSecurityVulnerability(new SecurityVulnerability("Test Ref Id", "Test Source", 7.5F));
    setSaasResponse(new UriParamRequestMatcher(getSaasComponentDetailsUrl(hash, groupId, artifactId, version),
        toJson(saasComponentDetails), 200));
    Response response = AuthedRestAccess.get(getComponentDetailsUrl(applicationPublicId, groupId, artifactId, version,
        "01234567890123456789", "similar"));
    assertResponseStatus(200, response);

    ComponentDetails componentDetails = fromJson(response, ComponentDetails.class);
    Assert.assertNotNull(componentDetails);
    assertGavInComponentDetails(groupId, artifactId, version, componentDetails);
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
    Assert.assertEquals("Policy1", policyAlerts.get(0).getTrigger().getPolicyName());
  }

  @Test
  public void testGetComponentDetails_OverriddenLicense() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(applicationPublicId);

    tempEntity.newLicenseOverride(application.getId(), MAVEN_COORDINATES, LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0", null /* comment */);

    String groupId = MAVEN_COORDINATES.get(MAVEN_GROUP_ID);
    String artifactId = MAVEN_COORDINATES.get(MAVEN_ARTIFACT_ID);
    String version = MAVEN_COORDINATES.get(VERSION);
    String serviceUrl = getComponentDetailsUrl(applicationPublicId, groupId, artifactId, version, null, null);
    String saasUrl = convertToSaasUrl(serviceUrl, applicationPublicId);
    ComponentDetails saasComponentDetails = new ComponentDetails(MAVEN_COORDINATES);
    setSaasResponseForURI(saasUrl, toJson(saasComponentDetails), 200);
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);

    ComponentDetails componentDetails = fromJson(response, ComponentDetails.class);
    Assert.assertNotNull(componentDetails);
    Assert.assertEquals(MAVEN_COORDINATES, componentDetails.getComponentIdentifier());
    Assert.assertEquals(1, componentDetails.getOverriddenLicenses().size());
    License overriddenLicense = componentDetails.getOverriddenLicenses().iterator().next();
    Assert.assertNotNull(overriddenLicense);
    Assert.assertEquals("GPL-2.0", overriddenLicense.getLicenseId());
    Assert.assertEquals("GPL-2.0", overriddenLicense.getLicenseName());
    Assert.assertEquals(new Integer(9), componentDetails.getLicenseThreatLevel());
    Assert.assertEquals(1, componentDetails.getLicenseThreatGroupNames().size());
    Assert.assertEquals("Copyleft", componentDetails.getLicenseThreatGroupNames().get(0));
    Assert.assertEquals(1, componentDetails.getEffectiveLicenses().size());
    License effectiveLicense = componentDetails.getEffectiveLicenses().iterator().next();
    Assert.assertEquals("GPL-2.0", effectiveLicense.getLicenseId());
    Assert.assertEquals("GPL-2.0", effectiveLicense.getLicenseName());
    Assert.assertEquals(LicenseStatus.Overridden, componentDetails.getEffectiveLicenseStatus());
  }

  @Test
  public void testGetComponentDetails_SelectedLicense() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(applicationPublicId);

    tempEntity.newLicenseOverride(application.getId(), MAVEN_COORDINATES, LicenseOverrideStatus.SELECTED, "GPL-2.0",
      null /* comment */);

    String groupId = MAVEN_COORDINATES.get(MAVEN_GROUP_ID);
    String artifactId = MAVEN_COORDINATES.get(MAVEN_ARTIFACT_ID);
    String version = MAVEN_COORDINATES.get(VERSION);
    String serviceUrl = getComponentDetailsUrl(applicationPublicId, groupId, artifactId, version, null, null);
    String saasUrl = getSaasComponentDetailsUrl(groupId, artifactId, version);
    ComponentDetails saasComponentDetails = new ComponentDetails(MAVEN_COORDINATES);

    setSaasResponseForURI(saasUrl, toJson(saasComponentDetails), 200);
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);

    ComponentDetails componentDetails = fromJson(response, ComponentDetails.class);
    Assert.assertNotNull(componentDetails);
    Assert.assertEquals(MAVEN_COORDINATES, componentDetails.getComponentIdentifier());
    Assert.assertEquals(1, componentDetails.getOverriddenLicenses().size());
    License overriddenLicense = componentDetails.getOverriddenLicenses().iterator().next();
    Assert.assertNotNull(overriddenLicense);
    Assert.assertEquals("GPL-2.0", overriddenLicense.getLicenseId());
    Assert.assertEquals("GPL-2.0", overriddenLicense.getLicenseName());
    Assert.assertEquals(new Integer(9), componentDetails.getLicenseThreatLevel());
    Assert.assertEquals(1, componentDetails.getLicenseThreatGroupNames().size());
    Assert.assertEquals("Copyleft", componentDetails.getLicenseThreatGroupNames().get(0));
    Assert.assertEquals(1, componentDetails.getEffectiveLicenses().size());
    License effectiveLicense = componentDetails.getEffectiveLicenses().iterator().next();
    Assert.assertEquals("GPL-2.0", effectiveLicense.getLicenseId());
    Assert.assertEquals("GPL-2.0", effectiveLicense.getLicenseName());
    Assert.assertEquals(LicenseStatus.Selected, componentDetails.getEffectiveLicenseStatus());
  }

  @Test
  public void testGetComponentDetails_OverriddenSecurityVulnerabilityStatus() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(applicationPublicId);

    setSecurityAuditLog(application.getId(),
        "/AbstractComponentInfoResourceTest/SecurityOverride_abababababababababab.json");

    String groupId = "g1.testGetComponentDetails_OverriddenSecurityVulnerabilityStatus";
    String artifactId = "a1";
    String version = "v1";
    String serviceUrl = getComponentDetailsUrl(applicationPublicId, groupId, artifactId, version, null, null);
    ComponentDetails saasComponentDetails = newComponentDetailsForMaven(groupId, artifactId, version);
    saasComponentDetails.addSecurityVulnerability(new SecurityVulnerability("36079", "osvdb", 7.5F, "Summary"));
    setSaasResponse(new UriParamRequestMatcher(getSaasComponentDetailsUrl(groupId, artifactId, version),
        toJson(saasComponentDetails), 200));
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);

    ComponentDetails componentDetails = fromJson(response, ComponentDetails.class);
    Assert.assertNotNull(componentDetails);
    assertGavInComponentDetails(groupId, artifactId, version, componentDetails);
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
    assertGavInComponentDetails(groupId, artifactId, version, componentDetails);
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
    assertGavInComponentDetails("", "", "", componentDetails);
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
    ComponentDetails saasComponentDetails = newComponentDetailsForMaven(groupId, artifactId, version);
    setSaasResponseForURI(convertToSaasUrl(serviceUrl, applicationPublicId), toJson(saasComponentDetails), 200);
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);

    ComponentDetails componentDetails = fromJson(response, ComponentDetails.class);
    Assert.assertNotNull(componentDetails);
    assertGavInComponentDetails(groupId, artifactId, version, componentDetails);
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
    ComponentDetails saasComponentDetails = newComponentDetailsForMaven(groupId, artifactId, version);
    setSaasResponseForURI(saasUrl, toJson(saasComponentDetails), 200);
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);

    ComponentDetails componentDetails = fromJson(response, ComponentDetails.class);
    Assert.assertNotNull(componentDetails);
    assertGavInComponentDetails(groupId, artifactId, version, componentDetails);
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
    assertGavInComponentDetails(groupId, artifactId, version, componentDetails);
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
    assertGavInComponentDetails(groupId, artifactId, version, componentDetails);
    Assert.assertEquals(MatchState.SIMILAR.getId(), componentDetails.getMatchState());
    Assert.assertEquals(IdentificationSource.SONATYPE.getId(), componentDetails.getIdentificationSource());
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(0, policyAlerts.size());

    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(hash,
        ComponentIdentifier.createMavenCoordinates("Claimed" + groupId, "Claimed" + artifactId, "Claimed" + version));
    hashComponentIdentifier.setComment("ClaimedComment");
    hashComponentIdentifier.setCreateTime(new Date());
    HashComponentIdentifierDAO hashComponentIdentifierDAO = new HashComponentIdentifierDAO();
    hashComponentIdentifierDAO.insert(hashComponentIdentifier);
    response = AuthedRestAccess.get(serviceUrl);
    hashComponentIdentifierDAO.delete(hashComponentIdentifier);
    assertResponseStatus(200, response);
    componentDetails = fromJson(response, ComponentDetails.class);
    Assert.assertNotNull(componentDetails);
    Assert.assertEquals(hash, componentDetails.getHash());
    assertGavInComponentDetails("Claimed" + groupId, "Claimed" + artifactId, "Claimed" + version, componentDetails);
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
    Label label = tempEntity.newLabel(orgLabel ? app.getOrganizationId() : app.getId(), "red");
    tempEntity.newComponentLabel(orgComponentLabel ? app.getOrganizationId() : app.getId(), label
        .getId(), hash);

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
    assertGavInComponentDetails(groupId, artifactId, version, componentDetails);
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

  private String getSaasComponentDetailsUrl(String hash, String g, String a, String v) {
    return "rest/" + getToolName() + "/componentDetails?componentIdentifier=" + getComponentIdentifierParam(g, a, v)
        + "&hash="
        + hash;
  }

  private String getSaasComponentDetailsUrl(String g, String a, String v) {
    return "rest/" + getToolName() + "/componentDetails?componentIdentifier=" + getComponentIdentifierParam(g, a, v);
  }

  private String convertToSaasUrl(String brainUrl, String applicationId) {
    return brainUrl.replaceFirst("/rest/[^/]+/", "/rest/" + getToolName() + "/").substring(getRestBaseUrl().length())
        .replace("/" + applicationId, "").replace("component/details", "componentDetails");
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
    builder.queryParam("componentIdentifier", getComponentIdentifierParam(groupId, artifactId, version));
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

  private String getLicensesServiceURL(String applicationPublicId, String g, String a, String v) {
    return getServiceURL() + "/licenses/" + applicationPublicId + "?groupId=" + g + "&artifactId=" + a + "&version="
        + v;
  }

  private String getSelectableLicensesServiceURL(String applicationPublicId, String g, String a, String v) {
    return getServiceURL() + "/selectableLicenses/" + applicationPublicId + "?groupId=" + g + "&artifactId=" + a
        + "&version=" + v;
  }

  private String getComponentIdentifierParam(String g, String a, String v) {
    return UrlEncoded.encodeString(toJson(ComponentIdentifier.createMavenCoordinates(g, a, v)));
  }

  private String getServiceURL() {
    return getRestBaseUrl() + getResourcePath();
  }

  private ComponentDetails newComponentDetailsForMaven(String groupId, String artifactId, String version) {
    return new ComponentDetails(ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version));
  }

  protected abstract String getToolName();
}
