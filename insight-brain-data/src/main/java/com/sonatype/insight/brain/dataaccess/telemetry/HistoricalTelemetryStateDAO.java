/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.telemetry;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.telemetry.HistoricalTelemetryState;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Record;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.HistoricalTelemetryState.HISTORICAL_TELEMETRY_STATE;

@Named
@Singleton
public class HistoricalTelemetryStateDAO
    extends AbstractOperationalSqlDAO<HistoricalTelemetryState>
{
  @Inject
  public HistoricalTelemetryStateDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  protected HistoricalTelemetryState toEntity(final Record record) {
    if (record == null) {
      return null;
    }
    HistoricalTelemetryState entity = super.toEntity(record);
    LocalDate cutoffDate = record.get(HISTORICAL_TELEMETRY_STATE.CUTOFF_DATE);
    entity.setCutoffDate(cutoffDate != null
        ? Date.from(cutoffDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        : null);
    return entity;
  }

  private LocalDate toLocalDate(Date date) {
    if (date == null) {
      return null;
    }
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }

  @Override
  public Table<?> getJooqTable() {
    return HISTORICAL_TELEMETRY_STATE;
  }

  @Override
  public Class<HistoricalTelemetryState> getEntityClass() {
    return HistoricalTelemetryState.class;
  }
}
