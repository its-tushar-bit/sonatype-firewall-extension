/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.label;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

public class ComponentLabelDAO
    extends AbstractOperationalSqlDAO<ComponentLabel>
{
  @Override
  protected ComponentLabel getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM ComponentLabel entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public List<ComponentLabel> getByLabelId(TransactionContext tx, String labelId) {
    String sQuery = "SELECT entity FROM ComponentLabel entity" + //
        " WHERE entity.labelId=?1";
    return getList(tx, sQuery, labelId);
  }

  public List<ComponentLabel> getByLabelId(String labelId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByLabelId(tx, labelId);
    }
  }

  public List<ComponentLabel> getByLabelIdAndOwnerIds(TransactionContext tx, String labelId, Set<String> ownerIds) {
    String sQuery = "SELECT entity FROM ComponentLabel entity" + //
        " WHERE entity.labelId=?1 AND entity.ownerId IN (?2)";
    return getList(tx, sQuery, labelId, ownerIds);
  }

  public List<ComponentLabel> getByOwnerIdAndHash(String ownerId, String hash) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndHash(tx, ownerId, hash);
    }
  }

  public List<ComponentLabel> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public List<ComponentLabel> getByOwnerId(TransactionContext tx, String ownerId) {
    final String sQuery = "SELECT label FROM ComponentLabel label" + //
        " WHERE label.ownerId=?1";
    return getList(tx, sQuery, ownerId);
  }

  public List<ComponentLabel> getByOwnerIdAndHash(TransactionContext tx, String ownerId, String hash) {
    final String sQuery = "SELECT label FROM ComponentLabel label" + //
        " WHERE label.ownerId=?1 AND label.hash=?2";

    List<ComponentLabel> labels = new ArrayList<>();
    OwnerDAO ownerDAO = new OwnerDAO();
    for (Owner owner : ownerDAO.walkHierarchy(tx, ownerId)) {
      labels.addAll(getList(tx, sQuery, owner.getId(), hash));
    }
    return labels;
  }

  /**
   * Gets the component label applied to a given component and context (org/app) using the specified label.
   * 
   * @since 1.6
   */
  public ComponentLabel getByOwnerIdAndHashAndLabelId(String ownerId, String hash, String labelId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndHashAndLabelId(tx, ownerId, hash, labelId);
    }
  }

  private ComponentLabel getByOwnerIdAndHashAndLabelId(TransactionContext tx,
                                                       String ownerId,
                                                       String hash,
                                                       String labelId)
  {
    String sQuery = "SELECT entity FROM ComponentLabel entity" + //
        " WHERE entity.ownerId=?1 AND entity.hash=?2 AND entity.labelId=?3";
    return get(tx, sQuery, ownerId, hash, labelId);
  }

  public List<ComponentLabel> getByOwnerIds(Collection<String> ownerIds) {
    final String sQuery = "SELECT label FROM ComponentLabel label" + //
        " WHERE label.ownerId IN (?1)";
    return getList(sQuery, ownerIds);
  }

  /**
   * @since 1.35
   */
  public List<ComponentLabel> getAll() {
    String sQuery = "SELECT entity FROM ComponentLabel entity";
    return getList(sQuery);
  }

  @Override
  public void insert(TransactionContext tx, ComponentLabel entity) {
    validate(tx, entity);
    super.insert(tx, entity);
  }

  @Override
  public void update(TransactionContext tx, ComponentLabel entity) {
    validate(tx, entity);
    super.update(tx, entity);
  }

  private void validate(TransactionContext tx, ComponentLabel entity) {
    LabelDAO labelDAO = new LabelDAO();
    Label label = labelDAO.getByIdNotNull(tx, entity.getLabelId());
    ComponentLabel other =
        getByOwnerIdAndHashAndLabelId(tx, entity.getOwnerId(), entity.getHash(), entity.getLabelId());
    if (other != null && !other.getId().equals(entity.getId())) {
      throw new BadRequestException("The label '" + label.getLabel() + "' is already applied to the component "
          + entity.getHash() + ".");
    }
    if (!isLabelApplicable(tx, label, entity.getOwnerId(), labelDAO)) {
      throw new BadRequestException("The label '" + label.getLabel() + "' is not applicable for the selected context "
          + entity.getOwnerId() + ".");
    }
  }

  private boolean isLabelApplicable(TransactionContext tx, Label label, String ownerId, LabelDAO labelDAO) {
    if (label.getOwnerId().equals(ownerId)) {
      return true;
    }
    for (Label applicable : labelDAO.getByOwnerId(tx, ownerId, true)) {
      if (applicable.getId().equals(label.getId())) {
        return true;
      }
    }
    return false;
  }
}
