/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import com.sonatype.insight.brain.hds.ComponentInfoService.ComponentLicenses;
import com.sonatype.insight.brain.hds.ComponentInfoService.ComponentSecurityVulnerabilities;
import com.sonatype.insight.brain.hds.ComponentInfoService.LicenseWithThreatLevel;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
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
import com.sonatype.insight.brain.model.repository.Repository;
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

  private HdsClient hdsClientMock = mock(HdsClient.class);

  private Repository repository;

  private HttpServletRequest httpRequestMock = mock(HttpServletRequest.class);

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(hdsClientMock);
    super.configure(binder);
  }

  @Before
  public void before() {
    componentInfoService.setToolName(TOOL_NAME);

    application = tempEntity.newApplicationWithParent(applicationPublicId);
    repository = tempEntity.newRepository();
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
  
  private void mockHdsGetComponentDetails(NamedComponentDetails hdsComponentDetails) throws IOException {
    when(
        hdsClientMock.get(httpRequestMock, NamedComponentDetails.class, "rest/" + TOOL_NAME + "/componentDetails",
            newCoordinatesQueryParam(hdsComponentDetails))).thenReturn(hdsComponentDetails);
  }

  private void mockHdsGetComponentDetailsList(ComponentDetailsList hdsComponentDetailsList) throws IOException {
    when(
        hdsClientMock.get(httpRequestMock, ComponentDetailsList.class, "rest/" + TOOL_NAME
            + "/componentDetails/list")).thenReturn(hdsComponentDetailsList);
  }

  @Test
  public void testGetSelectableLicenses() throws Exception {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);

    // Verify that UNSPECIFIED is removed from the result
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("EPL-1.0", "UNSPECIFIED"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    List<License> licenses = componentInfoService.getLicenses(applicationPublicId, MAVEN_COORDINATES, httpRequestMock).selectableLicenses;
    assertEquals(1, licenses.size());
    assertEquals("EPL-1.0", licenses.get(0).getLicenseId());

    // Verify that a versionless license is resolved to versioned licenses
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-UNSPECIFIED"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    licenses = componentInfoService.getLicenses(applicationPublicId, MAVEN_COORDINATES, httpRequestMock).selectableLicenses;
    assertEquals(Arrays.asList(licenses).toString(), 4, licenses.size());
    assertContainsLicenseId("Apache-UNSPECIFIED", licenses);
    assertContainsLicenseId("Apache-1.0", licenses);
    assertContainsLicenseId("Apache-1.1", licenses);
    assertContainsLicenseId("Apache-2.0", licenses);

    // Verify that declared and observed licenses are merged
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-2.0", "EPL-1.0"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("EPL-1.0", "GPL-2.0"));
    mockHdsGetComponentDetails(hdsComponentDetails);
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
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);

    // Verify component without licenses
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentLicenses licenses = componentInfoService.getLicenses(applicationPublicId, MAVEN_COORDINATES,
        httpRequestMock);
    assertThat(licenses.declaredlicenses, empty());
    assertThat(licenses.observedlicenses, empty());
    assertThat(licenses.effectiveLicenses, empty());
    assertThat(licenses.selectableLicenses, empty());

    // Verify component with licenses
    tempEntity.newLicenseThreatGroup(application.getId(), "ComponentInfoServiceTest", 5, "LGPL-2.0", "BSD-3-Clause");

    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-2.0", "LGPL-2.0-MPL-1.1"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("GPL-2.0", "AFL-2.1-BSD-3-Clause"));
    mockHdsGetComponentDetails(hdsComponentDetails);
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

    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-2.0", "LGPL-2.0-MPL-1.1"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("GPL-2.0", "AFL-2.1-BSD-3-Clause"));
    mockHdsGetComponentDetails(hdsComponentDetails);
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
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("Not-Declared"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("GPL-2.0"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentLicenses licenses = componentInfoService.getLicenses(applicationPublicId, MAVEN_COORDINATES,
        httpRequestMock);
    assertThat(licenses.declaredlicenses, hasSize(1));
    assertContainsLicenseWithThreatLevel("Not-Declared", "Not Declared", 5, licenses.declaredlicenses);
    assertThat(licenses.observedlicenses, hasSize(1));
    assertContainsLicenseWithThreatLevel("GPL-2.0", "GPL-2.0", 9, licenses.observedlicenses);
    assertThat(licenses.effectiveLicenses, hasSize(1));
    List<LicenseWithThreatLevel> effectiveList = new ArrayList<>(licenses.effectiveLicenses);
    assertContainsLicenseWithThreatLevel("GPL-2.0", "GPL-2.0", 9, effectiveList);
  }

  @Test
  public void testGetLicenses_withNoSourcesForObservedLicenses() throws Exception {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("GPL-2.0"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("No-Sources"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentLicenses licenses = componentInfoService.getLicenses(applicationPublicId, MAVEN_COORDINATES,
        httpRequestMock);
    assertThat(licenses.declaredlicenses, hasSize(1));
    assertContainsLicenseWithThreatLevel("GPL-2.0", "GPL-2.0", 9, licenses.declaredlicenses);
    assertThat(licenses.observedlicenses, hasSize(1));
    assertContainsLicenseWithThreatLevel("No-Sources", "No Sources", 5, licenses.observedlicenses);
    assertThat(licenses.effectiveLicenses, hasSize(1));
    List<LicenseWithThreatLevel> effectiveList = new ArrayList<>(licenses.effectiveLicenses);
    assertContainsLicenseWithThreatLevel("GPL-2.0", "GPL-2.0", 9, effectiveList);
  }

  @Test
  public void testGetLicenses_withNoSourceLicenseForObservedLicenses() throws Exception {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("GPL-2.0"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("No-Source-License"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentLicenses licenses = componentInfoService.getLicenses(applicationPublicId, MAVEN_COORDINATES,
        httpRequestMock);
    assertThat(licenses.declaredlicenses, hasSize(1));
    assertContainsLicenseWithThreatLevel("GPL-2.0", "GPL-2.0", 9, licenses.declaredlicenses);
    assertThat(licenses.observedlicenses, hasSize(1));
    assertContainsLicenseWithThreatLevel("No-Source-License", "No Source License", 5, licenses.observedlicenses);
    assertThat(licenses.effectiveLicenses, hasSize(1));
    List<LicenseWithThreatLevel> effectiveList = new ArrayList<>(licenses.effectiveLicenses);
    assertContainsLicenseWithThreatLevel("GPL-2.0", "GPL-2.0", 9, effectiveList);
  }

  @Test
  public void testGetLicenses_withNotDeclaredForDeclaredLicensesAndNoSourcesForObservedLicenses() throws Exception {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("Not-Declared"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("No-Source-License"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentLicenses licenses = componentInfoService.getLicenses(applicationPublicId, MAVEN_COORDINATES,
        httpRequestMock);
    assertThat(licenses.declaredlicenses, hasSize(1));
    assertContainsLicenseWithThreatLevel("Not-Declared", "Not Declared", 5, licenses.declaredlicenses);
    assertThat(licenses.observedlicenses, hasSize(1));
    assertContainsLicenseWithThreatLevel("No-Source-License", "No Source License", 5, licenses.observedlicenses);
    assertThat(licenses.effectiveLicenses, hasSize(2));
    List<LicenseWithThreatLevel> effectiveList = new ArrayList<>(licenses.effectiveLicenses);
    assertContainsLicenseWithThreatLevel("Not-Declared", "Not Declared", 5, effectiveList);
    assertContainsLicenseWithThreatLevel("No-Source-License", "No Source License", 5, effectiveList);
  }

  @Test
  public void testGetLicenses_claimedComponent() throws Exception {
    // Verify exception is not thrown if component is not known to HDS
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("componentIdentifier", ComponentIdentifierAdapter.toJson(MAVEN_COORDINATES));

    when(
        hdsClientMock.get(httpRequestMock, NamedComponentDetails.class, "rest/" + TOOL_NAME + "/componentDetails",
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
    Organization organization = tempEntity.newOrganization("testGetComponentDetailsList");
    String applicationPublicId = "testGetComponentDetailsList";
    Application application = tempEntity.newApplication(applicationPublicId, applicationPublicId, organization.getId());
    String appId = application.getId();
    // Create license threat groups
    tempEntity.newLicenseThreatGroup(appId, "Group1", 9, "Apache-2.0");
    // Various LTG groups to test case insensitive ordering
    tempEntity.newLicenseThreatGroup(appId, "groupA", 10, "GPL-2.0");
    tempEntity.newLicenseThreatGroup(appId, "Groupb", 10, "GPL-2.0");
    tempEntity.newLicenseThreatGroup(appId, "GroupC", 10, "GPL-2.0");

    // Create the mocked hds response
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "1.0.0");
    ComponentDetails hdsComponentDetails1 = newNamedComponentDetails(componentIdentifier1);
    Set<License> licenses1 = new LinkedHashSet<>();
    licenses1.add(new License("Apache-2.0", "Apache-2.0"));
    hdsComponentDetails1.setDeclaredLicenses(licenses1);
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "2.0.0");
    ComponentDetails hdsComponentDetails2 = newNamedComponentDetails(componentIdentifier2);
    Set<License> licenses2 = new LinkedHashSet<>();
    licenses2.add(new License("GPL-2.0", "GPL-2.0"));
    hdsComponentDetails2.setDeclaredLicenses(licenses2);
    // This should match the default LTG Copyleft from the root organization
    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "3.0.0");
    ComponentDetails hdsComponentDetails3 = newNamedComponentDetails(componentIdentifier3);
    Set<License> licenses3 = new LinkedHashSet<>();
    licenses3.add(new License("OSL-1.0", "OSL-1.0"));
    hdsComponentDetails3.setDeclaredLicenses(licenses3);
    ComponentDetailsList hdsComponentDetailsList = new ComponentDetailsList();
    hdsComponentDetailsList.setList(Arrays.asList(hdsComponentDetails1, hdsComponentDetails2, hdsComponentDetails3));
    mockHdsGetComponentDetailsList(hdsComponentDetailsList);
    ComponentDetailsList componentDetailsList = componentInfoService.getComponentDetailsList(application,
        componentIdentifier1, MatchState.EXACT.getId(), httpRequestMock);
    assertNotNull(componentDetailsList);
    assertEquals(3, componentDetailsList.getList().size());
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
    assertEquals(new Integer(10), componentDetails.getLicenseThreatLevel());
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
    // Test match against default LGT Copyleft from the root organization
    componentDetails = componentDetailsList.getList().get(2);
    assertEquals(componentIdentifier3, componentDetails.getComponentIdentifier());
    assertEquals(new Integer(9), componentDetails.getLicenseThreatLevel());
    assertEquals(1, componentDetails.getLicenseThreatGroupNames().size());
    assertThat(componentDetails.getLicenseThreatGroupNames(), contains("Copyleft"));
    assertEquals(1, componentDetails.getDeclaredLicenses().size());
    assertEquals("OSL-1.0", componentDetails.getDeclaredLicenses().iterator().next().getLicenseName());
    assertEquals("OSL-1.0", componentDetails.getDeclaredLicenses().iterator().next().getLicenseId());
    assertEquals(0, componentDetails.getObservedLicenses().size());
    assertEquals(1, componentDetails.getEffectiveLicenses().size());
    assertEquals("OSL-1.0", componentDetails.getEffectiveLicenses().iterator().next().getLicenseName());
    assertEquals("OSL-1.0", componentDetails.getEffectiveLicenses().iterator().next().getLicenseId());
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

    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    hdsComponentDetails.setHash(hash);
    hdsComponentDetails.addSecurityVulnerability(new SecurityVulnerability("Test Ref Id", "Test Source", 7.5F));
    mockHdsGetComponentDetails(hdsComponentDetails);
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

    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    mockHdsGetComponentDetails(hdsComponentDetails);

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

    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    mockHdsGetComponentDetails(hdsComponentDetails);

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
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    hdsComponentDetails.setHash(hash);
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_COORDINATES,
        MatchState.UNKNOWN.getId(), hash, false /* proprietary */, httpRequestMock);

    assertNotNull(componentDetails);
    assertEquals(MAVEN_COORDINATES, componentDetails.getComponentIdentifier());
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    assertNotNull(policyAlerts);
    assertEquals(1, policyAlerts.size());
    assertEquals("Policy1", policyAlerts.get(0).getTrigger().getPolicyName());

    ComponentIdentifier emptyComponentIdentifier = ComponentIdentifier.createMavenCoordinates("", "", "");
    hdsComponentDetails = newNamedComponentDetails(emptyComponentIdentifier);
    hdsComponentDetails.setHash(hash);
    when(
        hdsClientMock.get(httpRequestMock, NamedComponentDetails.class, "rest/" + TOOL_NAME + "/componentDetails",
            newCoordinatesQueryParam(hdsComponentDetails))).thenThrow(new NotFoundException("unknown GAV"));
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
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    hdsComponentDetails.setHash(hash);
    mockHdsGetComponentDetails(hdsComponentDetails);
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
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    hdsComponentDetails.setHash(hash);
    mockHdsGetComponentDetails(hdsComponentDetails);
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
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    hdsComponentDetails.setHash(hash);
    mockHdsGetComponentDetails(hdsComponentDetails);
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
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    hdsComponentDetails.setHash(hash);
    mockHdsGetComponentDetails(hdsComponentDetails);
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

    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    hdsComponentDetails.setHash(hash);
    mockHdsGetComponentDetails(hdsComponentDetails);
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
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    mockHdsGetComponentDetails(hdsComponentDetails);
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
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(componentIdentifier);
    mockHdsGetComponentDetails(hdsComponentDetails);
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
    ComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    ComponentDetailsList hdsComponentDetailsList = new ComponentDetailsList();
    hdsComponentDetailsList.setList(Arrays.asList(hdsComponentDetails));
    mockHdsGetComponentDetailsList(hdsComponentDetailsList);
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
    ComponentDetails hdsComponentDetails = newNamedComponentDetails(componentIdentifier);
    ComponentDetailsList hdsComponentDetailsList = new ComponentDetailsList();
    hdsComponentDetailsList.setList(Arrays.asList(hdsComponentDetails));
    mockHdsGetComponentDetailsList(hdsComponentDetailsList);
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

    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    hdsComponentDetails.setHash(hash);
    mockHdsGetComponentDetails(hdsComponentDetails);

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

  @Test
  public void testGetSecurityVulnerabilities() throws Exception{
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    String hash = "01234567890123456789";
    SecurityVulnerability vulnerability = new SecurityVulnerability("refId", "source", 5.0f, "summary");
    vulnerability.setStatus("status");
    hdsComponentDetails.setSecurityVulnerabilities(Collections.singletonList(vulnerability));
    hdsComponentDetails.setHash(hash);
    mockHdsGetComponentDetails(hdsComponentDetails);

    ComponentSecurityVulnerabilities retrievedVulnerabilities =
        componentInfoService.getSecurityVulnerabilities(OwnerType.REPOSITORY, repository.getId(), hash,
            MAVEN_COORDINATES, httpRequestMock);
    assertThat(retrievedVulnerabilities.securityVulnerabilities, hasSize(1));
    SecurityVulnerability retrievedVulnerability = retrievedVulnerabilities.securityVulnerabilities.get(0);
    assertThat(retrievedVulnerability.getRefId(), is(vulnerability.getRefId()));
    assertThat(retrievedVulnerability.getSource(), is(vulnerability.getSource()));
    assertThat(retrievedVulnerability.getSeverity(), is(vulnerability.getSeverity()));
    assertThat(retrievedVulnerability.getSummary(), is(vulnerability.getSummary()));
    assertThat(retrievedVulnerability.getStatus(), is(vulnerability.getStatus()));
  }
}
