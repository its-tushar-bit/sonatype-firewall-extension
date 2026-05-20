/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.consumption;

import java.util.Optional;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.consumption.ConsumptionLimitConfig;
import com.sonatype.insight.brain.model.consumption.EnforcementMode;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.UpdatableRecord;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ConsumptionLimitConfig.CONSUMPTION_LIMIT_CONFIG;

@Named
@Singleton
public class ConsumptionLimitConfigDAO
    extends AbstractOperationalSqlDAO<ConsumptionLimitConfig>
{
  @Inject
  public ConsumptionLimitConfigDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public Table<?> getJooqTable() {
    return CONSUMPTION_LIMIT_CONFIG;
  }

  @Override
  public Class<ConsumptionLimitConfig> getEntityClass() {
    return ConsumptionLimitConfig.class;
  }

  @Override
  protected UpdatableRecord<?> fromEntity(final UpdatableRecord<?> record, final ConsumptionLimitConfig entity) {
    super.fromEntity(record, entity);
    if (entity.getEnforcementMode() != null) {
      record.set(CONSUMPTION_LIMIT_CONFIG.ENFORCEMENT_MODE, entity.getEnforcementMode().name());
    }
    return record;
  }

  @Override
  protected ConsumptionLimitConfig toEntity(final Record record) {
    if (record == null) {
      return null;
    }
    ConsumptionLimitConfig entity = super.toEntity(record);
    String enforcementModeStr = record.get(CONSUMPTION_LIMIT_CONFIG.ENFORCEMENT_MODE);
    if (enforcementModeStr != null) {
      entity.setEnforcementMode(EnforcementMode.valueOf(enforcementModeStr));
    }
    return entity;
  }

  public Optional<ConsumptionLimitConfig> getConfig(final String orgId) {
    try (TransactionContext tx = createTransactionContext()) {
      Record record = tx.dsl()
          .selectFrom(CONSUMPTION_LIMIT_CONFIG)
          .where(CONSUMPTION_LIMIT_CONFIG.ORG_ID.eq(orgId))
          .fetchOne();
      return Optional.ofNullable(toEntity(record));
    }
  }

  public void saveConfig(final ConsumptionLimitConfig config) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();

      generateIdIfNeeded(config);
      String enforcementModeValue = config.getEnforcementMode() != null
          ? config.getEnforcementMode().name()
          : null;

      if (tx.dsl().dialect() == SQLDialect.H2) {
        String existingId = tx.dsl()
            .select(CONSUMPTION_LIMIT_CONFIG.ID)
            .from(CONSUMPTION_LIMIT_CONFIG)
            .where(CONSUMPTION_LIMIT_CONFIG.ORG_ID.eq(config.getOrgId()))
            .forUpdate()
            .fetchOne(CONSUMPTION_LIMIT_CONFIG.ID);
        if (existingId != null) {
          tx.dsl()
              .update(CONSUMPTION_LIMIT_CONFIG)
              .set(CONSUMPTION_LIMIT_CONFIG.MONTHLY_LIMIT, config.getMonthlyLimit())
              .set(CONSUMPTION_LIMIT_CONFIG.WARNING_THRESHOLD_PCT, config.getWarningThresholdPct())
              .set(CONSUMPTION_LIMIT_CONFIG.ENFORCEMENT_MODE, enforcementModeValue)
              .where(CONSUMPTION_LIMIT_CONFIG.ID.eq(existingId))
              .execute();
          config.setId(existingId);
        }
        else {
          tx.dsl()
              .insertInto(CONSUMPTION_LIMIT_CONFIG)
              .columns(
                  CONSUMPTION_LIMIT_CONFIG.ID,
                  CONSUMPTION_LIMIT_CONFIG.ORG_ID,
                  CONSUMPTION_LIMIT_CONFIG.MONTHLY_LIMIT,
                  CONSUMPTION_LIMIT_CONFIG.WARNING_THRESHOLD_PCT,
                  CONSUMPTION_LIMIT_CONFIG.ENFORCEMENT_MODE)
              .values(
                  config.getId(),
                  config.getOrgId(),
                  config.getMonthlyLimit(),
                  config.getWarningThresholdPct(),
                  enforcementModeValue)
              .execute();
        }
      }
      else {
        String persistedId = tx.dsl()
            .insertInto(CONSUMPTION_LIMIT_CONFIG)
            .columns(
                CONSUMPTION_LIMIT_CONFIG.ID,
                CONSUMPTION_LIMIT_CONFIG.ORG_ID,
                CONSUMPTION_LIMIT_CONFIG.MONTHLY_LIMIT,
                CONSUMPTION_LIMIT_CONFIG.WARNING_THRESHOLD_PCT,
                CONSUMPTION_LIMIT_CONFIG.ENFORCEMENT_MODE)
            .values(
                config.getId(),
                config.getOrgId(),
                config.getMonthlyLimit(),
                config.getWarningThresholdPct(),
                enforcementModeValue)
            .onConflict(CONSUMPTION_LIMIT_CONFIG.ORG_ID)
            .doUpdate()
            .set(CONSUMPTION_LIMIT_CONFIG.MONTHLY_LIMIT, config.getMonthlyLimit())
            .set(CONSUMPTION_LIMIT_CONFIG.WARNING_THRESHOLD_PCT, config.getWarningThresholdPct())
            .set(CONSUMPTION_LIMIT_CONFIG.ENFORCEMENT_MODE, enforcementModeValue)
            .returning(CONSUMPTION_LIMIT_CONFIG.ID)
            .fetchOne(CONSUMPTION_LIMIT_CONFIG.ID);
        if (persistedId != null) {
          config.setId(persistedId);
        }
      }

      tx.commit();
    }
  }
}
