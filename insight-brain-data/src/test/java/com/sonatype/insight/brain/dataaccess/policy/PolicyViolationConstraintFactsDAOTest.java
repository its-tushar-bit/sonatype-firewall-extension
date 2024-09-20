/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.UUID;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.HashHelper;
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
}
