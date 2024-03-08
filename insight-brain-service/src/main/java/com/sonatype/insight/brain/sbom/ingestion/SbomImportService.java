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
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.sbom.utils.SbomDetectionResult;
import com.sonatype.insight.brain.sbom.utils.SbomFileDetector;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.InternalServerException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class SbomImportService
{
  private static final Logger log = LoggerFactory.getLogger(SbomImportService.class);

  private final ApplicationDAO applicationDAO;

  private final InsightWork insightWork;

  private final SbomFileDetector sbomFileDetector;
  
  @Inject
  public SbomImportService(ApplicationDAO applicationDAO,
                           InsightWork insightWork,
                           SbomFileDetector sbomFileDetector)
  {
    this.applicationDAO = applicationDAO;
    this.insightWork = insightWork;
    this.sbomFileDetector = sbomFileDetector;
  }

  @Authorize(permission = Permission.WRITE)
  public SbomDetectionResultDTO detectSbom(@AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
                                              InputStream sbom)
  {
    if (applicationDAO.getById(applicationId) == null) {
      throw new NotFoundException("Application with id " + applicationId + " does not exist");
    }
    String requestId = UUID.randomUUID().toString().replace("-", "");
    String filename = requestId + ".tmp";
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
    SbomDetectionResult result = sbomFileDetector.getSbomMetadata(tempSbomFile);
    if (result.errorMessage != null && tempSbomFile.exists()) {
      String deletionResult = tempSbomFile.delete() ? "succeeded" : "failed";
      log.debug("Deleting file due to an error, {}, {} ", result.errorMessage, deletionResult);
    }
    return new SbomDetectionResultDTO(requestId, result.summary, result.errorMessage);
  }
}
