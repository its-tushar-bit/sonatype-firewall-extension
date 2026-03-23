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
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import org.jooq.Table;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ComponentObligation.COMPONENT_OBLIGATION;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

/**
 * @since 1.105
 */
@Named
@Singleton
public class ComponentObligationDAO
    extends AbstractOperationalSqlDAO<ComponentObligation>
{
  private final OwnerDAO ownerDAO;

  @Inject
  public ComponentObligationDAO(
      final OperationalDataStore operationalDataStore,
      final OwnerDAO ownerDAO)
  {
    super(operationalDataStore);
    this.ownerDAO = ownerDAO;
  }

  public List<ComponentObligation> getByOwnerId(TransactionContext tx, String ownerId) {
    return tx.dsl()
        .selectFrom(COMPONENT_OBLIGATION)
        .where(COMPONENT_OBLIGATION.OWNER_ID.eq(ownerId))
        .fetch(super::toEntity);
  }

  public List<ComponentObligation> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public ComponentObligation getByOwnerIdAndComponentIdentifierAndObligationName(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier,
      String obligationName)
  {
    return super.toEntity(tx.dsl()
        .selectFrom(COMPONENT_OBLIGATION)
        .where(COMPONENT_OBLIGATION.OWNER_ID.eq(ownerId))
        .and(COMPONENT_OBLIGATION.COMPONENT_ID_FORMAT.eq(componentIdentifier.getFormat()))
        .and(COMPONENT_OBLIGATION.COMPONENT_ID_COORDINATES_JSON.eq(
            ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates())))
        .and(COMPONENT_OBLIGATION.OBLIGATION_NAME.eq(obligationName))
        .fetchOne());
  }

  public ComponentObligation getByOwnerIdAndComponentIdentifierAndObligationName(
      String ownerId,
      ComponentIdentifier componentIdentifier,
      String obligationName)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndComponentIdentifierAndObligationName(tx, ownerId, componentIdentifier, obligationName);
    }
  }

  public List<ComponentObligation> getByOwnerIdAndComponentIdentifier(
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndComponentIdentifier(tx, ownerId, componentIdentifier);
    }
  }

  public List<ComponentObligation> getByOwnerIdAndComponentIdentifier(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    return tx.dsl()
        .selectFrom(COMPONENT_OBLIGATION)
        .where(COMPONENT_OBLIGATION.OWNER_ID.eq(ownerId))
        .and(COMPONENT_OBLIGATION.COMPONENT_ID_FORMAT.eq(componentIdentifier.getFormat()))
        .and(COMPONENT_OBLIGATION.COMPONENT_ID_COORDINATES_JSON.eq(
            ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates())))
        .fetch(super::toEntity);
  }

  public List<ComponentObligation> getByOwnerIdsAndComponentIdentifierAndObligationNames(
      TransactionContext tx,
      List<String> ownerIds,
      ComponentIdentifier componentIdentifier,
      Set<String> obligationNames)
  {
    List<ComponentObligation> componentObligationsFromDb = tx.dsl()
        .selectFrom(COMPONENT_OBLIGATION)
        .where(COMPONENT_OBLIGATION.OWNER_ID.in(ownerIds))
        .and(COMPONENT_OBLIGATION.COMPONENT_ID_FORMAT.eq(componentIdentifier.getFormat()))
        .and(COMPONENT_OBLIGATION.COMPONENT_ID_COORDINATES_JSON.eq(
            ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates())))
        .and(COMPONENT_OBLIGATION.OBLIGATION_NAME.in(obligationNames))
        .fetch(super::toEntity);

    Map<String, List<ComponentObligation>> ownerIdAndComponentObligation =
        componentObligationsFromDb.stream().collect(groupingBy(ComponentObligation::getOwnerId, toList()));

    Set<String> missingObligations = new HashSet<>(obligationNames);
    List<ComponentObligation> results = new ArrayList<>();
    for (String ownerId : ownerIds) {
      if (!ownerIdAndComponentObligation.containsKey(ownerId)) {
        continue;
      }

      List<ComponentObligation> obligationsFromOwner = ownerIdAndComponentObligation.get(ownerId);
      for (ComponentObligation componentObligation : obligationsFromOwner) {
        if (missingObligations.contains(componentObligation.getObligationName())) {
          results.add(componentObligation);

          missingObligations.remove(componentObligation.getObligationName());
          if (missingObligations.isEmpty()) {
            return results;
          }
        }
      }
    }

    return results;
  }

  public List<ComponentObligation> getByOwnerIdAndComponentIdentifierWithHierarchy(
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    Map<String, ComponentObligation> nameToObligationMap = new HashMap<>();

    try (TransactionContext tx = createTransactionContext()) {
      for (Owner owner : ownerDAO.walkHierarchy(tx, ownerId)) {
        List<ComponentObligation> componentObligations =
            getByOwnerIdAndComponentIdentifier(tx, owner.getId(), componentIdentifier);
        for (ComponentObligation componentObligation : componentObligations) {
          nameToObligationMap.putIfAbsent(componentObligation.getObligationName(), componentObligation);
        }
      }
    }

    return new ArrayList<>(nameToObligationMap.values());
  }

  public Map<ComponentIdentifier, Set<String>> getAddressedObligationsByOwnerIdWithHierarchy(String ownerId) {
    // Keeps a set of all obligations saved by component to avoid considering them when querying the parent owner
    Map<ComponentIdentifier, Set<String>> componentObligationsFound = new HashMap<>();
    // Keeps a set of fulfilled or ignored obligations by component
    Map<ComponentIdentifier, Set<String>> componentObligationsAddressed = new HashMap<>();

    try (TransactionContext tx = createTransactionContext()) {
      for (Owner owner : ownerDAO.walkHierarchy(ownerId)) {
        getByOwnerId(tx, owner.getId()).forEach(componentObligation -> {
          Set<String> obligationNames = componentObligationsFound
              .computeIfAbsent(componentObligation.getComponentIdentifier(), key -> new HashSet<>());
          if (obligationNames.add(componentObligation.getObligationName())
              && (componentObligation.getStatus() == ObligationStatus.FULFILLED
                  || componentObligation.getStatus() == ObligationStatus.IGNORED))
          {
            // The obligation was not saved in the scope of the previous owner and has been addressed in the current one
            Set<String> obligationNamesAddressed = componentObligationsAddressed
                .computeIfAbsent(componentObligation.getComponentIdentifier(), key -> new HashSet<>());
            obligationNamesAddressed.add(componentObligation.getObligationName());
          }
        });
      }
    }

    return componentObligationsAddressed;
  }

  @Override
  public void insert(TransactionContext tx, ComponentObligation componentObligation) {
    if (getByOwnerIdAndComponentIdentifierAndObligationName(tx, componentObligation.getOwnerId(),
        componentObligation.getComponentIdentifier(), componentObligation.getObligationName()) != null)
    {
      throw new BadRequestException(
          "Component obligation already exists for owner with id " + componentObligation.getOwnerId() +
              " and component " + componentObligation.getComponentIdentifier() + " and obligation name " +
              componentObligation.getObligationName() + ".");
    }
    if (componentObligation.getLastUpdatedAt() == null) {
      componentObligation.setLastUpdatedAt(new Date());
    }
    super.insert(tx, componentObligation);
  }

  @Override
  public void update(TransactionContext tx, ComponentObligation componentObligation) {
    if (getById(tx, componentObligation.getId()) == null) {
      throw new BadRequestException(
          "Cannot update component obligation with id " + componentObligation.getId() + " because it does not exist.");
    }
    componentObligation.setLastUpdatedAt(new Date());
    super.update(tx, componentObligation);
  }

  @Override
  public Table<?> getJooqTable() {
    return COMPONENT_OBLIGATION;
  }

  @Override
  public Class<ComponentObligation> getEntityClass() {
    return ComponentObligation.class;
  }
}
