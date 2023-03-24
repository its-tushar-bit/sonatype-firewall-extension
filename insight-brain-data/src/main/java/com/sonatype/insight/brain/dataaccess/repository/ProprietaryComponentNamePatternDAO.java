/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternFilter.SearchFilter;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternFilter.SortField;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.collections.CollectionUtils;

public class ProprietaryComponentNamePatternDAO
    extends AbstractOperationalSqlDAO<ProprietaryComponentNamePattern>
{
  @Override
  protected ProprietaryComponentNamePattern getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM ProprietaryComponentNamePattern entity WHERE entity.id = ?1";
    return get(tx, sQuery, id);
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

  public void deleteByRepositoryManager(String repositoryManagerInstanceId) {
    String sQuery = "SELECT DISTINCT entity.repositoryPublicId FROM ProprietaryComponentNamePattern entity"
        + " WHERE entity.repositoryManagerInstanceId=?1";
    List<String> repositoryPublicIds = new Query<String>(sQuery, repositoryManagerInstanceId).getList();
    for (String repositoryPublicId : repositoryPublicIds) {
      deleteByRepository(repositoryManagerInstanceId, repositoryPublicId);
    }
  }

  public void deleteByRepository(String repositoryManagerInstanceId, String repositoryPublicId) {
    String sQuery = "DELETE FROM ProprietaryComponentNamePattern entity"
        + " WHERE entity.repositoryManagerInstanceId=?1 AND entity.repositoryPublicId=?2";
    createQuery(sQuery, repositoryManagerInstanceId, repositoryPublicId).executeUpdate();
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

  public List<ProprietaryComponentNamePattern> getByFilter(
      ProprietaryComponentNamePatternFilter filter)
  {
    // We need two SELECTs to be able to have namespace_pattern and name_pattern concatenated,
    // so we can filter and sort on it.
    String innerSelect = "SELECT proprietary_component_name_pattern_id, " + //
        "format, " + //
        "namespace_pattern, " + //
        "name_pattern, " + //
        "CONCAT(namespace_pattern, name_pattern) as pattern, " + // to be able to sort and filter on both fields
        "repository_manager_instance_id, " + //
        "repository_public_id, " + //
        "enabled" + //
        " FROM " + OperationalDataStoreProvider.getDatabaseSchema() + ".proprietary_component_name_pattern";
    String sQuery = "SELECT proprietary_component_name_pattern_id, " + //
        "format, " + //
        "namespace_pattern, " + //
        "name_pattern, " + //
        "pattern, " + //
        "repository_manager_instance_id, " + //
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
          case REPOSITORY_MANAGER_INSTANCE_ID:
            sQuery += "repository_manager_instance_id";
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
      javax.persistence.Query query = tx.createNativeQuery(sQuery.toString());
      for (int iParam = 0; iParam < queryParams.size(); iParam++) {
        query.setParameter(iParam + 1, queryParams.get(iParam));
      }
      query.setFirstResult((filter.page - 1) * filter.pageSize);
      // Incremented page size to help UI determine whether to enable / disable NextPage button
      query.setMaxResults(filter.pageSize + 1);

      List<ProprietaryComponentNamePattern> results = ((Stream<Object[]>) query.getResultStream())
          .map(array -> new ProprietaryComponentNamePattern( //
              (String) array[0], // id
              (String) array[1], // format
              (String) array[2], // namespacePattern
              (String) array[3], // namePattern
              // Skip 4 - the concatenated namespacePattern+namePattern
              (String) array[5], // repositoryManagerInstanceId
              (String) array[6], // repositoryPublicId
              (boolean) array[7])) // enabled
          .collect(Collectors.toList());

      return results;
    }
  }
}
