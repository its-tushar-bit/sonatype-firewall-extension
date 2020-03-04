/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dto.repository.RepositoryDTO;
import com.sonatype.insight.brain.hds.FirewallAuditHdsClient;
import com.sonatype.insight.brain.hds.FirewallQuarantineHdsClient;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.integration.repository.FirewallIgnorePatternService;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.policy.violation.AbstractPolicyViolationLogger;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTO;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTOAssert;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.repository.RepositoryReportResource.RepositoryReportSummary;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Binder;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

public class RepositoryServiceTest extends AbstractComponentTest
{
  private static final String MANUAL_REPO_MAN_INSTANCE_ID = "manualDeleteRepoManagerInstanceId";

  private static final String REPO_MAN_INSTANCE_ID = "repoManagerInstanceId";

  private static final String REPO_PUBLIC_ID = "repoPublicId";

  @Rule
  public LogOutput policyViolationLoggerOutput = new LogOutput(
      AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);

  @Inject
  private RepositoryService repositoryService;

  private RepositoryManagerDAO repositoryManagerDAO = new RepositoryManagerDAO();

  private static final RepositoryDAO repositoryDAO = new RepositoryDAO();

  private RepositoryComponentDAO repositoryComponentDAO = new RepositoryComponentDAO();

  @Mock
  private FirewallAuditHdsClient auditHdsClient;

  @Mock
  private HdsClient hdsClient;

  @Mock
  private FirewallQuarantineHdsClient quarantineHdsClient;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(hdsClient);
    binder.bind(FirewallAuditHdsClient.class).toInstance(auditHdsClient);
    binder.bind(FirewallQuarantineHdsClient.class).toInstance(quarantineHdsClient);
    super.configure(binder);
  }

  @Before
  public void before() {
    FirewallIgnorePatterns firewallIgnorePatterns = new FirewallIgnorePatterns();
    firewallIgnorePatterns.regexpsByRepositoryFormat = new HashMap<>();
    lenient().when(hdsClient.get(eq(FirewallIgnorePatterns.class),
        eq(FirewallIgnorePatternService.HDS_IGNORE_PATTERNS_PATH))).thenReturn(firewallIgnorePatterns);
  }

  @After
  public void cleanup() {
    RepositoryManager repositoryManager = repositoryManagerDAO.getByInstanceId(MANUAL_REPO_MAN_INSTANCE_ID);
    if (repositoryManager != null) {
      repositoryManagerDAO.delete(repositoryManager);
    }
  }

  @Test
  public void testUnquarantineComponent_WasQuarantined() throws Exception {
    String pathname = "path";
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "maven2");
    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), pathname, new Date(), null);

    mockHdsRequestForComponent(repositoryComponent, true);

    repositoryService.unquarantineComponent(repository.getId(), pathname, null);
    repositoryComponent = repositoryComponentDAO.getById(repositoryComponent.getId());

    assertThat(repositoryComponent.isQuarantined()).isFalse();
  }

  @Test
  public void testUnquarantineComponent_WasNotQuarantined() throws Exception {
    String pathname = "path";
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "maven2");
    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), pathname, null, null);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      repositoryService.unquarantineComponent(repository.getId(), pathname, null);
    }).withMessage("Component " + pathname + " in repository " + repository.getId() + " is not quarantined.");
    repositoryComponent = repositoryComponentDAO.getById(repositoryComponent.getId());

    assertThat(repositoryComponent.isQuarantined()).isFalse();
  }

  @Test
  public void testUnquarantineComponent_WithViolations() throws Exception {
    String pathname = "path";
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "maven2");
    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), pathname, new Date(), null);

    createQuarantiningPolicy(repository);
    mockHdsRequestForComponent(repositoryComponent, true);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      repositoryService.unquarantineComponent(repository.getId(), pathname, null);
    }).withMessage("Component " + pathname + " in repository " + repository.getId() + " has policy violations.");
  }

  @Test
  public void testUnquarantineComponent_WithViolationsNotFailed() throws Exception {
    String pathname = "path";
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "maven2");
    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), pathname, new Date(), null);

    createQuarantiningPolicy(repository);
    mockHdsRequestForComponent(repositoryComponent, false);

    repositoryService.unquarantineComponent(repository.getId(), pathname, null);
    repositoryComponent = repositoryComponentDAO.getById(repositoryComponent.getId());

    assertThat(repositoryComponent.isQuarantined()).isFalse();
  }

  private void mockHdsRequestForComponent(RepositoryComponent repositoryComponent, boolean withSecurityVulnerabilities)
      throws Exception
  {
    // Prepare request and mock the HDS request
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    RepositoryComponentEvaluationDataRequest repositoryComponentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest(
            "maven2", repositoryComponent.getPathname(), repositoryComponent.getHash());
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList(
            RepositoryComponentEvaluationDataRequestList.REEVALUATION);
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);

    List<SecurityVulnerability> securityVulnerabilities = new ArrayList<>();
    if (withSecurityVulnerabilities) {
      securityVulnerabilities = createSecurityVulnerabilities();
    }

    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    hdsResult.components.add(
        createComponentEvaluationData(componentIdentifier, repositoryComponent.getHash(), MatchState.EXACT,
            0 /* index */, Collections.emptySet(), Collections.emptySet(), securityVulnerabilities,
            0 /* popularity */));

    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);
  }

  @Test
  public void testGetPolicyThreats() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    String pathname = "path1";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    RepositoryPolicyViolation repositoryPolicyViolation1 = tempEntity
        .newRepositoryPolicyViolation(repository.getId(), 8, pathname, false, true, "policyId1", "policyName1",
            componentIdentifier);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 7, pathname, true, true, "policyId2", "policyName2",
        componentIdentifier);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 8, pathname, false, false, "policyId3", "policyName3",
        componentIdentifier);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, "path4", false, true, Action.ID_FAIL, "policyId4",
        "policyName4", componentIdentifier);

    repositoryPolicyViolation1.setConstraintFacts(Collections.singletonList(new ConstraintFact("id", "name", "op")));
    new RepositoryPolicyViolationDAO().update(repositoryPolicyViolation1);

    tempEntity.newRepositoryComponent(repository.getId(), pathname, new Date(), null);
    tempEntity.newRepositoryComponent(repository.getId(), "path4", new Date(), null);

    RepositoryPolicyThreatDTO repositoryPolicyThreatDTO = repositoryService
        .getPolicyThreats(repository.getId(), pathname);

    assertThat(repositoryPolicyThreatDTO.activePolicyViolations).hasSize(1);
    RepositoryPolicyViolationDTO repositoryViolationDTO = repositoryPolicyThreatDTO.activePolicyViolations.get(0);
    assertThat(repositoryViolationDTO.policyId).isEqualTo("policyId1");
    assertThat(repositoryViolationDTO.policyName).isEqualTo("policyName1");
    assertThat(repositoryViolationDTO.policyThreatLevel).isEqualTo(8);
    assertThat(repositoryViolationDTO.constraintFactsJson)
        .isEqualTo(repositoryPolicyViolation1.getConstraintFactsJson());
    assertThat(repositoryViolationDTO.blocksUnquarantine).isFalse();

    repositoryViolationDTO = repositoryService.getPolicyThreats(repository.getId(), "path4").activePolicyViolations
        .get(0);
    assertThat(repositoryViolationDTO.blocksUnquarantine).isTrue();
  }

  @Test
  public void testGetPolicyThreats_RepositoryComponentDoesNotExist() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      repositoryService.getPolicyThreats(repository.getId(), "pathDoesNotExist");
    }).withMessage(
        "Cannot find a component with path pathDoesNotExist in repository with ID " + repository.getId() + ".");
  }

  @Test
  public void testGetPolicyThreats_RepositoryDoesNotExist() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      repositoryService.getPolicyThreats("RepositoryIdDoesNotExist", null);
    }).withMessage("Cannot find a repository with ID RepositoryIdDoesNotExist.");
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

    assertThat(summary.knownComponentCount).isEqualTo(5);
    assertThat(summary.totalComponentCount).isEqualTo(6);
    assertThat(summary.criticalComponentCount).isEqualTo(1);
    assertThat(summary.severeComponentCount).isEqualTo(2);
    assertThat(summary.moderateComponentCount).isEqualTo(0);
    assertThat(summary.affectedComponentCount).isEqualTo(3);
    assertThat(summary.quarantinedComponentCount).isEqualTo(1);
  }

  private void mockHdsRequest(
      RepositoryComponentEvaluationDataRequestList serviceRequest,
      ComponentEvaluationDataList hdsResult,
      boolean quarantine)
  {
    RepositoryComponentEvaluationDataRequestList hdsRequest = new RepositoryComponentEvaluationDataRequestList();
    hdsRequest.cause = serviceRequest.cause;
    hdsRequest.components = new ArrayList<>();
    for (RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest : serviceRequest.components) {
      String hash = HashHelper.truncateHash(componentEvaluationDataRequest.hash);
      String pathname = componentEvaluationDataRequest.pathname
          .substring(componentEvaluationDataRequest.pathname.startsWith("/") ? 1 : 0);
      hdsRequest.components
          .add(new RepositoryComponentEvaluationDataRequest(componentEvaluationDataRequest.format, pathname, hash));
    }
    when((quarantine ? quarantineHdsClient : auditHdsClient)
        .post(any(), eq(ComponentEvaluationDataList.class), eq(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH),
            isNull(), eq(hdsRequest))).thenReturn(hdsResult);
  }

  private ComponentEvaluationData createComponentEvaluationData(ComponentIdentifier componentIdentifier,
                                                                String hash,
                                                                MatchState matchState,
                                                                int index,
                                                                Set<License> declaredLicenses,
                                                                Set<License> observedLicenses,
                                                                List<SecurityVulnerability> securityVulnerabilities,
                                                                Integer relativePopularity)
  {
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    componentEvaluationData.requestIndex = index;
    componentEvaluationData.hash = hash;
    componentEvaluationData.componentIdentifier = componentIdentifier;
    componentEvaluationData.matchState = matchState.getId();
    componentEvaluationData.declaredLicenses = declaredLicenses == null ? Collections.emptySet() : declaredLicenses;
    componentEvaluationData.observedLicenses = observedLicenses == null ? Collections.emptySet() : observedLicenses;
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

  @Test
  public void testTHREAT_LEVEL_DESC_PATHNAME_ASC() throws Exception {
    final RepositoryReportDetail detail1 = RepositoryReportDetail
        .create(new RepositoryComponent(null, "z", null, null, null, null, null, null));
    final RepositoryReportDetail detail2 = RepositoryReportDetail
        .create(new RepositoryComponent(null, "a", null, null, null, null, null, null),
            new RepositoryPolicyViolation(null, null, null, null, null, 9, null, null, null,
                "[]" /* constraintFacts */), false);
    assertThat(RepositoryService.THREAT_LEVEL_DESC_PATHNAME_ASC.compare(detail1, detail2))
        .as("Should sort ThreatLevel Descending").isPositive();

    final RepositoryReportDetail detail3 = RepositoryReportDetail
        .create(new RepositoryComponent(null, "a", null, null, null, null, null, null),
            new RepositoryPolicyViolation(null, null, null, null, null, 0, null, null, null,
                "[]" /* constraintFacts */), false);
    assertThat(RepositoryService.THREAT_LEVEL_DESC_PATHNAME_ASC.compare(detail1, detail3))
        .as("Should sort Pathname Ascending").isPositive();

    final RepositoryReportDetail detail4 = RepositoryReportDetail
        .create(new RepositoryComponent(null, "z", null, null, null, null, null, null),
            new RepositoryPolicyViolation(null, null, null, null, null, 0, null, null, null,
                "[]" /* constraintFacts */), false);
    assertThat(RepositoryService.THREAT_LEVEL_DESC_PATHNAME_ASC.compare(detail1, detail4))
        .as("Equal ThreatLevel and pathname").isZero();
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

    // component with 1 violation that is waived
    final String pathname4 = "pathname4";
    createRepositoryPolicyViolation(repository, pathname4, true, 1);

    // add violations for a different repository, which should not be included in current repo details
    final Repository repositoryOther = tempEntity.newRepository(repositoryManager, "otherRepoPublicId");
    createRepositoryPolicyViolation(repositoryOther, pathname1, 6);

    final List<RepositoryReportDetail> reportDetails = repositoryService
        .getReportDetails(repository.getId(), null, null);

    assertThat(reportDetails).hasSize(6);

    int idx = 0;
    // list should be sorted by 'threadLevel DESC', 'pathname ASC'
    assertRepositoryReportDetail(reportDetails.get(idx++), pathname3, "policyName", 9, true, false);
    assertRepositoryReportDetail(reportDetails.get(idx++), pathname1, "policyName", 5, true, false);
    assertRepositoryReportDetail(reportDetails.get(idx++), pathname3, "policyName", 5, false, false);
    assertRepositoryReportDetail(reportDetails.get(idx++), pathname4, "policyName", 1, true, true);
    assertRepositoryReportDetail(reportDetails.get(idx++), pathname2, null, 0, true, false);
    assertRepositoryReportDetail(reportDetails.get(idx), pathname4, null, 0, true, false);
  }

  @Test
  public void testGetReportDetails_ByHash() throws Exception {
    final RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    final Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID);

    RepositoryComponent component1 = tempEntity.newRepositoryComponent(repository, "hash1");
    RepositoryComponent component2 = tempEntity.newRepositoryComponent(repository, "hash1");
    tempEntity.newRepositoryComponent(repository, "hash2");

    // Add a component for a different repository, which should not be included in current repo details
    final Repository repositoryOther = tempEntity.newRepository(repositoryManager, "otherRepoPublicId");
    tempEntity.newRepositoryComponent(repositoryOther, "hash1");

    final List<RepositoryReportDetail> reportDetails = repositoryService
        .getReportDetails(repository.getId(), "hash1", null);

    assertThat(reportDetails).hasSize(2);

    for (RepositoryReportDetail detail : reportDetails) {
      assertThat(detail.getHash()).isEqualTo("hash1");
      assertThat(detail.getPathname()).isIn(component1.getPathname(), component2.getPathname());
    }
  }

  @Test
  public void testGetReportDetails_ByPathname() throws Exception {
    final RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    final Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID);

    RepositoryComponent component = tempEntity.newRepositoryComponent(repository, "hash1");
    tempEntity.newRepositoryComponent(repository, "hash1");

    // Add a component for a different repository, which should not be included in current repo details
    final Repository repositoryOther = tempEntity.newRepository(repositoryManager, "otherRepoPublicId");
    tempEntity.newRepositoryComponent(repositoryOther.getId(), component.getPathname());

    final List<RepositoryReportDetail> reportDetails = repositoryService
        .getReportDetails(repository.getId(), null, component.getPathname());

    assertThat(reportDetails).hasSize(1);
    assertThat(reportDetails.get(0).getPathname()).isEqualTo(component.getPathname());
  }

  @Test
  public void testGetRepositoryById() {
    Repository repository = tempEntity.newRepository();
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(), new Date());

    RepositoryDTO actual = repositoryService.getRepositoryById(repository.getId());
    assertThat(actual.repository).isNotNull();
    assertThat(actual.repository.getPublicId()).isEqualTo(repository.getPublicId());
    assertThat(actual.oldestEvalTimestamp).isEqualTo(repositoryComponent.getLastEvaluationTime().getTime());
  }

  @Test
  public void testGetRepositoryById_NoEvaluation() {
    Repository repository = tempEntity.newRepository();

    RepositoryDTO actual = repositoryService.getRepositoryById(repository.getId());
    assertThat(actual.repository).isNotNull();
    assertThat(actual.repository.getPublicId()).isEqualTo(repository.getPublicId());
    assertThat(actual.oldestEvalTimestamp).isNull();
  }

  @Test
  public void testGetRepositoryById_UnknownId() throws Exception {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      repositoryService.getRepositoryById("foobar");
    }).withMessage("Cannot find a repository with ID foobar.");
  }

  @Test
  public void testReevaluateRepository() throws Exception {
    Repository repository = tempEntity.newRepository();
    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), DateUtils.addDays(new Date(), -1));

    ComponentEvaluationDataList response = new ComponentEvaluationDataList();
    ComponentEvaluationData component = new ComponentEvaluationData();
    component.hash = repositoryComponent.getHash();
    component.observedLicenses = Collections.emptySet();
    component.declaredLicenses = Collections.emptySet();
    component.matchState = MatchState.UNKNOWN.getId();
    response.components.add(component);
    when(auditHdsClient
        .post(any(), eq(ComponentEvaluationDataList.class), eq(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH),
            isNull(), any(RepositoryComponentEvaluationDataRequestList.class))).thenReturn(response);

    Date beforeEvaluation = new Date();
    repositoryService.reevaluateRepository(repository.getId());

    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      Date lastEvaluationTime = repositoryComponentDAO.getByRepositoryId(repository.getId()).get(0)
          .getLastEvaluationTime();
      assertThat(lastEvaluationTime).isAfterOrEqualTo(beforeEvaluation);
    });
  }

  @Test
  public void testReevaluateRepository_UnknownId() throws Exception {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      repositoryService.reevaluateRepository("foobar");
    }).withMessage("Cannot find a repository with ID foobar.");
  }

  @Test
  public void testDeleteRepository() throws Exception {
    Repository repository = tempEntity.newRepository();
    repositoryService.deleteRepository(repository.getId());
    assertThat(repositoryDAO.getById(repository.getId())).isNull();
  }

  @Test
  public void testDeleteRepository_UnknownId() throws Exception {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      repositoryService.deleteRepository("foobar");
    }).withMessage("Cannot find a repository with ID foobar.");
  }

  @Test
  public void testReevaluateComponent() throws Exception {
    Repository repository = tempEntity.newRepository();
    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), DateUtils.addDays(new Date(), -1));

    ComponentEvaluationDataList response = new ComponentEvaluationDataList();
    ComponentEvaluationData component = new ComponentEvaluationData();
    component.hash = repositoryComponent.getHash();
    component.observedLicenses = Collections.emptySet();
    component.declaredLicenses = Collections.emptySet();
    component.matchState = MatchState.UNKNOWN.getId();
    response.components.add(component);
    when(auditHdsClient
        .post(any(), eq(ComponentEvaluationDataList.class), eq(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH),
            isNull(), any(RepositoryComponentEvaluationDataRequestList.class))).thenReturn(response);

    repositoryService.reevaluateComponent(repository.getId(), repositoryComponent.getHash(), null);

    RepositoryComponent actualComponent = repositoryComponentDAO
        .getByRepositoryIdAndPathname(repository.getId(), repositoryComponent.getPathname());

    assertThat(actualComponent.getLastEvaluationTime()).isAfter(repositoryComponent.getLastEvaluationTime());
  }

  @Test
  public void testReevaluateComponent_UnknownHash() throws Exception {
    Repository repo = tempEntity.newRepository();
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      repositoryService.reevaluateComponent(repo.getId(), "missing-hash", null);
    }).withMessage("Cannot find a repository component for hash missing-hash in " + repo.getPublicId() + ".");
  }

  private RepositoryComponent createRepositoryPolicyViolation(final Repository repository,
                                                              final String pathname,
                                                              int... threatLevels)
  {
    return createRepositoryPolicyViolation(repository, pathname, false, threatLevels);
  }

  private RepositoryComponent createRepositoryPolicyViolation(final Repository repository,
                                                              final String pathname,
                                                              final boolean waived,
                                                              int... threatLevels)
  {
    RepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(), pathname);
    for (final int threatLevel : threatLevels) {
      tempEntity.newRepositoryPolicyViolation(repository.getId(), threatLevel, pathname, waived, null);
    }
    return component;
  }

  private void assertRepositoryReportDetail(final RepositoryReportDetail actualReportDetail,
                                            final String expectedPathname,
                                            final String expectedPolicyName,
                                            final int expectedThreatLevel,
                                            final boolean expectedHighestThreatLevel,
                                            final boolean isWaived)
  {
    assertThat(actualReportDetail.getPathname()).isEqualTo(expectedPathname);
    assertThat(actualReportDetail.getPolicyName()).isEqualTo(expectedPolicyName);
    assertThat(actualReportDetail.getThreatLevel()).isEqualTo(expectedThreatLevel);
    assertThat(actualReportDetail.isHighestThreatLevel()).isEqualTo(expectedHighestThreatLevel);

    assertThat(actualReportDetail.getHash()).isEqualTo("hash");
    assertThat(actualReportDetail.getMatchState()).isEqualTo("exact");
    assertThat(actualReportDetail.getComponentDisplayText()).isEqualTo("g : a : v");
    assertThat(actualReportDetail.getComponentIdentifier().getFormat()).isEqualTo("maven");
    assertThat(actualReportDetail.isQuarantined()).isFalse();
    assertThat(actualReportDetail.isWaived()).isEqualTo(isWaived);
  }

  private Policy createQuarantiningPolicy(Repository repository) {
    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId());
    policy.setAction(ProxyStageType.ID, Action.ID_FAIL);
    new PolicyDAO().update(policy);
    return policy;
  }

  @Test
  public void testDeleteRepository_PolicyViolationLogger_RepositoryEnabled() throws Exception {
    Repository repository = tempEntity.newRepository();

    Date before = new Date();
    repositoryService.deleteRepository(repository.getId());
    Date after = new Date();

    List<PolicyViolationLogDTO> policyViolationLogDTOs = PolicyViolationLogDTOAssert
        .assertPolicyViolationLogDTOs(policyViolationLoggerOutput, 1);
    PolicyViolationLogDTOAssert
        .assertRepositoryPolicyViolationData(policyViolationLogDTOs.get(0), PolicyViolationLogEvent.CLEAR, repository,
            before, after);
  }

  @Test
  public void testDeleteRepository_PolicyViolationLogger_RepositoryDisabled() throws Exception {
    Repository repository = tempEntity.newRepository();
    repository.setEnabled(false);
    repositoryDAO.update(repository);

    repositoryService.deleteRepository(repository.getId());

    PolicyViolationLogDTOAssert.assertPolicyViolationLogDTOs(policyViolationLoggerOutput, 0);
  }
}
