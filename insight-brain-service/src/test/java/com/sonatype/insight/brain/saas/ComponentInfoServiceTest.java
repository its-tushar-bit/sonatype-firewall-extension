/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.clm.dto.model.ide.LicenseStatus;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
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
import com.sonatype.insight.brain.saas.ComponentInfoService.ComponentLicenses;
import com.sonatype.insight.brain.saas.ComponentInfoService.LicenseWithThreatLevel;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.InternalServerException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.inject.Binder;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ComponentInfoServiceTest
    extends AbstractComponentTest
{
  private static final ComponentIdentifier MAVEN_COORDINATES = ComponentIdentifier.createMavenCoordinates("g1", "a1",
      "v1", "", "jar");

  // This is the tool name (ci, ide, rm) used in REST paths for HDS resources. Since we use it when we mock the HDS
  // client, it doesn't really matter what value we use here, because we don't really access HDS REST paths.
  private static final String TOOL_NAME = "ci";

  @Inject
  private ComponentInfoService componentInfoService;

  @Inject
  private InsightWork insightWork;

  private String applicationPublicId = "ComponentInfoServiceTest";

  private Application application;

  private SaasClient saasClientMock = mock(SaasClient.class);

  private HttpServletRequest httpRequestMock = mock(HttpServletRequest.class);

  @Override
  public void configure(Binder binder) {
    binder.bind(SaasClient.class).toInstance(saasClientMock);
    super.configure(binder);
  }

  @Before
  public void before() {
    componentInfoService.setToolName(TOOL_NAME);

    application = tempEntity.newApplicationWithParent(applicationPublicId);
  }

  private NamedComponentDetails newNamedComponentDetails(ComponentIdentifier componentIdentifier) {
    NamedComponentDetails namedComponentDetails = new NamedComponentDetails();
    namedComponentDetails.setComponentIdentifier(componentIdentifier);
    return namedComponentDetails;
  }

  private Map<String, String> newCoordinatesQueryParam(NamedComponentDetails componentDetails) {
    Map<String, String> queryParams = new HashMap<>();
    if (componentDetails.getHash() != null) {
      queryParams.put("hash", componentDetails.getHash());
    }
    queryParams
        .put("componentIdentifier", ComponentIdentifierAdapter.toJson(componentDetails.getComponentIdentifier()));
    return queryParams;
  }
  
  private void mockSaasGetComponentDetails(NamedComponentDetails saasComponentDetails) throws IOException {
    when(
        saasClientMock.get(httpRequestMock, NamedComponentDetails.class, "rest/" + TOOL_NAME + "/componentDetails",
            newCoordinatesQueryParam(saasComponentDetails))).thenReturn(saasComponentDetails);
  }

  private void mockSaasGetComponentDetailsList(ComponentDetailsList saasComponentDetailsList) throws IOException {
    when(
        saasClientMock.get(httpRequestMock, ComponentDetailsList.class, "rest/" + TOOL_NAME
            + "/componentDetails/list")).thenReturn(saasComponentDetailsList);
  }

  @Test
  public void testGetSelectableLicenses() throws Exception {
    NamedComponentDetails saasComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);

    // Verify that UNSPECIFIED is removed from the result
    saasComponentDetails.setDeclaredLicenses(toLicenseSet("EPL-1.0", "UNSPECIFIED"));
    mockSaasGetComponentDetails(saasComponentDetails);
    List<License> licenses = componentInfoService.getLicenses(applicationPublicId, MAVEN_COORDINATES, httpRequestMock).selectableLicenses;
    assertEquals(1, licenses.size());
    assertEquals("EPL-1.0", licenses.get(0).getLicenseId());

    // Verify that a versionless license is resolved to versioned licenses
    saasComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-UNSPECIFIED"));
    mockSaasGetComponentDetails(saasComponentDetails);
    licenses = componentInfoService.getLicenses(applicationPublicId, MAVEN_COORDINATES, httpRequestMock).selectableLicenses;
    assertEquals(Arrays.asList(licenses).toString(), 4, licenses.size());
    assertContainsLicenseId("Apache-UNSPECIFIED", licenses);
    assertContainsLicenseId("Apache-1.0", licenses);
    assertContainsLicenseId("Apache-1.1", licenses);
    assertContainsLicenseId("Apache-2.0", licenses);

    // Verify that declared and observed licenses are merged
    saasComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-2.0", "EPL-1.0"));
    saasComponentDetails.setObservedLicenses(toLicenseSet("EPL-1.0", "GPL-2.0"));
    mockSaasGetComponentDetails(saasComponentDetails);
    licenses = componentInfoService.getLicenses(applicationPublicId, MAVEN_COORDINATES, httpRequestMock).selectableLicenses;
    assertEquals(Arrays.asList(licenses).toString(), 3, licenses.size());
    assertContainsLicenseId("Apache-2.0", licenses);
    assertContainsLicenseId("EPL-1.0", licenses);
    assertContainsLicenseId("GPL-2.0", licenses);
  }

  @Test
  public void testGetLicenses_NoComponentIdentifier() throws Exception {
    try {
      componentInfoService.getLicenses(applicationPublicId, null /* componentIdentifier */, httpRequestMock);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      assertThat(expected.getMessage(), is("componentIdentifier is required"));
    }
  }

  @Test
  public void testGetLicenses() throws Exception {
    NamedComponentDetails saasComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);

    // Verify component without licenses
    mockSaasGetComponentDetails(saasComponentDetails);
    ComponentLicenses licenses = componentInfoService.getLicenses(applicationPublicId, MAVEN_COORDINATES,
        httpRequestMock);
    assertThat(licenses.declaredlicenses, empty());
    assertThat(licenses.observedlicenses, empty());
    assertThat(licenses.effectiveLicenses, empty());
    assertThat(licenses.selectableLicenses, empty());

    // Verify component with licenses
    tempEntity.newLicenseThreatGroup(application.getId(), "ComponentInfoServiceTest", 5, "LGPL-2.0", "BSD-3-Clause");

    saasComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-2.0", "LGPL-2.0-MPL-1.1"));
    saasComponentDetails.setObservedLicenses(toLicenseSet("GPL-2.0", "AFL-2.1-BSD-3-Clause"));
    mockSaasGetComponentDetails(saasComponentDetails);
    licenses = componentInfoService.getLicenses(applicationPublicId, MAVEN_COORDINATES, httpRequestMock);
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
    assertThat(licenses.selectableLicenses, hasSize(6));
    assertContainsLicenseId("Apache-2.0", licenses.selectableLicenses);
    assertContainsLicenseId("LGPL-2.0", licenses.selectableLicenses);
    assertContainsLicenseId("LGPL-2.0", licenses.selectableLicenses);
    assertContainsLicenseId("GPL-2.0", licenses.selectableLicenses);
    assertContainsLicenseId("MPL-1.1", licenses.selectableLicenses);
    assertContainsLicenseId("BSD-3-Clause", licenses.selectableLicenses);
  }

  @Test
  public void testGetLicenses_withOverride() throws Exception {
    // Verify component with licenses
    tempEntity.newLicenseThreatGroup(application.getId(), "ComponentInfoServiceTest", 5, "LGPL-2.0", "BSD-3-Clause");
    tempEntity.newLicenseOverride(application.getId(), MAVEN_COORDINATES, LicenseOverrideStatus.SELECTED,
      "BSD-3-Clause");

    NamedComponentDetails saasComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    saasComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-2.0", "LGPL-2.0-MPL-1.1"));
    saasComponentDetails.setObservedLicenses(toLicenseSet("GPL-2.0", "AFL-2.1-BSD-3-Clause"));
    mockSaasGetComponentDetails(saasComponentDetails);
    ComponentLicenses licenses = componentInfoService.getLicenses(applicationPublicId, MAVEN_COORDINATES,
        httpRequestMock);
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
    NamedComponentDetails saasComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    saasComponentDetails.setDeclaredLicenses(toLicenseSet("Not-Declared"));
    saasComponentDetails.setObservedLicenses(toLicenseSet("GPL-2.0"));
    mockSaasGetComponentDetails(saasComponentDetails);
    ComponentLicenses licenses = componentInfoService.getLicenses(applicationPublicId, MAVEN_COORDINATES,
        httpRequestMock);
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
    NamedComponentDetails saasComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    saasComponentDetails.setDeclaredLicenses(toLicenseSet("GPL-2.0"));
    saasComponentDetails.setObservedLicenses(toLicenseSet("No-Sources"));
    mockSaasGetComponentDetails(saasComponentDetails);
    ComponentLicenses licenses = componentInfoService.getLicenses(applicationPublicId, MAVEN_COORDINATES,
        httpRequestMock);
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
    NamedComponentDetails saasComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    saasComponentDetails.setDeclaredLicenses(toLicenseSet("GPL-2.0"));
    saasComponentDetails.setObservedLicenses(toLicenseSet("No-Source-License"));
    mockSaasGetComponentDetails(saasComponentDetails);
    ComponentLicenses licenses = componentInfoService.getLicenses(applicationPublicId, MAVEN_COORDINATES,
        httpRequestMock);
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
    NamedComponentDetails saasComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    saasComponentDetails.setDeclaredLicenses(toLicenseSet("Not-Declared"));
    saasComponentDetails.setObservedLicenses(toLicenseSet("No-Source-License"));
    mockSaasGetComponentDetails(saasComponentDetails);
    ComponentLicenses licenses = componentInfoService.getLicenses(applicationPublicId, MAVEN_COORDINATES,
        httpRequestMock);
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
    // Verify exception is not thrown if component is not known to HDS
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("componentIdentifier", ComponentIdentifierAdapter.toJson(MAVEN_COORDINATES));

    when(
        saasClientMock.get(httpRequestMock, NamedComponentDetails.class, "rest/" + TOOL_NAME + "/componentDetails",
            queryParams)).thenThrow(new NotFoundException("test"));
    ComponentLicenses licenses = componentInfoService.getLicenses(applicationPublicId, MAVEN_COORDINATES,
        httpRequestMock);
    // if we got here, we are good, but let's do some sanity check
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
  public void testGetComponentDetailsList() throws Exception {
    // Create an application without LTGs
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
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "1.0.0");
    ComponentDetails saasComponentDetails1 = newNamedComponentDetails(componentIdentifier1);
    Set<License> licenses1 = new LinkedHashSet<>();
    licenses1.add(new License("Apache-2.0", "Apache-2.0"));
    saasComponentDetails1.setDeclaredLicenses(licenses1);
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "2.0.0");
    ComponentDetails saasComponentDetails2 = newNamedComponentDetails(componentIdentifier2);
    Set<License> licenses2 = new LinkedHashSet<>();
    licenses2.add(new License("GPL-2.0", "GPL-2.0"));
    saasComponentDetails2.setDeclaredLicenses(licenses2);
    ComponentDetailsList saasComponentDetailsList = new ComponentDetailsList();
    saasComponentDetailsList.setList(Arrays.asList(saasComponentDetails1, saasComponentDetails2));
    mockSaasGetComponentDetailsList(saasComponentDetailsList);
    ComponentDetailsList componentDetailsList = componentInfoService.getComponentDetailsList(application,
        componentIdentifier1, MatchState.EXACT.getId(), httpRequestMock);
    assertNotNull(componentDetailsList);
    assertEquals(2, componentDetailsList.getList().size());
    ComponentDetails componentDetails = componentDetailsList.getList().get(0);
    assertEquals(componentIdentifier1, componentDetails.getComponentIdentifier());
    assertEquals(new Integer(9), componentDetails.getLicenseThreatLevel());
    assertEquals(1, componentDetails.getLicenseThreatGroupNames().size());
    assertEquals("Group1", componentDetails.getLicenseThreatGroupNames().get(0));
    assertEquals(1, componentDetails.getDeclaredLicenses().size());
    assertEquals("Apache-2.0", componentDetails.getDeclaredLicenses().iterator().next().getLicenseName());
    assertEquals("Apache-2.0", componentDetails.getDeclaredLicenses().iterator().next().getLicenseId());
    assertEquals(0, componentDetails.getObservedLicenses().size());
    assertEquals(1, componentDetails.getEffectiveLicenses().size());
    assertEquals("Apache-2.0", componentDetails.getEffectiveLicenses().iterator().next().getLicenseName());
    assertEquals("Apache-2.0", componentDetails.getEffectiveLicenses().iterator().next().getLicenseId());
    assertNull(componentDetails.getEffectiveLicenseStatus());
    componentDetails = componentDetailsList.getList().get(1);
    assertEquals(componentIdentifier2, componentDetails.getComponentIdentifier());
    assertEquals(new Integer(1), componentDetails.getLicenseThreatLevel());
    assertEquals(3, componentDetails.getLicenseThreatGroupNames().size());
    assertThat(componentDetails.getLicenseThreatGroupNames(), contains("groupA", "Groupb", "GroupC"));
    assertEquals(1, componentDetails.getDeclaredLicenses().size());
    assertEquals("GPL-2.0", componentDetails.getDeclaredLicenses().iterator().next().getLicenseName());
    assertEquals("GPL-2.0", componentDetails.getDeclaredLicenses().iterator().next().getLicenseId());
    assertEquals(0, componentDetails.getObservedLicenses().size());
    assertEquals(1, componentDetails.getEffectiveLicenses().size());
    assertEquals("GPL-2.0", componentDetails.getEffectiveLicenses().iterator().next().getLicenseName());
    assertEquals("GPL-2.0", componentDetails.getEffectiveLicenses().iterator().next().getLicenseId());
    assertNull(componentDetails.getEffectiveLicenseStatus());
  }

  @Test
  public void testGetComponentDetails_PolicyAlerts() throws Exception {
    String hash = "01234567890123456789";

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
    Policy policy2 = new Policy("PolicyId2", "Policy2");
    policy2.setThreatLevel(8);
    policy2.addConstraint(constraint2);
    policy2.addAction(BuildStageType.ID, failAction);
    addPolicy(applicationPublicId, policy2);

    NamedComponentDetails saasComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    saasComponentDetails.setHash(hash);
    saasComponentDetails.addSecurityVulnerability(new SecurityVulnerability("Test Ref Id", "Test Source", 7.5F));
    mockSaasGetComponentDetails(saasComponentDetails);
    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_COORDINATES,
        MatchState.SIMILAR.getId(), hash, false /* proprietary */, httpRequestMock);
    assertNotNull(componentDetails);
    assertThat(componentDetails.getComponentIdentifier(), is(MAVEN_COORDINATES));
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    assertNotNull(policyAlerts);
    assertEquals(1, policyAlerts.size());
    assertEquals("Policy1", policyAlerts.get(0).getTrigger().getPolicyName());
  }

  @Test
  public void testGetComponentDetails_OverriddenLicense() throws Exception {
    tempEntity.newLicenseOverride(application.getId(), MAVEN_COORDINATES, LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0", null /* comment */);

    NamedComponentDetails saasComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    mockSaasGetComponentDetails(saasComponentDetails);

    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_COORDINATES,
        MatchState.EXACT.getId(), null /* hash */, false /* proprietary */, httpRequestMock);
    assertNotNull(componentDetails);
    assertEquals(MAVEN_COORDINATES, componentDetails.getComponentIdentifier());
    assertEquals(1, componentDetails.getOverriddenLicenses().size());
    License overriddenLicense = componentDetails.getOverriddenLicenses().iterator().next();
    assertNotNull(overriddenLicense);
    assertEquals("GPL-2.0", overriddenLicense.getLicenseId());
    assertEquals("GPL-2.0", overriddenLicense.getLicenseName());
    assertEquals(new Integer(9), componentDetails.getLicenseThreatLevel());
    assertEquals(1, componentDetails.getLicenseThreatGroupNames().size());
    assertEquals("Copyleft", componentDetails.getLicenseThreatGroupNames().get(0));
    assertEquals(1, componentDetails.getEffectiveLicenses().size());
    License effectiveLicense = componentDetails.getEffectiveLicenses().iterator().next();
    assertEquals("GPL-2.0", effectiveLicense.getLicenseId());
    assertEquals("GPL-2.0", effectiveLicense.getLicenseName());
    assertEquals(LicenseStatus.Overridden, componentDetails.getEffectiveLicenseStatus());
  }

  @Test
  public void testGetComponentDetails_SelectedLicense() throws Exception {
    tempEntity.newLicenseOverride(application.getId(), MAVEN_COORDINATES, LicenseOverrideStatus.SELECTED, "GPL-2.0",
      null /* comment */);

    NamedComponentDetails saasComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    mockSaasGetComponentDetails(saasComponentDetails);

    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_COORDINATES,
        MatchState.EXACT.getId(), null /* hash */, false /* proprietary */, httpRequestMock);
    assertNotNull(componentDetails);
    assertEquals(MAVEN_COORDINATES, componentDetails.getComponentIdentifier());
    assertEquals(1, componentDetails.getOverriddenLicenses().size());
    License overriddenLicense = componentDetails.getOverriddenLicenses().iterator().next();
    assertNotNull(overriddenLicense);
    assertEquals("GPL-2.0", overriddenLicense.getLicenseId());
    assertEquals("GPL-2.0", overriddenLicense.getLicenseName());
    assertEquals(new Integer(9), componentDetails.getLicenseThreatLevel());
    assertEquals(1, componentDetails.getLicenseThreatGroupNames().size());
    assertEquals("Copyleft", componentDetails.getLicenseThreatGroupNames().get(0));
    assertEquals(1, componentDetails.getEffectiveLicenses().size());
    License effectiveLicense = componentDetails.getEffectiveLicenses().iterator().next();
    assertEquals("GPL-2.0", effectiveLicense.getLicenseId());
    assertEquals("GPL-2.0", effectiveLicense.getLicenseName());
    assertEquals(LicenseStatus.Selected, componentDetails.getEffectiveLicenseStatus());
  }

  @Test
  public void testGetComponentDetails_UnknownComponent() throws Exception {
    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(MatchStateConditionType.ID, "is", "unknown"));
    Policy policy1 = new Policy("PolicyId1", "Policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    Action failAction = new Action(FailActionType.ID);
    policy1.addAction(BuildStageType.ID, failAction);
    addPolicy(applicationPublicId, policy1);

    String hash = "01234567890123456789";
    NamedComponentDetails saasComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    saasComponentDetails.setHash(hash);
    mockSaasGetComponentDetails(saasComponentDetails);
    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_COORDINATES,
        MatchState.UNKNOWN.getId(), hash, false /* proprietary */, httpRequestMock);

    assertNotNull(componentDetails);
    assertEquals(MAVEN_COORDINATES, componentDetails.getComponentIdentifier());
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    assertNotNull(policyAlerts);
    assertEquals(1, policyAlerts.size());
    assertEquals("Policy1", policyAlerts.get(0).getTrigger().getPolicyName());

    ComponentIdentifier emptyComponentIdentifier = ComponentIdentifier.createMavenCoordinates("", "", "");
    saasComponentDetails = newNamedComponentDetails(emptyComponentIdentifier);
    saasComponentDetails.setHash(hash);
    when(
        saasClientMock.get(httpRequestMock, NamedComponentDetails.class, "rest/" + TOOL_NAME + "/componentDetails",
            newCoordinatesQueryParam(saasComponentDetails))).thenThrow(new NotFoundException("unknown GAV"));
    componentDetails = componentInfoService.getComponentDetails(application, emptyComponentIdentifier,
        MatchState.UNKNOWN.getId(), "01234567890123456789", false /* proprietary */, httpRequestMock);
    assertNotNull(componentDetails);
    assertEquals(emptyComponentIdentifier, componentDetails.getComponentIdentifier());
    policyAlerts = componentDetails.getPolicyAlerts();
    assertNotNull(policyAlerts);
    assertEquals(1, policyAlerts.size());
    assertEquals("Policy1", policyAlerts.get(0).getTrigger().getPolicyName());
  }

  // CLM-4195
  @Test
  public void testGetComponentDetails_UnknownComponentNullIdentifier() throws Exception {
    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(ProprietaryConditionType.ID, "is true"));
    Policy policy1 = new Policy("PolicyId1", "Policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.addAction(BuildStageType.ID, new Action(FailActionType.ID));
    addPolicy(applicationPublicId, policy1);

    String hash = "01234567890123456789";
    NamedComponentDetails saasComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    saasComponentDetails.setHash(hash);
    mockSaasGetComponentDetails(saasComponentDetails);
    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application,
        null /* componentIdentifier */, MatchState.UNKNOWN.getId(), hash, true /* proprietary */, httpRequestMock);
    assertNotNull(componentDetails);
    assertEquals(hash, componentDetails.getHash());
    assertNull(componentDetails.getComponentIdentifier());

    assertEquals(MatchState.UNKNOWN.getId(), componentDetails.getMatchState());

    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    assertNotNull(policyAlerts);
    assertNotNull(policyAlerts);
    assertEquals(1, policyAlerts.size());
  }

  @Test
  public void testGetComponentDetails_AppPublicIdWithUnsafeCharacters() throws Exception {
    String applicationPublicId = "bom 1&2%20?";
    tempEntity.newApplicationWithInvalidPublicId(applicationPublicId);

    String hash = "01234567890123456789";
    NamedComponentDetails saasComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    saasComponentDetails.setHash(hash);
    mockSaasGetComponentDetails(saasComponentDetails);
    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_COORDINATES,
        MatchState.UNKNOWN.getId(), hash, false /* proprietary */, httpRequestMock);
    assertNotNull(componentDetails);
    assertEquals(MAVEN_COORDINATES, componentDetails.getComponentIdentifier());
  }

  @Test
  public void testGetComponentDetails_ProprietaryComponent() throws Exception {
    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(ProprietaryConditionType.ID, "is true"));
    Policy policy1 = new Policy("PolicyId1", "Policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.addAction(BuildStageType.ID, new Action(FailActionType.ID));
    addPolicy(applicationPublicId, policy1);

    String hash = "01234567890123456789";
    NamedComponentDetails saasComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    saasComponentDetails.setHash(hash);
    mockSaasGetComponentDetails(saasComponentDetails);
    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_COORDINATES,
        MatchState.SIMILAR.getId(), hash, true /* proprietary */, httpRequestMock);
    assertNotNull(componentDetails);
    assertEquals(MAVEN_COORDINATES, componentDetails.getComponentIdentifier());
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    assertNotNull(policyAlerts);
    assertEquals(1, policyAlerts.size());
    assertEquals("Policy1", policyAlerts.get(0).getTrigger().getPolicyName());

    componentDetails = componentInfoService.getComponentDetails(application, MAVEN_COORDINATES,
        MatchState.SIMILAR.getId(), hash, false /* proprietary */, httpRequestMock);
    assertNotNull(componentDetails);
    assertEquals(MAVEN_COORDINATES, componentDetails.getComponentIdentifier());
    policyAlerts = componentDetails.getPolicyAlerts();
    assertNotNull(policyAlerts);
    assertEquals(0, policyAlerts.size());
  }

  @Test
  public void testGetComponentDetails_ManuallyIdentifiedComponent() throws Exception {
    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(MatchStateConditionType.ID, "is", "exact"));
    constraint1.addCondition(new Condition(AgeInDaysConditionType.ID, "younger than", "30"));
    Policy policy1 = new Policy("PolicyId1", "Policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.addAction(BuildStageType.ID, new Action(FailActionType.ID));
    addPolicy(applicationPublicId, policy1);

    String hash = "01234567890123456789";
    NamedComponentDetails saasComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    saasComponentDetails.setHash(hash);
    mockSaasGetComponentDetails(saasComponentDetails);
    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_COORDINATES,
        MatchState.SIMILAR.getId(), hash, false /* proprietary */, httpRequestMock);
    assertNotNull(componentDetails);
    assertEquals(hash, componentDetails.getHash());
    assertEquals(MAVEN_COORDINATES, componentDetails.getComponentIdentifier());
    assertEquals(MatchState.SIMILAR.getId(), componentDetails.getMatchState());
    assertEquals(IdentificationSource.SONATYPE.getId(), componentDetails.getIdentificationSource());
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    assertNotNull(policyAlerts);
    assertNotNull(policyAlerts);
    assertEquals(0, policyAlerts.size());

    ComponentIdentifier claimedComponentIdentifier = ComponentIdentifier.createMavenCoordinates("Claimed g",
        "Claimed a", "Claimed v");
    HashComponentIdentifier claimedComponent = tempEntity.newClaimedComponent(hash, claimedComponentIdentifier);
    componentDetails = componentInfoService.getComponentDetails(application, MAVEN_COORDINATES,
        MatchState.SIMILAR.getId(), hash, false /* proprietary */, httpRequestMock);
    assertNotNull(componentDetails);
    assertEquals(hash, componentDetails.getHash());
    assertEquals(claimedComponentIdentifier, componentDetails.getComponentIdentifier());
    assertEquals(MatchState.EXACT.getId(), componentDetails.getMatchState());
    assertEquals(IdentificationSource.MANUAL.getId(), componentDetails.getIdentificationSource());
    assertEquals(claimedComponent.getComment(), componentDetails.getIdentificationSourceComment());
    policyAlerts = componentDetails.getPolicyAlerts();
    assertEquals(1, policyAlerts.size());
    assertEquals("Policy1", policyAlerts.get(0).getTrigger().getPolicyName());
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
    Label label = tempEntity.newLabel(orgLabel ? application.getOrganizationId() : application.getId(), "red");
    tempEntity.newComponentLabel(orgComponentLabel ? application.getOrganizationId() : application.getId(),
        label.getId(), hash);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(LabelConditionType.ID, "is", label.getId()));
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    Action failAction = new Action(FailActionType.ID);
    policy1.addAction(BuildStageType.ID, failAction);
    addPolicy(applicationPublicId, policy1);

    NamedComponentDetails saasComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    saasComponentDetails.setHash(hash);
    mockSaasGetComponentDetails(saasComponentDetails);
    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_COORDINATES,
        MatchState.SIMILAR.getId(), hash, false /* proprietary */, httpRequestMock);
    assertThat(componentDetails, is(notNullValue()));
    assertThat(componentDetails.getHash(), is(hash));
    assertEquals(MAVEN_COORDINATES, componentDetails.getComponentIdentifier());
    assertThat(componentDetails.getMatchState(), is(MatchState.SIMILAR.getId()));
    assertThat(componentDetails.getIdentificationSource(), is(IdentificationSource.SONATYPE.getId()));
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    assertThat(policyAlerts, is(notNullValue()));
    assertThat(policyAlerts.size(), is(1));
  }

  private void addPolicy(String applicationPublicId, Policy policy) throws Exception {
    String appId = new ApplicationDAO().getByPublicIdNotNull(applicationPublicId).getId();
    PolicyDAO policyDAO = new PolicyDAO();
    policy.setOwnerId(appId);
    policyDAO.insert(policy);
  }

  private void assertContainsLicenseId(String licenseId, Iterable<License> licenses) {
    for (License license : licenses) {
      if (licenseId.equals(license.getLicenseId())) {
        return;
      }
    }
    fail("Expected license id " + licenseId);
  }

  private Set<License> toLicenseSet(String... licenseIds) {
    Set<License> result = new LinkedHashSet<>();
    MultiLicenseDAO dao = new MultiLicenseDAO();
    for (String licenseId : licenseIds) {
      MultiLicense multiLicense = dao.getByIdNotNull(licenseId);
      result.add(new License(multiLicense.getId(), multiLicense.getShortDisplayName()));
    }
    return result;
  }

  @Test
  public void testGetComponentDetails_ReadPermission() throws Exception {
    NamedComponentDetails saasComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    mockSaasGetComponentDetails(saasComponentDetails);
    String reportId = "4cabb3f39eb945158c240f36aedf05e8";
    FileUtils.copyDirectoryStructure(new File(
        "target/test-classes/ComponentInfoServiceTest/GetComponentDetailsWithReadPermission", reportId), insightWork
        .getReportDir(application.getId(), reportId));
    ComponentDetails componentDetails = componentInfoService.getComponentDetails_ReadPermission(applicationPublicId,
        reportId, MAVEN_COORDINATES, MatchState.EXACT.getId(), null /* hash */, false /* proprietary */,
        httpRequestMock);
    assertThat(componentDetails, is(notNullValue()));
    assertThat(componentDetails.getComponentIdentifier(), is(MAVEN_COORDINATES));
    assertThat(componentDetails.getMatchState(), is(MatchState.EXACT.getId()));
    assertThat(componentDetails.getIdentificationSource(), is(IdentificationSource.SONATYPE.getId()));
  }

  @Test
  public void testGetComponentDetails_ReadPermission_ReportDoesNotExist() throws Exception {
    String reportId = "noSuchReport";
    try {
      componentInfoService.getComponentDetails_ReadPermission(applicationPublicId, reportId, MAVEN_COORDINATES,
          MatchState.EXACT.getId(), null /* hash */, false /* proprietary */, httpRequestMock);
      fail("Expected NotFoundException");
    }
    catch (NotFoundException expected) {
      assertThat(expected.getMessage(), is("Cannot find a report with ID 'noSuchReport'."));
    }
  }

  @Test
  public void testGetComponentDetails_ReadPermission_ComponentNotInReport() throws Exception {
    String reportId = "4cabb3f39eb945158c240f36aedf05e8";
    FileUtils.copyDirectoryStructure(new File(
        "target/test-classes/ComponentInfoServiceTest/GetComponentDetailsWithReadPermission", reportId), insightWork
        .getReportDir(application.getId(), reportId));
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNugetCoordinates("packageId", "version");
    try {
      componentInfoService.getComponentDetails_ReadPermission(applicationPublicId, reportId, componentIdentifier,
          MatchState.EXACT.getId(), null /* hash */, false /* proprietary */, httpRequestMock);
      fail("Expected InternalServerException");
    }
    catch (InternalServerException expected) {
      assertThat(expected.getMessage(), is("Cannot get component details."));
    }
  }

  @Test
  public void testGetComponentDetails_ReadPermission_ComponentWithDifferentVersionInReport() throws Exception {
    ComponentIdentifier componentIdentifier = MAVEN_COORDINATES.createAlternativeVersion("1.2.3.4");
    NamedComponentDetails saasComponentDetails = newNamedComponentDetails(componentIdentifier);
    mockSaasGetComponentDetails(saasComponentDetails);
    String reportId = "4cabb3f39eb945158c240f36aedf05e8";
    FileUtils.copyDirectoryStructure(new File(
        "target/test-classes/ComponentInfoServiceTest/GetComponentDetailsWithReadPermission", reportId), insightWork
        .getReportDir(application.getId(), reportId));
    ComponentDetails componentDetails = componentInfoService.getComponentDetails_ReadPermission(applicationPublicId,
        reportId, componentIdentifier, MatchState.EXACT.getId(), null /* hash */, false /* proprietary */,
        httpRequestMock);
    assertThat(componentDetails, is(notNullValue()));
    assertThat(componentDetails.getComponentIdentifier(), is(componentIdentifier));
    assertThat(componentDetails.getMatchState(), is(MatchState.EXACT.getId()));
    assertThat(componentDetails.getIdentificationSource(), is(IdentificationSource.SONATYPE.getId()));
  }

  @Test
  public void testGetComponentDetails_ReadPermission_NoReportId() throws Exception {
    // reportId is null
    try {
      componentInfoService.getComponentDetails_ReadPermission(applicationPublicId, null /* reportId */,
          MAVEN_COORDINATES, MatchState.EXACT.getId(), null /* hash */, false /* proprietary */, httpRequestMock);
      fail("Expected InternalServerException");
    }
    catch (InternalServerException expected) {
      assertThat(expected.getMessage(), is("The report ID must be specified."));
    }

    // reportId is empty
    try {
      componentInfoService.getComponentDetails_ReadPermission(applicationPublicId, " " /* reportId */,
          MAVEN_COORDINATES, MatchState.EXACT.getId(), null /* hash */, false /* proprietary */, httpRequestMock);
      fail("Expected InternalServerException");
    }
    catch (InternalServerException expected) {
      assertThat(expected.getMessage(), is("The report ID must be specified."));
    }
  }

  @Test
  public void testGetComponentDetailsList_ReadPermission() throws Exception {
    ComponentDetails saasComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    ComponentDetailsList saasComponentDetailsList = new ComponentDetailsList();
    saasComponentDetailsList.setList(Arrays.asList(saasComponentDetails));
    mockSaasGetComponentDetailsList(saasComponentDetailsList);
    String reportId = "4cabb3f39eb945158c240f36aedf05e8";
    FileUtils.copyDirectoryStructure(new File(
        "target/test-classes/ComponentInfoServiceTest/GetComponentDetailsWithReadPermission", reportId), insightWork
        .getReportDir(application.getId(), reportId));
    ComponentDetailsList componentDetailsList = componentInfoService.getComponentDetailsList_ReadPermission(
        applicationPublicId, reportId, MAVEN_COORDINATES, MatchState.EXACT.getId(), httpRequestMock);
    assertThat(componentDetailsList.getList(), hasSize(1));
    ComponentDetails componentDetails = componentDetailsList.getList().get(0);
    assertThat(componentDetails.getComponentIdentifier(), is(MAVEN_COORDINATES));
    assertThat(componentDetails.getMatchState(), is(MatchState.EXACT.getId()));
  }

  @Test
  public void testGetComponentDetailsList_ReadPermission_ReportDoesNotExist() throws Exception {
    String reportId = "noSuchReport";
    try {
      componentInfoService.getComponentDetailsList_ReadPermission(applicationPublicId, reportId, MAVEN_COORDINATES,
          MatchState.EXACT.getId(), httpRequestMock);
      fail("Expected NotFoundException");
    }
    catch (NotFoundException expected) {
      assertThat(expected.getMessage(), is("Cannot find a report with ID 'noSuchReport'."));
    }
  }

  @Test
  public void testGetComponentDetailsList_ReadPermission_ComponentNotInReport() throws Exception {
    String reportId = "4cabb3f39eb945158c240f36aedf05e8";
    FileUtils.copyDirectoryStructure(new File(
        "target/test-classes/ComponentInfoServiceTest/GetComponentDetailsWithReadPermission", reportId), insightWork
        .getReportDir(application.getId(), reportId));
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNugetCoordinates("packageId", "version");
    try {
      componentInfoService.getComponentDetailsList_ReadPermission(applicationPublicId, reportId, componentIdentifier,
          MatchState.EXACT.getId(), httpRequestMock);
      fail("Expected InternalServerException");
    }
    catch (InternalServerException expected) {
      assertThat(expected.getMessage(), is("Cannot get component details."));
    }
  }

  @Test
  public void testGetComponentDetailsList_ReadPermission_ComponentWithDifferentVersionInReport() throws Exception {
    ComponentIdentifier componentIdentifier = MAVEN_COORDINATES.createAlternativeVersion("1.2.3.4");
    ComponentDetails saasComponentDetails = newNamedComponentDetails(componentIdentifier);
    ComponentDetailsList saasComponentDetailsList = new ComponentDetailsList();
    saasComponentDetailsList.setList(Arrays.asList(saasComponentDetails));
    mockSaasGetComponentDetailsList(saasComponentDetailsList);
    String reportId = "4cabb3f39eb945158c240f36aedf05e8";
    FileUtils.copyDirectoryStructure(new File(
        "target/test-classes/ComponentInfoServiceTest/GetComponentDetailsWithReadPermission", reportId), insightWork
        .getReportDir(application.getId(), reportId));
    ComponentDetailsList componentDetailsList = componentInfoService.getComponentDetailsList_ReadPermission(
        applicationPublicId, reportId, componentIdentifier, MatchState.EXACT.getId(), httpRequestMock);
    assertThat(componentDetailsList.getList(), hasSize(1));
    ComponentDetails componentDetails = componentDetailsList.getList().get(0);
    assertThat(componentDetails.getComponentIdentifier(), is(componentIdentifier));
    assertThat(componentDetails.getMatchState(), is(MatchState.EXACT.getId()));
  }

  @Test
  public void testGetComponentDetailsList_ReadPermission_NoReportId() throws Exception {
    // reportId is null
    try {
      componentInfoService.getComponentDetailsList_ReadPermission(applicationPublicId, null /* reportId */,
          MAVEN_COORDINATES, MatchState.EXACT.getId(), httpRequestMock);
      fail("Expected InternalServerException");
    }
    catch (InternalServerException expected) {
      assertThat(expected.getMessage(), is("The report ID must be specified."));
    }

    // reportId is empty
    try {
      componentInfoService.getComponentDetailsList_ReadPermission(applicationPublicId, " " /* reportId */,
          MAVEN_COORDINATES, MatchState.EXACT.getId(), httpRequestMock);
      fail("Expected InternalServerException");
    }
    catch (InternalServerException expected) {
      assertThat(expected.getMessage(), is("The report ID must be specified."));
    }
  }

  @Test
  public void testGetComponentDetails_TruncatesFullSha1WhenLoadingHashBasedData() throws Exception {
    String hash = "01234567890123456789";
    String fullHash = hash + hash;

    Label label = tempEntity.newLabel(application.getId(), "red");
    tempEntity.newComponentLabel(application.getId(), label.getId(), hash);

    // policy that triggers if the component label was loaded properly by hash
    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(LabelConditionType.ID, "is", label.getId()));
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.addAction(BuildStageType.ID, new Action(FailActionType.ID));
    addPolicy(applicationPublicId, policy1);

    // policy that doesn't trigger if the corresponding waiver was loaded properly by hash
    Constraint constraint2 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint2.addCondition(new Condition(LabelConditionType.ID, "is", label.getId()));
    Policy policy2 = new Policy("PolicyId2", "Policy Name 2");
    policy2.setThreatLevel(8);
    policy2.addConstraint(constraint1);
    policy2.addAction(BuildStageType.ID, new Action(FailActionType.ID));
    addPolicy(applicationPublicId, policy2);
    tempEntity.newWaiver(hash, policy2.getId(), application.getId());

    NamedComponentDetails saasComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    saasComponentDetails.setHash(hash);
    mockSaasGetComponentDetails(saasComponentDetails);

    NamedComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_COORDINATES,
        MatchState.EXACT.getId(), fullHash, false /* proprietary */, httpRequestMock);
    assertNotNull(componentDetails);
    assertEquals(hash, componentDetails.getHash());
    assertEquals(MAVEN_COORDINATES, componentDetails.getComponentIdentifier());
    assertEquals(MatchState.EXACT.getId(), componentDetails.getMatchState());
    assertEquals(IdentificationSource.SONATYPE.getId(), componentDetails.getIdentificationSource());
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    assertEquals(1, policyAlerts.size());
    assertEquals(policy1.getName(), policyAlerts.get(0).getTrigger().getPolicyName());
  }
}
