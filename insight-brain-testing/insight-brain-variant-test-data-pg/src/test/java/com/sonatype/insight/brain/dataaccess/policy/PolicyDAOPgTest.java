/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL-backed tests relocated from {@link PolicyDAOTest} (CLM-45228).
 */
@PostgresTest
public class PolicyDAOPgTest
    extends AbstractDbDAOTest
{
  private PolicyDAO policyDAO;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    policyDAO = daoFactory.createPolicyDAO();
  }

  @Test
  public void testGetByOwnerIds_PostgresLimit() {
    testGetByOwnerIds_internal();
  }

  private void testGetByOwnerIds_internal() {
    Organization organization = tempEntity.newOrganization("TempOrg");
    Application application = tempEntity.newApplication("TempAppName", "TempAppPublicId",
        organization.getId());
    Policy appPolicy = tempEntity.newPolicy(application);
    tempEntity.newPolicy(organization);
    List<Policy> policies;

    Set<String> largeIdList = new HashSet<>();
    for (int i = 0; i < AbstractOperationalSqlDAO.POSTGRES_IN_OPERATOR_THRESHOLD; i++) {
      largeIdList.add(Integer.toString(i));
    }
    largeIdList.add(application.getId());

    policies = policyDAO.getByOwnerIds(largeIdList);
    assertThat(policies).extracting(Policy::getId).containsExactly(appPolicy.getId());
  }
}
