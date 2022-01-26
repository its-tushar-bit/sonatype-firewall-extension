/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.ApplicationComponentLicense;
import com.sonatype.insight.brain.model.ApplicationComponentLicensesDTO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.104
 */
public class ApplicationComponentLicenseDAO
    extends AbstractOperationalSqlDAO<ApplicationComponentLicense>
{
  private static final int H2_IN_OPERATOR_THRESHOLD_COMPLEX_QUERY = 350;

  @Override
  public ApplicationComponentLicense getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM ApplicationComponentLicense entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  @Override
  public void update(TransactionContext tx, ApplicationComponentLicense entity) {
    throw new UnsupportedOperationException("ApplicationComponentLicense does not support update operations");
  }

  public List<ApplicationComponentLicense> getByApplicationComponentId(
      TransactionContext tx,
      String applicationComponentId)
  {
    String sQuery = "SELECT entity FROM ApplicationComponentLicense entity" + //
        " WHERE entity.applicationComponentId=?1";
    return getList(tx, sQuery, applicationComponentId);
  }

  public List<ApplicationComponentLicense> getByApplicationComponentId(String applicationComponentId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationComponentId(tx, applicationComponentId);
    }
  }

  /**
   * Gets the effective licenses for components from an evaluation made for an application in a give state type, grouped
   * by {@link ComponentIdentifier}.
   * An effective license may come from an override (made by application, organization or root organization scope) or an
   * existing record in the table application_component_license (found during evaluation).
   * 
   * @param applicationId Application ID to query.
   * @param stageTypeIds Stage type IDs to query.
   * @return A list of {@link ApplicationComponentLicensesDTO} where a {@link ComponentIdentifier} has the list of
   *         licenses.
   */
  public List<ApplicationComponentLicensesDTO> getApplicationComponentEffectiveLicenses(
      String applicationId,
      Set<String> stageTypeIds)
  {
    return getApplicationComponentEffectiveLicenses(Collections.singleton(applicationId), stageTypeIds, false);
  }

  /**
   * Gets the effective licenses for components from an evaluation made for applications in a give state type, grouped
   * by {@link ComponentIdentifier}.
   * An effective license may come from an override (made by application, organization or root organization scope) or an
   * existing record in the table application_component_license (found during evaluation).
   * 
   * @param applicationIds Application IDs to query.
   * @param stageTypeIds Stage type IDs to query.
   * @return A list of {@link ApplicationComponentLicensesDTO} where a {@link ComponentIdentifier} has the list of
   *         licenses.
   */
  @SuppressWarnings("unchecked")
  public List<ApplicationComponentLicensesDTO> getApplicationComponentEffectiveLicenses(
      Set<String> applicationIds,
      Set<String> stageTypeIds,
      boolean isOnlyOrganizationRootScope)
  {
    try (TransactionContext tx = createTransactionContext()) {
      boolean requiresManualFilter = requiresManualFilter(applicationIds);

      String sQuery = "SELECT ac.application_id, ac.hash, ac.component_id_format," + //
          "  ac.component_id_coordinates_json," + //
          "  STRING_AGG(DISTINCT COALESCE(" + (!isOnlyOrganizationRootScope ? "li.license_id, li2.license_id, " : "") +
          "    li3.license_id, acl.effective_license_id), CHR(10)) licenses" +
          " FROM insight_brain_ods.application_component ac" + //
          "   INNER JOIN insight_brain_ods.application a" + //
          "     ON a.application_id = ac.application_id" + //
          (!isOnlyOrganizationRootScope ?
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
          "     AND li2.component_id_coordinates_json = ac.component_id_coordinates_json" : "") + //
          "   LEFT JOIN (SELECT lo.owner_id, lo.component_id_format, lo.component_id_coordinates_json, lol.license_id" +
          "              FROM insight_brain_ods.license_override lo, insight_brain_ods.license_override_license lol" +
          "              WHERE lol.license_override_id = lo.license_override_id) li3" + //
          "     ON li3.owner_id = ?1" + //
          "     AND li3.component_id_format = ac.component_id_format" + //
          "     AND li3.component_id_coordinates_json = ac.component_id_coordinates_json" + //
          "   LEFT JOIN insight_brain_ods.application_component_license acl" + //
          "     ON acl.application_component_id = ac.application_component_id" + //
          " WHERE ac.stage_type_id IN " + buildPositionalParameters(stageTypeIds, 2) + //
          (!requiresManualFilter
              ? " AND ac.application_id IN " + buildPositionalParameters(applicationIds, stageTypeIds.size() + 2)
              : "") + //
          " GROUP BY ac.application_id, ac.hash, ac.component_id_format,ac.component_id_coordinates_json";

      javax.persistence.Query query = tx.createNativeQuery(sQuery);
      query.setParameter(1, Organization.ROOT_ORGANIZATION_ID);
      addPositionalParameters(query, stageTypeIds, 2);
      if (!requiresManualFilter) {
        addPositionalParameters(query, applicationIds, stageTypeIds.size() + 2);
      }

      return ((Stream<Object[]>) query.getResultStream()).parallel()
          .filter(array -> !requiresManualFilter || applicationIds.contains(array[0]))
          .filter(array -> array[1] != null && array[2] != null)
          .map(array -> new ApplicationComponentLicensesDTO((String) array[0], (String) array[1], (String) array[2],
              (String) array[3], (String) array[4]))
          .collect(Collectors.toList());
    }
  }

  public void deleteByApplicationComponentId(TransactionContext tx, String applicationComponentId) {
    String sQuery = "DELETE FROM ApplicationComponentLicense entity" + //
        " WHERE entity.applicationComponentId=?1";
    createQuery(sQuery, applicationComponentId).executeUpdate(tx);
  }

  public void deleteByApplicationComponentId(String applicationComponentId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteByApplicationComponentId(tx, applicationComponentId);
      tx.commit();
    }
  }

  @Override
  public final void delete(TransactionContext tx, ApplicationComponentLicense applicationComponentLicense) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all entities associated with an application component.
    super.delete(tx, applicationComponentLicense);
  }

  @Override
  public final void delete(ApplicationComponentLicense applicationComponentLicense) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all entities associated with an application component.
    super.delete(applicationComponentLicense);
  }

  private boolean requiresManualFilter(Collection<?> items) {
    return (isDatabaseEmbedded() && items.size() >= H2_IN_OPERATOR_THRESHOLD_COMPLEX_QUERY)
        || items.size() >= getInOperatorThreshold();
  }
}
