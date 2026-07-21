/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import com.sonatype.insight.brain.common.test.SlowTest;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO.ContainerImageInQuarantineData;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsForImageContainer;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsForImageContainerFilter;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsForImageContainerFilter.SortField;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsForImageContainerFilter.SortField.SortableField;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.containerimages.ContainerImagePolicyViolationSummaryDTO;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolationConstraintFacts;
import com.sonatype.insight.brain.model.policy.ReachabilityStatus;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.json.store.JsonUtils;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.PolicyViolation.POLICY_VIOLATION;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAOTest.Created.CREATED_AFTER_CUTOFF;
import static com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAOTest.Created.CREATED_BEFORE_CUTOFF;
import static com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAOTest.PolicyViolationState.FIXED;
import static com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAOTest.PolicyViolationState.LEGACY;
import static com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAOTest.PolicyViolationState.OPEN;
import static com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAOTest.PolicyViolationState.WAIVED;
import static com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAOTest.Resolved.RESOLVED_AFTER_CUTOFF;
import static com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAOTest.Resolved.RESOLVED_BEFORE_CUTOFF;
import static com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAOTest.Resolved.UNRESOLVED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Category(SlowTest.class)
public class PolicyViolationDAOTest
    extends AbstractDbDAOTest
{
  enum PolicyViolationState
  {
    OPEN,
    FIXED,
    WAIVED,
    LEGACY
  }

  enum Created
  {
    CREATED_BEFORE_CUTOFF,
    CREATED_AFTER_CUTOFF
  }

  enum Resolved
  {
    RESOLVED_BEFORE_CUTOFF,
    RESOLVED_AFTER_CUTOFF,
    UNRESOLVED
  }

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
  public void countUnfixedByThreatLevel_includesUnfixedWaivedAndLegacyRows() {
    newUnfixedViolation(application, BuildStageType.ID, 1);
    PolicyViolation waived = newUnfixedViolation(application, BuildStageType.ID, 3);
    waived.setWaiveTime(new Date());
    dao.update(waived);
    PolicyViolation legacy = newUnfixedViolation(application, BuildStageType.ID, 8);
    legacy.setLegacyViolationTime(new Date());
    dao.update(legacy);

    assertThat(dao.countUnfixedByThreatLevel(Set.of(application.getId()), null))
        .containsExactlyInAnyOrder(
            new PolicyViolationDAO.RawThreatLevelCount((short) 1, 1),
            new PolicyViolationDAO.RawThreatLevelCount((short) 3, 1),
            new PolicyViolationDAO.RawThreatLevelCount((short) 8, 1));
  }

  @Test
  public void countUnfixedByThreatLevel_excludesFixedRows() {
    newUnfixedViolation(application, BuildStageType.ID, 4);
    PolicyViolation fixed = newUnfixedViolation(application, BuildStageType.ID, 8);
    fixed.setFixTime(new Date());
    dao.update(fixed);

    assertThat(dao.countUnfixedByThreatLevel(Set.of(application.getId()), null))
        .containsExactly(new PolicyViolationDAO.RawThreatLevelCount((short) 4, 1));
  }

  @Test
  public void countUnfixedByThreatLevel_emptyApplicationsReturnsEmpty() {
    assertThat(dao.countUnfixedByThreatLevel(Set.of(), null)).isEmpty();
  }

  @Test
  public void countUnfixedByThreatLevel_nullApplicationsCountsGlobal() {
    newUnfixedViolation(application, BuildStageType.ID, 7);

    assertThat(dao.countUnfixedByThreatLevel(null, null))
        .contains(new PolicyViolationDAO.RawThreatLevelCount((short) 7, 1));
  }

  @Test
  public void countUnfixedByThreatLevel_optionalStagePredicateLimitsRows() {
    newUnfixedViolation(application, BuildStageType.ID, 3);
    newUnfixedViolation(application, ReleaseStageType.ID, 8);

    assertThat(dao.countUnfixedByThreatLevel(Set.of(application.getId()), Set.of(BuildStageType.ID)))
        .containsExactly(new PolicyViolationDAO.RawThreatLevelCount((short) 3, 1));
  }

  @Test
  public void countUnfixedByThreatLevel_mergesDisjointChunksByThreatLevel() {
    Application other = tempEntity.newApplication(organization.getId());
    newUnfixedViolation(application, BuildStageType.ID, 4);
    newUnfixedViolation(other, BuildStageType.ID, 4);
    PolicyViolationDAO chunkedDAO = org.mockito.Mockito.spy(dao);
    org.mockito.Mockito.when(chunkedDAO.getInOperatorThreshold()).thenReturn(2);

    assertThat(chunkedDAO.countUnfixedByThreatLevel(Set.of(application.getId(), other.getId(), "missing"), null))
        .containsExactly(new PolicyViolationDAO.RawThreatLevelCount((short) 4, 2));
  }

  @Test
  public void testCRUD() throws Exception {
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID,
        "PolicyViolationDAOTestScanId");

    // Create
    ConstraintFact constraintFact = new ConstraintFact("constraintdata", "constraintdata", "constraintdata");
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("Group1", "Artifact1",
        "Version1");
    PolicyViolation policyViolation = new PolicyViolation(policyEvaluation, policy.getId(), policy.getName(), 5,
        PolicyThreatCategory.LICENSE, "acacacacacac", componentIdentifier, List.of(constraintFact), "filename");
    assertThat(policyViolation.getId()).isNull();
    dao.insert(policyViolation);
    assertThat(policyViolation.getId()).isNotNull();
    // Restore constraint facts after insert since storeConstraints() clears them for memory optimization
    policyViolation.setConstraintFacts(List.of(constraintFact));

    // Test constraints stored
    assertThat(policyViolation.getConstraintFactsId()).isNotNull();
    PolicyViolationConstraintFacts facts = constraintFactsDAO.getById(policyViolation.getConstraintFactsId());
    ConstraintFact[] constraintFacts = JsonUtils.parse(facts.getConstraintFactsJson(), ConstraintFact[].class);
    assertThat(constraintFacts[0].getConstraintId()).isEqualTo(constraintFact.getConstraintId());

    // Read
    {
      PolicyViolation persistedPolicyViolation = dao.getById(policyViolation.getId());
      assertThat(persistedPolicyViolation).isNotNull();
      assertPolicyViolation(policyEvaluation.getApplicationId(), policyEvaluation.getStageTypeId(), policy.getId(),
          policy.getName(), 5, PolicyThreatCategory.LICENSE, "acacacacacac", componentIdentifier, "filename",
          policyEvaluation.getTime(), null /* actionTypeId */, persistedPolicyViolation);
      assertThatExceptionOfType(IllegalStateException.class)
          .isThrownBy(() -> persistedPolicyViolation.getConstraintFactsJson())
          .withMessageContaining("Constraint facts are not loaded yet for policyViolationId=");
    }

    // Update
    policyViolation.setActionTypeId(Action.ID_FAIL);
    dao.update(policyViolation);

    // Read
    PolicyViolation persistedPolicyViolation = dao.getById(policyViolation.getId());
    assertThat(policyViolation).isNotNull();
    assertPolicyViolation(policyEvaluation.getApplicationId(), policyEvaluation.getStageTypeId(), policy.getId(),
        policy.getName(), 5, PolicyThreatCategory.LICENSE, "acacacacacac", componentIdentifier, "filename",
        policyEvaluation.getTime(), Action.ID_FAIL, persistedPolicyViolation);

    // Delete
    dao.delete(policyViolation);

    policyViolation = dao.getById(policyViolation.getId());
    assertThat(policyViolation).isNull();
  }

  @Test
  public void testConsumePolicyViolationsSinceDate() throws Exception {
    // given: a number of policy violations with various dates and resolutions
    final Date cutoffDate = DateUtils.addDays(new Date(), -10);
    List<PolicyViolation> persistedPolicyViolations = createPolicyViolations(cutoffDate,
        new Object[][]{
          {OPEN, CREATED_BEFORE_CUTOFF, UNRESOLVED}, // yes
          {OPEN, CREATED_AFTER_CUTOFF, UNRESOLVED}, // yes
          {FIXED, CREATED_BEFORE_CUTOFF, RESOLVED_AFTER_CUTOFF}, // yes
          {FIXED, CREATED_AFTER_CUTOFF, RESOLVED_AFTER_CUTOFF}, // yes
          {FIXED, CREATED_BEFORE_CUTOFF, RESOLVED_BEFORE_CUTOFF}, // no - resolved before
          {WAIVED, CREATED_BEFORE_CUTOFF, RESOLVED_AFTER_CUTOFF}, // yes
          {WAIVED, CREATED_AFTER_CUTOFF, RESOLVED_AFTER_CUTOFF}, // yes
          {WAIVED, CREATED_BEFORE_CUTOFF, RESOLVED_BEFORE_CUTOFF}, // no - resolved before
          {LEGACY, CREATED_BEFORE_CUTOFF, RESOLVED_AFTER_CUTOFF}, // yes
          {LEGACY, CREATED_AFTER_CUTOFF, RESOLVED_AFTER_CUTOFF}, // yes
          {LEGACY, CREATED_BEFORE_CUTOFF, RESOLVED_BEFORE_CUTOFF}, // no - resolved before
        });

    final var batchSize = 5;

    // when: stream the historical violations
    var consumedPolicyViolations = new ArrayList<PolicyViolation>();
    AtomicBoolean terminalViolationSent = new AtomicBoolean(false);
    dao.consumePolicyViolationsSinceDate(cutoffDate, batchSize, policyViolation -> {
      if (null == policyViolation) {
        terminalViolationSent.set(true);
      }
      else {
        consumedPolicyViolations.add(policyViolation);
      }
    });

    // then: only the violations that are either still open or were opened or resolved after the cutoff date are
    // included
    assertThat(terminalViolationSent.get()).isTrue();
    assertThat(consumedPolicyViolations).hasSize(8);
    for (PolicyViolation violation : consumedPolicyViolations) {
      var openedBefore = violation.getOpenTime().before(cutoffDate) || violation.getOpenTime().equals(cutoffDate);
      var openedAfter = !openedBefore;
      var noFix = violation.getFixTime() == null;
      var fixedAfter = !noFix && violation.getFixTime().after(cutoffDate);
      var noWaiver = violation.getWaiveTime() == null;
      var waivedAfter = !noWaiver && violation.getWaiveTime().after(cutoffDate);
      var noLegacy = violation.getLegacyViolationTime() == null;
      var legacyAfter = !noLegacy && violation.getLegacyViolationTime().after(cutoffDate);

      var criteria =
          (openedBefore && noFix && noWaiver && noLegacy) || openedAfter || fixedAfter || waivedAfter || legacyAfter;
      assertThat(criteria).isTrue();
    }

    // and the policy violations are identical to persisted policy violations
    // and in the expected order (ignoring constraintFacts which are stored separately and not loaded by default)
    assertThat(consumedPolicyViolations.get(0)).usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .ignoringFields("deprecatedConstraintFactsJson", "constraintFacts")
        .isEqualTo(persistedPolicyViolations.get(0));
    assertThat(consumedPolicyViolations.get(1)).usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .ignoringFields("deprecatedConstraintFactsJson", "constraintFacts")
        .isEqualTo(persistedPolicyViolations.get(9));
    assertThat(consumedPolicyViolations.get(2)).usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .ignoringFields("deprecatedConstraintFactsJson", "constraintFacts")
        .isEqualTo(persistedPolicyViolations.get(1));
    assertThat(consumedPolicyViolations.get(3)).usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .ignoringFields("deprecatedConstraintFactsJson", "constraintFacts")
        .isEqualTo(persistedPolicyViolations.get(2));
    assertThat(consumedPolicyViolations.get(4)).usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .ignoringFields("deprecatedConstraintFactsJson", "constraintFacts")
        .isEqualTo(persistedPolicyViolations.get(3));
    assertThat(consumedPolicyViolations.get(5)).usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .ignoringFields("deprecatedConstraintFactsJson", "constraintFacts")
        .isEqualTo(persistedPolicyViolations.get(5));
    assertThat(consumedPolicyViolations.get(6)).usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .ignoringFields("deprecatedConstraintFactsJson", "constraintFacts")
        .isEqualTo(persistedPolicyViolations.get(6));
    assertThat(consumedPolicyViolations.get(7)).usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .ignoringFields("deprecatedConstraintFactsJson", "constraintFacts")
        .isEqualTo(persistedPolicyViolations.get(8));
  }

  @Test
  public void testConsumePolicyViolationsSinceDate_PolicyViolationWithoutEmbeddedConstraintFacts() throws Exception {
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), StageTypes.BUILD.getId(), "testScanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    // Sanity checks
    assertThat(policyViolation.getConstraintFactsId()).isNotNull();
    assertThat(policyViolation.getDeprecatedConstraintFactsJson()).isNull();

    int batchSize = 5;
    Date cutoffDate = DateUtils.addMilliseconds(new Date(), 1);

    // when: stream the historical violations
    List<PolicyViolation> violations = new ArrayList<>();
    AtomicBoolean terminalViolationSent = new AtomicBoolean(false);
    dao.consumePolicyViolationsSinceDate(cutoffDate, batchSize, consumedPolicyViolation -> {
      if (null == consumedPolicyViolation) {
        terminalViolationSent.set(true);
      }
      else {
        violations.add(consumedPolicyViolation);
      }
    });

    // then: the consumed policy violation is identical to the persisted policy violation
    assertThat(terminalViolationSent.get()).isTrue();
    assertThat(violations).hasSize(1);
    PolicyViolation consumedPolicyViolation = violations.get(0);
    assertThat(consumedPolicyViolation).usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .ignoringFields("deprecatedConstraintFactsJson")
        .isEqualTo(policyViolation);
  }

  private PolicyViolation createPolicyViolationWithEmbeddedConstraintFacts(
      PolicyEvaluation policyEvaluation,
      Policy policy)
  {
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    dao.loadConstraintFacts(List.of(policyViolation));
    // Sanity checks
    assertThat(policyViolation.getConstraintFactsId()).isNotNull();
    assertThat(policyViolation.getDeprecatedConstraintFactsJson()).isNull();

    String constraintFactsId = policyViolation.getConstraintFactsId();
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.dsl()
          .update(POLICY_VIOLATION)
          .setNull(POLICY_VIOLATION.CONSTRAINT_FACTS_ID)
          .set(POLICY_VIOLATION.CONSTRAINT_FACTS_JSON, policyViolation.getConstraintFactsJson())
          .where(POLICY_VIOLATION.POLICY_VIOLATION_ID.eq(policyViolation.getId()))
          .execute();
    }
    constraintFactsDAO.delete(constraintFactsDAO.getById(constraintFactsId));

    policyViolation = dao.getById(policyViolation.getId());
    assertThat(policyViolation.getConstraintFactsId()).isNull();
    assertThat(policyViolation.getDeprecatedConstraintFactsJson()).isNotNull();

    return policyViolation;
  }

  @Test
  public void testConsumePolicyViolationsSinceDate_PolicyViolationWithEmbeddedConstraintFacts() throws Exception {
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), StageTypes.BUILD.getId(), "testScanId");
    PolicyViolation policyViolation = createPolicyViolationWithEmbeddedConstraintFacts(policyEvaluation, policy);

    int batchSize = 5;
    Date cutoffDate = DateUtils.addMilliseconds(new Date(), 1);

    // when: stream the historical violations
    List<PolicyViolation> violations = new ArrayList<>();
    AtomicBoolean terminalViolationSent = new AtomicBoolean(false);
    dao.consumePolicyViolationsSinceDate(cutoffDate, batchSize, consumedPolicyViolation -> {
      if (null == consumedPolicyViolation) {
        terminalViolationSent.set(true);
      }
      else {
        violations.add(consumedPolicyViolation);
      }
    });

    // then: the consumed policy violation is identical to the persisted policy violation
    assertThat(terminalViolationSent.get()).isTrue();
    assertThat(violations).hasSize(1);
    PolicyViolation consumedPolicyViolation = violations.get(0);
    assertThat(consumedPolicyViolation).usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .ignoringFields("deprecatedConstraintFactsJson")
        .isEqualTo(policyViolation);
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetMeanTimeToRemediate_shouldCorrectlyComputeMeanTimeToRemediateMillisUsingPostgres() {
    doGetMeanTimeToRemediateShouldCorrectlyComputeMeanTimeToRemediateMillisUsingTest();
  }

  @Test
  public void testGetMeanTimeToRemediate_shouldCorrectlyComputeMeanTimeToRemediateMillisUsingH2() {
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
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetMeanTimeToRemediate_shouldUseLeastValueWhenWaivedAndFixedUsingPostges() {
    doGetMeanTimeToRemediate_shouldUseLeastValueWhenWaivedAndFixed();
  }

  @Test
  public void testGetMeanTimeToRemediate_shouldUseLeastValueWhenWaivedAndFixedUsingH2() {
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
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetMeanTimeToRemediate_shouldHandleViolationsStillOpenUsingPostgres() {
    doGetMeanTimeToRemediate_shouldHandleViolationsStillOpen();
  }

  @Test
  public void testGetMeanTimeToRemediateWithThreeMonthSlidingWindow_shouldHandleViolationsStillUsingOpenH2() {
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

  private void assertPolicyViolation(
      String applicationId,
      String stageTypeId,
      String policyId,
      String policyName,
      int threatLevel,
      PolicyThreatCategory threatCategory,
      String hash,
      ComponentIdentifier componentIdentifier,
      String filename,
      Date openTime,
      String actionTypeId,
      PolicyViolation actual)
  {
    assertThat(actual.getApplicationId()).isEqualTo(applicationId);
    assertThat(actual.getStageTypeId()).isEqualTo(stageTypeId);
    assertThat(actual.getPolicyId()).isEqualTo(policyId);
    assertThat(actual.getPolicyName()).isEqualTo(policyName);
    assertThat(actual.getThreatLevel()).isEqualTo(threatLevel);
    assertThat(actual.getThreatCategory()).isEqualTo(threatCategory);
    assertThat(actual.getHash()).isEqualTo(hash);
    assertThat(actual.getComponentIdentifier()).isEqualTo(componentIdentifier);
    assertThat(actual.getFilename()).isEqualTo(filename);
    assertThat(actual.getOpenTime()).isEqualTo(openTime);
    assertThat(actual.getActionTypeId()).isEqualTo(actionTypeId);
  }

  @Test
  public void testGetByApplicationId() {
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1",
        new Date(System.currentTimeMillis() - 2000));
    PolicyViolation openViolation1 = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-2");
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);
    PolicyViolation openViolation2 = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        tempEntity.newWaiver(policy.getId(), application.getId()));
    PolicyViolation legacyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    legacyViolation.setLegacyViolationTime(policyEvaluation.getTime());
    dao.update(legacyViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scan-3");
    PolicyViolation openViolation3 = tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-4");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getByApplicationId(application.getId());

    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openViolation1.getId(),
            openViolation2.getId(), openViolation3.getId(), fixedViolation.getId(), waivedViolation.getId(),
            legacyViolation.getId());
  }

  @Test
  public void testGetByApplicationIdAndPolicyAndHash() {
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1",
        new Date(System.currentTimeMillis() - 2000));
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID,
        "scan-2", new Date(System.currentTimeMillis() - 1000));

    PolicyViolation openViolation1 = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-3");
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);
    PolicyViolation openViolation2 = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        tempEntity.newWaiver(policy.getId(), application.getId()));
    PolicyViolation legacyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    legacyViolation.setLegacyViolationTime(policyEvaluation.getTime());
    dao.update(legacyViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scan-4");
    PolicyViolation openViolation3 = tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-4");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    tempEntity.newPolicyViolation(policyEvaluation2, tempEntity.newPolicy(application));
    tempEntity.newPolicyViolation(policyEvaluation2, tempEntity.newPolicy(application));

    List<PolicyViolation> violations = dao.getByApplicationIdAndPolicyIdAndHash(application.getId(),
        policy.getId(), openViolation1.getHash());

    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openViolation1.getId(),
            openViolation2.getId(), openViolation3.getId(), fixedViolation.getId(), waivedViolation.getId(),
            legacyViolation.getId());
  }

  @Test
  public void testGetByApplicationIdAndPolicyAndHash_NullHash() {
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID,
        "scan-1", new Date(System.currentTimeMillis() - 2000));
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID,
        "scan-2", new Date(System.currentTimeMillis() - 1000));

    PolicyViolation openViolation1 = tempEntity.newPolicyViolation(policyEvaluation, policy, null, null);
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy, null, null);
    // Another violation with different hash, for same policy and evaluation
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-3");
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);
    PolicyViolation openViolation2 = tempEntity.newPolicyViolation(policyEvaluation, policy, null, null);

    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy, null,
        null, tempEntity.newWaiver(policy.getId(), application.getId()));
    PolicyViolation legacyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy, null, null);
    legacyViolation.setLegacyViolationTime(policyEvaluation.getTime());
    dao.update(legacyViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scan-4");
    PolicyViolation openViolation3 = tempEntity.newPolicyViolation(policyEvaluation, policy, null, null);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-4");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    tempEntity.newPolicyViolation(policyEvaluation2, tempEntity.newPolicy(application));
    tempEntity.newPolicyViolation(policyEvaluation2, tempEntity.newPolicy(application));

    List<PolicyViolation> violations = dao.getByApplicationIdAndPolicyIdAndHash(application.getId(),
        policy.getId(), null);

    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openViolation1.getId(),
            openViolation2.getId(), openViolation3.getId(), fixedViolation.getId(), waivedViolation.getId(),
            legacyViolation.getId());
  }

  @Test
  public void testGetUnfixedByApplicationIdAndStageId() {
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1");
    PolicyViolation openViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        tempEntity.newWaiver(policy.getId(), application.getId()));
    PolicyViolation legacyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    legacyViolation.setLegacyViolationTime(policyEvaluation.getTime());
    dao.update(legacyViolation);
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-3");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getUnfixedByApplicationIdAndStageId(application.getId(), BuildStageType.ID);

    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openViolation.getId(),
            waivedViolation.getId(), legacyViolation.getId());
  }

  @Test
  public void testGetActiveByApplicationIdAndStageId() {
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1");
    PolicyViolation openViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        tempEntity.newWaiver(policy.getId(), application.getId()));
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);
    PolicyViolation legacyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    legacyViolation.setLegacyViolationTime(policyEvaluation.getTime());
    dao.update(legacyViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-3");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdAndStageId(application.getId(), BuildStageType.ID);

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(openViolation.getId());
  }

  @Test
  public void testActiveByApplicationIdAndStageIdAndActionId() {
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation1 =
        tempEntity.newPolicyEvaluation(application.getId(), ProxyStageType.ID, "scan-1");

    PolicyEvaluation policyEvaluation2 =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-2");

    PolicyViolation violation1 = tempEntity.newPolicyViolation(policyEvaluation1, policy);
    violation1.setActionTypeId(Action.ID_FAIL);
    dao.update(violation1);

    PolicyViolation violation2 = tempEntity.newPolicyViolation(policyEvaluation2, policy);
    violation2.setActionTypeId(Action.ID_FAIL);
    dao.update(violation2);

    PolicyViolation violation3 = tempEntity.newPolicyViolation(policyEvaluation1, policy);
    violation3.setActionTypeId(Action.ID_WARN);
    dao.update(violation3);

    tempEntity.newWaivedPolicyViolation(policyEvaluation1, policy,
        tempEntity.newWaiver(policy.getId(), application.getId()));

    tempEntity.newPolicyViolation(policyEvaluation1, policy);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdAndStageIdAndActionId(
        application.getId(), ProxyStageType.ID, Action.ID_FAIL);

    assertThat(violations).hasSize(1);

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(violation1.getId());
  }

  @Test
  public void testActiveByApplicationIdAndStageIdAndActionId_ProxyStage_IncludesAllLegacy() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(),
        ProxyStageType.ID, "scan-1");

    // High-severity legacy violation
    PolicyViolation highSeverityLegacy = tempEntity.newPolicyViolation(policyEvaluation, policy);
    highSeverityLegacy.setThreatLevel(10);
    highSeverityLegacy.setActionTypeId(Action.ID_FAIL);
    highSeverityLegacy.setLegacyViolationTime(new Date());
    dao.update(highSeverityLegacy);

    // Low-severity legacy violation
    PolicyViolation lowSeverityLegacy = tempEntity.newPolicyViolation(policyEvaluation, policy);
    lowSeverityLegacy.setThreatLevel(3);
    lowSeverityLegacy.setActionTypeId(Action.ID_FAIL);
    lowSeverityLegacy.setLegacyViolationTime(new Date());
    dao.update(lowSeverityLegacy);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdAndStageIdAndActionId(
        application.getId(), ProxyStageType.ID, Action.ID_FAIL);

    // Firewall includes ALL legacy violations regardless of severity
    assertThat(violations).hasSize(2);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(highSeverityLegacy.getId(), lowSeverityLegacy.getId());
  }

  @Test
  public void testActiveByApplicationIdAndStageIdAndActionId_ProxyStage_ExcludesFixedAndWaived() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(),
        ProxyStageType.ID, "scan-1");

    // Legacy violation (should be included)
    PolicyViolation legacyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    legacyViolation.setActionTypeId(Action.ID_FAIL);
    legacyViolation.setLegacyViolationTime(new Date());
    dao.update(legacyViolation);

    // Fixed legacy violation (should be excluded)
    PolicyViolation fixedLegacy = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedLegacy.setActionTypeId(Action.ID_FAIL);
    fixedLegacy.setLegacyViolationTime(new Date());
    fixedLegacy.setFixTime(new Date());
    dao.update(fixedLegacy);

    // Waived legacy violation (should be excluded)
    PolicyViolation waivedLegacy = tempEntity.newPolicyViolation(policyEvaluation, policy);
    waivedLegacy.setActionTypeId(Action.ID_FAIL);
    waivedLegacy.setLegacyViolationTime(new Date());
    waivedLegacy.setWaiveTime(new Date());
    dao.update(waivedLegacy);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdAndStageIdAndActionId(
        application.getId(), ProxyStageType.ID, Action.ID_FAIL);

    // Firewall only excludes fixed/waived, includes legacy
    assertThat(violations).hasSize(1);
    assertThat(violations.get(0).getId()).isEqualTo(legacyViolation.getId());
  }

  @Test
  public void testActiveByApplicationIdAndStageIdAndActionId_BuildStage_ExcludesAllLegacy() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(),
        BuildStageType.ID, "scan-1");

    // Legacy violation in build stage (Lifecycle)
    PolicyViolation legacyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    legacyViolation.setThreatLevel(10);
    legacyViolation.setActionTypeId(Action.ID_FAIL);
    legacyViolation.setLegacyViolationTime(new Date());
    dao.update(legacyViolation);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdAndStageIdAndActionId(
        application.getId(), BuildStageType.ID, Action.ID_FAIL);

    // Lifecycle excludes ALL legacy violations
    assertThat(violations).isEmpty();
  }

  @Test
  public void testGetActiveByApplicationIdAndStageId_ProxyStage_IncludesLegacy() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(),
        ProxyStageType.ID, "scan-1");

    // Create legacy violation
    PolicyViolation legacyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    legacyViolation.setThreatLevel(9);
    legacyViolation.setLegacyViolationTime(new Date());
    dao.update(legacyViolation);

    // Create regular violation
    PolicyViolation regularViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    regularViolation.setThreatLevel(8);
    dao.update(regularViolation);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdAndStageId(
        application.getId(), ProxyStageType.ID);

    // Firewall includes ALL violations (legacy and regular)
    assertThat(violations).hasSize(2);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(legacyViolation.getId(), regularViolation.getId());
  }

  @Test
  public void testGetActiveByApplicationIdAndStageId_BuildStage_ExcludesLegacy() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(),
        BuildStageType.ID, "scan-1");

    // Create legacy violation
    PolicyViolation legacyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    legacyViolation.setThreatLevel(9);
    legacyViolation.setLegacyViolationTime(new Date());
    dao.update(legacyViolation);

    // Create regular violation
    PolicyViolation regularViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    regularViolation.setThreatLevel(8);
    dao.update(regularViolation);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdAndStageId(
        application.getId(), BuildStageType.ID);

    // Lifecycle excludes legacy violations
    assertThat(violations).hasSize(1);
    assertThat(violations.get(0).getId()).isEqualTo(regularViolation.getId());
  }

  @Test
  public void testGetActiveByApplicationIdAndStageIdAndHash_ProxyStage_IncludesLegacy() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(),
        ProxyStageType.ID, "scan-1");

    String testHash = "test-hash-123";

    // Create legacy violation with specific hash
    PolicyViolation legacyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    legacyViolation.setHash(testHash);
    legacyViolation.setThreatLevel(9);
    legacyViolation.setLegacyViolationTime(new Date());
    dao.update(legacyViolation);

    // Create regular violation with same hash
    PolicyViolation regularViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    regularViolation.setHash(testHash);
    regularViolation.setThreatLevel(8);
    dao.update(regularViolation);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdAndStageIdAndHash(
        application.getId(), ProxyStageType.ID, testHash);

    // Firewall includes ALL violations for this hash (legacy and regular)
    assertThat(violations).hasSize(2);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(legacyViolation.getId(), regularViolation.getId());
  }

  @Test
  public void testGetActiveByApplicationIdAndStageIdAndHash_BuildStage_ExcludesLegacy() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(),
        BuildStageType.ID, "scan-1");

    String testHash = "test-hash-123";

    // Create legacy violation with specific hash
    PolicyViolation legacyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    legacyViolation.setHash(testHash);
    legacyViolation.setThreatLevel(9);
    legacyViolation.setLegacyViolationTime(new Date());
    dao.update(legacyViolation);

    // Create regular violation with same hash
    PolicyViolation regularViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    regularViolation.setHash(testHash);
    regularViolation.setThreatLevel(8);
    dao.update(regularViolation);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdAndStageIdAndHash(
        application.getId(), BuildStageType.ID, testHash);

    // Lifecycle excludes legacy violations
    assertThat(violations).hasSize(1);
    assertThat(violations.get(0).getId()).isEqualTo(regularViolation.getId());
  }

  @Test
  public void testGetActiveByApplicationIdAndStageIdAndHash() {
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1");
    PolicyViolation openViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        tempEntity.newWaiver(policy.getId(), application.getId()));
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);
    PolicyViolation legacyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    legacyViolation.setLegacyViolationTime(policyEvaluation.getTime());
    dao.update(legacyViolation);

    tempEntity.newPolicyViolation(policyEvaluation, policy, null, "other-hash", "reason");

    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-3");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations =
        dao.getActiveByApplicationIdAndStageIdAndHash(application.getId(), BuildStageType.ID,
            openViolation.getHash());

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(openViolation.getId());
  }

  @Test
  public void testGetActiveByApplicationIdsAndStageIdAndHashes() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1");

    PolicyViolation app1HashA = tempEntity.newPolicyViolation(eval1, policy);
    app1HashA.setHash("hash-a");
    dao.update(app1HashA);

    // fixed, waived and legacy violations for a requested hash must be excluded (build stage)
    PolicyViolation fixed = tempEntity.newPolicyViolation(eval1, policy);
    fixed.setHash("hash-a");
    fixed.setFixTime(eval1.getTime());
    dao.update(fixed);
    PolicyViolation waived =
        tempEntity.newWaivedPolicyViolation(eval1, policy, tempEntity.newWaiver(policy.getId(), application.getId()));
    waived.setHash("hash-a");
    dao.update(waived);
    PolicyViolation legacy = tempEntity.newPolicyViolation(eval1, policy);
    legacy.setHash("hash-a");
    legacy.setLegacyViolationTime(eval1.getTime());
    dao.update(legacy);

    // an active violation for a non-requested hash must be excluded
    PolicyViolation app1HashOther = tempEntity.newPolicyViolation(eval1, policy);
    app1HashOther.setHash("hash-other");
    dao.update(app1HashOther);

    // a second application with a requested hash
    Application application2 = tempEntity.newApplication(organization.getId());
    Policy policy2 = tempEntity.newPolicy(application2);
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID, "scan-2");
    PolicyViolation app2HashB = tempEntity.newPolicyViolation(eval2, policy2);
    app2HashB.setHash("hash-b");
    dao.update(app2HashB);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdsAndStageIdAndHashes(
        Set.of(application.getId(), application2.getId()), BuildStageType.ID, Set.of("hash-a", "hash-b"));

    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(app1HashA.getId(), app2HashB.getId());

    // empty inputs short-circuit to an empty result
    assertThat(dao.getActiveByApplicationIdsAndStageIdAndHashes(Collections.emptySet(), BuildStageType.ID,
        Set.of("hash-a"))).isEmpty();
    assertThat(dao.getActiveByApplicationIdsAndStageIdAndHashes(Set.of(application.getId()), BuildStageType.ID,
        Collections.emptySet())).isEmpty();
  }

  @Test
  public void testGetActiveByApplicationIdsAndStageIdAndHashes_ProxyStage_IncludesLegacy() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(),
        ProxyStageType.ID, "scan-1");

    String testHash = "test-hash-123";

    // Create legacy violation with specific hash
    PolicyViolation legacyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    legacyViolation.setHash(testHash);
    legacyViolation.setThreatLevel(9);
    legacyViolation.setLegacyViolationTime(new Date());
    dao.update(legacyViolation);

    // Create regular violation with same hash
    PolicyViolation regularViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    regularViolation.setHash(testHash);
    regularViolation.setThreatLevel(8);
    dao.update(regularViolation);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdsAndStageIdAndHashes(
        Set.of(application.getId()), ProxyStageType.ID, Set.of(testHash));

    // Firewall includes ALL violations for this hash (legacy and regular)
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(legacyViolation.getId(), regularViolation.getId());
  }

  @Test
  public void testGetActiveByApplicationIdsAndStageIdAndHashes_chunksAcrossPartitions() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1");
    PolicyViolation violation = tempEntity.newPolicyViolation(eval, policy);
    violation.setHash("real-hash");
    dao.update(violation);

    int threshold = dao.isDatabaseEmbedded()
        ? PolicyViolationDAO.H2_IN_OPERATOR_THRESHOLD
        : PolicyViolationDAO.POSTGRES_IN_OPERATOR_THRESHOLD;

    // Both the app-id and hash sets exceed the IN-clause threshold, forcing the nested chunking to span multiple
    // partitions. The combined bind-parameter budget must stay within limits and the results must merge correctly
    // across partitions (only the single real violation is returned).
    Set<String> manyAppIds = new HashSet<>();
    manyAppIds.add(application.getId());
    Set<String> manyHashes = new HashSet<>();
    manyHashes.add("real-hash");
    for (int i = 0; i <= threshold; i++) {
      manyAppIds.add("app-" + i);
      manyHashes.add("hash-" + i);
    }

    List<PolicyViolation> violations =
        dao.getActiveByApplicationIdsAndStageIdAndHashes(manyAppIds, BuildStageType.ID, manyHashes);

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(violation.getId());
  }

  @Test
  public void testGetUnfixedByApplicationIds() {
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1");
    PolicyViolation openViolation1 = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        tempEntity.newWaiver(policy.getId(), application.getId()));
    PolicyViolation legacyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    legacyViolation.setLegacyViolationTime(policyEvaluation.getTime());
    dao.update(legacyViolation);
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scan-2");
    PolicyViolation openViolation2 = tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-3");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getUnfixedByApplicationIds(Collections.singletonList(application.getId()));

    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openViolation1.getId(),
            openViolation2.getId(), waivedViolation.getId(), legacyViolation.getId());
  }

  @Test
  public void testGetActiveByApplicationIds() {
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1");
    PolicyViolation openViolation1 = tempEntity.newPolicyViolation(policyEvaluation, policy);
    tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        tempEntity.newWaiver(policy.getId(), application.getId()));
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);
    PolicyViolation legacyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    legacyViolation.setLegacyViolationTime(policyEvaluation.getTime());
    dao.update(legacyViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scan-2");
    PolicyViolation openViolation2 = tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-3");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getActiveByApplicationIds(Collections.singletonList(application.getId()));

    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openViolation1.getId(),
            openViolation2.getId());
  }

  @Test
  public void testGetUnfixedByApplicationIdsAndStageIds() {
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1");
    PolicyViolation openViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        tempEntity.newWaiver(policy.getId(), application.getId()));
    PolicyViolation legacyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    legacyViolation.setLegacyViolationTime(policyEvaluation.getTime());
    dao.update(legacyViolation);
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-3");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), null, null, null, null, null, null);

    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openViolation.getId(),
            waivedViolation.getId(), legacyViolation.getId());
  }

  @Test
  public void testGetUnfixedBy_threatLevel() {
    Policy policyFive = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan1");
    PolicyViolation openViolationFive = tempEntity.newPolicyViolation(policyEvaluation, policyFive);

    Policy policyThreatLevelZero = tempEntity.newPolicy(application.getId(), "Low", 0);
    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan3");
    PolicyViolation violationZero = tempEntity.newPolicyViolation(policyEvaluation, policyThreatLevelZero);

    Policy policyThreatLevelTen = tempEntity.newPolicy(application.getId(), "Critical", 10);
    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan2");
    PolicyViolation violationTen = tempEntity.newPolicyViolation(policyEvaluation, policyThreatLevelTen);

    List<PolicyViolation> violations;

    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), 4, 6, null, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId).containsExactly(openViolationFive.getId());

    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), 4, 5, null, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId).containsExactly(openViolationFive.getId());

    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), 5, 6, null, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId).containsExactly(openViolationFive.getId());

    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), 0, 10, null, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openViolationFive.getId(), violationZero.getId(), violationTen.getId());

    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), null, 10, null, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openViolationFive.getId(), violationZero.getId(), violationTen.getId());

    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), 0, null, null, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openViolationFive.getId(), violationZero.getId(), violationTen.getId());

    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), null, null, null, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openViolationFive.getId(), violationZero.getId(), violationTen.getId());

    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), 1, 10, null, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openViolationFive.getId(), violationTen.getId());

    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), 0, 9, null, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openViolationFive.getId(), violationZero.getId());

    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), 1, 9, null, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openViolationFive.getId());

    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), 0, 1, null, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(violationZero.getId());

    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), 9, 10, null, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(violationTen.getId());
  }

  @Test
  public void testGetUnfixedBy_threatCategory() {
    Policy security = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan1");
    PolicyViolation violationSecurity = tempEntity.newPolicyViolation(policyEvaluation, security);

    Policy license = tempEntity.newPolicy(application.getId(), "license",
        new Condition(LicenseConditionType.ID, "is not", "GPL-2.0"));
    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan2");
    PolicyViolation violationLicense = tempEntity.newPolicyViolation(policyEvaluation, license);

    Set<PolicyThreatCategory> policyThreatCategorySet = new HashSet<>();

    List<PolicyViolation> violations;
    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), null, null, null, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactly(violationSecurity.getId(), violationLicense.getId());

    policyThreatCategorySet.add(PolicyThreatCategory.SECURITY);
    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), null, null, policyThreatCategorySet, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId).containsExactly(violationSecurity.getId());

    policyThreatCategorySet.clear();
    policyThreatCategorySet.add(PolicyThreatCategory.LICENSE);
    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), null, null, policyThreatCategorySet, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId).containsExactly(violationLicense.getId());

    policyThreatCategorySet.clear();
    policyThreatCategorySet.add(PolicyThreatCategory.SECURITY);
    policyThreatCategorySet.add(PolicyThreatCategory.LICENSE);
    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), null, null, policyThreatCategorySet, null, null, null);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(violationLicense.getId(), violationSecurity.getId());

    policyThreatCategorySet.clear();
    policyThreatCategorySet.add(PolicyThreatCategory.QUALITY);
    policyThreatCategorySet.add(PolicyThreatCategory.OTHER);
    violations = dao.getUnfixedBy(Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), null, null, policyThreatCategorySet, null, null, null);
    assertThat(violations).isEmpty();
  }

  @Test
  public void testGetUnfixedByApplicationIdsAndStageIds_policyViolationState() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), application.getId());
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan1");

    PolicyViolation openPolicyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation waivedPolicyViolation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy, policyWaiver);
    PolicyViolation legacyViolationPolicyCount = tempEntity.newLegacyPolicyViolation(policyEvaluation, policy);

    List<String> applicationIds = Collections.singletonList(application.getId());
    List<String> stageTypeIds = Collections.singletonList(BuildStageType.ID);
    List<PolicyViolation> violations;

    // All violation states
    violations = dao.getUnfixedBy(applicationIds, stageTypeIds, null, null, null, true, true, true);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openPolicyViolation.getId(), waivedPolicyViolation.getId(),
            legacyViolationPolicyCount.getId());

    // None violation states (Equal to all)
    violations = dao.getUnfixedBy(applicationIds, stageTypeIds, null, null, null, false, false, false);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openPolicyViolation.getId(), waivedPolicyViolation.getId(),
            legacyViolationPolicyCount.getId());

    // Only Open
    violations = dao.getUnfixedBy(applicationIds, stageTypeIds, null, null, null, true, false, false);
    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(openPolicyViolation.getId());

    // Only Waived
    violations = dao.getUnfixedBy(applicationIds, stageTypeIds, null, null, null, false, true, false);
    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(waivedPolicyViolation.getId());

    // Only Legacy Violation
    violations = dao.getUnfixedBy(applicationIds, stageTypeIds, null, null, null, false, false, true);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(legacyViolationPolicyCount.getId());

    // Open and Waived
    violations = dao.getUnfixedBy(applicationIds, stageTypeIds, null, null, null, true, true, false);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openPolicyViolation.getId(),
            waivedPolicyViolation.getId());

    // Open and Legacy Violation
    violations = dao.getUnfixedBy(applicationIds, stageTypeIds, null, null, null, true, false, true);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openPolicyViolation.getId(),
            legacyViolationPolicyCount.getId());

    // Waived and Legacy Violation
    violations = dao.getUnfixedBy(applicationIds, stageTypeIds, null, null, null, false, true, true);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(waivedPolicyViolation.getId(),
            legacyViolationPolicyCount.getId());
  }

  @Test
  public void testGetActiveByApplicationIdsAndStageIds() {
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1");
    PolicyViolation openViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        tempEntity.newWaiver(policy.getId(), application.getId()));
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);
    PolicyViolation legacyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    legacyViolation.setLegacyViolationTime(policyEvaluation.getTime());
    dao.update(legacyViolation);

    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID,
        "scan-3");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdsAndStageIds(
        Collections.singletonList(application.getId()),
        Collections.singletonList(BuildStageType.ID), null, null, null);

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(openViolation.getId());
  }

  @Test
  public void testGetActiveByApplicationIdsAndPolicyIds() {
    Policy policy1 = tempEntity.newPolicy(application);
    Policy policy2 = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1");
    PolicyViolation openViolation = tempEntity.newPolicyViolation(policyEvaluation, policy1);
    tempEntity.newWaivedPolicyViolation(policyEvaluation, policy1,
        tempEntity.newWaiver(policy1.getId(), application.getId()));
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation, policy1);
    fixedViolation.setFixTime(policyEvaluation.getTime());
    dao.update(fixedViolation);
    PolicyViolation legacyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy1);
    legacyViolation.setLegacyViolationTime(policyEvaluation.getTime());
    dao.update(legacyViolation);
    // Open policy violation for a different policy
    tempEntity.newPolicyViolation(policyEvaluation, policy2);

    // Policy violation for a different application
    policyEvaluation =
        tempEntity.newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID, "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation, policy1);

    List<PolicyViolation> violations =
        dao.getActiveByApplicationIdsAndPolicyIds(Collections.singleton(application.getId()),
            Collections.singleton(policy1.getId()), null, null);

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(openViolation.getId());
  }

  @Test
  public void testGetActiveByApplicationIdsAndPolicyIds_BeforeDate() {
    String appId = application.getId();
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation eval;
    eval = tempEntity.newPolicyEvaluation(appId, BuildStageType.ID, "scan-" + TemporaryEntity.uuid(),
        asDate("2023-01-01"));
    String jan01 = tempEntity.newPolicyViolation(eval, policy).getId();

    eval = tempEntity.newPolicyEvaluation(appId, BuildStageType.ID, "scan-" + TemporaryEntity.uuid(),
        asDate("2023-01-15"));
    String jan15 = tempEntity.newPolicyViolation(eval, policy).getId();

    Set<String> app = Collections.singleton(appId);
    Set<String> policyId = Collections.singleton(policy.getId());

    // Test openTimeAfter and openTimeBefore both null
    List<PolicyViolation> result = dao.getActiveByApplicationIdsAndPolicyIds(app, policyId, null, null);
    assertThat(result).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(jan01, jan15);

    // Test openTimeAfter
    result = dao.getActiveByApplicationIdsAndPolicyIds(app, policyId, asDate("2023-01-01"), null);
    assertThat(result).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(jan01, jan15);

    result = dao.getActiveByApplicationIdsAndPolicyIds(app, policyId, asDate("2023-01-02"), null);
    assertThat(result).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(jan15);

    result = dao.getActiveByApplicationIdsAndPolicyIds(app, policyId, asDate("2023-01-15"), null);
    assertThat(result).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(jan15);

    result = dao.getActiveByApplicationIdsAndPolicyIds(app, policyId, asDate("2023-01-16"), null);
    assertThat(result).isEmpty();

    // Test openTimeBefore
    result = dao.getActiveByApplicationIdsAndPolicyIds(app, policyId, null, asDate("2023-01-15"));
    assertThat(result).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(jan01, jan15);

    result = dao.getActiveByApplicationIdsAndPolicyIds(app, policyId, null, asDate("2023-01-14"));
    assertThat(result).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(jan01);

    result = dao.getActiveByApplicationIdsAndPolicyIds(app, policyId, null, asDate("2023-01-01"));
    assertThat(result).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(jan01);

    result = dao.getActiveByApplicationIdsAndPolicyIds(app, policyId, null, asDate("2022-12-31"));
    assertThat(result).extracting(PolicyViolation::getId).isEmpty();

    // Test openTimeAfter AND openTimeBefore
    result = dao.getActiveByApplicationIdsAndPolicyIds(app, policyId, asDate("2023-01-01"), asDate("2023-01-15"));
    assertThat(result).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(jan01, jan15);

    result = dao.getActiveByApplicationIdsAndPolicyIds(app, policyId, asDate("2023-01-02"), asDate("2023-01-15"));
    assertThat(result).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(jan15);

    result = dao.getActiveByApplicationIdsAndPolicyIds(app, policyId, asDate("2023-01-01"), asDate("2023-01-14"));
    assertThat(result).extracting(PolicyViolation::getId).containsExactlyInAnyOrder(jan01);

    result = dao.getActiveByApplicationIdsAndPolicyIds(app, policyId, asDate("2023-01-02"), asDate("2023-01-14"));
    assertThat(result).isEmpty();
  }

  private Date asDate(String dateString) {
    return org.joda.time.Instant.parse(dateString).toDate();
  }

  private String addViolation(
      PolicyViolationDAO dao,
      String stageTypeId,
      Date openTime,
      Date legacyViolationTime,
      Date waiveTime,
      Date fixTime,
      int threatLevel,
      Condition condition)
  {
    Policy policy = tempEntity.newPolicy(application.getId(), ("" + UUID.randomUUID()).replace("-", ""), threatLevel);

    if (condition != null) {
      Constraint constraint = new Constraint();
      constraint.setConditions(Collections.singletonList(condition));
      policy.setConstraints(Collections.singletonList(constraint));
    }

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), stageTypeId,
        "scan-" + TemporaryEntity.uuid(), openTime);
    PolicyViolation violation;
    if (waiveTime != null) {
      violation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
          tempEntity.newWaiver(policy.getId(), application.getId()));
    }
    else {
      violation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    }
    violation.setLegacyViolationTime(legacyViolationTime);
    violation.setWaiveTime(waiveTime);
    violation.setFixTime(fixTime);
    dao.update(violation);

    return violation.getId();
  }

  private String addViolation(
      PolicyViolationDAO dao,
      String stageTypeId,
      Date openTime,
      Date legacyViolationTime,
      Date waiveTime,
      Date fixTime)
  {
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), stageTypeId,
        "scan-" + TemporaryEntity.uuid(), openTime);
    PolicyViolation violation;
    if (waiveTime != null) {
      violation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
          tempEntity.newWaiver(policy.getId(), application.getId()));
    }
    else {
      violation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    }
    violation.setLegacyViolationTime(legacyViolationTime);
    violation.setWaiveTime(waiveTime);
    violation.setFixTime(fixTime);
    dao.update(violation);

    return violation.getId();
  }

  @Test
  public void testGetActiveByApplicationIdsOpenedAfterDate() {
    Instant reference = Instant.now();
    Date openBefore = new Date(reference.minus(Duration.ofMinutes(1)).toEpochMilli());
    Date cutoff = new Date(reference.toEpochMilli());
    Date openAfter = new Date(reference.plus(Duration.ofMinutes(1)).toEpochMilli());
    Date legacyViolationTime = new Date(reference.plus(Duration.ofMinutes(2)).toEpochMilli());
    Date waiveTime = new Date(reference.plus(Duration.ofMinutes(3)).toEpochMilli());
    Date fixTime = new Date(reference.plus(Duration.ofMinutes(4)).toEpochMilli());
    Date notLegacyViolation = null;
    Date notWaived = null;
    Date notFixed = null;

    List<String> expectedIds = new ArrayList<>();

    expectedIds.add(addViolation(dao, BuildStageType.ID, cutoff, notLegacyViolation, notWaived, notFixed));
    expectedIds.add(addViolation(dao, BuildStageType.ID, openAfter, notLegacyViolation, notWaived, notFixed));
    expectedIds.add(addViolation(dao, ReleaseStageType.ID, openAfter, notLegacyViolation, notWaived, notFixed));

    addViolation(dao, BuildStageType.ID, cutoff, legacyViolationTime, notWaived, notFixed);
    addViolation(dao, BuildStageType.ID, cutoff, notLegacyViolation, notWaived, fixTime);
    addViolation(dao, BuildStageType.ID, cutoff, notLegacyViolation, waiveTime, notFixed);
    addViolation(dao, BuildStageType.ID, openAfter, legacyViolationTime, notWaived, notFixed);
    addViolation(dao, BuildStageType.ID, openAfter, notLegacyViolation, notWaived, fixTime);
    addViolation(dao, BuildStageType.ID, openAfter, notLegacyViolation, waiveTime, fixTime);
    addViolation(dao, BuildStageType.ID, openAfter, notLegacyViolation, waiveTime, notFixed);
    addViolation(dao, BuildStageType.ID, openBefore, notLegacyViolation, notWaived, notFixed);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdsOpenedAfterDate(
        Collections.singletonList(application.getId()),
        cutoff, null, null, null);

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrderElementsOf(expectedIds);
  }

  @Test
  public void testGetActiveByApplicationIdsOpenedAfterDate_ThreatLevel() {
    Instant reference = Instant.now();
    Date cutoff = new Date(reference.toEpochMilli());
    Date openAfter = new Date(reference.plus(Duration.ofMinutes(1)).toEpochMilli());
    Date legacyViolationTime = new Date(reference.plus(Duration.ofMinutes(2)).toEpochMilli());
    Date fixTime = new Date(reference.plus(Duration.ofMinutes(4)).toEpochMilli());

    List<String> expectedIds = new ArrayList<>();

    // matches by date and threat level
    expectedIds.add(addViolation(dao, BuildStageType.ID, cutoff, null, null, null, 10, null));
    // matches by date but not threat level
    addViolation(dao, BuildStageType.ID, openAfter, null, null, null, 2, null);
    // matches by threat level but not date
    addViolation(dao, BuildStageType.ID, cutoff, legacyViolationTime, null, null, 10, null);
    // matches neither by date nor by threat level
    addViolation(dao, BuildStageType.ID, cutoff, null, null, fixTime, 2, null);
    List<PolicyViolation> violations = dao.getActiveByApplicationIdsOpenedAfterDate(
        Collections.singletonList(application.getId()),
        cutoff, 8, 10, null);

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrderElementsOf(expectedIds);
  }

  @Test
  public void testGetActiveByApplicationIdsOpenedAfterDate_ThreatCategory() {
    Instant reference = Instant.now();
    Date cutoff = new Date(reference.toEpochMilli());
    Date openAfter = new Date(reference.plus(Duration.ofMinutes(1)).toEpochMilli());
    Date legacyViolationTime = new Date(reference.plus(Duration.ofMinutes(2)).toEpochMilli());
    Date fixTime = new Date(reference.plus(Duration.ofMinutes(4)).toEpochMilli());

    List<String> expectedIds = new ArrayList<>();

    Condition licenseCondition = new Condition(LicenseConditionType.ID, "is not", "GPL-2.0");

    // matches by date and threat category
    expectedIds.add(addViolation(dao, BuildStageType.ID, cutoff, null, null, null, 10, null));
    // matches by date but not threat category
    addViolation(dao, BuildStageType.ID, openAfter, null, null, null, 10, licenseCondition);
    // matches by threat category but not date
    addViolation(dao, BuildStageType.ID, cutoff, legacyViolationTime, null, null, 10, null);
    // matches neither by date nor by threat category
    addViolation(dao, BuildStageType.ID, cutoff, null, null, fixTime, 10, licenseCondition);

    Set<PolicyThreatCategory> policyThreatCategories = new HashSet<>();
    policyThreatCategories.add(PolicyThreatCategory.SECURITY);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdsOpenedAfterDate(
        Collections.singletonList(application.getId()),
        cutoff, null, null, policyThreatCategories);
    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrderElementsOf(expectedIds);
  }

  @Test
  public void getUnfixedByApplicationIdsOpenedAfterDate() {
    Instant reference = Instant.now();
    Date openBefore = new Date(reference.minus(Duration.ofMinutes(1)).toEpochMilli());
    Date cutoff = new Date(reference.toEpochMilli());
    Date openAfter = new Date(reference.plus(Duration.ofMinutes(1)).toEpochMilli());
    Date legacyViolationTime = new Date(reference.plus(Duration.ofMinutes(2)).toEpochMilli());
    Date waiveTime = new Date(reference.plus(Duration.ofMinutes(3)).toEpochMilli());
    Date fixTime = new Date(reference.plus(Duration.ofMinutes(4)).toEpochMilli());
    Date notLegacyViolation = null;
    Date notWaived = null;
    Date notFixed = null;

    List<String> expectedIds = new ArrayList<>();

    expectedIds.add(addViolation(dao, BuildStageType.ID, cutoff, legacyViolationTime, notWaived, notFixed));
    expectedIds.add(addViolation(dao, BuildStageType.ID, cutoff, notLegacyViolation, notWaived, notFixed));
    expectedIds.add(addViolation(dao, BuildStageType.ID, cutoff, notLegacyViolation, waiveTime, notFixed));
    expectedIds.add(addViolation(dao, BuildStageType.ID, openAfter, notLegacyViolation, notWaived, notFixed));
    expectedIds.add(addViolation(dao, BuildStageType.ID, openAfter, notLegacyViolation, waiveTime, notFixed));
    expectedIds.add(addViolation(dao, ReleaseStageType.ID, cutoff, legacyViolationTime, notWaived, notFixed));
    expectedIds.add(addViolation(dao, ReleaseStageType.ID, cutoff, notLegacyViolation, waiveTime, notFixed));
    expectedIds.add(addViolation(dao, ReleaseStageType.ID, openAfter, notLegacyViolation, notWaived, notFixed));
    expectedIds.add(addViolation(dao, ReleaseStageType.ID, openAfter, notLegacyViolation, waiveTime, notFixed));

    addViolation(dao, BuildStageType.ID, cutoff, legacyViolationTime, notWaived, fixTime);
    addViolation(dao, BuildStageType.ID, cutoff, notLegacyViolation, notWaived, fixTime);
    addViolation(dao, BuildStageType.ID, cutoff, notLegacyViolation, waiveTime, fixTime);
    addViolation(dao, BuildStageType.ID, openAfter, legacyViolationTime, notWaived, fixTime);
    addViolation(dao, BuildStageType.ID, openAfter, notLegacyViolation, notWaived, fixTime);
    addViolation(dao, BuildStageType.ID, openAfter, notLegacyViolation, waiveTime, fixTime);
    addViolation(dao, BuildStageType.ID, openBefore, notLegacyViolation, notWaived, notFixed);

    List<PolicyViolation> violations = dao.getUnfixedByApplicationIdsOpenedAfterDate(
        Collections.singletonList(application.getId()),
        cutoff, null, null, null);

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrderElementsOf(expectedIds);
  }

  @Test
  public void testGetActiveByApplicationIdsAndStageIdsOpenedAfterDate() {
    Instant reference = Instant.now();
    Date openBefore = new Date(reference.minus(Duration.ofMinutes(1)).toEpochMilli());
    Date cutoff = new Date(reference.toEpochMilli());
    Date openAfter = new Date(reference.plus(Duration.ofMinutes(1)).toEpochMilli());
    Date legacyViolationTime = new Date(reference.plus(Duration.ofMinutes(2)).toEpochMilli());
    Date waiveTime = new Date(reference.plus(Duration.ofMinutes(3)).toEpochMilli());
    Date fixTime = new Date(reference.plus(Duration.ofMinutes(4)).toEpochMilli());
    Date notLegacyViolation = null;
    Date notWaived = null;
    Date notFixed = null;

    List<String> expectedIds = new ArrayList<>();

    expectedIds.add(addViolation(dao, BuildStageType.ID, cutoff, notLegacyViolation, notWaived, notFixed));
    expectedIds.add(addViolation(dao, BuildStageType.ID, openAfter, notLegacyViolation, notWaived, notFixed));

    addViolation(dao, BuildStageType.ID, cutoff, legacyViolationTime, notWaived, notFixed);
    addViolation(dao, BuildStageType.ID, cutoff, notLegacyViolation, notWaived, fixTime);
    addViolation(dao, BuildStageType.ID, cutoff, notLegacyViolation, waiveTime, notFixed);
    addViolation(dao, BuildStageType.ID, openAfter, legacyViolationTime, notWaived, notFixed);
    addViolation(dao, BuildStageType.ID, openAfter, notLegacyViolation, notWaived, fixTime);
    addViolation(dao, BuildStageType.ID, openAfter, notLegacyViolation, waiveTime, fixTime);
    addViolation(dao, BuildStageType.ID, openAfter, notLegacyViolation, waiveTime, notFixed);
    addViolation(dao, BuildStageType.ID, openBefore, notLegacyViolation, notWaived, notFixed);
    addViolation(dao, ReleaseStageType.ID, openAfter, notLegacyViolation, notWaived, notFixed);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdsAndStageIdsOpenedAfterDate(
        Collections.singletonList(application.getId()), Collections.singletonList(BuildStageType.ID), cutoff, null,
        null, null);

    assertThat(violations).extracting(PolicyViolation::getId).containsExactlyInAnyOrderElementsOf(expectedIds);
  }

  @Test
  public void testGetActiveByApplicationIdAndStageIdsAndTimeRange() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver waiver = tempEntity.newWaiver(policy.getId(), application.getId());
    Date to = new Date(System.currentTimeMillis() - 10 * 1000);
    Date from = new Date(to.getTime() - 60 * 1000);

    Date before = new Date(from.getTime() - 1000);
    Date during1 = new Date(from.getTime() + 1000);
    Date during2 = new Date(from.getTime() + 2000);

    PolicyEvaluation policyEvalBeforeDateRange = tempEntity
        .newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-0", before);
    // waived before time range
    tempEntity.newWaivedPolicyViolation(policyEvalBeforeDateRange, policy, waiver);
    // fixed before time range
    PolicyViolation fixedBefore = tempEntity.newPolicyViolation(policyEvalBeforeDateRange, policy);
    fixedBefore.setFixTime(before);
    dao.update(fixedBefore);
    // legacy violation before time range
    PolicyViolation legacyViolationBefore = tempEntity.newPolicyViolation(policyEvalBeforeDateRange, policy);
    legacyViolationBefore.setFixTime(before);
    dao.update(legacyViolationBefore);

    // opened before time range and still unresolved
    PolicyViolation openedBefore = tempEntity.newPolicyViolation(policyEvalBeforeDateRange, policy);
    // opened before time range and waived during time range
    PolicyViolation openedBeforeWaivedDuring = tempEntity.newPolicyViolation(policyEvalBeforeDateRange, policy);
    openedBeforeWaivedDuring.setWaiveTime(during1);
    dao.update(openedBeforeWaivedDuring);
    // opened before time range and fixed during time range
    PolicyViolation openedBeforeFixedDuring = tempEntity.newPolicyViolation(policyEvalBeforeDateRange, policy);
    openedBeforeFixedDuring.setFixTime(during1);
    dao.update(openedBeforeFixedDuring);
    // opened before time range and legacy violation during time range
    PolicyViolation openedBeforeLegacyViolationDuring = tempEntity
        .newPolicyViolation(policyEvalBeforeDateRange, policy);
    openedBeforeLegacyViolationDuring.setLegacyViolationTime(during1);
    dao.update(openedBeforeLegacyViolationDuring);

    PolicyEvaluation policyEvalOnStartDateRange = tempEntity
        .newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1", from);
    // opened during time range and still unresolved
    PolicyViolation openedDuring = tempEntity.newPolicyViolation(policyEvalOnStartDateRange, policy);
    // opened during time range but immediately waived
    tempEntity.newWaivedPolicyViolation(policyEvalOnStartDateRange, policy, waiver);
    // opened during time range and waived after time range
    PolicyViolation openedDuringWaivedAfter = tempEntity.newPolicyViolation(policyEvalOnStartDateRange, policy);
    openedDuringWaivedAfter.setWaiveTime(to);
    dao.update(openedDuringWaivedAfter);
    // opened during time range and fixed after time range
    PolicyViolation openedDuringFixedAfter = tempEntity.newPolicyViolation(policyEvalOnStartDateRange, policy);
    openedDuringFixedAfter.setFixTime(to);
    dao.update(openedDuringFixedAfter);
    // opened during time range and legacy violation after time range
    PolicyViolation openedDuringLegacyViolationAfter = tempEntity
        .newPolicyViolation(policyEvalOnStartDateRange, policy);
    openedDuringLegacyViolationAfter.setLegacyViolationTime(to);
    dao.update(openedDuringLegacyViolationAfter);

    PolicyEvaluation policyEvalInDateRange = tempEntity
        .newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-2", during1);
    // opened and waived during time range
    PolicyViolation openedAndWaivedDuring = tempEntity.newPolicyViolation(policyEvalInDateRange, policy);
    openedAndWaivedDuring.setWaiveTime(during2);
    dao.update(openedAndWaivedDuring);
    // opened and fixed during time range
    PolicyViolation openedAndFixedDuring = tempEntity.newPolicyViolation(policyEvalInDateRange, policy);
    openedAndFixedDuring.setFixTime(during2);
    dao.update(openedAndFixedDuring);
    // opened and legacy violation during time range
    PolicyViolation openedAndLegacyViolationDuring = tempEntity.newPolicyViolation(policyEvalInDateRange, policy);
    openedAndLegacyViolationDuring.setLegacyViolationTime(during2);
    dao.update(openedAndLegacyViolationDuring);

    PolicyEvaluation policyEvalOnEndDateRange = tempEntity
        .newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-3", to);
    // opened after time range
    tempEntity.newPolicyViolation(policyEvalOnEndDateRange, policy);

    PolicyEvaluation policyEvalOnStartDateRangeOtherStage = tempEntity
        .newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scan-os", from);
    // matching app and time range but wrong stage
    tempEntity.newPolicyViolation(policyEvalOnStartDateRangeOtherStage, policy);

    PolicyEvaluation policyEvalOnStartDateRangeOtherApp = tempEntity
        .newPolicyEvaluation(tempEntity.newApplicationWithParent().getId(), BuildStageType.ID, "scan-oa", from);
    // matching stage and time range but wrong app
    tempEntity.newPolicyViolation(policyEvalOnStartDateRangeOtherApp, policy);

    List<PolicyViolation> violations = dao.getActiveByApplicationIdAndStageIdsAndTimeRange(application.getId(),
        Collections.singletonList(BuildStageType.ID), from, to);

    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openedBefore.getId(),
            openedBeforeWaivedDuring.getId(), openedBeforeFixedDuring.getId(),
            openedBeforeLegacyViolationDuring.getId(),
            openedDuring.getId(), openedDuringWaivedAfter.getId(), openedDuringFixedAfter.getId(),
            openedDuringLegacyViolationAfter.getId(), openedAndWaivedDuring.getId(), openedAndFixedDuring.getId(),
            openedAndLegacyViolationDuring.getId());
  }

  @Test
  public void testgetUnfixedLegacyViolationByApplicationId() {
    Policy policy1 = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation1 =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1");
    tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    PolicyViolation unfixedLegacyViolation1 = tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    unfixedLegacyViolation1.setLegacyViolationTime(policyEvaluation1.getTime());
    dao.update(unfixedLegacyViolation1);
    PolicyViolation fixedLegacyViolation1 = tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    fixedLegacyViolation1.setFixTime(new Date());
    fixedLegacyViolation1.setLegacyViolationTime(new Date());
    dao.update(fixedLegacyViolation1);

    Application application2 = tempEntity.newApplicationWithParent();
    Policy policy2 = tempEntity.newPolicy(application2);
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID,
        "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation2, policy2);
    PolicyViolation unfixedLegacyViolation2 = tempEntity.newPolicyViolation(policyEvaluation2, policy2);
    unfixedLegacyViolation2.setLegacyViolationTime(new Date());
    dao.update(unfixedLegacyViolation2);
    PolicyViolation fixedLegacyViolation2 = tempEntity.newPolicyViolation(policyEvaluation2, policy2);
    fixedLegacyViolation2.setFixTime(new Date());
    fixedLegacyViolation2.setLegacyViolationTime(new Date());
    dao.update(fixedLegacyViolation2);

    try (TransactionContext tx = dao.createTransactionContext()) {
      List<PolicyViolation> violations1 = dao.getUnfixedLegacyViolationByApplicationId(tx, application.getId());
      assertThat(violations1).extracting(PolicyViolation::getId)
          .containsExactly(unfixedLegacyViolation1.getId());

      List<PolicyViolation> violations2 = dao.getUnfixedLegacyViolationByApplicationId(tx, application2.getId());
      assertThat(violations2).extracting(PolicyViolation::getId)
          .containsExactly(unfixedLegacyViolation2.getId());
    }
  }

  @Test
  public void testgetUnfixedLegacyViolationByApplicationId_stringOverload() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation evaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-str-overload");
    PolicyViolation unfixed = tempEntity.newPolicyViolation(evaluation, policy);
    unfixed.setLegacyViolationTime(evaluation.getTime());
    dao.update(unfixed);

    // A second application's unfixed legacy violation must NOT leak into the first application's
    // result — proves the string overload scopes by application id and does not return other apps' rows.
    Application application2 = tempEntity.newApplicationWithParent();
    Policy policy2 = tempEntity.newPolicy(application2);
    PolicyEvaluation evaluation2 =
        tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID, "scan-str-overload-2");
    PolicyViolation unfixed2 = tempEntity.newPolicyViolation(evaluation2, policy2);
    unfixed2.setLegacyViolationTime(evaluation2.getTime());
    dao.update(unfixed2);

    List<PolicyViolation> violations = dao.getUnfixedLegacyViolationByApplicationId(application.getId());
    assertThat(violations).extracting(PolicyViolation::getId).containsExactly(unfixed.getId());

    List<PolicyViolation> violations2 = dao.getUnfixedLegacyViolationByApplicationId(application2.getId());
    assertThat(violations2).extracting(PolicyViolation::getId).containsExactly(unfixed2.getId());
  }

  @Test
  public void testGetUnfixedByApplicationId() {
    Policy policy1 = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation1 =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1");
    PolicyViolation unfixedViolation1 = tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    PolicyViolation fixedViolation1 = tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    fixedViolation1.setFixTime(new Date());
    dao.update(fixedViolation1);

    Application application2 = tempEntity.newApplicationWithParent();
    Policy policy2 = tempEntity.newPolicy(application2);
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID,
        "scan-2");
    PolicyViolation unfixedViolation2 = tempEntity.newPolicyViolation(policyEvaluation2, policy2);
    PolicyViolation fixedViolation2 = tempEntity.newPolicyViolation(policyEvaluation2, policy2);
    fixedViolation2.setFixTime(new Date());
    dao.update(fixedViolation2);

    try (TransactionContext tx = dao.createTransactionContext()) {
      List<PolicyViolation> violations1 = dao.getUnfixedByApplicationId(tx, application.getId());
      assertThat(violations1).extracting(PolicyViolation::getId).containsExactly(unfixedViolation1.getId());

      List<PolicyViolation> violations2 = dao.getUnfixedByApplicationId(tx, application2.getId());
      assertThat(violations2).extracting(PolicyViolation::getId).containsExactly(unfixedViolation2.getId());
    }
  }

  @Test
  public void testReplacePolicyId() {
    Policy fromPolicy = tempEntity.newPolicy(application);
    Policy toPolicy = tempEntity.newPolicy(application);
    Policy otherPolicy = tempEntity.newPolicy(application);
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scanId");
    PolicyViolation fromPolicyViolation = tempEntity.newPolicyViolation(evaluation, fromPolicy);
    PolicyViolation toPolicyViolation = tempEntity.newPolicyViolation(evaluation, toPolicy);
    PolicyViolation otherPolicyViolation = tempEntity.newPolicyViolation(evaluation, otherPolicy);

    dao.replacePolicyId(fromPolicy.getId(), toPolicy.getId());

    fromPolicyViolation = dao.getById(fromPolicyViolation.getId());
    assertThat(fromPolicyViolation.getPolicyId()).isEqualTo(toPolicy.getId());
    toPolicyViolation = dao.getById(toPolicyViolation.getId());
    assertThat(toPolicyViolation.getPolicyId()).isEqualTo(toPolicy.getId());
    otherPolicyViolation = dao.getById(otherPolicyViolation.getId());
    assertThat(otherPolicyViolation.getPolicyId()).isEqualTo(otherPolicy.getId());
  }

  @Test
  public void testReplacePolicyIdForApplication() {
    Policy fromPolicy = tempEntity.newPolicy(organization);
    Policy toPolicy = tempEntity.newPolicy(application);
    Policy otherPolicy = tempEntity.newPolicy(application);
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scanId");
    PolicyViolation fromPolicyViolation = tempEntity.newPolicyViolation(evaluation, fromPolicy);
    PolicyViolation otherPolicyViolation = tempEntity.newPolicyViolation(evaluation, otherPolicy);
    PolicyEvaluation otherAppEvaluation = tempEntity.newPolicyEvaluation(tempEntity
        .newApplication(organization.getId())
        .getId(), BuildStageType.ID, "scanId");
    PolicyViolation otherAppPolicyViolation = tempEntity.newPolicyViolation(otherAppEvaluation, fromPolicy);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.replacePolicyId(tx, application.getId(), fromPolicy.getId(), toPolicy.getId());
      tx.commit();
    }

    fromPolicyViolation = dao.getById(fromPolicyViolation.getId());
    assertThat(fromPolicyViolation.getPolicyId()).isEqualTo(toPolicy.getId());
    otherPolicyViolation = dao.getById(otherPolicyViolation.getId());
    assertThat(otherPolicyViolation.getPolicyId()).isEqualTo(otherPolicy.getId());
    otherAppPolicyViolation = dao.getById(otherAppPolicyViolation.getId());
    assertThat(otherAppPolicyViolation.getPolicyId()).isEqualTo(fromPolicy.getId());
  }

  @Test
  public void testDeleteFixedByApplicationIdAndDate_H2() {
    testDeleteFixedByApplicationIdAndDate(true);
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testDeleteFixedByApplicationIdAndDate_Postgres() {
    testDeleteFixedByApplicationIdAndDate(false);
  }

  private void testDeleteFixedByApplicationIdAndDate(boolean isDatabaseEmbedded) {
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

    int deletedRows = dao.deleteFixedByApplicationIdAndDate(app.getId(), evaluation3.getTime());

    assertThat(deletedRows).isEqualTo(PolicyViolationDAO.DELETE_BATCH_SIZE + 2);
    assertThat(dao.getByApplicationId(app.getId()))
        .usingElementComparator(Comparator.comparing(PolicyViolation::getId))
        .containsExactlyInAnyOrder(violation1, violation2);
    assertThat(dao.getById(violation0.getId())).isNotNull();
  }

  @Test
  public void testGetCount() {
    Policy policy = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    assertThat(dao.getCount()).isEqualTo(2);
  }

  @Test
  public void testGetCountApplicationsWithPolicyActionFailures_DoNotCountAppWithoutFailPolicyActions() {
    final Application application2 = tempEntity.newApplication(organization.getId());
    final Application application3 = tempEntity.newApplication(organization.getId());

    final Policy policy1 = tempEntity.newPolicy(application);
    final Policy policy2 = tempEntity.newPolicy(application2);
    final Policy policy3 = tempEntity.newPolicy(application3);

    // Application 1 evaluations
    final PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID,
        "scan-1", new Date(System.currentTimeMillis()));
    // Null policy action ID
    tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    // Warn policy action
    tempEntity.newPolicyViolation(policyEvaluation1, policy1, 6, PolicyThreatCategory.OTHER, "test-group-id",
        "test-artifact-id", "v1", "test-hash", WarnActionType.ID);

    // Application 2 evaluations
    final PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID,
        "scan-1", new Date(System.currentTimeMillis()));
    // Fail policy action
    tempEntity.newPolicyViolation(policyEvaluation2, policy2, 9, PolicyThreatCategory.OTHER, "test-group-id",
        "test-artifact-id", "v1", "test-hash", FailActionType.ID);
    // Fail policy action
    tempEntity.newPolicyViolation(policyEvaluation2, policy2, 10, PolicyThreatCategory.QUALITY, "test-group-id",
        "test-artifact-id", "v1", "test-hash", FailActionType.ID);

    // Application 3 evaluations
    final PolicyEvaluation policyEvaluation3 = tempEntity.newPolicyEvaluation(application3.getId(), BuildStageType.ID,
        "scan-1", new Date(System.currentTimeMillis()));
    // Fail policy action
    tempEntity.newPolicyViolation(policyEvaluation3, policy3, 10, PolicyThreatCategory.OTHER, "test-group-id",
        "test-artifact-id", "v1", "test-hash", FailActionType.ID);
    // Null policy action ID
    tempEntity.newPolicyViolation(policyEvaluation3, policy3);

    final int numAppsWithPolicyActionFailures = dao.getCountApplicationsWithPolicyActionFailures(Stage.ID_BUILD);

    assertThat(numAppsWithPolicyActionFailures)
        .isEqualTo(2);
  }

  @Test
  public void testGetCountApplicationsWithPolicyActionFailures_DoNotCountAppsWithWaivedViolations() {
    final Application application2 = tempEntity.newApplication(organization.getId());

    final Policy policy1 = tempEntity.newPolicy(application);
    final Policy policy2 = tempEntity.newPolicy(application2);

    // Application 1 evaluations
    final PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID,
        "scan-1", new Date(System.currentTimeMillis()));
    // Null policy action ID
    tempEntity.newPolicyViolation(policyEvaluation1, policy1);

    // Application 2 evaluations
    final PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID,
        "scan-1", new Date(System.currentTimeMillis()));
    // Fail policy action but it is waived
    final PolicyViolation waivedViolation = tempEntity.newPolicyViolation(policyEvaluation2, policy2, 9,
        PolicyThreatCategory.OTHER, "test-group-id", "test-artifact-id", "v1", "test-hash", FailActionType.ID);
    waivedViolation.setWaiveTime(new Date(System.currentTimeMillis()));
    dao.update(waivedViolation);

    final int numAppsWithPolicyActionFailures = dao.getCountApplicationsWithPolicyActionFailures(Stage.ID_BUILD);

    assertThat(numAppsWithPolicyActionFailures)
        .isZero();
  }

  @Test
  public void testGetCountApplicationsWithPolicyActionFailures_DoNotCountAppsWithFixedViolations() {
    final Application application2 = tempEntity.newApplication(organization.getId());

    final Policy policy1 = tempEntity.newPolicy(application);
    final Policy policy2 = tempEntity.newPolicy(application2);

    // Application 1 evaluations
    final PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID,
        "scan-1", new Date(System.currentTimeMillis()));
    // Null policy action ID
    tempEntity.newPolicyViolation(policyEvaluation1, policy1);

    // Application 2 evaluations
    final PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID,
        "scan-1", new Date(System.currentTimeMillis()));
    // Fail policy action but it is fixed
    final PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation2, policy2, 9,
        PolicyThreatCategory.OTHER, "test-group-id", "test-artifact-id", "v1", "test-hash", FailActionType.ID);
    fixedViolation.setFixTime(new Date(System.currentTimeMillis()));
    dao.update(fixedViolation);

    final int numAppsWithPolicyActionFailures = dao.getCountApplicationsWithPolicyActionFailures(Stage.ID_BUILD);

    assertThat(numAppsWithPolicyActionFailures)
        .isZero();
  }

  @Test
  public void testGetCountApplicationsWithPolicyActionFailures_CountAppsByCorrectStage() {
    final Application application2 = tempEntity.newApplication(organization.getId());

    final Policy policy1 = tempEntity.newPolicy(application);
    final Policy policy2 = tempEntity.newPolicy(application2);

    // Application 1 evaluations
    final PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID,
        "scan-1", new Date(System.currentTimeMillis()));
    // Fail policy action for eval at build stage
    tempEntity.newPolicyViolation(policyEvaluation1, policy1, 9, PolicyThreatCategory.OTHER, "test-group-id",
        "test-artifact-id", "v1", "test-hash", FailActionType.ID);

    // Application 2 evaluations
    final PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(application2.getId(), ReleaseStageType.ID,
        "scan-1", new Date(System.currentTimeMillis()));
    final PolicyEvaluation policyEvaluation3 = tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID,
        "scan-1", new Date(System.currentTimeMillis()));
    // Fail policy action for eval at release stage
    tempEntity.newPolicyViolation(policyEvaluation2, policy2, 9, PolicyThreatCategory.OTHER, "test-group-id",
        "test-artifact-id", "v1", "test-hash", FailActionType.ID);
    // Fail policy action for eval at build stage
    tempEntity.newPolicyViolation(policyEvaluation3, policy1, 9, PolicyThreatCategory.OTHER, "test-group-id",
        "test-artifact-id", "v1", "test-hash", FailActionType.ID);

    final int numAppsWithPolicyActionFailuresBuildStage =
        dao.getCountApplicationsWithPolicyActionFailures(Stage.ID_BUILD);
    assertThat(numAppsWithPolicyActionFailuresBuildStage)
        .isEqualTo(2);

    final int numAppsWithPolicyActionFailuresReleaseStage =
        dao.getCountApplicationsWithPolicyActionFailures(Stage.ID_RELEASE);
    assertThat(numAppsWithPolicyActionFailuresReleaseStage)
        .isEqualTo(1);
  }

  @Test
  public void testGetCountActiveWaivers_OnlyCountViolationsWithActiveWaivers() {
    final Application application2 = tempEntity.newApplication(organization.getId());

    final Policy policy1 = tempEntity.newPolicy(application);
    final Policy policy2 = tempEntity.newPolicy(application2);

    // Application 1 evaluations
    final PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID,
        "scan-1", new Date(System.currentTimeMillis()));
    // Violations with active waiver
    final PolicyWaiver policyWaiver1 = tempEntity.newWaiver(policy1.getId(), application.getId());
    tempEntity.newWaivedPolicyViolation(policyEvaluation1, policy1, policyWaiver1);
    tempEntity.newWaivedPolicyViolation(policyEvaluation1, policy1, policyWaiver1);

    // Application 2 evaluations
    final PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(application2.getId(), ReleaseStageType.ID,
        "scan-1", new Date(System.currentTimeMillis()));
    // Violation with expired waiver
    final PolicyWaiver policyWaiver2 = tempEntity.newWaiver(policy2.getId(), application2.getId());
    final PolicyViolation expiredWaivedViolation =
        tempEntity.newWaivedPolicyViolation(policyEvaluation2, policy2, policyWaiver2);
    expiredWaivedViolation.setFixTime(new Date(System.currentTimeMillis()));
    dao.update(expiredWaivedViolation);
    // Violation with active waiver
    tempEntity.newWaivedPolicyViolation(policyEvaluation2, policy2, policyWaiver2);
    // Unfixed and unwaived violation
    tempEntity.newPolicyViolation(policyEvaluation2, policy2);

    final int numActiveWaivers = dao.getCountActiveWaivers();

    assertThat(numActiveWaivers)
        .isEqualTo(3);
  }

  @Test
  public void testGetWaivedFixed_DoNotIncludeUnfixedOrUnwaivedViolations() {
    final Application application2 = tempEntity.newApplication(organization.getId());

    final Policy policy1 = tempEntity.newPolicy(application);
    final Policy policy2 = tempEntity.newPolicy(application2);

    // Application 1 evaluations
    final PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID,
        "scan-1", new Date(System.currentTimeMillis()));
    // Violations with active waiver
    final PolicyWaiver policyWaiver1 = tempEntity.newWaiver(policy1.getId(), application.getId());
    final PolicyViolation waivedViolation1 =
        tempEntity.newWaivedPolicyViolation(policyEvaluation1, policy1, policyWaiver1);
    // Unfixed and unwaived violation
    final PolicyViolation unfixedUnwaivedViolation = tempEntity.newPolicyViolation(policyEvaluation1, policy1);

    // Application 2 evaluations
    final PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(application2.getId(), ReleaseStageType.ID,
        "scan-1", new Date(System.currentTimeMillis()));
    // Violation with expired waiver
    final PolicyWaiver policyWaiver2 = tempEntity.newWaiver(policy2.getId(), application2.getId());
    final PolicyViolation expiredWaivedViolation =
        tempEntity.newWaivedPolicyViolation(policyEvaluation2, policy2, policyWaiver2);
    expiredWaivedViolation.setFixTime(new Date(System.currentTimeMillis()));
    dao.update(expiredWaivedViolation);
    // Violation with active waiver
    final PolicyViolation waivedViolation2 =
        tempEntity.newWaivedPolicyViolation(policyEvaluation2, policy2, policyWaiver2);
    // Fixed violation
    final PolicyViolation fixedViolation = tempEntity.newPolicyViolation(policyEvaluation2, policy2);
    fixedViolation.setFixTime(new Date(System.currentTimeMillis()));
    dao.update(fixedViolation);

    final List<PolicyViolation> fixedOrWaivedViolations = dao.getWaivedFixed();

    assertThat(fixedOrWaivedViolations)
        .hasSize(4)
        .extracting("id")
        .containsExactlyInAnyOrder(waivedViolation1.getId(), expiredWaivedViolation.getId(), waivedViolation2.getId(),
            fixedViolation.getId())
        .doesNotContain(unfixedUnwaivedViolation.getId());
  }

  @Test
  public void testGetByIds() {
    Application application = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scanId");
    Policy policy = tempEntity.newPolicy(application);
    PolicyViolation v1 = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolation v2 = tempEntity.newPolicyViolation(policyEvaluation, policy);
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    List<PolicyViolation> result = dao.getByIds(Set.of(v1.getId(), v2.getId()));

    assertThat(result).extracting(PolicyViolation::getId).containsExactly(v1.getId(), v2.getId());
  }

  @Test
  public void testGetByApplicationIdsAndPolicyIdsAndTypes() {
    Policy policy1 = tempEntity.newPolicy(application);

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-1");

    PolicyViolation openViolation = tempEntity.newPolicyViolation(policyEvaluation, policy1);

    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy1,
        tempEntity.newWaiver(policy1.getId(), application.getId()));

    PolicyViolation legacyViolation = tempEntity.newLegacyPolicyViolation(policyEvaluation, policy1);

    Set<String> applicationIds = Collections.singleton(application.getId());
    Set<String> policyIds = Collections.singleton(policy1.getId());

    // active
    Collection<PolicyViolation> violations = dao.getByApplicationIdsAndPolicyIdsAndTypes(
        applicationIds,
        policyIds,
        null,
        null,
        true,
        false,
        false);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openViolation.getId());

    // waived
    violations = dao.getByApplicationIdsAndPolicyIdsAndTypes(
        applicationIds,
        policyIds,
        null,
        null,
        false,
        true,
        false);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(waivedViolation.getId());

    // legacy
    violations = dao.getByApplicationIdsAndPolicyIdsAndTypes(
        applicationIds,
        policyIds,
        null,
        null,
        false,
        false,
        true);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(legacyViolation.getId());

    // active and waived
    violations = dao.getByApplicationIdsAndPolicyIdsAndTypes(
        applicationIds,
        policyIds,
        null,
        null,
        true,
        true,
        false);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openViolation.getId(), waivedViolation.getId());

    // active and legacy
    violations = dao.getByApplicationIdsAndPolicyIdsAndTypes(
        applicationIds,
        policyIds,
        null,
        null,
        true,
        false,
        true);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openViolation.getId(), legacyViolation.getId());

    // legacy and waived
    violations = dao.getByApplicationIdsAndPolicyIdsAndTypes(
        applicationIds,
        policyIds,
        null,
        null,
        false,
        true,
        true);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(legacyViolation.getId(), waivedViolation.getId());

    // all types
    violations = dao.getByApplicationIdsAndPolicyIdsAndTypes(
        applicationIds,
        policyIds,
        null,
        null,
        true,
        true,
        true);
    assertThat(violations).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder(openViolation.getId(), waivedViolation.getId(), legacyViolation.getId());
  }

  @Test
  public void testGetContainerImagePolicyViolationSummaryForRepository() {
    Organization organization = tempEntity.newOrganization("org");
    organization.setRelatedRepositoryId(repository.getId());
    repository.setRelatedOrganizationId(organization.getId());
    organizationDAO.update(organization);
    // Container Image applications
    Application application1 = tempEntity.newApplication("app1", "appPublicId1", organization.getId());
    Application application2 = tempEntity.newApplication("app2", "appPublicId2", organization.getId());

    // policy evaluation
    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(application1.getId(), "proxy", "scanId1");
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(application2.getId(), "proxy", "scanId2");

    // policy for policy violation
    Policy policy1 = tempEntity.newPolicy(application1.getId(), "policy1");
    Policy policy2 = tempEntity.newPolicy(application1.getId(), "policy2");
    Policy policy3 = tempEntity.newPolicy(application1.getId(), "policy3");
    Policy policy4 = tempEntity.newPolicy(application1.getId(), "policy4");

    Policy policy5 = tempEntity.newPolicy(application2.getId(), "policy5");
    Policy policy6 = tempEntity.newPolicy(application2.getId(), "policy6");

    // create policy violations app 1
    PolicyViolation policyViolation1 = tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    PolicyViolation policyViolation2 = tempEntity.newPolicyViolation(policyEvaluation1, policy2);
    PolicyViolation policyViolation3 = tempEntity.newPolicyViolation(policyEvaluation1, policy3);
    PolicyViolation policyViolation4 = tempEntity.newPolicyViolation(policyEvaluation1, policy4);
    policyViolation1.setThreatLevel(10);
    policyViolation2.setThreatLevel(8);
    policyViolation3.setThreatLevel(10);
    policyViolation4.setThreatLevel(5);
    policyViolation1.setActionTypeId("fail");

    // create policy violations app 1
    PolicyViolation policyViolation5 = tempEntity.newPolicyViolation(policyEvaluation2, policy5);
    PolicyViolation policyViolation6 = tempEntity.newPolicyViolation(policyEvaluation2, policy6);
    policyViolation5.setThreatLevel(10);
    policyViolation6.setThreatLevel(2);

    dao.update(policyViolation1);
    dao.update(policyViolation2);
    dao.update(policyViolation3);
    dao.update(policyViolation4);
    dao.update(policyViolation5);
    dao.update(policyViolation6);

    ContainerImagePolicyViolationSummaryDTO result = dao
        .getContainerImagePolicyViolationSummaryForRepository(repository.getId());
    assertThat(result.getCriticalPolicyViolationsCount()).isEqualTo(4);
    assertThat(result.getSeverePolicyViolationsCount()).isEqualTo(1);
    assertThat(result.getModeratePolicyViolationsCount()).isEqualTo(1);
    assertThat(result.getQuarantinedContainerImagesCount()).isEqualTo(1);
  }

  @Test
  public void testGetContainerImagePolicyViolationSummaryForRepository_NoViolations() {
    String repositoryId = "repo-empty";
    Organization organization = tempEntity.newOrganization();
    organization.setRelatedRepositoryId(repositoryId);
    organizationDAO.update(organization);
    tempEntity.newApplication(organization.getId());
    ContainerImagePolicyViolationSummaryDTO result = dao
        .getContainerImagePolicyViolationSummaryForRepository(repositoryId);
    assertThat(result.getCriticalPolicyViolationsCount()).isEqualTo(0);
    assertThat(result.getModeratePolicyViolationsCount()).isEqualTo(0);
    assertThat(result.getSeverePolicyViolationsCount()).isEqualTo(0);
    assertThat(result.getQuarantinedContainerImagesCount()).isEqualTo(0);
  }

  @Test
  public void getContainerImagesInQuarantine() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo1 = tempEntity.newRepository("repo1", repoManager.getId(), "docker");
    repo1.setQuarantineEnabled(true);
    repositoryDAO.update(repo1);
    Organization org1 = tempEntity.newOrganization("org1");
    org1.setRelatedRepositoryId(repo1.getId());
    organizationDAO.update(org1);

    Application app1 = tempEntity.newApplication("app1", org1.getId());
    Policy policy = tempEntity.newPolicy(organization);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), ProxyStageType.ID,
        "scan-1");
    tempEntity.newPolicyViolation(policyEvaluation, policy, 10, PolicyThreatCategory.OTHER, "test-group-id",
        "test-artifact-id", "v1", "test-hash", FailActionType.ID);

    List<ContainerImageInQuarantineData> containerImagesInQuarantine = dao.getContainerImagesInQuarantine(1, 10);

    assertThat(containerImagesInQuarantine).hasSize(1);
  }

  @Test
  public void getContainerImagesInQuarantine_accountForMultipleComponents() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo1 = tempEntity.newRepository("repo1", repoManager.getId(), "docker");
    repo1.setQuarantineEnabled(true);
    repositoryDAO.update(repo1);
    Organization org1 = tempEntity.newOrganization("org1");
    org1.setRelatedRepositoryId(repo1.getId());
    organizationDAO.update(org1);

    Application app1 = tempEntity.newApplication("app1", org1.getId());
    Policy policy = tempEntity.newPolicy(organization);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), ProxyStageType.ID,
        "scan-1");
    tempEntity.newPolicyViolation(policyEvaluation, policy, 8, PolicyThreatCategory.OTHER, "test-group-id",
        "test-artifact-id1", "v1", "test-hash", FailActionType.ID);
    tempEntity.newPolicyViolation(policyEvaluation, policy, 9, PolicyThreatCategory.OTHER, "test-group-id",
        "test-artifact-id2", "v1", "test-hash", FailActionType.ID);

    List<ContainerImageInQuarantineData> containerImagesInQuarantine = dao.getContainerImagesInQuarantine(1, 10);

    assertThat(containerImagesInQuarantine).hasSize(1);
    assertThat(containerImagesInQuarantine.get(0).policyViolationCount()).isEqualTo(2);
    assertThat(containerImagesInQuarantine.get(0).threatLevel()).isEqualTo(9);
  }

  @Test
  public void getContainerImagesInQuarantine_returnsMultipleContainerImages() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo1 = tempEntity.newRepository("repo1", repoManager.getId(), "docker");
    repo1.setQuarantineEnabled(true);
    repositoryDAO.update(repo1);
    Organization org1 = tempEntity.newOrganization("org1");
    org1.setRelatedRepositoryId(repo1.getId());
    organizationDAO.update(org1);

    setupContainerImagesWithViolations(org1);

    List<ContainerImageInQuarantineData> containerImagesInQuarantine = dao.getContainerImagesInQuarantine(1, 10);

    assertThat(containerImagesInQuarantine).hasSize(2);
  }

  @Test
  public void getContainerImagesInQuarantine_returnsPagedList() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo1 = tempEntity.newRepository("repo1", repoManager.getId(), "docker");
    repo1.setQuarantineEnabled(true);
    repositoryDAO.update(repo1);
    Organization org1 = tempEntity.newOrganization("org1");
    org1.setRelatedRepositoryId(repo1.getId());
    organizationDAO.update(org1);

    setupContainerImagesWithViolations(org1);

    List<ContainerImageInQuarantineData> page1 = dao.getContainerImagesInQuarantine(1, 1);
    assertThat(page1).hasSize(1);
    List<ContainerImageInQuarantineData> page2 = dao.getContainerImagesInQuarantine(2, 1);
    assertThat(page2).hasSize(1);

    assertThat(page1.get(0).applicationName()).isNotEqualTo(page2.get(0).applicationName());
  }

  @Test
  public void getContainerImagesInQuarantine_returnsAllFromMultipleOrgs() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo1 = tempEntity.newRepository("repo1", repoManager.getId(), "docker");
    repo1.setQuarantineEnabled(true);
    repositoryDAO.update(repo1);
    Organization org1 = tempEntity.newOrganization("org1");
    org1.setRelatedRepositoryId(repo1.getId());
    organizationDAO.update(org1);
    Organization org2 = tempEntity.newOrganization("org2");
    org2.setRelatedRepositoryId(repo1.getId());
    organizationDAO.update(org2);

    setupContainerImagesWithViolations(org1);
    setupContainerImagesWithViolations(org2);

    List<ContainerImageInQuarantineData> page1 = dao.getContainerImagesInQuarantine(1, 10);
    assertThat(page1).hasSize(4);
  }

  @Test
  public void getContainerImagesInQuarantine_doesNotIncludeWaivedViolations() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo1 = tempEntity.newRepository("repo1", repoManager.getId(), "docker");
    repo1.setQuarantineEnabled(true);
    repositoryDAO.update(repo1);
    Organization org1 = tempEntity.newOrganization("org1");
    org1.setRelatedRepositoryId(repo1.getId());
    organizationDAO.update(org1);

    Application app1 = tempEntity.newApplication("app1", org1.getId());
    Policy policy = tempEntity.newPolicy(organization);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), ProxyStageType.ID,
        "scan-1");
    tempEntity.newPolicyViolation(policyEvaluation, policy, 8, PolicyThreatCategory.OTHER, "test-group-id",
        "test-artifact-id1", "v1", "test-hash", FailActionType.ID);
    tempEntity.newPolicyViolation(policyEvaluation, policy, 9, PolicyThreatCategory.OTHER, "test-group-id",
        "test-artifact-id2", "v1", "test-hash", FailActionType.ID);
    PolicyViolation policyViolation =
        tempEntity.newPolicyViolation(policyEvaluation, policy, 10, PolicyThreatCategory.OTHER, "test-group-id",
            "test-artifact-id2", "v1", "test-hash", FailActionType.ID);
    policyViolation.setWaiveTime(DateUtils.addDays(new Date(), 1));
    dao.update(policyViolation);

    List<ContainerImageInQuarantineData> containerImagesInQuarantine = dao.getContainerImagesInQuarantine(1, 10);

    assertThat(containerImagesInQuarantine).hasSize(1);
    assertThat(containerImagesInQuarantine.get(0).policyViolationCount()).isEqualTo(2);
    assertThat(containerImagesInQuarantine.get(0).threatLevel()).isEqualTo(9);
  }

  @Test
  public void testGetContainerImagesInQuarantine_IncludesLegacyViolations() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo1 = tempEntity.newRepository("repo1", repoManager.getId(), "docker");
    repo1.setQuarantineEnabled(true);
    repositoryDAO.update(repo1);
    Organization org1 = tempEntity.newOrganization("org1");
    org1.setRelatedRepositoryId(repo1.getId());
    organizationDAO.update(org1);

    Application app1 = tempEntity.newApplication("app1", org1.getId());
    Policy policy = tempEntity.newPolicy(organization);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(
        app1.getId(), ProxyStageType.ID, "scan-1");

    // Create legacy violation with fail action
    PolicyViolation legacyViolation = tempEntity.newPolicyViolation(
        policyEvaluation, policy, 8, PolicyThreatCategory.SECURITY,
        "test-group-id", "test-artifact-id1", "v1", "test-hash", FailActionType.ID);
    legacyViolation.setLegacyViolationTime(new Date());
    dao.update(legacyViolation);

    List<ContainerImageInQuarantineData> results = dao.getContainerImagesInQuarantine(1, 10);

    // Firewall should include legacy violations in quarantine list
    assertThat(results).hasSize(1);
    assertThat(results.get(0).applicationId()).isEqualTo(app1.getId());
    assertThat(results.get(0).policyViolationCount()).isEqualTo(1);
  }

  @Test
  public void getContainerImagesQuarantinedCount() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo1 = tempEntity.newRepository("repo1", repoManager.getId(), "docker");
    repo1.setQuarantineEnabled(true);
    repositoryDAO.update(repo1);
    Organization org1 = tempEntity.newOrganization("org1");
    org1.setRelatedRepositoryId(repo1.getId());
    organizationDAO.update(org1);

    Application app1 = tempEntity.newApplication("app1", org1.getId());
    Application app2 = tempEntity.newApplication("app2", org1.getId());
    Application app3 = tempEntity.newApplication("app3", org1.getId());
    Policy policy = tempEntity.newPolicy(organization);

    // First container with violations
    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app1.getId(), ProxyStageType.ID,
        "scan-1");
    tempEntity.newPolicyViolation(policyEvaluation1, policy, 8, PolicyThreatCategory.OTHER, "test-group-id",
        "test-artifact-id1", "v1", "test-hash", FailActionType.ID);
    tempEntity.newPolicyViolation(policyEvaluation1, policy, 9, PolicyThreatCategory.OTHER, "test-group-id",
        "test-artifact-id2", "v1", "test-hash", FailActionType.ID);
    PolicyViolation policyViolation =
        tempEntity.newPolicyViolation(policyEvaluation1, policy, 10, PolicyThreatCategory.OTHER, "test-group-id",
            "test-artifact-id2", "v1", "test-hash", FailActionType.ID);
    policyViolation.setWaiveTime(DateUtils.addDays(new Date(), 1));
    dao.update(policyViolation);

    // Second container image with violations
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(app2.getId(), ProxyStageType.ID,
        "scan-1");
    tempEntity.newPolicyViolation(policyEvaluation2, policy, 8, PolicyThreatCategory.OTHER, "test-group-id",
        "test-artifact-id1", "v1", "test-hash", FailActionType.ID);

    // Third container image with no violations
    tempEntity.newPolicyEvaluation(app3.getId(), ProxyStageType.ID,
        "scan-1");

    assertThat(dao.getContainerImagesQuarantinedCount()).isEqualTo(2);
  }

  @Test
  public void testGetContainerImagesQuarantinedCount_IncludesLegacyViolations() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo1 = tempEntity.newRepository("repo1", repoManager.getId(), "docker");
    repo1.setQuarantineEnabled(true);
    repositoryDAO.update(repo1);
    Organization org1 = tempEntity.newOrganization("org1");
    org1.setRelatedRepositoryId(repo1.getId());
    organizationDAO.update(org1);

    Application app1 = tempEntity.newApplication("app1", org1.getId());
    Policy policy = tempEntity.newPolicy(organization);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(
        app1.getId(), ProxyStageType.ID, "scan-1");

    // Create legacy violation with fail action
    PolicyViolation legacyViolation = tempEntity.newPolicyViolation(
        policyEvaluation, policy, 8, PolicyThreatCategory.SECURITY,
        "test-group-id", "test-artifact-id1", "v1", "test-hash", FailActionType.ID);
    legacyViolation.setLegacyViolationTime(new Date());
    dao.update(legacyViolation);

    // Firewall should count legacy violations as quarantined
    assertThat(dao.getContainerImagesQuarantinedCount()).isEqualTo(1);
  }

  @Test
  public void getContainerImagesInQuarantine_excludesAuditOnlyRepos() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    // audit-only repo: quarantine_enabled = false
    Repository auditOnlyRepo = tempEntity.newRepository("audit-repo", repoManager.getId(), "docker");
    Organization org1 = tempEntity.newOrganization("org1-audit");
    org1.setRelatedRepositoryId(auditOnlyRepo.getId());
    organizationDAO.update(org1);

    Application app1 = tempEntity.newApplication("app1", org1.getId());
    Policy policy = tempEntity.newPolicy(organization);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), ProxyStageType.ID, "scan-1");
    tempEntity.newPolicyViolation(policyEvaluation, policy, 10, PolicyThreatCategory.OTHER, "test-group-id",
        "test-artifact-id", "v1", "test-hash", FailActionType.ID);

    List<ContainerImageInQuarantineData> result = dao.getContainerImagesInQuarantine(1, 10);

    assertThat(result).isEmpty();
  }

  @Test
  public void getContainerImagesQuarantinedCount_excludesAuditOnlyRepos() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    // audit-only repo: quarantine_enabled = false
    Repository auditOnlyRepo = tempEntity.newRepository("audit-repo", repoManager.getId(), "docker");
    Organization org1 = tempEntity.newOrganization("org1-audit");
    org1.setRelatedRepositoryId(auditOnlyRepo.getId());
    organizationDAO.update(org1);

    Application app1 = tempEntity.newApplication("app1", org1.getId());
    Policy policy = tempEntity.newPolicy(organization);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), ProxyStageType.ID, "scan-1");
    tempEntity.newPolicyViolation(policyEvaluation, policy, 10, PolicyThreatCategory.OTHER, "test-group-id",
        "test-artifact-id", "v1", "test-hash", FailActionType.ID);

    assertThat(dao.getContainerImagesQuarantinedCount()).isEqualTo(0);
  }

  @Test
  public void getRepositoryResultsForImageContainerAggregate_TestNullQuarantineTime() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo1 = tempEntity.newRepository("repo1", repoManager.getId(), "docker");
    Organization org1 = tempEntity.newOrganization("org1");
    org1.setRelatedRepositoryId(repo1.getId());
    organizationDAO.update(org1);

    Application app1 = tempEntity.newApplication("app1", org1.getId());
    Application app2 = tempEntity.newApplication("app2", org1.getId());
    Policy policy = tempEntity.newPolicy(organization);

    // First container with violations
    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app1.getId(), ProxyStageType.ID,
        "scan-1");
    tempEntity.newPolicyViolation(policyEvaluation1, policy, 8, PolicyThreatCategory.OTHER, "test-group-id",
        "test-artifact-id1", "v1", "test-hash", FailActionType.ID);
    tempEntity.newPolicyViolation(policyEvaluation1, policy, 9, PolicyThreatCategory.OTHER, "test-group-id",
        "test-artifact-id2", "v1", "test-hash", FailActionType.ID);

    // Second container image with violations
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(app2.getId(), ProxyStageType.ID,
        "scan-2");
    // Create PolicyViolation with waiver time not null
    ConstraintFact constraintFact = new ConstraintFact("constraintdata", "constraintdata", "constraintdata");
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("Group1", "Artifact1",
        "Version1");
    PolicyViolation policyViolation2 = new PolicyViolation(policyEvaluation2, policy.getId(), policy.getName(), 5,
        PolicyThreatCategory.OTHER, "hash", componentIdentifier, List.of(constraintFact), "filename");
    policyViolation2.setWaiveTime(DateUtils.addDays(new Date(), 1));
    policyViolation2.setActionTypeId(FailActionType.ID);
    dao.insert(policyViolation2);

    Collection<String> repoId = Collections.singleton(repo1.getId());
    Collection<String> applicationIds = Arrays.asList(app1.getId(), app2.getId());
    final RepositoryResultsForImageContainerFilter detailsFilter = getDetailsFilter();
    List<RepositoryResultsForImageContainer> result =
        dao.getRepositoryResultsForImageContainerAggregate(repoId, applicationIds, detailsFilter);
    assertThat(result).hasSize(2);
    assertThat(result.get(1).quarantineTime).isNull();
  }

  private RepositoryResultsForImageContainerFilter getDetailsFilter() {
    RepositoryResultsForImageContainerFilter detailsFilter = new RepositoryResultsForImageContainerFilter();
    detailsFilter.page = 1;
    detailsFilter.pageSize = 50;
    detailsFilter.aggregate = true;
    detailsFilter.searchFilters = Collections.EMPTY_MAP;
    detailsFilter.violationStateFilters = Collections.singleton("VIOLATION_STATE_ALL");
    SortField sortField = new SortField();
    sortField.asc = false;
    sortField.sortPriority = 1;
    sortField.sortableField = SortableField.QUARANTINE_TIME;
    detailsFilter.sortFields = Collections.singletonList(sortField);
    return detailsFilter;
  }

  private void setupContainerImagesWithViolations(final Organization org) {
    Application app1 = tempEntity.newApplication("app1-" + org.getName(), org.getId());
    Application app2 = tempEntity.newApplication("app2-" + org.getName(), org.getId());
    Policy policy = tempEntity.newPolicy(organization);

    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app1.getId(), ProxyStageType.ID,
        "scan-1");
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(app2.getId(), ProxyStageType.ID,
        "scan-2");
    tempEntity.newPolicyViolation(policyEvaluation1, policy, 10, PolicyThreatCategory.OTHER, "test-group-id",
        "test-artifact-id1", "v1", "test-hash", FailActionType.ID);
    tempEntity.newPolicyViolation(policyEvaluation2, policy, 9, PolicyThreatCategory.OTHER, "test-group-id",
        "test-artifact-id2", "v1", "test-hash", FailActionType.ID);
  }

  private List<PolicyViolation> createPolicyViolations(Date cutoffDate, Object[][] data) {
    final Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation;

    List<PolicyViolation> policyViolations = new ArrayList<>();
    for (PolicyViolationDefinition policyViolationDef : toPolicyViolations(data)) {
      Date policyEvalDate = switch (policyViolationDef.created) {
        case CREATED_BEFORE_CUTOFF -> DateUtils.addDays(cutoffDate, -1);
        case CREATED_AFTER_CUTOFF -> DateUtils.addDays(cutoffDate, 1);
      };

      policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID,
          "PolicyViolationDAOTestScanId", policyEvalDate);

      PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

      Date resolvedDate = switch (policyViolationDef.resolved) {
        case RESOLVED_BEFORE_CUTOFF -> DateUtils.addDays(cutoffDate, -1);
        case RESOLVED_AFTER_CUTOFF -> DateUtils.addDays(cutoffDate, 1);
        default -> null;
      };

      if (null != resolvedDate) {
        switch (policyViolationDef.violationState) {
          case FIXED -> policyViolation.setFixTime(resolvedDate);
          case WAIVED -> policyViolation.setWaiveTime(resolvedDate);
          case LEGACY -> policyViolation.setLegacyViolationTime(resolvedDate);
          default -> {
          }
        }
        dao.update(policyViolation);
      }
      policyViolations.add(policyViolation);
    }

    return policyViolations;
  }

  private List<PolicyViolationDefinition> toPolicyViolations(Object[][] data) {
    final List<PolicyViolationDefinition> result = new ArrayList<>(data.length);
    for (Object[] row : data) {
      result.add(new PolicyViolationDefinition(
          (PolicyViolationState) row[0],
          (Created) row[1],
          (Resolved) row[2]));
    }
    return result;
  }

  private record PolicyViolationDefinition(PolicyViolationState violationState, Created created, Resolved resolved)
  {
  }

  @Test
  public void testGetRepositoryResultsForImageContainerAggregate_ViolationStateFilters() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo1 = tempEntity.newRepository("repo1", repoManager.getId(), "docker");
    Organization org1 = tempEntity.newOrganization("org1");
    org1.setRelatedRepositoryId(repo1.getId());
    organizationDAO.update(org1);

    // Create applications for different violation states
    Application app1 = tempEntity.newApplication("app1", org1.getId()); // Open violation
    Application app2 = tempEntity.newApplication("app2", org1.getId()); // Quarantined violation
    Application app3 = tempEntity.newApplication("app3", org1.getId()); // Waived violation

    Policy policy = tempEntity.newPolicy(organization);

    // Create policy evaluations
    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app1.getId(), ProxyStageType.ID, "scan-1");
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(app2.getId(), ProxyStageType.ID, "scan-2");
    PolicyEvaluation policyEvaluation3 = tempEntity.newPolicyEvaluation(app3.getId(), ProxyStageType.ID, "scan-3");

    // App1: Create open violation (not waived, not quarantined)
    tempEntity.newPolicyViolation(policyEvaluation1, policy, 5, PolicyThreatCategory.OTHER, "test-group-id",
        "test-artifact-id1", "v1", "test-hash", "warn");

    // App2: Create quarantined violation (fail action, not waived)
    tempEntity.newPolicyViolation(policyEvaluation2, policy, 7, PolicyThreatCategory.OTHER, "test-group-id",
        "test-artifact-id2", "v1", "test-hash", "fail");

    // App3: Create waived violation
    ConstraintFact constraintFact = new ConstraintFact("constraintdata", "constraintdata", "constraintdata");
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("Group1", "Artifact1", "Version1");
    PolicyViolation waivedViolation = new PolicyViolation(policyEvaluation3, policy.getId(), policy.getName(), 6,
        PolicyThreatCategory.OTHER, "hash3", componentIdentifier, List.of(constraintFact), "filename");
    waivedViolation.setWaiveTime(new Date());
    waivedViolation.setActionTypeId("fail");
    dao.insert(waivedViolation);

    Collection<String> repoId = Collections.singleton(repo1.getId());
    Collection<String> applicationIds = Arrays.asList(app1.getId(), app2.getId(), app3.getId());

    // Test VIOLATION_STATE_OPEN - should return only open violations (not waived)
    // Note: Legacy violations ARE included in Firewall (legacy flag is ignored)
    RepositoryResultsForImageContainerFilter filterOpen = getDetailsFilter();
    filterOpen.violationStateFilters = Collections.singleton("VIOLATION_STATE_OPEN");
    List<RepositoryResultsForImageContainer> resultOpen =
        dao.getRepositoryResultsForImageContainerAggregate(repoId, applicationIds, filterOpen);
    assertThat(resultOpen)
        .extracting(result -> result.applicationPublicId)
        .containsExactlyInAnyOrder(app1.getPublicId(), app2.getPublicId());
    // app1 (open) and app2 (quarantined but still open)

    // Test VIOLATION_STATE_QUARANTINED - should return only quarantined violations (fail action, not waived)
    RepositoryResultsForImageContainerFilter filterQuarantined = getDetailsFilter();
    filterQuarantined.violationStateFilters = Collections.singleton("VIOLATION_STATE_QUARANTINED");
    List<RepositoryResultsForImageContainer> resultQuarantined =
        dao.getRepositoryResultsForImageContainerAggregate(repoId, applicationIds, filterQuarantined);
    assertThat(resultQuarantined)
        .extracting(result -> result.applicationPublicId)
        .containsExactly(app2.getPublicId()); // only app2 has quarantined violation (fail action, not waived)

    // Test VIOLATION_STATE_WAIVED - should return only waived violations
    RepositoryResultsForImageContainerFilter filterWaived = getDetailsFilter();
    filterWaived.violationStateFilters = Collections.singleton("VIOLATION_STATE_WAIVED");
    List<RepositoryResultsForImageContainer> resultWaived =
        dao.getRepositoryResultsForImageContainerAggregate(repoId, applicationIds, filterWaived);
    assertThat(resultWaived)
        .extracting(result -> result.applicationPublicId)
        .containsExactly(app3.getPublicId()); // only app3 has waived violation

    // Verify the waived violation has null quarantine time
    RepositoryResultsForImageContainer waivedResult = resultWaived.get(0);
    assertThat(waivedResult.quarantineTime).isNull();

  }

  @Test
  public void testGetRepositoryResultsForImageContainer_OpenFilter_IncludesLegacy() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo1 = tempEntity.newRepository("repo1", repoManager.getId(), "docker");
    Organization org1 = tempEntity.newOrganization("org1");
    org1.setRelatedRepositoryId(repo1.getId());
    organizationDAO.update(org1);

    Application app1 = tempEntity.newApplication("app1", org1.getId());
    Application app2 = tempEntity.newApplication("app2", org1.getId());
    Policy policy = tempEntity.newPolicy(organization);

    // App1: Regular open violation
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app1.getId(), ProxyStageType.ID, "scan-1");
    tempEntity.newPolicyViolation(eval1, policy, 8, PolicyThreatCategory.SECURITY,
        "group1", "artifact1", "v1", "hash1", FailActionType.ID);

    // App2: Legacy violation (should be included in OPEN filter for Firewall)
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app2.getId(), ProxyStageType.ID, "scan-2");
    PolicyViolation legacyViolation = tempEntity.newPolicyViolation(eval2, policy, 9,
        PolicyThreatCategory.SECURITY, "group2", "artifact2", "v1", "hash2", FailActionType.ID);
    legacyViolation.setLegacyViolationTime(new Date());
    dao.update(legacyViolation);

    Collection<String> repoIds = Collections.singleton(repo1.getId());
    Collection<String> appIds = Arrays.asList(app1.getId(), app2.getId());

    RepositoryResultsForImageContainerFilter filter = getDetailsFilter();
    filter.violationStateFilters = Collections.singleton("VIOLATION_STATE_OPEN");

    List<RepositoryResultsForImageContainer> results =
        dao.getRepositoryResultsForImageContainerAggregate(repoIds, appIds, filter);

    // Both regular AND legacy violations should be included in OPEN filter for Firewall
    assertThat(results)
        .extracting(r -> r.applicationPublicId)
        .containsExactlyInAnyOrder(app1.getPublicId(), app2.getPublicId());
  }

  @Test
  public void testGetRepositoryResultsForImageContainer_NullActionTypeId_ReturnsNullQuarantineTime() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo1 = tempEntity.newRepository("repo1", repoManager.getId(), "docker");
    Organization org1 = tempEntity.newOrganization("org1");
    org1.setRelatedRepositoryId(repo1.getId());
    organizationDAO.update(org1);

    Application app1 = tempEntity.newApplication("app1", org1.getId());
    Policy policy = tempEntity.newPolicy(organization);

    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app1.getId(), ProxyStageType.ID, "scan-1");

    ConstraintFact constraintFact = new ConstraintFact("constraintdata", "constraintdata", "constraintdata");
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("Group1", "Artifact1",
        "Version1");
    PolicyViolation violation = new PolicyViolation(policyEvaluation1, policy.getId(), policy.getName(), 8,
        PolicyThreatCategory.OTHER, "hash1", componentIdentifier, List.of(constraintFact), "filename");
    violation.setActionTypeId(null);
    dao.insert(violation);

    Collection<String> repoId = Collections.singleton(repo1.getId());
    Collection<String> applicationIds = Collections.singleton(app1.getId());
    RepositoryResultsForImageContainerFilter detailsFilter = getDetailsFilter();

    List<RepositoryResultsForImageContainer> result =
        dao.getRepositoryResultsForImageContainerAggregate(repoId, applicationIds, detailsFilter);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).quarantineTime).isNull();
  }

  @Test
  public void testGetRepositoryResultsForImageContainer_OnlyProxyStageIncluded() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo1 = tempEntity.newRepository("repo1", repoManager.getId(), "docker");
    Organization org1 = tempEntity.newOrganization("org1");
    org1.setRelatedRepositoryId(repo1.getId());
    organizationDAO.update(org1);

    Application app1 = tempEntity.newApplication("app1", org1.getId());
    Application app2 = tempEntity.newApplication("app2", org1.getId());
    Policy policy = tempEntity.newPolicy(organization);

    PolicyEvaluation proxyEval = tempEntity.newPolicyEvaluation(app1.getId(), ProxyStageType.ID, "scan-proxy");
    PolicyViolation proxyViolation = tempEntity.newPolicyViolation(proxyEval, policy, 8, PolicyThreatCategory.OTHER,
        "test-group-id", "test-artifact-id1", "v1", "hash1", FailActionType.ID);

    PolicyEvaluation buildEval = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scan-build");
    tempEntity.newPolicyViolation(buildEval, policy, 9, PolicyThreatCategory.OTHER,
        "test-group-id", "test-artifact-id2", "v1", "hash2", FailActionType.ID);

    Collection<String> repoId = Collections.singleton(repo1.getId());
    Collection<String> applicationIds = Arrays.asList(app1.getId(), app2.getId());
    RepositoryResultsForImageContainerFilter detailsFilter = getDetailsFilter();

    List<RepositoryResultsForImageContainer> result =
        dao.getRepositoryResultsForImageContainerAggregate(repoId, applicationIds, detailsFilter);

    assertThat(result)
        .extracting(r -> r.applicationPublicId)
        .contains(app1.getPublicId());

    RepositoryResultsForImageContainer app1Result = result.stream()
        .filter(r -> r.applicationPublicId.equals(app1.getPublicId()))
        .findFirst()
        .get();
    assertThat(app1Result.violationCount).isEqualTo(1);
    assertThat(app1Result.quarantineTime).isNotNull();
    assertThat(app1Result.quarantineTime).isEqualTo(proxyViolation.getOpenTime());
  }

  @Test
  public void testGetRepositoryResultsForImageContainer_MixedActions_OnlyFailShowsQuarantine() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo1 = tempEntity.newRepository("repo1", repoManager.getId(), "docker");
    Organization org1 = tempEntity.newOrganization("org1");
    org1.setRelatedRepositoryId(repo1.getId());
    organizationDAO.update(org1);

    Application app1 = tempEntity.newApplication("app1", org1.getId());
    Policy policy = tempEntity.newPolicy(organization);

    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app1.getId(), ProxyStageType.ID, "scan-1");

    PolicyViolation failViolation = tempEntity.newPolicyViolation(policyEvaluation1, policy, 8,
        PolicyThreatCategory.OTHER, "group1", "artifact1", "v1", "hash1", FailActionType.ID);

    tempEntity.newPolicyViolation(policyEvaluation1, policy, 7,
        PolicyThreatCategory.OTHER, "group2", "artifact2", "v1", "hash2", "warn");

    ConstraintFact constraintFact = new ConstraintFact("constraintdata", "constraintdata", "constraintdata");
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("Group3", "Artifact3", "V1");
    PolicyViolation nullActionViolation = new PolicyViolation(policyEvaluation1, policy.getId(), policy.getName(), 6,
        PolicyThreatCategory.OTHER, "hash3", componentIdentifier, List.of(constraintFact), "filename");
    nullActionViolation.setActionTypeId(null);
    dao.insert(nullActionViolation);

    Collection<String> repoId = Collections.singleton(repo1.getId());
    Collection<String> applicationIds = Collections.singleton(app1.getId());
    RepositoryResultsForImageContainerFilter detailsFilter = getDetailsFilter();

    List<RepositoryResultsForImageContainer> result =
        dao.getRepositoryResultsForImageContainerAggregate(repoId, applicationIds, detailsFilter);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).violationCount).isEqualTo(3);
    assertThat(result.get(0).quarantineTime).isNotNull();
    assertThat(result.get(0).quarantineTime).isEqualTo(failViolation.getOpenTime());
  }

  @Test
  public void testGetRepositoryResultsForImageContainer_AllWaivedOrNonFail_NullQuarantineTime() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo1 = tempEntity.newRepository("repo1", repoManager.getId(), "docker");
    Organization org1 = tempEntity.newOrganization("org1");
    org1.setRelatedRepositoryId(repo1.getId());
    organizationDAO.update(org1);

    Application app1 = tempEntity.newApplication("app1", org1.getId());
    Policy policy = tempEntity.newPolicy(organization);

    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app1.getId(), ProxyStageType.ID, "scan-1");

    ConstraintFact constraintFact = new ConstraintFact("constraintdata", "constraintdata", "constraintdata");
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("Group1", "Artifact1", "V1");
    PolicyViolation waivedFailViolation = new PolicyViolation(policyEvaluation1, policy.getId(), policy.getName(), 8,
        PolicyThreatCategory.OTHER, "hash1", componentIdentifier, List.of(constraintFact), "filename");
    waivedFailViolation.setActionTypeId(FailActionType.ID);
    waivedFailViolation.setWaiveTime(new Date());
    dao.insert(waivedFailViolation);

    tempEntity.newPolicyViolation(policyEvaluation1, policy, 7,
        PolicyThreatCategory.OTHER, "group2", "artifact2", "v1", "hash2", "warn");

    PolicyViolation nullActionViolation = new PolicyViolation(policyEvaluation1, policy.getId(), policy.getName(), 6,
        PolicyThreatCategory.OTHER, "hash3", componentIdentifier, List.of(constraintFact), "filename");
    nullActionViolation.setActionTypeId(null);
    dao.insert(nullActionViolation);

    Collection<String> repoId = Collections.singleton(repo1.getId());
    Collection<String> applicationIds = Collections.singleton(app1.getId());
    RepositoryResultsForImageContainerFilter detailsFilter = getDetailsFilter();

    List<RepositoryResultsForImageContainer> result =
        dao.getRepositoryResultsForImageContainerAggregate(repoId, applicationIds, detailsFilter);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).quarantineTime).isNull();
  }

  @Test
  public void testAllFieldsRoundTrip_insertAndUpdate() {
    doTestAllFieldsRoundTrip_insertAndUpdate();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testAllFieldsRoundTrip_insertAndUpdate_Postgres() {
    doTestAllFieldsRoundTrip_insertAndUpdate();
  }

  @Test
  public void testAllFieldsRoundTrip_insertBatchAndUpdateBatch() {
    doTestAllFieldsRoundTrip_insertBatchAndUpdateBatch();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
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
  public void testInsertBatch_storesConstraintFacts() {
    doTestInsertBatch_storesConstraintFacts();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testInsertBatch_storesConstraintFacts_Postgres() {
    doTestInsertBatch_storesConstraintFacts();
  }

  @Test
  public void testUpdateBatch_storesConstraintFacts() {
    doTestUpdateBatch_storesConstraintFacts();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
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
  public void testInsertBatch_multipleViolations_storesConstraintsBatched() {
    doTestInsertBatch_multipleViolations_storesConstraintsBatched();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
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
  public void testUpdateBatch_skipsAlreadyPersistedConstraints() {
    doTestUpdateBatch_skipsAlreadyPersistedConstraints();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
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
  public void testInsertBatch_throwsWhenConstraintFactsNotLoaded() {
    doTestInsertBatch_throwsWhenConstraintFactsNotLoaded();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
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

  private PolicyViolation newUnfixedViolation(Application owner, String stageTypeId, int threatLevel) {
    Policy policy = tempEntity.newPolicy(owner);
    PolicyEvaluation evaluation =
        tempEntity.newPolicyEvaluation(owner.getId(), stageTypeId, "metric-" + UUID.randomUUID());
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, policy);
    violation.setThreatLevel(threatLevel);
    dao.update(violation);
    return violation;
  }
}
