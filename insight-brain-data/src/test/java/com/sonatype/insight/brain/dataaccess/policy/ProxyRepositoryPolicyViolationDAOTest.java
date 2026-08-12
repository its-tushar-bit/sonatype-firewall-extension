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
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.json.store.JsonUtils;

import org.jooq.SQLDialect;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import com.google.common.collect.ImmutableSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Category(SlowTest.class)
public class ProxyRepositoryPolicyViolationDAOTest
    extends AbstractDbDAOTest
{
  private ProxyRepositoryPolicyViolationDAO dao;

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
    ProxyRepositoryPolicyViolation policyViolation = new ProxyRepositoryPolicyViolation(repository.getId(), "path", now,
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
      ProxyRepositoryPolicyViolation persistedPolicyViolation = dao.getById(policyViolation.getId());
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
    ProxyRepositoryPolicyViolation persistedpolicyViolation = dao.getById(policyViolation.getId());
    assertThat(persistedpolicyViolation).isNotNull();
    assertPolicyViolation(repository.getId(), "path", policy.getId(), policy.getName(), 5,
        PolicyThreatCategory.LICENSE, "acacacacacac", componentIdentifier, now, Action.ID_FAIL,
        persistedpolicyViolation);

    // Delete
    dao.delete(policyViolation);

    policyViolation = dao.getById(policyViolation.getId());
    assertThat(policyViolation).isNull();
  }

  @Test
  public void testInsertBatch_persistsAllAndStoresConstraintFacts_H2() {
    testInsertBatch_persistsAllAndStoresConstraintFacts();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testInsertBatch_persistsAllAndStoresConstraintFacts_Postgres() {
    assertThat(dao.isDatabaseEmbedded()).isFalse();
    assertThatDialectIs(SQLDialect.POSTGRES);
    testInsertBatch_persistsAllAndStoresConstraintFacts();
  }

  private void testInsertBatch_persistsAllAndStoresConstraintFacts() {
    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId());
    Date now = new Date();
    ProxyRepositoryPolicyViolation v1 =
        newViolation(policy, "p1", 5, PolicyThreatCategory.LICENSE, "h1", "1", "c1", now);
    ProxyRepositoryPolicyViolation v2 =
        newViolation(policy, "p2", 3, PolicyThreatCategory.SECURITY, "h2", "2", "c2", now);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.insertBatch(tx, List.of(v1, v2), false);
      tx.commit();
    }

    assertThat(v1.getId()).isNotNull();
    assertThat(v2.getId()).isNotNull();
    assertThat(v1.getConstraintFactsId()).isNotNull();
    assertThat(v2.getConstraintFactsId()).isNotNull();
    assertThat(dao.getById(v1.getId())).isNotNull();
    assertThat(dao.getById(v2.getId())).isNotNull();
  }

  @Test
  public void testUpdateBatch_persistsChangesForAllEntries_H2() {
    testUpdateBatch_persistsChangesForAllEntries();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testUpdateBatch_persistsChangesForAllEntries_Postgres() {
    assertThat(dao.isDatabaseEmbedded()).isFalse();
    assertThatDialectIs(SQLDialect.POSTGRES);
    testUpdateBatch_persistsChangesForAllEntries();
  }

  private void testUpdateBatch_persistsChangesForAllEntries() {
    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId());
    Date now = new Date();
    ProxyRepositoryPolicyViolation v1 =
        newViolation(policy, "p1", 5, PolicyThreatCategory.LICENSE, "h1", "1", "c1", now);
    ProxyRepositoryPolicyViolation v2 =
        newViolation(policy, "p2", 3, PolicyThreatCategory.SECURITY, "h2", "2", "c2", now);
    dao.insert(v1);
    dao.insert(v2);

    v1.setActionTypeId(Action.ID_FAIL);
    v2.setActionTypeId(Action.ID_WARN);
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.updateBatch(tx, List.of(v1, v2));
      tx.commit();
    }

    assertThat(dao.getById(v1.getId()).getActionTypeId()).isEqualTo(Action.ID_FAIL);
    assertThat(dao.getById(v2.getId()).getActionTypeId()).isEqualTo(Action.ID_WARN);
  }

  @Test
  public void testDeleteBatch_removesSelectedViolationsPreservingOthers_H2() {
    testDeleteBatch_removesSelectedViolationsPreservingOthers();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testDeleteBatch_removesSelectedViolationsPreservingOthers_Postgres() {
    assertThat(dao.isDatabaseEmbedded()).isFalse();
    assertThatDialectIs(SQLDialect.POSTGRES);
    testDeleteBatch_removesSelectedViolationsPreservingOthers();
  }

  private void testDeleteBatch_removesSelectedViolationsPreservingOthers() {
    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId());
    Date now = new Date();
    ProxyRepositoryPolicyViolation keep =
        newViolation(policy, "keep", 5, PolicyThreatCategory.LICENSE, "hk", "k", "ck", now);
    ProxyRepositoryPolicyViolation del1 =
        newViolation(policy, "del1", 3, PolicyThreatCategory.SECURITY, "hd1", "d1", "cd1", now);
    ProxyRepositoryPolicyViolation del2 =
        newViolation(policy, "del2", 4, PolicyThreatCategory.SECURITY, "hd2", "d2", "cd2", now);
    dao.insert(keep);
    dao.insert(del1);
    dao.insert(del2);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteBatch(tx, List.of(del1, del2));
      tx.commit();
    }

    assertThat(dao.getById(keep.getId())).isNotNull();
    assertThat(dao.getById(del1.getId())).isNull();
    assertThat(dao.getById(del2.getId())).isNull();
  }

  @Test
  public void testDeleteBatch_emptyList_noOp() {
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteBatch(tx, Collections.emptyList());
      tx.commit();
    }
  }

  private void assertThatDialectIs(SQLDialect expected) {
    try (TransactionContext tx = dao.createTransactionContext()) {
      assertThat(tx.dsl().dialect()).isEqualTo(expected);
    }
  }

  private ProxyRepositoryPolicyViolation newViolation(
      Policy policy,
      String pathname,
      int threatLevel,
      PolicyThreatCategory category,
      String hash,
      String versionSuffix,
      String constraintData,
      Date time)
  {
    return new ProxyRepositoryPolicyViolation(repository.getId(), pathname, time,
        policy.getId(), policy.getName(), threatLevel, category, hash,
        ComponentIdentifier.createMavenCoordinates("g", "a", versionSuffix),
        List.of(new ConstraintFact(constraintData, constraintData, constraintData)));
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
      ProxyRepositoryPolicyViolation actual)
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

    final List<ProxyRepositoryPolicyViolation> violations = dao.getActiveByRepositoryIdAndPathname(repository.getId(),
        pathname);

    int i = 0;
    final ProxyRepositoryPolicyViolation firstViolation = violations.get(i++);
    assertThat(firstViolation.getThreatLevel()).isEqualTo(3);
    assertThat(firstViolation.getPolicyId()).isEqualTo("policyId");

    final ProxyRepositoryPolicyViolation secondViolation = violations.get(i++);
    assertThat(secondViolation.getThreatLevel()).isEqualTo(3);
    assertThat(secondViolation.getPolicyId()).isEqualTo(policyIdSecond);

    assertThat(violations.get(i++).getThreatLevel()).isEqualTo(2);
    assertThat(violations.get(i).getThreatLevel()).isEqualTo(1);
  }

  // ---- getActiveByRepositoryIdAndPathnameOrInnerPathnames (CLM-40943 archive-of-archives fan-out) ----

  @Test
  public void testGetActiveByRepositoryIdAndPathnameOrInnerPathnames_returnsBothOuterAndInnerViolations() {
    String outer = "archive/archive/3/archive-3.zip";
    String innerLog4j = outer + "!/log4j-core-2.14.1.jar";
    String innerCli = outer + "!/commons-cli-1.9.0.jar";
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 2, outer, null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 10, innerLog4j, false, "p-log4j", "Security-Critical",
        null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, innerCli, false, "p-cli", "Architecture-Quality",
        null);

    List<ProxyRepositoryPolicyViolation> all =
        dao.getActiveByRepositoryIdAndPathnameOrInnerPathnames(repository.getId(), outer);

    assertThat(all).hasSize(3);
    assertThat(all).extracting(ProxyRepositoryPolicyViolation::getPathname)
        .containsExactlyInAnyOrder(outer, innerLog4j, innerCli);
  }

  @Test
  public void testGetActiveByRepositoryIdAndPathnameOrInnerPathnames_excludesUnrelatedPathnames() {
    String outer = "a/a/1/a-1.zip";
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, outer, null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, outer + "!/inner.jar", null);
    // Different outer that happens to start with the same letters — must NOT match.
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, "a/a/1/a-1-other.zip!/x.jar", null);

    List<ProxyRepositoryPolicyViolation> result =
        dao.getActiveByRepositoryIdAndPathnameOrInnerPathnames(repository.getId(), outer);

    assertThat(result).hasSize(2);
    assertThat(result).extracting(ProxyRepositoryPolicyViolation::getPathname)
        .containsExactlyInAnyOrder(outer, outer + "!/inner.jar");
  }

  @Test
  public void testGetActiveByRepositoryIdAndPathnameOrInnerPathnames_excludesViolationsForOtherRepositories() {
    // Confirms the repositoryId predicate is honored — a same-shape pathname under a different
    // repository must not leak through. (active=false is documented as deprecated/cleanup-only,
    // see ProxyRepositoryPolicyViolation.active javadoc, so a per-repository test gives broader
    // coverage of the WHERE clause without depending on a setter that no longer exists.)
    String outer = "x/x/1/x-1.zip";
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, outer, null);
    Repository otherRepo = tempEntity.newRepository(repositoryManager);
    tempEntity.newRepositoryPolicyViolation(otherRepo.getId(), 5, outer + "!/inner.jar", null);

    List<ProxyRepositoryPolicyViolation> result =
        dao.getActiveByRepositoryIdAndPathnameOrInnerPathnames(repository.getId(), outer);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPathname()).isEqualTo(outer);
  }

  @Test
  public void testGetActiveByRepositoryIdAndPathnameOrInnerPathnames_pathnameWithPercentLiteral_escapedCorrectly() {
    // The DAO escapes %, _ and \ in the LIKE prefix so that a real outer pathname containing
    // these characters does not turn into a wildcard. Without escaping, "outer%/" would match
    // any string starting with "outer" plus any prefix — including the unrelated "outerX!/y.jar".
    String outer = "weird%pct/file.zip";
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, outer, null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, outer + "!/inner.jar", null);
    // This row would match a naive (un-escaped) LIKE 'weird%pct/file.zip!/%' because the literal
    // '%' is wildcarded — it only matches when escaping is correct.
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, "weirdABCpct/file.zip!/x.jar", null);

    List<ProxyRepositoryPolicyViolation> result =
        dao.getActiveByRepositoryIdAndPathnameOrInnerPathnames(repository.getId(), outer);

    assertThat(result).extracting(ProxyRepositoryPolicyViolation::getPathname)
        .containsExactlyInAnyOrder(outer, outer + "!/inner.jar");
  }

  @Test
  public void testGetActiveByRepositoryIdAndPathnameOrInnerPathnames_pathnameWithUnderscoreLiteral_escapedCorrectly() {
    String outer = "lib_under/file.zip";
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, outer, null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, outer + "!/inner.jar", null);
    // Without escaping, '_' would match any single character so 'libXunder/file.zip!/x.jar'
    // would also be returned.
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, "libXunder/file.zip!/x.jar", null);

    List<ProxyRepositoryPolicyViolation> result =
        dao.getActiveByRepositoryIdAndPathnameOrInnerPathnames(repository.getId(), outer);

    assertThat(result).extracting(ProxyRepositoryPolicyViolation::getPathname)
        .containsExactlyInAnyOrder(outer, outer + "!/inner.jar");
  }

  @Test
  public void testGetActiveByRepositoryIdAndPathnameOrInnerPathnames_nullRepositoryId_returnsEmpty() {
    assertThat(dao.getActiveByRepositoryIdAndPathnameOrInnerPathnames(null, "x")).isEmpty();
  }

  @Test
  public void testGetActiveByRepositoryIdAndPathnameOrInnerPathnames_nullPathname_returnsEmpty() {
    assertThat(dao.getActiveByRepositoryIdAndPathnameOrInnerPathnames(repository.getId(), null)).isEmpty();
  }

  @Test
  public void testGetActiveByRepositoryIdAndPathnameOrInnerPathnames_orderedByPathnameThenThreatLevel() {
    String outer = "ord/ord/1/ord-1.zip";
    String innerA = outer + "!/a.jar";
    String innerB = outer + "!/b.jar";
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, outer, null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 9, innerA, false, "p-A-hi", "Security-High", null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 3, innerA, false, "p-A-lo", "Security-Low", null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 7, innerB, false, "p-B-hi", "Security-Med", null);

    List<ProxyRepositoryPolicyViolation> result =
        dao.getActiveByRepositoryIdAndPathnameOrInnerPathnames(repository.getId(), outer);

    // Ordering: pathname asc, then threatLevel desc, then policyId asc — matches the DAO's
    // .orderBy clause. Lexicographically the outer pathname sorts first because it's a prefix of
    // every inner (a shorter string is less than any longer one starting with it under standard
    // SQL collation, regardless of whether '!' or '/' compares first as a glyph). Then the two
    // innerA rows in threat-desc order, then innerB.
    assertThat(result).extracting(ProxyRepositoryPolicyViolation::getPathname)
        .containsExactly(
            outer, innerA, innerA, innerB);
    // Outer's only violation has threat=5; first innerA row has threat=9 (desc), then threat=3.
    assertThat(result.get(0).getThreatLevel()).isEqualTo(5);
    assertThat(result.get(1).getThreatLevel()).isEqualTo(9);
    assertThat(result.get(2).getThreatLevel()).isEqualTo(3);
    assertThat(result.get(3).getThreatLevel()).isEqualTo(7);
  }

  // ---- getActiveByRepositoryIdAndPathnamesOrInnerPathnames batch variant (CLM-40943 Defect 5) ----

  @Test
  public void testGetActiveByRepositoryIdAndPathnamesOrInnerPathnames_batchRollsUpInnersPerOuter() {
    String outer1 = "g/a1/1/a1-1.zip";
    String outer2 = "g/a2/1/a2-1.zip";
    String outer3UnusedNoMatch = "g/a3/1/a3-1.zip";
    String inner1A = outer1 + "!/log4j-core.jar";
    String inner1B = outer1 + "!/commons-cli.jar";
    String inner2 = outer2 + "!/jackson.jar";
    String unrelated = "g/other/1/other-1.jar";

    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, outer1, null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 9, inner1A, null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 3, inner1B, null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 7, outer2, null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 8, inner2, null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 4, unrelated, null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 6, outer3UnusedNoMatch, null);

    List<ProxyRepositoryPolicyViolation> result =
        dao.getActiveByRepositoryIdAndPathnamesOrInnerPathnames(
            repository.getId(), java.util.List.of(outer1, outer2));

    // The batch query returns: outer1 + inner1A + inner1B + outer2 + inner2 (five rows).
    // Unrelated row and outer3 (not in the input list) are excluded.
    assertThat(result).extracting(ProxyRepositoryPolicyViolation::getPathname)
        .containsExactlyInAnyOrder(outer1, inner1A, inner1B, outer2, inner2);
  }

  @Test
  public void testGetActiveByRepositoryIdAndPathnamesOrInnerPathnames_emptyInputReturnsEmpty() {
    assertThat(dao.getActiveByRepositoryIdAndPathnamesOrInnerPathnames(
        repository.getId(), java.util.List.of())).isEmpty();
    assertThat(dao.getActiveByRepositoryIdAndPathnamesOrInnerPathnames(
        repository.getId(), null)).isEmpty();
    assertThat(dao.getActiveByRepositoryIdAndPathnamesOrInnerPathnames(
        null, java.util.List.of("a"))).isEmpty();
  }

  @Test
  public void testGetActiveByRepositoryIdAndPathnamesOrInnerPathnames_nullsInListSkipped() {
    String outer = "g/a/1/a-1.zip";
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, outer, null);

    List<String> input = new java.util.ArrayList<>();
    input.add(null);
    input.add(outer);
    input.add(null);

    List<ProxyRepositoryPolicyViolation> result =
        dao.getActiveByRepositoryIdAndPathnamesOrInnerPathnames(repository.getId(), input);

    assertThat(result).extracting(ProxyRepositoryPolicyViolation::getPathname).containsExactly(outer);
  }

  @Test
  public void testGetActiveByRepositoryIdAndPathnamesOrInnerPathnames_prefixIsolatedAcrossOuters() {
    // Ensures the LIKE pattern for outer1 does not bleed into outer2's namespace.
    String outer1 = "g/a/1/a-1.zip";
    String outer1Inner = outer1 + "!/x.jar";
    String outer1Look = outer1 + "Suffix/extra.jar"; // not a true inner — different outer
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, outer1, null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 6, outer1Inner, null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 7, outer1Look, null);

    List<ProxyRepositoryPolicyViolation> result =
        dao.getActiveByRepositoryIdAndPathnamesOrInnerPathnames(repository.getId(),
            java.util.List.of(outer1));

    // The "!/x.jar" inner matches; the "Suffix/extra.jar" sibling that lacks "!/" does NOT.
    assertThat(result).extracting(ProxyRepositoryPolicyViolation::getPathname)
        .containsExactlyInAnyOrder(outer1, outer1Inner);
  }

  // ---- getPolicyViolationSummary rolls inner pathnames under outer (CLM-40943 Defect 5) ----

  @Test
  public void testGetPolicyViolationSummary_rollsInnerThreatLevelIntoOuter() {
    String outer = "archive/archive/1/archive-1.zip";
    String innerCritical = outer + "!/log4j-core-2.14.1.jar";
    // Outer's own violation is only severe (5); the inner is critical (9). After rollup the
    // outer's tier reflects the worst threat anywhere inside the archive = critical.
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, outer, null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 9, innerCritical, null);

    PolicyViolationSummary summary = dao.getPolicyViolationSummary(repository.getId());

    assertThat(summary.getCriticalCount()).isEqualTo(1L);
    assertThat(summary.getSevereCount()).isEqualTo(0L);
    assertThat(summary.getModerateCount()).isEqualTo(0L);
  }

  @Test
  public void testGetPolicyViolationSummary_innerOnlyOuterCountsOnce() {
    // Outer has zero rows of its own — all violations are on inner pathnames. The outer must
    // still be counted as one affected component, tier=max(inner threats).
    String outer = "archive/archive/2/archive-2.zip";
    String innerCritical = outer + "!/x.jar";
    String innerSevere = outer + "!/y.jar";
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 9, innerCritical, null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, innerSevere, null);

    PolicyViolationSummary summary = dao.getPolicyViolationSummary(repository.getId());

    assertThat(summary.getCriticalCount()).isEqualTo(1L);
    assertThat(summary.getSevereCount()).isEqualTo(0L);
  }

  @Test
  public void testGetPolicyViolationSummary_pureOuterUnchanged() {
    // Regression guard: a non-archive single-jar with one severe violation must still produce a
    // severe count of 1. Pre-CLM-40943 behavior for pathnames without "!/" is preserved.
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, "g/jar/1/jar-1.jar", null);

    PolicyViolationSummary summary = dao.getPolicyViolationSummary(repository.getId());

    assertThat(summary.getCriticalCount()).isEqualTo(0L);
    assertThat(summary.getSevereCount()).isEqualTo(1L);
    assertThat(summary.getModerateCount()).isEqualTo(0L);
  }

  @Test
  public void testHasActiveMalwareWaivedViolation_returnsTrueWhenActiveWaivedViolationExistsForPathname() {
    final String pathname = "pathname";

    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, pathname, true, "policyId",
        "Security-Malicious", null /* componentIdentifier */);

    assertThat(dao.hasActiveMalwareWaivedViolation(repository.getId(), pathname)).isTrue();
  }

  @Test
  public void testHasActiveMalwareWaivedViolation_returnsFalseWhenViolationIsNotWaived() {
    final String pathname = "pathname";

    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, pathname, false, "policyId",
        "Security-Malicious", null /* componentIdentifier */);

    assertThat(dao.hasActiveMalwareWaivedViolation(repository.getId(), pathname)).isFalse();
  }

  @Test
  public void testHasActiveMalwareWaivedViolation_returnsFalseWhenPathnameDoesNotMatch() {
    final String pathname = "pathname";

    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, pathname, true, "policyId",
        "Security-Malicious", null /* componentIdentifier */);

    assertThat(dao.hasActiveMalwareWaivedViolation(repository.getId(), "differentpathname")).isFalse();
  }

  @Test
  public void testHasActiveMalwareWaivedViolation_returnsFalseForDifferentRepository() {
    final String pathname = "pathname";

    Repository otherRepository = tempEntity.newRepository(repositoryManager);
    tempEntity.newRepositoryPolicyViolation(otherRepository.getId(), 5, pathname, true, "policyId",
        "Security-Malicious", null /* componentIdentifier */);

    assertThat(dao.hasActiveMalwareWaivedViolation(repository.getId(), pathname)).isFalse();
  }

  @Test
  public void testHasActiveMalwareWaivedViolation_returnsFalseWhenWaivedViolationIsNotMalware() {
    final String pathname = "pathname";

    // A waived license violation must not be treated as a malware waiver
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, pathname, true, "policyId",
        "License-Use-Prohibited", null /* componentIdentifier */);

    assertThat(dao.hasActiveMalwareWaivedViolation(repository.getId(), pathname)).isFalse();
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
      final List<ProxyRepositoryPolicyViolation> nonWaivedViolations =
          dao.getActiveByRepositoryIdAndPathnameAndWaived(tx, repository.getId(), pathname, false);

      assertThat(nonWaivedViolations).hasSize(2);
      assertThat(nonWaivedViolations.get(0).getThreatLevel()).isEqualTo(3);
      assertThat(nonWaivedViolations.get(0).isWaived()).isFalse();
      assertThat(nonWaivedViolations.get(1).getThreatLevel()).isEqualTo(2);
      assertThat(nonWaivedViolations.get(1).isWaived()).isFalse();

      // Test fetching waived violations
      final List<ProxyRepositoryPolicyViolation> waivedViolations =
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
    ProxyRepositoryComponent c1 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g1/a1/v1/test-v1-c1.e1", "hash1",
            componentIdentifier1, false);
    ProxyRepositoryPolicyViolation c1v1 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), p1.getThreatLevel(), c1.getPathname(), false,
            p1.getId(), p1.getName(), c1.getComponentIdentifier());
    ProxyRepositoryPolicyViolation c1v2 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), p2.getThreatLevel(), c1.getPathname(), false,
            p2.getId(), p2.getName(), c1.getComponentIdentifier());
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2");
    ProxyRepositoryComponent c2 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g2/a2/v2/test-v2-c2.e2", "hash2",
            componentIdentifier2, false);
    ProxyRepositoryPolicyViolation c2v1 =
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
    ProxyRepositoryComponent c1 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g1/a1/v1/test-v1-c1.e1", "hash1",
            componentIdentifier1, false);
    ProxyRepositoryPolicyViolation c1v1 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), p1.getThreatLevel(), c1.getPathname(), false,
            p1.getId(), p1.getName(), c1.getComponentIdentifier());
    tempEntity.newRepositoryPolicyViolation(repository.getId(), p2.getThreatLevel(), c1.getPathname(), false,
        p2.getId(), p2.getName(), c1.getComponentIdentifier());
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2");
    ProxyRepositoryComponent c2 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g2/a2/v2/test-v2-c2.e2", "hash2",
            componentIdentifier2, false);
    ProxyRepositoryPolicyViolation c2v1 =
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

    ProxyRepositoryComponent c1 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g1/a1/v1/test-v1-c1.e1", "hash1",
            componentIdentifier1, date1, date1);
    ProxyRepositoryComponent c2 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g2/a2/v2/test-v2-c2.e2", "hash2",
            componentIdentifier2, date2, date2);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g3/a3/v3/test-v3-c3.e3", "hash3",
        componentIdentifier3, false);

    ProxyRepositoryPolicyViolation c1v1 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getThreatLevel(), c1.getPathname(), false,
            policy.getId(), policy.getName(), c1.getComponentIdentifier());
    ProxyRepositoryPolicyViolation c2v1 =
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
  public void testGetRepositoryResultsDetails_FilterEvaluationTime_H2() {
    testGetRepositoryResultsDetails_FilterEvaluationTime();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetRepositoryResultsDetails_FilterEvaluationTime_Postgres() {
    assertThat(dao.isDatabaseEmbedded()).isFalse();
    testGetRepositoryResultsDetails_FilterEvaluationTime();
  }

  private void testGetRepositoryResultsDetails_FilterEvaluationTime() {
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

    ProxyRepositoryComponent c1 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g1/a1/v1/test-v1-c1.e1", "hash1",
            componentIdentifier1, date1, null);
    ProxyRepositoryComponent c2 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g2/a2/v2/test-v2-c2.e2", "hash2",
            componentIdentifier2, date2, null);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g3/a3/v3/test-v3-c3.e3", "hash3",
        componentIdentifier3, false);

    ProxyRepositoryPolicyViolation c1v1 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getThreatLevel(), c1.getPathname(), false,
            policy.getId(), policy.getName(), c1.getComponentIdentifier());
    ProxyRepositoryPolicyViolation c2v1 =
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

    repositoryResultsDetailsFilter.searchFilters.put("EVALUATION_TIME", "19");
    repositoryResultsDetails = dao.getRepositoryResultsDetails(repositoryIds, repositoryResultsDetailsFilter);
    assertThat(repositoryResultsDetails)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields("constraintFactsJson")
        .containsExactly(
            toRepositoryResultsDetails(repository, c1, c1v1));

    repositoryResultsDetailsFilter.searchFilters.put("EVALUATION_TIME", "18");
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

    ProxyRepositoryComponent c1 = tempEntity.newRepositoryComponent(
        repository.getId(), MatchState.EXACT, "g1/a1/v1/test-v1-c1.e1", "hash1", componentIdentifier1, false);
    ProxyRepositoryComponent c2 = tempEntity.newRepositoryComponent(
        repository.getId(), MatchState.EXACT, "g2/a2/v2/test-v2-c2.e2", "hash1", componentIdentifier2, false);
    ProxyRepositoryComponent c3 = tempEntity.newRepositoryComponent(
        repository.getId(), MatchState.EXACT, "g3/a3/v3/test-v3-c3.e3", "hash1", componentIdentifier3, false);
    ProxyRepositoryComponent c4 = tempEntity.newRepositoryComponent(
        repository.getId(), MatchState.EXACT, "g4/a4/v4/test-v4-c4.e4", "hash1", componentIdentifier4, false);
    ProxyRepositoryComponent c5 = tempEntity.newRepositoryComponent(
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

    ProxyRepositoryComponent c1 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g1/a1/v1/test-v1-c1.e1", "hash1",
            componentIdentifier1, false);

    ProxyRepositoryPolicyViolation c1v1 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), p1.getThreatLevel(), c1.getPathname(), false,
            p1.getId(), p1.getName(), c1.getComponentIdentifier());
    ProxyRepositoryPolicyViolation c1v2 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), p2.getThreatLevel(), c1.getPathname(), false,
            p2.getId(), p2.getName(), c1.getComponentIdentifier());
    ProxyRepositoryPolicyViolation c1v3 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), p3.getThreatLevel(), c1.getPathname(), false,
            p3.getId(), p3.getName(), c1.getComponentIdentifier());
    ProxyRepositoryPolicyViolation c1v4 =
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
    ProxyRepositoryComponent c1 =
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
    ProxyRepositoryComponent c1 =
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
    ProxyRepositoryComponent c1 =
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
    ProxyRepositoryComponent c1 =
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
      ProxyRepositoryComponent proxyRepositoryComponent,
      ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation)
  {
    RepositoryResultsDetails result =
        toRepositoryResultsDetails(repository, proxyRepositoryComponent, proxyRepositoryPolicyViolation);
    result.waived = null;
    result.policyViolationId = null; // Aggregate queries don't return violation ID
    return result;
  }

  private RepositoryResultsDetails toRepositoryResultsDetails(
      Repository repository,
      ProxyRepositoryComponent proxyRepositoryComponent,
      ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation)
  {
    return new RepositoryResultsDetails(
        proxyRepositoryPolicyViolation.getThreatLevel(),
        proxyRepositoryPolicyViolation.getPolicyName(),
        repository.getRepositoryManagerId(),
        repository.getId(),
        proxyRepositoryComponent.getComponentIdentifier().getFormat(),
        proxyRepositoryComponent.getPathname(),
        ComponentIdentifierAdapter.toJson(proxyRepositoryComponent.getComponentIdentifier().getCoordinates()),
        proxyRepositoryComponent.getDisplayName(),
        proxyRepositoryComponent.getHash(),
        proxyRepositoryComponent.getMatchStateId(),
        proxyRepositoryComponent.getLastEvaluationTime(),
        (proxyRepositoryComponent.getQuarantineTime() != null &&
            proxyRepositoryComponent.getUnquarantineTime() == null)
                ? proxyRepositoryComponent.getQuarantineTime()
                : null,
        proxyRepositoryPolicyViolation.isWaived(),
        null, // constraintFactsJson - tested separately, excluded from filter tests
        proxyRepositoryPolicyViolation.getId());
  }

  @Test
  public void testPaginationStability_NonAggregate_H2() {
    testPaginationStability_NonAggregate();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testPaginationStability_NonAggregate_Postgres() {
    assertThat(dao.isDatabaseEmbedded()).isFalse();
    testPaginationStability_NonAggregate();
  }

  private void testPaginationStability_NonAggregate() {
    // Create 25 violations with IDENTICAL threat levels to test worst-case pagination scenario
    // This reproduces the Django components bug where all violations have the same sort values
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Test Policy", 10);

    // Create 25 components with violations, all with same threat level
    for (int i = 1; i <= 25; i++) {
      ComponentIdentifier componentId =
          ComponentIdentifier.createMavenCoordinates("com.example", "component-" + i, "1.0.0");
      ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(
          repository.getId(),
          MatchState.EXACT,
          "com/example/component-" + i + "/1.0.0/component-" + i + "-1.0.0.jar",
          "hash-" + i,
          componentId,
          false);

      tempEntity.newRepositoryPolicyViolation(
          repository.getId(),
          policy.getThreatLevel(), // All violations have same threat level (10)
          component.getPathname(),
          false,
          policy.getId(),
          policy.getName(),
          component.getComponentIdentifier());
    }

    // Query with pageSize=12 to force pagination across 3 pages
    RepositoryResultsDetailsFilter filter = new RepositoryResultsDetailsFilter();
    filter.page = 1;
    filter.pageSize = 12;
    filter.violationStateFilters = new HashSet<>();
    filter.searchFilters = Collections.emptyMap();
    filter.matchStateFilter = "";
    filter.aggregate = false;
    filter.sortFields = Arrays
        .asList(createSortField(RepositoryResultsDetailsFilter.SortField.SortableField.POLICY_THREAT_LEVEL, false, 1));

    Set<String> repositoryIds = ImmutableSet.of(repository.getId());

    // Fetch three pages
    // Note: DAO returns pageSize+1 (13) to detect "has more pages", but we only check first pageSize (12) records
    filter.page = 1;
    List<RepositoryResultsDetails> page1All = dao.getRepositoryResultsDetails(repositoryIds, filter);
    List<RepositoryResultsDetails> page1 = page1All.subList(0, Math.min(filter.pageSize, page1All.size()));

    filter.page = 2;
    List<RepositoryResultsDetails> page2All = dao.getRepositoryResultsDetails(repositoryIds, filter);
    List<RepositoryResultsDetails> page2 = page2All.subList(0, Math.min(filter.pageSize, page2All.size()));

    filter.page = 3;
    List<RepositoryResultsDetails> page3All = dao.getRepositoryResultsDetails(repositoryIds, filter);
    List<RepositoryResultsDetails> page3 = page3All.subList(0, Math.min(filter.pageSize, page3All.size()));

    // Extract policyViolationIds from each page
    Set<String> page1Ids = page1.stream().map(d -> d.policyViolationId).collect(java.util.stream.Collectors.toSet());
    Set<String> page2Ids = page2.stream().map(d -> d.policyViolationId).collect(java.util.stream.Collectors.toSet());
    Set<String> page3Ids = page3.stream().map(d -> d.policyViolationId).collect(java.util.stream.Collectors.toSet());

    // CRITICAL ASSERTION: No policyViolationId should appear on multiple pages
    Set<String> page1And2Overlap = new HashSet<>(page1Ids);
    page1And2Overlap.retainAll(page2Ids);
    assertThat(page1And2Overlap).as("No duplicates between page 1 and page 2").isEmpty();

    Set<String> page2And3Overlap = new HashSet<>(page2Ids);
    page2And3Overlap.retainAll(page3Ids);
    assertThat(page2And3Overlap).as("No duplicates between page 2 and page 3").isEmpty();

    Set<String> page1And3Overlap = new HashSet<>(page1Ids);
    page1And3Overlap.retainAll(page3Ids);
    assertThat(page1And3Overlap).as("No duplicates between page 1 and page 3").isEmpty();

    // Verify all 25 violations are present exactly once across all pages
    Set<String> allIds = new HashSet<>();
    allIds.addAll(page1Ids);
    allIds.addAll(page2Ids);
    allIds.addAll(page3Ids);
    assertThat(allIds).hasSize(25);
  }

  @Test
  public void testPaginationStability_Aggregate_H2() {
    testPaginationStability_Aggregate();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testPaginationStability_Aggregate_Postgres() {
    assertThat(dao.isDatabaseEmbedded()).isFalse();
    testPaginationStability_Aggregate();
  }

  private void testPaginationStability_Aggregate() {
    // Test aggregate mode pagination stability with identical sort values
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Test Policy", 10);

    // Create 12 components with 2-3 violations each (30 total violations)
    // All violations have identical threat level to test worst-case pagination
    for (int i = 1; i <= 12; i++) {
      ComponentIdentifier componentId =
          ComponentIdentifier.createMavenCoordinates("com.example", "component-" + i, "1.0.0");
      ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(
          repository.getId(),
          MatchState.EXACT,
          "com/example/component-" + i + "/1.0.0/component-" + i + "-1.0.0.jar",
          "hash-" + i,
          componentId,
          false);

      // Create 2-3 violations per component
      int violationCount = (i % 2 == 0) ? 2 : 3;
      for (int j = 1; j <= violationCount; j++) {
        tempEntity.newRepositoryPolicyViolation(
            repository.getId(),
            policy.getThreatLevel(), // All violations have same threat level (10)
            component.getPathname(),
            false,
            policy.getId(),
            policy.getName(),
            component.getComponentIdentifier());
      }
    }

    // Query in aggregate mode with pageSize=12
    RepositoryResultsDetailsFilter filter = new RepositoryResultsDetailsFilter();
    filter.page = 1;
    filter.pageSize = 12;
    filter.violationStateFilters = new HashSet<>();
    filter.searchFilters = Collections.emptyMap();
    filter.matchStateFilter = "";
    filter.aggregate = true; // Aggregate mode - group by component
    filter.sortFields = Arrays
        .asList(createSortField(RepositoryResultsDetailsFilter.SortField.SortableField.POLICY_THREAT_LEVEL, false, 1));

    Set<String> repositoryIds = ImmutableSet.of(repository.getId());

    // Fetch two pages (should get all 12 components)
    filter.page = 1;
    List<RepositoryResultsDetails> page1All = dao.getRepositoryResultsDetails(repositoryIds, filter);
    List<RepositoryResultsDetails> page1 = page1All.subList(0, Math.min(filter.pageSize, page1All.size()));

    filter.page = 2;
    List<RepositoryResultsDetails> page2All = dao.getRepositoryResultsDetails(repositoryIds, filter);
    List<RepositoryResultsDetails> page2 = page2All.subList(0, Math.min(filter.pageSize, page2All.size()));

    // In aggregate mode, use pathname as unique identifier (violation ID is null)
    Set<String> page1Pathnames = page1.stream().map(d -> d.pathname).collect(java.util.stream.Collectors.toSet());
    Set<String> page2Pathnames = page2.stream().map(d -> d.pathname).collect(java.util.stream.Collectors.toSet());

    // CRITICAL ASSERTION: No pathname should appear on multiple pages
    Set<String> overlap = new HashSet<>(page1Pathnames);
    overlap.retainAll(page2Pathnames);
    assertThat(overlap).as("No duplicates between pages in aggregate mode").isEmpty();

    // Verify all 12 components present exactly once
    Set<String> allPathnames = new HashSet<>();
    allPathnames.addAll(page1Pathnames);
    allPathnames.addAll(page2Pathnames);
    assertThat(allPathnames).hasSize(12);
  }

  private RepositoryResultsDetailsFilter.SortField createSortField(
      RepositoryResultsDetailsFilter.SortField.SortableField field,
      boolean asc,
      int priority)
  {
    RepositoryResultsDetailsFilter.SortField sortField = new RepositoryResultsDetailsFilter.SortField();
    sortField.sortableField = field;
    sortField.asc = asc;
    sortField.sortPriority = priority;
    return sortField;
  }
}
