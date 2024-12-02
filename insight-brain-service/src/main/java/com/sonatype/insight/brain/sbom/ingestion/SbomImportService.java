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
import com.sonatype.insight.brain.sbom.SbomComponentInfoTelemetry;
import com.sonatype.insight.brain.sbom.utils.SbomDetectionResult;
import com.sonatype.insight.brain.sbom.utils.SbomFileDetector;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.thirdparty.SbomScanType;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.InternalServerException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.error.exception.PaymentRequiredException;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.telemetry.model.TelemetryData;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.sbom.ingestion.SbomRequestIdElements.decodeFromRequestId;

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

  private final TelemetrySender telemetrySender;

  private final TelemetryUtils telemetryUtils;

  @Inject
  public SbomImportService(
      ApplicationDAO applicationDAO,
      SbomScanEvaluator sbomScanEvaluator,
      InsightWork insightWork,
      SbomFileDetector sbomFileDetector,
      SbomMetadataUtils sbomMetadataUtils,
      ProductLicense productLicense,
      TelemetryUtils telemetryUtils,
      TelemetrySender telemetrySender)
  {
    this.applicationDAO = applicationDAO;
    this.sbomScanEvaluator = sbomScanEvaluator;
    this.insightWork = insightWork;
    this.sbomFileDetector = sbomFileDetector;
    this.sbomMetadataUtils = sbomMetadataUtils;
    this.productLicense = productLicense;
    this.telemetryUtils = telemetryUtils;
    this.telemetrySender = telemetrySender;
  }

  @Authorize(permission = Permission.WRITE)
  public SbomDetectionResultDTO detectSbom(
      @AuthzContext(Key.APPLICATION_ID) String applicationId,
      InputStream sbom,
      String originalFilename,
      boolean ignoreValidationError)
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

    SbomDetectionResult result = sbomFileDetector.getSbomDetectionResult(tempSbomFile, ignoreValidationError);

    SbomRequestIdElements idElements;
    if (result.isSbom && StringUtils.isNotEmpty(result.errorMessage)) {
      sendTelemetry(result);
      deleteTempFile(tempSbomFile, result.errorMessage);
      return new SbomDetectionResultDTO("", SbomScanType.SBOM, result);
    }
    else if (result.isSbom) {
      String sbomSpecification = result.summary != null ? result.summary.specification : "";
      idElements = new SbomRequestIdElements(fileNameUUID, originalFilename, SbomFormat.forMimeType(result.mimeType),
          sbomMetadataUtils.determineItemContentType(sbomSpecification), result.isValid);
    }
    else {
      if (SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.isEnabled()) {
        idElements = new SbomRequestIdElements(fileNameUUID, originalFilename);
      }
      else {
        deleteTempFile(tempSbomFile, "Importing binary files for SBOM Manager is disabled.");
        throw new BadRequestException("Importing binary files for SBOM Manager is disabled.");
      }
    }

    return new SbomDetectionResultDTO(idElements, result);
  }

  @Authorize(permission = Permission.WRITE)
  public Response importDetectedSbom(
      @AuthzContext(Key.APPLICATION_ID) String applicationId,
      String requestId,
      String clientUserAgent)
  {
    if (sbomMetadataUtils.hasMaxSbomLimitBeenReached()) {
      throw new PaymentRequiredException("You have exceeded the licensed limit of " + productLicense.getMaxSboms()
          + " sboms.");
    }

    SbomRequestIdElements idElements = decodeFromRequestId(requestId);
    if (idElements == null) {
      throw new BadRequestException("Request with id " + requestId + " does not exist");
    }

    Path tmpImportFilePath = insightWork.getSbomTempDir().toPath().resolve(idElements.getStoredFileName());
    if (!Files.exists(tmpImportFilePath)) {
      throw new NotFoundException("Request with id " + requestId + " does not exist");
    }

    Path tmpDir = null;
    try {
      //copy the file to a new directory to avoid name conflicts in case of concurrent imports with the same file name
      tmpDir = Files.createTempDirectory(insightWork.getSbomTempDir().toPath(), null);
      File tmpFileToScan = new File(tmpDir.toFile(), idElements.getOriginalFileName());
      FileUtils.copyFile(tmpImportFilePath.toFile(), tmpFileToScan);

      if (SbomScanType.SBOM.equals(idElements.getScanType())) {
        ApiThirdPartyScanTicketDTO importTicket = sbomScanEvaluator.evaluateSbom(applicationId, tmpFileToScan,
            idElements.getSbomFormat(), idElements.getContentType(), ScanTriggerType.SBOM_UI, clientUserAgent, null,
            idElements.isSbomValid());

        return Response.status(Status.ACCEPTED).entity(importTicket).build();
      }
      else { //Binary
        ApiThirdPartyScanTicketDTO scanTicketDTO = sbomScanEvaluator.evaluateBinary(applicationId, tmpFileToScan,
            ScanTriggerType.SBOM_UI, clientUserAgent, null);
        return Response.status(Status.ACCEPTED).entity(scanTicketDTO).build();
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

  private void sendTelemetry(final SbomDetectionResult result) {
    SbomComponentInfoTelemetry componentInfoTelemetry = new SbomComponentInfoTelemetry();

    if (result.summary != null) {
      componentInfoTelemetry.setContentType(result.summary.format);
      componentInfoTelemetry.setSpecVersion(result.summary.version);
      componentInfoTelemetry.setSpec(result.summary.specification);
    }
    componentInfoTelemetry.setValidationErrorsCount(
        result.validationErrors == null ? 0 : result.validationErrors.size());
    TelemetryData thirdPartyScanComponentInfoTelemetryData =
        telemetryUtils.buildThirdPartyScanComponentInfoTelemetryData(componentInfoTelemetry,
            SystemConfigurationPropertyFeature.SKIP_SBOM_IMPORT_VALIDATION.isEnabled(),
            result.isValid != null && result.isValid);
    telemetrySender.send(thirdPartyScanComponentInfoTelemetryData);
  }

  private void deleteTempFile(File tempFile, String errorMessage) {
    if (tempFile.exists()) {
      String deletionResult = tempFile.delete() ? "succeeded" : "failed";
      log.debug("Deleting file due to an error, {}, {} ", errorMessage, deletionResult);
    }
  }
}
