/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.model.AggregateFile;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.ApplicationComponentLicense;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.common.annotations.VisibleForTesting;

/**
 * @since 1.11
 */
public class ApplicationComponentDAO
    extends AbstractOperationalSqlDAO<ApplicationComponent>
{
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

  @Override
  public void delete(TransactionContext tx, ApplicationComponent applicationComponent) {
    // Cascade to aggregate files
    AggregateFileDAO aggregateFileDAO = new AggregateFileDAO();
    List<AggregateFile> aggregateFiles = aggregateFileDAO.getByApplicationComponentId(tx, applicationComponent.getId());
    for (AggregateFile aggregateFile : aggregateFiles) {
      aggregateFileDAO.delete(tx, aggregateFile);
    }

    // Cascade to application component licenses
    ApplicationComponentLicenseDAO applicationComponentLicenseDAO = new ApplicationComponentLicenseDAO();
    List<ApplicationComponentLicense> applicationComponentLicenses =
        applicationComponentLicenseDAO.getByApplicationComponentId(tx, applicationComponent.getId());
    for (ApplicationComponentLicense applicationComponentLicense : applicationComponentLicenses) {
      applicationComponentLicenseDAO.delete(tx, applicationComponentLicense);
    }
    super.delete(tx, applicationComponent);
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
    if (isDatabaseEmbedded() && applicationIds != null && applicationIds.size() >= H2_IN_OPERATOR_THRESHOLD) {
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
    else {
      String sQuery = "SELECT entity FROM ApplicationComponent entity" + //
          " WHERE entity.applicationId IN (?1) AND entity.stageTypeId IN (?2) AND entity.time >= ?3" + //
          " ORDER BY entity.time ASC";
      return getList(sQuery, applicationIds, stageTypeIds, date);
    }
  }

  public long getCount() {
    String sQuery = "SELECT COUNT(entity) FROM ApplicationComponent entity";
    return getSingle(Long.class, sQuery);
  }

  @SuppressWarnings("unchecked")
  public List<Object[]> getApplicationIdsAndStageTypeIdsByLicenses(
      Set<String> applicationIds,
      Set<String> stageTypeIds,
      Set<String> licenseIds)
  {
    try (TransactionContext tx = createTransactionContext()) {
      String sQuery = "SELECT DISTINCT ac.application_id, ac.stage_type_id" + //
          " FROM insight_brain_ods.application_component ac" + //
          "   INNER JOIN insight_brain_ods.application a" + //
          "     ON a.application_id = ac.application_id" + //
          "   LEFT JOIN (SELECT lo.owner_id, lo.component_id_format, lo.component_id_coordinates_json, lol.license_id" +
          "              FROM insight_brain_ods.license_override lo, insight_brain_ods.license_override_license lol" +
          "              WHERE lol.license_override_id = lo.license_override_id) li" + //
          "     ON li.owner_id = ac.application_id" + //
          "     AND li.component_id_format = ac.component_id_format" + //
          "     AND li.component_id_coordinates_json = ac.component_id_coordinates_json" + //
          "   LEFT JOIN (SELECT lo.owner_id, lo.component_id_format, lo.component_id_coordinates_json, lol.license_id" +
          "              FROM insight_brain_ods.license_override lo, insight_brain_ods.license_override_license lol" +
          "              WHERE lol.license_override_id = lo.license_override_id) li2" + //
          "     ON li2.owner_id = a.organization_id" + //
          "     AND li2.component_id_format = ac.component_id_format" + //
          "    AND li2.component_id_coordinates_json = ac.component_id_coordinates_json" + //
          "   LEFT JOIN insight_brain_ods.application_component_license acl" + //
          "     ON acl.application_component_id = ac.application_component_id" + //
          " WHERE ac.stage_type_id IN " + buildPositionalParameters(stageTypeIds, 1) + //
          " AND COALESCE(li.license_id, li2.license_id, acl.effective_license_id) IN " + //
          buildPositionalParameters(licenseIds, stageTypeIds.size() + 1);

      // For this particular query degradation starts before H2_IN_OPERATOR_THRESHOLD
      boolean requiresManualFilter = isDatabaseEmbedded() && applicationIds.size() >= 350;

      if (!requiresManualFilter) {
        sQuery += " AND ac.application_id IN "
            + buildPositionalParameters(applicationIds, stageTypeIds.size() + licenseIds.size() + 1);
      }

      javax.persistence.Query query = tx.createNativeQuery(sQuery);

      addPositionalParameters(query, stageTypeIds, 1);
      addPositionalParameters(query, licenseIds, stageTypeIds.size() + 1);

      List<Object[]> queryResult = null;

      if (!requiresManualFilter) {
        addPositionalParameters(query, applicationIds, stageTypeIds.size() + licenseIds.size() + 1);
        queryResult = query.getResultList();
      }

      queryResult = (List<Object[]>) query.getResultList().parallelStream()
          .filter(array -> applicationIds.contains(((Object[]) array)[0].toString()))
          .collect(Collectors.toList());

      return queryResult;
    }
  }

  @VisibleForTesting
  String buildPositionalParameters(Collection<?> collection, int startFrom) {
    StringJoiner joiner = new StringJoiner(",");
    for (int i = 0; i < collection.size(); i++) {
      joiner.add("?" + (i + startFrom));
    }
    return "(" + joiner.toString() + ")";
  }

  @VisibleForTesting
  void addPositionalParameters(javax.persistence.Query query, Collection<?> collection, int startFrom) {
    for (Object object : collection) {
      query.setParameter(startFrom++, object);
    }
  }
}
