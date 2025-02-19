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
import com.sonatype.insight.brain.model.roi.RoiConfiguration;
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
  }

  @Test
  public void testGetCurrentAndMinimumValuesByCurrencyType() throws Exception {
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
        BigDecimal.valueOf(100),
        1448L,
        true,
        BigDecimal.valueOf(23000),
        true,
        BigDecimal.valueOf(30000),
        false,
        BigDecimal.valueOf(45000),
        false,
        BigDecimal.valueOf(80000),
        BigDecimal.valueOf(500000),
        BigDecimal.valueOf(600000),
        BigDecimal.valueOf(700000),
        false
    );

    HttpResponse response = restRequest()
        .body(roiConfigurationDTO).post();
    assertResponseStatus(402, response);
  }

  @Test
  public void testSaveRoiConfiguration() throws Exception {
    RoiConfigurationDTO roiConfigurationDTO = new RoiConfigurationDTO(
        null,
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
        BigDecimal.valueOf(80000),
        BigDecimal.valueOf(500000),
        BigDecimal.valueOf(600000),
        BigDecimal.valueOf(700000),
        false
    );
    HttpResponse response = restRequest()
        .body(roiConfigurationDTO).post();
    assertResponseStatus(200, response);
    RoiConfigurationDTO roiConfigurationActual = response.getBody(RoiConfigurationDTO.class);
    assertThat(roiConfigurationActual).isNotNull();
    assertThat(roiConfigurationActual.currency()).isEqualTo(CurrencyTypes.USD);
    assertThat(roiConfigurationActual.developerHourlyRate()).isEqualTo(BigDecimal.valueOf(100));
    assertThat(roiConfigurationActual.fixRateHours()).isEqualTo(1448L);
    assertThat(roiConfigurationActual.securityViolationCriticalEnabled()).isTrue();
    assertThat(roiConfigurationActual.securityViolationCriticalValue()).isEqualTo(BigDecimal.valueOf(23000));
    assertThat(roiConfigurationActual.securityViolationHighEnabled()).isTrue();
    assertThat(roiConfigurationActual.securityViolationHighValue()).isEqualTo(BigDecimal.valueOf(30000));
    assertThat(roiConfigurationActual.securityViolationMediumEnabled()).isFalse();
    assertThat(roiConfigurationActual.securityViolationMediumValue()).isEqualTo(BigDecimal.valueOf(45000));
    assertThat(roiConfigurationActual.securityViolationLowEnabled()).isFalse();
  }

  @Test
  public void testSaveRoiConfiguration_Update() throws Exception {
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
    RoiConfiguration roiConfiguration = roiConfigurationDAO.getByCurrencyType(CurrencyTypes.USD);
    RoiConfigurationDTO roiConfigurationDTO = new RoiConfigurationDTO(
        null,
        CurrencyTypes.USD,
        BigDecimal.valueOf(1000),
        1448L,
        true,
        BigDecimal.valueOf(30000),
        true,
        BigDecimal.valueOf(50000),
        false,
        BigDecimal.valueOf(45000),
        false,
        BigDecimal.valueOf(80000),
        BigDecimal.valueOf(500000),
        BigDecimal.valueOf(600000),
        BigDecimal.valueOf(700000),
        false
    );
    HttpResponse response = restRequest()
        .body(roiConfigurationDTO).post();
    assertResponseStatus(200, response);
    RoiConfigurationDTO roiConfigurationActual = response.getBody(RoiConfigurationDTO.class);
    assertThat(roiConfigurationActual).isNotNull();
    assertThat(roiConfigurationActual.id()).isEqualTo(roiConfiguration.getId());
    assertThat(roiConfigurationActual.currency()).isEqualTo(CurrencyTypes.USD);
    assertThat(roiConfigurationActual.developerHourlyRate()).isEqualTo(BigDecimal.valueOf(1000));
    assertThat(roiConfigurationActual.securityViolationCriticalValue()).isEqualTo(BigDecimal.valueOf(30000));
    assertThat(roiConfigurationActual.securityViolationHighValue()).isEqualTo(BigDecimal.valueOf(50000));
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
    RoiConfigurationDTO roiConfigurationActual = response.getBody(RoiConfigurationDTO.class);
    assertThat(roiConfigurationActual).isNotNull();
    assertThat(roiConfigurationActual.currency()).isEqualTo(CurrencyTypes.USD);
    assertThat(roiConfigurationActual.developerHourlyRate()).isEqualTo(BigDecimal.valueOf(100));
    assertThat(roiConfigurationActual.fixRateHours()).isEqualTo(3600L);
    assertThat(roiConfigurationActual.securityViolationCriticalEnabled()).isTrue();
    assertThat(roiConfigurationActual.securityViolationCriticalValue()).isEqualTo(BigDecimal.valueOf(12000));
    assertThat(roiConfigurationActual.securityViolationHighEnabled()).isTrue();
    assertThat(roiConfigurationActual.securityViolationHighValue()).isEqualTo(BigDecimal.valueOf(24000));
    assertThat(roiConfigurationActual.securityViolationMediumEnabled()).isFalse();
    assertThat(roiConfigurationActual.securityViolationMediumValue()).isEqualTo(BigDecimal.valueOf(72000));
    assertThat(roiConfigurationActual.securityViolationLowEnabled()).isFalse();
  }
}
