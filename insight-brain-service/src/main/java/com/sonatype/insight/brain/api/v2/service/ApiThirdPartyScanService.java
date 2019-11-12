/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanResultDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.cyclonedx.CycloneDxSchemaValidator;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;
import com.sonatype.insight.brain.scan.ScanResult;
import com.sonatype.insight.brain.scan.Scanner;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.scan.model.ItemContentType;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * @since 1.76
 */
@Named
@Singleton
public class ApiThirdPartyScanService
{
  private final CycloneDxSchemaValidator schemaValidator;

  private final Scanner scanner;

  private final ProprietaryConfigService proprietaryConfigService;

  private final InsightWork work;

  private final PolicyEvaluateService policyEvaluateService;

  private static final Logger log = LoggerFactory.getLogger(ApiThirdPartyScanService.class);

  @Inject
  public ApiThirdPartyScanService(
      final CycloneDxSchemaValidator schemaValidator,
      final Scanner scanner,
      final ProprietaryConfigService proprietaryConfigService,
      final InsightWork work,
      final PolicyEvaluateService policyEvaluateService)
  {
    this.schemaValidator = schemaValidator;
    this.scanner = scanner;
    this.proprietaryConfigService = proprietaryConfigService;
    this.work = work;
    this.policyEvaluateService = policyEvaluateService;
  }

  @Authorize(permission = Permission.READ)
  public ApiThirdPartyScanTicketDTO scanComponents(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId,
      final String source,
      final String stageId,
      final String sbom)
  {
    Stage stage = new Stage(stageId);
    validateRequest(sbom, stage);
    
    String scanRequestId = UUID.randomUUID().toString().replace("-", "");
    ApiThirdPartyScanTicketDTO scanTicketDTO = createScanTicket(applicationId, scanRequestId);

    log.debug("Received request to scan SBOM for app id {}, source {}, stageTypeId {}. "
        + "The status ID of the operation is {}.", applicationId, source, stage.getStageTypeId(), scanRequestId);
    Application app = new ApplicationDAO().getById(applicationId);
    ScanResult scanResult = createScanFile(sbom, app);

    policyEvaluateService.doEvaluationWithPolling(scanRequestId, app.getPublicId(),
        ClientScanType.SONATYPE_THIRD_PARTY, stage,
        scanResult.getScanFile());

    return scanTicketDTO;
  }

  private void validateRequest(final String sbom, Stage stage) {

    if (!Stage.isValidStageTypeId(stage.getStageTypeId())) {
      throw new BadRequestException("Invalid stage id=" + stage.getStageTypeId());
    }
    if (StringUtils.isBlank(sbom)) {
      throw new BadRequestException("sbom content is null or empty");
    }

    try {
      schemaValidator.validate(sbom);
    }
    catch (SAXException ex) {
      throw new BadRequestException(ex.getMessage());
    }
  }

  private ScanResult createScanFile(final String sbom, final Application app) {
    try {
      ProprietaryConfig proprietaryConfig =
          proprietaryConfigService.getProprietaryConfig(OwnerType.APPLICATION, app.getPublicId());
      return scanner.scanContent(sbom, work.getScanDir(app.getId()), ItemContentType.SBOM, proprietaryConfig);
    }
    catch (IOException ex) {
      log.error("Error processing sbom content", ex);
      throw new UncheckedIOException(ex.getMessage(), ex);
    }
  }

  private ApiThirdPartyScanTicketDTO createScanTicket(final String applicationId, final String scanRequestId) {
    ApiThirdPartyScanTicketDTO scanTicketDTO = new ApiThirdPartyScanTicketDTO();
    scanTicketDTO.statusUrl = PublicApiPaths.THIRD_PARTY_SCAN_PATH + "/" + applicationId + "/status/" + scanRequestId;
    return scanTicketDTO;
  }

  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public ApiThirdPartyScanResultDTO getScanStatus(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId,
      String scanRequestId)
  {
    // TODO: It'll return 404 until is implemented
    throw new NotFoundException(String.format("Report with status id %s for application with id %s was not found.",
        scanRequestId, applicationId));
  }
}
