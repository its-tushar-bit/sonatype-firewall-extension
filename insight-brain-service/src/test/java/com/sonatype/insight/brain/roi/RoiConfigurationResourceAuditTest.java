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

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(RoiConfigurationResource.RESOURCE_PATH);
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
    HttpResponse response =
        restRequest().body(roiConfigurationDTO).post();
    assertResponseStatus(200, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.ROI_CONFIG_CREATE, null);
    assertCustomData(auditDTO, roiConfigurationDTO);
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

    RoiConfigurationDTO roiConfigurationDTO = new RoiConfigurationDTO(
        null,
        CurrencyTypes.USD,
        BigDecimal.valueOf(150),
        1448L,
        true,
        BigDecimal.valueOf(30000),
        true,
        BigDecimal.valueOf(40000),
        false,
        BigDecimal.valueOf(50000),
        false,
        BigDecimal.valueOf(80000),
        BigDecimal.valueOf(500000),
        BigDecimal.valueOf(600000),
        BigDecimal.valueOf(700000),
        false
    );
    HttpResponse response =
        restRequest().body(roiConfigurationDTO).post();
    assertResponseStatus(200, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.ROI_CONFIG_UPDATE, null);
    assertCustomData(auditDTO, roiConfigurationDTO);
  }

  @Test
  public void testSaveRoiConfiguration_unlicensed() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.ROI_CONFIGURATION);
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
        BigDecimal.valueOf(50),
        1448L,
        true,
        BigDecimal.valueOf(5000),
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
    HttpResponse response =
        restRequest().body(roiConfigurationDTO).post();
    assertResponseStatus(400, response);
    assertAuditLog(AuditEvent.ROI_CONFIG_CREATE, "bad-request");
  }

  private void assertCustomData(final AuditDTO auditDTO, RoiConfigurationDTO roiConfigurationDTO) {
    assertCustomData(auditDTO, "currency", roiConfigurationDTO.currency().toString());
    assertCustomData(auditDTO, "developerHourlyRate", roiConfigurationDTO.developerHourlyRate().toString());
    assertCustomData(auditDTO, "fixRateHours", roiConfigurationDTO.fixRateHours().toString());
    assertCustomData(auditDTO, "securityViolationCriticalEnabled",
        roiConfigurationDTO.securityViolationCriticalEnabled());
    assertCustomData(auditDTO, "securityViolationCriticalValue",
        roiConfigurationDTO.securityViolationCriticalValue().toString());
    assertCustomData(auditDTO, "securityViolationHighEnabled", roiConfigurationDTO.securityViolationHighEnabled());
    assertCustomData(auditDTO, "securityViolationHighValue",
        roiConfigurationDTO.securityViolationHighValue().toString());
    assertCustomData(auditDTO, "securityViolationMediumEnabled", roiConfigurationDTO.securityViolationMediumEnabled());
    assertCustomData(auditDTO, "securityViolationMediumValue",
        roiConfigurationDTO.securityViolationMediumValue().toString());
    assertCustomData(auditDTO, "securityViolationLowEnabled", roiConfigurationDTO.securityViolationLowEnabled());
    assertCustomData(auditDTO, "securityViolationLowValue", roiConfigurationDTO.securityViolationLowValue().toString());
    assertCustomData(auditDTO, "supplyChainAttacksBlocked", roiConfigurationDTO.supplyChainAttacksBlocked().toString());
    assertCustomData(auditDTO, "namespaceAttacksBlocked", roiConfigurationDTO.namespaceAttacksBlocked().toString());
    assertCustomData(auditDTO, "safeComponentsAutoSelected",
        roiConfigurationDTO.safeComponentsAutoSelected().toString());
    assertCustomData(auditDTO, "waivedPoliciesCounted", roiConfigurationDTO.waivedPoliciesCounted());
  }
}
