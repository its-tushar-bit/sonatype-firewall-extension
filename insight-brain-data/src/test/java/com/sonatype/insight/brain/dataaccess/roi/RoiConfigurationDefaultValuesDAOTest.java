/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.roi;

import java.math.BigDecimal;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.roi.CurrencyTypes;
import com.sonatype.insight.brain.model.roi.RoiConfigurationDefaultValues;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RoiConfigurationDefaultValuesDAOTest
    extends AbstractDbDAOTest
{
  private RoiConfigurationDefaultValuesDAO dao;

  @Before
  @Override
  public  void setup() {
    super.setup();
    dao = daoFactory.createRoiConfigurationDefaultValuesDAO();
  }

  @Test
  public void testCRUD() {
    RoiConfigurationDefaultValues roiConfigurationDefaultValues =
        tempEntity.createRoiConfigurationDefaultValues(
        CurrencyTypes.USD,
        BigDecimal.valueOf(100),
        BigDecimal.valueOf(80),
        12000L,
        13000L,
        BigDecimal.valueOf(14000),
        BigDecimal.valueOf(15000),
        true,
        BigDecimal.valueOf(16000),
        BigDecimal.valueOf(17000),
        true,
        BigDecimal.valueOf(18000),
        BigDecimal.valueOf(19000),
        false,
        BigDecimal.valueOf(20000),
        BigDecimal.valueOf(21000),
        false,
        BigDecimal.valueOf(22000),
        BigDecimal.valueOf(23000),
        BigDecimal.valueOf(24000),
        BigDecimal.valueOf(25000),
        BigDecimal.valueOf(26000),
        BigDecimal.valueOf(27000),
            false
    );
    assertThat(roiConfigurationDefaultValues.getId()).isNotNull();

    // read
    roiConfigurationDefaultValues = dao.getById(roiConfigurationDefaultValues.getId());
    assertRoiConfigurationDefaultValuesEntityValues(roiConfigurationDefaultValues);

    // update
    roiConfigurationDefaultValues.setNamespaceAttacksBlockedDefault(BigDecimal.valueOf(10000));
    dao.update(roiConfigurationDefaultValues);

    roiConfigurationDefaultValues = dao.getById(roiConfigurationDefaultValues.getId());

    assertThat(roiConfigurationDefaultValues).isNotNull();
    assertThat(roiConfigurationDefaultValues.getNamespaceAttacksBlockedDefault()).isEqualTo(
        BigDecimal.valueOf(10000));

    // delete
    String id = roiConfigurationDefaultValues.getId();
    dao.delete(roiConfigurationDefaultValues);

    assertThat(dao.getById(id)).isNull();
  }

  @Test
  public void testGetByCurrencyType() {
    RoiConfigurationDefaultValues roiConfigurationDefaultValues =
        tempEntity.createRoiConfigurationDefaultValues(
            CurrencyTypes.USD,
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(80),
            12000L,
            13000L,
            BigDecimal.valueOf(14000),
            BigDecimal.valueOf(15000),
            true,
            BigDecimal.valueOf(16000),
            BigDecimal.valueOf(17000),
            true,
            BigDecimal.valueOf(18000),
            BigDecimal.valueOf(19000),
            false,
            BigDecimal.valueOf(20000),
            BigDecimal.valueOf(21000),
            false,
            BigDecimal.valueOf(22000),
            BigDecimal.valueOf(23000),
            BigDecimal.valueOf(24000),
            BigDecimal.valueOf(25000),
            BigDecimal.valueOf(26000),
            BigDecimal.valueOf(27000),
            false
        );
    assertThat(dao.getByCurrencyType(CurrencyTypes.USD)).isNotNull();
    assertRoiConfigurationDefaultValuesEntityValues(roiConfigurationDefaultValues);
  }

  private void assertRoiConfigurationDefaultValuesEntityValues(
      RoiConfigurationDefaultValues roiConfigurationDefaultValues)
  {
    assertThat(roiConfigurationDefaultValues.getCurrency()).isEqualTo(CurrencyTypes.USD);
    assertThat(roiConfigurationDefaultValues.getDeveloperHourlyRateDefault())
        .isEqualTo(BigDecimal.valueOf(100));
    assertThat(roiConfigurationDefaultValues.getDeveloperHourlyRateMinimum())
        .isEqualTo(BigDecimal.valueOf(80));
    assertThat(roiConfigurationDefaultValues.getFixRateHoursDefault()).isEqualTo(12000L);
    assertThat(roiConfigurationDefaultValues.getFixRateHoursMinimum()).isEqualTo(13000L);
    assertThat(roiConfigurationDefaultValues.getSecurityViolationCriticalDefault())
        .isEqualTo(BigDecimal.valueOf(14000));
    assertThat(roiConfigurationDefaultValues.getSecurityViolationCriticalMinimum())
        .isEqualTo(BigDecimal.valueOf(15000));
    assertThat(roiConfigurationDefaultValues.getSecurityViolationHighDefault())
        .isEqualTo(BigDecimal.valueOf(16000));
    assertThat(roiConfigurationDefaultValues.getSecurityViolationHighMinimum())
        .isEqualTo(BigDecimal.valueOf(17000));
    assertThat(roiConfigurationDefaultValues.getSecurityViolationMediumDefault())
        .isEqualTo(BigDecimal.valueOf(18000));
    assertThat(roiConfigurationDefaultValues.getSecurityViolationMediumMinimum())
        .isEqualTo(BigDecimal.valueOf(19000));
    assertThat(roiConfigurationDefaultValues.getSecurityViolationLowEnabled())
        .isEqualTo(BigDecimal.valueOf(20000));
    assertThat(roiConfigurationDefaultValues.getSecurityViolationLowMinimum())
        .isEqualTo(BigDecimal.valueOf(21000));
    assertThat(roiConfigurationDefaultValues.getSupplyChainAttacksBlockedDefault())
        .isEqualTo(BigDecimal.valueOf(22000));
    assertThat(roiConfigurationDefaultValues.getSupplyChainAttacksBlockedMinimum())
        .isEqualTo(BigDecimal.valueOf(23000));
    assertThat(roiConfigurationDefaultValues.getNamespaceAttacksBlockedDefault())
        .isEqualTo(BigDecimal.valueOf(24000));
    assertThat(roiConfigurationDefaultValues.getNamespaceAttacksBlockedMinimum())
        .isEqualTo(BigDecimal.valueOf(25000));
    assertThat(roiConfigurationDefaultValues.getSafeComponentsAutoSelectedDefault())
        .isEqualTo(BigDecimal.valueOf(26000));
    assertThat(roiConfigurationDefaultValues.getSafeComponentsAutoSelectedMinimum())
        .isEqualTo(BigDecimal.valueOf(27000));
    assertThat(roiConfigurationDefaultValues.isWaivedPoliciesCounted()).isFalse();
  }
}
