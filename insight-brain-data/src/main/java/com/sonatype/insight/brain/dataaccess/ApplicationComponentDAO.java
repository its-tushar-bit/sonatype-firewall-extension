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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.11
 */
public class ApplicationComponentDAO
    extends AbstractOperationalSqlDAO<ApplicationComponent>
{
  private static final int H2_IN_OPERATOR_THRESHOLD_COMPLEX_QUERY = 350;

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
    new AggregateFileDAO().deleteByApplicationComponentId(tx, applicationComponent.getId());

    // Cascade to application component licenses
    new ApplicationComponentLicenseDAO().deleteByApplicationComponentId(tx, applicationComponent.getId());
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
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationIdAndHash(tx, appId, hash);
    }
  }

  public List<ApplicationComponent> getByApplicationIdAndHash(TransactionContext tx, String appId, String hash) {
    String sQuery = "SELECT entity FROM ApplicationComponent entity" + //
        " WHERE entity.applicationId=?1 AND entity.hash=?2";
    return getList(tx, sQuery, appId, hash);
  }

  public List<ApplicationComponent> getByApplicationIdAndComponentIdentifier(
      String appId,
      ComponentIdentifier componentIdentifier)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationIdAndComponentIdentifier(tx, appId, componentIdentifier);
    }
  }

  public List<ApplicationComponent> getByApplicationIdAndComponentIdentifier(
      TransactionContext tx,
      String appId,
      ComponentIdentifier componentIdentifier)
  {
    String sQuery = "SELECT entity FROM ApplicationComponent entity" + //
        " WHERE entity.applicationId=?1 AND entity.componentIdFormat=?2 AND entity.componentIdCoordinatesJson=?3";
    return getList(tx, sQuery, appId, componentIdentifier.getFormat(),
        ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates()));
  }

  public ApplicationComponent getLastByHash(String hash) {
    String sQuery = "SELECT entity FROM ApplicationComponent entity" + //
        " WHERE entity.hash=?1" + //
        " ORDER BY entity.time DESC";
    return createQuery(sQuery, hash).forceSingleResult().get();
  }

  public ApplicationComponent getLastByComponentIdentifier(ComponentIdentifier componentIdentifier) {
    String sQuery = "SELECT entity FROM ApplicationComponent entity" + //
        " WHERE entity.componentIdFormat=?1 AND entity.componentIdCoordinatesJson=?2" +
        " ORDER BY entity.time DESC";
    return createQuery(sQuery, componentIdentifier.getFormat(),
        ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates())).forceSingleResult().get();
  }

  public List<ApplicationComponent> getByApplicationIdsAndStageTypeIdsSince(Set<String> applicationIds,
                                                                            Set<String> stageTypeIds,
                                                                            Date date)
  {
    if (applicationIds != null && applicationIds.size() >= getInOperatorThreshold()) {
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

  public List<ApplicationComponent> getByApplicationIdsAndStageTypeIds(
      Set<String> applicationIds,
      Set<String> stageTypeIds)
  {
    if (applicationIds != null && applicationIds.size() >= getInOperatorThreshold()) {
      String sQuery = "SELECT entity FROM ApplicationComponent entity" + //
          " WHERE entity.stageTypeId IN (?1)";

      List<ApplicationComponent> applicationComponents = getList(sQuery, stageTypeIds);
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
          " WHERE entity.applicationId IN (?1) AND entity.stageTypeId IN (?2)";
      return getList(sQuery, applicationIds, stageTypeIds);
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
          "   LEFT JOIN (SELECT lo.owner_id, lo.component_id_format, lo.component_id_coordinates_json, lol.license_id" +
          "              FROM insight_brain_ods.license_override lo, insight_brain_ods.license_override_license lol" +
          "              WHERE lol.license_override_id = lo.license_override_id) li3" + //
          "     ON li3.owner_id = ?1" + //
          "     AND li3.component_id_format = ac.component_id_format" + //
          "    AND li3.component_id_coordinates_json = ac.component_id_coordinates_json" + //
          "   LEFT JOIN insight_brain_ods.application_component_license acl" + //
          "     ON acl.application_component_id = ac.application_component_id" + //
          " WHERE ac.stage_type_id IN " + buildPositionalParameters(stageTypeIds, 2) + //
          " AND COALESCE(li.license_id, li2.license_id, li3.license_id, acl.effective_license_id) IN " + //
          buildPositionalParameters(licenseIds, stageTypeIds.size() + 2);

      boolean requiresManualFilter = requiresManualFilter(applicationIds);

      if (!requiresManualFilter) {
        sQuery += " AND ac.application_id IN "
            + buildPositionalParameters(applicationIds, stageTypeIds.size() + licenseIds.size() + 2);
      }

      javax.persistence.Query query = tx.createNativeQuery(sQuery);

      query.setParameter(1, Organization.ROOT_ORGANIZATION_ID);
      addPositionalParameters(query, stageTypeIds, 2);
      addPositionalParameters(query, licenseIds, stageTypeIds.size() + 2);

      if (!requiresManualFilter) {
        addPositionalParameters(query, applicationIds, stageTypeIds.size() + licenseIds.size() + 2);
        return query.getResultList();
      }

      return ((Stream<Object[]>) query.getResultStream()).parallel()
          .filter(array -> applicationIds.contains(array[0].toString()))
          .collect(Collectors.toList());
    }
  }

  /**
   * Queries the combination of applications IDs and stage type IDs where the components found in the last evaluation
   * have a review of the license legal obligations already started or not.
   *
   * A license legal obligations review is considered started in an application and stage type when there is a least one
   * entry for a component in {@ComponentObligation} whether at the application, organization or root organization scope
   * while a not started review is when there is not a single entry.
   *
   * @param applicationIds  Applications IDs where the query can be made.
   * @param stageTypeIds    Stage type IDs where the query can be made.
   * @param isReviewStarted {@code true} to query the applications and stage types where the review already started,
   *                        {@code false} to query the ones where the review hasn't started.
   * @return A list of Object arrays with 2 positions: the application ID and the stage type ID.
   */
  @SuppressWarnings("unchecked")
  public List<Object[]> getApplicationIdsAndStageTypeIdsByReviewStatus(
      Set<String> applicationIds,
      Set<String> stageTypeIds,
      boolean isReviewStarted)
  {
    String reviewStartedCondition = isReviewStarted ? "" : " NOT ";
    String reviewStartedOperator = (isReviewStarted ? " OR " : " AND ") + reviewStartedCondition;

    try (TransactionContext tx = createTransactionContext()) {
      String sQuery = "SELECT DISTINCT ac.applicationId, ac.stageTypeId" + //
          " FROM ApplicationComponent ac, Application a" + //
          " WHERE a.id = ac.applicationId" + //
          " AND ac.stageTypeId IN (?1)" + //
          " AND (" + reviewStartedCondition + " EXISTS (SELECT 1" + //
          "                  FROM ComponentObligation co" + //
          "                  WHERE co.ownerId = ac.applicationId" + //
          "                  AND co.componentIdFormat = ac.componentIdFormat" + //
          "                  AND co.componentIdCoordinatesJson = ac.componentIdCoordinatesJson)" + //
          reviewStartedOperator + " EXISTS (SELECT 1" + //
          "                  FROM ComponentObligation co" + //
          "                  WHERE co.ownerId = a.organizationId" + //
          "                  AND co.componentIdFormat = ac.componentIdFormat" + //
          "                  AND co.componentIdCoordinatesJson = ac.componentIdCoordinatesJson)" + //
          reviewStartedOperator + " EXISTS (SELECT 1" + //
          "                  FROM ComponentObligation co" + //
          "                  WHERE co.ownerId = ?2" + //
          "                  AND co.componentIdFormat = ac.componentIdFormat" + //
          "                  AND co.componentIdCoordinatesJson = ac.componentIdCoordinatesJson))";

      boolean requiresManualFilter = requiresManualFilter(applicationIds);

      if (!requiresManualFilter) {
        sQuery += " AND ac.applicationId IN (?3)";
      }

      javax.persistence.Query query = tx.createQuery(sQuery);
      query.setParameter(1, stageTypeIds);
      query.setParameter(2, Organization.ROOT_ORGANIZATION_ID);

      if (!requiresManualFilter) {
        query.setParameter(3, applicationIds);
        return query.getResultList();
      }

      return ((Stream<Object[]>) query.getResultStream()).parallel()
          .filter(array -> applicationIds.contains(array[0].toString()))
          .collect(Collectors.toList());
    }
  }

  private boolean requiresManualFilter(Collection<?> items) {
    return isDatabaseEmbedded() && items.size() >= H2_IN_OPERATOR_THRESHOLD_COMPLEX_QUERY;
  }
}
