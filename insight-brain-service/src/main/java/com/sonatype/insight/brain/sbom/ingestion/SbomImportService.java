/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.ingestion;

import java.io.IOException;
import java.io.InputStream;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.sbom.SbomComponentInfoTelemetry;
import com.sonatype.insight.brain.sbom.datastore.SbomEntity;
import com.sonatype.insight.brain.sbom.utils.SbomDetectionResult;
import com.sonatype.insight.brain.sbom.utils.SbomFileDetector;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.thirdparty.SbomScanType;
import com.sonatype.insight.brain.thirdparty.ThirdPartyPersistenceService;
import com.sonatype.insight.brain.utils.CheckedIllegalArgumentException;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.error.exception.InternalServerException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.error.exception.PaymentRequiredException;
import com.sonatype.insight.telemetry.model.TelemetryData;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.RollbackException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class SbomImportService
{
  private static final Logger log = LoggerFactory.getLogger(SbomImportService.class);

  private final ApplicationDAO applicationDAO;

  private final ThirdPartySbomMetadataDAO sbomMetadataDAO;

  private final ThirdPartyFileDAO thirdPartyFileDAO;

  private final SbomScanEvaluator sbomScanEvaluator;

  private final SbomFileDetector sbomFileDetector;

  private final SbomMetadataUtils sbomMetadataUtils;

  private final ProductLicense productLicense;

  private final TelemetrySender telemetrySender;

  private final TelemetryUtils telemetryUtils;

  private final ThirdPartyPersistenceService thirdPartyPersistenceService;

  @Inject
  public SbomImportService(
      ApplicationDAO applicationDAO,
      ThirdPartySbomMetadataDAO sbomMetadataDAO,
      ThirdPartyFileDAO thirdPartyFileDAO,
      SbomScanEvaluator sbomScanEvaluator,
      SbomFileDetector sbomFileDetector,
      SbomMetadataUtils sbomMetadataUtils,
      ProductLicense productLicense,
      TelemetryUtils telemetryUtils,
      TelemetrySender telemetrySender,
      ThirdPartyPersistenceService thirdPartyPersistenceService)
  {
    this.applicationDAO = applicationDAO;
    this.sbomMetadataDAO = sbomMetadataDAO;
    this.thirdPartyFileDAO = thirdPartyFileDAO;
    this.sbomScanEvaluator = sbomScanEvaluator;
    this.sbomFileDetector = sbomFileDetector;
    this.sbomMetadataUtils = sbomMetadataUtils;
    this.productLicense = productLicense;
    this.telemetryUtils = telemetryUtils;
    this.telemetrySender = telemetrySender;
    this.thirdPartyPersistenceService = thirdPartyPersistenceService;
  }

  @Authorize(permission = Permission.WRITE)
  public SbomDetectionResultDTO detectSbom(
      @AuthzContext(Key.APPLICATION_ID) String applicationId,
      InputStream sbomStream,
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

    // A temp file that lasts only the duration of this method call, used only to avoid reading the whole
    // SBOM into memory at once
    SbomEntity tempSbomEntity = null;
    try {
      tempSbomEntity = thirdPartyPersistenceService.writeToTransientStorage(sbomStream, originalFilename);
      log.debug("Saved file for detection at {}", tempSbomEntity);

      SbomDetectionResult result =
          sbomFileDetector.getSbomDetectionResult(tempSbomEntity.getPath(), originalFilename,
              ignoreValidationError);

      if (StringUtils.isNotEmpty(result.errorMessage)) {
        // telemetry for when there aren't validation errors is sent elsewhere. SBOM-1113 will consolidate this.
        sendTelemetry(result);
      }

      if (Boolean.FALSE.equals(result.isValidationErrorIgnorable)) {
        // don't attempt to save if there was an unignorable validation error
        return new SbomDetectionResultDTO(SbomScanType.SBOM, result);
      }
      else if (result.isSbom) {
        String savedVersion = saveSbomManagerSbomOrBinary(tempSbomEntity, originalFilename, applicationId, result);
        return new SbomDetectionResultDTO(SbomScanType.SBOM, result, savedVersion);
      }
      else if (SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.isEnabled()) {
        String savedVersion = saveSbomManagerSbomOrBinary(tempSbomEntity, originalFilename, applicationId, result);
        return new SbomDetectionResultDTO(SbomScanType.BINARY, result, savedVersion);
      }
      else {
        throw new BadRequestException("Importing binary files for SBOM Manager is disabled.");
      }
    }
    catch (IOException e) {
      throw new InternalServerException("Internal server error detecting SBOM", e);
    }
    catch (CheckedIllegalArgumentException e) {
      throw new BadRequestException(e);
    }
    finally {
      try {
        if (tempSbomEntity != null) {
          thirdPartyPersistenceService.deleteSbomFromTransientStorage(tempSbomEntity);
        }
      }
      catch (IOException e) {
        log.warn("Failed to delete temporary SBOM file: {}", tempSbomEntity.getLocation(), e);
      }
    }
  }

  @Authorize(permission = Permission.WRITE)
  public Response importDetectedSbom(
      @AuthzContext(Key.APPLICATION_ID) String applicationId,
      String applicationVersion,
      String applicationVersionOverride,
      String clientUserAgent)
  {
    ThirdPartySbomMetadata sbomMetadata =
        sbomMetadataDAO.getByApplicationIdAndSbomVersion(applicationId, applicationVersion);

    if (sbomMetadata == null) {
      throw new NotFoundException("SBOM with applicationId " + applicationId + " and version " + applicationVersion
          + " does not exist");
    }
    else if (sbomMetadata.getStatus() != ThirdPartySbomMetadataStatus.UPLOADED) {
      throw new BadRequestException("SBOM with applicationId " + applicationId + " and version " + applicationVersion
          + " is not in UPLOADED state");
    }

    ThirdPartyFile thirdPartyFile = thirdPartyFileDAO.getByIdNotNull(sbomMetadata.getThirdPartyFileId());

    try (TransactionContext tx = sbomMetadataDAO.createTransactionContext()) {
      tx.begin();

      handleVersionOverride(tx, sbomMetadata, applicationVersionOverride);

      var importTicket = switch (SbomScanType.valueOf(sbomMetadata.getScanType())) {
        case SBOM -> importSbom(sbomMetadata, clientUserAgent);
        case BINARY -> importBinary(sbomMetadata, thirdPartyFile, clientUserAgent);
      };

      tx.commit();
      return Response.status(Status.ACCEPTED).entity(importTicket).build();
    }
    catch (IOException e) {
      throw new InternalServerException("Internal server error importing SBOM", e);
    }
    catch (EntityExistsException | RollbackException e) {
      String conflictVersion = StringUtils.defaultIfEmpty(applicationVersionOverride, applicationVersion);
      if (e instanceof EntityExistsException || e.getCause() instanceof EntityExistsException) {
        throw new ConflictException("Version %s already exists".formatted(conflictVersion), e);
      }
      else {
        throw e;
      }
    }
  }

  /**
   * @return The version under which the SBOM was saved in the database
   */
  private String saveSbomManagerSbomOrBinary(
      SbomEntity tempSbomEntity,
      String originalFilename,
      String applicationId,
      SbomDetectionResult result) throws IOException, CheckedIllegalArgumentException
  {
    var sbomMetadata = thirdPartyPersistenceService.saveSbomManagerSbomOrBinary(
        tempSbomEntity,
        originalFilename,
        applicationId,
        result
    ).getLeft();

    return sbomMetadata.getSbomVersion();
  }

  private ApiThirdPartyScanTicketDTO importSbom(
      ThirdPartySbomMetadata sbomMetadata,
      String clientUserAgent)
  {
    return sbomScanEvaluator.evaluateSbom(sbomMetadata, ScanTriggerType.SBOM_UI, clientUserAgent);
  }

  private ApiThirdPartyScanTicketDTO importBinary(
      ThirdPartySbomMetadata sbomMetadata,
      ThirdPartyFile thirdPartyFile,
      String clientUserAgent) throws IOException
  {
    ApiThirdPartyScanTicketDTO retval = sbomScanEvaluator.evaluateBinary(
        sbomMetadata,
        thirdPartyFile,
        ScanTriggerType.SBOM_UI,
        clientUserAgent
    );

    thirdPartyPersistenceService.deletePersistentTempBinary(sbomMetadata, thirdPartyFile);

    return retval;
  }

  private void handleVersionOverride(
      TransactionContext tx,
      ThirdPartySbomMetadata sbomMetadata,
      String applicationVersionOverride)
  {
    // While it isn't this class' job to do validation of the new value per se, it is this class' job to assess whether
    // the user was even trying to update the version in the first place.
    if (StringUtils.isNotEmpty(applicationVersionOverride)) {
      try {
        thirdPartyPersistenceService.updateApplicationVersion(tx, sbomMetadata, applicationVersionOverride);
      }
      catch (CheckedIllegalArgumentException e) {
        throw new BadRequestException(e);
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
}
