/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.ingestion;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.sbom.utils.SbomDetectionResult;
import com.sonatype.insight.brain.sbom.utils.SbomFileDetector;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.thirdparty.SbomScanType;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.InternalServerException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.error.exception.PaymentRequiredException;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.model.ItemContentType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class SbomImportService
{
  private static final Logger log = LoggerFactory.getLogger(SbomImportService.class);

  private final ApplicationDAO applicationDAO;

  private final SbomScanEvaluator sbomScanEvaluator;

  private final InsightWork insightWork;

  private final SbomFileDetector sbomFileDetector;

  private final SbomMetadataUtils sbomMetadataUtils;

  private final ProductLicense productLicense;

  @Inject
  public SbomImportService(
      ApplicationDAO applicationDAO,
      SbomScanEvaluator sbomScanEvaluator,
      InsightWork insightWork,
      SbomFileDetector sbomFileDetector,
      SbomMetadataUtils sbomMetadataUtils,
      ProductLicense productLicense)
  {
    this.applicationDAO = applicationDAO;
    this.sbomScanEvaluator = sbomScanEvaluator;
    this.insightWork = insightWork;
    this.sbomFileDetector = sbomFileDetector;
    this.sbomMetadataUtils = sbomMetadataUtils;
    this.productLicense = productLicense;
  }

  @Authorize(permission = Permission.WRITE)
  public SbomDetectionResultDTO detectSbom(
      @AuthzContext(Key.APPLICATION_ID) String applicationId,
      InputStream sbom,
      String originalFilename)
  {
    if (applicationDAO.getById(applicationId) == null) {
      throw new NotFoundException("Application with id " + applicationId + " does not exist");
    }

    if (sbomMetadataUtils.hasMaxSbomLimitBeenReached()) {
      throw new PaymentRequiredException("You have exceeded the licensed limit of " + productLicense.getMaxSboms()
          + " sboms.");
    }

    String fileNameUUID = UUID.randomUUID().toString().replace("-", "");
    String tempFilename = String.format("%s-%s", fileNameUUID, originalFilename);
    File tempSbomFile = new File(insightWork.getSbomTempDir(), tempFilename);
    try (OutputStream outputStream = Files.newOutputStream(tempSbomFile.toPath())) {
      IOUtils.copy(sbom, outputStream);
      log.debug("Saved file for detection at {}", tempSbomFile.getPath());
    }
    catch (IOException e) {
      if (tempSbomFile.exists()) {
        String deletionResult = tempSbomFile.delete() ? "succeeded" : "failed";
        log.debug("Deleting file located at {} due to an error {}", tempSbomFile.getPath(), deletionResult);
      }
      throw new InternalServerException("Internal error saving the supplied file", e);
    }
    SbomDetectionResult result = sbomFileDetector.getSbomDetectionResult(tempSbomFile);

    String filenameToUseForRequestId = "";
    SbomScanType scanType = null;

    if (result.isSbom) {
      scanType = SbomScanType.SBOM;
      filenameToUseForRequestId = String.format("%s-%s-%s-%s-%s", scanType.name(), result.mimeType,
          (result.summary != null ? result.summary.specification : ""), fileNameUUID, originalFilename);
    }
    else if (result.isBinary) {
      if (SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.isEnabled()) {
        scanType = SbomScanType.BINARY;
        filenameToUseForRequestId =
            String.format("%s-%s-%s", scanType.name(), fileNameUUID, originalFilename);
      }
      else {
        throw new BadRequestException("Importing binary files for SBOM Manager is disabled.");
      }
    }
    else if (result.validationErrors != null) {
      scanType = SbomScanType.SBOM;
      deleteTempFile(tempSbomFile, result.errorMessage);
    }
    else {
      log.debug("Unable to process the SBOM import for file: {}, error: {}", tempFilename, result.errorMessage);
      deleteTempFile(tempSbomFile, result.errorMessage);

      throw new BadRequestException("Unable to process the input file");
    }

    String requestId = Base64.getEncoder().encodeToString(filenameToUseForRequestId.getBytes());
    return new SbomDetectionResultDTO(requestId, result.summary, result.errorMessage, result.validationErrors,
        scanType);
  }

  @Authorize(permission = Permission.WRITE)
  public Response importDetectedSbom(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String requestId, String clientUserAgent)
  {
    if (sbomMetadataUtils.hasMaxSbomLimitBeenReached()) {
      throw new PaymentRequiredException("You have exceeded the licensed limit of " + productLicense.getMaxSboms()
          + " sboms.");
    }

    SbomRequestIdElements idElements = sbomMetadataUtils.decodeRequestId(requestId);
    SbomFormat sbomFormat = idElements.getSbomFormat();
    ItemContentType contentType = idElements.getContentType();

    Path tmpImportFilePath = Paths.get(insightWork.getSbomTempDir().getPath(), idElements.getFilename());
    if (!Files.exists(tmpImportFilePath)) {
      throw new NotFoundException("Request with id " + requestId + " does not exist");
    }

    Path tmpDir = null;
    String originalFileName = StringUtils.substringAfter(idElements.getFilename(), "-");
    try {
      //copy the file to a new directory to avoid name conflicts in case of concurrent imports with the same file name
      tmpDir = Files.createTempDirectory(insightWork.getSbomTempDir().toPath(), null);
      File tmpFileToScan = new File(tmpDir.toFile(), originalFileName);
      FileUtils.copyFile(tmpImportFilePath.toFile(), tmpFileToScan);

      if (SbomScanType.SBOM.equals(idElements.getScanType())) {
        ApiThirdPartyScanTicketDTO importTicket = sbomScanEvaluator.evaluateSbom(
            applicationId, tmpFileToScan, sbomFormat, contentType, ScanTriggerType.SBOM_UI, clientUserAgent, null);
        return Response.status(Status.ACCEPTED).entity(importTicket).build();
      }
      else { //Binary
        ApiThirdPartyScanTicketDTO scanTicketDTO =
            sbomScanEvaluator.evaluateBinary(applicationId, tmpFileToScan, ScanTriggerType.SBOM_UI, clientUserAgent,
                null);
        return Response.status(Status.ACCEPTED)
            .entity(scanTicketDTO)
            .build();
      }
    }
    catch (IOException e) {
      throw new InternalServerException("Internal server error importing SBOM", e);
    }
    finally {
      if (tmpDir != null) {
        try {
          Files.deleteIfExists(tmpDir);
          Files.delete(tmpImportFilePath);
        }
        catch (IOException e) {
          log.error("error deleting temporary sbom file", e);
        }
      }
    }
  }

  private void deleteTempFile(File tempFile, String errorMessage) {
    if (tempFile.exists()) {
      String deletionResult = tempFile.delete() ? "succeeded" : "failed";
      log.debug("Deleting file due to an error, {}, {} ", errorMessage, deletionResult);
    }
  }
}
