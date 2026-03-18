/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.ingestion;

import java.io.IOException;
import java.io.UncheckedIOException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.sbom.datastore.SbomEntity;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;
import com.sonatype.insight.brain.scan.ScanContext;
import com.sonatype.insight.brain.scan.ScanResult;
import com.sonatype.insight.brain.thirdparty.ThirdPartyPersistenceService;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.scan.application.ScannerDriver;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.scan.model.ItemContentType;

@Named
@Singleton
public class SbomScanEvaluator
{
  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluateService policyEvaluateService;

  private final SbomMetadataUtils sbomMetadataUtils;

  private final ThirdPartyPersistenceService thirdPartyPersistenceService;

  @Inject
  public SbomScanEvaluator(
      final ApplicationDAO applicationDAO,
      final PolicyEvaluateService policyEvaluateService,
      final SbomMetadataUtils sbomMetadataUtils,
      final ThirdPartyPersistenceService thirdPartyPersistenceService)
  {
    this.applicationDAO = applicationDAO;
    this.policyEvaluateService = policyEvaluateService;
    this.sbomMetadataUtils = sbomMetadataUtils;
    this.thirdPartyPersistenceService = thirdPartyPersistenceService;
  }

  public ApiThirdPartyScanTicketDTO evaluateBinary(
      ThirdPartySbomMetadata sbomMetadata,
      ThirdPartyFile thirdPartyFile,
      ScanTriggerType scanTriggerType,
      String clientUserAgent)
  {
    var applicationId = sbomMetadata.getApplicationId();
    Application application = applicationDAO.getById(applicationId);
    SbomEntity tmpFileToScan =
        thirdPartyPersistenceService.getBinaryPersistentTempFilePath(sbomMetadata, thirdPartyFile);

    ScanResult scanResult = sbomMetadataUtils.scanBinaryFile(application, tmpFileToScan.getPath().toFile());

    ApiThirdPartyScanTicketDTO scanTicketDTO = sbomMetadataUtils.createSbomImportTicket(applicationId);

    // Scan processing would do this automatically for an actual SBOM, but for a binary we need to do it manually here
    thirdPartyPersistenceService.associateWithScan(thirdPartyFile, scanTicketDTO.requestId);
    setSbomMetadataStatusToPending(sbomMetadata);

    ClientScanType clientScanType = scanResult.getClientScanType();

    policyEvaluateService.evaluateWithPolling(
        scanTicketDTO.requestId,
        application,
        clientScanType,
        new Stage(StageTypes.COMPLIANCE.getId()),
        scanTriggerType,
        scanResult.getScanEntity(),
        ScannerDriver.SBOM_API.getValue(),
        clientUserAgent,
        null,
        new ScanContext.Builder().applicationVersion(sbomMetadata.getSbomVersion()).isValid(true).build());

    return scanTicketDTO;
  }

  public ApiThirdPartyScanTicketDTO evaluateSbom(
      ThirdPartySbomMetadata sbomMetadata,
      ScanTriggerType scanTriggerType,
      String clientUserAgent)
  {
    var applicationId = sbomMetadata.getApplicationId();
    Application application = applicationDAO.getById(applicationId);
    SbomFormat sbomFormat = SbomFormat.forString(sbomMetadata.getSpecFormat());
    ItemContentType contentType = SbomSpecification.fromValue(sbomMetadata.getSpec()).toItemContentType();
    ApiThirdPartyScanTicketDTO importTicket = sbomMetadataUtils.createSbomImportTicket(applicationId);

    ScanResult scanResult;
    try {
      scanResult = sbomMetadataUtils.scanSbomInputStream(
          application,
          thirdPartyPersistenceService.getSbomContentsInputStream(sbomMetadata),
          sbomFormat,
          contentType,
          ScannerDriver.SBOM_API);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    setSbomMetadataStatusToPending(sbomMetadata);

    policyEvaluateService.evaluateWithPolling(
        importTicket.requestId,
        application,
        ClientScanType.SONATYPE_THIRD_PARTY,
        new Stage(StageTypes.COMPLIANCE.getId()),
        scanTriggerType,
        scanResult.getScanEntity(),
        ScannerDriver.SBOM_API.getValue(),
        clientUserAgent,
        null,
        new ScanContext.Builder()
            .applicationVersion(sbomMetadata.getSbomVersion())
            .sbomMetadataId(sbomMetadata.getId())
            .isValid(sbomMetadata.getIsValid())
            .build());
    return importTicket;
  }

  private void setSbomMetadataStatusToPending(ThirdPartySbomMetadata sbomMetadata) {
    try {
      thirdPartyPersistenceService.setSbomMetadataStatusToPending(sbomMetadata);
    }
    catch (IllegalStateException e) {
      // Existing status is not UPLOADED
      throw new BadRequestException(e);
    }
  }
}
