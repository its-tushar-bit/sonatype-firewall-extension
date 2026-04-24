/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolationConstraintFacts;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyViolationConstraintFactsDAOTest
    extends AbstractDbDAOTest
{
  private PolicyViolationConstraintFactsDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createPolicyViolationConstraintFactsDAO();
  }

  @Test
  public void testCRUD() {
    String hash = HashHelper.truncateHash(UUID.randomUUID().toString());
    String json = "json";

    PolicyViolationConstraintFacts constraints = new PolicyViolationConstraintFacts(hash, json);
    try {
      dao.insert(constraints);

      PolicyViolationConstraintFacts byId = dao.getById(hash);
      assertThat(byId).isNotNull();
      assertThat(byId.getConstraintFactsJson()).isEqualTo(json);
    }
    finally {
      dao.delete(constraints);
    }

  }

  @Test(expected = UnsupportedOperationException.class)
  public void testUpdateNotSupported() {
    String hash = HashHelper.truncateHash(UUID.randomUUID().toString());
    String json = "json";

    dao.update(new PolicyViolationConstraintFacts(hash, json));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testUpdateWithTransactionNotSupported() {
    String hash = HashHelper.truncateHash(UUID.randomUUID().toString());
    String json = "json";

    dao.update(null, new PolicyViolationConstraintFacts(hash, json));
  }

  @Test
  public void testCreateBatchIfNotExists_insertsMissingDedupsAndSkipsExisting() {
    String preExistingJson = "[{\"pre\":\"" + UUID.randomUUID() + "\"}]";
    String preExistingHash = AbstractPolicyViolation.calculateConstraintFactsId(preExistingJson);
    dao.insert(new PolicyViolationConstraintFacts(preExistingHash, preExistingJson));

    String newJsonA = "[{\"a\":\"" + UUID.randomUUID() + "\"}]";
    String newJsonB = "[{\"b\":\"" + UUID.randomUUID() + "\"}]";
    String newHashA = AbstractPolicyViolation.calculateConstraintFactsId(newJsonA);
    String newHashB = AbstractPolicyViolation.calculateConstraintFactsId(newJsonB);

    try {
      // Input includes: pre-existing (skip), two new distinct (insert), and a duplicate of newJsonA (dedup).
      dao.createBatchIfNotExists(List.of(preExistingJson, newJsonA, newJsonB, newJsonA));

      List<PolicyViolationConstraintFacts> fetched =
          dao.getByIds(Set.of(preExistingHash, newHashA, newHashB));
      assertThat(fetched).hasSize(3);
      assertThat(fetched).extracting(PolicyViolationConstraintFacts::getId)
          .containsExactlyInAnyOrder(preExistingHash, newHashA, newHashB);
    }
    finally {
      PolicyViolationConstraintFacts pre = dao.getById(preExistingHash);
      if (pre != null) {
        dao.delete(pre);
      }
      PolicyViolationConstraintFacts a = dao.getById(newHashA);
      if (a != null) {
        dao.delete(a);
      }
      PolicyViolationConstraintFacts b = dao.getById(newHashB);
      if (b != null) {
        dao.delete(b);
      }
    }
  }

  @Test
  public void testCreateBatchIfNotExists_emptyInputIsNoOp() {
    dao.createBatchIfNotExists(List.of());
  }
}
