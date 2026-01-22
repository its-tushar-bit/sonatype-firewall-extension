/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.telemetry;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.telemetry.HistoricalTelemetryState;

@Named
@Singleton
public class HistoricalTelemetryStateDAO
    extends AbstractOperationalSqlDAO<HistoricalTelemetryState>
{
  @Inject
  public HistoricalTelemetryStateDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }
}
