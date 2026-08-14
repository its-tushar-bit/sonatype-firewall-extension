/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.telemetry;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.telemetry.HistoricalTelemetryState;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HistoricalTelemetryStateDAOTest
    extends AbstractDbDAOTest
{
  private HistoricalTelemetryStateDAO dao;

  @Override
  @BeforeEach
  public void setup() {
    dao = daoFactory.createHistoricalTelemetryStateDAO();
  }

  @Test
  public void testCrud() {
    final var purpose = "testing";
    final var status = "PENDING";
    HistoricalTelemetryState historicalTelemetryState = new HistoricalTelemetryState();
    historicalTelemetryState.setId(purpose);
    historicalTelemetryState.setCreated(new Date());
    historicalTelemetryState.setStatus(status);
    historicalTelemetryState.setCutoffDate(new Date());
    dao.insert(historicalTelemetryState);

    var state = dao.getById(purpose);
    assertThat(state).isNotNull();
    assertThat(state.getId()).isEqualTo(purpose);
    assertThat(state.getCreated()).isNotNull();
    assertThat(state.getLastUpdated()).isNull();
    assertThat(state.getStartTime()).isNull();
    assertThat(state.getStatus()).isEqualTo(status);
    assertThat(state.getMinFreeMemoryMb()).isEqualTo(10);

    state.setStartTime(new Date());
    state.setStatus("SUSPENDED");
    state.setLastUpdated(new Date());

    dao.update(state);

    state = dao.getById(purpose);
    assertThat(state).isNotNull();
    assertThat(state.getId()).isEqualTo(purpose);
    assertThat(state.getCreated()).isNotNull();
    assertThat(state.getLastUpdated()).isNotNull();
    assertThat(state.getStartTime()).isNotNull();
    assertThat(state.getStatus()).isEqualTo("SUSPENDED");

    dao.delete(state);

    state = dao.getById(purpose);
    assertThat(state).isNull();
  }
}
