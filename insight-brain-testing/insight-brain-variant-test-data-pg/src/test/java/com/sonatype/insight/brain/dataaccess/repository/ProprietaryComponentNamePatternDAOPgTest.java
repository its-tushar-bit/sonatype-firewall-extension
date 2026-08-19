/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL-backed tests relocated from {@link ProprietaryComponentNamePatternDAOTest} (CLM-45228).
 */
@PostgresTest
public class ProprietaryComponentNamePatternDAOPgTest
    extends AbstractDbDAOTest
{
  private RepositoryDAO repositoryDAO;

  private RepositoryManagerDAO repositoryManagerDAO;

  private ProprietaryComponentNamePatternDAO dao;

  private RepositoryManager repoManager;

  private Repository repo;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    repositoryDAO = daoFactory.createRepositoryDAO();
    repositoryManagerDAO = daoFactory.createRepositoryManagerDAO();
    dao = daoFactory.createProprietaryComponentNamePatternDAO();
    repoManager = tempEntity.newRepositoryManager();
    repo = tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);
  }

  /**
   * Postgres variant of {@code testInsertBatch_IgnoreDuplicateKey()}. On H2 the {@code ignoreDuplicateKey} path falls
   * back to per-entity inserts with savepoints, whereas Postgres uses jOOQ's native
   * {@code batch(INSERT ... ON CONFLICT DO NOTHING)}. This exercises that native batch path end-to-end.
   */
  @Test
  public void testInsertBatch_IgnoreDuplicateKey_Postgres() {
    assertInsertBatchIgnoresDuplicateKey();
  }

  private void assertInsertBatchIgnoresDuplicateKey() {
    ProprietaryComponentNamePattern existing = tempEntity.newProprietaryComponentNamePattern(repo, "@sonatype", null);

    // A batch of entirely new patterns (no collision) inserts every row, so the count equals the batch size.
    int freshInserted = dao.insertBatch(List.of(
        new ProprietaryComponentNamePattern(repo.getId(), ComponentIdentifier.FORMAT_NPM).withNamespacePattern("@new1"),
        new ProprietaryComponentNamePattern(repo.getId(), ComponentIdentifier.FORMAT_NPM)
            .withNamespacePattern("@new2")),
        true);
    assertThat(freshInserted).isEqualTo(2);

    // Batch contains a duplicate of the existing pattern plus a genuinely new one.
    ProprietaryComponentNamePattern duplicate =
        new ProprietaryComponentNamePattern(repo.getId(), ComponentIdentifier.FORMAT_NPM).withNamespacePattern(
            "@sonatype");
    ProprietaryComponentNamePattern fresh =
        new ProprietaryComponentNamePattern(repo.getId(), ComponentIdentifier.FORMAT_NPM).withNamespacePattern(
            "@example");

    // ignoreDuplicateKey=true: the duplicate is silently skipped and the new pattern is inserted, no exception thrown.
    // The return value counts only the rows actually written, so the skipped duplicate is excluded (1, not 2).
    int inserted = dao.insertBatch(List.of(duplicate, fresh), true);
    assertThat(inserted).isEqualTo(1);

    assertThat(dao.getByFormat(ComponentIdentifier.FORMAT_NPM))
        .extracting(ProprietaryComponentNamePattern::getNamespacePattern)
        .containsExactlyInAnyOrder("@sonatype", "@example", "@new1", "@new2");
    // The pre-existing row is untouched (the duplicate did not overwrite it).
    assertThat(dao.getById(existing.getId())).isNotNull();

    // A batch of only duplicates inserts nothing and returns 0.
    int reinserted = dao.insertBatch(List.of(
        new ProprietaryComponentNamePattern(repo.getId(), ComponentIdentifier.FORMAT_NPM)
            .withNamespacePattern("@example")),
        true);
    assertThat(reinserted).isEqualTo(0);
  }

  @Test
  public void testGetByFilter_FiltersAndSorting_Postgres() {
    RepositoryManager repositoryManager1 = tempEntity.newRepositoryManager("testInstanceId1");
    Repository repo1 = tempEntity.newRepository(repositoryManager1, "testMavenRepo1", RepositoryType.hosted, "maven");
    ProprietaryComponentNamePattern pattern1 =
        tempEntity.newProprietaryComponentNamePattern(repo1, "testNamespacePattern1", "");
    RepositoryManager repositoryManager2 = tempEntity.newRepositoryManager("testInstanceId2");
    Repository repo2 = tempEntity.newRepository(repositoryManager2, "testMavenRepo2", RepositoryType.hosted, "maven");
    ProprietaryComponentNamePattern pattern2 =
        tempEntity.newProprietaryComponentNamePattern(repo2, "testNamespacePattern1", "");
    RepositoryManager repositoryManager3 = tempEntity.newRepositoryManager("testInstanceId3");
    Repository repo3 = tempEntity.newRepository(repositoryManager3, "testMavenRepo3", RepositoryType.hosted, "maven");
    tempEntity.newProprietaryComponentNamePattern(repo3, "testNamespacePattern3", "");

    ProprietaryComponentNamePatternFilter filter = new ProprietaryComponentNamePatternFilter();
    filter.page = 1;
    filter.pageSize = 3;

    // Filter on namespace and sort on repo manager instance ID DESC
    filter.searchFilters = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SearchFilter(
        ProprietaryComponentNamePatternFilter.SearchFilter.FilterableField.PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME,
        "testNamespacePattern1"));
    filter.sortFields = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SortField(
        ProprietaryComponentNamePatternFilter.SortField.SortableField.REPOSITORY_MANAGER_INSTANCE_ID_OR_NAME,
        false /* asc */, 1 /* sortPriority */));

    Set<String> repoIds = ImmutableSet.of(repo1.getId(), repo2.getId(), repo3.getId());
    List<ProprietaryComponentNamePatternDTO> result = dao.getByFilter(repoIds, filter);
    assertThat(result).hasSize(2);
    assertPattern(result.get(0), pattern2);
    assertPattern(result.get(1), pattern1);
  }

  private void assertPattern(ProprietaryComponentNamePatternDTO actual, ProprietaryComponentNamePattern expected) {
    Repository expectedRepository = repositoryDAO.getById(expected.getRepositoryId());
    RepositoryManager expectedRepositoryManager =
        repositoryManagerDAO.getById(expectedRepository.getRepositoryManagerId());

    assertThat(actual.namespacePattern).isEqualTo(expected.getNamespacePattern());
    assertThat(actual.namePattern).isEqualTo(expected.getNamePattern());
    assertThat(actual.id).isEqualTo(expected.getId());
    assertThat(actual.repositoryManagerInstanceId).isEqualTo(expectedRepositoryManager.getInstanceId());
    assertThat(actual.repositoryPublicId).isEqualTo(expectedRepository.getPublicId());
    assertThat(actual.format).isEqualTo(expected.getFormat());
    assertThat(actual.enabled).isEqualTo(expected.isEnabled());
  }
}
