/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.policy.AutoUnquarantinePolicyConditionType;
import com.sonatype.insight.brain.model.policy.ConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

/**
 * @since 1.107
 */
@Named
@Singleton
public class AutoUnquarantinePolicyConditionTypeDAO
    extends AbstractOperationalSqlDAO<AutoUnquarantinePolicyConditionType>
{
  @Inject
  public AutoUnquarantinePolicyConditionTypeDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public List<AutoUnquarantinePolicyConditionType> getAll() {
    String sQuery = "SELECT entity FROM AutoUnquarantinePolicyConditionType entity" + //
        " ORDER BY entity.id";
    return getList(sQuery);
  }

  @Override
  public void insert(
      final TransactionContext tx,
      final AutoUnquarantinePolicyConditionType entity)
  {
    // will throw IllegalArgumentException if the id is not a valid ConditionType
    final ConditionType conditionType = ConditionTypes.getById(entity.getId());
    if (!conditionType.isAutoUnquarantineSupported()) {
      throw new IllegalArgumentException(
          String.format("Condition type with id '%s' does not support auto release from quarantine.", entity.getId()));
    }

    final AutoUnquarantinePolicyConditionType retrievedEntity = getById(tx, conditionType.getId());
    if (retrievedEntity != null) {
      throw new BadRequestException("The condition type already exists: " + entity.getId());
    }
    super.insert(tx, entity);
  }
}
