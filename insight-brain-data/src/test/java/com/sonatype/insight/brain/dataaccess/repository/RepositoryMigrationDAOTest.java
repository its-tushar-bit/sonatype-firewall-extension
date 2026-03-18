/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import com.sonatype.clm.dto.model.repository.migration.MigrationState;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryMigration;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryMigrationDAOTest
    extends AbstractDbDAOTest
{
  private RepositoryMigrationDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createRepositoryMigrationDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    RepositoryMigration repositoryMigration = createRepositoryMigration(tempEntity.newRepository());
    dao.insert(repositoryMigration);
    assertThat(repositoryMigration.getId()).isNotNull();

    // Read
    assertThat(dao.getByRepositoryId(repositoryMigration.getRepositoryId())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(repositoryMigration);

    // Update
    repositoryMigration.setState(MigrationState.COMPLETED);
    dao.update(repositoryMigration);
    assertThat(dao.getByRepositoryId(repositoryMigration.getRepositoryId())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(repositoryMigration);

    // Delete
    dao.delete(repositoryMigration);
    assertThat(dao.getByRepositoryId(repositoryMigration.getId())).isNull();
  }

  @Test
  public void testGetAll() {
    RepositoryMigration repositoryMigration1 = tempEntity.newRepositoryMigration(tempEntity.newRepository());
    RepositoryMigration repositoryMigration2 = tempEntity.newRepositoryMigration(tempEntity.newRepository());

    assertThat(dao.getAll()).extracting(RepositoryMigration::getId)
        .doesNotContainNull()
        .containsExactlyInAnyOrder(repositoryMigration1.getId(), repositoryMigration2.getId());
  }

  @Test
  public void testTryInsert_SameRepositoryId() {
    Repository repository = tempEntity.newRepository();
    assertThat(dao.tryInsert(createRepositoryMigration(repository))).isTrue();
    assertThat(dao.tryInsert(createRepositoryMigration(repository))).isFalse();
  }

  private RepositoryMigration createRepositoryMigration(Repository repository) {
    RepositoryMigration repositoryMigration = new RepositoryMigration();
    repositoryMigration.setRepositoryId(repository.getId());
    repositoryMigration.setState(MigrationState.RUNNING);
    return repositoryMigration;
  }
}
