/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ComponentObligationAttribution.COMPONENT_OBLIGATION_ATTRIBUTION;

/**
 * @since 1.105
 */
@Named
@Singleton
public class ComponentObligationAttributionDAO
    extends AbstractOperationalSqlDAO<ComponentObligationAttribution>
{
  private final OwnerDAO ownerDAO;

  @Inject
  public ComponentObligationAttributionDAO(
      final OperationalDataStore operationalDataStore,
      final OwnerDAO ownerDAO)
  {
    super(operationalDataStore);
    this.ownerDAO = ownerDAO;
  }

  public List<ComponentObligationAttribution> getByOwnerId(TransactionContext tx, String ownerId) {
    return tx.dsl()
        .selectFrom(COMPONENT_OBLIGATION_ATTRIBUTION)
        .where(COMPONENT_OBLIGATION_ATTRIBUTION.OWNER_ID.eq(ownerId))
        .fetch(super::toEntity);
  }

  public List<ComponentObligationAttribution> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public List<ComponentObligationAttribution> getByOwnerIdAndComponentIdentifierAndObligationNames(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier,
      Set<String> obligationNames)
  {
    Set<String> nonNullObligationNames = obligationNames.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    org.jooq.Condition obligationCondition;
    if (obligationNames.contains(null)) {
      obligationCondition = COMPONENT_OBLIGATION_ATTRIBUTION.OBLIGATION_NAME.in(nonNullObligationNames)
          .or(COMPONENT_OBLIGATION_ATTRIBUTION.OBLIGATION_NAME.isNull());
    }
    else {
      obligationCondition = COMPONENT_OBLIGATION_ATTRIBUTION.OBLIGATION_NAME.in(nonNullObligationNames);
    }

    return tx.dsl()
        .selectFrom(COMPONENT_OBLIGATION_ATTRIBUTION)
        .where(COMPONENT_OBLIGATION_ATTRIBUTION.OWNER_ID.eq(ownerId))
        .and(COMPONENT_OBLIGATION_ATTRIBUTION.COMPONENT_ID_FORMAT.eq(componentIdentifier.getFormat()))
        .and(COMPONENT_OBLIGATION_ATTRIBUTION.COMPONENT_ID_COORDINATES_JSON.eq(
            ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates())))
        .and(obligationCondition)
        .fetch(super::toEntity);
  }

  public List<ComponentObligationAttribution> getByOwnerIdAndComponentIdentifier(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    return tx.dsl()
        .selectFrom(COMPONENT_OBLIGATION_ATTRIBUTION)
        .where(COMPONENT_OBLIGATION_ATTRIBUTION.OWNER_ID.eq(ownerId))
        .and(COMPONENT_OBLIGATION_ATTRIBUTION.COMPONENT_ID_FORMAT.eq(componentIdentifier.getFormat()))
        .and(COMPONENT_OBLIGATION_ATTRIBUTION.COMPONENT_ID_COORDINATES_JSON.eq(
            ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates())))
        .fetch(super::toEntity);
  }

  public List<ComponentObligationAttribution> getByOwnerIdAndComponentIdentifierAndObligationNames(
      String ownerId,
      ComponentIdentifier componentIdentifier,
      Set<String> obligationNames)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndComponentIdentifierAndObligationNames(tx, ownerId, componentIdentifier, obligationNames);
    }
  }

  public List<ComponentObligationAttribution> getByOwnerIdAndComponentIdentifierWithHierarchy(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    Map<String, ComponentObligationAttribution> obligationNameToAttribution = new HashMap<>();
    for (Owner owner : ownerDAO.walkHierarchy(ownerId)) {
      List<ComponentObligationAttribution> componentObligationAttributions =
          getByOwnerIdAndComponentIdentifier(tx, owner.getId(), componentIdentifier);
      for (ComponentObligationAttribution attribution : componentObligationAttributions) {
        obligationNameToAttribution.putIfAbsent(attribution.getObligationName(), attribution);
      }
    }
    return new ArrayList<>(obligationNameToAttribution.values());
  }

  public List<ComponentObligationAttribution> getByOwnerIdAndComponentIdentifierWithHierarchy(
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndComponentIdentifierWithHierarchy(tx, ownerId, componentIdentifier);
    }
  }

  public List<ComponentObligationAttribution> getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier,
      Set<String> obligationNames)
  {
    List<ComponentObligationAttribution> results = new ArrayList<>();
    Set<String> missingObligations = new HashSet<>(obligationNames);
    for (Owner owner : ownerDAO.walkHierarchy(ownerId)) {
      List<ComponentObligationAttribution> componentObligationAttributions =
          getByOwnerIdAndComponentIdentifierAndObligationNames(tx, owner.getId(), componentIdentifier,
              missingObligations);
      results.addAll(componentObligationAttributions);
      missingObligations.removeAll(componentObligationAttributions.stream()
          .map(ComponentObligationAttribution::getObligationName)
          .collect(Collectors.toSet()));
      if (missingObligations.isEmpty()) {
        break;
      }
    }
    return results;
  }

  public List<ComponentObligationAttribution> getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
      String ownerId,
      ComponentIdentifier componentIdentifier,
      Set<String> obligationNames)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(tx, ownerId, componentIdentifier,
          obligationNames);
    }
  }

  @Override
  public void insert(TransactionContext tx, ComponentObligationAttribution componentObligationAttribution) {
    if (componentObligationAttribution.getLastUpdatedAt() == null) {
      componentObligationAttribution.setLastUpdatedAt(new Date());
    }
    super.insert(tx, componentObligationAttribution);
  }

  @Override
  public void update(TransactionContext tx, ComponentObligationAttribution componentObligationAttribution) {
    if (getById(tx, componentObligationAttribution.getId()) == null) {
      throw new BadRequestException(
          "Cannot update component obligation attribution with id " + componentObligationAttribution.getId() +
              " because it does not exist.");
    }
    componentObligationAttribution.setLastUpdatedAt(new Date());
    super.update(tx, componentObligationAttribution);
  }

  @Override
  public Table<?> getJooqTable() {
    return COMPONENT_OBLIGATION_ATTRIBUTION;
  }

  @Override
  public Class<ComponentObligationAttribution> getEntityClass() {
    return ComponentObligationAttribution.class;
  }
}
