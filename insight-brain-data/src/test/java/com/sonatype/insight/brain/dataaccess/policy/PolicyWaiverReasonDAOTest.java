/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyWaiverReasonDAOTest
    extends AbstractDbDAOTest
{
  private PolicyWaiverReasonDAO policyWaiverReasonDAO;

  public static final PolicyWaiverReason ACKNOWLEDGED_VIOLATION_WAIVER_REASON = new PolicyWaiverReason(
      "9b704ef5bc064fc29d7fe08a251ee9a6", "system", "Acknowledged violation", 10);

  public static final PolicyWaiverReason EVALUATING_COMPONENT_WAIVER_REASON = new PolicyWaiverReason(
      "ab704ef5bc064fc29d7fe08a251ee9aa", "system", "Evaluating component", 15);

  public static final PolicyWaiverReason MITIGATED_EXTERNALLY_WAIVER_REASON = new PolicyWaiverReason(
      "42069f58114f4df8b435a40a415d2835", "system", "Mitigated externally", 20);

  public static final PolicyWaiverReason NO_UPGRADE_PATH_WAIVER_REASON = new PolicyWaiverReason(
      "39984de3d6e64f508df82b4cbfd72f70", "system", "No upgrade path", 30);

  public static final PolicyWaiverReason NOT_EXPLOITABLE_WAIVER_REASON = new PolicyWaiverReason(
      "f6990a32cd8d4ea78853ca829d948927", "system", "Not exploitable", 40);

  public static final PolicyWaiverReason NOT_REACHABLE_WAIVER_REASON = new PolicyWaiverReason(
      "19bbf1a7d591497698ab3172461d971a", "system", "Not reachable", 50);

  public static final PolicyWaiverReason RESEARCHING_WAIVER_REASON = new PolicyWaiverReason(
      "3446e70e60e04676a90131f3dea9bdb5", "system", "Researching", 60);

  public static final PolicyWaiverReason OTHER_WAIVER_REASON = new PolicyWaiverReason(
      "c991ef95866d4903ad0c6c217ac47c07", "system", "Other", 70);

  @Before
  @Override
  public void setup() {
    super.setup();
    policyWaiverReasonDAO = daoFactory.createPolicyWaiverReasonDAO();
  }

  @Test
  public void testGetAllByIds() {
    PolicyWaiverReason policyWaiverReason1 = tempEntity.newWaiverReason("type1", "because reasons 1");
    PolicyWaiverReason policyWaiverReason2 = tempEntity.newWaiverReason("type2", "because reasons 2");
    PolicyWaiverReason policyWaiverReason3 = tempEntity.newWaiverReason("type3", "because reasons 3");

    List<PolicyWaiverReason> policyWaiverReasons = policyWaiverReasonDAO.getAllByIds(
        Arrays.asList(policyWaiverReason1.getId(), policyWaiverReason2.getId(), policyWaiverReason3.getId()));

    assertThat(policyWaiverReasons.stream().map(PolicyWaiverReason::getId))
        .containsExactlyInAnyOrder(
            policyWaiverReason1.getId(), policyWaiverReason2.getId(), policyWaiverReason3.getId());
    assertThat(policyWaiverReasons.stream().map(PolicyWaiverReason::getType))
        .containsExactlyInAnyOrder(
            policyWaiverReason1.getType(), policyWaiverReason2.getType(), policyWaiverReason3.getType());
    assertThat(policyWaiverReasons.stream().map(PolicyWaiverReason::getReasonText))
        .containsExactlyInAnyOrder(
            policyWaiverReason1.getReasonText(), policyWaiverReason2.getReasonText(),
            policyWaiverReason3.getReasonText());
  }

  @Test
  public void testGetPolicyWaiverReasonIdToPolicyWaiverReasonMap() {
    final var policyWaiverReason1 = tempEntity.newWaiverReason("type1", "because reasons 1");

    final var results = policyWaiverReasonDAO.getPolicyWaiverReasonIdToPolicyWaiverReasonMap();

    assertThat(results.size()).isEqualTo(9);
    assertPolicyWaiverReasonsEqual(results.get(policyWaiverReason1.getId()), policyWaiverReason1);
    assertPolicyWaiverReasonsEqual(
        results.get(ACKNOWLEDGED_VIOLATION_WAIVER_REASON.getId()),
        ACKNOWLEDGED_VIOLATION_WAIVER_REASON);
    assertPolicyWaiverReasonsEqual(
        results.get(EVALUATING_COMPONENT_WAIVER_REASON.getId()),
        EVALUATING_COMPONENT_WAIVER_REASON);
    assertPolicyWaiverReasonsEqual(
        results.get(MITIGATED_EXTERNALLY_WAIVER_REASON.getId()),
        MITIGATED_EXTERNALLY_WAIVER_REASON);
    assertPolicyWaiverReasonsEqual(
        results.get(NO_UPGRADE_PATH_WAIVER_REASON.getId()),
        NO_UPGRADE_PATH_WAIVER_REASON);
    assertPolicyWaiverReasonsEqual(
        results.get(NOT_EXPLOITABLE_WAIVER_REASON.getId()),
        NOT_EXPLOITABLE_WAIVER_REASON);
    assertPolicyWaiverReasonsEqual(
        results.get(NOT_REACHABLE_WAIVER_REASON.getId()),
        NOT_REACHABLE_WAIVER_REASON);
    assertPolicyWaiverReasonsEqual(
        results.get(RESEARCHING_WAIVER_REASON.getId()),
        RESEARCHING_WAIVER_REASON);
    assertPolicyWaiverReasonsEqual(
        results.get(OTHER_WAIVER_REASON.getId()),
        OTHER_WAIVER_REASON);
  }

  @Test
  public void testGetByReasonText() {
    PolicyWaiverReason policyWaiverReason = tempEntity.newWaiverReason("type1", "because reasons");

    PolicyWaiverReason savedPolicyWaiverReason = policyWaiverReasonDAO.getByReasonText("because reasons");

    assertThat(savedPolicyWaiverReason.getId()).isEqualTo(policyWaiverReason.getId());
    assertThat(savedPolicyWaiverReason.getType()).isEqualTo(policyWaiverReason.getType());
    assertThat(savedPolicyWaiverReason.getReasonText()).isEqualTo(policyWaiverReason.getReasonText());
  }

  @Test
  public void testInsert() {
    PolicyWaiverReason policyWaiverReason = new PolicyWaiverReason("system", "reason");

    policyWaiverReasonDAO.insert(policyWaiverReason);

    PolicyWaiverReason savedPolicyWaiverReason = policyWaiverReasonDAO.getById(policyWaiverReason.getId());

    assertThat(savedPolicyWaiverReason.getId()).isEqualTo(policyWaiverReason.getId());
    assertThat(savedPolicyWaiverReason.getType()).isEqualTo(policyWaiverReason.getType());
    assertThat(savedPolicyWaiverReason.getReasonText()).isEqualTo(policyWaiverReason.getReasonText());
  }

  @Test
  public void testUpdate() {
    PolicyWaiverReason policyWaiverReason = tempEntity.newWaiverReason("type1", "because reasons");
    policyWaiverReason.setReasonText("reason");

    policyWaiverReasonDAO.update(policyWaiverReason);

    PolicyWaiverReason savedPolicyWaiverReason = policyWaiverReasonDAO.getById(policyWaiverReason.getId());

    assertThat(savedPolicyWaiverReason.getId()).isEqualTo(policyWaiverReason.getId());
    assertThat(savedPolicyWaiverReason.getType()).isEqualTo(policyWaiverReason.getType());
    assertThat(savedPolicyWaiverReason.getReasonText()).isEqualTo(policyWaiverReason.getReasonText());
  }

  @Test
  public void testGetAll_returnsAllEntriesCorrectlySorted() {
    final var oranges = tempEntity.newWaiverReason("system", "oranges", null);
    final var apples = tempEntity.newWaiverReason("system", "apples", null);
    final var plumbs = tempEntity.newWaiverReason("system", "plumbs", null);

    final var overLappingSortOrder = tempEntity.newWaiverReason("system", "over-lapping-sort-order", 10);
    final var endOfTheLine = tempEntity.newWaiverReason("system", "end-of-the-line", 77);

    final List<PolicyWaiverReason> results = policyWaiverReasonDAO.getAll();
    assertPolicyWaiverReasonListEqual(
        results,
        Lists.newArrayList(
            // Entries without sort-order, appear alphabetically at the beginning, this is really an edge case for now.
            // We are currently not inserting any entries without a sort-order, but the fallback sort guarantees
            // this is determinant
            apples,
            oranges,
            plumbs,
            ACKNOWLEDGED_VIOLATION_WAIVER_REASON,
            overLappingSortOrder, // show come second, same sort-order, but greater alphabetical value
            EVALUATING_COMPONENT_WAIVER_REASON,
            MITIGATED_EXTERNALLY_WAIVER_REASON,
            NO_UPGRADE_PATH_WAIVER_REASON,
            NOT_EXPLOITABLE_WAIVER_REASON,
            NOT_REACHABLE_WAIVER_REASON,
            RESEARCHING_WAIVER_REASON,
            OTHER_WAIVER_REASON,
            endOfTheLine // comes at the end of entries with sort-orders
        ));
  }

  private void assertPolicyWaiverReasonListEqual(
      final List<PolicyWaiverReason> actualReasons,
      final List<PolicyWaiverReason> expectedReasons)
  {
    assertThat(actualReasons.size()).isEqualTo(expectedReasons.size());

    for (int i = 0; i < actualReasons.size(); i++) {
      final var actual = actualReasons.get(i);
      final var expected = expectedReasons.get(i);

      assertPolicyWaiverReasonsEqual(actual, expected);
    }
  }

  private void assertPolicyWaiverReasonsEqual(final PolicyWaiverReason actual, final PolicyWaiverReason expected) {
    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getType()).isEqualTo(expected.getType());
    assertThat(actual.getReasonText()).isEqualTo(expected.getReasonText());
    assertThat(actual.getSortOrder()).isEqualTo(expected.getSortOrder());
  }
}
