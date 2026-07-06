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

import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.Application.APPLICATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.ApplicationComponent.APPLICATION_COMPONENT;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.ApplicationComponentLicense.APPLICATION_COMPONENT_LICENSE;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.LicenseOverride.LICENSE_OVERRIDE;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.LicenseOverrideLicense.LICENSE_OVERRIDE_LICENSE;

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
  public int update(TransactionContext tx, ApplicationComponentLicense entity) {
    throw new UnsupportedOperationException("ApplicationComponentLicense does not support update operations");
  }

  public List<ApplicationComponentLicense> getByApplicationComponentId(
      TransactionContext tx,
      String applicationComponentId)
  {
    return tx.dsl()
        .selectFrom(APPLICATION_COMPONENT_LICENSE)
        .where(APPLICATION_COMPONENT_LICENSE.APPLICATION_COMPONENT_ID.eq(applicationComponentId))
        .fetchInto(ApplicationComponentLicense.class);
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
  public List<ApplicationComponentLicensesDTO> getApplicationComponentEffectiveLicensesWithOverridesAtRootOrganization(
      Set<String> applicationIds,
      Set<String> stageTypeIds)
  {
    try (TransactionContext tx = createTransactionContext()) {
      boolean requiresManualFilter = requiresManualFilter(applicationIds);

      var ac = APPLICATION_COMPONENT.as("ac");
      var a = APPLICATION.as("a");
      var acl = APPLICATION_COMPONENT_LICENSE.as("acl");
      var lo = LICENSE_OVERRIDE.as("lo");
      var lol = LICENSE_OVERRIDE_LICENSE.as("lol");

      // Build the license override subquery
      var licenseOverrideSubquery = DSL.select(
          lo.OWNER_ID,
          lo.COMPONENT_ID_FORMAT,
          lo.COMPONENT_ID_COORDINATES_JSON,
          lol.LICENSE_ID)
          .from(lo, lol)
          .where(lol.LICENSE_OVERRIDE_ID.eq(lo.LICENSE_OVERRIDE_ID))
          .asTable("li");

      var liOwnerId = licenseOverrideSubquery.field("owner_id", String.class);
      var liComponentIdFormat = licenseOverrideSubquery.field("component_id_format", String.class);
      var liComponentIdCoordinatesJson = licenseOverrideSubquery.field("component_id_coordinates_json", String.class);
      var liLicenseId = licenseOverrideSubquery.field("license_id", String.class);

      // Build the WHERE condition
      var whereCondition = ac.STAGE_TYPE_ID.in(stageTypeIds);
      if (!requiresManualFilter) {
        whereCondition = whereCondition.and(ac.APPLICATION_ID.in(applicationIds));
      }

      // Use CASE expression instead of filterWhere for aggregate (listAgg ignores NULLs)
      var licenseValue = DSL.coalesce(liLicenseId, acl.EFFECTIVE_LICENSE_ID);
      var licenseField = DSL.when(licenseValue.isNotNull(), licenseValue).otherwise((String) null);
      var query = tx.dsl()
          .select(
              ac.APPLICATION_ID,
              ac.HASH,
              ac.COMPONENT_ID_FORMAT,
              ac.COMPONENT_ID_COORDINATES_JSON,
              DSL.listAgg(licenseField, "\n").withinGroupOrderBy(licenseField).as("licenses"))
          .from(ac)
          .join(a)
          .on(a.APPLICATION_ID.eq(ac.APPLICATION_ID))
          .leftJoin(licenseOverrideSubquery)
          .on(liOwnerId.eq(Organization.ROOT_ORGANIZATION_ID)
              .and(liComponentIdFormat.eq(ac.COMPONENT_ID_FORMAT))
              .and(liComponentIdCoordinatesJson.eq(ac.COMPONENT_ID_COORDINATES_JSON)))
          .leftJoin(acl)
          .on(acl.APPLICATION_COMPONENT_ID.eq(ac.APPLICATION_COMPONENT_ID))
          .where(whereCondition)
          .groupBy(ac.APPLICATION_ID, ac.HASH, ac.COMPONENT_ID_FORMAT, ac.COMPONENT_ID_COORDINATES_JSON);

      return query.fetchStream()
          .filter(r -> !requiresManualFilter || applicationIds.contains(r.get(ac.APPLICATION_ID)))
          .filter(r -> r.get(ac.HASH) != null && r.get(ac.COMPONENT_ID_FORMAT) != null)
          .map(r -> new ApplicationComponentLicensesDTO(
              r.get(ac.APPLICATION_ID),
              r.get(ac.HASH),
              r.get(ac.COMPONENT_ID_FORMAT),
              r.get(ac.COMPONENT_ID_COORDINATES_JSON),
              r.get("licenses", String.class)))
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
  public List<ApplicationComponentLicensesDTO> getApplicationComponentEffectiveLicenses(
      String applicationId,
      Set<String> stageTypeIds)
  {
    try (TransactionContext tx = createTransactionContext()) {

      // Query original effective licenses

      var ac = APPLICATION_COMPONENT.as("ac");
      var acl = APPLICATION_COMPONENT_LICENSE.as("acl");

      // Use CASE expression instead of filterWhere for aggregate (listAgg ignores NULLs)
      var effectiveLicenseField =
          DSL.when(acl.EFFECTIVE_LICENSE_ID.isNotNull(), acl.EFFECTIVE_LICENSE_ID).otherwise((String) null);
      List<ApplicationComponentLicensesDTO> componentLicenses = tx.dsl()
          .select(
              ac.HASH,
              ac.COMPONENT_ID_FORMAT,
              ac.COMPONENT_ID_COORDINATES_JSON,
              DSL.listAgg(effectiveLicenseField, "\n").withinGroupOrderBy(effectiveLicenseField).as("licenses"))
          .from(ac)
          .leftJoin(acl)
          .on(acl.APPLICATION_COMPONENT_ID.eq(ac.APPLICATION_COMPONENT_ID))
          .where(ac.APPLICATION_ID.eq(applicationId))
          .and(ac.STAGE_TYPE_ID.in(stageTypeIds))
          .groupBy(ac.HASH, ac.COMPONENT_ID_FORMAT, ac.COMPONENT_ID_COORDINATES_JSON)
          .fetchStream()
          .filter(r -> r.get(ac.HASH) != null && r.get(ac.COMPONENT_ID_FORMAT) != null)
          .map(r -> new ApplicationComponentLicensesDTO(
              applicationId,
              r.get(ac.HASH),
              r.get(ac.COMPONENT_ID_FORMAT),
              r.get(ac.COMPONENT_ID_COORDINATES_JSON),
              r.get("licenses", String.class)))
          .collect(Collectors.toList());

      // Query and replace by license overrides, if any

      Map<ComponentIdentifier, List<ApplicationComponentLicensesDTO>> componentByComponentIdentifier = componentLicenses
          .stream()
          .collect(Collectors.groupingBy(ApplicationComponentLicensesDTO::getComponentIdentifier));

      Set<ComponentIdentifier> componentsWithOverrides = new HashSet<>();

      ownerDAO.walkHierarchy(tx, applicationId).forEach(owner -> {
        for (LicenseOverride licenseOverride : licenseOverrideDAO.getByOwnerId(tx, owner.getId())) {
          List<ApplicationComponentLicensesDTO> componentsWithLicenseOverride =
              componentByComponentIdentifier.get(licenseOverride.getComponentIdentifier());

          if (componentsWithLicenseOverride != null
              && !componentsWithOverrides.contains(licenseOverride.getComponentIdentifier()))
          {
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

  @Override
  public Table<?> getJooqTable() {
    return APPLICATION_COMPONENT_LICENSE;
  }

  @Override
  public Class<ApplicationComponentLicense> getEntityClass() {
    return ApplicationComponentLicense.class;
  }
}
