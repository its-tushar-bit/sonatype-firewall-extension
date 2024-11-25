/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.ingestion;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportSpecification;
import com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;
import com.sonatype.insight.brain.scan.ScanContext;
import com.sonatype.insight.brain.scan.ScanResult;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.thirdparty.SbomAction;
import com.sonatype.insight.brain.thirdparty.SbomScanType;
import com.sonatype.insight.brain.thirdparty.SbomStatus;
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

  private final ThirdPartyFileDAO thirdPartyFileDAO;

  private final ThirdPartyScanDAO thirdPartyScanDAO;

  private final InsightWork insightWork;

  private final PolicyEvaluateService policyEvaluateService;

  private final SbomMetadataUtils sbomMetadataUtils;

  @Inject
  public SbomScanEvaluator(
      final ApplicationDAO applicationDAO,
      final ThirdPartyFileDAO thirdPartyFileDAO,
      final ThirdPartyScanDAO thirdPartyScanDAO,
      final InsightWork insightWork,
      final PolicyEvaluateService policyEvaluateService,
      final SbomMetadataUtils sbomMetadataUtils)
  {
    this.applicationDAO = applicationDAO;
    this.thirdPartyFileDAO = thirdPartyFileDAO;
    this.thirdPartyScanDAO = thirdPartyScanDAO;
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
    createAndSaveBinaryThirdPartyData(
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
        null
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

  private void createAndSaveBinaryThirdPartyData(
      String applicationId,
      String fileName,
      String applicationVersion,
      String scanRequestId)
  {
    ThirdPartyFile thirdPartyFile = new ThirdPartyFile(fileName, new Date());
    thirdPartyFileDAO.insert(thirdPartyFile);
    ThirdPartyScan thirdPartyScan = new ThirdPartyScan(thirdPartyFile.getId(), scanRequestId, new Date());
    thirdPartyScanDAO.insert(thirdPartyScan);
    ThirdPartySbomMetadata thirdPartySbomMetadata = new ThirdPartySbomMetadata(
        thirdPartyFile.getId(),
        applicationId,
        applicationVersion != null ? applicationVersion : dtFormatter.format(LocalDateTime.now()),
        thirdPartyFile.getFilename(),
        UUID.randomUUID().toString(),
        SbomSpecification.CYCLONEDX.toString(),
        SbomFormat.JSON.toString(),
        ExportSpecification.DEFAULT.getVersion(),
        SbomStatus.PENDING.toString(),
        new Date(),
        SbomCycloneDxUtils.getGenericSbomCreationDetailsAsString(),
        SbomScanType.BINARY.toString(),
        true,
        thirdPartyFile.getFilename()
    );
    sbomMetadataUtils.insertThirdPartySbomMetadataWithRetry(thirdPartySbomMetadata);
    AuditData.get().setSbomVersion(thirdPartySbomMetadata, SbomAction.CREATE);
  }
}
