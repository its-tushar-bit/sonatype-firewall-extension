/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.IOException;
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
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.clm.dto.model.ide.LicenseStatus;
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
import com.sonatype.insight.brain.model.Owner;
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
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ProprietaryConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import org.assertj.core.groups.Tuple;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

public class ComponentInfoServiceTest
    extends AbstractComponentTest
{
  private static final ComponentIdentifier MAVEN_COORDINATES = ComponentIdentifier.createMavenCoordinates("g1", "a1",
      "v1", "", "jar");

  private static final ComponentIdentifier NUGET_COORDINATES = ComponentIdentifier.createNugetCoordinates("a", "v");

  // This is the tool name (ci, ide, rm) used in REST paths for HDS resources. Since we use it when we mock the HDS
  // client, it doesn't really matter what value we use here, because we don't really access HDS REST paths.
  private static final String TOOL_NAME = "ci";

  @Inject
  private ComponentInfoService componentInfoService;

  private String applicationPublicId = "ComponentInfoServiceTest";

  private Application application;

  private Repository repository;

  @Mock
  private HdsClient hdsClientMock;

  @Mock
  private HttpServletRequest httpRequestMock;

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
        hdsClientMock.relay(httpRequestMock, NamedComponentDetails.class, "rest/" + TOOL_NAME + "/componentDetails",
            newCoordinatesQueryParam(hdsComponentDetails))).thenReturn(hdsComponentDetails);
  }

  private void mockHdsGetComponentDetailsList(ComponentDetailsList hdsComponentDetailsList,
                                              ComponentIdentifier identifier)
  {
    when(hdsClientMock.get(ComponentDetailsList.class, "rest/" + TOOL_NAME +
            "/componentDetails/list",
        Collections.singletonMap("componentIdentifier", ComponentIdentifierAdapter.toJson(identifier))))
        .thenReturn(hdsComponentDetailsList);
  }

  @Test
  public void testGetSelectableLicenses() throws Exception {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);

    // Verify that UNSPECIFIED is removed from the result
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("EPL-1.0", "UNSPECIFIED"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    List<License> licenses = componentInfoService.getLicenses(OwnerType.APPLICATION, applicationPublicId,
        MAVEN_COORDINATES, httpRequestMock).selectableLicenses;
    assertThat(licenses).extracting(License::getLicenseId).containsExactlyInAnyOrder("EPL-1.0");

    // Verify that a versionless license is resolved to versioned licenses
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-UNSPECIFIED"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    licenses = componentInfoService.getLicenses(OwnerType.APPLICATION, applicationPublicId, MAVEN_COORDINATES,
        httpRequestMock).selectableLicenses;
    assertThat(licenses).extracting(License::getLicenseId).containsExactlyInAnyOrder("Apache-UNSPECIFIED", "Apache-1.0",
        "Apache-1.1", "Apache-2.0", "Apache-XML-Security-License");

    // Verify that declared and observed licenses are merged
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-2.0", "EPL-1.0"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("EPL-1.0", "GPL-2.0"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    licenses = componentInfoService.getLicenses(OwnerType.APPLICATION, applicationPublicId, MAVEN_COORDINATES,
        httpRequestMock).selectableLicenses;
    assertThat(licenses).extracting(License::getLicenseId).containsExactlyInAnyOrder("Apache-2.0", "EPL-1.0",
        "GPL-2.0");
  }

  @Test
  public void testGetLicenses_NoComponentIdentifier() throws Exception {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      componentInfoService.getLicenses(null, null, null /* componentIdentifier */, httpRequestMock);
    }).withMessage("componentIdentifier is required");
  }

  @Test
  public void testGetLicenses_BadOwnerId() throws Exception {
    testGetLicenses_BadOwnerId(OwnerType.APPLICATION, "Could not find an application with public ID ");
    testGetLicenses_BadOwnerId(OwnerType.REPOSITORY, "Cannot find a repository with ID ");
  }

  private void testGetLicenses_BadOwnerId(final OwnerType ownerType, final String expectedErrMsgPrefix)
      throws Exception
  {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      componentInfoService.getLicenses(ownerType, "bogusOwnerId", MAVEN_COORDINATES, httpRequestMock);
    }).withMessage(expectedErrMsgPrefix + "bogusOwnerId.");
  }

  @Test
  public void testGetLicensesApplication() throws Exception {
    testGetLicenses(OwnerType.APPLICATION, applicationPublicId);
  }

  @Test
  public void testGetLicensesRepository() throws Exception {
    testGetLicenses(OwnerType.REPOSITORY, repository.getId());
  }

  private void testGetLicenses(final OwnerType ownerType, final String ownerId) throws Exception {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);

    // Verify component without licenses
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentLicenses licenses = componentInfoService.getLicenses(ownerType, ownerId, MAVEN_COORDINATES,
        httpRequestMock);
    assertThat(licenses.declaredlicenses).isEmpty();
    assertThat(licenses.observedlicenses).isEmpty();
    assertThat(licenses.effectiveLicenses).isEmpty();
    assertThat(licenses.selectableLicenses).isEmpty();

    final String privateOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    // Verify component with licenses
    tempEntity.newLicenseThreatGroup(
        // Note: For now, only an Org or App (not a Repository) can contain a LTG
        OwnerType.APPLICATION.equals(ownerType) ? privateOwnerId : Organization.ROOT_ORGANIZATION_ID,
        "ComponentInfoServiceTest", 5, "LGPL-2.0", "BSD-3-Clause");

    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-2.0", "LGPL-2.0-MPL-1.1"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("GPL-2.0", "AFL-2.1-BSD-3-Clause"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    licenses = componentInfoService.getLicenses(ownerType, ownerId, MAVEN_COORDINATES, httpRequestMock);
    assertLicenses(licenses.declaredlicenses, tuple("Apache-2.0", "Apache-2.0", 0), tuple("LGPL-2.0", "LGPL-2.0", 5),
        tuple("MPL-1.1", "MPL-1.1", 2));
    assertLicenses(licenses.observedlicenses, tuple("GPL-2.0", "GPL-2.0", 9), tuple("AFL-2.1", "AFL-2.1", 2),
        tuple("BSD-3-Clause", "BSD-3-Clause", 5));
    assertLicenses(licenses.effectiveLicenses, tuple("Apache-2.0", "Apache-2.0", 0), tuple("LGPL-2.0", "LGPL-2.0", 5),
        tuple("MPL-1.1", "MPL-1.1", 2), tuple("GPL-2.0", "GPL-2.0", 9), tuple("AFL-2.1", "AFL-2.1", 2),
        tuple("BSD-3-Clause", "BSD-3-Clause", 5));
    assertThat(licenses.selectableLicenses).extracting(License::getLicenseId).containsExactlyInAnyOrder("Apache-2.0",
        "LGPL-2.0", "MPL-1.1", "GPL-2.0", "BSD-3-Clause", "AFL-2.1");
  }

  @Test
  public void testGetLicensesApplication_withOverride() throws Exception {
    testGetLicenses_withOverride(OwnerType.APPLICATION, applicationPublicId);
  }

  @Test
  public void testGetLicensesRepository_withOverride() throws Exception {
    testGetLicenses_withOverride(OwnerType.REPOSITORY, repository.getId());
  }

  private void testGetLicenses_withOverride(final OwnerType ownerType, final String ownerId) throws Exception {
    final String privateOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    // Verify component with licenses
    // Note: For now, only an Org or App (not a Repository) can contain a LTG
    final String tempEntityOwnerId = OwnerType.APPLICATION.equals(ownerType) ? privateOwnerId
        : Organization.ROOT_ORGANIZATION_ID;
    tempEntity.newLicenseThreatGroup(tempEntityOwnerId, "ComponentInfoServiceTest", 5, "LGPL-2.0", "BSD-3-Clause");
    tempEntity.newLicenseOverride(tempEntityOwnerId, MAVEN_COORDINATES, LicenseOverrideStatus.SELECTED, "BSD-3-Clause");

    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("Apache-2.0", "LGPL-2.0-MPL-1.1"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("GPL-2.0", "AFL-2.1-BSD-3-Clause"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentLicenses licenses = componentInfoService.getLicenses(ownerType, ownerId, MAVEN_COORDINATES,
        httpRequestMock);
    assertLicenses(licenses.declaredlicenses, tuple("Apache-2.0", "Apache-2.0", 0), tuple("LGPL-2.0", "LGPL-2.0", 5),
        tuple("MPL-1.1", "MPL-1.1", 2));
    assertLicenses(licenses.observedlicenses, tuple("GPL-2.0", "GPL-2.0", 9), tuple("AFL-2.1", "AFL-2.1", 2),
        tuple("BSD-3-Clause", "BSD-3-Clause", 5));
    assertLicenses(licenses.effectiveLicenses, tuple("BSD-3-Clause", "BSD-3-Clause", 5));
  }

  @Test
  public void testGetLicenses_withNotDeclaredForDeclaredLicenses() throws Exception {
    testGetLicenses_withNotDeclaredForDeclaredLicenses(OwnerType.APPLICATION, applicationPublicId);
    testGetLicenses_withNotDeclaredForDeclaredLicenses(OwnerType.REPOSITORY, repository.getId());
  }

  private void testGetLicenses_withNotDeclaredForDeclaredLicenses(final OwnerType ownerType, final String ownerId)
      throws Exception
  {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("Not-Declared"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("GPL-2.0"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentLicenses licenses = componentInfoService.getLicenses(ownerType, ownerId, MAVEN_COORDINATES,
        httpRequestMock);
    assertLicenses(licenses.declaredlicenses, tuple("Not-Declared", "Not Declared", 5));
    assertLicenses(licenses.observedlicenses, tuple("GPL-2.0", "GPL-2.0", 9));
    assertLicenses(licenses.effectiveLicenses, tuple("GPL-2.0", "GPL-2.0", 9));
  }

  @Test
  public void testGetLicenses_withNoSourcesForObservedLicenses() throws Exception {
    testGetLicenses_withNoSourcesForObservedLicenses(OwnerType.APPLICATION, applicationPublicId);
    testGetLicenses_withNoSourcesForObservedLicenses(OwnerType.REPOSITORY, repository.getId());
  }

  private void testGetLicenses_withNoSourcesForObservedLicenses(final OwnerType ownerType, final String ownerId)
      throws Exception
  {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("GPL-2.0"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("No-Sources"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentLicenses licenses = componentInfoService.getLicenses(ownerType, ownerId, MAVEN_COORDINATES,
        httpRequestMock);
    assertLicenses(licenses.declaredlicenses, tuple("GPL-2.0", "GPL-2.0", 9));
    assertLicenses(licenses.observedlicenses, tuple("No-Sources", "No Sources", 5));
    assertLicenses(licenses.effectiveLicenses, tuple("GPL-2.0", "GPL-2.0", 9));
  }

  @Test
  public void testGetLicenses_withNoSourceLicenseForObservedLicenses() throws Exception {
    testGetLicenses_withNoSourceLicenseForObservedLicenses(OwnerType.APPLICATION, applicationPublicId);
    testGetLicenses_withNoSourceLicenseForObservedLicenses(OwnerType.REPOSITORY, repository.getId());
  }

  private void testGetLicenses_withNoSourceLicenseForObservedLicenses(final OwnerType ownerType, final String ownerId)
      throws Exception
  {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("GPL-2.0"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("No-Source-License"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentLicenses licenses = componentInfoService.getLicenses(ownerType, ownerId, MAVEN_COORDINATES,
        httpRequestMock);
    assertLicenses(licenses.declaredlicenses, tuple("GPL-2.0", "GPL-2.0", 9));
    assertLicenses(licenses.observedlicenses, tuple("No-Source-License", "No Source License", 5));
    assertLicenses(licenses.effectiveLicenses, tuple("GPL-2.0", "GPL-2.0", 9));
  }

  @Test
  public void testGetLicenses_withNotDeclaredForDeclaredLicensesAndNoSourcesForObservedLicenses() throws Exception {
    testGetLicenses_withNotDeclaredForDeclaredLicensesAndNoSourcesForObservedLicenses(OwnerType.APPLICATION,
        applicationPublicId);
    testGetLicenses_withNotDeclaredForDeclaredLicensesAndNoSourcesForObservedLicenses(OwnerType.REPOSITORY,
        repository.getId());
  }

  private void testGetLicenses_withNotDeclaredForDeclaredLicensesAndNoSourcesForObservedLicenses(
      final OwnerType ownerType,
      final String ownerId) throws Exception
  {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("Not-Declared"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("No-Source-License"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentLicenses licenses = componentInfoService.getLicenses(ownerType, ownerId, MAVEN_COORDINATES,
        httpRequestMock);
    assertLicenses(licenses.declaredlicenses, tuple("Not-Declared", "Not Declared", 5));
    assertLicenses(licenses.observedlicenses, tuple("No-Source-License", "No Source License", 5));
    assertLicenses(licenses.effectiveLicenses, tuple("Not-Declared", "Not Declared", 5),
        tuple("No-Source-License", "No Source License", 5));
  }

  @Test
  public void testGetLicenses_withNotSupportedLicense() throws Exception {
    testGetLicenses_withNotSupportedLicense(OwnerType.APPLICATION,
        applicationPublicId);
    testGetLicenses_withNotSupportedLicense(OwnerType.REPOSITORY,
        repository.getId());
  }

  private void testGetLicenses_withNotSupportedLicense(final OwnerType ownerType,
                                                       final String ownerId)
      throws Exception
  {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(NUGET_COORDINATES);
    hdsComponentDetails.setDeclaredLicenses(toLicenseSet("MIT"));
    hdsComponentDetails.setObservedLicenses(toLicenseSet("Not-Supported"));
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentLicenses licenses = componentInfoService.getLicenses(ownerType, ownerId, NUGET_COORDINATES,
        httpRequestMock);
    assertLicenses(licenses.declaredlicenses, tuple("MIT", "MIT", 0));
    assertLicenses(licenses.observedlicenses, tuple("Not-Supported", "Not Supported", null));
    assertLicenses(licenses.effectiveLicenses, tuple("MIT", "MIT", 0));
    assertThat(licenses.selectableLicenses).isNotEmpty().extracting(License::getLicenseId)
        .doesNotContain("Not-Supported");
  }

  @Test
  public void testGetLicensesApplication_claimedComponent() throws Exception {
    testGetLicenses_claimedComponent(OwnerType.APPLICATION, applicationPublicId);
  }

  @Test
  public void testGetLicensesRepository_claimedComponent() throws Exception {
    testGetLicenses_claimedComponent(OwnerType.REPOSITORY, repository.getId());
  }

  private void testGetLicenses_claimedComponent(final OwnerType ownerType, final String ownerId) throws Exception {
    // Verify exception is not thrown if component is not known to HDS
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("componentIdentifier", ComponentIdentifierAdapter.toJson(MAVEN_COORDINATES));

    when(
        hdsClientMock.relay(httpRequestMock, NamedComponentDetails.class, "rest/" + TOOL_NAME + "/componentDetails",
            queryParams)).thenThrow(new NotFoundException("test"));
    ComponentLicenses licenses = componentInfoService.getLicenses(ownerType, ownerId, MAVEN_COORDINATES,
        httpRequestMock);
    // if we got here, we are good, but let's do some sanity check
    assertThat(licenses.declaredlicenses).isEmpty();
    assertThat(licenses.observedlicenses).isEmpty();
  }

  private void assertLicenses(Iterable<LicenseWithThreatLevel> actual, Tuple... tuples) {
    assertThat(actual).extracting(lwtl -> lwtl.license.getLicenseId(), lwtl -> lwtl.license.getLicenseName(),
        lwtl -> lwtl.threatLevel).containsExactlyInAnyOrder(tuples);
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
    hdsComponentDetailsList.setList(asList(hdsComponentDetails1, hdsComponentDetails2, hdsComponentDetails3));
    mockHdsGetComponentDetailsList(hdsComponentDetailsList, componentIdentifier1);

    ComponentDetailsList componentDetailsList = componentInfoService.getComponentDetailsList(componentIdentifier1);
    componentInfoService.augmentComponentDetails(componentDetailsList.getList(), MatchState.EXACT.getId(), application);

    assertThat(componentDetailsList).isNotNull();
    assertThat(componentDetailsList.getList()).hasSize(3);
    ComponentDetails componentDetails = componentDetailsList.getList().get(0);
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(componentIdentifier1);
    assertThat(componentDetails.getLicenseThreatLevel()).isEqualTo(9);
    assertThat(componentDetails.getLicenseThreatGroupNames()).hasSize(1);
    assertThat(componentDetails.getLicenseThreatGroupNames().get(0)).isEqualTo("Group1");
    assertThat(componentDetails.getDeclaredLicenses()).hasSize(1);
    assertThat(componentDetails.getDeclaredLicenses().iterator().next().getLicenseName()).isEqualTo("Apache-2.0");
    assertThat(componentDetails.getDeclaredLicenses().iterator().next().getLicenseId()).isEqualTo("Apache-2.0");
    assertThat(componentDetails.getObservedLicenses()).isEmpty();
    assertThat(componentDetails.getEffectiveLicenses()).hasSize(1);
    assertThat(componentDetails.getEffectiveLicenses().iterator().next().getLicenseName()).isEqualTo("Apache-2.0");
    assertThat(componentDetails.getEffectiveLicenses().iterator().next().getLicenseId()).isEqualTo("Apache-2.0");
    assertThat(componentDetails.getEffectiveLicenseStatus()).isNull();
    componentDetails = componentDetailsList.getList().get(1);
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(componentIdentifier2);
    assertThat(componentDetails.getLicenseThreatLevel()).isEqualTo(10);
    assertThat(componentDetails.getLicenseThreatGroupNames()).containsExactly("groupA", "Groupb", "GroupC");
    assertThat(componentDetails.getDeclaredLicenses()).hasSize(1);
    assertThat(componentDetails.getDeclaredLicenses().iterator().next().getLicenseName()).isEqualTo("GPL-2.0");
    assertThat(componentDetails.getDeclaredLicenses().iterator().next().getLicenseId()).isEqualTo("GPL-2.0");
    assertThat(componentDetails.getObservedLicenses()).isEmpty();
    assertThat(componentDetails.getEffectiveLicenses()).hasSize(1);
    assertThat(componentDetails.getEffectiveLicenses().iterator().next().getLicenseName()).isEqualTo("GPL-2.0");
    assertThat(componentDetails.getEffectiveLicenses().iterator().next().getLicenseId()).isEqualTo("GPL-2.0");
    assertThat(componentDetails.getEffectiveLicenseStatus()).isNull();
    // Test match against default LGT Copyleft from the root organization
    componentDetails = componentDetailsList.getList().get(2);
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(componentIdentifier3);
    assertThat(componentDetails.getLicenseThreatLevel()).isEqualTo(9);
    assertThat(componentDetails.getLicenseThreatGroupNames()).containsExactly("Copyleft");
    assertThat(componentDetails.getDeclaredLicenses()).hasSize(1);
    assertThat(componentDetails.getDeclaredLicenses().iterator().next().getLicenseName()).isEqualTo("OSL-1.0");
    assertThat(componentDetails.getDeclaredLicenses().iterator().next().getLicenseId()).isEqualTo("OSL-1.0");
    assertThat(componentDetails.getObservedLicenses()).isEmpty();
    assertThat(componentDetails.getEffectiveLicenses()).hasSize(1);
    assertThat(componentDetails.getEffectiveLicenses().iterator().next().getLicenseName()).isEqualTo("OSL-1.0");
    assertThat(componentDetails.getEffectiveLicenses().iterator().next().getLicenseId()).isEqualTo("OSL-1.0");
    assertThat(componentDetails.getEffectiveLicenseStatus()).isNull();
  }

  @Test
  public void testGetComponentDetails_PolicyAlerts() throws Exception {
    String hash = "01234567890123456789";

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    Condition condition1 = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");
    constraint1.addCondition(condition1);
    Policy policy1 = new Policy("PolicyId1", "Policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    addPolicy(applicationPublicId, policy1);

    Constraint constraint2 = new Constraint("C2", "Constraint 2", LogicalOperator.AND);
    constraint2.addCondition(new Condition(MatchStateConditionType.ID, "is not", "similar"));
    Policy policy2 = new Policy("PolicyId2", "Policy2");
    policy2.setThreatLevel(8);
    policy2.addConstraint(constraint2);
    policy2.setAction(BuildStageType.ID, FailActionType.ID);
    addPolicy(applicationPublicId, policy2);

    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    hdsComponentDetails.setHash(hash);
    hdsComponentDetails.addSecurityVulnerability(new SecurityVulnerability("Test Ref Id", "Test Source", 7.5F));
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_COORDINATES,
        MatchState.SIMILAR.getId(), hash, false /* proprietary */, httpRequestMock);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_COORDINATES);
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    assertThat(policyAlerts).hasSize(1);
    assertThat(policyAlerts.get(0).getTrigger().getPolicyName()).isEqualTo("Policy1");
  }

  @Test
  public void testGetComponentDetails_OverriddenLicense() throws Exception {
    tempEntity.newLicenseOverride(application.getId(), MAVEN_COORDINATES, LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0",
        null /* comment */);

    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    mockHdsGetComponentDetails(hdsComponentDetails);

    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_COORDINATES,
        MatchState.EXACT.getId(), null /* hash */, false /* proprietary */, httpRequestMock);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_COORDINATES);
    assertThat( componentDetails.getOverriddenLicenses()).hasSize(1);
    License overriddenLicense = componentDetails.getOverriddenLicenses().iterator().next();
    assertThat(overriddenLicense).isNotNull();
    assertThat(overriddenLicense.getLicenseId()).isEqualTo("GPL-2.0");
    assertThat(overriddenLicense.getLicenseName()).isEqualTo("GPL-2.0");
    assertThat(componentDetails.getLicenseThreatLevel()).isEqualTo(9);
    assertThat(componentDetails.getLicenseThreatGroupNames()).containsExactlyInAnyOrder("Copyleft");
    assertThat(componentDetails.getEffectiveLicenses()).hasSize(1);
    License effectiveLicense = componentDetails.getEffectiveLicenses().iterator().next();
    assertThat(effectiveLicense.getLicenseId()).isEqualTo("GPL-2.0");
    assertThat(effectiveLicense.getLicenseName()).isEqualTo("GPL-2.0");
    assertThat(componentDetails.getEffectiveLicenseStatus()).isEqualTo(LicenseStatus.Overridden);
  }

  @Test
  public void testGetComponentDetails_SelectedLicense() throws Exception {
    tempEntity.newLicenseOverride(application.getId(), MAVEN_COORDINATES, LicenseOverrideStatus.SELECTED, "GPL-2.0",
        null /* comment */);

    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    mockHdsGetComponentDetails(hdsComponentDetails);

    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_COORDINATES,
        MatchState.EXACT.getId(), null /* hash */, false /* proprietary */, httpRequestMock);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_COORDINATES);
    assertThat(componentDetails.getOverriddenLicenses()).hasSize(1);
    License overriddenLicense = componentDetails.getOverriddenLicenses().iterator().next();
    assertThat(overriddenLicense).isNotNull();
    assertThat(overriddenLicense.getLicenseId()).isEqualTo("GPL-2.0");
    assertThat(overriddenLicense.getLicenseName()).isEqualTo("GPL-2.0");
    assertThat(componentDetails.getLicenseThreatLevel()).isEqualTo(9);
    assertThat(componentDetails.getLicenseThreatGroupNames()).containsExactlyInAnyOrder("Copyleft");
    assertThat(componentDetails.getEffectiveLicenses()).hasSize(1);
    License effectiveLicense = componentDetails.getEffectiveLicenses().iterator().next();
    assertThat(effectiveLicense.getLicenseId()).isEqualTo("GPL-2.0");
    assertThat(effectiveLicense.getLicenseName()).isEqualTo("GPL-2.0");
    assertThat(componentDetails.getEffectiveLicenseStatus()).isEqualTo(LicenseStatus.Selected);
  }

  @Test
  public void testGetComponentDetails_UnknownComponent() throws Exception {
    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(MatchStateConditionType.ID, "is", "unknown"));
    Policy policy1 = new Policy("PolicyId1", "Policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    addPolicy(applicationPublicId, policy1);

    String hash = "01234567890123456789";
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    hdsComponentDetails.setHash(hash);
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_COORDINATES,
        MatchState.UNKNOWN.getId(), hash, false /* proprietary */, httpRequestMock);

    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_COORDINATES);
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    assertThat(policyAlerts).hasSize(1);
    assertThat(policyAlerts.get(0).getTrigger().getPolicyName()).isEqualTo("Policy1");

    ComponentIdentifier emptyComponentIdentifier = ComponentIdentifier.createMavenCoordinates("", "", "");
    hdsComponentDetails = newNamedComponentDetails(emptyComponentIdentifier);
    hdsComponentDetails.setHash(hash);
    when(
        hdsClientMock.relay(httpRequestMock, NamedComponentDetails.class, "rest/" + TOOL_NAME + "/componentDetails",
            newCoordinatesQueryParam(hdsComponentDetails))).thenThrow(new NotFoundException("unknown GAV"));
    componentDetails = componentInfoService.getComponentDetails(application, emptyComponentIdentifier,
        MatchState.UNKNOWN.getId(), "01234567890123456789", false /* proprietary */, httpRequestMock);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(emptyComponentIdentifier);
    policyAlerts = componentDetails.getPolicyAlerts();
    assertThat(policyAlerts).hasSize(1);
    assertThat(policyAlerts.get(0).getTrigger().getPolicyName()).isEqualTo("Policy1");
  }

  // CLM-4195
  @Test
  public void testGetComponentDetails_UnknownComponentNullIdentifier() throws Exception {
    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(ProprietaryConditionType.ID, "is true"));
    Policy policy1 = new Policy("PolicyId1", "Policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    addPolicy(applicationPublicId, policy1);

    String hash = "01234567890123456789";
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    hdsComponentDetails.setHash(hash);
    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application,
        null /* componentIdentifier */, MatchState.UNKNOWN.getId(), hash, true /* proprietary */, httpRequestMock);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getHash()).isEqualTo(hash);
    assertThat(componentDetails.getComponentIdentifier()).isNull();

    assertThat(componentDetails.getMatchState()).isEqualTo(MatchState.UNKNOWN.getId());

    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    assertThat(policyAlerts).hasSize(1);
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
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_COORDINATES);
  }

  @Test
  public void testGetComponentDetails_ProprietaryComponent() throws Exception {
    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(ProprietaryConditionType.ID, "is true"));
    Policy policy1 = new Policy("PolicyId1", "Policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    addPolicy(applicationPublicId, policy1);

    String hash = "01234567890123456789";
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    hdsComponentDetails.setHash(hash);
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_COORDINATES,
        MatchState.SIMILAR.getId(), hash, true /* proprietary */, httpRequestMock);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_COORDINATES);
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    assertThat(policyAlerts).hasSize(1);
    assertThat(policyAlerts.get(0).getTrigger().getPolicyName()).isEqualTo("Policy1");

    componentDetails = componentInfoService.getComponentDetails(application, MAVEN_COORDINATES,
        MatchState.SIMILAR.getId(), hash, false /* proprietary */, httpRequestMock);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_COORDINATES);
    policyAlerts = componentDetails.getPolicyAlerts();
    assertThat(policyAlerts).isEmpty();
  }

  @Test
  public void testGetComponentDetails_ManuallyIdentifiedComponent() throws Exception {
    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(MatchStateConditionType.ID, "is", "exact"));
    constraint1.addCondition(new Condition(AgeInDaysConditionType.ID, "younger than", "30"));
    Policy policy1 = new Policy("PolicyId1", "Policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    addPolicy(applicationPublicId, policy1);

    String hash = "01234567890123456789";
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    hdsComponentDetails.setHash(hash);
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_COORDINATES,
        MatchState.SIMILAR.getId(), hash, false /* proprietary */, httpRequestMock);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getHash()).isEqualTo(hash);
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_COORDINATES);
    assertThat(componentDetails.getMatchState()).isEqualTo(MatchState.SIMILAR.getId());
    assertThat(componentDetails.getIdentificationSource()).isEqualTo(IdentificationSource.SONATYPE.getId());
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    assertThat(policyAlerts).isEmpty();

    ComponentIdentifier claimedComponentIdentifier = ComponentIdentifier.createMavenCoordinates("Claimed g",
        "Claimed a", "Claimed v");
    HashComponentIdentifier claimedComponent = tempEntity.newClaimedComponent(hash, claimedComponentIdentifier);
    componentDetails = componentInfoService.getComponentDetails(application, MAVEN_COORDINATES,
        MatchState.SIMILAR.getId(), hash, false /* proprietary */, httpRequestMock);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getHash()).isEqualTo(hash);
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(claimedComponentIdentifier);
    assertThat(componentDetails.getMatchState()).isEqualTo(MatchState.EXACT.getId());
    assertThat(componentDetails.getIdentificationSource()).isEqualTo(IdentificationSource.MANUAL.getId());
    assertThat(componentDetails.getIdentificationSourceComment()).isEqualTo(claimedComponent.getComment());
    policyAlerts = componentDetails.getPolicyAlerts();
    assertThat(policyAlerts).hasSize(1);
    assertThat(policyAlerts.get(0).getTrigger().getPolicyName()).isEqualTo("Policy1");
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
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    addPolicy(applicationPublicId, policy1);

    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    hdsComponentDetails.setHash(hash);
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_COORDINATES,
        MatchState.SIMILAR.getId(), hash, false /* proprietary */, httpRequestMock);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getHash()).isEqualTo(hash);
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_COORDINATES);
    assertThat(componentDetails.getMatchState()).isEqualTo(MatchState.SIMILAR.getId());
    assertThat(componentDetails.getIdentificationSource()).isEqualTo(IdentificationSource.SONATYPE.getId());
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    assertThat(policyAlerts).hasSize(1);
  }

  private void addPolicy(String applicationPublicId, Policy policy) throws Exception {
    String appId = new ApplicationDAO().getByPublicIdNotNull(applicationPublicId).getId();
    PolicyDAO policyDAO = new PolicyDAO();
    policy.setOwnerId(appId);
    policyDAO.insert(policy);
  }

  private static Set<License> toLicenseSet(String... licenseIds) {
    Set<License> result = new LinkedHashSet<>();
    MultiLicenseDAO dao = new MultiLicenseDAO();
    for (String licenseId : licenseIds) {
      MultiLicense multiLicense = dao.getByIdNotNull(licenseId);
      result.add(new License(multiLicense.getId(), multiLicense.getShortDisplayName()));
    }
    return result;
  }

  private void testGetComponentDetails_ReadPermission(final Owner owner, final String ownerId) throws Exception {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    mockHdsGetComponentDetails(hdsComponentDetails);
    ComponentDetails componentDetails = componentInfoService
        .getComponentDetails_ReadPermission(owner.getType(), ownerId, MAVEN_COORDINATES, MatchState.EXACT.getId(),
            null /* hash */, false /* proprietary */, httpRequestMock);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_COORDINATES);
    assertThat(componentDetails.getMatchState()).isEqualTo(MatchState.EXACT.getId());
    assertThat(componentDetails.getIdentificationSource()).isEqualTo(IdentificationSource.SONATYPE.getId());
  }

  @Test
  public void testGetComponentDetails_ReadPermission_Application() throws Exception {
    testGetComponentDetails_ReadPermission(application, applicationPublicId);
  }

  @Test
  public void testGetComponentDetails_ReadPermission_Repository() throws Exception {
    testGetComponentDetails_ReadPermission(repository, repository.getId());
  }

  @Deprecated
  private void testGetComponentDetailsList_ReadPermission(final Owner owner, final String ownerId) throws Exception {
    ComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    ComponentDetailsList hdsComponentDetailsList = new ComponentDetailsList();
    hdsComponentDetailsList.setList(asList(hdsComponentDetails));
    mockHdsGetComponentDetailsList(hdsComponentDetailsList, MAVEN_COORDINATES);
    ComponentDetailsList componentDetailsList = componentInfoService.getComponentDetailsList_ReadPermission(
        owner.getType(), ownerId, MAVEN_COORDINATES, MatchState.EXACT.getId());
    assertThat(componentDetailsList.getList()).hasSize(1);
    ComponentDetails componentDetails = componentDetailsList.getList().get(0);
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_COORDINATES);
    assertThat(componentDetails.getMatchState()).isEqualTo(MatchState.EXACT.getId());
  }

  @Deprecated
  @Test
  public void testGetComponentDetailsList_ReadPermission_Application() throws Exception {
    testGetComponentDetailsList_ReadPermission(application, application.getPublicId());
  }

  @Deprecated
  @Test
  public void testGetComponentDetailsList_ReadPermission_Repository() throws Exception {
    testGetComponentDetailsList_ReadPermission(repository, repository.getId());
  }

  private List<ComponentDetailsDTO> testGetComponentDetailsForAllVersions_ReadPermission(final Owner owner,
                                                                                         final String ownerId)
      throws Exception
  {
    ComponentDetails hdsComponentDetails1 = newNamedComponentDetails(MAVEN_COORDINATES);
    long timestamp = DateTime.now().getMillis();
    hdsComponentDetails1.setCatalogDate(timestamp);
    hdsComponentDetails1.setSecurityVulnerabilities(asList(
        new SecurityVulnerability("cve-8", "cve", 8.1f),
        new SecurityVulnerability("cve-4", "cve", 4f)));
    ComponentDetails hdsComponentDetails2 = newNamedComponentDetails(NUGET_COORDINATES);
    hdsComponentDetails2.setCatalogDate(timestamp);
    hdsComponentDetails2.setSecurityVulnerabilities(asList(
        new SecurityVulnerability("cve-7", "cve", 0.1f))); // too low for our security policy
    ComponentDetailsList hdsComponentDetailsList = new ComponentDetailsList();
    hdsComponentDetailsList.setList(asList(hdsComponentDetails1, hdsComponentDetails2));
    mockHdsGetComponentDetailsList(hdsComponentDetailsList, MAVEN_COORDINATES);

    List<ComponentDetailsDTO> componentDetailsList = componentInfoService
        .getComponentDetailsForAllVersions_ReadPermission(owner.getType(), ownerId, MAVEN_COORDINATES);

    assertThat(componentDetailsList).hasSize(2);

    ComponentDetailsDTO componentDetails1 = componentDetailsList.get(0);
    assertThat(componentDetails1.displayName)
        .hasToString(ComponentDisplayNameUtil.fromIdentifier(MAVEN_COORDINATES).toString());
    assertThat(componentDetails1.matchState).isEqualTo(MatchState.EXACT.getId());
    assertThat(componentDetails1.componentIdentifier).isEqualTo(hdsComponentDetails1.getComponentIdentifier());
    assertThat(componentDetails1.highestSecurityVulnerabilitySeverity).isEqualTo(8.1f);
    assertThat(componentDetails1.catalogDate).isEqualTo(timestamp);

    ComponentDetailsDTO componentDetails2 = componentDetailsList.get(1);
    assertThat(componentDetails2.displayName)
        .hasToString(ComponentDisplayNameUtil.fromIdentifier(NUGET_COORDINATES).toString());
    assertThat(componentDetails2.matchState).isEqualTo(MatchState.EXACT.getId());
    assertThat(componentDetails2.componentIdentifier).isEqualTo(hdsComponentDetails2.getComponentIdentifier());
    assertThat(componentDetails2.highestSecurityVulnerabilitySeverity).isEqualTo(0.1f);
    assertThat(componentDetails2.securityVulnerabilityCount).isEqualTo(1);
    assertThat(componentDetails2.catalogDate).isEqualTo(timestamp);

    return componentDetailsList;
  }

  @Test
  public void testGetComponentDetailsForAllVersions_ReadPermission_Application() throws Exception {
    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "8"));
    Policy policy1 = new Policy("security-high", "Security-High");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    addPolicy(applicationPublicId, policy1);

    Constraint constraint2 = new Constraint("C2", "Constraint 2", LogicalOperator.AND);
    constraint2.addCondition(new Condition(LicenseConditionType.ID, "is not", "GPL-2.0")); // will hit both components
    Policy policy2 = new Policy("NonGpl2", "Non-GPL-2");
    policy2.setThreatLevel(6);
    policy2.addConstraint(constraint2);
    policy2.setAction(BuildStageType.ID, WarnActionType.ID);
    addPolicy(applicationPublicId, policy2);

    List<ComponentDetailsDTO> componentDetailsList = testGetComponentDetailsForAllVersions_ReadPermission(
        application, application.getPublicId());

    ComponentDetailsDTO componentDetails1 = componentDetailsList.get(0);
    assertThat(componentDetails1.policyMaxThreatLevelsByCategory)
        .isEqualTo(ImmutableMap.of(PolicyThreatCategory.SECURITY, 8, PolicyThreatCategory.LICENSE, 6));
    assertThat(componentDetails1.violatedPolicyCount).isEqualTo(2);

    ComponentDetailsDTO componentDetails2 = componentDetailsList.get(1);
    assertThat(componentDetails2.policyMaxThreatLevelsByCategory)
        .isEqualTo(ImmutableMap.of(PolicyThreatCategory.LICENSE, 6));
    assertThat(componentDetails2.violatedPolicyCount).isEqualTo(1);
  }

  @Test
  public void testGetComponentDetailsForAllVersions_ReadPermission_Repository() throws Exception {
    testGetComponentDetailsForAllVersions_ReadPermission(repository, repository.getId());
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
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    addPolicy(applicationPublicId, policy1);

    // policy that doesn't trigger if the corresponding waiver was loaded properly by hash
    Constraint constraint2 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint2.addCondition(new Condition(LabelConditionType.ID, "is", label.getId()));
    Policy policy2 = new Policy("PolicyId2", "Policy Name 2");
    policy2.setThreatLevel(8);
    policy2.addConstraint(constraint1);
    policy2.setAction(BuildStageType.ID, FailActionType.ID);
    addPolicy(applicationPublicId, policy2);
    tempEntity.newWaiver(hash, policy2.getId(), application.getId());

    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    hdsComponentDetails.setHash(hash);
    mockHdsGetComponentDetails(hdsComponentDetails);

    NamedComponentDetails componentDetails = componentInfoService.getComponentDetails(application, MAVEN_COORDINATES,
        MatchState.EXACT.getId(), fullHash, false /* proprietary */, httpRequestMock);
    assertThat(componentDetails).isNotNull();
    assertThat(componentDetails.getHash()).isEqualTo(hash);
    assertThat(componentDetails.getComponentIdentifier()).isEqualTo(MAVEN_COORDINATES);
    assertThat(componentDetails.getMatchState()).isEqualTo(MatchState.EXACT.getId());
    assertThat(componentDetails.getIdentificationSource()).isEqualTo(IdentificationSource.SONATYPE.getId());
    List<PolicyAlert> policyAlerts = componentDetails.getPolicyAlerts();
    assertThat(policyAlerts).hasSize(1);
    assertThat(policyAlerts.get(0).getTrigger().getPolicyName()).isEqualTo(policy1.getName());
  }

  @Test
  public void testGetSecurityVulnerabilities_Repository() throws Exception {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    String hash = "01234567890123456789";
    SecurityVulnerability vulnerability = new SecurityVulnerability("refId", "source", 5.0f, "summary");
    hdsComponentDetails.setSecurityVulnerabilities(Collections.singletonList(vulnerability));
    hdsComponentDetails.setHash(hash);
    mockHdsGetComponentDetails(hdsComponentDetails);

    tempEntity.newSecurityVulnerabilityOverride(repository.getId(), hash, "source", "refId",
        SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED, "abcd");

    ComponentSecurityVulnerabilities retrievedVulnerabilities = componentInfoService.getSecurityVulnerabilities(
        OwnerType.REPOSITORY, repository.getId(), hash, MAVEN_COORDINATES, httpRequestMock);
    assertThat(retrievedVulnerabilities.securityVulnerabilities).hasSize(1);
    SecurityVulnerability retrievedVulnerability = retrievedVulnerabilities.securityVulnerabilities.get(0);
    assertThat(retrievedVulnerability.getRefId()).isEqualTo(vulnerability.getRefId());
    assertThat(retrievedVulnerability.getSource()).isEqualTo(vulnerability.getSource());
    assertThat(retrievedVulnerability.getSeverity()).isEqualTo(vulnerability.getSeverity());
    assertThat(retrievedVulnerability.getSummary()).isEqualTo(vulnerability.getSummary());
    assertThat(retrievedVulnerability.getStatus())
        .isEqualTo(SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED.getName());
  }

  @Test
  public void testGetSecurityVulnerabilities_Application() throws Exception {
    NamedComponentDetails hdsComponentDetails = newNamedComponentDetails(MAVEN_COORDINATES);
    String hash = "01234567890123456789";
    SecurityVulnerability vulnerability = new SecurityVulnerability("refId", "source", 5.0f, "summary");
    hdsComponentDetails.setSecurityVulnerabilities(Collections.singletonList(vulnerability));
    hdsComponentDetails.setHash(hash);
    mockHdsGetComponentDetails(hdsComponentDetails);

    tempEntity.newSecurityVulnerabilityOverride(application.getId(), hash, "source", "refId",
        SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED, "abcd");

    ComponentSecurityVulnerabilities retrievedVulnerabilities = componentInfoService.getSecurityVulnerabilities(
        OwnerType.APPLICATION, application.getPublicId(), hash, MAVEN_COORDINATES, httpRequestMock);
    assertThat(retrievedVulnerabilities.securityVulnerabilities).hasSize(1);
    SecurityVulnerability retrievedVulnerability = retrievedVulnerabilities.securityVulnerabilities.get(0);
    assertThat(retrievedVulnerability.getRefId()).isEqualTo(vulnerability.getRefId());
    assertThat(retrievedVulnerability.getSource()).isEqualTo(vulnerability.getSource());
    assertThat(retrievedVulnerability.getSeverity()).isEqualTo(vulnerability.getSeverity());
    assertThat(retrievedVulnerability.getSummary()).isEqualTo(vulnerability.getSummary());
    assertThat(retrievedVulnerability.getStatus())
        .isEqualTo(SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED.getName());
  }
}
