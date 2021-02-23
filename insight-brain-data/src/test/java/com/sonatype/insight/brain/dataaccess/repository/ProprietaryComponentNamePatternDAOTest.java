/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import javax.persistence.EntityExistsException;
import javax.persistence.PersistenceException;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ProprietaryComponentNamePatternDAOTest
    extends AbstractDbDAOTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  private ProprietaryComponentNamePatternDAO dao = new ProprietaryComponentNamePatternDAO();

  private String repoManId;

  private String repoId = "repo-id";

  @Before
  public void init() {
    repoManId = tempEntity.newRepositoryManager().getInstanceId();
  }

  @Test
  public void testCRD() {
    ProprietaryComponentNamePattern pattern =
        new ProprietaryComponentNamePattern("npm").withNamespacePattern("@sonatype").withRepository(repoManId, repoId);
    dao.insert(pattern);

    pattern = dao.getById(pattern.getId());
    assertThat(pattern.getFormat()).isEqualTo("npm");
    assertThat(pattern.getNamespacePattern()).isEqualTo("@sonatype");
    assertThat(pattern.getNamePattern()).isNull();
    assertThat(pattern.getRepositoryManagerInstanceId()).isEqualTo(repoManId);
    assertThat(pattern.getRepositoryPublicId()).isEqualTo(repoId);

    dao.delete(pattern);

    assertThat(dao.getById(pattern.getId())).isNull();
  }

  @Test
  public void testInsert_Uniqueness() {
    ProprietaryComponentNamePattern pattern =
        new ProprietaryComponentNamePattern("npm").withNamespacePattern("@sonatype").withRepository(repoManId, repoId);
    dao.insert(pattern);

    assertThatExceptionOfType(PersistenceException.class).isThrownBy(() -> {
      ProprietaryComponentNamePattern dup = new ProprietaryComponentNamePattern("npm").withNamespacePattern("@sonatype")
          .withRepository(repoManId, repoId);
      dao.insert(dup);
    }).withCauseInstanceOf(EntityExistsException.class);
  }

  @Test
  public void testGetByFormat() {
    ProprietaryComponentNamePattern pattern1 =
        new ProprietaryComponentNamePattern("npm").withNamespacePattern("@sonatype").withRepository(repoManId, repoId);
    dao.insert(pattern1);
    ProprietaryComponentNamePattern pattern2 =
        new ProprietaryComponentNamePattern("npm").withNamePattern("sonatype").withRepository(repoManId, repoId);
    dao.insert(pattern2);
    ProprietaryComponentNamePattern pattern3 =
        new ProprietaryComponentNamePattern("nuget").withNamePattern("sonatype").withRepository(repoManId, repoId);
    dao.insert(pattern3);

    assertThat(dao.getByFormat("npm")).extracting(ProprietaryComponentNamePattern::getId)
        .containsExactlyInAnyOrder(pattern1.getId(), pattern2.getId());
    assertThat(dao.getByFormat("nuget")).extracting(ProprietaryComponentNamePattern::getId)
        .containsExactlyInAnyOrder(pattern3.getId());
  }

  @Test
  public void testDeleteByRepository() {
    ProprietaryComponentNamePattern pattern1 =
        new ProprietaryComponentNamePattern("npm").withNamePattern("one").withRepository(repoManId, repoId);
    dao.insert(pattern1);
    ProprietaryComponentNamePattern pattern2 =
        new ProprietaryComponentNamePattern("npm").withNamePattern("two").withRepository(repoManId, repoId);
    dao.insert(pattern2);
    ProprietaryComponentNamePattern pattern3 =
        new ProprietaryComponentNamePattern("npm").withNamePattern("one").withRepository(repoManId, repoId + "-other");
    dao.insert(pattern3);

    dao.deleteByRepository(repoManId, repoId);

    assertThat(dao.getById(pattern1.getId())).isNull();
    assertThat(dao.getById(pattern2.getId())).isNull();
    assertThat(dao.getById(pattern3.getId())).isNotNull();
  }

  @Test
  public void testDeleteByRepositoryManager() {
    ProprietaryComponentNamePattern pattern1 =
        new ProprietaryComponentNamePattern("npm").withNamePattern("sonatype").withRepository(repoManId, repoId);
    dao.insert(pattern1);
    ProprietaryComponentNamePattern pattern2 = new ProprietaryComponentNamePattern("npm").withNamePattern("sonatype")
        .withRepository(repoManId, repoId + "-other");
    dao.insert(pattern2);
    ProprietaryComponentNamePattern pattern3 = new ProprietaryComponentNamePattern("npm").withNamePattern("sonatype")
        .withRepository(tempEntity.newRepositoryManager().getInstanceId(), repoId);
    dao.insert(pattern3);

    dao.deleteByRepositoryManager(repoManId);

    assertThat(dao.getById(pattern1.getId())).isNull();
    assertThat(dao.getById(pattern2.getId())).isNull();
    assertThat(dao.getById(pattern3.getId())).isNotNull();
  }
}
