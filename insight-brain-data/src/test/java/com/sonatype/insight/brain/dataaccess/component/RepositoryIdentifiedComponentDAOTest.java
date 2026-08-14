/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.component;

import java.time.Duration;
import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.model.component.RepositoryIdentifiedComponent;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class RepositoryIdentifiedComponentDAOTest
    extends AbstractDbDAOTest
{
  private RepositoryIdentifiedComponentDAO dao;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createRepositoryIdentifiedComponentDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    RepositoryIdentifiedComponent repositoryIdentifiedComponent = new RepositoryIdentifiedComponent("hash",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"), new Date(0), new Date(1));
    dao.insert(repositoryIdentifiedComponent);

    // Read
    RepositoryIdentifiedComponent storedRepositoryIdentifiedComponent =
        dao.getByHash(repositoryIdentifiedComponent.getHash());
    assertThat(storedRepositoryIdentifiedComponent).usingRecursiveComparison(
        JPA.RECURSIVE_COMPARISON_CONFIG).isEqualTo(repositoryIdentifiedComponent);

    // Update
    repositoryIdentifiedComponent.setComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2"));
    repositoryIdentifiedComponent.setCreateTime(new Date(2));
    repositoryIdentifiedComponent.setLastAccessTime(new Date(3));
    dao.update(repositoryIdentifiedComponent);
    storedRepositoryIdentifiedComponent = dao.getByHash(repositoryIdentifiedComponent.getHash());
    assertThat(storedRepositoryIdentifiedComponent).usingRecursiveComparison(
        JPA.RECURSIVE_COMPARISON_CONFIG).isEqualTo(repositoryIdentifiedComponent);

    // Delete
    dao.delete(repositoryIdentifiedComponent);
    storedRepositoryIdentifiedComponent = dao.getByHash(repositoryIdentifiedComponent.getHash());
    assertThat(storedRepositoryIdentifiedComponent).isNull();
  }

  @Test
  public void testGetById() {
    RepositoryIdentifiedComponent repositoryIdentifiedComponentA =
        tempEntity.newRepositoryIdentifiedComponent("hashA",
            ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"), new Date(0), new Date(1));
    tempEntity.newRepositoryIdentifiedComponent("hashB",
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2"), new Date(2), new Date(3));

    assertThat(dao.getById(repositoryIdentifiedComponentA.getHash())).usingRecursiveComparison(
        JPA.RECURSIVE_COMPARISON_CONFIG).isEqualTo(repositoryIdentifiedComponentA);
  }

  @Test
  public void testGetByHash() {
    RepositoryIdentifiedComponent repositoryIdentifiedComponentA =
        tempEntity.newRepositoryIdentifiedComponent("hashA",
            ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"), new Date(0), new Date(1));
    tempEntity.newRepositoryIdentifiedComponent("hashB",
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2"), new Date(2), new Date(3));

    assertThat(dao.getByHash(repositoryIdentifiedComponentA.getHash())).usingRecursiveComparison(
        JPA.RECURSIVE_COMPARISON_CONFIG).isEqualTo(repositoryIdentifiedComponentA);
  }

  @Test
  public void testHash_HexadecimalLength() {
    String hash = DigestUtils.sha256Hex("hash");
    assertThat(hash).hasSize(64);
    tempEntity.newRepositoryIdentifiedComponent(hash,
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), new Date(0), new Date(1));

    RepositoryIdentifiedComponent repositoryIdentifiedComponent = dao.getByHash(hash);
    assertThat(repositoryIdentifiedComponent).isNotNull();
    assertThat(repositoryIdentifiedComponent.getHash()).hasSize(64);
  }

  @Test
  public void testGetByHashNotNullAndUpdateLastAccessTime_DoesNotExist() {
    String hash = "unknown";
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> dao.getByHashNotNullAndUpdateLastAccessTime(hash))
        .withMessageContaining("RepositoryIdentifiedComponent with hash " + hash + " does not exist.");
  }

  @Test
  public void testGetByHashNotNullAndUpdateLastAccessTime() {
    RepositoryIdentifiedComponent initial =
        tempEntity.newRepositoryIdentifiedComponent("hash",
            ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), new Date(0), new Date(1));

    Date date = new Date();
    RepositoryIdentifiedComponent updated = dao.getByHashNotNullAndUpdateLastAccessTime(initial.getHash());
    assertThat(updated).usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .ignoringFields("lastAccessTime")
        .isEqualTo(initial);
    assertThat(updated.getLastAccessTime()).isAfterOrEqualTo(date);
  }

  @Test
  public void testGetAll() {
    RepositoryIdentifiedComponent repositoryIdentifiedComponentA =
        tempEntity.newRepositoryIdentifiedComponent("hashA",
            ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"), new Date(0), new Date(1));
    RepositoryIdentifiedComponent repositoryIdentifiedComponentB =
        tempEntity.newRepositoryIdentifiedComponent("hashB",
            ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2"), new Date(2), new Date(3));

    assertThat(dao.getAll()).usingRecursiveFieldByFieldElementComparator(
        JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(repositoryIdentifiedComponentA, repositoryIdentifiedComponentB);
  }

  @Test
  public void testDeleteInfrequentlyAccessed() {
    long maxLastAccess = 1;
    long now = 3;
    RepositoryIdentifiedComponentDAO spy = spy(dao);
    when(spy.now()).thenReturn(now);
    tempEntity.newRepositoryIdentifiedComponent(new Date(0));
    tempEntity.newRepositoryIdentifiedComponent(new Date(1));
    RepositoryIdentifiedComponent repositoryIdentifiedComponent3 =
        tempEntity.newRepositoryIdentifiedComponent(new Date(2));
    RepositoryIdentifiedComponent repositoryIdentifiedComponent4 =
        tempEntity.newRepositoryIdentifiedComponent(new Date(3));

    spy.deleteInfrequentlyAccessed(Duration.ofMillis(maxLastAccess));

    assertThat(spy.getAll()).usingRecursiveFieldByFieldElementComparator(
        JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(repositoryIdentifiedComponent3, repositoryIdentifiedComponent4);
  }

  @Test
  public void testDeleteByHash() {
    RepositoryIdentifiedComponent repositoryIdentifiedComponent1 = tempEntity.newRepositoryIdentifiedComponent();
    RepositoryIdentifiedComponent repositoryIdentifiedComponent2 = tempEntity.newRepositoryIdentifiedComponent();

    int result = dao.deleteByHash(repositoryIdentifiedComponent1.getHash());

    assertThat(result).isEqualTo(1);
    assertThat(dao.getAll()).usingRecursiveFieldByFieldElementComparator(
        JPA.RECURSIVE_COMPARISON_CONFIG).containsExactly(repositoryIdentifiedComponent2);
  }

  @Test
  public void testDeleteByComponentIdentifier() {
    RepositoryIdentifiedComponent repositoryIdentifiedComponent1 = tempEntity.newRepositoryIdentifiedComponent();
    RepositoryIdentifiedComponent repositoryIdentifiedComponent2 = tempEntity.newRepositoryIdentifiedComponent();

    int result = dao.deleteByComponentIdentifier(repositoryIdentifiedComponent1.getComponentIdentifier());

    assertThat(result).isEqualTo(1);
    assertThat(dao.getAll()).usingRecursiveFieldByFieldElementComparator(
        JPA.RECURSIVE_COMPARISON_CONFIG).containsExactly(repositoryIdentifiedComponent2);
  }

  @Test
  public void testDeleteAll() {
    tempEntity.newRepositoryIdentifiedComponent();
    tempEntity.newRepositoryIdentifiedComponent();

    int result = dao.deleteAll();

    assertThat(result).isEqualTo(2);
    assertThat(dao.getAll()).isEmpty();
  }
}
