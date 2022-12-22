/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.postgres.PostgresServer;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryPolicyViolationDAOTest
    extends AbstractDbDAOTest
{
  private RepositoryPolicyViolationDAO dao = new RepositoryPolicyViolationDAO();

  @Test
  public void testCRUD() {
    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId());

    // Create
    Date now = new Date();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    RepositoryPolicyViolation policyViolation = new RepositoryPolicyViolation(repository.getId(), "path", now,
        policy.getId(), policy.getName(), 5, PolicyThreatCategory.LICENSE, "acacacacacac", componentIdentifier,
        "constraint data");
    assertThat(policyViolation.getId()).isNull();
    dao.insert(policyViolation);
    assertThat(policyViolation.getId()).isNotNull();

    // Read
    policyViolation = dao.getById(policyViolation.getId());
    assertThat(policyViolation).isNotNull();
    assertPolicyViolation(repository.getId(), "path", policy.getId(), policy.getName(), 5,
        PolicyThreatCategory.LICENSE, "acacacacacac", componentIdentifier, now, null /* actionTypeId */,
        policyViolation);

    policyViolation.setActionTypeId(Action.ID_FAIL);
    dao.update(policyViolation);

    // Read
    policyViolation = dao.getById(policyViolation.getId());
    assertThat(policyViolation).isNotNull();
    assertPolicyViolation(repository.getId(), "path", policy.getId(), policy.getName(), 5,
        PolicyThreatCategory.LICENSE, "acacacacacac", componentIdentifier, now, Action.ID_FAIL, policyViolation);

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
  public void testDeleteByRepositoryId_Postgres() {
    DataSourceFactory.clear_ForTestsOnly();

    try (PostgresServer postgres = new PostgresServer()) {
      // Create a postgres ODS database
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);

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
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }
}
