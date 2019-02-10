/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.11
 */
public class ApplicationComponentDAO
    extends AbstractOperationalSqlDAO<ApplicationComponent>
{
  private static final int IN_OPERATOR_THRESHOLD = 2000;

  @Override
  public ApplicationComponent getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM ApplicationComponent entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  @Override
  public void update(TransactionContext tx, ApplicationComponent entity) {
    throw new UnsupportedOperationException("ApplicationComponent does not support update operations");
  }

  public List<ApplicationComponent> getByApplicationId(TransactionContext tx, String appId) {
    String sQuery = "SELECT entity FROM ApplicationComponent entity" + //
        " WHERE entity.applicationId=?1";
    return getList(tx, sQuery, appId);
  }

  public List<ApplicationComponent> getByApplicationId(String appId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationId(tx, appId);
    }
  }

  public List<ApplicationComponent> getByApplicationIdAndStageTypeId(String appId, String stageTypeId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationIdAndStageTypeId(tx, appId, stageTypeId);
    }
  }

  public List<ApplicationComponent> getByApplicationIdAndStageTypeId(TransactionContext tx,
                                                                     String appId,
                                                                     String stageTypeId)
  {
    String sQuery = "SELECT entity FROM ApplicationComponent entity" + //
        " WHERE entity.applicationId=?1 AND entity.stageTypeId=?2";
    return getList(tx, sQuery, appId, stageTypeId);
  }

  public ApplicationComponent getByApplicationIdAndStageTypeIdAndHash(String appId, String stageTypeId, String hash) {
    String sQuery = "SELECT entity FROM ApplicationComponent entity" + //
        " WHERE entity.applicationId=?1 AND entity.stageTypeId=?2 and entity.hash=?3";
    return get(sQuery, appId, stageTypeId, hash);
  }

  public List<ApplicationComponent> getByApplicationIdAndHash(String appId, String hash) {
    String sQuery = "SELECT entity FROM ApplicationComponent entity" + //
        " WHERE entity.applicationId=?1 AND entity.hash=?2";
    return getList(sQuery, appId, hash);
  }

  public ApplicationComponent getLastByHash(String hash) {
    String sQuery = "SELECT entity FROM ApplicationComponent entity" + //
        " WHERE entity.hash=?1" + //
        " ORDER BY entity.time DESC";
    return createQuery(sQuery, hash).forceSingleResult().get();
  }

  public List<ApplicationComponent> getByApplicationIdsAndStageTypeIdsSince(Set<String> applicationIds,
                                                                            Set<String> stageTypeIds,
                                                                            Date date)
  {
    if (applicationIds != null && applicationIds.size() >= IN_OPERATOR_THRESHOLD) {
      return getByApplicationIdsAndStageTypeIdsSinceManualApplicationFilter(applicationIds, stageTypeIds, date);
    }
    else {
      String sQuery = "SELECT entity FROM ApplicationComponent entity" + //
          " WHERE entity.applicationId IN (?1) AND entity.stageTypeId IN (?2) AND entity.time >= ?3" + //
          " ORDER BY entity.time ASC";
      return getList(sQuery, applicationIds, stageTypeIds, date);
    }
  }

  @SuppressWarnings("checkstyle:LineLength")
  private List<ApplicationComponent> getByApplicationIdsAndStageTypeIdsSinceManualApplicationFilter(Set<String> applicationIds,
                                                                                                    Set<String> stageTypeIds,
                                                                                                    Date date)
  {
    String sQuery = "SELECT entity FROM ApplicationComponent entity" + //
        " WHERE entity.stageTypeId IN (?1) AND entity.time >= ?2" + //
        " ORDER BY entity.time ASC";

    List<ApplicationComponent> applicationComponents = getList(sQuery, stageTypeIds, date);
    List<ApplicationComponent> retval = new ArrayList<>();

    for (ApplicationComponent applicationComponent : applicationComponents) {
      if (applicationIds.contains(applicationComponent.getApplicationId())) {
        retval.add(applicationComponent);
      }
    }

    return retval;
  }
}
