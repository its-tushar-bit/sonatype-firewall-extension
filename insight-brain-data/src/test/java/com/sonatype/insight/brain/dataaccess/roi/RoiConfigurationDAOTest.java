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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RoiConfigurationDAOTest
    extends AbstractDbDAOTest
{
  private RoiConfigurationDAO dao;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createRoiConfigurationDAO();
  }

  @Test
  public void testCRUD() {
    RoiConfiguration roiConfiguration = tempEntity.createRoiConfiguration(
        CurrencyTypes.USD,
        BigDecimal.valueOf(50000),
        BigDecimal.valueOf(60000),
        BigDecimal.valueOf(70000),
        15,
        BigDecimal.valueOf(400));
    assertThat(roiConfiguration.getId()).isNotNull();

    // read
    roiConfiguration = dao.getById(roiConfiguration.getId());
    assertRoiConfigurationEntityValues(roiConfiguration);

    // update
    roiConfiguration.setNamespaceAttacksPrevented(BigDecimal.valueOf(10000));
    dao.update(roiConfiguration);

    roiConfiguration = dao.getById(roiConfiguration.getId());

    assertThat(roiConfiguration).isNotNull();
    assertThat(roiConfiguration.getNamespaceAttacksPrevented()).isEqualTo(BigDecimal.valueOf(10000));

    // delete
    String id = roiConfiguration.getId();
    dao.delete(roiConfiguration);

    assertThat(dao.getById(id)).isNull();
  }

  @Test
  public void testGetByCurrencyType() {
    RoiConfiguration roiConfiguration = tempEntity.createRoiConfiguration(
        CurrencyTypes.USD,
        BigDecimal.valueOf(50000),
        BigDecimal.valueOf(60000),
        BigDecimal.valueOf(70000),
        15,
        BigDecimal.valueOf(400));
    assertThat(dao.getByCurrencyType(CurrencyTypes.USD)).isNotNull();
    assertRoiConfigurationEntityValues(roiConfiguration);
  }

  private void assertRoiConfigurationEntityValues(
      RoiConfiguration roiConfiguration)
  {
    assertThat(roiConfiguration.getCurrency()).isEqualTo(CurrencyTypes.USD);
    assertThat(roiConfiguration.getNamespaceAttacksPrevented()).isEqualTo(BigDecimal.valueOf(60000));
    assertThat(roiConfiguration.getMalwareAttacksPrevented()).isEqualTo(BigDecimal.valueOf(50000));
    assertThat(roiConfiguration.getSafeComponentsAutoSelected()).isEqualTo(BigDecimal.valueOf(70000));
    assertThat(roiConfiguration.getBaselineDaysToResolveViolation()).isEqualTo(15);
    assertThat(roiConfiguration.getDailyRiskCostOfUnfixedViolation()).isEqualTo(BigDecimal.valueOf(400));
  }
}
