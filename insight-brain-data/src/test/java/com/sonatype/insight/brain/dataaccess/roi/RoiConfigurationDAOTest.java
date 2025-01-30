/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.roi;

import java.math.BigDecimal;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.roi.CurrencyTypes;
import com.sonatype.insight.brain.model.roi.RoiConfiguration;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RoiConfigurationDAOTest
    extends AbstractDbDAOTest
{
  private RoiConfigurationDAO dao;

  @Before
  @Override
  public  void setup() {
    super.setup();
    dao = daoFactory.createRoiConfigurationDAO();
  }

  @Test
  public void testCRUD() {
    RoiConfiguration roiConfiguration = tempEntity.createRoiConfiguration(
        CurrencyTypes.USD,
        BigDecimal.valueOf(100),
        80L,
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
    assertThat(roiConfiguration.getId()).isNotNull();

    // read
    roiConfiguration = dao.getById(roiConfiguration.getId());
    assertRoiConfigurationEntityValues(roiConfiguration);

    // update
    roiConfiguration.setNamespaceAttacksBlocked(BigDecimal.valueOf(10000));
    dao.update(roiConfiguration);

    roiConfiguration = dao.getById(roiConfiguration.getId());

    assertThat(roiConfiguration).isNotNull();
    assertThat(roiConfiguration.getNamespaceAttacksBlocked()).isEqualTo(BigDecimal.valueOf(10000));

    // delete
    String id = roiConfiguration.getId();
    dao.delete(roiConfiguration);

    assertThat(dao.getById(id)).isNull();
  }

  @Test
  public void testGetByCurrencyType() {
    RoiConfiguration roiConfiguration = tempEntity.createRoiConfiguration(
        CurrencyTypes.USD,
        BigDecimal.valueOf(100),
        80L,
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
    assertThat(dao.getByCurrencyType(CurrencyTypes.USD)).isNotNull();
    assertRoiConfigurationEntityValues(roiConfiguration);
  }

  private void assertRoiConfigurationEntityValues(
      RoiConfiguration roiConfiguration)
  {
    assertThat(roiConfiguration.getCurrency()).isEqualTo(CurrencyTypes.USD);
    assertThat(roiConfiguration.getFixRateHours()).isEqualTo(80L);
    assertThat(roiConfiguration.getDeveloperHourlyRate()).isEqualTo(BigDecimal.valueOf(100));
    assertThat(roiConfiguration.isSecurityViolationCriticalEnabled()).isTrue();
    assertThat(roiConfiguration.getSecurityViolationCriticalValue()).isEqualTo(BigDecimal.valueOf(23000));
    assertThat(roiConfiguration.isSecurityViolationHighEnabled()).isTrue();
    assertThat(roiConfiguration.getSecurityViolationHighValue()).isEqualTo(BigDecimal.valueOf(30000));
    assertThat(roiConfiguration.isSecurityViolationMediumEnabled()).isFalse();
    assertThat(roiConfiguration.getSecurityViolationMediumValue()).isEqualTo(BigDecimal.valueOf(45000));
    assertThat(roiConfiguration.isSecurityViolationLowEnabled()).isFalse();
    assertThat(roiConfiguration.getSecurityViolationLowValue()).isEqualTo(BigDecimal.valueOf(40000));
    assertThat(roiConfiguration.getNamespaceAttacksBlocked()).isEqualTo(BigDecimal.valueOf(60000));
    assertThat(roiConfiguration.getSupplyChainAttacksBlocked()).isEqualTo(BigDecimal.valueOf(50000));
    assertThat(roiConfiguration.getSafeComponentsAutoSelected()).isEqualTo(BigDecimal.valueOf(70000));
    assertThat(roiConfiguration.isWaivedPoliciesCounted()).isFalse();
  }
}
