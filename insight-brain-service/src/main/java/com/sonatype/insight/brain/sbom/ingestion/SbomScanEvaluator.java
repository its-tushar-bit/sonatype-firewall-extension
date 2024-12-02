/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.ingestion;

import java.io.File;
import java.time.format.DateTimeFormatter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;
import com.sonatype.insight.brain.scan.ScanContext;
import com.sonatype.insight.brain.scan.ScanResult;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.scan.application.ScannerDriver;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.scan.model.ItemContentType;

@Named
@Singleton
public class SbomScanEvaluator
{
  private static final DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

  private final ApplicationDAO applicationDAO;

  private final InsightWork insightWork;

  private final PolicyEvaluateService policyEvaluateService;

  private final SbomMetadataUtils sbomMetadataUtils;

  @Inject
  public SbomScanEvaluator(
      final ApplicationDAO applicationDAO,
      final InsightWork insightWork,
      final PolicyEvaluateService policyEvaluateService,
      final SbomMetadataUtils sbomMetadataUtils)
  {
    this.applicationDAO = applicationDAO;
    this.insightWork = insightWork;
    this.policyEvaluateService = policyEvaluateService;
    this.sbomMetadataUtils = sbomMetadataUtils;
  }

  public ApiThirdPartyScanTicketDTO evaluateBinary(
      String applicationId,
      File tmpFileToScan,
      ScanTriggerType scanTriggerType,
      String clientUserAgent,
      String applicationVersion)
  {
    Application application = applicationDAO.getById(applicationId);
    ScanResult scanResult = sbomMetadataUtils.scanBinaryFile(
        application,
        tmpFileToScan,
        insightWork.getScanDir(applicationId)
    );
    ApiThirdPartyScanTicketDTO scanTicketDTO = sbomMetadataUtils.createSbomImportTicket(applicationId);
    ThirdPartySbomMetadata sbomMetadata = sbomMetadataUtils.createAndSaveBinaryThirdPartyData(
        applicationId,
        tmpFileToScan.getName(),
        applicationVersion,
        scanTicketDTO.requestId
    );
    ClientScanType clientScanType = scanResult.getClientScanType();
    policyEvaluateService.evaluateWithPolling(
        scanTicketDTO.requestId,
        application,
        clientScanType,
        new Stage(StageTypes.COMPLIANCE.getId()),
        scanTriggerType,
        scanResult.getScanFile(),
        ScannerDriver.SBOM_API.getValue(),
        clientUserAgent,
        null,
        new ScanContext.Builder().applicationVersion(sbomMetadata.getSbomVersion()).isValid(true).build()
    );
    return scanTicketDTO;
  }

  public ApiThirdPartyScanTicketDTO evaluateSbom(
      String applicationId,
      File tmpFileToScan,
      SbomFormat sbomFormat,
      ItemContentType contentType,
      ScanTriggerType scanTriggerType,
      String clientUserAgent,
      String applicationVersion,
      boolean isValid)
  {
    Application application = applicationDAO.getById(applicationId);
    ApiThirdPartyScanTicketDTO importTicket = sbomMetadataUtils.createSbomImportTicket(applicationId);
    ScanResult scanResult = sbomMetadataUtils.scanSbomFile(
        application,
        tmpFileToScan,
        insightWork.getScanDir(application.getId()),
        sbomFormat,
        contentType,
        ScannerDriver.SBOM_API
    );
    policyEvaluateService.evaluateWithPolling(
        importTicket.requestId,
        application,
        ClientScanType.SONATYPE_THIRD_PARTY,
        new Stage(StageTypes.COMPLIANCE.getId()),
        scanTriggerType,
        scanResult.getScanFile(),
        ScannerDriver.SBOM_API.getValue(),
        clientUserAgent,
        null,
        new ScanContext.Builder().applicationVersion(applicationVersion).isValid(isValid).build()
    );
    return importTicket;
  }
}
