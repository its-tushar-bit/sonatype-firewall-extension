/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.roi;

import java.math.BigDecimal;
import javax.inject.Inject;
import com.sonatype.insight.error.exception.NotFoundException;

import com.sonatype.insight.brain.dataaccess.roi.RoiConfigurationDefaultValuesDAO;
import com.sonatype.insight.brain.model.roi.CurrencyTypes;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class RoiConfigurationServiceTest extends AbstractComponentTest
{
  @Inject
  private RoiConfigurationService roiConfigurationService;

  @Inject
  private RoiConfigurationDefaultValuesDAO dao;

  @Before
  public  void setup() {
    dao.getAll().forEach(dao::delete);
  }

  @Test
  public void testGetRoiConfigurationByCurrencyType() {
    tempEntity.createRoiConfigurationDefaultValues(
        CurrencyTypes.USD,
        BigDecimal.valueOf(100),
        BigDecimal.valueOf(50),
        3600L,
        1440L,
        BigDecimal.valueOf(12000),
        BigDecimal.valueOf(6000),
        true,
        BigDecimal.valueOf(24000),
        BigDecimal.valueOf(12000),
        true,
        BigDecimal.valueOf(72000),
        BigDecimal.valueOf(36000),
        false,
        BigDecimal.valueOf(144000),
        BigDecimal.valueOf(72000),
        false,
        BigDecimal.valueOf(4350000),
        BigDecimal.valueOf(500000),
        BigDecimal.valueOf(35000),
        BigDecimal.valueOf(10000),
        BigDecimal.valueOf(25000),
        BigDecimal.valueOf(5000),
        false
    );

    tempEntity.createRoiConfiguration(
        CurrencyTypes.USD,
        BigDecimal.valueOf(100),
        1448L,
        true,
        BigDecimal.valueOf(23000),
        true,
        BigDecimal.valueOf(30000),
        false,
        BigDecimal.valueOf(45000),
        false,
        BigDecimal.valueOf(40000),
        BigDecimal.valueOf(50000),
        BigDecimal.valueOf(60000),
        BigDecimal.valueOf(70000),
        false
    );
    RoiConfigurationCurrentAndMinimumValuesDTO roiConfigurationActual =
        roiConfigurationService.getCurrentAndMinimumValuesByCurrencyType("usd");
    assertThat(roiConfigurationActual).isNotNull();
    assertRoiConfigurationEntityValues(roiConfigurationActual);
  }

  @Test
  public void testGetRoiConfigurationByCurrencyType_NotFound() {
    tempEntity.createRoiConfiguration(
        CurrencyTypes.USD,
        BigDecimal.valueOf(100),
        1448L,
        true,
        BigDecimal.valueOf(23000),
        true,
        BigDecimal.valueOf(30000),
        false,
        BigDecimal.valueOf(45000),
        false,
        BigDecimal.valueOf(40000),
        BigDecimal.valueOf(50000),
        BigDecimal.valueOf(60000),
        BigDecimal.valueOf(70000),
        false
    );
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> roiConfigurationService.getCurrentAndMinimumValuesByCurrencyType("aud"))
        .withMessage("Provided currency type aud is not found");
  }

  private void assertRoiConfigurationEntityValues(
      RoiConfigurationCurrentAndMinimumValuesDTO roiConfiguration)
  {
    assertThat(roiConfiguration.currency).isEqualTo(CurrencyTypes.USD);
    assertThat(roiConfiguration.fixRateHoursValue).isEqualTo(1448L);
    assertThat(roiConfiguration.fixRateHoursMinimum).isEqualTo(1440L);
    assertThat(roiConfiguration.developerHourlyRateValue).isEqualTo(BigDecimal.valueOf(100));
    assertThat(roiConfiguration.developerHourlyRateMinimum).isEqualTo(BigDecimal.valueOf(50));
    assertThat(roiConfiguration.securityViolationCriticalEnabled).isTrue();
    assertThat(roiConfiguration.securityViolationCriticalValue).isEqualTo(BigDecimal.valueOf(23000));
    assertThat(roiConfiguration.securityViolationCriticalValueMinimum).isEqualTo(BigDecimal.valueOf(6000));
    assertThat(roiConfiguration.securityViolationHighEnabled).isTrue();
    assertThat(roiConfiguration.securityViolationHighValue).isEqualTo(BigDecimal.valueOf(30000));
    assertThat(roiConfiguration.securityViolationHighValueMinimum).isEqualTo(BigDecimal.valueOf(12000));
    assertThat(roiConfiguration.securityViolationMediumEnabled).isFalse();
    assertThat(roiConfiguration.securityViolationMediumValue).isEqualTo(BigDecimal.valueOf(45000));
    assertThat(roiConfiguration.securityViolationMediumValueMinimum).isEqualTo(BigDecimal.valueOf(36000));
    assertThat(roiConfiguration.securityViolationLowEnabled).isFalse();
    assertThat(roiConfiguration.securityViolationLowValue).isEqualTo(BigDecimal.valueOf(40000));
    assertThat(roiConfiguration.securityViolationLowValueMinimum).isEqualTo(BigDecimal.valueOf(72000));
    assertThat(roiConfiguration.namespaceAttacksBlockedValue).isEqualTo(BigDecimal.valueOf(60000));
    assertThat(roiConfiguration.namespaceAttacksBlockedValueMinimum).isEqualTo(BigDecimal.valueOf(10000));
    assertThat(roiConfiguration.supplyChainAttacksBlockedValue).isEqualTo(BigDecimal.valueOf(50000));
    assertThat(roiConfiguration.supplyChainAttacksBlockedValueMinimum).isEqualTo(BigDecimal.valueOf(500000));
    assertThat(roiConfiguration.safeComponentsAutoSelectedValue).isEqualTo(BigDecimal.valueOf(70000));
    assertThat(roiConfiguration.safeComponentsAutoSelectedValueMinimum).isEqualTo(BigDecimal.valueOf(5000));
    assertThat(roiConfiguration.waivedPoliciesCounted).isFalse();
  }
}
