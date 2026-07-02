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

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ComponentLabel.COMPONENT_LABEL;

@Named
@Singleton
public class ComponentLabelDAO
    extends AbstractOperationalSqlDAO<ComponentLabel>
{
  private final OwnerDAO ownerDAO;

  private final LabelDAO labelDAO;

  @Inject
  public ComponentLabelDAO(
      final OperationalDataStore operationalDataStore,
      final OwnerDAO ownerDAO,
      final LabelDAO labelDAO)
  {
    super(operationalDataStore);
    this.ownerDAO = ownerDAO;
    this.labelDAO = labelDAO;
  }

  public List<ComponentLabel> getByLabelId(TransactionContext tx, String labelId) {
    return tx.dsl()
        .selectFrom(COMPONENT_LABEL)
        .where(COMPONENT_LABEL.LABEL_ID.eq(labelId))
        .fetch()
        .map(this::toEntity);
  }

  public List<ComponentLabel> getByLabelId(String labelId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByLabelId(tx, labelId);
    }
  }

  public List<ComponentLabel> getByLabelIdAndOwnerIds(TransactionContext tx, String labelId, Set<String> ownerIds) {
    return tx.dsl()
        .selectFrom(COMPONENT_LABEL)
        .where(COMPONENT_LABEL.LABEL_ID.eq(labelId))
        .and(COMPONENT_LABEL.OWNER_ID.in(ownerIds))
        .fetch()
        .map(this::toEntity);
  }

  public List<ComponentLabel> getByOwnerIdAndHashWithHierarchy(String ownerId, String hash) {
    List<ComponentLabel> labels = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(ownerId)) {
      labels.addAll(getByOwnerIdAndHash(owner.getId(), hash));
    }
    return labels;
  }

  public List<ComponentLabel> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public List<ComponentLabel> getByOwnerId(TransactionContext tx, String ownerId) {
    return tx.dsl()
        .selectFrom(COMPONENT_LABEL)
        .where(COMPONENT_LABEL.OWNER_ID.eq(ownerId))
        .fetch()
        .map(this::toEntity);
  }

  public List<ComponentLabel> getByOwnerIdAndHash(String ownerId, String hash) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndHash(tx, ownerId, hash);
    }
  }

  public List<ComponentLabel> getByOwnerIdAndHash(TransactionContext tx, String ownerId, String hash) {
    return tx.dsl()
        .selectFrom(COMPONENT_LABEL)
        .where(COMPONENT_LABEL.OWNER_ID.eq(ownerId))
        .and(COMPONENT_LABEL.HASH.eq(hash))
        .fetch()
        .map(this::toEntity);
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

  private ComponentLabel getByOwnerIdAndHashAndLabelId(
      TransactionContext tx,
      String ownerId,
      String hash,
      String labelId)
  {
    return toEntity(tx.dsl()
        .selectFrom(COMPONENT_LABEL)
        .where(COMPONENT_LABEL.OWNER_ID.eq(ownerId))
        .and(COMPONENT_LABEL.HASH.eq(hash))
        .and(COMPONENT_LABEL.LABEL_ID.eq(labelId))
        .fetchOne());
  }

  public List<ComponentLabel> getByOwnerIds(Collection<String> ownerIds) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(COMPONENT_LABEL)
          .where(COMPONENT_LABEL.OWNER_ID.in(ownerIds))
          .fetch()
          .map(this::toEntity);
    }
  }

  @Override
  public int insert(TransactionContext tx, ComponentLabel entity) {
    validate(tx, entity);
    return super.insert(tx, entity);
  }

  @Override
  public void update(TransactionContext tx, ComponentLabel entity) {
    validate(tx, entity);
    super.update(tx, entity);
  }

  private void validate(TransactionContext tx, ComponentLabel entity) {
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
    for (Label applicable : labelDAO.getByOwnerIdWithHierarchy(tx, ownerId)) {
      if (applicable.getId().equals(label.getId())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Table<?> getJooqTable() {
    return COMPONENT_LABEL;
  }

  @Override
  public Class<ComponentLabel> getEntityClass() {
    return ComponentLabel.class;
  }
}
