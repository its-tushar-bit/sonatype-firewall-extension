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
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyFact;
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
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.scan.model.ItemContentType;

import com.google.common.annotations.VisibleForTesting;
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
  private static final Logger log = LoggerFactory.getLogger(ApiThirdPartyScanService.class);

  private final CycloneDxSchemaValidator schemaValidator;

  private final Scanner scanner;

  private final ProprietaryConfigService proprietaryConfigService;

  private final BaseUrl baseUrl;

  private final InsightWork work;

  private final PolicyEvaluateService policyEvaluateService;

  private final ApplicationDAO applicationDAO;

  @Inject
  public ApiThirdPartyScanService(
      final CycloneDxSchemaValidator schemaValidator,
      final Scanner scanner,
      final ProprietaryConfigService proprietaryConfigService,
      final BaseUrl baseUrl,
      final InsightWork work,
      final PolicyEvaluateService policyEvaluateService,
      final ApplicationDAO applicationDAO)
  {
    this.schemaValidator = schemaValidator;
    this.scanner = scanner;
    this.proprietaryConfigService = proprietaryConfigService;
    this.baseUrl = baseUrl;
    this.work = work;
    this.policyEvaluateService = policyEvaluateService;
    this.applicationDAO = applicationDAO;
  }

  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public ApiThirdPartyScanTicketDTO scanComponents(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId,
      final String source,
      final String stageId,
      final String sbom,
      final String userAgent)
  {
    Stage stage = new Stage(stageId);
    validateRequest(sbom, stage);
    
    String scanRequestId = UUID.randomUUID().toString().replace("-", "");
    ApiThirdPartyScanTicketDTO scanTicketDTO = createScanTicket(applicationId, scanRequestId);

    log.debug("Received request to scan SBOM for app id {}, source {}, stageTypeId {}. "
        + "The status ID of the operation is {}.", applicationId, source, stage.getStageTypeId(), scanRequestId);
    Application app = new ApplicationDAO().getById(applicationId);
    ScanResult scanResult = createScanFile(app, sbom, source);

    policyEvaluateService.doEvaluationWithPolling(scanRequestId, app.getPublicId(),
        ClientScanType.SONATYPE_THIRD_PARTY, stage,
        scanResult.getScanFile(), "api", userAgent);

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

  private ScanResult createScanFile(final Application app, final String sbom, final String source) {
    try {
      ProprietaryConfig proprietaryConfig =
          proprietaryConfigService.getProprietaryConfig(OwnerType.APPLICATION, app.getPublicId());
      return scanner.scanContent(sbom, work.getScanDir(app.getId()), ItemContentType.SBOM, source, proprietaryConfig);
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
    Application application = applicationDAO.getById(applicationId);

    PolicyEvaluationPollingResult policyEvaluationPollingResult =
        policyEvaluateService.pollEvaluationResult(application.getPublicId(), scanRequestId);

    switch (policyEvaluationPollingResult.getStatus()) {
      case COMPLETED:
        return completed(policyEvaluationPollingResult);
      case FAILED:
        return failed(policyEvaluationPollingResult);
      case PENDING:
        throw new NotFoundException(String
            .format("Report with status id %s for application with id %s is not ready.", scanRequestId,
                applicationId));
      default:
        throw new IllegalArgumentException(String
            .format("Unexpected result %s with status id %s for application with id %s",
                policyEvaluationPollingResult.getStatus(), scanRequestId, applicationId));
    }
  }

  private ApiThirdPartyScanResultDTO completed(final PolicyEvaluationPollingResult policyEvaluationPollingResult) {
    String reportUrl = policyEvaluationPollingResult.getScanReceipt().resolveReportUrl(baseUrl.get());

    ApiPolicyAction outcome = ApiPolicyAction.NONE;
    for (PolicyAlert alert : policyEvaluationPollingResult.getResult().getAlerts()) {
      PolicyFact trigger = alert.getTrigger();
      for (final Action action : alert.getActions()) {
        final String actionTypeId = action.getActionTypeId();
        if (Action.ID_FAIL.equals(actionTypeId)) {
          outcome = outcome.combine(ApiPolicyAction.FAIL);
          log.error("The IQ Server reports policy failing due to {}", trigger);
        }
        else if (Action.ID_WARN.equals(actionTypeId)) {
          outcome = outcome.combine(ApiPolicyAction.WARN);
          log.warn("The IQ Server reports policy warning due to {}", trigger);
        }
      }
    }
    return new ApiThirdPartyScanResultDTO(outcome.toString(), reportUrl);
  }

  private ApiThirdPartyScanResultDTO failed(final PolicyEvaluationPollingResult policyEvaluationPollingResult) {
    return new ApiThirdPartyScanResultDTO(policyEvaluationPollingResult.getReason());
  }

  @VisibleForTesting
  enum ApiPolicyAction
  {
    NONE, WARN, FAIL;

    @Override
    public String toString() {
      switch (this) {
        case NONE:
          return "None";
        case WARN:
          return "Warning";
        case FAIL:
          return "Failure";
        default:
          return super.toString();
      }
    }

    public ApiPolicyAction combine(ApiPolicyAction that) {
      return (this.ordinal() < that.ordinal()) ? that : this;
    }
  }
}
