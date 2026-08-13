/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.jooq.exception.IntegrityConstraintViolationException;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternFilter.SortField.SortableField;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ProprietaryComponentNamePatternDAOTest
    extends AbstractDbDAOTest
{
  private RepositoryDAO repositoryDAO;

  private RepositoryManagerDAO repositoryManagerDAO;

  private ProprietaryComponentNamePatternDAO dao;

  private RepositoryManager repoManager;

  private Repository repo;

  @Before
  @Override
  public void setup() {
    super.setup();
    repositoryDAO = daoFactory.createRepositoryDAO();
    repositoryManagerDAO = daoFactory.createRepositoryManagerDAO();
    dao = daoFactory.createProprietaryComponentNamePatternDAO();
    repoManager = tempEntity.newRepositoryManager();
    repo = tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);
  }

  @Test
  public void testCRUD() {
    // Insert
    ProprietaryComponentNamePattern pattern = tempEntity.newProprietaryComponentNamePattern(repo, "@sonatype", null);

    pattern = dao.getById(pattern.getId());
    assertThat(pattern.getFormat()).isEqualTo(ComponentIdentifier.FORMAT_NPM);
    assertThat(pattern.getNamespacePattern()).isEqualTo("@sonatype");
    assertThat(pattern.getNamePattern()).isNull();
    assertThat(pattern.getRepositoryId()).isEqualTo(repo.getId());
    assertThat(pattern.isEnabled()).isTrue();

    // Update
    pattern.setEnabled(false);
    dao.update(pattern);

    pattern = dao.getById(pattern.getId());
    assertThat(pattern.isEnabled()).isFalse();

    // Delete
    dao.delete(pattern);

    assertThat(dao.getById(pattern.getId())).isNull();
  }

  @Test
  public void testInsert_Uniqueness() {
    tempEntity.newProprietaryComponentNamePattern(repo, "@sonatype", null);

    assertThatExceptionOfType(IntegrityConstraintViolationException.class).isThrownBy(() -> {
      tempEntity.newProprietaryComponentNamePattern(repo, "@sonatype", null);
    });
  }

  @Test
  public void testInsertBatch_IgnoreDuplicateKey() {
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
  public void testGetByFormat() {
    // Enabled npm pattern
    ProprietaryComponentNamePattern pattern1 = tempEntity.newProprietaryComponentNamePattern(repo, "@sonatype", null);
    // Disabled npm pattern
    ProprietaryComponentNamePattern pattern2 =
        tempEntity.newProprietaryComponentNamePattern(repo, null, "sonatype", false /* enabled */);
    // Enabled nuget pattern
    Repository repoNuget =
        tempEntity.newRepository(repoManager, "testNuget", RepositoryType.hosted, ComponentIdentifier.FORMAT_NUGET);
    ProprietaryComponentNamePattern pattern3 =
        tempEntity.newProprietaryComponentNamePattern(repoNuget, null, "sonatype");

    assertThat(dao.getByFormat(ComponentIdentifier.FORMAT_NPM)).extracting(ProprietaryComponentNamePattern::getId)
        .containsExactlyInAnyOrder(pattern1.getId(), pattern2.getId());
    assertThat(dao.getByFormat(ComponentIdentifier.FORMAT_NUGET)).extracting(ProprietaryComponentNamePattern::getId)
        .containsExactlyInAnyOrder(pattern3.getId());
  }

  @Test
  public void testGetEnabledByFormat() {
    // Enabled npm pattern
    ProprietaryComponentNamePattern pattern1 = tempEntity.newProprietaryComponentNamePattern(repo, "@sonatype", null);
    // Disabled npm pattern
    tempEntity.newProprietaryComponentNamePattern(repo, null, "sonatype", false);
    // Enabled nuget pattern
    Repository repoNuget =
        tempEntity.newRepository(repoManager, "testNuget", RepositoryType.hosted, ComponentIdentifier.FORMAT_NUGET);
    ProprietaryComponentNamePattern pattern3 =
        tempEntity.newProprietaryComponentNamePattern(repoNuget, null, "sonatype");

    assertThat(dao.getEnabledByFormat(ComponentIdentifier.FORMAT_NPM))
        .extracting(ProprietaryComponentNamePattern::getId)
        .containsExactlyInAnyOrder(pattern1.getId());
    assertThat(dao.getEnabledByFormat(ComponentIdentifier.FORMAT_NUGET))
        .extracting(ProprietaryComponentNamePattern::getId)
        .containsExactlyInAnyOrder(pattern3.getId());
  }

  @Test
  public void testDeleteByRepository() {
    ProprietaryComponentNamePattern pattern1 = tempEntity.newProprietaryComponentNamePattern(repo, null, "one");
    ProprietaryComponentNamePattern pattern2 = tempEntity.newProprietaryComponentNamePattern(repo, null, "two");
    Repository repo1 =
        tempEntity.newRepository(repoManager, "testNuget", RepositoryType.hosted, ComponentIdentifier.FORMAT_NUGET);
    ProprietaryComponentNamePattern pattern3 = tempEntity.newProprietaryComponentNamePattern(repo1, null, "one");

    dao.deleteByRepository(repo.getId());

    assertThat(dao.getById(pattern1.getId())).isNull();
    assertThat(dao.getById(pattern2.getId())).isNull();
    assertThat(dao.getById(pattern3.getId())).isNotNull();
  }

  @Test
  public void testGetByFilter_Pagination() {
    repo = tempEntity.newRepository(repoManager, "testMavenRepo", RepositoryType.hosted, "maven");
    ProprietaryComponentNamePattern pattern1 = tempEntity.newProprietaryComponentNamePattern(repo,
        "testNamespacePattern1", "testNamespace1");
    ProprietaryComponentNamePattern pattern2 = tempEntity.newProprietaryComponentNamePattern(repo,
        "testNamespacePattern2", "testNamespace2");
    ProprietaryComponentNamePattern pattern3 = tempEntity.newProprietaryComponentNamePattern(repo,
        "testNamespacePattern3", "testNamespace3");

    // First page
    ProprietaryComponentNamePatternFilter filter = new ProprietaryComponentNamePatternFilter();
    filter.page = 1;
    filter.pageSize = 1;
    List<ProprietaryComponentNamePatternDTO> result = dao.getByFilter(Collections.singleton(repo.getId()), filter);
    assertThat(result).hasSize(2);
    assertPattern(result.get(0), pattern1);
    assertPattern(result.get(1), pattern2);

    filter.page = 1;
    filter.pageSize = 2;
    result = dao.getByFilter(Collections.singleton(repo.getId()), filter);
    assertThat(result).hasSize(3);
    assertPattern(result.get(0), pattern1);
    assertPattern(result.get(1), pattern2);
    assertPattern(result.get(2), pattern3);

    filter.page = 1;
    filter.pageSize = 3;
    result = dao.getByFilter(Collections.singleton(repo.getId()), filter);
    assertThat(result).hasSize(3);
    assertPattern(result.get(0), pattern1);
    assertPattern(result.get(1), pattern2);
    assertPattern(result.get(2), pattern3);

    // Second page
    filter.page = 2;
    filter.pageSize = 1;
    result = dao.getByFilter(Collections.singleton(repo.getId()), filter);
    assertThat(result).hasSize(2);
    assertPattern(result.get(0), pattern2);
    assertPattern(result.get(1), pattern3);

    filter.page = 2;
    filter.pageSize = 2;
    result = dao.getByFilter(Collections.singleton(repo.getId()), filter);
    assertThat(result).hasSize(1);
    assertPattern(result.get(0), pattern3);

    // Beyond last page
    filter.page = 3;
    filter.pageSize = 2;
    result = dao.getByFilter(Collections.singleton(repo.getId()), filter);
    assertThat(result).isEmpty();
  }

  @Test
  public void testGetByFilter_Filters() {
    repo = tempEntity.newRepository(repoManager, "testMavenRepo", RepositoryType.hosted, "maven");
    ProprietaryComponentNamePattern pattern1 =
        tempEntity.newProprietaryComponentNamePattern(repo, "testNamespacePattern1x", "testNamePattern1x");
    ProprietaryComponentNamePattern pattern2 =
        tempEntity.newProprietaryComponentNamePattern(repo, "testNamespacePattern2x", "testNamePattern2x");
    ProprietaryComponentNamePattern pattern3 =
        tempEntity.newProprietaryComponentNamePattern(repo, "testNamespacePattern3x", "testNamePattern3x");

    ProprietaryComponentNamePatternFilter filter = new ProprietaryComponentNamePatternFilter();
    filter.page = 1;
    filter.pageSize = 3;

    // No filters
    List<ProprietaryComponentNamePatternDTO> result = dao.getByFilter(Collections.singleton(repo.getId()), filter);
    assertThat(result).hasSize(3);
    assertPattern(result.get(0), pattern1);
    assertPattern(result.get(1), pattern2);
    assertPattern(result.get(2), pattern3);

    // Exact filter on namespace
    filter.searchFilters = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SearchFilter(
        ProprietaryComponentNamePatternFilter.SearchFilter.FilterableField.PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME,
        "testNamespacePattern2x"));
    result = dao.getByFilter(Collections.singleton(repo.getId()), filter);
    assertThat(result).hasSize(1);
    assertPattern(result.get(0), pattern2);

    // Contains filter on namespace
    filter.searchFilters = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SearchFilter(
        ProprietaryComponentNamePatternFilter.SearchFilter.FilterableField.PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME,
        "NamespacePattern2"));
    result = dao.getByFilter(Collections.singleton(repo.getId()), filter);
    assertThat(result).hasSize(1);
    assertPattern(result.get(0), pattern2);

    // Exact filter on name
    filter.searchFilters = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SearchFilter(
        ProprietaryComponentNamePatternFilter.SearchFilter.FilterableField.PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME,
        "testNamePattern2x"));
    result = dao.getByFilter(Collections.singleton(repo.getId()), filter);
    assertThat(result).hasSize(1);
    assertPattern(result.get(0), pattern2);

    // Contains filter on name
    filter.searchFilters = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SearchFilter(
        ProprietaryComponentNamePatternFilter.SearchFilter.FilterableField.PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME,
        "NamePattern2"));
    result = dao.getByFilter(Collections.singleton(repo.getId()), filter);
    assertThat(result).hasSize(1);
    assertPattern(result.get(0), pattern2);
  }

  @Test
  public void testGetByFilter_NoFiltersOrSorting() {
    repo = tempEntity.newRepository(repoManager, "testMavenRepo", RepositoryType.hosted, "maven");
    // Create patterns in reverse order so the creation order doesn't match the sorting
    ProprietaryComponentNamePattern pattern3 = tempEntity.newProprietaryComponentNamePattern(repo,
        "testNamespacePattern3", "testNamePattern3");
    ProprietaryComponentNamePattern pattern2 = tempEntity.newProprietaryComponentNamePattern(repo,
        "testNamespacePattern2", "testNamePattern2");
    ProprietaryComponentNamePattern pattern1 = tempEntity.newProprietaryComponentNamePattern(repo,
        "testNamespacePattern1", "testNamePattern1");

    // Null filters and sorting
    ProprietaryComponentNamePatternFilter filter = new ProprietaryComponentNamePatternFilter();
    filter.page = 1;
    filter.pageSize = 3;
    // No sorting specified - defaults to namespace+name
    List<ProprietaryComponentNamePatternDTO> result = dao.getByFilter(Collections.singleton(repo.getId()), filter);
    assertThat(result).hasSize(3);
    assertPattern(result.get(0), pattern1);
    assertPattern(result.get(1), pattern2);
    assertPattern(result.get(2), pattern3);

    // Empty filters and sorting
    filter.searchFilters = Collections.emptyList();
    filter.sortFields = Collections.emptyList();
    // No sorting specified - defaults to namespace+name
    result = dao.getByFilter(Collections.singleton(repo.getId()), filter);
    assertThat(result).hasSize(3);
    assertPattern(result.get(0), pattern1);
    assertPattern(result.get(1), pattern2);
    assertPattern(result.get(2), pattern3);
  }

  @Test
  public void testGetByFilter_SortOnNamespaceAndName() {
    repo = tempEntity.newRepository(repoManager, "testMavenRepo", RepositoryType.hosted, "maven");
    // Create patterns in reverse order so the creation order doesn't match the sorting
    ProprietaryComponentNamePattern pattern3 = tempEntity.newProprietaryComponentNamePattern(repo,
        "testNamespacePattern3", null);
    ProprietaryComponentNamePattern pattern2 = tempEntity.newProprietaryComponentNamePattern(repo,
        null, "testNamePattern2");
    ProprietaryComponentNamePattern pattern1 = tempEntity.newProprietaryComponentNamePattern(repo,
        "testNamespacePattern1", null);

    ProprietaryComponentNamePatternFilter filter = new ProprietaryComponentNamePatternFilter();
    filter.page = 1;
    filter.pageSize = 3;

    // Sort on namespace+name ASC
    filter.sortFields = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SortField(
        ProprietaryComponentNamePatternFilter.SortField.SortableField.PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME,
        true /* asc */, 1 /* sortPriority */));
    List<ProprietaryComponentNamePatternDTO> result = dao.getByFilter(Collections.singleton(repo.getId()), filter);
    assertThat(result).hasSize(3);
    assertPattern(result.get(0), pattern2);
    assertPattern(result.get(1), pattern1);
    assertPattern(result.get(2), pattern3);

    // Sort on namespace+name DESC
    filter.sortFields = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SortField(
        ProprietaryComponentNamePatternFilter.SortField.SortableField.PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME,
        false /* asc */, 1 /* sortPriority */));
    result = dao.getByFilter(Collections.singleton(repo.getId()), filter);
    assertThat(result).hasSize(3);
    assertPattern(result.get(0), pattern3);
    assertPattern(result.get(1), pattern1);
    assertPattern(result.get(2), pattern2);
  }

  @Test
  public void testGetByFilter_SortOnRepositoryManagerInstanceId() {
    // Create patterns in reverse order so the creation order doesn't match the sorting
    RepositoryManager repositoryManager3 = tempEntity.newRepositoryManager("testInstanceId3");
    Repository repo3 = tempEntity.newRepository(repositoryManager3, "testMavenRepo3", RepositoryType.hosted, "maven");
    ProprietaryComponentNamePattern pattern3 = tempEntity.newProprietaryComponentNamePattern(
        repo3, "testNamespacePattern", "testNamePattern");
    RepositoryManager repositoryManager2 = tempEntity.newRepositoryManager("testInstanceId2");
    Repository repo2 = tempEntity.newRepository(repositoryManager2, "testMavenRepo2", RepositoryType.hosted, "maven");
    ProprietaryComponentNamePattern pattern2 = tempEntity.newProprietaryComponentNamePattern(
        repo2, "testNamespacePattern", "testNamePattern");
    RepositoryManager repositoryManager1 = tempEntity.newRepositoryManager("testInstanceId1");
    Repository repo1 = tempEntity.newRepository(repositoryManager1, "testMavenRepo1", RepositoryType.hosted, "maven");
    ProprietaryComponentNamePattern pattern1 = tempEntity.newProprietaryComponentNamePattern(
        repo1, "testNamespacePattern", "testNamePattern");

    ProprietaryComponentNamePatternFilter filter = new ProprietaryComponentNamePatternFilter();
    filter.page = 1;
    filter.pageSize = 3;

    // Sort on repo manager instance ID ASC
    filter.sortFields = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SortField(
        ProprietaryComponentNamePatternFilter.SortField.SortableField.REPOSITORY_MANAGER_INSTANCE_ID_OR_NAME,
        true /* asc */, 1 /* sortPriority */));
    Set<String> repoIds = ImmutableSet.of(repo1.getId(), repo2.getId(), repo3.getId());
    List<ProprietaryComponentNamePatternDTO> result = dao.getByFilter(repoIds, filter);
    assertThat(result).hasSize(3);
    assertPattern(result.get(0), pattern1);
    assertPattern(result.get(1), pattern2);
    assertPattern(result.get(2), pattern3);

    // Sort on repo manager instance ID DESC
    filter.sortFields = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SortField(
        ProprietaryComponentNamePatternFilter.SortField.SortableField.REPOSITORY_MANAGER_INSTANCE_ID_OR_NAME,
        false /* asc */,
        1 /* sortPriority */));
    result = dao.getByFilter(repoIds, filter);
    assertThat(result).hasSize(3);
    assertPattern(result.get(0), pattern3);
    assertPattern(result.get(1), pattern2);
    assertPattern(result.get(2), pattern1);
  }

  @Test
  public void testGetByFilter_SortOnEnabled() {
    repo = tempEntity.newRepository(repoManager, "testMavenRepo", RepositoryType.hosted, "maven");
    // Create patterns in reverse order so the creation order doesn't match the sorting
    ProprietaryComponentNamePattern pattern1 =
        tempEntity.newProprietaryComponentNamePattern(repo, "testNamespacePattern1", null, true);
    ProprietaryComponentNamePattern pattern2 =
        tempEntity.newProprietaryComponentNamePattern(repo, "testNamespacePattern2", null, false);

    ProprietaryComponentNamePatternFilter filter = new ProprietaryComponentNamePatternFilter();
    filter.page = 1;
    filter.pageSize = 2;

    // Sort on enabled ASC
    filter.sortFields = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SortField(
        SortableField.ENABLED,
        true /* asc */, 1 /* sortPriority */));
    List<ProprietaryComponentNamePatternDTO> result = dao.getByFilter(Collections.singleton(repo.getId()), filter);
    assertThat(result).hasSize(2);
    assertPattern(result.get(0), pattern2);
    assertPattern(result.get(1), pattern1);

    // Sort on enabled DESC
    filter.sortFields = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SortField(
        SortableField.ENABLED,
        false /* asc */, 1 /* sortPriority */));
    result = dao.getByFilter(Collections.singleton(repo.getId()), filter);
    assertThat(result).hasSize(2);
    assertPattern(result.get(0), pattern1);
    assertPattern(result.get(1), pattern2);
  }

  @Test
  public void testGetByFilter_SortOnRepositoryPublicId() {
    // Create patterns in reverse order so the creation order doesn't match the sorting
    Repository repo3 = tempEntity.newRepository(repoManager, "testMavenRepo3", RepositoryType.hosted, "maven");
    ProprietaryComponentNamePattern pattern3 =
        tempEntity.newProprietaryComponentNamePattern(repo3, "testNamespacePattern", "testNamePattern");
    Repository repo2 = tempEntity.newRepository(repoManager, "testMavenRepo2", RepositoryType.hosted, "maven");
    ProprietaryComponentNamePattern pattern2 =
        tempEntity.newProprietaryComponentNamePattern(repo2, "testNamespacePattern", "testNamePattern");
    Repository repo1 = tempEntity.newRepository(repoManager, "testMavenRepo1", RepositoryType.hosted, "maven");
    ProprietaryComponentNamePattern pattern1 =
        tempEntity.newProprietaryComponentNamePattern(repo1, "testNamespacePattern", "testNamePattern");

    ProprietaryComponentNamePatternFilter filter = new ProprietaryComponentNamePatternFilter();
    filter.page = 1;
    filter.pageSize = 3;

    // Sort on repo public ID ASC
    filter.sortFields = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SortField(
        ProprietaryComponentNamePatternFilter.SortField.SortableField.REPOSITORY_PUBLIC_ID, true /* asc */,
        1 /* sortPriority */));
    Set<String> repoIds = ImmutableSet.of(repo1.getId(), repo2.getId(), repo3.getId());
    List<ProprietaryComponentNamePatternDTO> result = dao.getByFilter(repoIds, filter);
    assertThat(result).hasSize(3);
    assertPattern(result.get(0), pattern1);
    assertPattern(result.get(1), pattern2);
    assertPattern(result.get(2), pattern3);

    // Sort on repo public ID DESC
    filter.sortFields = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SortField(
        ProprietaryComponentNamePatternFilter.SortField.SortableField.REPOSITORY_PUBLIC_ID, false /* asc */,
        1 /* sortPriority */));
    result = dao.getByFilter(repoIds, filter);
    assertThat(result).hasSize(3);
    assertPattern(result.get(0), pattern3);
    assertPattern(result.get(1), pattern2);
    assertPattern(result.get(2), pattern1);
  }

  @Test
  public void testGetByFilter_SortPriorities() {
    RepositoryManager repositoryManager1 = tempEntity.newRepositoryManager("testInstanceId1");
    Repository repo1 = tempEntity.newRepository(repositoryManager1, "testMavenRepo1", RepositoryType.hosted, "maven");
    ProprietaryComponentNamePattern pattern1 =
        tempEntity.newProprietaryComponentNamePattern(repo1, "testNamespacePattern1", "");
    ProprietaryComponentNamePattern pattern2 =
        tempEntity.newProprietaryComponentNamePattern(repo1, "testNamespacePattern2", "");
    RepositoryManager repositoryManager2 = tempEntity.newRepositoryManager("testInstanceId2");
    Repository repo2 = tempEntity.newRepository(repositoryManager2, "testMavenRepo2", RepositoryType.hosted, "maven");
    ProprietaryComponentNamePattern pattern3 =
        tempEntity.newProprietaryComponentNamePattern(repo2, "testNamespacePattern3", "");

    ProprietaryComponentNamePatternFilter filter = new ProprietaryComponentNamePatternFilter();
    filter.page = 1;
    filter.pageSize = 3;

    // Sort on repo manager instance ID ASC and namespace ASC
    filter.sortFields = new ArrayList<>();
    filter.sortFields.add(new ProprietaryComponentNamePatternFilter.SortField(
        ProprietaryComponentNamePatternFilter.SortField.SortableField.PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME,
        true /* asc */, 2 /* sortPriority */));
    filter.sortFields.add(new ProprietaryComponentNamePatternFilter.SortField(
        ProprietaryComponentNamePatternFilter.SortField.SortableField.REPOSITORY_MANAGER_INSTANCE_ID_OR_NAME,
        true /* asc */,
        1 /* sortPriority */));
    Set<String> repoIds = ImmutableSet.of(repo1.getId(), repo2.getId());
    List<ProprietaryComponentNamePatternDTO> result = dao.getByFilter(repoIds, filter);
    assertThat(result).hasSize(3);
    assertPattern(result.get(0), pattern1);
    assertPattern(result.get(1), pattern2);
    assertPattern(result.get(2), pattern3);

    // Sort on repo manager instance ID ASC and namespace DESC
    filter.sortFields = new ArrayList<>();
    filter.sortFields.add(new ProprietaryComponentNamePatternFilter.SortField(
        ProprietaryComponentNamePatternFilter.SortField.SortableField.PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME,
        false /* asc */, 2 /* sortPriority */));
    filter.sortFields.add(new ProprietaryComponentNamePatternFilter.SortField(
        ProprietaryComponentNamePatternFilter.SortField.SortableField.REPOSITORY_MANAGER_INSTANCE_ID_OR_NAME,
        true /* asc */,
        1 /* sortPriority */));
    result = dao.getByFilter(repoIds, filter);
    assertThat(result).hasSize(3);
    assertPattern(result.get(0), pattern2);
    assertPattern(result.get(1), pattern1);
    assertPattern(result.get(2), pattern3);

    // Sort on repo manager instance ID DESC and namespace ASC
    filter.sortFields = new ArrayList<>();
    filter.sortFields.add(new ProprietaryComponentNamePatternFilter.SortField(
        ProprietaryComponentNamePatternFilter.SortField.SortableField.PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME,
        true /* asc */, 2 /* sortPriority */));
    filter.sortFields.add(new ProprietaryComponentNamePatternFilter.SortField(
        ProprietaryComponentNamePatternFilter.SortField.SortableField.REPOSITORY_MANAGER_INSTANCE_ID_OR_NAME,
        false /* asc */, 1 /* sortPriority */));
    result = dao.getByFilter(repoIds, filter);
    assertThat(result).hasSize(3);
    assertPattern(result.get(0), pattern3);
    assertPattern(result.get(1), pattern1);
    assertPattern(result.get(2), pattern2);

    // Sort on repo manager instance ID ASC and namespace DESC
    filter.sortFields = new ArrayList<>();
    filter.sortFields.add(new ProprietaryComponentNamePatternFilter.SortField(
        ProprietaryComponentNamePatternFilter.SortField.SortableField.PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME,
        false /* asc */, 2 /* sortPriority */));
    filter.sortFields.add(new ProprietaryComponentNamePatternFilter.SortField(
        ProprietaryComponentNamePatternFilter.SortField.SortableField.REPOSITORY_MANAGER_INSTANCE_ID_OR_NAME,
        false /* asc */,
        1 /* sortPriority */));
    result = dao.getByFilter(repoIds, filter);
    assertThat(result).hasSize(3);
    assertPattern(result.get(0), pattern3);
    assertPattern(result.get(1), pattern2);
    assertPattern(result.get(2), pattern1);
  }

  @Test
  public void testGetByFilter_FiltersAndSorting() {
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
        false /* asc */,
        1 /* sortPriority */));

    Set<String> repoIds = ImmutableSet.of(repo1.getId(), repo2.getId(), repo3.getId());
    List<ProprietaryComponentNamePatternDTO> result = dao.getByFilter(repoIds, filter);
    assertThat(result).hasSize(2);
    assertPattern(result.get(0), pattern2);
    assertPattern(result.get(1), pattern1);
  }

  @Test
  public void testGetByFilter_NoRepositories() {
    repo = tempEntity.newRepository(repoManager, "testMavenRepo", RepositoryType.hosted, "maven");
    ProprietaryComponentNamePattern pattern =
        tempEntity.newProprietaryComponentNamePattern(repo, "testNamespacePattern1x", "testNamePattern1x");

    ProprietaryComponentNamePatternFilter filter = new ProprietaryComponentNamePatternFilter();
    filter.page = 1;
    filter.pageSize = 3;

    // Sanity check
    List<ProprietaryComponentNamePatternDTO> result = dao.getByFilter(Collections.singleton(repo.getId()), filter);
    assertThat(result).hasSize(1);
    assertPattern(result.get(0), pattern);

    // No repositories
    result = dao.getByFilter(Collections.emptySet(), filter);
    assertThat(result).isEmpty();
  }

  @Test
  public void testGetByFilter_FilterByRepositoryIds() {
    RepositoryManager repositoryManager1 = tempEntity.newRepositoryManager("testInstanceId1");
    Repository repo1 = tempEntity.newRepository(repositoryManager1, "testMavenRepo1", RepositoryType.hosted, "maven");
    tempEntity.newProprietaryComponentNamePattern(repo1, "testNamespacePattern1", "testNamePattern1");
    RepositoryManager repositoryManager2 = tempEntity.newRepositoryManager("testInstanceId2");
    Repository repo2 = tempEntity.newRepository(repositoryManager2, "testMavenRepo2", RepositoryType.hosted, "maven");
    ProprietaryComponentNamePattern pattern2 =
        tempEntity.newProprietaryComponentNamePattern(repo2, "testNamespacePattern2", "testNamePattern2");
    RepositoryManager repositoryManager3 = tempEntity.newRepositoryManager("testInstanceId3");
    Repository repo3 = tempEntity.newRepository(repositoryManager3, "testMavenRepo3", RepositoryType.hosted, "maven");
    ProprietaryComponentNamePattern pattern3 =
        tempEntity.newProprietaryComponentNamePattern(repo3, "testNamespacePattern3", "testNamePattern3");

    ProprietaryComponentNamePatternFilter filter = new ProprietaryComponentNamePatternFilter();
    filter.page = 1;
    filter.pageSize = 3;

    Set<String> repoIds = ImmutableSet.of(repo2.getId(), repo3.getId());
    List<ProprietaryComponentNamePatternDTO> result = dao.getByFilter(repoIds, filter);
    assertThat(result).hasSize(2);
    assertPattern(result.get(0), pattern2);
    assertPattern(result.get(1), pattern3);
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
