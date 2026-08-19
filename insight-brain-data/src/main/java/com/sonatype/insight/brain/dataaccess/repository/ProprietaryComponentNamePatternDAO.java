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

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternFilter.SearchFilter;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.UpdatableRecord;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ProprietaryComponentNamePattern.PROPRIETARY_COMPONENT_NAME_PATTERN;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.Repository.REPOSITORY;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.RepositoryManager.REPOSITORY_MANAGER;

@Named
@Singleton
public class ProprietaryComponentNamePatternDAO
    extends AbstractOperationalSqlDAO<ProprietaryComponentNamePattern>
{
  @Inject
  public ProprietaryComponentNamePatternDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  protected UpdatableRecord<?> fromEntity(
      final UpdatableRecord<?> record,
      final ProprietaryComponentNamePattern entity)
  {
    super.fromEntity(record, entity);
    // DB schema comment: "using empty strings instead if needed" for uniqueness constraint
    record.set(PROPRIETARY_COMPONENT_NAME_PATTERN.NAMESPACE_PATTERN,
        entity.getNamespacePattern() != null ? entity.getNamespacePattern() : "");
    record.set(PROPRIETARY_COMPONENT_NAME_PATTERN.NAME_PATTERN,
        entity.getNamePattern() != null ? entity.getNamePattern() : "");
    return record;
  }

  @Override
  public Table<?> getJooqTable() {
    return PROPRIETARY_COMPONENT_NAME_PATTERN;
  }

  public List<ProprietaryComponentNamePattern> getByFormat(String format) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(PROPRIETARY_COMPONENT_NAME_PATTERN)
          .where(PROPRIETARY_COMPONENT_NAME_PATTERN.FORMAT.eq(format))
          .fetch(this::toEntity);
    }
  }

  public List<ProprietaryComponentNamePattern> getEnabledByFormat(String format) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(PROPRIETARY_COMPONENT_NAME_PATTERN)
          .where(PROPRIETARY_COMPONENT_NAME_PATTERN.FORMAT.eq(format))
          .and(PROPRIETARY_COMPONENT_NAME_PATTERN.ENABLED.eq(true))
          .fetch(this::toEntity);
    }
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
      tx.dsl()
          .deleteFrom(PROPRIETARY_COMPONENT_NAME_PATTERN)
          .where(PROPRIETARY_COMPONENT_NAME_PATTERN.REPOSITORY_ID.eq(repositoryId))
          .execute();
    }
  }

  @Override
  public final void delete(ProprietaryComponentNamePattern entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all patterns for a repository or repository manager.
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      delete(tx, entity);
      tx.commit();
    }
  }

  @Override
  public final void delete(TransactionContext tx, ProprietaryComponentNamePattern entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all patterns for a repository or repository manager.
    tx.dsl()
        .deleteFrom(PROPRIETARY_COMPONENT_NAME_PATTERN)
        .where(PROPRIETARY_COMPONENT_NAME_PATTERN.PROPRIETARY_COMPONENT_NAME_PATTERN_ID.eq(entity.getId()))
        .execute();
  }

  public List<ProprietaryComponentNamePatternDTO> getByFilter(
      Set<String> repositoryIds,
      ProprietaryComponentNamePatternFilter filter)
  {
    if (repositoryIds.isEmpty()) {
      return Collections.emptyList();
    }

    try (TransactionContext tx = createTransactionContext()) {
      // Create a concatenated field for namespace_pattern + name_pattern to support filtering and sorting
      Field<String> patternField = DSL.concat(
          DSL.coalesce(PROPRIETARY_COMPONENT_NAME_PATTERN.NAMESPACE_PATTERN, DSL.inline("")),
          DSL.coalesce(PROPRIETARY_COMPONENT_NAME_PATTERN.NAME_PATTERN, DSL.inline(""))).as("pattern");

      // Build the base query with joins
      var baseQuery = tx.dsl()
          .select(
              PROPRIETARY_COMPONENT_NAME_PATTERN.PROPRIETARY_COMPONENT_NAME_PATTERN_ID.as("id"),
              PROPRIETARY_COMPONENT_NAME_PATTERN.FORMAT,
              PROPRIETARY_COMPONENT_NAME_PATTERN.NAMESPACE_PATTERN,
              PROPRIETARY_COMPONENT_NAME_PATTERN.NAME_PATTERN,
              patternField,
              REPOSITORY_MANAGER.INSTANCE_ID.as("repository_manager_instance_id"),
              REPOSITORY_MANAGER.NAME.as("repository_manager_name"),
              REPOSITORY.PUBLIC_ID.as("repository_public_id"),
              PROPRIETARY_COMPONENT_NAME_PATTERN.ENABLED)
          .from(PROPRIETARY_COMPONENT_NAME_PATTERN)
          .join(REPOSITORY)
          .on(PROPRIETARY_COMPONENT_NAME_PATTERN.REPOSITORY_ID.eq(REPOSITORY.REPOSITORY_ID))
          .join(REPOSITORY_MANAGER)
          .on(REPOSITORY.REPOSITORY_MANAGER_ID.eq(REPOSITORY_MANAGER.REPOSITORY_MANAGER_ID));

      // Build conditions
      List<Condition> conditions = new ArrayList<>();
      conditions.add(REPOSITORY.REPOSITORY_ID.in(repositoryIds));

      // Add search filters
      if (!CollectionUtils.isEmpty(filter.searchFilters)) {
        for (SearchFilter searchFilter : filter.searchFilters) {
          switch (searchFilter.filterableField) {
            case PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME:
              // Match on the concatenated pattern field
              Field<String> concatField = DSL.concat(
                  DSL.coalesce(PROPRIETARY_COMPONENT_NAME_PATTERN.NAMESPACE_PATTERN, DSL.inline("")),
                  DSL.coalesce(PROPRIETARY_COMPONENT_NAME_PATTERN.NAME_PATTERN, DSL.inline("")));
              conditions.add(concatField.like("%" + searchFilter.value + "%"));
              break;
            default:
              throw new BadRequestException("Unknown filterable field: " + searchFilter.filterableField);
          }
        }
      }

      // Build sort fields
      List<org.jooq.SortField<?>> sortFields = new ArrayList<>();
      if (!CollectionUtils.isEmpty(filter.sortFields)) {
        filter.sortFields
            .sort((sortField1, sortField2) -> Integer.compare(sortField1.sortPriority, sortField2.sortPriority));

        for (ProprietaryComponentNamePatternFilter.SortField sortField : filter.sortFields) {
          Field<?> field;
          switch (sortField.sortableField) {
            case PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME:
              field = DSL.concat(
                  DSL.coalesce(PROPRIETARY_COMPONENT_NAME_PATTERN.NAMESPACE_PATTERN, DSL.inline("")),
                  DSL.coalesce(PROPRIETARY_COMPONENT_NAME_PATTERN.NAME_PATTERN, DSL.inline("")));
              break;
            case REPOSITORY_MANAGER_INSTANCE_ID_OR_NAME:
              field = DSL.coalesce(REPOSITORY_MANAGER.INSTANCE_ID, REPOSITORY_MANAGER.NAME);
              break;
            case REPOSITORY_PUBLIC_ID:
              field = REPOSITORY.PUBLIC_ID;
              break;
            case ENABLED:
              field = PROPRIETARY_COMPONENT_NAME_PATTERN.ENABLED;
              break;
            default:
              throw new BadRequestException("Unknown sortable field: " + sortField.sortableField);
          }
          sortFields.add(sortField.asc ? field.asc() : field.desc());
        }
      }
      else {
        // Default sort on concatenated pattern field
        Field<String> defaultSortField = DSL.concat(
            DSL.coalesce(PROPRIETARY_COMPONENT_NAME_PATTERN.NAMESPACE_PATTERN, DSL.inline("")),
            DSL.coalesce(PROPRIETARY_COMPONENT_NAME_PATTERN.NAME_PATTERN, DSL.inline("")));
        sortFields.add(defaultSortField.asc());
      }

      // Calculate pagination offset
      int offset = (filter.page - 1) * filter.pageSize;
      // Incremented page size to help UI determine whether to enable / disable NextPage button
      int limit = filter.pageSize + 1;

      // Execute query with conditions, sorting, and pagination
      return baseQuery
          .where(conditions)
          .orderBy(sortFields)
          .offset(offset)
          .limit(limit)
          .fetch(record -> new ProprietaryComponentNamePatternDTO(
              record.get("id", String.class),
              record.get(PROPRIETARY_COMPONENT_NAME_PATTERN.FORMAT),
              emptyToNull(record.get(PROPRIETARY_COMPONENT_NAME_PATTERN.NAMESPACE_PATTERN)),
              emptyToNull(record.get(PROPRIETARY_COMPONENT_NAME_PATTERN.NAME_PATTERN)),
              record.get("repository_manager_instance_id", String.class),
              record.get("repository_manager_name", String.class),
              record.get("repository_public_id", String.class),
              record.get(PROPRIETARY_COMPONENT_NAME_PATTERN.ENABLED)));
    }
  }

  public List<ProprietaryComponentNamePattern> getByRepositoryId(String repositoryId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRepositoryId(tx, repositoryId);
    }
  }

  public List<ProprietaryComponentNamePattern> getByRepositoryId(TransactionContext tx, String repositoryId) {
    return tx.dsl()
        .selectFrom(PROPRIETARY_COMPONENT_NAME_PATTERN)
        .where(PROPRIETARY_COMPONENT_NAME_PATTERN.REPOSITORY_ID.eq(repositoryId))
        .fetch(this::toEntity);
  }

  private static String emptyToNull(String value) {
    return "".equals(value) ? null : value;
  }

  @Override
  public Class<ProprietaryComponentNamePattern> getEntityClass() {
    return ProprietaryComponentNamePattern.class;
  }
}
