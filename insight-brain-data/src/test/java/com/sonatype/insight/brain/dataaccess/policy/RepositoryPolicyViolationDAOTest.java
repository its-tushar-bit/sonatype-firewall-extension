/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import com.sonatype.insight.brain.common.test.SlowTest;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsCountSummary;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetails;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetailsFilter;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolationConstraintFacts;
import com.sonatype.insight.brain.model.policy.PolicyViolationSummary;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import com.google.common.collect.ImmutableSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Category(SlowTest.class)
public class RepositoryPolicyViolationDAOTest
    extends AbstractDbDAOTest
{
  private RepositoryPolicyViolationDAO dao;

  private PolicyViolationConstraintFactsDAO constraintFactsDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createRepositoryPolicyViolationDAO();
    constraintFactsDAO = daoFactory.createPolicyViolationConstraintFactsDAO();
  }

  @Test
  public void testCRUD() throws Exception {
    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId());

    // Create
    ConstraintFact constraintFact = new ConstraintFact("constraintdata", "constraintdata", "constraintdata");
    Date now = new Date();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    RepositoryPolicyViolation policyViolation = new RepositoryPolicyViolation(repository.getId(), "path", now,
        policy.getId(), policy.getName(), 5, PolicyThreatCategory.LICENSE, "acacacacacac", componentIdentifier,
        List.of(constraintFact));
    assertThat(policyViolation.getId()).isNull();
    dao.insert(policyViolation);
    assertThat(policyViolation.getId()).isNotNull();

    // Test constraints stored
    assertThat(policyViolation.getConstraintFactsId()).isNotNull();
    PolicyViolationConstraintFacts facts = constraintFactsDAO.getById(policyViolation.getConstraintFactsId());
    ConstraintFact[] constraintFacts = JsonUtils.parse(facts.getConstraintFactsJson(), ConstraintFact[].class);
    assertThat(constraintFacts[0].getConstraintId()).isEqualTo(constraintFact.getConstraintId());

    // Read
    {
      RepositoryPolicyViolation persistedPolicyViolation = dao.getById(policyViolation.getId());
      assertThat(persistedPolicyViolation).isNotNull();
      assertPolicyViolation(repository.getId(), "path", policy.getId(), policy.getName(), 5,
          PolicyThreatCategory.LICENSE, "acacacacacac", componentIdentifier, now, null /* actionTypeId */,
          persistedPolicyViolation);
      assertThatExceptionOfType(IllegalStateException.class)
          .isThrownBy(() -> persistedPolicyViolation.getConstraintFactsJson())
          .withMessageContaining("Constraint facts are not loaded yet for policyViolationId=");
    }

    // Update
    policyViolation.setActionTypeId(Action.ID_FAIL);
    dao.update(policyViolation);

    // Read
    RepositoryPolicyViolation persistedpolicyViolation = dao.getById(policyViolation.getId());
    assertThat(persistedpolicyViolation).isNotNull();
    assertPolicyViolation(repository.getId(), "path", policy.getId(), policy.getName(), 5,
        PolicyThreatCategory.LICENSE, "acacacacacac", componentIdentifier, now, Action.ID_FAIL,
        persistedpolicyViolation);

    // Delete
    dao.delete(policyViolation);

    policyViolation = dao.getById(policyViolation.getId());
    assertThat(policyViolation).isNull();
  }

  private void assertPolicyViolation(
      String repositoryId,
      String pathname,
      String policyId,
      String policyName,
      int threatLevel,
      PolicyThreatCategory threatCategory,
      String hash,
      ComponentIdentifier componentIdentifier,
      Date time,
      String actionTypeId,
      RepositoryPolicyViolation actual)
  {
    assertThat(actual.getRepositoryId()).isEqualTo(repositoryId);
    assertThat(actual.getPathname()).isEqualTo(pathname);
    assertThat(actual.getPolicyId()).isEqualTo(policyId);
    assertThat(actual.getPolicyName()).isEqualTo(policyName);
    assertThat(actual.getThreatLevel()).isEqualTo(threatLevel);
    assertThat(actual.getThreatCategory()).isEqualTo(threatCategory);
    assertThat(actual.getHash()).isEqualTo(hash);
    assertThat(actual.getComponentIdentifier()).isEqualTo(componentIdentifier);
    assertThat(actual.getTime()).isEqualTo(time);
    assertThat(actual.getActionTypeId()).isEqualTo(actionTypeId);
  }

  @Test
  public void testGetActiveByRepositoryIdAndPathname_OrderByThreatLevelDesc_PolicyId() {
    final String pathname = "pathname";
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, pathname, null);

    final String policyIdSecond = "policyId2";
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 3, pathname, false, policyIdSecond,
        "policyName2", null);

    tempEntity.newRepositoryPolicyViolation(repository.getId(), 3, pathname, null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 2, pathname, null);

    final List<RepositoryPolicyViolation> violations = dao.getActiveByRepositoryIdAndPathname(repository.getId(),
        pathname);

    int i = 0;
    final RepositoryPolicyViolation firstViolation = violations.get(i++);
    assertThat(firstViolation.getThreatLevel()).isEqualTo(3);
    assertThat(firstViolation.getPolicyId()).isEqualTo("policyId");

    final RepositoryPolicyViolation secondViolation = violations.get(i++);
    assertThat(secondViolation.getThreatLevel()).isEqualTo(3);
    assertThat(secondViolation.getPolicyId()).isEqualTo(policyIdSecond);

    assertThat(violations.get(i++).getThreatLevel()).isEqualTo(2);
    assertThat(violations.get(i).getThreatLevel()).isEqualTo(1);
  }

  @Test
  public void testGetActiveByRepositoryIdAndPathnameAndWaived_WithTransactionContext() {
    final String pathname = "pathname";

    // Create non-waived violations
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 3, pathname, false, null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 2, pathname, false, null);

    // Create waived violations
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, pathname, true, null);

    try (TransactionContext tx = dao.createTransactionContext()) {
      // Test fetching non-waived violations
      final List<RepositoryPolicyViolation> nonWaivedViolations =
          dao.getActiveByRepositoryIdAndPathnameAndWaived(tx, repository.getId(), pathname, false);

      assertThat(nonWaivedViolations).hasSize(2);
      assertThat(nonWaivedViolations.get(0).getThreatLevel()).isEqualTo(3);
      assertThat(nonWaivedViolations.get(0).isWaived()).isFalse();
      assertThat(nonWaivedViolations.get(1).getThreatLevel()).isEqualTo(2);
      assertThat(nonWaivedViolations.get(1).isWaived()).isFalse();

      // Test fetching waived violations
      final List<RepositoryPolicyViolation> waivedViolations =
          dao.getActiveByRepositoryIdAndPathnameAndWaived(tx, repository.getId(), pathname, true);

      assertThat(waivedViolations).hasSize(1);
      assertThat(waivedViolations.get(0).getThreatLevel()).isEqualTo(1);
      assertThat(waivedViolations.get(0).isWaived()).isTrue();
    }
  }

  @Test
  public void testGetCount() {
    final String pathname = "pathname";
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, pathname, null);

    final String policyIdSecond = "policyId2";
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 3, pathname, false, policyIdSecond,
        "policyName2", null);

    tempEntity.newRepositoryPolicyViolation(repository.getId(), 3, pathname, null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 2, pathname, null);

    assertThat(dao.getCount()).isEqualTo(4);
  }

  @Test
  public void testDeleteByRepositoryId_H2() {
    assertThat(dao.isDatabaseEmbedded()).isTrue();

    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, "pathname1", null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, "pathname2", null);

    dao.deleteByRepositoryId(null /* TransactionContext */, repository.getId());

    assertThat(dao.getByRepositoryId(repository.getId())).isEmpty();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testDeleteByRepositoryId_Postgres() {
    assertThat(dao.isDatabaseEmbedded()).isFalse();

    repository = tempEntity.newRepository();

    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, "pathname1", null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, "pathname2", null);
    assertThat(dao.getByRepositoryId(repository.getId())).hasSize(2);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByRepositoryId(tx, repository.getId());
      tx.commit();
    }

    assertThat(dao.getByRepositoryId(repository.getId())).isEmpty();
  }

  @Test
  public void testGetRepositoryResultsDetailsNonAggregate_H2() {
    testGetRepositoryResultsDetailsNonAggregate();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetRepositoryResultsDetailsNotAggregate_Postgres() {
    assertThat(dao.isDatabaseEmbedded()).isFalse();
    testGetRepositoryResultsDetailsNonAggregate();
  }

  private void testGetRepositoryResultsDetailsNonAggregate() {
    Policy p1 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p1", 10);
    Policy p2 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p2", 1);
    Repository repository = tempEntity.newRepository();
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    RepositoryComponent c1 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g1/a1/v1/test-v1-c1.e1", "hash1",
            componentIdentifier1, false);
    RepositoryPolicyViolation c1v1 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), p1.getThreatLevel(), c1.getPathname(), false,
            p1.getId(), p1.getName(), c1.getComponentIdentifier());
    RepositoryPolicyViolation c1v2 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), p2.getThreatLevel(), c1.getPathname(), false,
            p2.getId(), p2.getName(), c1.getComponentIdentifier());
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2");
    RepositoryComponent c2 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g2/a2/v2/test-v2-c2.e2", "hash2",
            componentIdentifier2, false);
    RepositoryPolicyViolation c2v1 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), p1.getThreatLevel(), c2.getPathname(), false,
            p1.getId(), p1.getName(), c2.getComponentIdentifier());
    Set<String> repositoryIds = ImmutableSet.of(repository.getId());
    RepositoryResultsDetailsFilter repositoryResultsDetailsFilter = new RepositoryResultsDetailsFilter();
    repositoryResultsDetailsFilter.page = 1;
    repositoryResultsDetailsFilter.pageSize = 12;
    repositoryResultsDetailsFilter.violationStateFilters = new HashSet<>();
    repositoryResultsDetailsFilter.searchFilters = Collections.emptyMap();
    repositoryResultsDetailsFilter.matchStateFilter = "";
    repositoryResultsDetailsFilter.aggregate = false;

    List<RepositoryResultsDetails> repositoryResultsDetails =
        dao.getRepositoryResultsDetails(repositoryIds, repositoryResultsDetailsFilter);

    assertThat(repositoryResultsDetails)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields("constraintFactsJson")
        .containsExactlyInAnyOrder(
            toRepositoryResultsDetails(repository, c1, c1v1),
            toRepositoryResultsDetails(repository, c1, c1v2),
            toRepositoryResultsDetails(repository, c2, c2v1));
  }

  @Test
  public void testGetRepositoryResultsDetailsAggregate_H2() {
    testGetRepositoryResultsDetailsAggregate();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetRepositoryResultsDetailsAggregate_Postgres() {
    assertThat(dao.isDatabaseEmbedded()).isFalse();
    testGetRepositoryResultsDetailsAggregate();
  }

  private void testGetRepositoryResultsDetailsAggregate() {
    Policy p1 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p1", 10);
    Policy p2 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p2", 1);
    Repository repository = tempEntity.newRepository();
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    RepositoryComponent c1 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g1/a1/v1/test-v1-c1.e1", "hash1",
            componentIdentifier1, false);
    RepositoryPolicyViolation c1v1 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), p1.getThreatLevel(), c1.getPathname(), false,
            p1.getId(), p1.getName(), c1.getComponentIdentifier());
    tempEntity.newRepositoryPolicyViolation(repository.getId(), p2.getThreatLevel(), c1.getPathname(), false,
        p2.getId(), p2.getName(), c1.getComponentIdentifier());
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2");
    RepositoryComponent c2 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g2/a2/v2/test-v2-c2.e2", "hash2",
            componentIdentifier2, false);
    RepositoryPolicyViolation c2v1 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), p1.getThreatLevel(), c2.getPathname(), false,
            p1.getId(), p1.getName(), c2.getComponentIdentifier());
    Set<String> repositoryIds = ImmutableSet.of(repository.getId());
    RepositoryResultsDetailsFilter repositoryResultsDetailsFilter = new RepositoryResultsDetailsFilter();
    repositoryResultsDetailsFilter.page = 1;
    repositoryResultsDetailsFilter.pageSize = 12;
    repositoryResultsDetailsFilter.violationStateFilters = new HashSet<>();
    repositoryResultsDetailsFilter.searchFilters = Collections.emptyMap();
    repositoryResultsDetailsFilter.matchStateFilter = "";
    repositoryResultsDetailsFilter.aggregate = true;

    List<RepositoryResultsDetails> repositoryResultsDetails =
        dao.getRepositoryResultsDetails(repositoryIds, repositoryResultsDetailsFilter);

    assertThat(repositoryResultsDetails)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields("constraintFactsJson")
        .containsExactlyInAnyOrder(
            toRepositoryResultsDetailsWithoutWaived(repository, c1, c1v1),
            toRepositoryResultsDetailsWithoutWaived(repository, c2, c2v1));
  }

  @Test
  public void testGetRepositoryResultsDetails_FilterQuarantineTime_H2() {
    testGetRepositoryResultsDetails_FilterQuarantineTime();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetRepositoryResultsDetails_FilterQuarantineTime_Postgres() {
    assertThat(dao.isDatabaseEmbedded()).isFalse();
    testGetRepositoryResultsDetails_FilterQuarantineTime();
  }

  private void testGetRepositoryResultsDetails_FilterQuarantineTime() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "my-repo");
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p", 10);

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2");
    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3", "c3", "e3");

    LocalDate localDate1 = LocalDate.of(2023, 10, 19);
    LocalDate localDate2 = LocalDate.of(2023, 10, 18);
    ZoneId defaultZoneId = ZoneId.systemDefault();
    Date date1 = Date.from(localDate1.atStartOfDay(defaultZoneId).toInstant());
    Date date2 = Date.from(localDate2.atStartOfDay(defaultZoneId).toInstant());

    RepositoryComponent c1 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g1/a1/v1/test-v1-c1.e1", "hash1",
            componentIdentifier1, date1, date1);
    RepositoryComponent c2 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g2/a2/v2/test-v2-c2.e2", "hash2",
            componentIdentifier2, date2, date2);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g3/a3/v3/test-v3-c3.e3", "hash3",
        componentIdentifier3, false);

    RepositoryPolicyViolation c1v1 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getThreatLevel(), c1.getPathname(), false,
            policy.getId(), policy.getName(), c1.getComponentIdentifier());
    RepositoryPolicyViolation c2v1 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getThreatLevel(), c2.getPathname(), false,
            policy.getId(), policy.getName(), c2.getComponentIdentifier());
    Set<String> repositoryIds = ImmutableSet.of(repository.getId());

    RepositoryResultsDetailsFilter repositoryResultsDetailsFilter = new RepositoryResultsDetailsFilter();
    repositoryResultsDetailsFilter.page = 1;
    repositoryResultsDetailsFilter.pageSize = 12;
    repositoryResultsDetailsFilter.violationStateFilters = new HashSet<>();
    repositoryResultsDetailsFilter.searchFilters = new HashMap<>();
    repositoryResultsDetailsFilter.matchStateFilter = "";
    List<RepositoryResultsDetails> repositoryResultsDetails;

    repositoryResultsDetails = dao.getRepositoryResultsDetails(repositoryIds, repositoryResultsDetailsFilter);
    assertThat(repositoryResultsDetails)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields("constraintFactsJson")
        .containsExactlyInAnyOrder(
            toRepositoryResultsDetails(repository, c1, c1v1),
            toRepositoryResultsDetails(repository, c2, c2v1));

    repositoryResultsDetailsFilter.searchFilters.put("QUARANTINE_TIME", "19");
    repositoryResultsDetails = dao.getRepositoryResultsDetails(repositoryIds, repositoryResultsDetailsFilter);
    assertThat(repositoryResultsDetails)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields("constraintFactsJson")
        .containsExactly(
            toRepositoryResultsDetails(repository, c1, c1v1));

    repositoryResultsDetailsFilter.searchFilters.put("QUARANTINE_TIME", "18");
    repositoryResultsDetails = dao.getRepositoryResultsDetails(repositoryIds, repositoryResultsDetailsFilter);
    assertThat(repositoryResultsDetails)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields("constraintFactsJson")
        .containsExactly(
            toRepositoryResultsDetails(repository, c2, c2v1));
  }

  @Test
  public void testGetRepositoryResultsDetails_FilterThreatLevel_H2() {
    testGetRepositoryResultsDetails_FilterThreatLevel();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetRepositoryResultsDetails_FilterThreatLevel_Postgres() {
    assertThat(dao.isDatabaseEmbedded()).isFalse();
    testGetRepositoryResultsDetails_FilterThreatLevel();
  }

  @Test
  public void testGetPolicyViolationSummary() {
    PolicyViolationSummary policyViolationSummary;

    Policy p1 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p1", 10);
    Policy p2 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p2", 8);
    Policy p3 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p3", 5);
    Policy p4 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p4", 3);
    Policy p5 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p5", 2);
    Policy p6 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p6", 1);

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2");
    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3", "c3", "e3");
    ComponentIdentifier componentIdentifier4 = ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4", "c4", "e4");
    ComponentIdentifier componentIdentifier5 = ComponentIdentifier.createMavenCoordinates("g5", "a5", "v5", "c5", "e5");

    RepositoryComponent c1 = tempEntity.newRepositoryComponent(
        repository.getId(), MatchState.EXACT, "g1/a1/v1/test-v1-c1.e1", "hash1", componentIdentifier1, false);
    RepositoryComponent c2 = tempEntity.newRepositoryComponent(
        repository.getId(), MatchState.EXACT, "g2/a2/v2/test-v2-c2.e2", "hash1", componentIdentifier2, false);
    RepositoryComponent c3 = tempEntity.newRepositoryComponent(
        repository.getId(), MatchState.EXACT, "g3/a3/v3/test-v3-c3.e3", "hash1", componentIdentifier3, false);
    RepositoryComponent c4 = tempEntity.newRepositoryComponent(
        repository.getId(), MatchState.EXACT, "g4/a4/v4/test-v4-c4.e4", "hash1", componentIdentifier4, false);
    RepositoryComponent c5 = tempEntity.newRepositoryComponent(
        repository.getId(), MatchState.EXACT, "g5/a5/v5/test-v5-c5.e5", "hash1", componentIdentifier5, false);

    // c1 component has 2 critical policy violations
    tempEntity.newRepositoryPolicyViolation(repository.getId(), p1.getThreatLevel(), c1.getPathname(), false,
        p1.getId(), p1.getName(), c1.getComponentIdentifier());
    tempEntity.newRepositoryPolicyViolation(repository.getId(), p2.getThreatLevel(), c1.getPathname(), false,
        p2.getId(), p2.getName(), c1.getComponentIdentifier());
    policyViolationSummary = dao.getPolicyViolationSummary(repository.getId());
    assertThat(policyViolationSummary.getCriticalCount()).isEqualTo(1);
    assertThat(policyViolationSummary.getSevereCount()).isEqualTo(0);
    assertThat(policyViolationSummary.getModerateCount()).isEqualTo(0);
    assertThat(policyViolationSummary.getAffectedComponentCount()).isEqualTo(1);

    // c2 component has 1 severe policy violation
    tempEntity.newRepositoryPolicyViolation(repository.getId(), p3.getThreatLevel(), c2.getPathname(), false,
        p3.getId(), p3.getName(), c2.getComponentIdentifier());
    policyViolationSummary = dao.getPolicyViolationSummary(repository.getId());
    assertThat(policyViolationSummary.getCriticalCount()).isEqualTo(1);
    assertThat(policyViolationSummary.getSevereCount()).isEqualTo(1);
    assertThat(policyViolationSummary.getModerateCount()).isEqualTo(0);
    assertThat(policyViolationSummary.getAffectedComponentCount()).isEqualTo(2);

    // c3 component has 1 moderate policy violation
    tempEntity.newRepositoryPolicyViolation(repository.getId(), p4.getThreatLevel(), c3.getPathname(), false,
        p4.getId(), p4.getName(), c3.getComponentIdentifier());
    policyViolationSummary = dao.getPolicyViolationSummary(repository.getId());
    assertThat(policyViolationSummary.getCriticalCount()).isEqualTo(1);
    assertThat(policyViolationSummary.getSevereCount()).isEqualTo(1);
    assertThat(policyViolationSummary.getModerateCount()).isEqualTo(1);
    assertThat(policyViolationSummary.getAffectedComponentCount()).isEqualTo(3);

    // c4 component has 1 moderate policy violation
    tempEntity.newRepositoryPolicyViolation(repository.getId(), p5.getThreatLevel(), c4.getPathname(), false,
        p5.getId(), p5.getName(), c4.getComponentIdentifier());
    policyViolationSummary = dao.getPolicyViolationSummary(repository.getId());
    assertThat(policyViolationSummary.getCriticalCount()).isEqualTo(1);
    assertThat(policyViolationSummary.getSevereCount()).isEqualTo(1);
    assertThat(policyViolationSummary.getModerateCount()).isEqualTo(2);
    assertThat(policyViolationSummary.getAffectedComponentCount()).isEqualTo(4);

    // c5 component has 1 minor policy violation (we don't count it)
    tempEntity.newRepositoryPolicyViolation(repository.getId(), p6.getThreatLevel(), c5.getPathname(), false,
        p6.getId(), p6.getName(), c5.getComponentIdentifier());
    policyViolationSummary = dao.getPolicyViolationSummary(repository.getId());
    assertThat(policyViolationSummary.getCriticalCount()).isEqualTo(1);
    assertThat(policyViolationSummary.getSevereCount()).isEqualTo(1);
    assertThat(policyViolationSummary.getModerateCount()).isEqualTo(2);
    assertThat(policyViolationSummary.getAffectedComponentCount()).isEqualTo(4);

    // verify that violations for a different repository are not counted
    Repository repository2 = tempEntity.newRepository(repositoryManager);
    tempEntity.newRepositoryPolicyViolation(repository2.getId(), p1.getThreatLevel(), c1.getPathname(), false,
        p1.getId(), p1.getName(), c1.getComponentIdentifier());
    policyViolationSummary = dao.getPolicyViolationSummary(repository.getId());
    assertThat(policyViolationSummary.getCriticalCount()).isEqualTo(1);
    assertThat(policyViolationSummary.getSevereCount()).isEqualTo(1);
    assertThat(policyViolationSummary.getModerateCount()).isEqualTo(2);
    assertThat(policyViolationSummary.getAffectedComponentCount()).isEqualTo(4);
  }

  private void testGetRepositoryResultsDetails_FilterThreatLevel() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "my-repo");
    Policy p1 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p1", 10);
    Policy p2 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p2", 5);
    Policy p3 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p3", 1);
    Policy p4 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p4", 0);

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");

    RepositoryComponent c1 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g1/a1/v1/test-v1-c1.e1", "hash1",
            componentIdentifier1, false);

    RepositoryPolicyViolation c1v1 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), p1.getThreatLevel(), c1.getPathname(), false,
            p1.getId(), p1.getName(), c1.getComponentIdentifier());
    RepositoryPolicyViolation c1v2 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), p2.getThreatLevel(), c1.getPathname(), false,
            p2.getId(), p2.getName(), c1.getComponentIdentifier());
    RepositoryPolicyViolation c1v3 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), p3.getThreatLevel(), c1.getPathname(), false,
            p3.getId(), p3.getName(), c1.getComponentIdentifier());
    RepositoryPolicyViolation c1v4 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), p4.getThreatLevel(), c1.getPathname(), false,
            p4.getId(), p4.getName(), c1.getComponentIdentifier());
    Set<String> repositoryIds = ImmutableSet.of(repository.getId());

    RepositoryResultsDetailsFilter repositoryResultsDetailsFilter = new RepositoryResultsDetailsFilter();
    repositoryResultsDetailsFilter.page = 1;
    repositoryResultsDetailsFilter.pageSize = 12;
    repositoryResultsDetailsFilter.violationStateFilters = new HashSet<>();
    repositoryResultsDetailsFilter.searchFilters = new HashMap<>();
    repositoryResultsDetailsFilter.matchStateFilter = "";
    List<RepositoryResultsDetails> repositoryResultsDetails;

    repositoryResultsDetails = dao.getRepositoryResultsDetails(repositoryIds, repositoryResultsDetailsFilter);
    assertThat(repositoryResultsDetails)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields("constraintFactsJson")
        .containsExactlyInAnyOrder(
            toRepositoryResultsDetails(repository, c1, c1v1),
            toRepositoryResultsDetails(repository, c1, c1v2),
            toRepositoryResultsDetails(repository, c1, c1v3),
            toRepositoryResultsDetails(repository, c1, c1v4));

    repositoryResultsDetailsFilter.threatLevelFilters = Arrays.asList(5, 5);
    repositoryResultsDetails = dao.getRepositoryResultsDetails(repositoryIds, repositoryResultsDetailsFilter);
    assertThat(repositoryResultsDetails)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields("constraintFactsJson")
        .containsExactlyInAnyOrder(
            toRepositoryResultsDetails(repository, c1, c1v2));

    repositoryResultsDetailsFilter.threatLevelFilters = Arrays.asList(5, 10);
    repositoryResultsDetails = dao.getRepositoryResultsDetails(repositoryIds, repositoryResultsDetailsFilter);
    assertThat(repositoryResultsDetails)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields("constraintFactsJson")
        .containsExactlyInAnyOrder(
            toRepositoryResultsDetails(repository, c1, c1v1),
            toRepositoryResultsDetails(repository, c1, c1v2));
  }

  @Test
  public void testCountRepositoryResultsDetails_noFilters_H2() {
    testCountRepositoryResultsDetails_noFilters();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testCountRepositoryResultsDetails_noFilters_Postgres() {
    assertThat(dao.isDatabaseEmbedded()).isFalse();
    testCountRepositoryResultsDetails_noFilters();
  }

  private void testCountRepositoryResultsDetails_noFilters() {
    Policy p1 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p1", 10);
    Policy p2 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p2", 5);
    Repository repository = tempEntity.newRepository();

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    RepositoryComponent c1 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g1/a1/v1/test-v1-c1.e1", "hash1",
            componentIdentifier1, false);

    // 2 open violations
    tempEntity.newRepositoryPolicyViolation(repository.getId(), p1.getThreatLevel(), c1.getPathname(), false,
        p1.getId(), p1.getName(), c1.getComponentIdentifier());
    tempEntity.newRepositoryPolicyViolation(repository.getId(), p2.getThreatLevel(), c1.getPathname(), false,
        p2.getId(), p2.getName(), c1.getComponentIdentifier());

    Set<String> repositoryIds = ImmutableSet.of(repository.getId());
    RepositoryResultsDetailsFilter filter = new RepositoryResultsDetailsFilter();
    filter.page = 1;
    filter.pageSize = 12;
    filter.violationStateFilters = new HashSet<>();
    filter.searchFilters = Collections.emptyMap();
    filter.matchStateFilter = "";
    filter.aggregate = false;

    RepositoryResultsCountSummary countSummary = dao.countRepositoryResultsDetails(repositoryIds, filter);

    assertThat(countSummary.totalCount).isEqualTo(2);
    assertThat(countSummary.openCount).isEqualTo(2);
    assertThat(countSummary.waivedCount).isEqualTo(0);
    assertThat(countSummary.quarantinedCount).isEqualTo(0);
  }

  @Test
  public void testCountRepositoryResultsDetails_withWaivedViolations_H2() {
    testCountRepositoryResultsDetails_withWaivedViolations();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testCountRepositoryResultsDetails_withWaivedViolations_Postgres() {
    assertThat(dao.isDatabaseEmbedded()).isFalse();
    testCountRepositoryResultsDetails_withWaivedViolations();
  }

  private void testCountRepositoryResultsDetails_withWaivedViolations() {
    Policy p1 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p1", 10);
    Policy p2 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p2", 5);
    Repository repository = tempEntity.newRepository();

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    RepositoryComponent c1 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g1/a1/v1/test-v1-c1.e1", "hash1",
            componentIdentifier1, false);

    // 1 open, 1 waived
    tempEntity.newRepositoryPolicyViolation(repository.getId(), p1.getThreatLevel(), c1.getPathname(), false,
        p1.getId(), p1.getName(), c1.getComponentIdentifier());
    tempEntity.newRepositoryPolicyViolation(repository.getId(), p2.getThreatLevel(), c1.getPathname(), true,
        p2.getId(), p2.getName(), c1.getComponentIdentifier());

    Set<String> repositoryIds = ImmutableSet.of(repository.getId());
    RepositoryResultsDetailsFilter filter = new RepositoryResultsDetailsFilter();
    filter.page = 1;
    filter.pageSize = 12;
    filter.violationStateFilters = new HashSet<>();
    filter.searchFilters = Collections.emptyMap();
    filter.matchStateFilter = "";
    filter.aggregate = false;

    RepositoryResultsCountSummary countSummary = dao.countRepositoryResultsDetails(repositoryIds, filter);

    assertThat(countSummary.totalCount).isEqualTo(2);
    assertThat(countSummary.openCount).isEqualTo(1);
    assertThat(countSummary.waivedCount).isEqualTo(1);
    assertThat(countSummary.quarantinedCount).isEqualTo(0);
  }

  @Test
  public void testCountRepositoryResultsDetails_withQuarantinedViolations_H2() {
    testCountRepositoryResultsDetails_withQuarantinedViolations();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testCountRepositoryResultsDetails_withQuarantinedViolations_Postgres() {
    assertThat(dao.isDatabaseEmbedded()).isFalse();
    testCountRepositoryResultsDetails_withQuarantinedViolations();
  }

  private void testCountRepositoryResultsDetails_withQuarantinedViolations() {
    Policy p1 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p1", 10);
    Repository repository = tempEntity.newRepository();

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    // Quarantined component (quarantine_time set, unquarantine_time null)
    RepositoryComponent c1 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g1/a1/v1/test-v1-c1.e1", "hash1",
            componentIdentifier1, new Date(), new Date());

    // Violation with fail action (required for quarantine count)
    tempEntity.newRepositoryPolicyViolation(repository.getId(), p1.getThreatLevel(), c1.getPathname(), false,
        Action.ID_FAIL, p1.getId(), p1.getName(), c1.getComponentIdentifier());

    Set<String> repositoryIds = ImmutableSet.of(repository.getId());
    RepositoryResultsDetailsFilter filter = new RepositoryResultsDetailsFilter();
    filter.page = 1;
    filter.pageSize = 12;
    filter.violationStateFilters = new HashSet<>();
    filter.searchFilters = Collections.emptyMap();
    filter.matchStateFilter = "";
    filter.aggregate = false;

    RepositoryResultsCountSummary countSummary = dao.countRepositoryResultsDetails(repositoryIds, filter);

    assertThat(countSummary.totalCount).isEqualTo(1);
    assertThat(countSummary.openCount).isEqualTo(1);
    assertThat(countSummary.waivedCount).isEqualTo(0);
    assertThat(countSummary.quarantinedCount).isEqualTo(1);
  }

  @Test
  public void testCountRepositoryResultsDetails_withThreatLevelFilter_H2() {
    testCountRepositoryResultsDetails_withThreatLevelFilter();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testCountRepositoryResultsDetails_withThreatLevelFilter_Postgres() {
    assertThat(dao.isDatabaseEmbedded()).isFalse();
    testCountRepositoryResultsDetails_withThreatLevelFilter();
  }

  private void testCountRepositoryResultsDetails_withThreatLevelFilter() {
    Policy p1 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p1", 10);
    Policy p2 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p2", 5);
    Policy p3 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p3", 1);
    Repository repository = tempEntity.newRepository();

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    RepositoryComponent c1 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g1/a1/v1/test-v1-c1.e1", "hash1",
            componentIdentifier1, false);

    // 3 violations at different threat levels
    tempEntity.newRepositoryPolicyViolation(repository.getId(), p1.getThreatLevel(), c1.getPathname(), false,
        p1.getId(), p1.getName(), c1.getComponentIdentifier());
    tempEntity.newRepositoryPolicyViolation(repository.getId(), p2.getThreatLevel(), c1.getPathname(), false,
        p2.getId(), p2.getName(), c1.getComponentIdentifier());
    tempEntity.newRepositoryPolicyViolation(repository.getId(), p3.getThreatLevel(), c1.getPathname(), false,
        p3.getId(), p3.getName(), c1.getComponentIdentifier());

    Set<String> repositoryIds = ImmutableSet.of(repository.getId());
    RepositoryResultsDetailsFilter filter = new RepositoryResultsDetailsFilter();
    filter.page = 1;
    filter.pageSize = 12;
    filter.violationStateFilters = new HashSet<>();
    filter.searchFilters = Collections.emptyMap();
    filter.matchStateFilter = "";
    filter.aggregate = false;
    // Filter to threat level 5-10 (excludes threat level 1)
    filter.threatLevelFilters = Arrays.asList(5, 10);

    RepositoryResultsCountSummary countSummary = dao.countRepositoryResultsDetails(repositoryIds, filter);

    assertThat(countSummary.totalCount).isEqualTo(2);
    assertThat(countSummary.openCount).isEqualTo(2);
    assertThat(countSummary.waivedCount).isEqualTo(0);
    assertThat(countSummary.quarantinedCount).isEqualTo(0);
  }

  @Test
  public void testCountRepositoryResultsDetails_emptyResults_H2() {
    testCountRepositoryResultsDetails_emptyResults();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testCountRepositoryResultsDetails_emptyResults_Postgres() {
    assertThat(dao.isDatabaseEmbedded()).isFalse();
    testCountRepositoryResultsDetails_emptyResults();
  }

  private void testCountRepositoryResultsDetails_emptyResults() {
    Repository repository = tempEntity.newRepository();

    Set<String> repositoryIds = ImmutableSet.of(repository.getId());
    RepositoryResultsDetailsFilter filter = new RepositoryResultsDetailsFilter();
    filter.page = 1;
    filter.pageSize = 12;
    filter.violationStateFilters = new HashSet<>();
    filter.searchFilters = Collections.emptyMap();
    filter.matchStateFilter = "";
    filter.aggregate = false;

    RepositoryResultsCountSummary countSummary = dao.countRepositoryResultsDetails(repositoryIds, filter);

    assertThat(countSummary.totalCount).isEqualTo(0);
    assertThat(countSummary.openCount).isEqualTo(0);
    assertThat(countSummary.waivedCount).isEqualTo(0);
    assertThat(countSummary.quarantinedCount).isEqualTo(0);
  }

  private RepositoryResultsDetails toRepositoryResultsDetailsWithoutWaived(
      Repository repository,
      RepositoryComponent repositoryComponent,
      RepositoryPolicyViolation repositoryPolicyViolation)
  {
    RepositoryResultsDetails result =
        toRepositoryResultsDetails(repository, repositoryComponent, repositoryPolicyViolation);
    result.waived = null;
    result.policyViolationId = null; // Aggregate queries don't return violation ID
    return result;
  }

  private RepositoryResultsDetails toRepositoryResultsDetails(
      Repository repository,
      RepositoryComponent repositoryComponent,
      RepositoryPolicyViolation repositoryPolicyViolation)
  {
    return new RepositoryResultsDetails(
        repositoryPolicyViolation.getThreatLevel(),
        repositoryPolicyViolation.getPolicyName(),
        repository.getRepositoryManagerId(),
        repository.getId(),
        repositoryComponent.getComponentIdentifier().getFormat(),
        repositoryComponent.getPathname(),
        ComponentIdentifierAdapter.toJson(repositoryComponent.getComponentIdentifier().getCoordinates()),
        repositoryComponent.getDisplayName(),
        repositoryComponent.getHash(),
        repositoryComponent.getMatchStateId(),
        (repositoryComponent.getQuarantineTime() != null &&
            repositoryComponent.getUnquarantineTime() == null) ? repositoryComponent.getQuarantineTime() : null,
        repositoryPolicyViolation.isWaived(),
        null, // constraintFactsJson - tested separately, excluded from filter tests
        repositoryPolicyViolation.getId());
  }
}
