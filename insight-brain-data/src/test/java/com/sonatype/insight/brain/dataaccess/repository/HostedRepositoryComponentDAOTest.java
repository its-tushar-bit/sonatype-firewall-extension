/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.OwnerComponentDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.configuration.CallFlowAnalysisConfigDAO;
import com.sonatype.insight.brain.dataaccess.configuration.DataRetentionPolicyDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentCopyrightDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentLegalFileDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationAttributionDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssSeverityDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssVectorDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCweDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomRemediationDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityGroupDAO;
import com.sonatype.insight.brain.db.jooq.JooqSqlCounterListener;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.configuration.CallFlowAnalysisConfig;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.ComponentLegalFile;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;
import com.sonatype.insight.brain.model.legal.LegalFileType;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCvssSeverity;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCvssVector;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCwe;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomRemediation;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityGroup;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HostedRepositoryComponentDAOTest
    extends AbstractDbDAOTest
{
  private HostedRepositoryComponentDAO dao;

  private HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  private OwnerComponentDAO ownerComponentDAO;

  private PolicyDAO policyDAO;

  private PolicyWaiverDAO policyWaiverDAO;

  private PolicyWaiverRequestDAO policyWaiverRequestDAO;

  private LicenseOverrideDAO licenseOverrideDAO;

  private SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO;

  private DataRetentionPolicyDAO dataRetentionPolicyDAO;

  private PolicyMonitoringDAO policyMonitoringDAO;

  private ComponentCopyrightDAO componentCopyrightDAO;

  private ComponentLegalFileDAO componentLegalFileDAO;

  private ComponentObligationDAO componentObligationDAO;

  private ComponentObligationAttributionDAO componentObligationAttributionDAO;

  private VulnerabilityGroupDAO vulnerabilityGroupDAO;

  private VulnerabilityCustomRemediationDAO vulnerabilityCustomRemediationDAO;

  private VulnerabilityCustomCweDAO vulnerabilityCustomCweDAO;

  private VulnerabilityCustomCvssVectorDAO vulnerabilityCustomCvssVectorDAO;

  private VulnerabilityCustomCvssSeverityDAO vulnerabilityCustomCvssSeverityDAO;

  private CallFlowAnalysisConfigDAO callFlowAnalysisConfigDAO;

  private PolicyEvaluationDAO policyEvaluationDAO;

  private PolicyViolationDAO policyViolationDAO;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createHostedRepositoryComponentDAO();
    hostedRepositoryComponentDAO = dao;
    ownerComponentDAO = daoFactory.createOwnerComponentDAO();
    policyDAO = daoFactory.createPolicyDAO();
    policyWaiverDAO = daoFactory.createPolicyWaiverDAO();
    policyWaiverRequestDAO = daoFactory.createPolicyWaiverRequestDAO();
    licenseOverrideDAO = daoFactory.createLicenseOverrideDAO();
    securityVulnerabilityOverrideDAO = daoFactory.createSecurityVulnerabilityOverrideDAO();
    dataRetentionPolicyDAO = daoFactory.createDataRetentionPolicyDAO();
    policyMonitoringDAO = daoFactory.createPolicyMonitoringDAO();
    componentCopyrightDAO = daoFactory.createComponentCopyrightDAO();
    componentLegalFileDAO = daoFactory.createComponentLegalFileDAO();
    componentObligationDAO = daoFactory.createComponentObligationDAO();
    componentObligationAttributionDAO = daoFactory.createComponentObligationAttributionDAO();
    vulnerabilityGroupDAO = daoFactory.createVulnerabilityGroupDAO();
    vulnerabilityCustomRemediationDAO = daoFactory.createVulnerabilityCustomRemediationDAO();
    vulnerabilityCustomCweDAO = daoFactory.createVulnerabilityCustomCweDAO();
    vulnerabilityCustomCvssVectorDAO = daoFactory.createVulnerabilityCustomCvssVectorDAO();
    vulnerabilityCustomCvssSeverityDAO = daoFactory.createVulnerabilityCustomCvssSeverityDAO();
    callFlowAnalysisConfigDAO = daoFactory.createCallFlowAnalysisConfigDAO();
    policyEvaluationDAO = daoFactory.createPolicyEvaluationDAO();
    policyViolationDAO = daoFactory.createPolicyViolationDAO();
  }

  @Test
  public void testInsertAndGetById() {
    HostedRepositoryComponent hrc = new HostedRepositoryComponent(repository.getId(), "path/foo.jar", "abc123");
    hrc.setComponentId("nxrm-comp-1");
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.insert(tx, hrc);
      tx.commit();
    }
    assertThat(hrc.getId()).isNotNull();

    HostedRepositoryComponent loaded;
    try (TransactionContext tx = dao.createTransactionContext()) {
      loaded = dao.getById(tx, hrc.getId());
    }
    assertThat(loaded).isNotNull();
    assertThat(loaded.getRepositoryId()).isEqualTo(repository.getId());
    assertThat(loaded.getPathname()).isEqualTo("path/foo.jar");
    assertThat(loaded.getHash()).isEqualTo("abc123");
    assertThat(loaded.getComponentId()).isEqualTo("nxrm-comp-1");
    assertThat(loaded.getOwnerComponentId()).isNull();
  }

  @Test
  public void testUpdate() {
    HostedRepositoryComponent hrc = seedHrc("path/orig.jar", "hash1");
    hrc.setPathname("path/renamed.jar");
    hrc.setHash("hash2");
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.update(tx, hrc);
      tx.commit();
    }
    HostedRepositoryComponent loaded;
    try (TransactionContext tx = dao.createTransactionContext()) {
      loaded = dao.getById(tx, hrc.getId());
    }
    assertThat(loaded.getPathname()).isEqualTo("path/renamed.jar");
    assertThat(loaded.getHash()).isEqualTo("hash2");
  }

  @Test
  public void testGetByRepositoryIdAndPathname_hit() {
    HostedRepositoryComponent hrc = seedHrc("path/a.jar", "hash-a");
    HostedRepositoryComponent found;
    try (TransactionContext tx = dao.createTransactionContext()) {
      found = dao.getByRepositoryIdAndPathname(tx, repository.getId(), "path/a.jar");
    }
    assertThat(found).isNotNull();
    assertThat(found.getId()).isEqualTo(hrc.getId());
  }

  @Test
  public void testGetByRepositoryIdAndPathname_miss() {
    HostedRepositoryComponent found;
    try (TransactionContext tx = dao.createTransactionContext()) {
      found = dao.getByRepositoryIdAndPathname(tx, repository.getId(), "path/nonexistent.jar");
    }
    assertThat(found).isNull();
  }

  @Test
  public void testGetByRepositoryId_returnsAllRowsForRepository() {
    HostedRepositoryComponent hrcA = seedHrc("path/a.jar", "hash-a");
    HostedRepositoryComponent hrcB = seedHrc("path/b.jar", "hash-b");
    List<HostedRepositoryComponent> found;
    try (TransactionContext tx = dao.createTransactionContext()) {
      found = dao.getByRepositoryId(tx, repository.getId());
    }
    assertThat(found).extracting(HostedRepositoryComponent::getId)
        .containsExactlyInAnyOrder(hrcA.getId(), hrcB.getId());
  }

  @Test
  public void testGetByIdNotNull_throwsWhenMissing() {
    assertThatThrownBy(() -> {
      try (TransactionContext tx = dao.createTransactionContext()) {
        dao.getByIdNotNull(tx, "does-not-exist-id");
      }
    }).isInstanceOf(NotFoundException.class);
  }

  @Test
  public void testDelete_deletesRowAndInvokesCascade() {
    HostedRepositoryComponent hrc = seedHrc("path/b.jar", "hash-b");
    OwnerComponent oc = new OwnerComponent(hrc.getId(), BuildStageType.ID,
        new Date(), "hash-b",
        ComponentIdentifier.createMavenCoordinates("g", "a", "1.0"),
        MatchState.EXACT.getId(),
        IdentificationSource.SONATYPE.getId(), false, null);
    ownerComponentDAO.insert(oc);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.delete(tx, hrc);
      tx.commit();
    }

    try (TransactionContext tx = dao.createTransactionContext()) {
      assertThat(dao.getById(tx, hrc.getId())).isNull();
    }
    assertThat(ownerComponentDAO.getById(oc.getId())).isNull();
  }

  @Test
  public void testDelete_ownerComponentIdFkSetsNullOnParentDelete() {
    OwnerComponent oc = new OwnerComponent(application.getId(), BuildStageType.ID,
        new Date(), "hash-c",
        ComponentIdentifier.createMavenCoordinates("g", "a", "1.0"),
        MatchState.EXACT.getId(),
        IdentificationSource.SONATYPE.getId(), false, null);
    ownerComponentDAO.insert(oc);

    HostedRepositoryComponent hrc = new HostedRepositoryComponent(repository.getId(), "path/c.jar", "hash-c");
    hrc.setOwnerComponentId(oc.getId());
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.insert(tx, hrc);
      tx.commit();
    }

    ownerComponentDAO.delete(oc);

    HostedRepositoryComponent reloaded;
    try (TransactionContext tx = dao.createTransactionContext()) {
      reloaded = dao.getById(tx, hrc.getId());
    }
    assertThat(reloaded).isNotNull();
    assertThat(reloaded.getOwnerComponentId()).isNull();
  }

  @Test
  public void testDeleteByRepositoryId_batchDeletesAllHrcsAndOwnedRowsAcrossChunks() {
    // DELETE_CHUNK_SIZE in HostedRepositoryComponentDAO is 500; seed enough rows to force 2 chunks.
    int hrcCount = 520;
    List<HostedRepositoryComponent> hrcs = new ArrayList<>();
    for (int i = 0; i < hrcCount; i++) {
      hrcs.add(new HostedRepositoryComponent(repository.getId(), "path/chunk-test-" + i + ".jar", "hash-" + i));
    }
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.insertBatch(tx, hrcs, false);
      tx.commit();
    }

    HostedRepositoryComponent hrcWithOverride = hrcs.get(0);
    HostedRepositoryComponent hrcWithOwnerComponent = hrcs.get(1);
    // Dedicated HRC owner ID for seeding one row per remaining owner-keyed satellite table, so this test verifies
    // cleanup across the full table set OwnerDAO.cascadeDeleteByOwnerIds touches, not just 2-3 of them.
    String satOwnerId = hrcs.get(2).getId();

    OwnerComponent oc = new OwnerComponent(hrcWithOwnerComponent.getId(), BuildStageType.ID,
        new Date(), "hash-owner-component",
        ComponentIdentifier.createMavenCoordinates("g", "a", "1.0"),
        MatchState.EXACT.getId(),
        IdentificationSource.SONATYPE.getId(), false, null);
    ownerComponentDAO.insert(oc);

    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    Map<String, String> actionsOverride = new HashMap<>();
    actionsOverride.put(BuildStageType.ID, "warn");
    policy.addPolicyActionsOverride(hrcWithOverride.getId(), actionsOverride);
    policyDAO.update(policy);

    ComponentIdentifier ci = ComponentIdentifier.createMavenCoordinates("sat-g", "sat-a", "1.0");
    PolicyWaiver waiver = tempEntity.newWaiver("sat-hash", policy.getId(), satOwnerId);
    PolicyWaiverRequest waiverRequest =
        tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest(policy.getId(), satOwnerId, "sat comment"));
    LicenseOverride licenseOverride =
        tempEntity.newLicenseOverride(satOwnerId, ci, LicenseOverrideStatus.OVERRIDDEN, "MIT");
    SecurityVulnerabilityOverride svOverride = tempEntity.newSecurityVulnerabilityOverride(satOwnerId, "sat-hash",
        "source", "sat-refId", SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED);
    // No tempEntity helper exists for DataRetentionPolicy, so it's cleaned up explicitly in the finally block below.
    DataRetentionPolicy dataRetentionPolicy = new DataRetentionPolicy(satOwnerId, "sat-context");
    dataRetentionPolicyDAO.insert(dataRetentionPolicy);
    // VulnerabilityGroupDAO.insert() validates the owner's org hierarchy, which does not apply to HRC-scoped owners
    // (an HRC's parent owner is its repository, not an organization), so it's inserted directly to bypass that; it
    // has no tempEntity helper either, so it's also cleaned up explicitly in the finally block below.
    VulnerabilityGroup vulnerabilityGroup = new VulnerabilityGroup("sat-group", satOwnerId);
    try (TransactionContext tx = vulnerabilityGroupDAO.createTransactionContext()) {
      tx.begin();
      vulnerabilityGroupDAO.insertBatch(tx, List.of(vulnerabilityGroup), false);
      tx.commit();
    }
    try {
      PolicyMonitoring policyMonitoring = tempEntity.newPolicyMonitoring(satOwnerId, ProxyStageType.ID);
      ComponentCopyright componentCopyright = tempEntity.newComponentCopyright(ci, satOwnerId, "sat-legal-hash");
      ComponentLegalFile componentLegalFile =
          tempEntity.newComponentLegalFile(ci, satOwnerId, LegalFileType.NOTICE, "sat-legal-hash");
      ComponentObligation componentObligation = tempEntity.newComponentObligation(ci, satOwnerId, "sat-obligation",
          "sat comment", ObligationStatus.OPEN, "sat-legal-hash");
      ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(ci,
          satOwnerId, "sat-obligation", "sat content", "sat-legal-hash");
      VulnerabilityCustomRemediation vulnerabilityCustomRemediation =
          tempEntity.newVulnerabilityCustomRemediation(satOwnerId);
      VulnerabilityCustomCwe vulnerabilityCustomCwe =
          tempEntity.newVulnerabilityCustomCwe(satOwnerId, "sat-refId", new Date(), "CWE-1");
      VulnerabilityCustomCvssVector vulnerabilityCustomCvssVector =
          tempEntity.newVulnerabilityCustomCvssVector(satOwnerId, "sat-refId", ci, new Date(), "sat-vector");
      VulnerabilityCustomCvssSeverity vulnerabilityCustomCvssSeverity =
          tempEntity.newVulnerabilityCustomCvssSeverity(satOwnerId, "sat-refId", 5.0f);
      CallFlowAnalysisConfig callFlowAnalysisConfig = tempEntity.newCallFlowAnalysisConfig(satOwnerId, 4);
      PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(satOwnerId, BuildStageType.ID, "sat-scan");
      PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

      try (TransactionContext tx = dao.createTransactionContext()) {
        tx.begin();
        dao.deleteByRepositoryId(tx, repository.getId());
        tx.commit();
      }

      try (TransactionContext tx = dao.createTransactionContext()) {
        assertThat(dao.getByRepositoryId(tx, repository.getId())).isEmpty();
      }
      assertThat(ownerComponentDAO.getById(oc.getId())).isNull();

      Policy reloadedPolicy = policyDAO.getById(policy.getId());
      assertThat(reloadedPolicy.getPolicyActionsOverrides()).doesNotContainKey(hrcWithOverride.getId());

      assertThat(policyWaiverDAO.getByOwnerId(satOwnerId)).isEmpty();
      assertThat(policyWaiverRequestDAO.getByOwnerId(satOwnerId)).isEmpty();
      assertThat(licenseOverrideDAO.getByOwnerId(satOwnerId)).isEmpty();
      assertThat(securityVulnerabilityOverrideDAO.getByOwnerId(satOwnerId)).isEmpty();
      assertThat(dataRetentionPolicyDAO.getByOwnerId(satOwnerId)).isEmpty();
      assertThat(policyMonitoringDAO.getByOwnerId(satOwnerId)).isEmpty();
      assertThat(componentCopyrightDAO.getByOwnerId(satOwnerId)).isEmpty();
      assertThat(componentLegalFileDAO.getByOwnerId(satOwnerId)).isEmpty();
      assertThat(componentObligationDAO.getByOwnerId(satOwnerId)).isEmpty();
      assertThat(componentObligationAttributionDAO.getByOwnerId(satOwnerId)).isEmpty();
      assertThat(vulnerabilityGroupDAO.getByOwnerId(satOwnerId)).isEmpty();
      assertThat(vulnerabilityCustomRemediationDAO.getByOwnerId(satOwnerId)).isEmpty();
      assertThat(vulnerabilityCustomCweDAO.getByOwnerId(satOwnerId)).isEmpty();
      assertThat(vulnerabilityCustomCvssVectorDAO.getByOwnerId(satOwnerId)).isEmpty();
      assertThat(vulnerabilityCustomCvssSeverityDAO.getByOwnerId(satOwnerId)).isEmpty();
      assertThat(callFlowAnalysisConfigDAO.getByOwnerId(satOwnerId)).isNull();
      assertThat(policyEvaluationDAO.getById(policyEvaluation.getId())).isNull();
      assertThat(policyViolationDAO.getById(policyViolation.getId())).isNull();
    }
    finally {
      dataRetentionPolicyDAO.delete(dataRetentionPolicy);
      vulnerabilityGroupDAO.delete(vulnerabilityGroup);
    }
  }

  /**
   * Batch delete must issue O(chunks) SELECTs, not O(HRC count). hrcCount is deliberately > the 500 chunk size
   * used by {@code deleteByRepositoryId} so this exercises 2 chunks, not just a single-chunk pass.
   * <p>
   * This query-count assertion is opt-in (see the {@code Assume} below) and does not run in a routine build,
   * matching the existing pattern used by e.g. {@code PolicyWaiverRequestDAOTest}. The unconditional
   * {@link #testDeleteByRepositoryId_batchDeletesAllHrcsAndOwnedRowsAcrossChunks()} above still exercises the same
   * chunking/cleanup logic on every run.
   * </p>
   */
  @Test
  public void testDeleteByRepositoryId_issuesBoundedQueryCount_notProportionalToHrcCount() {
    Assumptions.assumeTrue(JooqSqlCounterListener.getInstance().isEnabled(),
        "Enable with -DargLine=\"-DcustomMetrics=sqlcount\" to run this validation");

    int hrcCount = 600;
    List<HostedRepositoryComponent> hrcs = new ArrayList<>();
    for (int i = 0; i < hrcCount; i++) {
      hrcs.add(new HostedRepositoryComponent(repository.getId(), "path/qc-" + i + ".jar", "hash-qc-" + i));
    }
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.insertBatch(tx, hrcs, false);
      tx.commit();
    }

    // Multiple policies exist so that a per-HRC policyDAO.getAll() scan would show up as one SELECT per HRC.
    for (int i = 0; i < 20; i++) {
      tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    }

    JooqSqlCounterListener counter = JooqSqlCounterListener.getInstance();
    counter.reset();

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByRepositoryId(tx, repository.getId());
      tx.commit();
    }

    long selectCount = counter.getSelectCount();
    // 600 HRCs span 2 chunks (500 + 100); a bounded per-chunk pass should cost roughly the same regardless of
    // hrcCount, so this must stay far below hrcCount even though it now spans multiple chunks.
    assertThat(selectCount)
        .as("Batch delete over %s HRCs (2 chunks) must issue a bounded number of SELECTs, not one per HRC", hrcCount)
        .isLessThan(hrcCount / 10);
  }

  @Test
  public void testDeleteByRepositoryId_noOpWhenNoHrcsExist() {
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByRepositoryId(tx, repository.getId());
      tx.commit();
    }

    try (TransactionContext tx = dao.createTransactionContext()) {
      assertThat(dao.getByRepositoryId(tx, repository.getId())).isEmpty();
    }
  }

  @Test
  public void testDeleteByRepositoryId_deletesExactlyOneFullChunk() {
    // Exactly DELETE_CHUNK_SIZE (500) rows: exercises the exact-multiple boundary, distinct from the
    // over-the-boundary (501/520) cases covered elsewhere.
    int hrcCount = 500;
    List<HostedRepositoryComponent> hrcs = new ArrayList<>();
    for (int i = 0; i < hrcCount; i++) {
      hrcs.add(new HostedRepositoryComponent(repository.getId(), "path/exact-chunk-" + i + ".jar", "hash-" + i));
    }
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.insertBatch(tx, hrcs, false);
      tx.commit();
    }

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByRepositoryId(tx, repository.getId());
      tx.commit();
    }

    try (TransactionContext tx = dao.createTransactionContext()) {
      assertThat(dao.getByRepositoryId(tx, repository.getId())).isEmpty();
    }
  }

  @Test
  public void getLastScanTimesByRepositoryIds_returnsMaxAcrossHrcsAndStages() {
    Repository repoA = tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo-a");
    Repository repoB = tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo-b");

    HostedRepositoryComponent hrcA1 = tempEntity.newHostedRepositoryComponent(repoA);
    HostedRepositoryComponent hrcA2 = tempEntity.newHostedRepositoryComponent(repoA);
    HostedRepositoryComponent hrcB1 = tempEntity.newHostedRepositoryComponent(repoB);

    Date earlier = new Date(1_700_000_000_000L);
    Date later = new Date(1_800_000_000_000L);
    Date newest = new Date(1_900_000_000_000L);

    PolicyEvaluation peA1 = tempEntity.newPolicyEvaluation(hrcA1.getId(), "build", TemporaryEntity.uuid(),
        false, false, earlier, ScanTriggerType.HOSTED_REPOSITORY_SCANNING);
    PolicyEvaluation peA2 = tempEntity.newPolicyEvaluation(hrcA2.getId(), "release", TemporaryEntity.uuid(),
        false, false, later, ScanTriggerType.HOSTED_REPOSITORY_SCANNING);
    PolicyEvaluation peB1 = tempEntity.newPolicyEvaluation(hrcB1.getId(), "build", TemporaryEntity.uuid(),
        false, false, newest, ScanTriggerType.HOSTED_REPOSITORY_SCANNING);

    Map<String, Date> times = hostedRepositoryComponentDAO
        .getLastScanTimesByRepositoryIds(List.of(repoA.getId(), repoB.getId()));

    assertThat(times).containsExactlyInAnyOrderEntriesOf(Map.of(
        repoA.getId(), later,
        repoB.getId(), newest));
  }

  @Test
  public void getLastScanTimesByRepositoryIds_omitsRepositoriesWithNoEvaluations() {
    Repository repo = tempEntity.newRepository(tempEntity.newRepositoryManager(), "unscanned-repo");
    tempEntity.newHostedRepositoryComponent(repo);

    Map<String, Date> times = hostedRepositoryComponentDAO
        .getLastScanTimesByRepositoryIds(List.of(repo.getId()));

    assertThat(times).isEmpty();
  }

  @Test
  public void getLastScanTimesByRepositoryIds_multiStageSameHrc_picksLatestAcrossStages() {
    Repository repo = tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo-multistage");
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repo);

    Date buildTime = new Date(1_700_000_000_000L);
    Date releaseTime = new Date(1_800_000_000_000L);

    tempEntity.newPolicyEvaluation(hrc.getId(), "build", TemporaryEntity.uuid(),
        false, false, buildTime, ScanTriggerType.HOSTED_REPOSITORY_SCANNING);
    tempEntity.newPolicyEvaluation(hrc.getId(), "release", TemporaryEntity.uuid(),
        false, false, releaseTime, ScanTriggerType.HOSTED_REPOSITORY_SCANNING);

    Map<String, Date> times = hostedRepositoryComponentDAO
        .getLastScanTimesByRepositoryIds(List.of(repo.getId()));

    // A single HRC evaluated at two stages must surface the newer of the two — not both, not the older.
    assertThat(times).containsEntry(repo.getId(), releaseTime);
  }

  @Test
  public void getLastScanTimesByRepositoryIds_reEvaluationSameStage_picksLatestEvaluation() {
    Repository repo = tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo-reeval");
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repo);

    Date first = new Date(1_700_000_000_000L);
    Date reeval = new Date(1_800_000_000_000L);

    tempEntity.newPolicyEvaluation(hrc.getId(), "build", TemporaryEntity.uuid(),
        false, false, first, ScanTriggerType.HOSTED_REPOSITORY_SCANNING);
    tempEntity.newPolicyEvaluation(hrc.getId(), "build", TemporaryEntity.uuid(),
        true, false, reeval, ScanTriggerType.HOSTED_REPOSITORY_SCANNING);

    Map<String, Date> times = hostedRepositoryComponentDAO
        .getLastScanTimesByRepositoryIds(List.of(repo.getId()));

    assertThat(times).containsEntry(repo.getId(), reeval);
  }

  @Test
  public void getLastScanTimesByRepositoryIds_emptyInput_returnsEmptyMap() {
    assertThat(hostedRepositoryComponentDAO.getLastScanTimesByRepositoryIds(List.of())).isEmpty();
    assertThat(hostedRepositoryComponentDAO.getLastScanTimesByRepositoryIds((Collection<String>) null)).isEmpty();
  }

  @Test
  public void getByRepositoryIdPaged_returnsHrcsWithJoinedOwnerComponent() {
    Repository repo = tempEntity.newRepository(tempEntity.newRepositoryManager("nx"), "repo");
    HostedRepositoryComponent hrc1 = tempEntity.newHostedRepositoryComponent(repo, "lib/aaa.jar", "hash-aaa");
    HostedRepositoryComponent hrc2 = tempEntity.newHostedRepositoryComponent(repo, "lib/bbb.jar", "hash-bbb");

    OwnerComponent oc1 = new OwnerComponent(hrc1.getId(), "build", new Date(), "hash-aaa",
        ComponentIdentifier.createMavenCoordinates("g", "aaa", "1.0"),
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), false, List.of("lib/aaa.jar"));
    ownerComponentDAO.insert(oc1);
    try (TransactionContext tx = hostedRepositoryComponentDAO.createTransactionContext()) {
      hostedRepositoryComponentDAO.updateOwnerComponentId(tx, hrc1.getId(), oc1.getId());
    }

    List<HostedRepositoryComponentDAO.HrcWithOwnerComponent> rows =
        hostedRepositoryComponentDAO.getByRepositoryIdPaged(repo.getId(), null, 10, 0);

    assertThat(rows).hasSize(2);
    HostedRepositoryComponentDAO.HrcWithOwnerComponent first = rows.get(0);
    HostedRepositoryComponentDAO.HrcWithOwnerComponent second = rows.get(1);
    assertThat(first.hrc().getPathname()).isEqualTo("lib/aaa.jar");
    assertThat(first.ownerComponent()).isNotNull();
    assertThat(first.ownerComponent().getMatchStateId()).isEqualTo(MatchState.EXACT.getId());
    assertThat(second.hrc().getPathname()).isEqualTo("lib/bbb.jar");
    assertThat(second.ownerComponent()).isNull();
  }

  @Test
  public void getByRepositoryIdPaged_filterMatchesPathnameCaseInsensitive() {
    Repository repo = tempEntity.newRepository(tempEntity.newRepositoryManager("nx"), "repo-filter");
    tempEntity.newHostedRepositoryComponent(repo, "lib/log4j-core-2.14.1.jar", "h1");
    tempEntity.newHostedRepositoryComponent(repo, "lib/commons-text-1.9.0.jar", "h2");

    List<HostedRepositoryComponentDAO.HrcWithOwnerComponent> rows =
        hostedRepositoryComponentDAO.getByRepositoryIdPaged(repo.getId(), "LOG4J", 10, 0);

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).hrc().getPathname()).contains("log4j");
  }

  @Test
  public void getByRepositoryIdPaged_filterEscapesLikeMetacharacters() {
    Repository repo = tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo-escape");
    tempEntity.newHostedRepositoryComponent(repo, "lib/100%-valid.jar", "h1");
    tempEntity.newHostedRepositoryComponent(repo, "lib/file_underscore.jar", "h2");
    tempEntity.newHostedRepositoryComponent(repo, "lib/plain.jar", "h3");

    // '%' filter must match the literal percent, not act as a wildcard.
    assertThat(hostedRepositoryComponentDAO.countByRepositoryIdWithFilter(repo.getId(), "%")).isEqualTo(1);
    // '_' filter must match the literal underscore, not any-char wildcard.
    assertThat(hostedRepositoryComponentDAO.countByRepositoryIdWithFilter(repo.getId(), "_")).isEqualTo(1);
    // Backslash filter must not create a broken escape sequence.
    assertThat(hostedRepositoryComponentDAO.countByRepositoryIdWithFilter(repo.getId(), "\\")).isEqualTo(0);
  }

  @Test
  public void getByRepositoryIdPaged_filterMatchesOwnerComponentCoordinatesJson() {
    Repository repo = tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo-filter-coords");
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repo, "lib/mystery.jar", "h1");
    OwnerComponent oc = new OwnerComponent(hrc.getId(), "build", new Date(), "h1",
        new ComponentIdentifier("maven",
            Map.of("groupId", "org.apache.logging.log4j", "artifactId", "log4j-core", "version", "2.14.1")),
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), false, List.of("lib/mystery.jar"));
    ownerComponentDAO.insert(oc);
    try (TransactionContext tx = hostedRepositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      hostedRepositoryComponentDAO.updateOwnerComponentId(tx, hrc.getId(), oc.getId());
      tx.commit();
    }

    // Filter text appears only inside the coordinates JSON, never in the pathname — matches the
    // UI's rendered displayName so the search field agrees with what the user sees.
    assertThat(hostedRepositoryComponentDAO.countByRepositoryIdWithFilter(repo.getId(), "log4j")).isEqualTo(1);
    assertThat(hostedRepositoryComponentDAO.countByRepositoryIdWithFilter(repo.getId(), "org.apache.logging"))
        .isEqualTo(1);
  }

  @Test
  public void countByRepositoryIdWithFilter_respectsFilter() {
    Repository repo = tempEntity.newRepository(tempEntity.newRepositoryManager("nx"), "repo-count");
    tempEntity.newHostedRepositoryComponent(repo, "lib/log4j.jar", "h1");
    tempEntity.newHostedRepositoryComponent(repo, "lib/commons.jar", "h2");
    tempEntity.newHostedRepositoryComponent(repo, "lib/logback.jar", "h3");

    assertThat(hostedRepositoryComponentDAO.countByRepositoryIdWithFilter(repo.getId(), null)).isEqualTo(3);
    assertThat(hostedRepositoryComponentDAO.countByRepositoryIdWithFilter(repo.getId(), "log")).isEqualTo(2);
    assertThat(hostedRepositoryComponentDAO.countByRepositoryIdWithFilter(repo.getId(), "")).isEqualTo(3);
  }

  @Test
  public void getByRepositoryIdPaged_pagination() {
    Repository repo = tempEntity.newRepository(tempEntity.newRepositoryManager("nx"), "repo-page");
    for (int i = 0; i < 5; i++) {
      tempEntity.newHostedRepositoryComponent(repo, String.format("path/%02d.jar", i), "h" + i);
    }
    assertThat(hostedRepositoryComponentDAO.getByRepositoryIdPaged(repo.getId(), null, 2, 0)).hasSize(2);
    assertThat(hostedRepositoryComponentDAO.getByRepositoryIdPaged(repo.getId(), null, 2, 2)).hasSize(2);
    assertThat(hostedRepositoryComponentDAO.getByRepositoryIdPaged(repo.getId(), null, 2, 4)).hasSize(1);
  }

  @Test
  public void getByRepositoryIdPaged_paginationRespectsFilter() {
    // Locks in the count/page invariant: countByRepositoryIdWithFilter and getByRepositoryIdPaged
    // must apply the same filter+join+order, so page 1 + page 2 union to exactly the filtered set,
    // in pathname order, with no dupes and no non-matching rows leaking through.
    Repository repo = tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo-filter-page");
    for (int i = 0; i < 5; i++) {
      tempEntity.newHostedRepositoryComponent(repo, String.format("libs/log4j-%02d.jar", i), "h-log-" + i);
    }
    for (int i = 0; i < 3; i++) {
      tempEntity.newHostedRepositoryComponent(repo, String.format("libs/commons-%02d.jar", i), "h-com-" + i);
    }

    assertThat(hostedRepositoryComponentDAO.countByRepositoryIdWithFilter(repo.getId(), "log4j")).isEqualTo(5);

    var page1 = hostedRepositoryComponentDAO.getByRepositoryIdPaged(repo.getId(), "log4j", 3, 0);
    var page2 = hostedRepositoryComponentDAO.getByRepositoryIdPaged(repo.getId(), "log4j", 3, 3);

    assertThat(page1).extracting(r -> r.hrc().getPathname())
        .containsExactly("libs/log4j-00.jar", "libs/log4j-01.jar", "libs/log4j-02.jar");
    assertThat(page2).extracting(r -> r.hrc().getPathname())
        .containsExactly("libs/log4j-03.jar", "libs/log4j-04.jar");
    assertThat(page1).noneMatch(r -> r.hrc().getPathname().contains("commons"));
    assertThat(page2).noneMatch(r -> r.hrc().getPathname().contains("commons"));
  }

  @Test
  public void getByRepositoryIdAndComponentId_returnsHrcWithOwnerComponent() {
    Repository repo = tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo-lookup");
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repo, "lib/lookup.jar", "hlookup");
    OwnerComponent oc = new OwnerComponent(hrc.getId(), "build", new Date(), "hlookup",
        new ComponentIdentifier("maven", Map.of("groupId", "g", "artifactId", "a", "version", "1.0")),
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), false, List.of("lib/lookup.jar"));
    ownerComponentDAO.insert(oc);
    try (TransactionContext tx = hostedRepositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      hostedRepositoryComponentDAO.updateOwnerComponentId(tx, hrc.getId(), oc.getId());
      tx.commit();
    }

    HostedRepositoryComponentDAO.HrcWithOwnerComponent row =
        hostedRepositoryComponentDAO.getByRepositoryIdAndComponentId(repo.getId(), hrc.getId());

    assertThat(row).isNotNull();
    assertThat(row.hrc().getPathname()).isEqualTo("lib/lookup.jar");
    assertThat(row.ownerComponent()).isNotNull();
    assertThat(row.ownerComponent().getId()).isEqualTo(oc.getId());
  }

  @Test
  public void getByRepositoryIdAndComponentId_returnsNullWhenNotFound() {
    Repository repo = tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo-lookup-miss");
    assertThat(hostedRepositoryComponentDAO.getByRepositoryIdAndComponentId(repo.getId(), "missing-id")).isNull();
    assertThat(hostedRepositoryComponentDAO.getByRepositoryIdAndComponentId(null, "x")).isNull();
    assertThat(hostedRepositoryComponentDAO.getByRepositoryIdAndComponentId("x", null)).isNull();
  }

  @Test
  public void getByRepositoryIdAndComponentId_unpinnedHrc_returnsRowWithNullOwnerComponent() {
    Repository repo = tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo-unpinned");
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repo, "lib/x.jar", "hx");

    var row = hostedRepositoryComponentDAO.getByRepositoryIdAndComponentId(repo.getId(), hrc.getId());

    assertThat(row).isNotNull();
    assertThat(row.hrc().getId()).isEqualTo(hrc.getId());
    assertThat(row.ownerComponent()).isNull();
  }

  @Test
  public void getByRepositoryIdAndComponentId_rejectsCrossRepoLookup() {
    Repository repoA = tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo-cross-a");
    Repository repoB = tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo-cross-b");
    HostedRepositoryComponent hrcA = tempEntity.newHostedRepositoryComponent(repoA, "lib/a.jar", "ha");

    // Looking up hrcA under repoB must return null even though the id exists.
    assertThat(hostedRepositoryComponentDAO.getByRepositoryIdAndComponentId(repoB.getId(), hrcA.getId())).isNull();
    assertThat(hostedRepositoryComponentDAO.getByRepositoryIdAndComponentId(repoA.getId(), hrcA.getId())).isNotNull();
  }

  private HostedRepositoryComponent seedHrc(String pathname, String hash) {
    HostedRepositoryComponent hrc = new HostedRepositoryComponent(repository.getId(), pathname, hash);
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.insert(tx, hrc);
      tx.commit();
    }
    return hrc;
  }
}
