/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.policy.AutoUnquarantinePolicyConditionType;
import com.sonatype.insight.brain.model.policy.ConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.AutoUnquarantinePolicyConditionType.AUTO_UNQUARANTINE_POLICY_CONDITION_TYPE;

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
  public int insert(
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
    return super.insert(tx, entity);
  }

  @Override
  public Table<?> getJooqTable() {
    return AUTO_UNQUARANTINE_POLICY_CONDITION_TYPE;
  }

  @Override
  public Class<AutoUnquarantinePolicyConditionType> getEntityClass() {
    return AutoUnquarantinePolicyConditionType.class;
  }
}
