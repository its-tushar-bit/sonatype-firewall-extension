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
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.105
 */
public class ComponentObligationAttributionDAO
    extends AbstractOperationalSqlDAO<ComponentObligationAttribution>
{
  public List<ComponentObligationAttribution> getAll() {
    String sQuery = "SELECT entity FROM ComponentObligationAttribution entity";
    return getList(sQuery);
  }

  @Override
  public ComponentObligationAttribution getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM ComponentObligationAttribution entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public ComponentObligationAttribution getByIdNotNull(TransactionContext tx, String id) {
    ComponentObligationAttribution componentObligationAttribution = getById(tx, id);
    if (componentObligationAttribution == null) {
      throw new NotFoundException("ComponentObligationAttribution with ID " + id + " does not exist.");
    }
    return componentObligationAttribution;
  }

  public ComponentObligationAttribution getByIdNotNull(String id) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByIdNotNull(tx, id);
    }
  }

  public List<ComponentObligationAttribution> getByOwnerId(TransactionContext tx, String ownerId) {
    String sQuery = "SELECT entity FROM ComponentObligationAttribution entity" + //
        " WHERE entity.ownerId=?1";
    return getList(tx, sQuery, ownerId);
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
    String sQuery = "SELECT entity FROM ComponentObligationAttribution entity" + //
        " WHERE entity.ownerId=?1" + //
        " AND entity.componentIdFormat=?2" + //
        " AND entity.componentIdCoordinatesJson=?3" + //
        " AND entity.obligationName IN (?4)";
    return getList(tx, sQuery, ownerId, componentIdentifier.getFormat(),
        ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates()), obligationNames);
  }

  public List<ComponentObligationAttribution> getByOwnerIdAndComponentIdentifier(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    String sQuery = "SELECT entity FROM ComponentObligationAttribution entity" + //
        " WHERE entity.ownerId=?1" + //
        " AND entity.componentIdFormat=?2" + //
        " AND entity.componentIdCoordinatesJson=?3";
    return getList(tx, sQuery, ownerId, componentIdentifier.getFormat(),
        ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates()));
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
    OwnerDAO ownerDAO = new OwnerDAO();
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
    OwnerDAO ownerDAO = new OwnerDAO();
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
}
