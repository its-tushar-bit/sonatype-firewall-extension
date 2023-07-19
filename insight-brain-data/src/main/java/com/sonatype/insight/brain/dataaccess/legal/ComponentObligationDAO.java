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
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

/**
 * @since 1.105
 */
public class ComponentObligationDAO
    extends AbstractOperationalSqlDAO<ComponentObligation>
{
  public List<ComponentObligation> getByOwnerId(TransactionContext tx, String ownerId) {
    String sQuery = "SELECT entity FROM ComponentObligation entity" + //
        " WHERE entity.ownerId=?1";
    return getList(tx, sQuery, ownerId);
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
    String sQuery = "SELECT entity FROM ComponentObligation entity" + //
        " WHERE entity.ownerId=?1" + //
        " AND entity.componentIdFormat=?2" + //
        " AND entity.componentIdCoordinatesJson=?3" + //
        " AND entity.obligationName=?4";
    return get(tx, sQuery, ownerId, componentIdentifier.getFormat(),
        ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates()), obligationName);
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

  public List<ComponentObligation> getByOwnerIdAndComponentIdentifierAndObligationNames(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier,
      Set<String> obligationNames)
  {
    String sQuery = "SELECT entity FROM ComponentObligation entity" + //
        " WHERE entity.ownerId=?1" + //
        " AND entity.componentIdFormat=?2" + //
        " AND entity.componentIdCoordinatesJson=?3" + //
        " AND entity.obligationName IN (?4)";
    return getList(tx, sQuery, ownerId, componentIdentifier.getFormat(),
        ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates()), obligationNames);
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
    String sQuery = "SELECT entity FROM ComponentObligation entity" + //
        " WHERE entity.ownerId=?1" + //
        " AND entity.componentIdFormat=?2" + //
        " AND entity.componentIdCoordinatesJson=?3";
    return getList(tx, sQuery, ownerId, componentIdentifier.getFormat(),
        ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates()));
  }

  public List<ComponentObligation> getByOwnerIdAndComponentIdentifierAndObligationNames(
      String ownerId,
      ComponentIdentifier componentIdentifier,
      Set<String> obligationNames)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndComponentIdentifierAndObligationNames(tx, ownerId, componentIdentifier, obligationNames);
    }
  }

  public List<ComponentObligation> getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier,
      Set<String> obligationNames)
  {
    List<ComponentObligation> results = new ArrayList<>();
    Set<String> missingObligations = new HashSet<>(obligationNames);
    OwnerDAO ownerDAO = new OwnerDAO();
    for (Owner owner : ownerDAO.walkHierarchy(ownerId)) {
      List<ComponentObligation> componentObligations =
          getByOwnerIdAndComponentIdentifierAndObligationNames(tx, owner.getId(), componentIdentifier,
              missingObligations);
      results.addAll(componentObligations);
      missingObligations.removeAll(componentObligations.stream()
          .map(ComponentObligation::getObligationName)
          .collect(Collectors.toSet()));
      if (missingObligations.isEmpty()) {
        break;
      }
    }
    return results;
  }

  public List<ComponentObligation> getByOwnerIdAndComponentIdentifierWithHierarchy(
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    Map<String, ComponentObligation> nameToObligationMap = new HashMap<>();
    OwnerDAO ownerDAO = new OwnerDAO();

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

  public List<ComponentObligation> getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
      String ownerId,
      ComponentIdentifier componentIdentifier,
      Set<String> obligationNames)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(tx, ownerId, componentIdentifier,
          obligationNames);
    }
  }

  public Map<ComponentIdentifier, Set<String>> getAddressedObligationsByOwnerIdWithHierarchy(String ownerId) {
    // Keeps a set of all obligations saved by component to avoid considering them when querying the parent owner
    Map<ComponentIdentifier, Set<String>> componentObligationsFound = new HashMap<>();
    // Keeps a set of fulfilled or ignored obligations by component
    Map<ComponentIdentifier, Set<String>> componentObligationsAddressed = new HashMap<>();

    try (TransactionContext tx = createTransactionContext()) {
      for (Owner owner : new OwnerDAO().walkHierarchy(ownerId)) {
        getByOwnerId(tx, owner.getId()).forEach(componentObligation -> {
          Set<String> obligationNames = componentObligationsFound
              .computeIfAbsent(componentObligation.getComponentIdentifier(), key -> new HashSet<>());
          if (obligationNames.add(componentObligation.getObligationName())
              && (componentObligation.getStatus() == ObligationStatus.FULFILLED
              || componentObligation.getStatus() == ObligationStatus.IGNORED)) {
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
        componentObligation.getComponentIdentifier(), componentObligation.getObligationName()) != null) {
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
}
