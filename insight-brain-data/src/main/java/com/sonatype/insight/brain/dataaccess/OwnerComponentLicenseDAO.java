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
import com.sonatype.insight.brain.model.OwnerComponentLicense;
import com.sonatype.insight.brain.model.OwnerComponentLicensesDTO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.collections4.CollectionUtils;

import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.Application.APPLICATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.OwnerComponent.OWNER_COMPONENT;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.OwnerComponentLicense.OWNER_COMPONENT_LICENSE;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.LicenseOverride.LICENSE_OVERRIDE;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.LicenseOverrideLicense.LICENSE_OVERRIDE_LICENSE;

/**
 * @since 1.104
 */
@Named
@Singleton
public class OwnerComponentLicenseDAO
    extends AbstractOperationalSqlDAO<OwnerComponentLicense>
{
  private static final int H2_IN_OPERATOR_THRESHOLD_COMPLEX_QUERY = 350;

  private final LicenseOverrideDAO licenseOverrideDAO;

  private final OwnerDAO ownerDAO;

  @Inject
  public OwnerComponentLicenseDAO(
      final OperationalDataStore operationalDataStore,
      final LicenseOverrideDAO licenseOverrideDAO,
      final OwnerDAO ownerDAO)
  {
    super(operationalDataStore);
    this.licenseOverrideDAO = licenseOverrideDAO;
    this.ownerDAO = ownerDAO;
  }

  @Override
  public int update(TransactionContext tx, OwnerComponentLicense entity) {
    throw new UnsupportedOperationException("OwnerComponentLicense does not support update operations");
  }

  public List<OwnerComponentLicense> getByOwnerComponentId(
      TransactionContext tx,
      String ownerComponentId)
  {
    return tx.dsl()
        .selectFrom(OWNER_COMPONENT_LICENSE)
        .where(OWNER_COMPONENT_LICENSE.OWNER_COMPONENT_ID.eq(ownerComponentId))
        .fetchInto(OwnerComponentLicense.class);
  }

  public List<OwnerComponentLicense> getByOwnerComponentId(String ownerComponentId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerComponentId(tx, ownerComponentId);
    }
  }

  /**
   * Gets the effective licenses for components from an evaluation made for an application in a give state type, grouped
   * by {@link ComponentIdentifier}.
   * An effective license may come from an override made at root organization scope or an existing record in the table
   * owner_component_license (found during evaluation).
   *
   * @param ownerIds Owner IDs to query.
   * @param stageTypeIds Stage type IDs to query.
   * @return A list of {@link OwnerComponentLicensesDTO} where a {@link ComponentIdentifier} has the list of
   *         licenses.
   */
  public List<OwnerComponentLicensesDTO> getApplicationComponentEffectiveLicensesWithOverridesAtRootOrganization(
      Set<String> ownerIds,
      Set<String> stageTypeIds)
  {
    try (TransactionContext tx = createTransactionContext()) {
      boolean requiresManualFilter = requiresManualFilter(ownerIds);

      var oc = OWNER_COMPONENT.as("oc");
      var a = APPLICATION.as("a");
      var ocl = OWNER_COMPONENT_LICENSE.as("ocl");
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
      var whereCondition = oc.STAGE_TYPE_ID.in(stageTypeIds);
      if (!requiresManualFilter) {
        whereCondition = whereCondition.and(oc.OWNER_ID.in(ownerIds));
      }

      // Use CASE expression instead of filterWhere for aggregate (listAgg ignores NULLs)
      var licenseValue = DSL.coalesce(liLicenseId, ocl.EFFECTIVE_LICENSE_ID);
      var licenseField = DSL.when(licenseValue.isNotNull(), licenseValue).otherwise((String) null);
      var query = tx.dsl()
          .select(
              oc.OWNER_ID,
              oc.HASH,
              oc.COMPONENT_ID_FORMAT,
              oc.COMPONENT_ID_COORDINATES_JSON,
              DSL.listAgg(licenseField, "\n").withinGroupOrderBy(licenseField).as("licenses"))
          .from(oc)
          .join(a)
          .on(a.APPLICATION_ID.eq(oc.OWNER_ID))
          .leftJoin(licenseOverrideSubquery)
          .on(liOwnerId.eq(Organization.ROOT_ORGANIZATION_ID)
              .and(liComponentIdFormat.eq(oc.COMPONENT_ID_FORMAT))
              .and(liComponentIdCoordinatesJson.eq(oc.COMPONENT_ID_COORDINATES_JSON)))
          .leftJoin(ocl)
          .on(ocl.OWNER_COMPONENT_ID.eq(oc.OWNER_COMPONENT_ID))
          .where(whereCondition)
          .groupBy(oc.OWNER_ID, oc.HASH, oc.COMPONENT_ID_FORMAT, oc.COMPONENT_ID_COORDINATES_JSON);

      try (var stream = query.fetchStream()
          .filter(r -> !requiresManualFilter || ownerIds.contains(r.get(oc.OWNER_ID)))
          .filter(r -> r.get(oc.HASH) != null && r.get(oc.COMPONENT_ID_FORMAT) != null)
          .map(r -> new OwnerComponentLicensesDTO(
              r.get(oc.OWNER_ID),
              r.get(oc.HASH),
              r.get(oc.COMPONENT_ID_FORMAT),
              r.get(oc.COMPONENT_ID_COORDINATES_JSON),
              r.get("licenses", String.class))))
      {
        return stream.collect(Collectors.toList());
      }
    }
  }

  /**
   * Gets the effective licenses for components from an evaluation made for an application in a give state type, grouped
   * by {@link ComponentIdentifier}.
   * An effective license may come from an override (made by application, organization or root organization scope) or an
   * existing record in the table owner_component_license (found during evaluation).
   *
   * @param ownerId Owner ID to query.
   * @param stageTypeIds Stage type IDs to query.
   * @return A list of {@link OwnerComponentLicensesDTO} where a {@link ComponentIdentifier} has the list of
   *         licenses.
   */
  public List<OwnerComponentLicensesDTO> getApplicationComponentEffectiveLicenses(
      String ownerId,
      Set<String> stageTypeIds)
  {
    try (TransactionContext tx = createTransactionContext()) {

      // Query original effective licenses

      var oc = OWNER_COMPONENT.as("oc");
      var ocl = OWNER_COMPONENT_LICENSE.as("ocl");

      // Use CASE expression instead of filterWhere for aggregate (listAgg ignores NULLs)
      var effectiveLicenseField =
          DSL.when(ocl.EFFECTIVE_LICENSE_ID.isNotNull(), ocl.EFFECTIVE_LICENSE_ID).otherwise((String) null);
      List<OwnerComponentLicensesDTO> componentLicenses;
      try (var stream = tx.dsl()
          .select(
              oc.HASH,
              oc.COMPONENT_ID_FORMAT,
              oc.COMPONENT_ID_COORDINATES_JSON,
              DSL.listAgg(effectiveLicenseField, "\n").withinGroupOrderBy(effectiveLicenseField).as("licenses"))
          .from(oc)
          .leftJoin(ocl)
          .on(ocl.OWNER_COMPONENT_ID.eq(oc.OWNER_COMPONENT_ID))
          .where(oc.OWNER_ID.eq(ownerId))
          .and(oc.STAGE_TYPE_ID.in(stageTypeIds))
          .groupBy(oc.HASH, oc.COMPONENT_ID_FORMAT, oc.COMPONENT_ID_COORDINATES_JSON)
          .fetchStream()
          .filter(r -> r.get(oc.HASH) != null && r.get(oc.COMPONENT_ID_FORMAT) != null)
          .map(r -> new OwnerComponentLicensesDTO(
              ownerId,
              r.get(oc.HASH),
              r.get(oc.COMPONENT_ID_FORMAT),
              r.get(oc.COMPONENT_ID_COORDINATES_JSON),
              r.get("licenses", String.class))))
      {
        componentLicenses = stream.collect(Collectors.toList());
      }

      // Query and replace by license overrides, if any

      Map<ComponentIdentifier, List<OwnerComponentLicensesDTO>> componentByComponentIdentifier = componentLicenses
          .stream()
          .collect(Collectors.groupingBy(OwnerComponentLicensesDTO::getComponentIdentifier));

      Set<ComponentIdentifier> componentsWithOverrides = new HashSet<>();

      for (LicenseOverride licenseOverride : licenseOverrideDAO.getByOwnerIdWithHierarchy(tx, ownerId)) {
        List<OwnerComponentLicensesDTO> componentsWithLicenseOverride =
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

      return componentLicenses;
    }
  }

  private boolean requiresManualFilter(Collection<?> items) {
    return (isDatabaseEmbedded() && items.size() >= H2_IN_OPERATOR_THRESHOLD_COMPLEX_QUERY)
        || items.size() >= getInOperatorThreshold();
  }

  @Override
  public Table<?> getJooqTable() {
    return OWNER_COMPONENT_LICENSE;
  }

  @Override
  public Class<OwnerComponentLicense> getEntityClass() {
    return OwnerComponentLicense.class;
  }
}
