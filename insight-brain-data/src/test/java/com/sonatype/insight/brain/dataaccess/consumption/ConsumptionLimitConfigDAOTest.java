/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.consumption;

import java.util.Optional;

import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.model.consumption.ConsumptionLimitConfig;
import com.sonatype.insight.brain.model.consumption.EnforcementMode;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConsumptionLimitConfigDAOTest
    extends AbstractDataTest
{
  private ConsumptionLimitConfigDAO dao;

  @Before
  public void setup() {
    initialize();
    dao = daoFactory.createConsumptionLimitConfigDAO();
  }

  @Test
  public void getConfig_unknownOrg_returnsEmpty() {
    Optional<ConsumptionLimitConfig> result = dao.getConfig("nonexistent-org");

    assertThat(result).isEmpty();
  }

  @Test
  public void getConfig_returnsSeededConfig_withAllFieldsPreserved() {
    ConsumptionLimitConfig seeded =
        tempEntity.newConsumptionLimitConfig("org-roundtrip", 5000L, 75, EnforcementMode.HARD);

    Optional<ConsumptionLimitConfig> loaded = dao.getConfig("org-roundtrip");

    assertThat(loaded).isPresent();
    assertThat(loaded.get().getOrgId()).isEqualTo("org-roundtrip");
    assertThat(loaded.get().getMonthlyLimit()).isEqualTo(5000L);
    assertThat(loaded.get().getWarningThresholdPct()).isEqualTo(75);
    assertThat(loaded.get().getEnforcementMode()).isEqualTo(EnforcementMode.HARD);
    assertThat(loaded.get().getId()).isEqualTo(seeded.getId()).isNotNull();
  }

  @Test
  public void saveConfig_generatesIdOnFirstInsert() {
    ConsumptionLimitConfig config = new ConsumptionLimitConfig("org-id-gen");
    config.setMonthlyLimit(1000L);
    assertThat(config.getId()).isNull();

    dao.saveConfig(config);

    assertThat(config.getId()).isNotNull().isNotBlank();
    Optional<ConsumptionLimitConfig> loaded = dao.getConfig("org-id-gen");
    assertThat(loaded).isPresent();
    assertThat(loaded.get().getId()).isEqualTo(config.getId());
  }

  @Test
  public void saveConfig_twiceOnSameOrgId_upsertsAndPreservesId() {
    ConsumptionLimitConfig first =
        tempEntity.newConsumptionLimitConfig("org-upsert", 1000L, 50, EnforcementMode.SOFT);
    String originalId = first.getId();

    ConsumptionLimitConfig second = new ConsumptionLimitConfig("org-upsert");
    second.setMonthlyLimit(2000L);
    second.setWarningThresholdPct(90);
    second.setEnforcementMode(EnforcementMode.HARD);
    dao.saveConfig(second);

    Optional<ConsumptionLimitConfig> loaded = dao.getConfig("org-upsert");
    assertThat(loaded).isPresent();
    assertThat(loaded.get().getMonthlyLimit()).isEqualTo(2000L);
    assertThat(loaded.get().getWarningThresholdPct()).isEqualTo(90);
    assertThat(loaded.get().getEnforcementMode()).isEqualTo(EnforcementMode.HARD);
    assertThat(loaded.get().getId()).isEqualTo(originalId);
  }

  @Test
  public void saveConfig_nullMonthlyLimit_roundTrips() {
    ConsumptionLimitConfig config = new ConsumptionLimitConfig("org-null-limit");

    dao.saveConfig(config);

    Optional<ConsumptionLimitConfig> loaded = dao.getConfig("org-null-limit");
    assertThat(loaded).isPresent();
    assertThat(loaded.get().getMonthlyLimit()).isNull();
  }
}
