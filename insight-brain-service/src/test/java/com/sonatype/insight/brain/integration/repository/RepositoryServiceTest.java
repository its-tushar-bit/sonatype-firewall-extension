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
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.RepositoryPolicyEvaluationSummary;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.hds.FirewallAuditHdsClient;
import com.sonatype.insight.brain.hds.FirewallQuarantineHdsClient;
import com.sonatype.insight.brain.integration.repository.RepositoryService.RepositoryDTO;
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
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.repository.RepositoryPolicyThreatDTO;
import com.sonatype.insight.brain.repository.RepositoryPolicyViolationDTO;
import com.sonatype.insight.brain.repository.RepositoryReportDetail;
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
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
  private FirewallAuditHdsClient auditHdsClient;

  @Mock
  private FirewallQuarantineHdsClient quarantineHdsClient;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    binder.bind(FirewallAuditHdsClient.class).toInstance(auditHdsClient);
    binder.bind(FirewallQuarantineHdsClient.class).toInstance(quarantineHdsClient);
  }

  @After
  public void cleanup() {
    RepositoryManager repositoryManager = repositoryManagerDAO.getByInstanceId(MANUAL_REPO_MAN_INSTANCE_ID);
    if (repositoryManager != null) {
      repositoryManagerDAO.delete(repositoryManager);
    }
  }

  @Test
  public void testGetPolicyThreats() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    String pathname = "path1";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 8, pathname, false, true, "policyId1", "policyName1",
        componentIdentifier);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 7, pathname, true, true, "policyId2", "policyName2",
        componentIdentifier);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 8, pathname, false, false, "policyId3", "policyName3",
        componentIdentifier);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, "path4", false, true, "policyId4", "policyName4",
        componentIdentifier);

    tempEntity.newRepositoryComponent(repository.getId(), pathname, new Date(), null);

    RepositoryPolicyThreatDTO repositoryPolicyThreatDTO =
        repositoryService.getPolicyThreats(repository.getId(), pathname);

    assertThat(repositoryPolicyThreatDTO.activePolicyViolations, hasSize(1));
    RepositoryPolicyViolationDTO repositoryViolationDTO = repositoryPolicyThreatDTO.activePolicyViolations.get(0);
    assertThat(repositoryViolationDTO.policyId, is("policyId1"));
    assertThat(repositoryViolationDTO.policyName, is("policyName1"));
    assertThat(repositoryViolationDTO.policyThreatLevel, is(8));
  }

  @Test
  public void testGetPolicyThreats_RepositoryComponentDoesNotExist() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    try {
      repositoryService.getPolicyThreats(repository.getId(), "pathDoesNotExist");
      fail("Expected NotFoundException");
    }
    catch (NotFoundException e) {
      assertThat(e.getMessage(),
          is("Cannot find a component with path pathDoesNotExist in repository with ID " + repository.getId() + "."));
    }
  }

  @Test
  public void testGetPolicyThreats_RepositoryDoesNotExist() {
    try {
      repositoryService.getPolicyThreats("RepositoryIdDoesNotExist", null);
      fail("Expected NotFoundException");
    }
    catch (NotFoundException e) {
      assertThat(e.getMessage(), is("Cannot find a repository with ID RepositoryIdDoesNotExist."));
    }
  }

  @Test
  public void testSetEnabled_NoRepositoryManager() throws Exception {
    repositoryService.setEnabled(MANUAL_REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, true);

    RepositoryManager repositoryManager = repositoryManagerDAO.getByInstanceId(MANUAL_REPO_MAN_INSTANCE_ID);

    assertNotNull(repositoryManager);

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());

    assertEquals(1, repositories.size());
    assertEquals(REPO_PUBLIC_ID, repositories.get(0).getPublicId());
    assertTrue(repositories.get(0).isEnabled());
  }

  @Test
  public void testSetEnabled_ExistingRepositoryManager() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);

    repositoryService.setEnabled(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, true);

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());

    assertEquals(1, repositories.size());
    assertEquals(REPO_PUBLIC_ID, repositories.get(0).getPublicId());
    assertTrue(repositories.get(0).isEnabled());
  }

  @Test
  public void testSetEnabled_TrueExistingRepository() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false);

    repositoryService.setEnabled(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, true);

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());

    assertEquals(1, repositories.size());
    assertEquals(REPO_PUBLIC_ID, repositories.get(0).getPublicId());
    assertTrue(repositories.get(0).isEnabled());
  }

  @Test
  public void testSetEnabled_MissingLicenseFeature() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    clmLicenseManager.installLicense(null);
    try {
      repositoryService.setEnabled(MANUAL_REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, true);
      fail("Expected exception");
    }
    catch (InvalidLicenseException expected) {
      assertThat(expected.getMessage(), is(InvalidLicenseException.INVALID_LICENSE_MSG));
    }
  }

  @Test
  public void testSetEnabled_FalseExistingRepository() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true);

    repositoryService.setEnabled(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, false);

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());

    assertEquals(1, repositories.size());
    assertEquals(REPO_PUBLIC_ID, repositories.get(0).getPublicId());
    assertFalse(repositories.get(0).isEnabled());
  }

  @Test
  public void testSetQuarantine_RepositoryDoesNotExist() throws Exception {
    try {
      repositoryService.setQuarantine(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, true);
      fail("Expected NotFoundException");
    }
    catch (NotFoundException expected) {
      assertThat(expected.getMessage(), is(RepositoryDAO.getErrMsgMissingRepo(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID)));
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

    // Check initial state
    assertThat(repository.isEnabled(), is(false));
    assertThat(repository.isQuarantineEnabled(), is(false));

    repositoryService.setQuarantine(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, false);
    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled(), is(false));
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
    assertThat(repository.isEnabled(), is(true));
    assertThat(repository.isQuarantineEnabled(), is(true));

    repositoryService.setQuarantine(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, false);
    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled(), is(true));
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
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 8, "path1", false, false, "policyId2", "policyName2",
        ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"));
    // And one not in the range that should not show up in the test
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, "path4",
        ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4"));

    // And a quarantined component
    tempEntity.newRepositoryComponent(repository.getId(), "/quarantined", new Date(), null);

    RepositoryPolicyEvaluationSummary policyEvaluationSummary =
        repositoryService.getPolicyEvaluationSummary(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    assertThat(policyEvaluationSummary.getCriticalComponentCount(), is(1));
    assertThat(policyEvaluationSummary.getSevereComponentCount(), is(0));
    assertThat(policyEvaluationSummary.getModerateComponentCount(), is(0));
    assertThat(policyEvaluationSummary.getAffectedComponentCount(), is(1));
    assertThat(policyEvaluationSummary.getReportUrl(), is("ui/links/repository/" + repository.getId() + "/result"));
    assertThat(policyEvaluationSummary.getQuarantinedComponentCount(), is(1));
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

    RepositoryPolicyEvaluationSummary policyEvaluationSummary =
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

    RepositoryPolicyEvaluationSummary policyEvaluationSummary =
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
      assertThat(expected.getMessage(), is(InvalidLicenseException.INVALID_LICENSE_MSG));
    }
  }

  @Test
  public void testEvaluateComponentWithQuarantine_RepositoryDoesNotExist() throws Exception {
    try {
      repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null, true);
      fail("Expected NotFoundException");
    }
    catch (NotFoundException expected) {
      assertThat(expected.getMessage(), is(RepositoryDAO.getErrMsgMissingRepo(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID)));
    }
  }

  @Test
  public void testEvaluateComponentWithQuarantine_NullRequest() throws Exception {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    RepositoryComponentEvaluationDataList componentEvaluationResultList =
        repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null, true);
    assertThat(componentEvaluationResultList.componentEvalResults, hasSize(0));
  }

  @Test
  public void testEvaluateComponentWithQuarantine_EmptyPathname() throws Exception {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    RepositoryComponentEvaluationDataRequest repositoryComponentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest();
    repositoryComponentEvaluationDataRequest.format = "maven";
    repositoryComponentEvaluationDataRequest.hash = "hash";
    repositoryComponentEvaluationDataRequest.pathname = "";

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components = new ArrayList<>();
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);

    try {
      repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
          componentEvaluationDataRequestList, true);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      assertThat(expected.getMessage(), is("The pathname cannot be null or empty."));
    }
  }

  @Test
  public void testEvaluateComponentWithQuarantine_EmptyHash() throws Exception {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    RepositoryComponentEvaluationDataRequest repositoryComponentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest();
    repositoryComponentEvaluationDataRequest.format = "maven";
    repositoryComponentEvaluationDataRequest.hash = "";
    repositoryComponentEvaluationDataRequest.pathname = "path";
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components = new ArrayList<>();
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);

    try {
      repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
          componentEvaluationDataRequestList, true);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      assertThat(expected.getMessage(), is("The hash cannot be null or empty."));
    }
  }

  @Test
  public void testEvaluateComponentWithQuarantine() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    Policy policy = createQuarantiningPolicy(repository);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Prepare request and mock the HDS request
    Set<License> declaredLicenseSet = Collections.singleton(new License("Apache-2.0", "Apache-2.0"));
    Set<License> observedLicenseSet = Collections.singleton(new License("ATT", "ATT"));
    List<SecurityVulnerability> securityVulnerabilities = createSecurityVulnerabilities();
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    String pathname = "path";
    String hash = "h";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    RepositoryComponentEvaluationDataRequest repositoryComponentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest("maven2", pathname, hash);
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, "h", MatchState.EXACT,
        0 /* index */, declaredLicenseSet, observedLicenseSet, securityVulnerabilities, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);

    // Call the service
    Date before = new Date();
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList =
        repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, true);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults, hasSize(1));
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).requestIndex, is(0));
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).quarantine, is(true));
    Date after = new Date();

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled(), is(true));
    assertThat(repository.isQuarantineEnabled(), is(true));

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents, hasSize(1));
    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO.getByRepositoryId(repository
        .getId());
    assertThat(policyViolations, hasSize(1));

    RepositoryComponent repositoryComponent = repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(),
        pathname);
    assertRepositoryComponent(repository.getId(), pathname, before, after, hash, componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), before, after, true, after,
        repositoryComponent);

    RepositoryPolicyViolation policyViolation = repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathname(
        repository.getId(), pathname).get(0);
    assertPolicyViolation(repository.getId(), pathname, policy.getId(), policy.getName(), policy.getThreatLevel(),
        policy.getThreatCategory(), hash, componentIdentifier, before, after, policyViolation);
  }

  @Test
  public void testEvaluateComponentWithQuarantine_pathnameSlashPrefix() throws Exception {
    String pathname = "path";
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    Policy policy = createQuarantiningPolicy(repository);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Prepare request and mock the HDS request
    Set<License> declaredLicenseSet = Collections.singleton(new License("Apache-2.0", "Apache-2.0"));
    Set<License> observedLicenseSet = Collections.singleton(new License("ATT", "ATT"));
    List<SecurityVulnerability> securityVulnerabilities = createSecurityVulnerabilities();
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    String hash = "h";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    RepositoryComponentEvaluationDataRequest repositoryComponentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest("maven2", "/" + pathname, hash);
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, "h", MatchState.EXACT,
        0 /* index */, declaredLicenseSet, observedLicenseSet, securityVulnerabilities, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);

    // Call the service
    Date before = new Date();
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList =
        repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, true);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults, hasSize(1));
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).requestIndex, is(0));
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).quarantine, is(true));
    Date after = new Date();

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled(), is(true));
    assertThat(repository.isQuarantineEnabled(), is(true));

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents, hasSize(1));
    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO.getByRepositoryId(repository
        .getId());
    assertThat(policyViolations, hasSize(1));

    RepositoryComponent repositoryComponent = repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(),
        pathname);
    assertRepositoryComponent(repository.getId(), pathname, before, after, hash, componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), before, after, true, after,
        repositoryComponent);

    RepositoryPolicyViolation policyViolation = repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathname(
        repository.getId(), pathname).get(0);
    assertPolicyViolation(repository.getId(), pathname, policy.getId(), policy.getName(), policy.getThreatLevel(),
        policy.getThreatCategory(), hash, componentIdentifier, before, after, policyViolation);
  }

  @Test
  public void testEvaluateComponentWithQuarantine_NoViolations() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Prepare request and mock the HDS request
    Set<License> declaredLicenseSet = Collections.singleton(new License("Apache-2.0", "Apache-2.0"));
    Set<License> observedLicenseSet = Collections.singleton(new License("ATT", "ATT"));
    List<SecurityVulnerability> securityVulnerabilities = new ArrayList<>();
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    String pathname = "path";
    String hash = "h";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    RepositoryComponentEvaluationDataRequest repositoryComponentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest("maven2", pathname, hash);
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, "h", MatchState.EXACT,
        0 /* index */, declaredLicenseSet, observedLicenseSet, securityVulnerabilities, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);

    // Call the service
    Date before = new Date();
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList =
        repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, true);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults, hasSize(1));
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).requestIndex, is(0));
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).quarantine, is(false));
    Date after = new Date();

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled(), is(true));
    assertThat(repository.isQuarantineEnabled(), is(true));

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents, hasSize(1));

    RepositoryComponent repositoryComponent = repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(),
        pathname);
    assertRepositoryComponent(repository.getId(), pathname, before, after, hash, componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), true, repositoryComponent);
  }

  @Test
  public void testEvaluateComponentWithQuarantine_Waived() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    String hash = "h";

    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId(), "Test Policy");
    tempEntity.newWaiver(hash, policy.getId(), repository.getId());

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Prepare request and mock the HDS request
    Set<License> declaredLicenseSet = Collections.singleton(new License("Apache-2.0", "Apache-2.0"));
    Set<License> observedLicenseSet = Collections.singleton(new License("ATT", "ATT"));
    List<SecurityVulnerability> securityVulnerabilities = createSecurityVulnerabilities();
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    String pathname = "path";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    RepositoryComponentEvaluationDataRequest repositoryComponentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest("maven2", pathname, hash);
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, "h", MatchState.EXACT,
        0 /* index */, declaredLicenseSet, observedLicenseSet, securityVulnerabilities, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);

    // Call the service
    Date before = new Date();
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList =
        repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, true);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults, hasSize(1));
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).requestIndex, is(0));
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).quarantine, is(false));

    Date after = new Date();

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled(), is(true));
    assertThat(repository.isQuarantineEnabled(), is(true));

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents, hasSize(1));
    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO.getByRepositoryId(repository
        .getId());
    assertThat(policyViolations, hasSize(1));

    RepositoryComponent repositoryComponent = repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(),
        pathname);
    assertRepositoryComponent(repository.getId(), pathname, before, after, hash, componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), true, repositoryComponent);
  }

  @Test
  public void testEvaluateComponentWithQuarantine_QuarantineRequestAfterAuditWithoutExplicitRemoval() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    createQuarantiningPolicy(repository);

    String hash = "hash";
    String pathname = "pathname";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNugetCoordinates("p", "1");

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList = new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("nuget", pathname,
        hash));

    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT, 0,
        Collections.singleton(new License("EPL-1.0", "EPL-2.0")),
        Collections.singleton(new License("EPL-1.0", "EPL-2.0")), createSecurityVulnerabilities(), 80));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // initial evaluation of component, audit-only
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList = repositoryService
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, false);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults, hasSize(1));
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).quarantine, is(false));

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled(), is(true));
    assertThat(repository.isQuarantineEnabled(), is(false));

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents, hasSize(1));
    assertThat(repositoryComponents.get(0).getPathname(), is(pathname));
    assertThat(repositoryComponents.get(0).isCanBeQuarantined(), is(false));
    assertThat(repositoryComponents.get(0).getQuarantineTime(), is(nullValue()));

    // re-evaluation of component, this time with quarantine enabled
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);
    Date before = new Date();
    repositoryComponentEvaluationResultList = repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID,
        REPO_PUBLIC_ID, componentEvaluationDataRequestList, true);
    Date after = new Date();
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults, hasSize(1));
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).quarantine, is(true));

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled(), is(true));
    assertThat(repository.isQuarantineEnabled(), is(true));

    repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents, hasSize(1));
    assertThat(repositoryComponents.get(0).getPathname(), is(pathname));
    assertThat(repositoryComponents.get(0).isCanBeQuarantined(), is(true));
    assertThat(repositoryComponents.get(0).getQuarantineTime(), is(greaterThanOrEqualTo(before)));
    assertThat(repositoryComponents.get(0).getQuarantineTime(), is(lessThanOrEqualTo(after)));
  }

  @Test
  public void testEvaluateComponentWithQuarantine_QuarantineRequestAfterUnquarantineWithoutExplicitRemoval()
      throws Exception
  {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    createQuarantiningPolicy(repository);

    String hash = "hash";
    String pathname = "pathname";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNugetCoordinates("p", "1");

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList = new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("nuget", pathname,
        hash));

    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT, 0,
        Collections.singleton(new License("EPL-1.0", "EPL-2.0")),
        Collections.singleton(new License("EPL-1.0", "EPL-2.0")), createSecurityVulnerabilities(), 80));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true /* quarantine */);

    // Initial evaluation of component, quarantine enabled
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList = repositoryService
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, true /* withQuarantine */);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults, hasSize(1));
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).quarantine, is(true));

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents, hasSize(1));
    RepositoryComponent repositoryComponent = repositoryComponents.get(0);
    assertThat(repositoryComponent.isQuarantined(), is(true));

    // Unquarantine the component
    repositoryComponent.setUnquarantineTime(new Date());
    repositoryComponentDAO.update(repositoryComponent);
    repositoryComponent = repositoryComponentDAO.getById(repositoryComponent.getId());
    assertThat(repositoryComponent.isQuarantined(), is(false));

    // Re-evaluation of component, quarantine enabled
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);
    Date before = new Date();
    repositoryComponentEvaluationResultList = repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID,
        REPO_PUBLIC_ID, componentEvaluationDataRequestList, true);
    Date after = new Date();
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults, hasSize(1));
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).quarantine, is(true));

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled(), is(true));
    assertThat(repository.isQuarantineEnabled(), is(true));

    repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents, hasSize(1));
    repositoryComponent = repositoryComponents.get(0);
    assertThat(repositoryComponent.getPathname(), is(pathname));
    assertThat(repositoryComponent.isCanBeQuarantined(), is(true));
    assertThat(repositoryComponent.getQuarantineTime(), is(greaterThanOrEqualTo(before)));
    assertThat(repositoryComponent.getQuarantineTime(), is(lessThanOrEqualTo(after)));
    assertThat(repositoryComponent.isQuarantined(), is(true));
  }

  @Test
  public void testEvaluateComponents_RepositoryDoesNotExist() throws Exception {
    try {
      repositoryService
          .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null /* componentEvaluationDataRequestList */,
              false);
      fail("Expected exception");
    }
    catch (NotFoundException expected) {
      assertThat(expected.getMessage(), is(RepositoryDAO.getErrMsgMissingRepo(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID)));
    }
  }

  @Test
  public void testEvaluateComponents_ExistingRepository_NotEnabled() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false, false);

    repositoryService
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null /* componentEvaluationDataRequestList */, false);

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled(), is(true));
    assertThat(repository.isQuarantineEnabled(), is(false));
  }

  @Test
  public void testEvaluateComponents_ExistingRepository_QuarantineNotEnabled() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true, false);

    repositoryService
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null /* componentEvaluationDataRequestList */, true);

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled(), is(true));
    assertThat(repository.isQuarantineEnabled(), is(true));
  }

  @Test
  public void testEvaluateComponents_ExistingRepository_RepositoryAnadQuarantineNotEnabled() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false, false);

    repositoryService
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null /* componentEvaluationDataRequestList */, true);

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled(), is(true));
    assertThat(repository.isQuarantineEnabled(), is(true));
  }

  @Test
  public void testEvaluateComponents_MultipleComponents() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId(), "Test Policy");

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList = new RepositoryComponentEvaluationDataRequestList();

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
      componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("maven2", "path"
          + i, "h" + i));
      hdsResult.components.add(createComponentEvaluationData(componentIdentifier, "h" + i, MatchState.EXACT,
          i /* index */, declaredLicenseSet, observedLicenseSet, securityVulnerabilities, i /* popularity */));
    }
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Call the service
    Date before = new Date();
    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
        false);
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

      RepositoryPolicyViolation policyViolation = repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathname(
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
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("maven2", "path",
        hash));
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT, 0 /* index */,
        declaredLicenseSet, observedLicenseSet, null /* securityVulnerabilities */, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Call the service first time
    Date before1 = new Date();
    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
        false);
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
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("maven2", "path",
        updatedHash));
    hdsResult.components = new ArrayList<>();
    hdsResult.components.add(createComponentEvaluationData(updatedComponentIdentifier, updatedHash, MatchState.EXACT,
        0 /* index */, declaredLicenseSet, observedLicenseSet, null /* securityVulnerabilities */, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);
    Date before2 = new Date();
    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
        false);
    Date after2 = new Date();

    repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents, hasSize(1));
    repositoryComponent = repositoryComponents.get(0);
    assertRepositoryComponent(repository.getId(), "path", before1, after1, updatedHash, updatedComponentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), before2, after2, false /* canBeQuarantined */,
        null, repositoryComponent);

    policyViolations = repositoryPolicyViolationDAO.getByRepositoryId(repository.getId());
    assertThat(policyViolations, hasSize(2));
    for (RepositoryPolicyViolation policyViolation : policyViolations) {
      if (policyViolation.isActive()) {
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

    // Prepare request and mock the HDS request
    Set<License> declaredLicenseSet = Collections.singleton(new License("Apache-2.0", "Apache-2.0"));
    Set<License> observedLicenseSet = Collections.singleton(new License("ATT", "ATT"));
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven2", "path", "h"));
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, "h", MatchState.EXACT, 0 /* index */,
        declaredLicenseSet, observedLicenseSet, null /* securityVulnerabilities */, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Call the service
    Date before = new Date();
    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
        false);
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

    // Prepare request and mock the HDS request
    Set<License> declaredLicenseSet = Collections.singleton(new License("Apache-2.0", "Apache-2.0"));
    Set<License> observedLicenseSet = Collections.singleton(new License("ATT", "ATT"));
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("maven2", "path",
        "h"));
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, "h", MatchState.EXACT, 0 /* index */,
        declaredLicenseSet, observedLicenseSet, null /* securityVulnerabilities */, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Call the service
    Date before = new Date();
    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
        false);
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
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("maven2", "path",
        longHash));
    Set<License> declaredLicenseSet = Collections.singleton(new License("Apache-2.0", "Apache-2.0"));
    Set<License> observedLicenseSet = Collections.singleton(new License("ATT", "ATT"));
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT, 0 /* index */,
        declaredLicenseSet, observedLicenseSet, null /* securityVulnerabilities */, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Call the service
    Date before = new Date();
    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
        false);
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
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("maven", "path",
        hash));
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.UNKNOWN,
        0 /* index */, Collections.<License>emptySet(), Collections.<License>emptySet(),
        null /* securityVulnerabilities */, null /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Call the service
    Date before = new Date();
    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
        false);
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
  public void testEvaluateComponents_pathnameSlashPrefix() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    String hash = "hash";
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNugetCoordinates("p", "v1");

    // Prepare request and mock the HDS request
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList = new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("maven2", "/path",
        hash));
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    hdsResult.components
        .add(createComponentEvaluationData(componentIdentifier1, hash, MatchState.EXACT, 0 /* index */,
            Collections.<License> emptySet(), Collections.<License> emptySet(), null /* securityVulnerabilities */,
            null /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Call the service
    Date before = new Date();
    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
        false);
    Date after = new Date();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents, hasSize(1));
    RepositoryComponent repositoryComponent = repositoryComponents.get(0);
    assertRepositoryComponent(repository.getId(), "path", before, after, hash, componentIdentifier1,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), false /* canBeQuarantined */,
        repositoryComponent);
  }

  @Test
  public void testEvaluateComponents_NullPathname() throws Exception {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList = new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("maven2", null,
        "hash"));

    try {
      repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
          false);
      fail("Expected exception");
    }
    catch (BadRequestException expected) {
      assertThat(expected.getMessage(), is("The pathname cannot be null or empty."));
    }
  }

  @Test
  public void testEvaluateComponents_EmptyPathname() throws Exception {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList = new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("maven2", " ",
        "hash"));

    try {
      repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
          false);
      fail("Expected exception");
    }
    catch (BadRequestException expected) {
      assertThat(expected.getMessage(), is("The pathname cannot be null or empty."));
    }
  }

  @Test
  public void testEvaluateComponents_NullFormat() throws Exception {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList = new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest(null, "pathname",
        "hash"));

    try {
      repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
          false);
      fail("Expected exception");
    }
    catch (BadRequestException expected) {
      assertThat(expected.getMessage(), is("The format cannot be null or empty."));
    }
  }

  @Test
  public void testEvaluateComponents_EmptyFormat() throws Exception {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList = new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest(" ", "pathname",
        "hash"));

    try {
      repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
          false);
      fail("Expected exception");
    }
    catch (BadRequestException expected) {
      assertThat(expected.getMessage(), is("The format cannot be null or empty."));
    }
  }

  @Test
  public void testEvaluateComponents_NullHash() throws Exception {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    String hash = null;

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList = new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("maven2", "path",
        hash));

    try {
      repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
          false);
      fail("Expected exception");
    }
    catch (BadRequestException expected) {
      assertThat(expected.getMessage(), is("The hash cannot be null or empty."));
    }
  }

  @Test
  public void testEvaluateComponents_EmptyHash() throws Exception {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList = new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("maven2", "path",
        " "));

    try {
      repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
          false);
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

    tempEntity.newRepositoryComponent(repo.getId(), "/quarantined", new Date(), null);

    RepositoryReportSummary summary = repositoryService.getReportSummary(repo.getId());

    assertThat(summary.knownComponentCount, is(5));
    assertThat(summary.totalComponentCount, is(6));
    assertThat(summary.criticalComponentCount, is(1));
    assertThat(summary.severeComponentCount, is(2));
    assertThat(summary.moderateComponentCount, is(0));
    assertThat(summary.affectedComponentCount, is(3));
    assertThat(summary.quarantinedComponentCount, is(1));
  }

  @Test
  public void testEvaluateComponents_MissingLicenseFeature() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    clmLicenseManager.installLicense(null);
    try {
      repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null, false);
      fail("Expected exception");
    }
    catch (InvalidLicenseException expected) {
      assertThat(expected.getMessage(), is(InvalidLicenseException.INVALID_LICENSE_MSG));
    }
  }

  private void mockHdsRequest(RepositoryComponentEvaluationDataRequestList serviceRequest,
      ComponentEvaluationDataList hdsResult, boolean quarantine) throws IOException
  {
    RepositoryComponentEvaluationDataRequestList hdsRequest = new RepositoryComponentEvaluationDataRequestList();
    hdsRequest.components = new ArrayList<>();
    for (RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest : serviceRequest.components) {
      String hash = HashHelper.truncateHash(componentEvaluationDataRequest.hash);
      hdsRequest.components.add(new RepositoryComponentEvaluationDataRequest(componentEvaluationDataRequest.format,
          componentEvaluationDataRequest.pathname, hash));
    }
    when(
        (quarantine ? quarantineHdsClient : auditHdsClient).post(eq(ComponentEvaluationDataList.class),
            eq(RepositoryService.HDS_COMPONENT_DETAILS_PATH), eq(hdsRequest))).thenReturn(hdsResult);
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
      Date beforeLastEvaluation, Date afterLastEvaluation, boolean canBeQuarantined, Date afterQuarantineTime,
      RepositoryComponent actual)
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
    if (afterQuarantineTime != null) {
      assertThat(actual.getQuarantineTime(), lessThanOrEqualTo(afterQuarantineTime));
    } else {
      assertThat(actual.getQuarantineTime(), nullValue());
    }
  }

  private void assertRepositoryComponent(String repositoryId, String pathname, Date beforeCreate, Date afterCreate,
      String hash, ComponentIdentifier componentIdentifier, String matchStateId, String identificationSourceId,
      boolean canBeQuarantined, RepositoryComponent actual)
  {
    assertRepositoryComponent(repositoryId, pathname, beforeCreate, afterCreate, hash, componentIdentifier,
        matchStateId, identificationSourceId, beforeCreate, afterCreate, canBeQuarantined, null, actual);
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

  @Test
  public void testRemoveComponent_RepositoryDoesNotExist() throws Exception {
    try {
      repositoryService.removeComponent(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "somepath");
      fail("Expected exception");
    }
    catch (NotFoundException expected) {
      assertThat(expected.getMessage(), is(RepositoryDAO.getErrMsgMissingRepo(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID)));
    }
  }

  @Test
  public void testRemoveComponent_RepositoryNotEnabled() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false /* enabled */);

    repositoryService.removeComponent(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "somepath");

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled(), is(true));
  }

  @Test
  public void testRemoveComponent() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID);
    String pathname1 = "pathname1";
    String pathname2 = "pathname2";
    RepositoryComponent repositoryComponent1 = tempEntity.newRepositoryComponent(repository.getId(), pathname1);
    RepositoryComponent repositoryComponent2 = tempEntity.newRepositoryComponent(repository.getId(), pathname2);
    RepositoryPolicyViolation policyViolation1 = tempEntity.newRepositoryPolicyViolation(repository.getId(), pathname1);
    RepositoryPolicyViolation policyViolation2 = tempEntity.newRepositoryPolicyViolation(repository.getId(), pathname2);

    repositoryService.removeComponent(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, pathname1);

    assertThat(repositoryComponentDAO.getById(repositoryComponent1.getId()), is(nullValue()));
    assertThat(repositoryComponentDAO.getById(repositoryComponent2.getId()), is(notNullValue()));
    assertThat(repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(), pathname1), is(nullValue()));
    policyViolation1 = repositoryPolicyViolationDAO.getById(policyViolation1.getId());
    assertThat(policyViolation1.isActive(), is(false));
    policyViolation2 = repositoryPolicyViolationDAO.getById(policyViolation2.getId());
    assertThat(policyViolation2.isActive(), is(true));
  }

  @Test
  public void testRemoveComponent_pathnameSlashPrefix() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID);
    String pathname1 = "pathname1";
    String pathname2 = "pathname2";
    RepositoryComponent repositoryComponent1 = tempEntity.newRepositoryComponent(repository.getId(), pathname1);
    RepositoryComponent repositoryComponent2 = tempEntity.newRepositoryComponent(repository.getId(), pathname2);
    RepositoryPolicyViolation policyViolation1 = tempEntity.newRepositoryPolicyViolation(repository.getId(), pathname1);
    RepositoryPolicyViolation policyViolation2 = tempEntity.newRepositoryPolicyViolation(repository.getId(), pathname2);

    repositoryService.removeComponent(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "/" + pathname1);

    assertThat(repositoryComponentDAO.getById(repositoryComponent1.getId()), is(nullValue()));
    assertThat(repositoryComponentDAO.getById(repositoryComponent2.getId()), is(notNullValue()));
    assertThat(repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(), pathname1), is(nullValue()));
    policyViolation1 = repositoryPolicyViolationDAO.getById(policyViolation1.getId());
    assertThat(policyViolation1.isActive(), is(false));
    policyViolation2 = repositoryPolicyViolationDAO.getById(policyViolation2.getId());
    assertThat(policyViolation2.isActive(), is(true));
  }

  @Test
  public void testTHREAT_LEVEL_DESC_PATHNAME_ASC() throws Exception {
    final RepositoryReportDetail detail1 = RepositoryReportDetail.create(
        new RepositoryComponent(null, "z", null, null, null, null, null, null, false));
    final RepositoryReportDetail detail2 = RepositoryReportDetail.create(
        new RepositoryComponent(null, "a", null, null, null, null, null, null, false),
        new RepositoryPolicyViolation(null, null, null, null, null, 9, null, null, null, "[]" /* constraintFacts */),
        false);
    assertTrue("Should sort ThreatLevel Descending",
        0 < RepositoryService.THREAT_LEVEL_DESC_PATHNAME_ASC.compare(detail1, detail2));

    final RepositoryReportDetail detail3 = RepositoryReportDetail.create(
        new RepositoryComponent(null, "a", null, null, null, null, null, null, false),
        new RepositoryPolicyViolation(null, null, null, null, null, 0, null, null, null, "[]" /* constraintFacts */),
        false);
    assertTrue("Should sort Pathname Ascending",
        0 < RepositoryService.THREAT_LEVEL_DESC_PATHNAME_ASC.compare(detail1, detail3));

    final RepositoryReportDetail detail4 = RepositoryReportDetail.create(
        new RepositoryComponent(null, "z", null, null, null, null, null, null, false),
        new RepositoryPolicyViolation(null, null, null, null, null, 0, null, null, null, "[]" /* constraintFacts */),
        false);
    assertEquals("Equal ThreatLevel and pathname",
        0, RepositoryService.THREAT_LEVEL_DESC_PATHNAME_ASC.compare(detail1, detail4));
  }

  @Test
  public void testGetReportDetails() throws Exception {
    final RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    final Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID);

    // component with 1 violation
    final String pathname1 = "pathname1";
    createRepositoryPolicyViolation(repository, pathname1, 5);

    // component with no violation
    final String pathname2 = "pathname2";
    createRepositoryPolicyViolation(repository, pathname2);

    // component with 2 violations
    final String pathname3 = "pathname3";
    createRepositoryPolicyViolation(repository, pathname3, 5, 9);

    // add violations for a different repository, which should not be included in current repo details
    final Repository repositoryOther = tempEntity.newRepository(repositoryManager, "otherRepoPublicId");
    createRepositoryPolicyViolation(repositoryOther, pathname1, 6);

    final List<RepositoryReportDetail> reportDetails = repositoryService.getReportDetails(repository.getId());

    assertThat(reportDetails.size(), is(4));

    int idx = 0;
    // list should be sorted by 'threadLevel DESC', 'pathname ASC'
    assertRepositoryReportDetail(reportDetails.get(idx++), pathname3, "policyName", 9, true);
    assertRepositoryReportDetail(reportDetails.get(idx++), pathname1, "policyName", 5, true);
    assertRepositoryReportDetail(reportDetails.get(idx++), pathname3, "policyName", 5, false);
    assertRepositoryReportDetail(reportDetails.get(idx), pathname2, null, 0, true);
  }

  @Test
  public void testGetRepositoryById() {
    Repository repository = tempEntity.newRepository();
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(), new Date());

    RepositoryDTO actual = repositoryService.getRepositoryById(repository.getId());
    assertNotNull(actual.repository);
    assertThat(actual.repository.getPublicId(), is(repository.getPublicId()));
    assertThat(actual.oldestEvalTimestamp, is(repositoryComponent.getLastEvaluationTime().getTime()));
  }

  @Test
  public void testGetRepositoryById_noEvaluation() {
    Repository repository = tempEntity.newRepository();

    RepositoryDTO actual = repositoryService.getRepositoryById(repository.getId());
    assertNotNull(actual.repository);
    assertThat(actual.repository.getPublicId(), is(repository.getPublicId()));
    assertNull(actual.oldestEvalTimestamp);
  }

  @Test
  public void testGetRepositoryById_unknownId() throws Exception {
    try {
      repositoryService.getRepositoryById("foobar");
      fail("Did not throw exception");
    }
    catch (NotFoundException e) {
      assertThat(e.getMessage(), is("Cannot find a repository with ID foobar."));
    }
  }

  private void createRepositoryPolicyViolation(final Repository repository, final String pathname,
      int... threatLevels)
  {
    tempEntity.newRepositoryComponent(repository.getId(), pathname);
    for (final int threatLevel : threatLevels) {
      tempEntity.newRepositoryPolicyViolation(repository.getId(), threatLevel, pathname, null);
    }
  }

  private void assertRepositoryReportDetail(final RepositoryReportDetail actualReportDetail,
      final String expectedPathname, final String expectedPolicyName, final int expectedThreatLevel,
      final boolean expectedHighestThreatLevel)
  {
    assertEquals(expectedPathname, actualReportDetail.getPathname());
    assertEquals(expectedPolicyName, actualReportDetail.getPolicyName());
    assertEquals(expectedThreatLevel, actualReportDetail.getThreatLevel());
    assertEquals(expectedHighestThreatLevel, actualReportDetail.isHighestThreatLevel());

    assertEquals("hash", actualReportDetail.getHash());
    assertEquals("exact", actualReportDetail.getMatchState());
    assertEquals("g : a : v", actualReportDetail.getComponentDisplayText());
    assertEquals("maven", actualReportDetail.getComponentIdentifier().getFormat());
    assertFalse(actualReportDetail.isQuarantined());
    assertFalse(actualReportDetail.isWaived());
  }

  private Policy createQuarantiningPolicy(Repository repository) {
    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId(), "Test Policy");
    policy.setActions(ProxyStageType.ID, Collections.singletonList(new Action(Action.ID_FAIL)));
    new PolicyDAO().update(policy);
    return policy;
  }
}
