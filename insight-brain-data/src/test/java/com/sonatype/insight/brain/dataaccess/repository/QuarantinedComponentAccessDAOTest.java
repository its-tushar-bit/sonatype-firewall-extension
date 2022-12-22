/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.repository.QuarantinedComponentAccess;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QuarantinedComponentAccessDAOTest
    extends AbstractDbDAOTest
{
  private QuarantinedComponentAccessDAO dao = new QuarantinedComponentAccessDAO();

  @Test
  public void testCRUD() {
    // Setup
    Date date = new Date();
    final Repository repository = tempEntity.newRepository("repo");
    final RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());
    final RepositoryComponent repositoryComponent2 = tempEntity.newRepositoryComponent(repository.getId(), "path2");

    // Create
    QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId(), date);
    String id = quarantinedComponentAccess.getId();
    quarantinedComponentAccess = dao.getById(id);
    assertThat(quarantinedComponentAccess.getRepositoryComponentId()).isEqualTo(repositoryComponent.getId());
    assertThat(quarantinedComponentAccess.getGenerateTime()).isEqualTo(date);

    // Update
    quarantinedComponentAccess.setRepositoryComponentId(repositoryComponent2.getId());
    dao.update(quarantinedComponentAccess);
    quarantinedComponentAccess = dao.getById(id);
    assertThat(quarantinedComponentAccess.getRepositoryComponentId()).isEqualTo(repositoryComponent2.getId());

    // Delete
    dao.delete(quarantinedComponentAccess);
    quarantinedComponentAccess = dao.getById(id);
    assertThat(quarantinedComponentAccess).isNull();
  }

  @Test
  public void testDeleteAllBeforeDate() {
    // Setup
    final Repository repository = tempEntity.newRepository("repo");
    final RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());
    final Date cutoffDate = DateUtils.addDays(new Date(), -2);

    for (int i = 0; i < 201; i++) {
      tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId(),
          DateUtils.addDays(cutoffDate, -1));
    }
    for (int i = 0; i < 10; i++) {
      tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId());
    }

    dao.deleteAllBeforeDate(cutoffDate);
    assertThat(dao.getAll()).hasSize(10);
  }

  @Test
  public void testDeleteByRepositoryId() {
    // Setup
    final Repository repository = tempEntity.newRepository("repo");
    final RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());
    final RepositoryComponent repositoryComponent2 = tempEntity.newRepositoryComponent(this.repository.getId());

    for (int i = 0; i < 10; i++) {
      tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId());
    }
    for (int i = 0; i < 10; i++) {
      tempEntity.newQuarantinedComponentAccess(this.repository.getId(), repositoryComponent2.getId());
    }

    assertThat(dao.getAll()).hasSize(20);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByRepositoryId(tx, repository.getId());
      tx.commit();
    }
    assertThat(dao.getAll()).hasSize(10);
  }

  @Test
  public void testDeleteByRepositoryComponentId() {
    // Setup
    final Repository repository = tempEntity.newRepository("repo");
    final RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());
    final RepositoryComponent repositoryComponent2 = tempEntity.newRepositoryComponent(repository.getId(), "path2");

    for (int i = 0; i < 10; i++) {
      tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId());
    }
    for (int i = 0; i < 10; i++) {
      tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent2.getId());
    }

    assertThat(dao.getAll()).hasSize(20);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByRepositoryComponentId(tx, repositoryComponent.getId());
      tx.commit();
    }
    assertThat(dao.getAll()).hasSize(10);
  }

  public void testAnonymousAccessEnabled() {
    // It is enabled by default
    assertThat(dao.isAnonymousAccessEnabled()).isTrue();

    dao.setAnonymousAccess(false);
    assertThat(dao.isAnonymousAccessEnabled()).isFalse();

    dao.setAnonymousAccess(true);
    assertThat(dao.isAnonymousAccessEnabled()).isTrue();
  }
}
