/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.math.BigDecimal;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.insight.brain.dataaccess.roi.RoiConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.roi.RoiConfigurationDefaultValuesDAO;
import com.sonatype.insight.brain.model.roi.CurrencyTypes;
import com.sonatype.insight.brain.model.roi.RoiConfiguration;
import com.sonatype.insight.brain.model.roi.RoiConfigurationDefaultValues;
import com.sonatype.insight.dataaccess.TransactionContext;

public abstract class AbstractRoiConfigurationPlaywrightTest
    extends AbstractIqUiTest
{
  protected static final Integer SEED_BASELINE_DAYS = 30;

  protected static final BigDecimal SEED_DAILY_RISK = new BigDecimal("800");

  /**
   * The {@code RoiConfigurationDefaultValues} row seeded here is intentionally left in place
   * across tests — {@link #deleteRoiConfiguration()} only removes the mutable
   * {@code RoiConfiguration} row. Defaults are stable, once-per-fork seed data that every
   * test in this hierarchy relies on. If a future test needs to mutate defaults, scope the row
   * through {@code TemporaryEntity} rather than relaxing this invariant.
   */
  protected void seedRoiConfigurationDefaultsIfMissing() {
    RoiConfigurationDefaultValuesDAO dao = lookup(RoiConfigurationDefaultValuesDAO.class);
    if (dao.getByCurrencyType(CurrencyTypes.USD) != null) {
      return;
    }
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.insert(tx, new RoiConfigurationDefaultValues(
          CurrencyTypes.USD,
          new BigDecimal("4350000"),
          new BigDecimal("500000"),
          new BigDecimal("35000"),
          new BigDecimal("10000"),
          new BigDecimal("25000"),
          new BigDecimal("5000"),
          30,
          15,
          new BigDecimal("800"),
          new BigDecimal("400")));
      tx.commit();
    }
  }

  protected void seedRoiConfigurationIfMissing() {
    RoiConfigurationDAO dao = lookup(RoiConfigurationDAO.class);
    if (dao.getByCurrencyType(CurrencyTypes.USD) != null) {
      return;
    }
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.insert(tx, new RoiConfiguration(CurrencyTypes.USD, SEED_BASELINE_DAYS, SEED_DAILY_RISK));
      tx.commit();
    }
  }

  protected void deleteRoiConfiguration() {
    RoiConfigurationDAO dao = lookup(RoiConfigurationDAO.class);
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      RoiConfiguration current = dao.getByCurrencyType(CurrencyTypes.USD);
      if (current != null) {
        dao.delete(tx, current);
      }
      tx.commit();
    }
  }
}
