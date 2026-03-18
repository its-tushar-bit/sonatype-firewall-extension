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
  public void setup() {
    super.setup();
    dao = daoFactory.createRoiConfigurationDefaultValuesDAO();
    dao.getAll().forEach(dao::delete);
  }

  @Test
  public void testCRUD() {
    RoiConfigurationDefaultValues roiConfigurationDefaultValues = tempEntity.createRoiConfigurationDefaultValues(
        CurrencyTypes.USD,
        BigDecimal.valueOf(4350000),
        BigDecimal.valueOf(500000),
        BigDecimal.valueOf(35000),
        BigDecimal.valueOf(10000),
        BigDecimal.valueOf(25000),
        BigDecimal.valueOf(5000),
        30,
        15,
        BigDecimal.valueOf(800),
        BigDecimal.valueOf(400));

    // read
    RoiConfigurationDefaultValues roiConfigurationDefaultValuesExisting =
        dao.getById(roiConfigurationDefaultValues.getId());
    assertRoiConfigurationDefaultValuesEntityValues(roiConfigurationDefaultValuesExisting);

    // update
    roiConfigurationDefaultValuesExisting.setNamespaceAttacksPreventedDefault(BigDecimal.valueOf(10000));
    dao.update(roiConfigurationDefaultValuesExisting);
    roiConfigurationDefaultValuesExisting = dao.getById(roiConfigurationDefaultValuesExisting.getId());

    assertThat(roiConfigurationDefaultValuesExisting).isNotNull();
    assertThat(roiConfigurationDefaultValuesExisting.getNamespaceAttacksPreventedDefault()).isEqualTo(
        BigDecimal.valueOf(10000));

    // delete
    dao.delete(roiConfigurationDefaultValuesExisting);
    assertThat(dao.getById(roiConfigurationDefaultValuesExisting.getId())).isNull();
  }

  @Test
  public void testGetByCurrencyType() {
    tempEntity.createRoiConfigurationDefaultValues(
        CurrencyTypes.USD,
        BigDecimal.valueOf(4350000),
        BigDecimal.valueOf(500000),
        BigDecimal.valueOf(35000),
        BigDecimal.valueOf(10000),
        BigDecimal.valueOf(25000),
        BigDecimal.valueOf(5000),
        30,
        15,
        BigDecimal.valueOf(800),
        BigDecimal.valueOf(400));
    RoiConfigurationDefaultValues roiConfigurationDefaultValues = dao.getByCurrencyType(CurrencyTypes.USD);
    assertThat(roiConfigurationDefaultValues).isNotNull();
    assertRoiConfigurationDefaultValuesEntityValues(roiConfigurationDefaultValues);
  }

  private void assertRoiConfigurationDefaultValuesEntityValues(
      RoiConfigurationDefaultValues roiConfigurationDefaultValues)
  {
    assertThat(roiConfigurationDefaultValues.getCurrency()).isEqualTo(CurrencyTypes.USD);
    assertThat(roiConfigurationDefaultValues.getMalwareAttacksPreventedDefault())
        .isEqualTo(BigDecimal.valueOf(4350000));
    assertThat(roiConfigurationDefaultValues.getMalwareAttacksPreventedMinimum())
        .isEqualTo(BigDecimal.valueOf(500000));
    assertThat(roiConfigurationDefaultValues.getNamespaceAttacksPreventedDefault())
        .isEqualTo(BigDecimal.valueOf(35000));
    assertThat(roiConfigurationDefaultValues.getNamespaceAttacksPreventedMinimum())
        .isEqualTo(BigDecimal.valueOf(10000));
    assertThat(roiConfigurationDefaultValues.getSafeComponentsAutoSelectedDefault())
        .isEqualTo(BigDecimal.valueOf(25000));
    assertThat(roiConfigurationDefaultValues.getSafeComponentsAutoSelectedMinimum())
        .isEqualTo(BigDecimal.valueOf(5000));
    assertThat(roiConfigurationDefaultValues.getBaselineDaysToResolveViolationDefault()).isEqualTo(30L);
    assertThat(roiConfigurationDefaultValues.getBaselineDaysToResolveViolationMinimum()).isEqualTo(15L);
    assertThat(roiConfigurationDefaultValues.getDailyRiskCostOfUnfixedViolationDefault()).isEqualTo(
        BigDecimal.valueOf(800));
    assertThat(roiConfigurationDefaultValues.getDailyRiskCostOfUnfixedViolationMinimum()).isEqualTo(
        BigDecimal.valueOf(400));
  }
}
