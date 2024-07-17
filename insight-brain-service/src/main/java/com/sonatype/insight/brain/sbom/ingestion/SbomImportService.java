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
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.sbom.utils.SbomDetectionResult;
import com.sonatype.insight.brain.sbom.utils.SbomFileDetector;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;
import com.sonatype.insight.brain.scan.ScanResult;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.InternalServerException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.error.exception.PaymentRequiredException;
import com.sonatype.insight.scan.application.ScannerDriver;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.scan.model.ItemContentType;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class SbomImportService
{
  private static final Logger log = LoggerFactory.getLogger(SbomImportService.class);

  private final ApplicationDAO applicationDAO;

  private final InsightWork insightWork;

  private final SbomFileDetector sbomFileDetector;

  private final PolicyEvaluateService policyEvaluateService;

  private final SbomMetadataUtils sbomMetadataUtils;

  private final ProductLicense productLicense;

  @Inject
  public SbomImportService(
      ApplicationDAO applicationDAO,
      InsightWork insightWork,
      SbomFileDetector sbomFileDetector,
      PolicyEvaluateService policyEvaluateService,
      SbomMetadataUtils sbomMetadataUtils,
      ProductLicense productLicense)
  {
    this.applicationDAO = applicationDAO;
    this.insightWork = insightWork;
    this.sbomFileDetector = sbomFileDetector;
    this.policyEvaluateService = policyEvaluateService;
    this.sbomMetadataUtils = sbomMetadataUtils;
    this.productLicense = productLicense;
  }

  @Authorize(permission = Permission.WRITE)
  public SbomDetectionResultDTO detectSbom(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      InputStream sbom)
  {
    if (applicationDAO.getById(applicationId) == null) {
      throw new NotFoundException("Application with id " + applicationId + " does not exist");
    }

    if (sbomMetadataUtils.hasMaxSbomLimitBeenReached()) {
      throw new PaymentRequiredException("You have exceeded the licensed limit of " + productLicense.getMaxSboms()
          + " sboms.");
    }

    String fileNameUUID = UUID.randomUUID().toString().replace("-", "");
    String filename = fileNameUUID + ".tmp";
    File tempSbomFile = new File(insightWork.getSbomTempDir(), filename);
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
    if (result.errorMessage != null && tempSbomFile.exists()) {
      String deletionResult = tempSbomFile.delete() ? "succeeded" : "failed";
      log.debug("Deleting file due to an error, {}, {} ", result.errorMessage, deletionResult);
    }
    String requestId = Base64.getEncoder().encodeToString(
        String.format("%s-%s-%s", fileNameUUID, result.mimeType,
            result.summary != null ? result.summary.specification : "").getBytes());
    return new SbomDetectionResultDTO(requestId, result.summary, result.errorMessage);
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
    String[] decodedRequestId;
    try {
      decodedRequestId = new String(Base64.getDecoder().decode(requestId)).split("-");

      if (decodedRequestId.length != 3 || decodedRequestId[0].isEmpty() || decodedRequestId[1].isEmpty() ||
          decodedRequestId[2].isEmpty()) {
        throw new BadRequestException("The provided requestId " + requestId + " is not valid.");
      }
    }
    catch (IllegalArgumentException e) {
      throw new BadRequestException("The provided requestId " + requestId + " is not valid.");
    }

    String fileName = decodedRequestId[0];
    SbomFormat sbomFormat = SbomFormat.forMimeType(decodedRequestId[1]);
    ItemContentType contentType = sbomMetadataUtils.determineItemContentType(decodedRequestId[2]);

    Application application = applicationDAO.getById(applicationId);

    Path path = Paths.get(insightWork.getSbomTempDir().getPath(), fileName + ".tmp");
    if (!Files.exists(path)) {
      throw new NotFoundException("Request with id " + requestId + " does not exist");
    }

    File tempSbomFile = path.toFile();
    try {
      ApiThirdPartyScanTicketDTO importTicket = sbomMetadataUtils.createSbomImportTicket(applicationId);
      ScanResult scanResult =
          sbomMetadataUtils.scanSbomFile(application, tempSbomFile, insightWork.getScanDir(application.getId()),
              sbomFormat, contentType, ScannerDriver.SBOM_API);

      policyEvaluateService.evaluateWithPolling(importTicket.requestId, application,
          ClientScanType.SONATYPE_THIRD_PARTY, new Stage(StageTypes.COMPLIANCE.getId()), ScanTriggerType.SBOM_UI,
          scanResult.getScanFile(), ScannerDriver.SBOM_API.getValue(), clientUserAgent, null);

      return Response.status(Response.Status.CREATED).entity(importTicket).build();
    }

    finally {
      try {
        Files.deleteIfExists(tempSbomFile.toPath());
      }
      catch (IOException e) {
        log.error("error deleting temporary sbom file", e);
      }
    }
  }
}
