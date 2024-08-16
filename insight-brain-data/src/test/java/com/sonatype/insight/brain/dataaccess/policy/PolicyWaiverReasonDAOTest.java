/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyWaiverReasonDAOTest
    extends AbstractDbDAOTest
{
  private PolicyWaiverReasonDAO policyWaiverReasonDAO;

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
}
