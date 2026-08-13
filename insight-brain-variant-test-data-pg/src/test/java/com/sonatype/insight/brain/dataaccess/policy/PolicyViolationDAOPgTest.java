/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.time.LocalDate;
import java.util.*;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolationConstraintFacts;
import com.sonatype.insight.brain.model.policy.ReachabilityStatus;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * PostgreSQL-backed tests relocated from {@link PolicyViolationDAOTest} (CLM-45228).
 */
@PostgresTest
public class PolicyViolationDAOPgTest
    extends AbstractDbDAOTest
{
  private PolicyViolationDAO dao;

  private PolicyViolationConstraintFactsDAO constraintFactsDAO;

  private OrganizationDAO organizationDAO;

  private RepositoryDAO repositoryDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createPolicyViolationDAO();
    constraintFactsDAO = daoFactory.createPolicyViolationConstraintFactsDAO();
    organizationDAO = daoFactory.createOrganizationDAO();
    repositoryDAO = daoFactory.createRepositoryDAO();

  }

  @Test
  public void testGetMeanTimeToRemediate_shouldCorrectlyComputeMeanTimeToRemediateMillisUsingPostgres() {
    doGetMeanTimeToRemediateShouldCorrectlyComputeMeanTimeToRemediateMillisUsingTest();
  }

  public void doGetMeanTimeToRemediateShouldCorrectlyComputeMeanTimeToRemediateMillisUsingTest() {
    // === Given ===
    final Date now = new Date();
    final Application application = tempEntity.newApplication(organization.getId());
    final Policy policy = tempEntity.newPolicy(application);
    final PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID,
        "scan-1", new Date(System.currentTimeMillis()));

    // App has 3 waived or fixed violations
    tempEntity.createWaivedPolicyViolation(
        policyEvaluation,
        policy,
        new Date(),
        11L);
    tempEntity.createWaivedPolicyViolation(
        policyEvaluation,
        policy,
        new Date(),
        3L);
    tempEntity.createFixedPolicyViolation(
        policyEvaluation,
        policy,
        new Date(),
        5L);

    // App has two waived or fixed violations outside the 3-month window
    tempEntity.createFixedPolicyViolation(
        policyEvaluation,
        policy,
        new Date(now.getTime() - (4L * 30L * 24L * 60L * 60L * 1000L)),
        50000L);
    tempEntity.createWaivedPolicyViolation(
        policyEvaluation,
        policy,
        new Date(now.getTime() - (5L * 30L * 24L * 60L * 60L * 1000L)),
        90000L);

    final long firstExpectedMeanTimeToRemediate = Math.round((float) (11L + 3L + 5L) / 3);

    long result = dao.getMeanTimeToRemediate(84);

    assertThat(result).isEqualTo(firstExpectedMeanTimeToRemediate);

    tempEntity.createFixedPolicyViolation(
        policyEvaluation,
        policy,
        new Date(),
        44L);

    result = dao.getMeanTimeToRemediate(84);

    final long secondExpectedMeanTimeToRemediate = Math.round((float) (11L + 3L + 5L + 44L) / 4);
    assertThat(result).isEqualTo(secondExpectedMeanTimeToRemediate);

    // with a larger look back window the other two waived violations affect the average
    final long expectedValueWith121DayWindow = Math.round((float) (11L + 3L + 5L + 44L + 50000L) / 5);
    final long expectedValueWith151DayWindow = Math.round((float) (11L + 3L + 5L + 44L + 50000L + 90000L) / 6);

    result = dao.getMeanTimeToRemediate(121);
    assertThat(result).isEqualTo(expectedValueWith121DayWindow);

    result = dao.getMeanTimeToRemediate(151);
    assertThat(result).isEqualTo(expectedValueWith151DayWindow);
  }

  @Test
  public void testGetMeanTimeToRemediate_shouldUseLeastValueWhenWaivedAndFixedUsingPostges() {
    doGetMeanTimeToRemediate_shouldUseLeastValueWhenWaivedAndFixed();
  }

  private void doGetMeanTimeToRemediate_shouldUseLeastValueWhenWaivedAndFixed() {
    // === Given ===
    final Application application = tempEntity.newApplication(organization.getId());
    final Policy policy = tempEntity.newPolicy(application);
    final PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID,
        "scan-1", new Date(System.currentTimeMillis()));

    // App has 3 violations, they have been both waived and fixed
    tempEntity.createWaivedAndFixedPolicyViolation(
        policyEvaluation,
        policy,
        new Date(),
        500L,
        3L);

    tempEntity.createWaivedAndFixedPolicyViolation(
        policyEvaluation,
        policy,
        new Date(),
        42L,
        48L);
    tempEntity.createWaivedAndFixedPolicyViolation(
        policyEvaluation,
        policy,
        new Date(),
        401L,
        533L);

    final long expectedMeanTimeToRemediate = Math.round((float) (3L + 42L + 401L) / 3);

    long result = dao.getMeanTimeToRemediate(84);

    assertThat(result).isEqualTo(expectedMeanTimeToRemediate);
  }

  @Test
  public void testGetMeanTimeToRemediate_shouldHandleViolationsStillOpenUsingPostgres() {
    doGetMeanTimeToRemediate_shouldHandleViolationsStillOpen();
  }

  private void doGetMeanTimeToRemediate_shouldHandleViolationsStillOpen() {
    // === Given wwe have 2 violations both still open
    final Application application = tempEntity.newApplication(organization.getId());
    final Policy policy = tempEntity.newPolicy(application);
    final PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID,
        "scan-1", new Date(System.currentTimeMillis()));

    tempEntity.newPolicyViolation(policyEvaluation, policy);
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    // === Then
    long result = dao.getMeanTimeToRemediate(84);

    assertThat(result).isEqualTo(0L);
  }

  @Test
  public void testDeleteFixedByOwnerIdAndDate_Postgres() {
    testDeleteFixedByOwnerIdAndDate(false);
  }

  private void testDeleteFixedByOwnerIdAndDate(boolean isDatabaseEmbedded) {
    assertThat(dao.isDatabaseEmbedded()).isEqualTo(isDatabaseEmbedded);

    Application app = tempEntity.newApplicationWithParent();

    Policy policy = tempEntity.newPolicy();
    PolicyEvaluation evaluation0 = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(),
        BuildStageType.ID, "scan-1", new Date(System.currentTimeMillis() - 900));
    PolicyViolation violation0 = tempEntity.newPolicyViolation(evaluation0, policy);
    violation0.setFixTime(evaluation0.getTime());
    dao.update(violation0);
    PolicyEvaluation evaluation1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan-1",
        new Date(System.currentTimeMillis() - 900));
    PolicyEvaluation evaluation2 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan-2",
        new Date(System.currentTimeMillis() - 500));
    for (int i = 0; i < PolicyViolationDAO.DELETE_BATCH_SIZE + 2; i++) {
      PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation1, policy);
      policyViolation.setFixTime(evaluation2.getTime());
      dao.update(policyViolation);
    }
    PolicyViolation violation1 = tempEntity.newPolicyViolation(evaluation2, policy);
    PolicyViolation violation2 = tempEntity.newPolicyViolation(evaluation2, policy);
    PolicyEvaluation evaluation3 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan-3");
    violation2.setFixTime(evaluation3.getTime());
    dao.update(violation2);

    int deletedRows = dao.deleteFixedByOwnerIdAndDate(app.getId(), evaluation3.getTime());

    assertThat(deletedRows).isEqualTo(PolicyViolationDAO.DELETE_BATCH_SIZE + 2);
    assertThat(dao.getByOwnerId(app.getId()))
        .usingElementComparator(Comparator.comparing(PolicyViolation::getId))
        .containsExactlyInAnyOrder(violation1, violation2);
    assertThat(dao.getById(violation0.getId())).isNotNull();
  }

  @Test
  public void testAllFieldsRoundTrip_insertAndUpdate_Postgres() {
    doTestAllFieldsRoundTrip_insertAndUpdate();
  }

  @Test
  public void testAllFieldsRoundTrip_insertBatchAndUpdateBatch_Postgres() {
    doTestAllFieldsRoundTrip_insertBatchAndUpdateBatch();
  }

  private void doTestAllFieldsRoundTrip_insertAndUpdate() {
    PolicyViolation violation = createFullyPopulatedViolation("scan-roundtrip");

    dao.insert(violation);
    assertAllFields(violation, loadWithConstraintFacts(violation.getId()));

    mutateViolation(violation);
    dao.update(violation);
    assertAllFields(violation, loadWithConstraintFacts(violation.getId()));
  }

  private void doTestAllFieldsRoundTrip_insertBatchAndUpdateBatch() {
    PolicyViolation violation = createFullyPopulatedViolation("scan-batch-rt");

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.insertBatch(tx, List.of(violation));
      tx.commit();
    }
    assertAllFields(violation, loadWithConstraintFacts(violation.getId()));

    mutateViolation(violation);
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.updateBatch(tx, List.of(violation));
      tx.commit();
    }
    assertAllFields(violation, loadWithConstraintFacts(violation.getId()));
  }

  private PolicyViolation createFullyPopulatedViolation(String scanId) {
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
    Date now = new Date();

    PolicyViolation violation = new PolicyViolation(eval, policy.getId(), policy.getName(), 7,
        PolicyThreatCategory.SECURITY, "aabbccddee",
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"),
        List.of(new ConstraintFact("cid", "cname", "cop")), "file.jar");
    violation.setActionTypeId(Action.ID_FAIL);
    violation.setWaiveTime(now);
    violation.setFixTime(now);
    violation.setLegacyViolationTime(now);
    violation.setPolicyWaiverId("waiver-id");
    violation.setPolicyWaiverComment("waiver comment");
    violation.setSeenByPrimaryEvaluation(true);
    violation.setSeenByMonitoringEvaluation(true);
    violation.setLegacyViolationApplied(true);
    violation.setReachabilityStatus(ReachabilityStatus.NON_REACHABLE);
    violation.setAutoPolicyWaiverId("auto-waiver-id");
    violation.setLastTelemetryEmittedDate(LocalDate.of(2026, 3, 15));
    return violation;
  }

  private void mutateViolation(PolicyViolation violation) {
    violation.setReachabilityStatus(ReachabilityStatus.REACHABLE);
    violation.setLegacyViolationApplied(false);
    violation.setSeenByMonitoringEvaluation(false);
    violation.setActionTypeId(Action.ID_WARN);
    violation.setLastTelemetryEmittedDate(LocalDate.of(2026, 3, 16));
  }

  private PolicyViolation loadWithConstraintFacts(String id) {
    PolicyViolation loaded = dao.getById(id);
    dao.loadConstraintFacts(List.of(loaded));
    return loaded;
  }

  @Test
  public void testInsertBatch_storesConstraintFacts_Postgres() {
    doTestInsertBatch_storesConstraintFacts();
  }

  @Test
  public void testUpdateBatch_storesConstraintFacts_Postgres() {
    doTestUpdateBatch_storesConstraintFacts();
  }

  private void doTestInsertBatch_storesConstraintFacts() {
    PolicyViolation violation = createFullyPopulatedViolation("scan-constraints");

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.insertBatch(tx, List.of(violation), false);
      tx.commit();
    }

    assertThat(violation.getConstraintFactsId()).isNotNull();
    PolicyViolationConstraintFacts facts = constraintFactsDAO.getById(violation.getConstraintFactsId());
    assertThat(facts).isNotNull();
    assertThat(facts.getConstraintFactsJson()).isNotBlank();
  }

  private void doTestUpdateBatch_storesConstraintFacts() {
    PolicyViolation violation = createFullyPopulatedViolation("scan-constraints-update");
    dao.insert(violation);
    String originalConstraintFactsId = violation.getConstraintFactsId();

    violation.setConstraintFacts(List.of(new ConstraintFact("new-cid", "new-cname", "new-cop")));
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.updateBatch(tx, List.of(violation));
      tx.commit();
    }

    assertThat(violation.getConstraintFactsId()).isNotNull();
    assertThat(violation.getConstraintFactsId()).isNotEqualTo(originalConstraintFactsId);
    PolicyViolationConstraintFacts facts = constraintFactsDAO.getById(violation.getConstraintFactsId());
    assertThat(facts).isNotNull();
    assertThat(facts.getConstraintFactsJson()).contains("new-cid");
  }

  @Test
  public void testInsertBatch_multipleViolations_storesConstraintsBatched_Postgres() {
    doTestInsertBatch_multipleViolations_storesConstraintsBatched();
  }

  private void doTestInsertBatch_multipleViolations_storesConstraintsBatched() {
    PolicyViolation v1 = createFullyPopulatedViolation("scan-batch-multi-1");
    PolicyViolation v2 = createFullyPopulatedViolation("scan-batch-multi-2");
    // v2 gets distinct constraint facts
    v2.setConstraintFacts(List.of(new ConstraintFact("cid-2", "cname-2", "cop-2")));

    // v3 shares the same constraint facts as v1 (tests dedup within the batch)
    PolicyViolation v3 = createFullyPopulatedViolation("scan-batch-multi-3");

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.insertBatch(tx, List.of(v1, v2, v3), false);
      tx.commit();
    }

    // All three should have constraint facts persisted
    assertThat(v1.getConstraintFactsId()).isNotNull();
    assertThat(v2.getConstraintFactsId()).isNotNull();
    assertThat(v3.getConstraintFactsId()).isNotNull();

    // v1 and v3 share the same constraint facts hash (same JSON)
    assertThat(v1.getConstraintFactsId()).isEqualTo(v3.getConstraintFactsId());
    assertThat(v1.getConstraintFactsId()).isNotEqualTo(v2.getConstraintFactsId());

    // Verify all persisted correctly
    assertThat(constraintFactsDAO.getById(v1.getConstraintFactsId())).isNotNull();
    assertThat(constraintFactsDAO.getById(v2.getConstraintFactsId())).isNotNull();
  }

  @Test
  public void testUpdateBatch_skipsAlreadyPersistedConstraints_Postgres() {
    doTestUpdateBatch_skipsAlreadyPersistedConstraints();
  }

  private void doTestUpdateBatch_skipsAlreadyPersistedConstraints() {
    // Insert two violations
    PolicyViolation v1 = createFullyPopulatedViolation("scan-batch-skip-1");
    PolicyViolation v2 = createFullyPopulatedViolation("scan-batch-skip-2");
    v2.setConstraintFacts(List.of(new ConstraintFact("skip-cid", "skip-cname", "skip-cop")));
    dao.insert(v1);
    dao.insert(v2);
    String v1OriginalId = v1.getConstraintFactsId();
    String v2OriginalId = v2.getConstraintFactsId();

    // Reload from DB without loading constraint facts (simulates the skip branch:
    // constraintFactsId is set but constraintFactsAreLoaded() returns false)
    PolicyViolation loaded1 = dao.getById(v1.getId());
    PolicyViolation loaded2 = dao.getById(v2.getId());
    assertThat(loaded1.constraintFactsAreLoaded()).isFalse();
    assertThat(loaded2.constraintFactsAreLoaded()).isFalse();

    // Mutate a non-constraint field and batch update
    loaded1.setActionTypeId(Action.ID_WARN);
    loaded2.setActionTypeId(Action.ID_WARN);
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.updateBatch(tx, List.of(loaded1, loaded2));
      tx.commit();
    }

    // Constraint facts ids should be unchanged (skip branch worked)
    assertThat(loaded1.getConstraintFactsId()).isEqualTo(v1OriginalId);
    assertThat(loaded2.getConstraintFactsId()).isEqualTo(v2OriginalId);

    // Verify the non-constraint field was actually updated
    assertThat(dao.getById(v1.getId()).getActionTypeId()).isEqualTo(Action.ID_WARN);
    assertThat(dao.getById(v2.getId()).getActionTypeId()).isEqualTo(Action.ID_WARN);
  }

  @Test
  public void testInsertBatch_throwsWhenConstraintFactsNotLoaded_Postgres() {
    doTestInsertBatch_throwsWhenConstraintFactsNotLoaded();
  }

  // On H2 the exception comes from the per-entity fallback (storeConstraints); on Postgres it comes
  // from storeConstraintsBatch. Both paths throw IllegalStateException for the same reason.
  private void doTestInsertBatch_throwsWhenConstraintFactsNotLoaded() {
    PolicyViolation violation = new PolicyViolation();

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      assertThatExceptionOfType(IllegalStateException.class)
          .isThrownBy(() -> dao.insertBatch(tx, List.of(violation), false));
    }
  }

  private void assertAllFields(PolicyViolation expected, PolicyViolation actual) {
    assertThat(actual).usingRecursiveComparison()
        .ignoringFields(
            "constraintFacts",
            "deprecatedConstraintFactsJson")
        .isEqualTo(expected);
    assertThat(actual.getConstraintFacts())
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .ignoringExpectedNullFields()
        .isEqualTo(expected.getConstraintFacts());
  }
}
