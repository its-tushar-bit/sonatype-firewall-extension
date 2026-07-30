/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.OwnerComponentDAO;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HostedRepositoryComponentDAOTest
    extends AbstractDbDAOTest
{
  private HostedRepositoryComponentDAO dao;

  private OwnerComponentDAO ownerComponentDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createHostedRepositoryComponentDAO();
    ownerComponentDAO = daoFactory.createOwnerComponentDAO();
  }

  @Test
  public void testInsertAndGetById() {
    HostedRepositoryComponent hrc = new HostedRepositoryComponent(repository.getId(), "path/foo.jar", "abc123");
    hrc.setComponentId("nxrm-comp-1");
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.insert(tx, hrc);
      tx.commit();
    }
    assertThat(hrc.getId()).isNotNull();

    HostedRepositoryComponent loaded;
    try (TransactionContext tx = dao.createTransactionContext()) {
      loaded = dao.getById(tx, hrc.getId());
    }
    assertThat(loaded).isNotNull();
    assertThat(loaded.getRepositoryId()).isEqualTo(repository.getId());
    assertThat(loaded.getPathname()).isEqualTo("path/foo.jar");
    assertThat(loaded.getHash()).isEqualTo("abc123");
    assertThat(loaded.getComponentId()).isEqualTo("nxrm-comp-1");
    assertThat(loaded.getOwnerComponentId()).isNull();
  }

  @Test
  public void testUpdate() {
    HostedRepositoryComponent hrc = seedHrc("path/orig.jar", "hash1");
    hrc.setPathname("path/renamed.jar");
    hrc.setHash("hash2");
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.update(tx, hrc);
      tx.commit();
    }
    HostedRepositoryComponent loaded;
    try (TransactionContext tx = dao.createTransactionContext()) {
      loaded = dao.getById(tx, hrc.getId());
    }
    assertThat(loaded.getPathname()).isEqualTo("path/renamed.jar");
    assertThat(loaded.getHash()).isEqualTo("hash2");
  }

  @Test
  public void testGetByRepositoryIdAndPathname_hit() {
    HostedRepositoryComponent hrc = seedHrc("path/a.jar", "hash-a");
    HostedRepositoryComponent found;
    try (TransactionContext tx = dao.createTransactionContext()) {
      found = dao.getByRepositoryIdAndPathname(tx, repository.getId(), "path/a.jar");
    }
    assertThat(found).isNotNull();
    assertThat(found.getId()).isEqualTo(hrc.getId());
  }

  @Test
  public void testGetByRepositoryIdAndPathname_miss() {
    HostedRepositoryComponent found;
    try (TransactionContext tx = dao.createTransactionContext()) {
      found = dao.getByRepositoryIdAndPathname(tx, repository.getId(), "path/nonexistent.jar");
    }
    assertThat(found).isNull();
  }

  @Test
  public void testGetByRepositoryId_returnsAllRowsForRepository() {
    HostedRepositoryComponent hrcA = seedHrc("path/a.jar", "hash-a");
    HostedRepositoryComponent hrcB = seedHrc("path/b.jar", "hash-b");
    List<HostedRepositoryComponent> found;
    try (TransactionContext tx = dao.createTransactionContext()) {
      found = dao.getByRepositoryId(tx, repository.getId());
    }
    assertThat(found).extracting(HostedRepositoryComponent::getId)
        .containsExactlyInAnyOrder(hrcA.getId(), hrcB.getId());
  }

  @Test
  public void testGetByIdNotNull_throwsWhenMissing() {
    assertThatThrownBy(() -> {
      try (TransactionContext tx = dao.createTransactionContext()) {
        dao.getByIdNotNull(tx, "does-not-exist-id");
      }
    }).isInstanceOf(NotFoundException.class);
  }

  @Test
  public void testDelete_deletesRowAndInvokesCascade() {
    HostedRepositoryComponent hrc = seedHrc("path/b.jar", "hash-b");
    OwnerComponent oc = new OwnerComponent(hrc.getId(), BuildStageType.ID,
        new Date(), "hash-b",
        ComponentIdentifier.createMavenCoordinates("g", "a", "1.0"),
        MatchState.EXACT.getId(),
        IdentificationSource.SONATYPE.getId(), false, null);
    ownerComponentDAO.insert(oc);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.delete(tx, hrc);
      tx.commit();
    }

    try (TransactionContext tx = dao.createTransactionContext()) {
      assertThat(dao.getById(tx, hrc.getId())).isNull();
    }
    assertThat(ownerComponentDAO.getById(oc.getId())).isNull();
  }

  @Test
  public void testDelete_ownerComponentIdFkSetsNullOnParentDelete() {
    OwnerComponent oc = new OwnerComponent(application.getId(), BuildStageType.ID,
        new Date(), "hash-c",
        ComponentIdentifier.createMavenCoordinates("g", "a", "1.0"),
        MatchState.EXACT.getId(),
        IdentificationSource.SONATYPE.getId(), false, null);
    ownerComponentDAO.insert(oc);

    HostedRepositoryComponent hrc = new HostedRepositoryComponent(repository.getId(), "path/c.jar", "hash-c");
    hrc.setOwnerComponentId(oc.getId());
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.insert(tx, hrc);
      tx.commit();
    }

    ownerComponentDAO.delete(oc);

    HostedRepositoryComponent reloaded;
    try (TransactionContext tx = dao.createTransactionContext()) {
      reloaded = dao.getById(tx, hrc.getId());
    }
    assertThat(reloaded).isNotNull();
    assertThat(reloaded.getOwnerComponentId()).isNull();
  }

  private HostedRepositoryComponent seedHrc(String pathname, String hash) {
    HostedRepositoryComponent hrc = new HostedRepositoryComponent(repository.getId(), pathname, hash);
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.insert(tx, hrc);
      tx.commit();
    }
    return hrc;
  }
}
