/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataRequestList.ComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationSummary;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.IdentificationSourceConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.repository.RepositoryReportResource.RepositoryReportSummary;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.google.inject.Binder;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * @since 1.17
 */
@RunWith(MockitoJUnitRunner.class)
public class RepositoryServiceTest
    extends AbstractComponentTest
{
  private static final String MANUAL_REPO_MAN_INSTANCE_ID = "manualDeleteRepoManagerInstanceId";

  private static final String REPO_MAN_INSTANCE_ID = "repoManagerInstanceId";

  private static final String REPO_PUBLIC_ID = "repoPublicId";

  @Inject
  private RepositoryService repositoryService;

  @Inject
  private CLMLicenseManager clmLicenseManager;

  @Inject
  private TestProductLicenseManager productLicenseManager;

  private RepositoryManagerDAO repositoryManagerDAO = new RepositoryManagerDAO();

  private RepositoryDAO repositoryDAO = new RepositoryDAO();

  private RepositoryComponentDAO repositoryComponentDAO = new RepositoryComponentDAO();

  private RepositoryPolicyViolationDAO repositoryPolicyViolationDAO = new RepositoryPolicyViolationDAO();

  @Mock
  private HdsClient hdsClient;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    binder.bind(HdsClient.class).toInstance(hdsClient);
  }

  @After
  public void cleanup() {
    RepositoryManager repositoryManager = repositoryManagerDAO.getByInstanceId(MANUAL_REPO_MAN_INSTANCE_ID);
    if (repositoryManager != null) {
      repositoryManagerDAO.delete(repositoryManager);
    }
  }

  @Test
  public void testEnableRepository_noRepositoryManager() throws Exception {
    repositoryService.enableRepository(MANUAL_REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    RepositoryManager repositoryManager = repositoryManagerDAO.getByInstanceId(MANUAL_REPO_MAN_INSTANCE_ID);

    assertNotNull(repositoryManager);

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());

    assertEquals(1, repositories.size());
    assertEquals(REPO_PUBLIC_ID, repositories.get(0).getPublicId());
    assertTrue(repositories.get(0).isEnabled());
  }

  @Test
  public void testEnableRepository_existingRepositoryManager() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);

    repositoryService.enableRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());

    assertEquals(1, repositories.size());
    assertEquals(REPO_PUBLIC_ID, repositories.get(0).getPublicId());
    assertTrue(repositories.get(0).isEnabled());
  }

  @Test
  public void testEnableRepository_existingRepository() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false);

    repositoryService.enableRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());

    assertEquals(1, repositories.size());
    assertEquals(REPO_PUBLIC_ID, repositories.get(0).getPublicId());
    assertTrue(repositories.get(0).isEnabled());
  }

  @Test
  public void testEnableRepository_MissingLicenseFeature() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    clmLicenseManager.installLicense(null);
    try {
      repositoryService.enableRepository(MANUAL_REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
      fail("Expected exception");
    }
    catch (InvalidLicenseException expected) {
      assertThat(expected.getMessage(), is("Your product license does not support the repository firewall feature."));
    }
  }

  @Test
  public void testSetQuarantine_RepositoryDoesNotExist() throws Exception {
    try {
      repositoryService.setQuarantine(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, true);
      fail("Expected NotFoundException");
    }
    catch (NotFoundException expected) {
      assertThat(expected.getMessage(),
          is("Unknown repository " + REPO_PUBLIC_ID + " for repositoryManagerInstanceId " +
              REPO_MAN_INSTANCE_ID + "."));
    }
  }

  @Test
  public void testSetQuarantine_EnabledWhenRepositoryNotEnabled() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false);

    try {
      repositoryService.setQuarantine(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, true);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      assertThat(expected.getMessage(),
          is("Cannot enable quarantine when repository " + REPO_PUBLIC_ID + " is disabled."));
    }
  }

  @Test
  public void testSetQuarantine_DisabledWhenRepositoryNotEnabled() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false, true);

    // Check that initial value is true
    assertThat(repository.isQuarantineEnabled(), is(true));

    repositoryService.setQuarantine(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, false);
    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isQuarantineEnabled(), is(false));
  }

  @Test
  public void testSetQuarantine_EnabledWhenRepositoryEnabled() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true);

    // Check that the initial value is false
    assertThat(repository.isQuarantineEnabled(), is(false));

    repositoryService.setQuarantine(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, true);
    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isQuarantineEnabled(), is(true));
  }

  @Test
  public void testSetQuarantine_DisabledWhenRepositoryEnabled() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true, true);

    // Check that initial value is true
    assertThat(repository.isQuarantineEnabled(), is(true));

    repositoryService.setQuarantine(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, false);
    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isQuarantineEnabled(), is(false));
  }

  @Test
  public void testGetPolicyEvaluationSummary() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 8, "path1",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    // Now add a waived one that should not show up in the test
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 8, "path1", true, true, "policyId1", "policyName1",
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    // Now add an obsolete one that should not show up in the test
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 8, "path1", false, false,  "policyId2", "policyName2",
        ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"));
    // And one not in the range that should not show up in the test
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, "path4",
        ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4"));

    PolicyEvaluationSummary policyEvaluationSummary =
        repositoryService.getPolicyEvaluationSummary(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    assertThat(policyEvaluationSummary.getCriticalComponentCount(), is(1));
    assertThat(policyEvaluationSummary.getSevereComponentCount(), is(0));
    assertThat(policyEvaluationSummary.getModerateComponentCount(), is(0));
    assertThat(policyEvaluationSummary.getAffectedComponentCount(), is(1));
  }

  @Test
  public void testGetPolicyEvaluationSummary_ComponentIsCriticalAndSevereAndModerate() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 8, "path1",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 4, "path1",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 2, "path1",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));

    PolicyEvaluationSummary policyEvaluationSummary =
        repositoryService.getPolicyEvaluationSummary(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    assertThat(policyEvaluationSummary.getCriticalComponentCount(), is(1));
    assertThat(policyEvaluationSummary.getSevereComponentCount(), is(0));
    assertThat(policyEvaluationSummary.getModerateComponentCount(), is(0));
    assertThat(policyEvaluationSummary.getAffectedComponentCount(), is(1));
  }

  @Test
  public void testGetPolicyEvaluationSummary_SameComponentDifferentPolicy() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 8, "path1", false, true, "policyId1", "policyName1",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 8, "path1", false, true, "policyId2", "policyName2",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));

    PolicyEvaluationSummary policyEvaluationSummary =
        repositoryService.getPolicyEvaluationSummary(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    assertThat(policyEvaluationSummary.getCriticalComponentCount(), is(1));
    assertThat(policyEvaluationSummary.getSevereComponentCount(), is(0));
    assertThat(policyEvaluationSummary.getModerateComponentCount(), is(0));
    assertThat(policyEvaluationSummary.getAffectedComponentCount(), is(1));
  }

  @Test
  public void testGetPolicyEvaluationSummary_MissingLicenseFeature() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    clmLicenseManager.installLicense(null);
    try {
      repositoryService.getPolicyEvaluationSummary(MANUAL_REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
      fail("Expected exception");
    }
    catch (InvalidLicenseException expected) {
      assertThat(expected.getMessage(), is("Your product license does not support the repository firewall feature."));
    }
  }

  @Test
  public void testEvaluateComponents_RepositoryDoesNotExist() throws Exception {
    try {
      repositoryService
          .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null /* componentEvaluationDataRequestList */);
      fail("Expected exception");
    }
    catch (NotFoundException expected) {
      assertThat(expected.getMessage(),
          is("Unknown repository repoPublicId for repositoryManagerInstanceId repoManagerInstanceId."));
    }
  }

  @Test
  public void testEvaluateComponents_ExistingRepository_NotEnabled() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false);

    repositoryService
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null /* componentEvaluationDataRequestList */);

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled(), is(true));
  }

  @Test
  public void testEvaluateComponents_MultipleComponents() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId(), "Test Policy");

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList = new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components = new ArrayList<>();

    // Prepare request and mock the HDS request
    int componentCount = 2;
    Set<License> declaredLicenseSet = Collections.singleton(new License("Apache-2.0", "Apache-2.0"));
    Set<License> observedLicenseSet = Collections.singleton(new License("ATT", "ATT"));
    List<SecurityVulnerability> securityVulnerabilities = createSecurityVulnerabilities();
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    for (int i = 0; i < componentCount; i++) {
      ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g" + i, "a" + i, "v" + i,
          "c" + i, "e" + i);
      componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("path" + i, "h"
          + i, componentIdentifier));
      hdsResult.components.add(createComponentEvaluationData(componentIdentifier, "h" + i, MatchState.EXACT,
          i /* index */, declaredLicenseSet, observedLicenseSet, securityVulnerabilities, i /* popularity */));
    }
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult);

    // Call the service
    Date before = new Date();
    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList);
    Date after = new Date();

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled(), is(true));

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents, hasSize(2));
    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO.getByRepositoryId(repository
        .getId());
    assertThat(policyViolations, hasSize(2));

    for (int i = 0; i < componentCount; i++) {
      String pathname = "path" + i;
      ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g" + i, "a" + i, "v" + i,
          "c" + i, "e" + i);
      String hash = "h" + i;

      RepositoryComponent repositoryComponent = repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(),
          pathname);
      assertRepositoryComponent(repository.getId(), pathname, before, after, hash, componentIdentifier,
          MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), false /* canBeQuarantined */,
          repositoryComponent);

      RepositoryPolicyViolation policyViolation = repositoryPolicyViolationDAO.getLastByRepositoryIdAndPathname(
          repository.getId(), pathname).get(0);
      assertPolicyViolation(repository.getId(), pathname, policy.getId(), policy.getName(), policy.getThreatLevel(),
          policy.getThreatCategory(), hash, componentIdentifier, before, after, policyViolation);
    }
  }

  @Test
  public void testEvaluateComponents_Reevaluation() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    Condition condition = new Condition(LicenseConditionType.ID, "is", "Apache-2.0");
    Constraint constraint = new Constraint("id", "name", LogicalOperator.AND);
    constraint.addCondition(condition);
    Policy policy = new Policy("id", "name");
    policy.setOwnerId(repository.getParentOwnerId());
    policy.addConstraint(constraint);
    tempEntity.newPolicy(policy);

    String hash = "h";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");

    // Prepare request and mock the HDS request
    Set<License> declaredLicenseSet = Collections.singleton(new License("Apache-2.0", "Apache-2.0"));
    Set<License> observedLicenseSet = Collections.singleton(new License("ATT", "ATT"));
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList = new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components = new ArrayList<>();
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("path", hash,
        componentIdentifier));
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT, 0 /* index */,
        declaredLicenseSet, observedLicenseSet, null /* securityVulnerabilities */, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult);

    // Call the service first time
    Date before1 = new Date();
    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList);
    Date after1 = new Date();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents, hasSize(1));
    RepositoryComponent repositoryComponent = repositoryComponents.get(0);
    assertRepositoryComponent(repository.getId(), "path", before1, after1, hash, componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), false /* canBeQuarantined */,
        repositoryComponent);

    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO.getByRepositoryId(repository
        .getId());
    assertThat(policyViolations, hasSize(1));
    assertPolicyViolation(repository.getId(), "path", policy.getId(), policy.getName(), policy.getThreatLevel(),
        policy.getThreatCategory(), hash, componentIdentifier, before1, after1, policyViolations.get(0));

    // Call the service second time
    String updatedHash = "h1";
    ComponentIdentifier updatedComponentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1",
        "e1");
    componentEvaluationDataRequestList = new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components = new ArrayList<>();
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("path", updatedHash,
        componentIdentifier));
    hdsResult.components = new ArrayList<>();
    hdsResult.components.add(createComponentEvaluationData(updatedComponentIdentifier, updatedHash, MatchState.EXACT,
        0 /* index */, declaredLicenseSet, observedLicenseSet, null /* securityVulnerabilities */, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult);
    Date before2 = new Date();
    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList);
    Date after2 = new Date();

    repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents, hasSize(1));
    repositoryComponent = repositoryComponents.get(0);
    assertRepositoryComponent(repository.getId(), "path", before1, after1, updatedHash, updatedComponentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), before2, after2, false /* canBeQuarantined */,
        repositoryComponent);

    policyViolations = repositoryPolicyViolationDAO.getByRepositoryId(repository.getId());
    assertThat(policyViolations, hasSize(2));
    for (RepositoryPolicyViolation policyViolation : policyViolations) {
      if (policyViolation.isLatestEvaluation()) {
        assertPolicyViolation(repository.getId(), "path", policy.getId(), policy.getName(), policy.getThreatLevel(),
            policy.getThreatCategory(), updatedHash, updatedComponentIdentifier, before2, after2, policyViolation);
      }
      else {
        assertPolicyViolation(repository.getId(), "path", policy.getId(), policy.getName(), policy.getThreatLevel(),
            policy.getThreatCategory(), hash, componentIdentifier, before1, after1, policyViolation);
      }
    }
  }

  @Test
  public void testEvaluateComponents_LicenseOverridden() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    Condition condition = new Condition(LicenseConditionType.ID, "is", "GPL-2.0");
    Constraint constraint = new Constraint("id", "name", LogicalOperator.AND);
    constraint.addCondition(condition);
    Policy policy = new Policy("id", "name");
    policy.setOwnerId(repository.getParentOwnerId());
    policy.addConstraint(constraint);
    tempEntity.newPolicy(policy);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    tempEntity.newLicenseOverride(repository.getId(), componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0");

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList = new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components = new ArrayList<>();

    // Prepare request and mock the HDS request
    Set<License> declaredLicenseSet = Collections.singleton(new License("Apache-2.0", "Apache-2.0"));
    Set<License> observedLicenseSet = Collections.singleton(new License("ATT", "ATT"));
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("path", "h",
        componentIdentifier));
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, "h", MatchState.EXACT, 0 /* index */,
        declaredLicenseSet, observedLicenseSet, null /* securityVulnerabilities */, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult);

    // Call the service
    Date before = new Date();
    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList);
    Date after = new Date();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents, hasSize(1));
    RepositoryComponent repositoryComponent = repositoryComponents.get(0);
    assertRepositoryComponent(repository.getId(), "path", before, after, "h", componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), false /* canBeQuarantined */,
        repositoryComponent);

    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO.getByRepositoryId(repository
        .getId());
    assertThat(policyViolations, hasSize(1));
    assertPolicyViolation(repository.getId(), "path", policy.getId(), policy.getName(), policy.getThreatLevel(),
        policy.getThreatCategory(), "h", componentIdentifier, before, after, policyViolations.get(0));
  }

  @Test
  public void testEvaluateComponents_ClaimedComponent() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    Condition condition = new Condition(IdentificationSourceConditionType.ID, "is", IdentificationSource.MANUAL.getId());
    Constraint constraint = new Constraint("id", "name", LogicalOperator.AND);
    constraint.addCondition(condition);
    Policy policy = new Policy("id", "name");
    policy.setOwnerId(repository.getParentOwnerId());
    policy.addConstraint(constraint);
    tempEntity.newPolicy(policy);

    ComponentIdentifier claimedComponentIdentifier = ComponentIdentifier.createMavenCoordinates("cg", "ca", "cv", "cc",
        "ce");
    tempEntity.newClaimedComponent("h", claimedComponentIdentifier);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList = new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components = new ArrayList<>();

    // Prepare request and mock the HDS request
    Set<License> declaredLicenseSet = Collections.singleton(new License("Apache-2.0", "Apache-2.0"));
    Set<License> observedLicenseSet = Collections.singleton(new License("ATT", "ATT"));
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("path", "h",
        componentIdentifier));
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, "h", MatchState.EXACT, 0 /* index */,
        declaredLicenseSet, observedLicenseSet, null /* securityVulnerabilities */, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult);

    // Call the service
    Date before = new Date();
    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList);
    Date after = new Date();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents, hasSize(1));
    assertRepositoryComponent(repository.getId(), "path", before, after, "h", claimedComponentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.MANUAL.getId(), false /* canBeQuarantined */,
        repositoryComponents.get(0));

    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO.getByRepositoryId(repository
        .getId());
    assertThat(policyViolations, hasSize(1));
    assertPolicyViolation(repository.getId(), "path", policy.getId(), policy.getName(), policy.getThreatLevel(),
        policy.getThreatCategory(), "h", claimedComponentIdentifier, before, after, policyViolations.get(0));
  }

  @Test
  public void testEvaluateComponents_LongHash() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    Condition condition = new Condition(LicenseConditionType.ID, "is", "Apache-2.0");
    Constraint constraint = new Constraint("id", "name", LogicalOperator.AND);
    constraint.addCondition(condition);
    Policy policy = new Policy("id", "name");
    policy.setOwnerId(repository.getParentOwnerId());
    policy.addConstraint(constraint);
    tempEntity.newPolicy(policy);

    String hash = "01234567890123456789";
    String longHash = hash + "1";
    // Sanity check
    assertThat(longHash.length(), greaterThan(HashHelper.MAX_LENGTH));
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");

    // Prepare request and mock the HDS request
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList = new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components = new ArrayList<>();
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("path", longHash,
        componentIdentifier));
    Set<License> declaredLicenseSet = Collections.singleton(new License("Apache-2.0", "Apache-2.0"));
    Set<License> observedLicenseSet = Collections.singleton(new License("ATT", "ATT"));
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT, 0 /* index */,
        declaredLicenseSet, observedLicenseSet, null /* securityVulnerabilities */, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult);

    // Call the service
    Date before = new Date();
    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList);
    Date after = new Date();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents, hasSize(1));
    RepositoryComponent repositoryComponent = repositoryComponents.get(0);
    assertRepositoryComponent(repository.getId(), "path", before, after, hash, componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), false /* canBeQuarantined */,
        repositoryComponent);

    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO.getByRepositoryId(repository
        .getId());
    assertThat(policyViolations, hasSize(1));
    assertPolicyViolation(repository.getId(), "path", policy.getId(), policy.getName(), policy.getThreatLevel(),
        policy.getThreatCategory(), hash, componentIdentifier, before, after, policyViolations.get(0));
  }

  @Test
  public void testEvaluateComponents_UnknownComponent() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    Condition condition = new Condition(MatchStateConditionType.ID, "is", MatchState.UNKNOWN.getId());
    Constraint constraint = new Constraint("id", "name", LogicalOperator.AND);
    constraint.addCondition(condition);
    Policy policy = new Policy("id", "name");
    policy.setOwnerId(repository.getParentOwnerId());
    policy.addConstraint(constraint);
    tempEntity.newPolicy(policy);

    String hash = "hash";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");

    // Prepare request and mock the HDS request
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList = new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components = new ArrayList<>();
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("path", hash,
        componentIdentifier));
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    hdsResult.components.add(createComponentEvaluationData(null /* componentIdentifier */, hash, MatchState.UNKNOWN,
        0 /* index */, Collections.<License> emptySet(), Collections.<License> emptySet(),
        null /* securityVulnerabilities */, null /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult);

    // Call the service
    Date before = new Date();
    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList);
    Date after = new Date();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents, hasSize(1));
    RepositoryComponent repositoryComponent = repositoryComponents.get(0);
    assertRepositoryComponent(repository.getId(), "path", before, after, hash, componentIdentifier,
        MatchState.UNKNOWN.getId(), IdentificationSource.SONATYPE.getId(), false /* canBeQuarantined */,
        repositoryComponent);

    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO.getByRepositoryId(repository
        .getId());
    assertThat(policyViolations, hasSize(1));
    assertPolicyViolation(repository.getId(), "path", policy.getId(), policy.getName(), policy.getThreatLevel(),
        policy.getThreatCategory(), hash, componentIdentifier, before, after, policyViolations.get(0));
  }

  @Test
  public void testEvaluateComponents_MultipleHdsResultsForSameHash() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    String hash = "hash";
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNugetCoordinates("p", "v1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNugetCoordinates("p", "v2");
    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createNugetCoordinates("p", "v3");

    // Prepare request and mock the HDS request
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList = new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components = new ArrayList<>();
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("path", hash,
        componentIdentifier2));
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    hdsResult.components
        .add(createComponentEvaluationData(componentIdentifier1, hash, MatchState.EXACT, 0 /* index */,
            Collections.<License> emptySet(), Collections.<License> emptySet(), null /* securityVulnerabilities */,
            null /* popularity */));
    hdsResult.components
        .add(createComponentEvaluationData(componentIdentifier2, hash, MatchState.EXACT, 0 /* index */,
            Collections.<License> emptySet(), Collections.<License> emptySet(), null /* securityVulnerabilities */,
            null /* popularity */));
    hdsResult.components
        .add(createComponentEvaluationData(componentIdentifier3, hash, MatchState.EXACT, 0 /* index */,
            Collections.<License> emptySet(), Collections.<License> emptySet(), null /* securityVulnerabilities */,
            null /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult);

    // Call the service
    Date before = new Date();
    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList);
    Date after = new Date();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents, hasSize(1));
    RepositoryComponent repositoryComponent = repositoryComponents.get(0);
    assertRepositoryComponent(repository.getId(), "path", before, after, hash, componentIdentifier2,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), false /* canBeQuarantined */,
        repositoryComponent);
  }

  @Test
  public void testEvaluateComponents_NullPathname() throws Exception {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    String pathname = null;
    String hash = "hash";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList = new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components = new ArrayList<>();
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest(pathname, hash,
        componentIdentifier));

    try {
      repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList);
      fail("Expected exception");
    }
    catch (BadRequestException expected) {
      assertThat(expected.getMessage(), is("The pathname cannot be null or empty."));
    }
  }

  @Test
  public void testEvaluateComponents_EmptyPathname() throws Exception {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    String pathname = " ";
    String hash = "hash";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList = new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components = new ArrayList<>();
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest(pathname, hash,
        componentIdentifier));

    try {
      repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList);
      fail("Expected exception");
    }
    catch (BadRequestException expected) {
      assertThat(expected.getMessage(), is("The pathname cannot be null or empty."));
    }
  }

  @Test
  public void testEvaluateComponents_NullHash() throws Exception {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    String pathname = "path";
    String hash = null;
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList = new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components = new ArrayList<>();
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest(pathname, hash,
        componentIdentifier));

    try {
      repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList);
      fail("Expected exception");
    }
    catch (BadRequestException expected) {
      assertThat(expected.getMessage(), is("The hash cannot be null or empty."));
    }
  }

  @Test
  public void testEvaluateComponents_EmptyHash() throws Exception {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    String pathname = "path";
    String hash = " ";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList = new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components = new ArrayList<>();
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest(pathname, hash,
        componentIdentifier));

    try {
      repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList);
      fail("Expected exception");
    }
    catch (BadRequestException expected) {
      assertThat(expected.getMessage(), is("The hash cannot be null or empty."));
    }
  }

  @Test
  public void testGetReportSummary() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repo = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID);
    RepositoryComponent component1 = tempEntity.newRepositoryComponent(repo.getId(), "1");
    RepositoryComponent component2 = tempEntity.newRepositoryComponent(repo.getId(), "2");
    RepositoryComponent component3 = tempEntity.newRepositoryComponent(repo.getId(), "3");
    RepositoryComponent component4 = tempEntity.newRepositoryComponent(repo.getId(), "4");
    tempEntity.newRepositoryComponent(repo.getId(), MatchState.UNKNOWN, null);

    tempEntity.newRepositoryPolicyViolation(repo.getId(), 1, component1.getPathname(), null);
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 5, component2.getPathname(), null);
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 6, component3.getPathname(), null);
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 9, component4.getPathname(), null);

    RepositoryReportSummary summary = repositoryService.getReportSummary(repositoryManager.getInstanceId(),
        repo.getPublicId());

    assertThat(summary.knownComponentCount, is(4));
    assertThat(summary.totalComponentCount, is(5));
    assertThat(summary.criticalComponentCount, is(1));
    assertThat(summary.severeComponentCount, is(2));
    assertThat(summary.moderateComponentCount, is(0));
    assertThat(summary.affectedComponentCount, is(3));
  }

  @Test
  public void testEvaluateComponents_MissingLicenseFeature() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    clmLicenseManager.installLicense(null);
    try {
      repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null);
      fail("Expected exception");
    }
    catch (InvalidLicenseException expected) {
      assertThat(expected.getMessage(), is("Your product license does not support the repository firewall feature."));
    }
  }

  private void mockHdsRequest(RepositoryComponentEvaluationDataRequestList serviceRequest,
      ComponentEvaluationDataList hdsResult) throws IOException
  {
    ComponentEvaluationDataRequestList hdsRequest = new ComponentEvaluationDataRequestList();
    hdsRequest.components = new ArrayList<>();
    for (RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest : serviceRequest.components) {
      String hash = HashHelper.truncateHash(componentEvaluationDataRequest.hash);
      hdsRequest.components.add(new ComponentEvaluationDataRequest(hash, null /* componentIdentifier */));
    }
    when(
        hdsClient.post(eq(ComponentEvaluationDataList.class), eq(RepositoryService.HDS_COMPONENT_DETAILS_PATH),
            eq(hdsRequest))).thenReturn(hdsResult);
  }

  private ComponentEvaluationData createComponentEvaluationData(ComponentIdentifier componentIdentifier, String hash,
      MatchState matchState, int index, Set<License> declaredLicenses, Set<License> observedLicenses,
      List<SecurityVulnerability> securityVulnerabilities, Integer relativePopularity)
  {
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    componentEvaluationData.requestIndex = index;
    componentEvaluationData.hash = hash;
    componentEvaluationData.componentIdentifier = componentIdentifier;
    componentEvaluationData.matchState = matchState.getId();
    componentEvaluationData.declaredLicenses = declaredLicenses;
    componentEvaluationData.observedLicenses = observedLicenses;
    componentEvaluationData.catalogDate = (long) index;
    componentEvaluationData.securityVulnerabilities = securityVulnerabilities;
    componentEvaluationData.relativePopularity = relativePopularity;

    return componentEvaluationData;
  }

  private List<SecurityVulnerability> createSecurityVulnerabilities() {
    List<SecurityVulnerability> securityVulnerabilities = new ArrayList<>();
    SecurityVulnerability securityVulnerability = new SecurityVulnerability();
    securityVulnerability.setRefId("refId");
    securityVulnerability.setSeverity(5.0F);
    securityVulnerability.setSource("source");
    securityVulnerability.setUrl("test-url");
    securityVulnerabilities.add(securityVulnerability);
    return securityVulnerabilities;
  }

  private void assertRepositoryComponent(String repositoryId, String pathname, Date beforeCreate, Date afterCreate,
      String hash, ComponentIdentifier componentIdentifier, String matchStateId, String identificationSourceId,
      Date beforeLastEvaluation, Date afterLastEvaluation, boolean canBeQuarantined, RepositoryComponent actual)
  {
    assertThat(actual.getRepositoryId(), is(repositoryId));
    assertThat(actual.getPathname(), is(pathname));
    assertThat(actual.getHash(), is(hash));
    assertThat(actual.getTime(), greaterThanOrEqualTo(beforeCreate));
    assertThat(actual.getTime(), lessThanOrEqualTo(afterCreate));
    assertThat(actual.getComponentIdentifier(), is(componentIdentifier));
    assertThat(actual.getMatchStateId(), is(matchStateId));
    assertThat(actual.getIdentificationSourceId(), is(identificationSourceId));
    assertThat(actual.getLastEvaluationTime(), greaterThanOrEqualTo(beforeLastEvaluation));
    assertThat(actual.getLastEvaluationTime(), lessThanOrEqualTo(afterLastEvaluation));
    assertThat(actual.isCanBeQuarantined(), is(canBeQuarantined));
  }

  private void assertRepositoryComponent(String repositoryId, String pathname, Date beforeCreate, Date afterCreate,
      String hash, ComponentIdentifier componentIdentifier, String matchStateId, String identificationSourceId,
      boolean canBeQuarantined, RepositoryComponent actual)
  {
    assertRepositoryComponent(repositoryId, pathname, beforeCreate, afterCreate, hash, componentIdentifier,
        matchStateId, identificationSourceId, beforeCreate, afterCreate, canBeQuarantined, actual);
  }

  private void assertPolicyViolation(String repositoryId, String pathname, String policyId, String policyName,
      int threatLevel, PolicyThreatCategory threatCategory, String hash, ComponentIdentifier componentIdentifier,
      Date before, Date after, RepositoryPolicyViolation actual)
  {
    assertThat(actual.getRepositoryId(), is(repositoryId));
    assertThat(actual.getPathname(), is(pathname));
    assertThat(actual.getPolicyId(), is(policyId));
    assertThat(actual.getPolicyName(), is(policyName));
    assertThat(actual.getThreatLevel(), is(threatLevel));
    assertThat(actual.getThreatCategory(), is(threatCategory));
    assertThat(actual.getHash(), is(hash));
    assertThat(actual.getComponentIdentifier(), is(componentIdentifier));
    assertThat(actual.getTime(), greaterThanOrEqualTo(before));
    assertThat(actual.getTime(), lessThanOrEqualTo(after));
  }
}
