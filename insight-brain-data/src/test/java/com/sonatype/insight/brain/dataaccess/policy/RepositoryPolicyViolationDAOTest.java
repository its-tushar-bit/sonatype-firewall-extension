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
import java.util.Map;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
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

  private void assertPolicyViolation(String repositoryId,
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
    repositoryResultsDetailsFilter.formatExclusionPatterns = Collections.emptyMap();

    List<RepositoryResultsDetails> repositoryResultsDetails =
        dao.getRepositoryResultsDetails(repositoryIds, repositoryResultsDetailsFilter);

    assertThat(repositoryResultsDetails).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(
        toRepositoryResultsDetails(repository, c1, c1v1),
        toRepositoryResultsDetails(repository, c1, c1v2),
        toRepositoryResultsDetails(repository, c2, c2v1)
    );
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
    repositoryResultsDetailsFilter.formatExclusionPatterns = Collections.emptyMap();

    List<RepositoryResultsDetails> repositoryResultsDetails =
        dao.getRepositoryResultsDetails(repositoryIds, repositoryResultsDetailsFilter);

    assertThat(repositoryResultsDetails).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(
        toRepositoryResultsDetailsWithoutWaived(repository, c1, c1v1),
        toRepositoryResultsDetailsWithoutWaived(repository, c2, c2v1)
    );
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
    repositoryResultsDetailsFilter.formatExclusionPatterns = Collections.emptyMap();
    List<RepositoryResultsDetails> repositoryResultsDetails;

    repositoryResultsDetails = dao.getRepositoryResultsDetails(repositoryIds, repositoryResultsDetailsFilter);
    assertThat(repositoryResultsDetails).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(
        toRepositoryResultsDetails(repository, c1, c1v1),
        toRepositoryResultsDetails(repository, c2, c2v1)
    );

    repositoryResultsDetailsFilter.searchFilters.put("QUARANTINE_TIME", "19");
    repositoryResultsDetails = dao.getRepositoryResultsDetails(repositoryIds, repositoryResultsDetailsFilter);
    assertThat(repositoryResultsDetails).usingRecursiveFieldByFieldElementComparator().containsExactly(
        toRepositoryResultsDetails(repository, c1, c1v1)
    );

    repositoryResultsDetailsFilter.searchFilters.put("QUARANTINE_TIME", "18");
    repositoryResultsDetails = dao.getRepositoryResultsDetails(repositoryIds, repositoryResultsDetailsFilter);
    assertThat(repositoryResultsDetails).usingRecursiveFieldByFieldElementComparator().containsExactly(
        toRepositoryResultsDetails(repository, c2, c2v1)
    );
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
    repositoryResultsDetailsFilter.formatExclusionPatterns = Collections.emptyMap();
    List<RepositoryResultsDetails> repositoryResultsDetails;

    repositoryResultsDetails = dao.getRepositoryResultsDetails(repositoryIds, repositoryResultsDetailsFilter);
    assertThat(repositoryResultsDetails).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(
        toRepositoryResultsDetails(repository, c1, c1v1),
        toRepositoryResultsDetails(repository, c1, c1v2),
        toRepositoryResultsDetails(repository, c1, c1v3),
        toRepositoryResultsDetails(repository, c1, c1v4)
    );

    repositoryResultsDetailsFilter.threatLevelFilters = Arrays.asList(5, 5);
    repositoryResultsDetails = dao.getRepositoryResultsDetails(repositoryIds, repositoryResultsDetailsFilter);
    assertThat(repositoryResultsDetails).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(
        toRepositoryResultsDetails(repository, c1, c1v2)
    );

    repositoryResultsDetailsFilter.threatLevelFilters = Arrays.asList(5, 10);
    repositoryResultsDetails = dao.getRepositoryResultsDetails(repositoryIds, repositoryResultsDetailsFilter);
    assertThat(repositoryResultsDetails).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(
        toRepositoryResultsDetails(repository, c1, c1v1),
        toRepositoryResultsDetails(repository, c1, c1v2)
    );
  }

  private RepositoryResultsDetails toRepositoryResultsDetailsWithoutWaived(
      Repository repository,
      RepositoryComponent repositoryComponent,
      RepositoryPolicyViolation repositoryPolicyViolation)
  {
    RepositoryResultsDetails result =
        toRepositoryResultsDetails(repository, repositoryComponent, repositoryPolicyViolation);
    result.waived = null;
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
        repositoryPolicyViolation.isWaived()
    );
  }

  @Test
  public void testGetRepositoryResultsDetails_ExcludesNuGetJsonFiles_H2() {
    testGetRepositoryResultsDetails_ExcludesNuGetJsonFiles();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetRepositoryResultsDetails_ExcludesNuGetJsonFiles_Postgres() {
    assertThat(dao.isDatabaseEmbedded()).isFalse();
    testGetRepositoryResultsDetails_ExcludesNuGetJsonFiles();
  }

  private void testGetRepositoryResultsDetails_ExcludesNuGetJsonFiles() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository nugetRepository = tempEntity.newRepository(repositoryManager, "nuget-repo", "nuget");
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "test-policy", 10);

    // Create NuGet components - one JSON file (should be excluded) and one DLL file (should be included)
    ComponentIdentifier jsonComponent = ComponentIdentifier.createNugetCoordinates("TestPackage", "1.0.0");
    ComponentIdentifier dllComponent = ComponentIdentifier.createNugetCoordinates("TestPackage", "1.0.0");

    RepositoryComponent jsonComp = tempEntity.newRepositoryComponent(
        nugetRepository.getId(), MatchState.EXACT, "testpackage/1.0.0/testpackage.1.0.0.json", "hash1",
        jsonComponent, false);
    RepositoryComponent dllComp = tempEntity.newRepositoryComponent(
        nugetRepository.getId(), MatchState.EXACT, "testpackage/1.0.0/testpackage.dll", "hash2",
        dllComponent, false);

    // Create policy violations for both components
    tempEntity.newRepositoryPolicyViolation(
        nugetRepository.getId(), policy.getThreatLevel(), jsonComp.getPathname(), false,
        policy.getId(), policy.getName(), jsonComp.getComponentIdentifier());
    tempEntity.newRepositoryPolicyViolation(
        nugetRepository.getId(), policy.getThreatLevel(), dllComp.getPathname(), false,
        policy.getId(), policy.getName(), dllComp.getComponentIdentifier());

    // Query for repository results
    Set<String> repositoryIds = ImmutableSet.of(nugetRepository.getId());
    RepositoryResultsDetailsFilter filter = new RepositoryResultsDetailsFilter();
    filter.page = 1;
    filter.pageSize = 10;
    filter.violationStateFilters = new HashSet<>();
    filter.searchFilters = Collections.emptyMap();
    filter.matchStateFilter = "";
    filter.aggregate = false;
    filter.formatExclusionPatterns = Map.of("nuget", List.of("%.json"));

    List<RepositoryResultsDetails> results = dao.getRepositoryResultsDetails(repositoryIds, filter);

    // Verify that only the DLL component is returned (JSON should be filtered out)
    assertThat(results).hasSize(1);
    assertThat(results.get(0).pathname).isEqualTo(dllComp.getPathname());
    assertThat(results.get(0).pathname).doesNotContain(".json");
  }

  /**
   * Integration test to verify NuGet JSON files are excluded from aggregated repository results
   */
  @Test
  public void testGetRepositoryResultsDetailsAggregate_ExcludesNuGetJsonFiles_H2() {
    testGetRepositoryResultsDetailsAggregate_ExcludesNuGetJsonFiles();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetRepositoryResultsDetailsAggregate_ExcludesNuGetJsonFiles_Postgres() {
    assertThat(dao.isDatabaseEmbedded()).isFalse();
    testGetRepositoryResultsDetailsAggregate_ExcludesNuGetJsonFiles();
  }

  private void testGetRepositoryResultsDetailsAggregate_ExcludesNuGetJsonFiles() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository nugetRepository = tempEntity.newRepository(repositoryManager, "nuget-repo", "nuget");
    Policy p1 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "policy1", 10);
    Policy p2 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "policy2", 8);

    // Create NuGet components - one JSON file (should be excluded) and one DLL file (should be included)
    ComponentIdentifier jsonComponent = ComponentIdentifier.createNugetCoordinates("TestPackage", "1.0.0");
    ComponentIdentifier dllComponent = ComponentIdentifier.createNugetCoordinates("TestPackage", "1.0.0");

    RepositoryComponent jsonComp = tempEntity.newRepositoryComponent(
        nugetRepository.getId(), MatchState.EXACT, "testpackage/1.0.0/testpackage.1.0.0.json", "hash1",
        jsonComponent, false);
    RepositoryComponent dllComp = tempEntity.newRepositoryComponent(
        nugetRepository.getId(), MatchState.EXACT, "testpackage/1.0.0/testpackage.dll", "hash2",
        dllComponent, false);

    // Create multiple policy violations for both components
    tempEntity.newRepositoryPolicyViolation(nugetRepository.getId(), p1.getThreatLevel(), jsonComp.getPathname(),
        false, p1.getId(), p1.getName(), jsonComp.getComponentIdentifier());
    tempEntity.newRepositoryPolicyViolation(nugetRepository.getId(), p2.getThreatLevel(), jsonComp.getPathname(),
        false, p2.getId(), p2.getName(), jsonComp.getComponentIdentifier());
    tempEntity.newRepositoryPolicyViolation(
        nugetRepository.getId(), p1.getThreatLevel(), dllComp.getPathname(), false,
        p1.getId(), p1.getName(), dllComp.getComponentIdentifier());

    // Query for aggregated repository results
    Set<String> repositoryIds = ImmutableSet.of(nugetRepository.getId());
    RepositoryResultsDetailsFilter filter = new RepositoryResultsDetailsFilter();
    filter.page = 1;
    filter.pageSize = 10;
    filter.violationStateFilters = new HashSet<>();
    filter.searchFilters = Collections.emptyMap();
    filter.matchStateFilter = "";
    filter.aggregate = true;
    filter.formatExclusionPatterns = Map.of("nuget", List.of("%.json"));

    List<RepositoryResultsDetails> results = dao.getRepositoryResultsDetails(repositoryIds, filter);

    // Verify that only the DLL component is returned (JSON should be filtered out even with multiple violations)
    assertThat(results).hasSize(1);
    assertThat(results.get(0).pathname).isEqualTo(dllComp.getPathname());
    assertThat(results.get(0).pathname).doesNotContain(".json");
  }

  /**
   * Test that non-NuGet repositories are not affected by the JSON exclusion filter
   */
  @Test
  public void testGetRepositoryResultsDetails_NonNuGetRepositoriesNotAffected() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository mavenRepository = tempEntity.newRepository(repositoryManager, "maven-repo", "maven");
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "test-policy", 10);

    // Create a Maven component with .json extension (should NOT be filtered)
    ComponentIdentifier jsonComponent = ComponentIdentifier.createMavenCoordinates("com.test", "artifact", "1.0.0");
    RepositoryComponent jsonComp = tempEntity.newRepositoryComponent(
        mavenRepository.getId(), MatchState.EXACT, "com/test/artifact/1.0.0/artifact-1.0.0.json", "hash1",
        jsonComponent, false);

    tempEntity.newRepositoryPolicyViolation(
        mavenRepository.getId(), policy.getThreatLevel(), jsonComp.getPathname(), false,
        policy.getId(), policy.getName(), jsonComp.getComponentIdentifier());

    // Query for repository results
    Set<String> repositoryIds = ImmutableSet.of(mavenRepository.getId());
    RepositoryResultsDetailsFilter filter = new RepositoryResultsDetailsFilter();
    filter.page = 1;
    filter.pageSize = 10;
    filter.violationStateFilters = new HashSet<>();
    filter.searchFilters = Collections.emptyMap();
    filter.matchStateFilter = "";
    filter.aggregate = false;
    // No format exclusions for Maven - JSON files should be included
    filter.formatExclusionPatterns = Collections.emptyMap();

    List<RepositoryResultsDetails> results = dao.getRepositoryResultsDetails(repositoryIds, filter);

    // Verify that Maven JSON file is NOT filtered out
    assertThat(results).hasSize(1);
    assertThat(results.get(0).pathname).isEqualTo(jsonComp.getPathname());
    assertThat(results.get(0).pathname).contains(".json");
  }
}
