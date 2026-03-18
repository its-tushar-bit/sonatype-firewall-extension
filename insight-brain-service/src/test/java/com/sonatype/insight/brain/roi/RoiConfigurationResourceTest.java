/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.roi;

import java.math.BigDecimal;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.roi.RoiConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.roi.RoiConfigurationDefaultValuesDAO;
import com.sonatype.insight.brain.model.roi.CurrencyTypes;
import com.sonatype.insight.brain.roi.dtos.RoiConfigurationDTO;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RoiConfigurationResourceTest
    extends AbstractResourceTest
{
  private RoiConfigurationDefaultValuesDAO dao;

  private RoiConfigurationDAO roiConfigurationDAO;

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(RoiConfigurationResource.RESOURCE_PATH);
  }

  @Before
  public void setup() {
    dao = lookup(RoiConfigurationDefaultValuesDAO.class);
    roiConfigurationDAO = lookup(RoiConfigurationDAO.class);
    dao.getAll().forEach(dao::delete);
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
  }

  @Test
  public void testGetCurrentAndMinimumValuesByCurrencyType() throws Exception {
    tempEntity.createRoiConfiguration(
        CurrencyTypes.USD,
        BigDecimal.valueOf(50000),
        BigDecimal.valueOf(60000),
        BigDecimal.valueOf(70000),
        15,
        BigDecimal.valueOf(400));
    HttpResponse response = restRequest()
        .parameter("usd")
        .path(RoiConfigurationResource.ROI_CONFIGURATION_CURRENCY_PATH)
        .get();
    assertResponseStatus(200, response);
    RoiConfigurationCurrentAndMinimumValuesDTO roiConfigurationActual =
        response.getBody(RoiConfigurationCurrentAndMinimumValuesDTO.class);
    assertThat(roiConfigurationActual).isNotNull();
    assertRoiConfigurationEntityValues(roiConfigurationActual);
  }

  @Test
  public void testGetCurrentAndMinimumValuesByCurrencyType_Unlicensed() throws Exception {
    setMissingFeature(LicensedFeature.ROI_CONFIGURATION);
    HttpResponse response = restRequest()
        .path(RoiConfigurationResource.ROI_CONFIGURATION_CURRENCY_PATH)
        .parameter("usd")
        .get();
    assertResponseStatus(402, response);
  }

  @Test
  public void testSaveRoiConfiguration_Unlicensed() throws Exception {
    setMissingFeature(LicensedFeature.ROI_CONFIGURATION);
    RoiConfigurationDTO roiConfigurationDTO = new RoiConfigurationDTO(
        null,
        CurrencyTypes.USD,
        BigDecimal.valueOf(500000),
        BigDecimal.valueOf(600000),
        BigDecimal.valueOf(700000),
        30,
        BigDecimal.valueOf(800));

    HttpResponse response = restRequest()
        .body(roiConfigurationDTO)
        .post();
    assertResponseStatus(402, response);
  }

  @Test
  public void testSaveRoiConfiguration() throws Exception {
    RoiConfigurationDTO roiConfigurationDTO = new RoiConfigurationDTO(
        null,
        CurrencyTypes.USD,
        BigDecimal.valueOf(500000),
        BigDecimal.valueOf(600000),
        BigDecimal.valueOf(700000),
        30,
        BigDecimal.valueOf(800));
    HttpResponse response = restRequest()
        .body(roiConfigurationDTO)
        .post();
    assertResponseStatus(200, response);
    RoiConfigurationCurrentAndMinimumValuesDTO roiConfigurationActual =
        response.getBody(RoiConfigurationCurrentAndMinimumValuesDTO.class);
    assertThat(roiConfigurationActual).isNotNull();
    assertThat(roiConfigurationActual.currency).isEqualTo(CurrencyTypes.USD);
    assertThat(roiConfigurationActual.baselineDaysToResolveViolation).isEqualTo(30L);
    assertThat(roiConfigurationActual.dailyRiskCostOfUnfixedViolation).isEqualTo(BigDecimal.valueOf(800));
    assertThat(roiConfigurationActual.malwareAttacksPrevented).isEqualTo(BigDecimal.valueOf(500000));
    assertThat(roiConfigurationActual.namespaceAttacksPrevented).isEqualTo(BigDecimal.valueOf(600000));
    assertThat(roiConfigurationActual.safeComponentsAutoSelected).isEqualTo(BigDecimal.valueOf(700000));
  }

  @Test
  public void testSaveRoiConfiguration_Update() throws Exception {
    tempEntity.createRoiConfiguration(
        CurrencyTypes.USD,
        BigDecimal.valueOf(50000),
        BigDecimal.valueOf(60000),
        BigDecimal.valueOf(70000),
        15,
        BigDecimal.valueOf(400));
    roiConfigurationDAO.getByCurrencyType(CurrencyTypes.USD);
    RoiConfigurationDTO roiConfigurationDTO = new RoiConfigurationDTO(
        null,
        CurrencyTypes.USD,
        BigDecimal.valueOf(550000),
        BigDecimal.valueOf(600000),
        BigDecimal.valueOf(700000),
        30,
        BigDecimal.valueOf(800));
    HttpResponse response = restRequest()
        .body(roiConfigurationDTO)
        .post();
    assertResponseStatus(200, response);
    RoiConfigurationCurrentAndMinimumValuesDTO roiConfigurationActual =
        response.getBody(RoiConfigurationCurrentAndMinimumValuesDTO.class);
    assertThat(roiConfigurationActual).isNotNull();
    assertThat(roiConfigurationActual.currency).isEqualTo(CurrencyTypes.USD);
    assertThat(roiConfigurationActual.malwareAttacksPrevented).isEqualTo(BigDecimal.valueOf(550000));
    assertThat(roiConfigurationActual.dailyRiskCostOfUnfixedViolation).isEqualTo(BigDecimal.valueOf(800));
  }

  private void assertRoiConfigurationEntityValues(
      RoiConfigurationCurrentAndMinimumValuesDTO roiConfiguration)
  {
    assertThat(roiConfiguration.currency).isEqualTo(CurrencyTypes.USD);
    assertThat(roiConfiguration.namespaceAttacksPrevented).isEqualTo(BigDecimal.valueOf(60000));
    assertThat(roiConfiguration.namespaceAttacksPreventedMinimum).isEqualTo(BigDecimal.valueOf(10000));
    assertThat(roiConfiguration.malwareAttacksPrevented).isEqualTo(BigDecimal.valueOf(50000));
    assertThat(roiConfiguration.malwareAttacksPreventedMinimum).isEqualTo(BigDecimal.valueOf(500000));
    assertThat(roiConfiguration.safeComponentsAutoSelected).isEqualTo(BigDecimal.valueOf(70000));
    assertThat(roiConfiguration.safeComponentsAutoSelectedMinimum).isEqualTo(BigDecimal.valueOf(5000));
    assertThat(roiConfiguration.baselineDaysToResolveViolation).isEqualTo(15L);
    assertThat(roiConfiguration.baselineDaysToResolveViolationMinimum).isEqualTo(15L);
    assertThat(roiConfiguration.dailyRiskCostOfUnfixedViolation).isEqualTo(BigDecimal.valueOf(400));
    assertThat(roiConfiguration.dailyRiskCostOfUnfixedViolationMinimum).isEqualTo(BigDecimal.valueOf(400));
  }

  @Test
  public void testRestoreToDefaultValuesByCurrencyType_Unlicensed() throws Exception {
    setMissingFeature(LicensedFeature.ROI_CONFIGURATION);
    HttpResponse response = restRequest()
        .parameter("usd")
        .path(RoiConfigurationResource.ROI_CONFIGURATION_DEFAULT_VALUES_PATH)
        .post();
    assertResponseStatus(402, response);
  }

  @Test
  public void testRestoreToDefaultValuesByCurrencyType() throws Exception {
    HttpResponse response = restRequest()
        .parameter("usd")
        .path(RoiConfigurationResource.ROI_CONFIGURATION_DEFAULT_VALUES_PATH)
        .post();
    assertResponseStatus(200, response);
    RoiConfigurationCurrentAndMinimumValuesDTO roiConfigurationActual =
        response.getBody(RoiConfigurationCurrentAndMinimumValuesDTO.class);
    assertThat(roiConfigurationActual).isNotNull();
    assertThat(roiConfigurationActual.currency).isEqualTo(CurrencyTypes.USD);
    assertThat(roiConfigurationActual.malwareAttacksPrevented).isEqualTo(BigDecimal.valueOf(4350000));
    assertThat(roiConfigurationActual.namespaceAttacksPrevented).isEqualTo(BigDecimal.valueOf(35000));
    assertThat(roiConfigurationActual.safeComponentsAutoSelected).isEqualTo(BigDecimal.valueOf(25000));
    assertThat(roiConfigurationActual.baselineDaysToResolveViolation).isEqualTo(30L);
    assertThat(roiConfigurationActual.dailyRiskCostOfUnfixedViolation).isEqualTo(BigDecimal.valueOf(800));
  }
}
