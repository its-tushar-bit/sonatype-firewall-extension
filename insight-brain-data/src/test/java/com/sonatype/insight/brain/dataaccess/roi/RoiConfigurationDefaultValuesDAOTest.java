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
    dao.getAll().forEach(dao::delete);
  }

  @Test
  public void testCRUD() {
    RoiConfigurationDefaultValues roiConfigurationDefaultValues = tempEntity.createRoiConfigurationDefaultValues(
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

    // read
    RoiConfigurationDefaultValues roiConfigurationDefaultValuesExisting =
        dao.getById(roiConfigurationDefaultValues.getId());
    assertRoiConfigurationDefaultValuesEntityValues(roiConfigurationDefaultValuesExisting);

    // update
    roiConfigurationDefaultValuesExisting.setNamespaceAttacksBlockedDefault(BigDecimal.valueOf(10000));
    dao.update(roiConfigurationDefaultValuesExisting);
    roiConfigurationDefaultValuesExisting = dao.getById(roiConfigurationDefaultValuesExisting.getId());

    assertThat(roiConfigurationDefaultValuesExisting).isNotNull();
    assertThat(roiConfigurationDefaultValuesExisting.getNamespaceAttacksBlockedDefault()).isEqualTo(
        BigDecimal.valueOf(10000));

    //delete
    dao.delete(roiConfigurationDefaultValuesExisting);
    assertThat(dao.getById(roiConfigurationDefaultValuesExisting.getId())).isNull();
  }

  @Test
  public void testGetByCurrencyType() {
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
    RoiConfigurationDefaultValues roiConfigurationDefaultValues = dao.getByCurrencyType(CurrencyTypes.USD);
    assertThat(roiConfigurationDefaultValues).isNotNull();
    assertRoiConfigurationDefaultValuesEntityValues(roiConfigurationDefaultValues);
  }

  private void assertRoiConfigurationDefaultValuesEntityValues(
      RoiConfigurationDefaultValues roiConfigurationDefaultValues)
  {
    assertThat(roiConfigurationDefaultValues.getCurrency()).isEqualTo(CurrencyTypes.USD);
    assertThat(roiConfigurationDefaultValues.getDeveloperHourlyRateDefault())
        .isEqualTo(BigDecimal.valueOf(100));
    assertThat(roiConfigurationDefaultValues.getDeveloperHourlyRateMinimum())
        .isEqualTo(BigDecimal.valueOf(50));
    assertThat(roiConfigurationDefaultValues.getFixRateHoursDefault()).isEqualTo(3600L);
    assertThat(roiConfigurationDefaultValues.getFixRateHoursMinimum()).isEqualTo(1440L);
    assertThat(roiConfigurationDefaultValues.getSecurityViolationCriticalDefault())
        .isEqualTo(BigDecimal.valueOf(12000));
    assertThat(roiConfigurationDefaultValues.getSecurityViolationCriticalMinimum())
        .isEqualTo(BigDecimal.valueOf(6000));
    assertThat(roiConfigurationDefaultValues.isSecurityViolationCriticalEnabled()).isTrue();
    assertThat(roiConfigurationDefaultValues.getSecurityViolationHighDefault())
        .isEqualTo(BigDecimal.valueOf(24000));
    assertThat(roiConfigurationDefaultValues.getSecurityViolationHighMinimum())
        .isEqualTo(BigDecimal.valueOf(12000));
    assertThat(roiConfigurationDefaultValues.isSecurityViolationHighEnabled()).isTrue();
    assertThat(roiConfigurationDefaultValues.getSecurityViolationMediumDefault())
        .isEqualTo(BigDecimal.valueOf(72000));
    assertThat(roiConfigurationDefaultValues.getSecurityViolationMediumMinimum())
        .isEqualTo(BigDecimal.valueOf(36000));
    assertThat(roiConfigurationDefaultValues.isSecurityViolationMediumEnabled()).isFalse();
    assertThat(roiConfigurationDefaultValues.getSecurityViolationLowDefault())
        .isEqualTo(BigDecimal.valueOf(144000));
    assertThat(roiConfigurationDefaultValues.getSecurityViolationLowMinimum())
        .isEqualTo(BigDecimal.valueOf(72000));
    assertThat(roiConfigurationDefaultValues.isSecurityViolationLowEnabled()).isFalse();
    assertThat(roiConfigurationDefaultValues.getSupplyChainAttacksBlockedDefault())
        .isEqualTo(BigDecimal.valueOf(4350000));
    assertThat(roiConfigurationDefaultValues.getSupplyChainAttacksBlockedMinimum())
        .isEqualTo(BigDecimal.valueOf(500000));
    assertThat(roiConfigurationDefaultValues.getNamespaceAttacksBlockedDefault())
        .isEqualTo(BigDecimal.valueOf(35000));
    assertThat(roiConfigurationDefaultValues.getNamespaceAttacksBlockedMinimum())
        .isEqualTo(BigDecimal.valueOf(10000));
    assertThat(roiConfigurationDefaultValues.getSafeComponentsAutoSelectedDefault())
        .isEqualTo(BigDecimal.valueOf(25000));
    assertThat(roiConfigurationDefaultValues.getSafeComponentsAutoSelectedMinimum())
        .isEqualTo(BigDecimal.valueOf(5000));
    assertThat(roiConfigurationDefaultValues.isWaivedPoliciesCounted()).isFalse();
  }
}
