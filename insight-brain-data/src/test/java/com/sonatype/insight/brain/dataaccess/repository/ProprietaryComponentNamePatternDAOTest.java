/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityExistsException;
import javax.persistence.PersistenceException;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.brain.model.repository.RepositoryManager;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ProprietaryComponentNamePatternDAOTest
    extends AbstractDbDAOTest
{
  private final ProprietaryComponentNamePatternDAO dao = new ProprietaryComponentNamePatternDAO();

  private RepositoryManager repoManager;

  private String repoManId;

  private final String repoId = "repo-id";

  @Before
  public void init() {
    repoManager = tempEntity.newRepositoryManager();
    repoManId = repoManager.getInstanceId();
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

  @Test
  public void testGetByFilter_Pagination() {
    ProprietaryComponentNamePattern pattern1 = tempEntity.newProprietaryComponentNamePattern(repoManId, repoId, "maven",
        "testNamespacePattern1", "testNamespace1");
    ProprietaryComponentNamePattern pattern2 = tempEntity.newProprietaryComponentNamePattern(repoManId, repoId, "maven",
        "testNamespacePattern2", "testNamespace2");
    ProprietaryComponentNamePattern pattern3 = tempEntity.newProprietaryComponentNamePattern(repoManId, repoId, "maven",
        "testNamespacePattern3", "testNamespace3");

    // First page
    ProprietaryComponentNamePatternFilter filter = new ProprietaryComponentNamePatternFilter();
    filter.page = 1;
    filter.pageSize = 1;
    List<ProprietaryComponentNamePattern> result = dao.getByFilter(filter);
    assertThat(result).hasSize(2);
    assertPattern(result.get(0), pattern1);
    assertPattern(result.get(1), pattern2);

    filter.page = 1;
    filter.pageSize = 2;
    result = dao.getByFilter(filter);
    assertThat(result).hasSize(3);
    assertPattern(result.get(0), pattern1);
    assertPattern(result.get(1), pattern2);
    assertPattern(result.get(2), pattern3);

    filter.page = 1;
    filter.pageSize = 3;
    result = dao.getByFilter(filter);
    assertThat(result).hasSize(3);
    assertPattern(result.get(0), pattern1);
    assertPattern(result.get(1), pattern2);
    assertPattern(result.get(2), pattern3);

    // Second page
    filter.page = 2;
    filter.pageSize = 1;
    result = dao.getByFilter(filter);
    assertThat(result).hasSize(2);
    assertPattern(result.get(0), pattern2);
    assertPattern(result.get(1), pattern3);

    filter.page = 2;
    filter.pageSize = 2;
    result = dao.getByFilter(filter);
    assertThat(result).hasSize(1);
    assertPattern(result.get(0), pattern3);

    // Beyond last page
    filter.page = 3;
    filter.pageSize = 2;
    result = dao.getByFilter(filter);
    assertThat(result).isEmpty();
  }

  @Test
  public void testGetByFilter_Filters() {
    ProprietaryComponentNamePattern pattern1 = tempEntity.newProprietaryComponentNamePattern(repoManId, repoId, "maven",
        "testNamespacePattern1x", "testNamePattern1x");
    ProprietaryComponentNamePattern pattern2 = tempEntity.newProprietaryComponentNamePattern(repoManId, repoId, "maven",
        "testNamespacePattern2x", "testNamePattern2x");
    ProprietaryComponentNamePattern pattern3 = tempEntity.newProprietaryComponentNamePattern(repoManId, repoId, "maven",
        "testNamespacePattern3x", "testNamePattern3x");

    ProprietaryComponentNamePatternFilter filter = new ProprietaryComponentNamePatternFilter();
    filter.page = 1;
    filter.pageSize = 3;

    // No filters
    List<ProprietaryComponentNamePattern> result = dao.getByFilter(filter);
    assertThat(result).hasSize(3);
    assertPattern(result.get(0), pattern1);
    assertPattern(result.get(1), pattern2);
    assertPattern(result.get(2), pattern3);

    // Exact filter on namespace
    filter.searchFilters = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SearchFilter(
        ProprietaryComponentNamePatternFilter.SearchFilter.FilterableField.PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME,
        "testNamespacePattern2x"));
    result = dao.getByFilter(filter);
    assertThat(result).hasSize(1);
    assertPattern(result.get(0), pattern2);

    // Contains filter on namespace
    filter.searchFilters = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SearchFilter(
        ProprietaryComponentNamePatternFilter.SearchFilter.FilterableField.PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME,
        "NamespacePattern2"));
    result = dao.getByFilter(filter);
    assertThat(result).hasSize(1);
    assertPattern(result.get(0), pattern2);

    // Exact filter on name
    filter.searchFilters = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SearchFilter(
        ProprietaryComponentNamePatternFilter.SearchFilter.FilterableField.PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME,
        "testNamePattern2x"));
    result = dao.getByFilter(filter);
    assertThat(result).hasSize(1);
    assertPattern(result.get(0), pattern2);

    // Contains filter on name
    filter.searchFilters = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SearchFilter(
        ProprietaryComponentNamePatternFilter.SearchFilter.FilterableField.PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME,
        "NamePattern2"));
    result = dao.getByFilter(filter);
    assertThat(result).hasSize(1);
    assertPattern(result.get(0), pattern2);
  }

  @Test
  public void testGetByFilter_NoFiltersOrSorting() {
    // Create patterns in reverse order so the creation order doesn't match the sorting
    ProprietaryComponentNamePattern pattern3 = tempEntity.newProprietaryComponentNamePattern(repoManId, repoId, "maven",
        "testNamespacePattern3", "testNamePattern3");
    ProprietaryComponentNamePattern pattern2 = tempEntity.newProprietaryComponentNamePattern(repoManId, repoId, "maven",
        "testNamespacePattern2", "testNamePattern2");
    ProprietaryComponentNamePattern pattern1 = tempEntity.newProprietaryComponentNamePattern(repoManId, repoId, "maven",
        "testNamespacePattern1", "testNamePattern1");

    // Null filters and sorting
    ProprietaryComponentNamePatternFilter filter = new ProprietaryComponentNamePatternFilter();
    filter.page = 1;
    filter.pageSize = 3;
    // No sorting specified - defaults to namespace+name
    List<ProprietaryComponentNamePattern> result = dao.getByFilter(filter);
    assertThat(result).hasSize(3);
    assertPattern(result.get(0), pattern1);
    assertPattern(result.get(1), pattern2);
    assertPattern(result.get(2), pattern3);

    // Empty filters and sorting
    filter.searchFilters = Collections.emptyList();
    filter.sortFields = Collections.emptyList();
    // No sorting specified - defaults to namespace+name
    result = dao.getByFilter(filter);
    assertThat(result).hasSize(3);
    assertPattern(result.get(0), pattern1);
    assertPattern(result.get(1), pattern2);
    assertPattern(result.get(2), pattern3);
  }

  @Test
  public void testGetByFilter_SortOnNamespaceAndName() {
    // Create patterns in reverse order so the creation order doesn't match the sorting
    ProprietaryComponentNamePattern pattern3 = tempEntity.newProprietaryComponentNamePattern(repoManId, repoId, "maven",
        "testNamespacePattern3", "testNamePattern3");
    ProprietaryComponentNamePattern pattern2 = tempEntity.newProprietaryComponentNamePattern(repoManId, repoId, "maven",
        "testNamespacePattern2", "testNamePattern2");
    ProprietaryComponentNamePattern pattern1 = tempEntity.newProprietaryComponentNamePattern(repoManId, repoId, "maven",
        "testNamespacePattern1", "testNamePattern1");

    ProprietaryComponentNamePatternFilter filter = new ProprietaryComponentNamePatternFilter();
    filter.page = 1;
    filter.pageSize = 3;

    // Sort on namespace+name ASC
    filter.sortFields = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SortField(
        ProprietaryComponentNamePatternFilter.SortField.SortableField.PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME,
        true /* asc */, 1 /* sortPriority */));
    List<ProprietaryComponentNamePattern> result = dao.getByFilter(filter);
    assertThat(result).hasSize(3);
    assertPattern(result.get(0), pattern1);
    assertPattern(result.get(1), pattern2);
    assertPattern(result.get(2), pattern3);

    // Sort on namespace+name DESC
    filter.sortFields = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SortField(
        ProprietaryComponentNamePatternFilter.SortField.SortableField.PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME,
        false /* asc */, 1 /* sortPriority */));
    result = dao.getByFilter(filter);
    assertThat(result).hasSize(3);
    assertPattern(result.get(0), pattern3);
    assertPattern(result.get(1), pattern2);
    assertPattern(result.get(2), pattern1);
  }

  @Test
  public void testGetByFilter_SortOnRepositoryManagerInstanceId() {
    // Create patterns in reverse order so the creation order doesn't match the sorting
    RepositoryManager repositoryManager3 = tempEntity.newRepositoryManager("testInstanceId3");
    ProprietaryComponentNamePattern pattern3 = tempEntity.newProprietaryComponentNamePattern(
        repositoryManager3.getInstanceId(), repoId, "maven", "testNamespacePattern", "testNamePattern");
    RepositoryManager repositoryManager2 = tempEntity.newRepositoryManager("testInstanceId2");
    ProprietaryComponentNamePattern pattern2 = tempEntity.newProprietaryComponentNamePattern(
        repositoryManager2.getInstanceId(), repoId, "maven", "testNamespacePattern", "testNamePattern");
    RepositoryManager repositoryManager1 = tempEntity.newRepositoryManager("testInstanceId1");
    ProprietaryComponentNamePattern pattern1 = tempEntity.newProprietaryComponentNamePattern(
        repositoryManager1.getInstanceId(), repoId, "maven", "testNamespacePattern", "testNamePattern");

    ProprietaryComponentNamePatternFilter filter = new ProprietaryComponentNamePatternFilter();
    filter.page = 1;
    filter.pageSize = 3;

    // Sort on repo manager instance ID ASC
    filter.sortFields = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SortField(
        ProprietaryComponentNamePatternFilter.SortField.SortableField.REPOSITORY_MANAGER_INSTANCE_ID, true /* asc */,
        1 /* sortPriority */));
    List<ProprietaryComponentNamePattern> result = dao.getByFilter(filter);
    assertThat(result).hasSize(3);
    assertPattern(result.get(0), pattern1);
    assertPattern(result.get(1), pattern2);
    assertPattern(result.get(2), pattern3);

    // Sort on repo manager instance ID DESC
    filter.sortFields = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SortField(
        ProprietaryComponentNamePatternFilter.SortField.SortableField.REPOSITORY_MANAGER_INSTANCE_ID, false /* asc */,
        1 /* sortPriority */));
    result = dao.getByFilter(filter);
    assertThat(result).hasSize(3);
    assertPattern(result.get(0), pattern3);
    assertPattern(result.get(1), pattern2);
    assertPattern(result.get(2), pattern1);
  }

  @Test
  public void testGetByFilter_SortOnRepositoryPublicId() {
    // Create patterns in reverse order so the creation order doesn't match the sorting
    ProprietaryComponentNamePattern pattern3 = tempEntity.newProprietaryComponentNamePattern(repoManId,
        "testRepoPublicId3", "maven", "testNamespacePattern", "testNamePattern");
    ProprietaryComponentNamePattern pattern2 = tempEntity.newProprietaryComponentNamePattern(repoManId,
        "testRepoPublicId2", "maven", "testNamespacePattern", "testNamePattern");
    ProprietaryComponentNamePattern pattern1 = tempEntity.newProprietaryComponentNamePattern(repoManId,
        "testRepoPublicId1", "maven", "testNamespacePattern", "testNamePattern");

    ProprietaryComponentNamePatternFilter filter = new ProprietaryComponentNamePatternFilter();
    filter.page = 1;
    filter.pageSize = 3;

    // Sort on repo public ID ASC
    filter.sortFields = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SortField(
        ProprietaryComponentNamePatternFilter.SortField.SortableField.REPOSITORY_PUBLIC_ID, true /* asc */,
        1 /* sortPriority */));
    List<ProprietaryComponentNamePattern> result = dao.getByFilter(filter);
    assertThat(result).hasSize(3);
    assertPattern(result.get(0), pattern1);
    assertPattern(result.get(1), pattern2);
    assertPattern(result.get(2), pattern3);

    // Sort on repo public ID DESC
    filter.sortFields = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SortField(
        ProprietaryComponentNamePatternFilter.SortField.SortableField.REPOSITORY_PUBLIC_ID, false /* asc */,
        1 /* sortPriority */));
    result = dao.getByFilter(filter);
    assertThat(result).hasSize(3);
    assertPattern(result.get(0), pattern3);
    assertPattern(result.get(1), pattern2);
    assertPattern(result.get(2), pattern1);
  }

  @Test
  public void testGetByFilter_SortPriorities() {
    RepositoryManager repositoryManager1 = tempEntity.newRepositoryManager("testInstanceId1");
    ProprietaryComponentNamePattern pattern1 = tempEntity.newProprietaryComponentNamePattern(
        repositoryManager1.getInstanceId(), repoId, "maven", "testNamespacePattern1", "");
    ProprietaryComponentNamePattern pattern2 = tempEntity.newProprietaryComponentNamePattern(
        repositoryManager1.getInstanceId(), repoId, "maven", "testNamespacePattern2", "");
    RepositoryManager repositoryManager2 = tempEntity.newRepositoryManager("testInstanceId2");
    ProprietaryComponentNamePattern pattern3 = tempEntity.newProprietaryComponentNamePattern(
        repositoryManager2.getInstanceId(), repoId, "maven", "testNamespacePattern3", "");

    ProprietaryComponentNamePatternFilter filter = new ProprietaryComponentNamePatternFilter();
    filter.page = 1;
    filter.pageSize = 3;

    // Sort on repo manager instance ID ASC and namespace ASC
    filter.sortFields = new ArrayList<>();
    filter.sortFields.add(new ProprietaryComponentNamePatternFilter.SortField(
        ProprietaryComponentNamePatternFilter.SortField.SortableField.PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME,
        true /* asc */, 2 /* sortPriority */));
    filter.sortFields.add(new ProprietaryComponentNamePatternFilter.SortField(
        ProprietaryComponentNamePatternFilter.SortField.SortableField.REPOSITORY_MANAGER_INSTANCE_ID, true /* asc */,
        1 /* sortPriority */));
    List<ProprietaryComponentNamePattern> result = dao.getByFilter(filter);
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
        ProprietaryComponentNamePatternFilter.SortField.SortableField.REPOSITORY_MANAGER_INSTANCE_ID, true /* asc */,
        1 /* sortPriority */));
    result = dao.getByFilter(filter);
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
        ProprietaryComponentNamePatternFilter.SortField.SortableField.REPOSITORY_MANAGER_INSTANCE_ID, false /* asc */,
        1 /* sortPriority */));
    result = dao.getByFilter(filter);
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
        ProprietaryComponentNamePatternFilter.SortField.SortableField.REPOSITORY_MANAGER_INSTANCE_ID, false /* asc */,
        1 /* sortPriority */));
    result = dao.getByFilter(filter);
    assertThat(result).hasSize(3);
    assertPattern(result.get(0), pattern3);
    assertPattern(result.get(1), pattern2);
    assertPattern(result.get(2), pattern1);
  }

  @Test
  public void testGetByFilter_FiltersAndSorting() {
    RepositoryManager repositoryManager1 = tempEntity.newRepositoryManager("testInstanceId1");
    ProprietaryComponentNamePattern pattern1 = tempEntity.newProprietaryComponentNamePattern(
        repositoryManager1.getInstanceId(), repoId, "maven", "testNamespacePattern1", "");
    RepositoryManager repositoryManager2 = tempEntity.newRepositoryManager("testInstanceId2");
    ProprietaryComponentNamePattern pattern2 = tempEntity.newProprietaryComponentNamePattern(
        repositoryManager2.getInstanceId(), repoId, "maven", "testNamespacePattern1", "");
    RepositoryManager repositoryManager3 = tempEntity.newRepositoryManager("testInstanceId3");
    tempEntity.newProprietaryComponentNamePattern(repositoryManager3.getInstanceId(), repoId, "maven",
        "testNamespacePattern3", "");

    ProprietaryComponentNamePatternFilter filter = new ProprietaryComponentNamePatternFilter();
    filter.page = 1;
    filter.pageSize = 3;

    // Filter on namespace and sort on repo manager instance ID DESC
    filter.searchFilters = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SearchFilter(
        ProprietaryComponentNamePatternFilter.SearchFilter.FilterableField.PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME,
        "testNamespacePattern1"));
    filter.sortFields = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SortField(
        ProprietaryComponentNamePatternFilter.SortField.SortableField.REPOSITORY_MANAGER_INSTANCE_ID, false /* asc */,
        1 /* sortPriority */));

    List<ProprietaryComponentNamePattern> result = dao.getByFilter(filter);
    assertThat(result).hasSize(2);
    assertPattern(result.get(0), pattern2);
    assertPattern(result.get(1), pattern1);
  }

  private void assertPattern(ProprietaryComponentNamePattern actual, ProprietaryComponentNamePattern expected) {
    assertThat(actual.getNamespacePattern()).isEqualTo(expected.getNamespacePattern());
    assertThat(actual.getNamePattern()).isEqualTo(expected.getNamePattern());
    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getRepositoryManagerInstanceId()).isEqualTo(expected.getRepositoryManagerInstanceId());
    assertThat(actual.getRepositoryPublicId()).isEqualTo(expected.getRepositoryPublicId());
    assertThat(actual.getFormat()).isEqualTo(expected.getFormat());
  }
}
