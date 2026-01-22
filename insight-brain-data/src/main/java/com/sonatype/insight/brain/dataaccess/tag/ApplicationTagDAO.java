/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.tag;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.brain.model.tag.ApplicationTagNameDTO;

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

  public List<ApplicationTag> getByApplicationId(String appId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationId(tx, appId);
    }
  }

  public List<ApplicationTag> getByApplicationId(TransactionContext tx, String appId) {
    String sQuery = "SELECT entity FROM ApplicationTag entity" + //
        " WHERE entity.applicationId=?1";
    return getList(tx, sQuery, appId);
  }

  public ApplicationTag getByApplicationIdAndTagId(String appId, String tagId) {
    String sQuery = "SELECT entity FROM ApplicationTag entity" + //
        " WHERE entity.applicationId=?1 AND entity.tagId=?2";
    return get(sQuery, appId, tagId);
  }

  public List<ApplicationTag> getByTagId(String tagId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByTagId(tx, tagId);
    }
  }

  public List<ApplicationTag> getByTagId(TransactionContext tx, String tagId) {
    String sQuery = "SELECT entity FROM ApplicationTag entity" + //
        " WHERE entity.tagId=?1";
    return getList(tx, sQuery, tagId);
  }

  /**
   * Retrieve list of Tags applied to any Applications in an Organization.
   */
  public List<ApplicationTag> getByOrganizationId(String organizationId) {
    String sQuery = "SELECT appTag FROM ApplicationTag appTag, Tag tag" + //
        " WHERE appTag.tagId = tag.id AND tag.organizationId =?1";
    return getList(sQuery, organizationId);
  }

  public List<ApplicationTag> getByApplicationIds(List<String> applicationIds) {
    if (applicationIds == null || applicationIds.isEmpty()) {
      return Collections.emptyList();
    }

    String sQuery = "SELECT entity FROM ApplicationTag entity" + //
        " WHERE entity.applicationId IN ?1";

    return getListWithSqlInClause(applicationIds, inClauseValuesPartition -> getList(sQuery, inClauseValuesPartition));
  }

  @SuppressWarnings("unchecked")
  public List<ApplicationTagNameDTO> getPaginatedApplicationIdsWithTags(
      final int page,
      final int pageSize)
  {
    String sQuery =
        "SELECT application_tag.application_id, tag.name" +
            " FROM " + getDatabaseSchema() + ".application_tag application_tag" +
            " INNER JOIN " + getDatabaseSchema() + ".tag tag ON application_tag.tag_id = tag.tag_id" +
            " ORDER BY application_tag.application_id, tag.name";

    try (TransactionContext tx = createTransactionContext()) {
      int offSet = (page - 1) * pageSize;
      jakarta.persistence.Query nativeQuery = createNativePaginationQuery(tx, sQuery, offSet, pageSize);
      List<Object[]> resultList = nativeQuery.getResultList();
      return resultList.stream()
          .map(result -> new ApplicationTagNameDTO((String) result[0], (String) result[1]))
          .collect(Collectors.toList());
    }
  }
}
