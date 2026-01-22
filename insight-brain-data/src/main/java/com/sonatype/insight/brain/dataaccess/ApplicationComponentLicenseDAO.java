/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.ApplicationComponentLicense;
import com.sonatype.insight.brain.model.ApplicationComponentLicensesDTO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.collections4.CollectionUtils;

/**
 * @since 1.104
 */
@Named
@Singleton
public class ApplicationComponentLicenseDAO
    extends AbstractOperationalSqlDAO<ApplicationComponentLicense>
{
  private static final int H2_IN_OPERATOR_THRESHOLD_COMPLEX_QUERY = 350;

  private final LicenseOverrideDAO licenseOverrideDAO;

  private final OwnerDAO ownerDAO;

  @Inject
  public ApplicationComponentLicenseDAO(
      final OperationalDataStore operationalDataStore,
      final LicenseOverrideDAO licenseOverrideDAO,
      final OwnerDAO ownerDAO)
  {
    super(operationalDataStore);
    this.licenseOverrideDAO = licenseOverrideDAO;
    this.ownerDAO = ownerDAO;
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
   * An effective license may come from an override made at root organization scope or an existing record in the table
   * application_component_license (found during evaluation).
   *
   * @param applicationIds Application IDs to query.
   * @param stageTypeIds Stage type IDs to query.
   * @return A list of {@link ApplicationComponentLicensesDTO} where a {@link ComponentIdentifier} has the list of
   *         licenses.
   */
  @SuppressWarnings("unchecked")
  public List<ApplicationComponentLicensesDTO> getApplicationComponentEffectiveLicensesWithOverridesAtRootOrganization(
      Set<String> applicationIds,
      Set<String> stageTypeIds)
  {
    try (TransactionContext tx = createTransactionContext()) {
      boolean requiresManualFilter = requiresManualFilter(applicationIds);

      String sQuery = "SELECT ac.application_id, ac.hash, ac.component_id_format," + //
          "  ac.component_id_coordinates_json," + //
          "  STRING_AGG(DISTINCT COALESCE(li.license_id, acl.effective_license_id), CHR(10)) licenses" +
          " FROM " + getDatabaseSchema() + ".application_component ac" + //
          "   INNER JOIN " + getDatabaseSchema() + ".application a" + //
          "     ON a.application_id = ac.application_id" + //
          "   LEFT JOIN (SELECT lo.owner_id, lo.component_id_format, lo.component_id_coordinates_json, lol.license_id" +
          "              FROM " + getDatabaseSchema() + ".license_override lo, " + //
          "              " + getDatabaseSchema() + ".license_override_license lol" + //
          "              WHERE lol.license_override_id = lo.license_override_id) li" + //
          "     ON li.owner_id = ?1" + //
          "     AND li.component_id_format = ac.component_id_format" + //
          "     AND li.component_id_coordinates_json = ac.component_id_coordinates_json" + //
          "   LEFT JOIN " + getDatabaseSchema() + ".application_component_license acl" + //
          "     ON acl.application_component_id = ac.application_component_id" + //
          " WHERE ac.stage_type_id IN " + buildPositionalParameters(stageTypeIds, 2) + //
          (!requiresManualFilter
              ? " AND ac.application_id IN " + buildPositionalParameters(applicationIds, stageTypeIds.size() + 2)
              : "") + //
          " GROUP BY ac.application_id, ac.hash, ac.component_id_format,ac.component_id_coordinates_json";

      jakarta.persistence.Query query = tx.createNativeQuery(sQuery);
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
  @SuppressWarnings("unchecked")
  public List<ApplicationComponentLicensesDTO> getApplicationComponentEffectiveLicenses(
      String applicationId,
      Set<String> stageTypeIds)
  {
    try (TransactionContext tx = createTransactionContext()) {

      // Query original effective licenses

      String sQuery = "SELECT ac.hash, ac.component_id_format, ac.component_id_coordinates_json," + //
          "  STRING_AGG(DISTINCT acl.effective_license_id, CHR(10)) licenses" +
          " FROM " + getDatabaseSchema() + ".application_component ac" + //
          "   LEFT JOIN " + getDatabaseSchema() + ".application_component_license acl" + //
          "     ON acl.application_component_id = ac.application_component_id" + //
          " WHERE ac.application_id = ?1" + //
          " AND ac.stage_type_id IN " + buildPositionalParameters(stageTypeIds, 2) + //
          " GROUP BY ac.hash, ac.component_id_format,ac.component_id_coordinates_json";

      jakarta.persistence.Query query = tx.createNativeQuery(sQuery);
      query.setParameter(1, applicationId);
      addPositionalParameters(query, stageTypeIds, 2);

      List<ApplicationComponentLicensesDTO> componentLicenses =
          ((Stream<Object[]>) query.getResultStream())
              .filter(array -> array[0] != null && array[1] != null)
              .map(array -> new ApplicationComponentLicensesDTO(applicationId, (String) array[0], (String) array[1],
                  (String) array[2], (String) array[3]))
              .collect(Collectors.toList());

      // Query and replace by license overrides, if any

      Map<ComponentIdentifier, List<ApplicationComponentLicensesDTO>> componentByComponentIdentifier = componentLicenses
          .stream().collect(Collectors.groupingBy(ApplicationComponentLicensesDTO::getComponentIdentifier));

      Set<ComponentIdentifier> componentsWithOverrides = new HashSet<>();

      ownerDAO.walkHierarchy(tx, applicationId).forEach(owner -> {
        for (LicenseOverride licenseOverride : licenseOverrideDAO.getByOwnerId(tx, owner.getId())) {
          List<ApplicationComponentLicensesDTO> componentsWithLicenseOverride =
              componentByComponentIdentifier.get(licenseOverride.getComponentIdentifier());

          if (componentsWithLicenseOverride != null
              && !componentsWithOverrides.contains(licenseOverride.getComponentIdentifier())) {
            if (CollectionUtils.isNotEmpty(licenseOverride.getLicenseIds())) {
              componentsWithLicenseOverride
                  .forEach(component -> component.setLicenses(licenseOverride.getLicenseIds()));
            }
            componentsWithOverrides.add(licenseOverride.getComponentIdentifier());
          }
        }
      });

      return componentLicenses;
    }
  }

  private boolean requiresManualFilter(Collection<?> items) {
    return (isDatabaseEmbedded() && items.size() >= H2_IN_OPERATOR_THRESHOLD_COMPLEX_QUERY)
        || items.size() >= getInOperatorThreshold();
  }
}
