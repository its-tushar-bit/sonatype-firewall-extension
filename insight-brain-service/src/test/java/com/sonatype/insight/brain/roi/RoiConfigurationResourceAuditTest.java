/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.roi;

import java.math.BigDecimal;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.roi.RoiConfigurationDefaultValuesDAO;
import com.sonatype.insight.brain.model.roi.CurrencyTypes;
import com.sonatype.insight.brain.roi.dtos.RoiConfigurationDTO;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Before;
import org.junit.Test;

public class RoiConfigurationResourceAuditTest
    extends AbstractAuditTest
{
  private RoiConfigurationDefaultValuesDAO dao;

  @Before
  public void setUp() throws Exception {
    setFeatures(LicensedFeature.ROI_CONFIGURATION);
    dao = lookup(RoiConfigurationDefaultValuesDAO.class);
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

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(RoiConfigurationResource.RESOURCE_PATH);
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
    HttpResponse response =
        restRequest().body(roiConfigurationDTO).post();
    assertResponseStatus(200, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.ROI_CONFIG_CREATE, null);
    RoiConfigurationCurrentAndMinimumValuesDTO roiConfigurationActual =
        response.getBody(RoiConfigurationCurrentAndMinimumValuesDTO.class);
    assertCustomData(auditDTO, roiConfigurationActual);
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

    RoiConfigurationDTO roiConfigurationDTO = new RoiConfigurationDTO(
        null,
        CurrencyTypes.USD,
        BigDecimal.valueOf(500000),
        BigDecimal.valueOf(600000),
        BigDecimal.valueOf(700000),
        30,
        BigDecimal.valueOf(800));
    HttpResponse response =
        restRequest().body(roiConfigurationDTO).post();
    assertResponseStatus(200, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.ROI_CONFIG_UPDATE, null);
    RoiConfigurationCurrentAndMinimumValuesDTO roiConfigurationActual =
        response.getBody(RoiConfigurationCurrentAndMinimumValuesDTO.class);
    assertCustomData(auditDTO, roiConfigurationActual);
  }

  @Test
  public void testSaveRoiConfiguration_unlicensed() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.ROI_CONFIGURATION);
    RoiConfigurationDTO roiConfigurationDTO = new RoiConfigurationDTO(
        null,
        CurrencyTypes.USD,
        BigDecimal.valueOf(500000),
        BigDecimal.valueOf(600000),
        BigDecimal.valueOf(700000),
        30,
        BigDecimal.valueOf(800));
    HttpResponse response =
        restRequest().body(roiConfigurationDTO).post();
    assertResponseStatus(402, response);
    assertAuditLog(AuditEvent.ROI_CONFIG_CREATE, "unlicensed");
  }

  @Test
  public void testSaveRoiConfiguration_badRequest() throws Exception {
    RoiConfigurationDTO roiConfigurationDTO = new RoiConfigurationDTO(
        null,
        CurrencyTypes.USD,
        BigDecimal.valueOf(500000),
        BigDecimal.valueOf(600000),
        BigDecimal.valueOf(700000),
        10,
        BigDecimal.valueOf(800));
    HttpResponse response =
        restRequest().body(roiConfigurationDTO).post();
    assertResponseStatus(400, response);
    assertAuditLog(AuditEvent.ROI_CONFIG_CREATE, "bad-request");
  }

  private void assertCustomData(
      final AuditDTO auditDTO,
      RoiConfigurationCurrentAndMinimumValuesDTO roiConfigurationDTO)
  {
    assertCustomData(auditDTO, "currency", roiConfigurationDTO.currency.toString());
    assertCustomData(auditDTO, "malwareAttacksPrevented", roiConfigurationDTO.malwareAttacksPrevented.toString());
    assertCustomData(auditDTO, "namespaceAttacksPrevented", roiConfigurationDTO.namespaceAttacksPrevented.toString());
    assertCustomData(auditDTO, "safeComponentsAutoSelected",
        roiConfigurationDTO.safeComponentsAutoSelected.toString());
    assertCustomData(auditDTO, "baselineDaysToResolveViolation",
        roiConfigurationDTO.baselineDaysToResolveViolation.toString());
    assertCustomData(auditDTO, "dailyRiskCostOfUnfixedViolation",
        roiConfigurationDTO.dailyRiskCostOfUnfixedViolation.toString());
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
    AuditDTO auditDTO = assertAuditLog(AuditEvent.ROI_CONFIG_UPDATE, null);
    assertCustomData(auditDTO, roiConfigurationActual);
  }

  @Test
  public void testRestoreToDefaultValuesByCurrencyType_unlicensed() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.ROI_CONFIGURATION);
    HttpResponse response = restRequest()
        .parameter("usd")
        .path(RoiConfigurationResource.ROI_CONFIGURATION_DEFAULT_VALUES_PATH)
        .post();
    assertResponseStatus(402, response);
    assertAuditLog(AuditEvent.ROI_CONFIG_UPDATE, "unlicensed");
  }
}
