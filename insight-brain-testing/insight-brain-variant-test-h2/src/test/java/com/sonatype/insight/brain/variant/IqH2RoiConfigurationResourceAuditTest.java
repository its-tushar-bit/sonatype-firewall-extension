/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.math.BigDecimal;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.roi.RoiConfigurationDefaultValuesDAO;
import com.sonatype.insight.brain.model.roi.CurrencyTypes;
import com.sonatype.insight.brain.roi.RoiConfigurationCurrentAndMinimumValuesDTO;
import com.sonatype.insight.brain.roi.RoiConfigurationResource;
import com.sonatype.insight.brain.roi.dtos.RoiConfigurationDTO;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Converted from the legacy {@code RoiConfigurationResourceAuditTest}.
 */
@IqH2Test
class IqH2RoiConfigurationResourceAuditTest
    implements AuditTestSupport
{
  // Injected by IqH2ServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private RoiConfigurationDefaultValuesDAO dao;

  private com.sonatype.insight.brain.model.security.User unauthorizedUser;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void setUp() throws Exception {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();

    ctx.setFeatures(LicensedFeature.ROI_CONFIGURATION);
    dao = ctx.lookup(RoiConfigurationDefaultValuesDAO.class);
    dao.getAll().forEach(dao::delete);
    ctx.tempEntity()
        .createRoiConfigurationDefaultValues(
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

  @AfterEach
  void after() {
    logOutput.tearDown();
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public String getUnauthorizedUsername() {
    return unauthorizedUser.getUsername();
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private java.util.function.Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(RoiConfigurationResource.RESOURCE_PATH);
  }

  @Test
  void testSaveRoiConfiguration() throws Exception {
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
    ctx.assertResponseStatus(200, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.ROI_CONFIG_CREATE, null);
    RoiConfigurationCurrentAndMinimumValuesDTO roiConfigurationActual =
        response.getBody(RoiConfigurationCurrentAndMinimumValuesDTO.class);
    assertCustomData(auditDTO, roiConfigurationActual);
  }

  @Test
  void testSaveRoiConfiguration_Update() throws Exception {
    ctx.tempEntity()
        .createRoiConfiguration(
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
    ctx.assertResponseStatus(200, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.ROI_CONFIG_UPDATE, null);
    RoiConfigurationCurrentAndMinimumValuesDTO roiConfigurationActual =
        response.getBody(RoiConfigurationCurrentAndMinimumValuesDTO.class);
    assertCustomData(auditDTO, roiConfigurationActual);
  }

  @Test
  void testSaveRoiConfiguration_unlicensed() throws Exception {
    ctx.setMissingFeature(LicensedFeature.ROI_CONFIGURATION);
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
    ctx.assertResponseStatus(402, response);
    assertAuditLog(AuditEvent.ROI_CONFIG_CREATE, "unlicensed");
  }

  @Test
  void testSaveRoiConfiguration_badRequest() throws Exception {
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
    ctx.assertResponseStatus(400, response);
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
  void testRestoreToDefaultValuesByCurrencyType() throws Exception {
    HttpResponse response = restRequest()
        .parameter("usd")
        .path(RoiConfigurationResource.ROI_CONFIGURATION_DEFAULT_VALUES_PATH)
        .post();
    ctx.assertResponseStatus(200, response);
    RoiConfigurationCurrentAndMinimumValuesDTO roiConfigurationActual =
        response.getBody(RoiConfigurationCurrentAndMinimumValuesDTO.class);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.ROI_CONFIG_UPDATE, null);
    assertCustomData(auditDTO, roiConfigurationActual);
  }

  @Test
  void testRestoreToDefaultValuesByCurrencyType_unlicensed() throws Exception {
    ctx.setMissingFeature(LicensedFeature.ROI_CONFIGURATION);
    HttpResponse response = restRequest()
        .parameter("usd")
        .path(RoiConfigurationResource.ROI_CONFIGURATION_DEFAULT_VALUES_PATH)
        .post();
    ctx.assertResponseStatus(402, response);
    assertAuditLog(AuditEvent.ROI_CONFIG_UPDATE, "unlicensed");
  }

  private static final class TestLogOutput
      extends LogOutput
  {
    TestLogOutput(String... loggerNames) {
      super(loggerNames);
    }

    void tearDown() {
      after();
    }
  }
}
