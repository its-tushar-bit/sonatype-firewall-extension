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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternFilter.SearchFilter;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternFilter.SortField;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.collections4.CollectionUtils;

@Named
@Singleton
public class ProprietaryComponentNamePatternDAO
    extends AbstractOperationalSqlDAO<ProprietaryComponentNamePattern>
{
  @Inject
  public ProprietaryComponentNamePatternDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public List<ProprietaryComponentNamePattern> getByFormat(String format) {
    String sQuery = "SELECT entity FROM ProprietaryComponentNamePattern entity WHERE entity.format = ?1";
    return getList(sQuery, format);
  }

  public List<ProprietaryComponentNamePattern> getEnabledByFormat(String format) {
    String sQuery = "SELECT entity FROM ProprietaryComponentNamePattern entity" + //
        " WHERE entity.format = ?1 AND entity.enabled = true";
    return getList(sQuery, format);
  }

  public void deleteByRepository(String repositoryId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteByRepository(tx, repositoryId);
      tx.commit();
    }
  }

  public void deleteByRepository(TransactionContext tx, String repositoryId) {
    if (detectTestEntityLeaks()) {
      // This is never executed in production
      List<ProprietaryComponentNamePattern> patterns = getByRepositoryId(tx, repositoryId);
      patterns.forEach(pattern -> delete(tx, pattern));
    }
    else {
      String sQuery = "DELETE FROM ProprietaryComponentNamePattern entity" + " WHERE entity.repositoryId=?1";
      createQuery(sQuery, repositoryId).executeUpdate(tx);
    }
  }

  @Override
  public final void delete(ProprietaryComponentNamePattern entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all patterns for a repository or repository manager.
    super.delete(entity);
  }

  @Override
  public final void delete(TransactionContext tx, ProprietaryComponentNamePattern entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all patterns for a repository or repository manager.
    super.delete(tx, entity);
  }

  public List<ProprietaryComponentNamePatternDTO> getByFilter(
      Set<String> repositoryIds,
      ProprietaryComponentNamePatternFilter filter)
  {
    if (repositoryIds.isEmpty()) {
      return Collections.emptyList();
    }

    // We need two SELECTs to be able to have namespace_pattern and name_pattern concatenated,
    // so we can filter and sort on it.
    String innerSelect = "SELECT pattern.proprietary_component_name_pattern_id, " + //
        "pattern.format, " + //
        "pattern.namespace_pattern, " + //
        "pattern.name_pattern, " + //
        // to be able to sort and filter on both fields
        "CONCAT(pattern.namespace_pattern, pattern.name_pattern) as pattern, " + //
        "repoManager.instance_id AS repository_manager_instance_id, " + //
        "repoManager.name AS repository_manager_name, " + //
        "repo.public_id AS repository_public_id, " + //
        "pattern.enabled" + //
        " FROM " + getDatabaseSchema() + ".proprietary_component_name_pattern pattern" + //
        " INNER JOIN " + getDatabaseSchema() + ".repository repo" + //
        " ON pattern.repository_id = repo.repository_id" + //
        " INNER JOIN " + getDatabaseSchema() + ".repository_manager repoManager" + //
        " ON repo.repository_manager_id = repoManager.repository_manager_id" + //
        " WHERE repo.repository_id IN " + //
        // I did not find a way to pass the list of repository IDs as query param
        "(" + repositoryIds.stream().map(repositoryId -> "'" + repositoryId + "'").collect(Collectors.joining(","))
        + ")";
    String sQuery = "SELECT proprietary_component_name_pattern_id, " + //
        "format, " + //
        "namespace_pattern, " + //
        "name_pattern, " + //
        "pattern, " + //
        "repository_manager_instance_id, " + //
        "repository_manager_name, " + //
        "repository_public_id, " + //
        "enabled" + //
        " FROM (" + innerSelect + ") AS inner_sql";

    // Filters
    List<String> queryParams = new ArrayList<>();
    if (!CollectionUtils.isEmpty(filter.searchFilters)) {
      sQuery += " WHERE ";
      int filterCount = 1;
      for (SearchFilter searchFilter : filter.searchFilters) {
        if (filterCount > 1) {
          sQuery += ", ";
        }
        switch (searchFilter.filterableField) {
          case PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME:
            sQuery += "pattern LIKE ?" + filterCount;
            queryParams.add('%' + searchFilter.value + '%');
            break;
          default:
            throw new BadRequestException("Unknown filterable field: " + searchFilter.filterableField);
        }
        filterCount++;
      }
    }

    // Sorting
    sQuery += " ORDER BY ";
    if (!CollectionUtils.isEmpty(filter.sortFields)) {
      filter.sortFields
          .sort((sortField1, sortField2) -> Integer.compare(sortField1.sortPriority, sortField2.sortPriority));

      int sortCount = 1;
      for (SortField sortField : filter.sortFields) {
        if (sortCount > 1) {
          sQuery += ", ";
        }
        switch (sortField.sortableField) {
          case PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME:
            sQuery += "pattern";
            break;
          case REPOSITORY_MANAGER_INSTANCE_ID_OR_NAME:
            sQuery += "COALESCE(repository_manager_instance_id, repository_manager_name)";
            break;
          case REPOSITORY_PUBLIC_ID:
            sQuery += "repository_public_id";
            break;
          case ENABLED:
            sQuery += "enabled";
            break;
          default:
            throw new BadRequestException("Unknown sortable field: " + sortField.sortableField);
        }
        sQuery += sortField.asc ? " ASC" : " DESC";
        sortCount++;
      }
    }
    else {
      // Always order to have consistent results
      sQuery += "pattern";
    }

    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query query = tx.createNativeQuery(sQuery.toString());
      for (int iParam = 0; iParam < queryParams.size(); iParam++) {
        query.setParameter(iParam + 1, queryParams.get(iParam));
      }
      query.setFirstResult((filter.page - 1) * filter.pageSize);
      // Incremented page size to help UI determine whether to enable / disable NextPage button
      query.setMaxResults(filter.pageSize + 1);

      List<ProprietaryComponentNamePatternDTO> results = ((Stream<Object[]>) query.getResultStream())
          .map(array -> new ProprietaryComponentNamePatternDTO( //
              (String) array[0], // id
              (String) array[1], // format
              emptyToNull((String) array[2]), // namespacePattern
              emptyToNull((String) array[3]), // namePattern
              // Skip 4 - the concatenated namespacePattern+namePattern
              (String) array[5], // repositoryManagerInstanceId
              (String) array[6], // repositoryManagerName
              (String) array[7], // repositoryPublicId
              (boolean) array[8])) // enabled
          .collect(Collectors.toList());

      return results;
    }
  }

  public List<ProprietaryComponentNamePattern> getByRepositoryId(String repositoryId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRepositoryId(tx, repositoryId);
    }
  }

  public List<ProprietaryComponentNamePattern> getByRepositoryId(TransactionContext tx, String repositoryId) {
    String sQuery = "SELECT entity FROM ProprietaryComponentNamePattern entity WHERE entity.repositoryId = ?1";
    return getList(tx, sQuery, repositoryId);
  }

  private static String emptyToNull(String value) {
    return "".equals(value) ? null : value;
  }
}
