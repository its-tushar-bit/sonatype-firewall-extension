/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Date;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiEvaluationResultCounterDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanResultDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.api.v2.dto.IdeUsersOverviewDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.NotAcceptableException;
import com.sonatype.insight.brain.dataaccess.ide.UserIdePolicyEvaluationDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationPollingResultDTO;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;
import com.sonatype.insight.brain.scan.ScanResult;
import com.sonatype.insight.brain.scan.Scanner;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.scan.application.ScannerDriver;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.file.SbomProcessingException;
import com.sonatype.insight.scan.file.SbomValidationException;
import com.sonatype.insight.scan.file.ThirdPartyUtils;
import com.sonatype.insight.scan.file.UnsupportedSbomException;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.scan.model.ItemContentType;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.76
 */
@Named
@Singleton
public class ApiThirdPartyScanService
{
  private static final Logger log = LoggerFactory.getLogger(ApiThirdPartyScanService.class);

  private final Scanner scanner;

  private final ProprietaryConfigService proprietaryConfigService;

  private final PolicyEvaluateService policyEvaluateService;

  private final ApplicationDAO applicationDAO;

  private final StageTypeService stageTypeService;

  private final UserIdePolicyEvaluationDAO userIdePolicyEvaluationDao;

  private final CurrentUser currentUser;

  @Inject
  public ApiThirdPartyScanService(
      final Scanner scanner,
      final ProprietaryConfigService proprietaryConfigService,
      final PolicyEvaluateService policyEvaluateService,
      final ApplicationDAO applicationDAO,
      final StageTypeService stageTypeService,
      final UserIdePolicyEvaluationDAO userIdePolicyEvaluationDao,
      final CurrentUser currentUser)
  {
    this.scanner = scanner;
    this.proprietaryConfigService = proprietaryConfigService;
    this.policyEvaluateService = policyEvaluateService;
    this.applicationDAO = applicationDAO;
    this.stageTypeService = stageTypeService;
    this.userIdePolicyEvaluationDao = userIdePolicyEvaluationDao;
    this.currentUser = currentUser;
  }

  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public ApiThirdPartyScanTicketDTO scanComponents(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId,
      final String source,
      final String stageTypeId,
      final String sbom,
      final String clientUserAgent,
      final SbomFormat format)
  {
    if (!Stage.isValidStageTypeId(stageTypeId)) {
      throw new InvalidStageException(stageTypeId);
    }
    if (!stageTypeService.getLicensedStageTypes().contains(StageTypes.getById(stageTypeId))) {
      throw new InvalidLicenseException("Stage '" + stageTypeId + "' is not supported by your license.");
    }

    userIdePolicyEvaluationDao.upsert(currentUser.getUsername());

    ItemContentType type = detectAndValidateSbom(sbom, format);
    String scanRequestId = UUID.randomUUID().toString().replace("-", "");
    ApiThirdPartyScanTicketDTO scanTicketDTO = createScanTicket(applicationId, scanRequestId);

    log.debug("Received request to scan SBOM for app id {}, source {}, stageTypeId {}. "
        + "The status ID of the operation is {}.", applicationId, source, stageTypeId, scanRequestId);
    Application app = applicationDAO.getById(applicationId);
    ScanResult scanResult = createScanFile(app, sbom, source, format, type);

    policyEvaluateService.evaluateWithPolling(scanRequestId, app, ClientScanType.SONATYPE_THIRD_PARTY,
        new Stage(stageTypeId), ScanTriggerType.THIRD_PARTY, scanResult.getScanEntity(),
        ScannerDriver.THIRD_PARTY_API.getValue(), clientUserAgent, null);

    return scanTicketDTO;
  }

  private ItemContentType detectAndValidateSbom(final String sbom, final SbomFormat format) {
    if (StringUtils.isBlank(sbom)) {
      throw new BadRequestException("sbom content is null or empty");
    }
    try {
      if (format == SbomFormat.XML && (sbom.contains("<spdxVersion>") || sbom.contains("<SPDXID>")) &&
          !sbom.contains("<bom") ||
          format == SbomFormat.JSON && (sbom.contains("\"spdxVersion\"") || sbom.contains("\"SPDXID\"")) &&
              !sbom.contains("\"bomFormat\""))
      {
        ThirdPartyUtils.parseAndValidateSpdx(sbom, format);
        return ItemContentType.SPDX;
      }
      else {
        try {
          ThirdPartyUtils.parseAndValidateCycloneDx(sbom, format);
        }
        catch (SbomValidationException ex) {
          if (SystemConfigurationPropertyFeature.SKIP_SBOM_IMPORT_VALIDATION.isEnabled()) {
            ThirdPartyUtils.parseCycloneDxWithNoValidation(sbom, format);
            log.info("SBOM validation skipped per configuration");
          }
          else {
            throw ex;
          }
        }

        return ItemContentType.SBOM;
      }
    }
    catch (SbomValidationException | UnsupportedSbomException e) {
      StringBuilder message = new StringBuilder(e.getMessage());
      for (Throwable suppressedEx : e.getSuppressed()) {
        message.append("\n - ").append(suppressedEx.getMessage());
      }
      throw new NotAcceptableException(message.toString());
    }
    catch (SbomProcessingException e) {
      throw new BadRequestException("SBOM content cannot be parsed.", e);
    }
  }

  private ScanResult createScanFile(
      final Application app,
      final String sbom,
      final String source,
      final SbomFormat format,
      final ItemContentType type)
  {
    try {
      ProprietaryConfig proprietaryConfig =
          proprietaryConfigService.getProprietaryConfig(OwnerType.APPLICATION, app.getPublicId());
      return scanner.scanThirdPartyContent(sbom, app.getId(), type, source, format, proprietaryConfig,
          ScannerDriver.THIRD_PARTY_API.getValue());
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

    PolicyEvaluationPollingResultDTO dto =
        policyEvaluateService.pollEvaluationResult(application.getPublicId(), scanRequestId);

    switch (dto.status) {
      case COMPLETED:
        return completed(application, dto);
      case FAILED:
        return failed(dto);
      case PENDING:
        throw new NotFoundException(String
            .format("Report with status id %s for application with id %s is not ready.", scanRequestId,
                applicationId));
      default:
        throw new IllegalArgumentException(String
            .format("Unexpected result %s with status id %s for application with id %s",
                dto.status, scanRequestId, applicationId));
    }
  }

  private ApiThirdPartyScanResultDTO completed(
      final Application application,
      final PolicyEvaluationPollingResultDTO policyEvaluationPollingResult)
  {
    ScanReceipt scanReceipt = policyEvaluationPollingResult.scanReceipt;
    String reportUrl = scanReceipt.getReportUrl();
    String reportPdfUrl = scanReceipt.getPdfUrl();
    String reportDataUrl = scanReceipt.getDataUrl();
    String embeddableReportUrl =
        UserInterfaceLinksHelper.getEmbeddableReportUrl(application.getPublicId(), scanReceipt.getScanId());

    ApiPolicyAction outcome = ApiPolicyAction.NONE;
    for (PolicyAlert alert : policyEvaluationPollingResult.result.getAlerts()) {
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

    PolicyEvaluationResult result = policyEvaluationPollingResult.result;

    ApiEvaluationResultCounterDTO componentsAffected = buildResultCounter(result.getCriticalComponentCount(),
        result.getModerateComponentCount(), result.getSevereComponentCount());

    ApiEvaluationResultCounterDTO openPolicyViolations = buildResultCounter(result.getCriticalPolicyViolationCount(),
        result.getModeratePolicyViolationCount(), result.getSeverePolicyViolationCount());

    return new ApiThirdPartyScanResultDTO(outcome.toString(), reportUrl, reportPdfUrl, reportDataUrl,
        embeddableReportUrl, componentsAffected, openPolicyViolations, result.getLegacyViolationCount());
  }

  private ApiEvaluationResultCounterDTO buildResultCounter(int critical, int moderate, int severe) {
    ApiEvaluationResultCounterDTO counter = new ApiEvaluationResultCounterDTO();
    counter.critical = critical;
    counter.moderate = moderate;
    counter.severe = severe;
    return counter;
  }

  private ApiThirdPartyScanResultDTO failed(final PolicyEvaluationPollingResultDTO policyEvaluationPollingResult) {
    return new ApiThirdPartyScanResultDTO(policyEvaluationPollingResult.reason);
  }

  public IdeUsersOverviewDTO getIdeUsersOverview(final Long sinceUtcTimestamp) {
    long count = sinceUtcTimestamp == null
        ? userIdePolicyEvaluationDao.getCount()
        : userIdePolicyEvaluationDao.getCountSince(new Date(sinceUtcTimestamp));
    return new IdeUsersOverviewDTO(count);
  }

  @VisibleForTesting
  enum ApiPolicyAction
  {
    NONE,
    WARN,
    FAIL;

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
