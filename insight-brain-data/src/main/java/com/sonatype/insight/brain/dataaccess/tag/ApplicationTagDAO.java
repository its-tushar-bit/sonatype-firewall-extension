/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.tag;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.brain.model.tag.ApplicationTagNameDTO;

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ApplicationTag.APPLICATION_TAG;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.Tag.TAG;

/**
 * @since 1.9
 */
@Named
@Singleton
public class ApplicationTagDAO
    extends AbstractOperationalSqlDAO<ApplicationTag>
{
  @Inject
  public ApplicationTagDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public Table<?> getJooqTable() {
    return APPLICATION_TAG;
  }

  public List<ApplicationTag> getByApplicationId(String appId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationId(tx, appId);
    }
  }

  public List<ApplicationTag> getByApplicationId(TransactionContext tx, String appId) {
    return tx.dsl()
        .selectFrom(APPLICATION_TAG)
        .where(APPLICATION_TAG.APPLICATION_ID.eq(appId))
        .fetch()
        .map(this::toEntity);
  }

  public ApplicationTag getByApplicationIdAndTagId(String appId, String tagId) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(APPLICATION_TAG)
          .where(APPLICATION_TAG.APPLICATION_ID.eq(appId)
              .and(APPLICATION_TAG.TAG_ID.eq(tagId)))
          .fetchOne());
    }
  }

  public List<ApplicationTag> getByTagId(String tagId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByTagId(tx, tagId);
    }
  }

  public List<ApplicationTag> getByTagId(TransactionContext tx, String tagId) {
    return tx.dsl()
        .selectFrom(APPLICATION_TAG)
        .where(APPLICATION_TAG.TAG_ID.eq(tagId))
        .fetch()
        .map(this::toEntity);
  }

  /**
   * Retrieve list of Tags applied to any Applications in an Organization.
   */
  public List<ApplicationTag> getByOrganizationId(String organizationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(APPLICATION_TAG.fields())
          .from(APPLICATION_TAG)
          .join(TAG)
          .on(APPLICATION_TAG.TAG_ID.eq(TAG.TAG_ID))
          .where(TAG.ORGANIZATION_ID.eq(organizationId))
          .fetch()
          .map(this::toEntity);
    }
  }

  public Map<String, List<ApplicationTag>> getByOrganizationIds(Collection<String> organizationIds) {
    if (organizationIds == null || organizationIds.isEmpty()) {
      return Collections.emptyMap();
    }
    try (TransactionContext tx = createTransactionContext()) {
      return getListWithSqlInClause(organizationIds,
          chunk -> tx.dsl()
              .select(APPLICATION_TAG.fields())
              .select(TAG.ORGANIZATION_ID)
              .from(APPLICATION_TAG)
              .join(TAG)
              .on(APPLICATION_TAG.TAG_ID.eq(TAG.TAG_ID))
              .where(TAG.ORGANIZATION_ID.in(chunk))
              .fetch(r -> Map.entry(r.get(TAG.ORGANIZATION_ID), toEntity(r.into(APPLICATION_TAG)))))
                  .stream()
                  .collect(Collectors.groupingBy(Map.Entry::getKey,
                      Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }
  }

  public List<ApplicationTag> getByApplicationIds(List<String> applicationIds) {
    if (applicationIds == null || applicationIds.isEmpty()) {
      return Collections.emptyList();
    }

    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(APPLICATION_TAG)
          .where(APPLICATION_TAG.APPLICATION_ID.in(applicationIds))
          .fetch()
          .map(this::toEntity);
    }
  }

  public List<ApplicationTagNameDTO> getPaginatedApplicationIdsWithTags(
      final int page,
      final int pageSize)
  {
    try (TransactionContext tx = createTransactionContext()) {
      int offset = (page - 1) * pageSize;
      return tx.dsl()
          .select(APPLICATION_TAG.APPLICATION_ID, TAG.NAME)
          .from(APPLICATION_TAG)
          .join(TAG)
          .on(APPLICATION_TAG.TAG_ID.eq(TAG.TAG_ID))
          .orderBy(APPLICATION_TAG.APPLICATION_ID, TAG.NAME)
          .offset(offset)
          .limit(pageSize)
          .fetch(r -> new ApplicationTagNameDTO(r.value1(), r.value2()));
    }
  }

  @Override
  public Class<ApplicationTag> getEntityClass() {
    return ApplicationTag.class;
  }
}
