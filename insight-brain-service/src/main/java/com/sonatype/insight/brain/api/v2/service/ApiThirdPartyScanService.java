/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.BadRequestException;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanResultDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.cyclonedx.CycloneDxSchemaValidator;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;
import com.sonatype.insight.brain.scan.ScanResult;
import com.sonatype.insight.brain.scan.Scanner;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.scan.model.ItemContentType;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.dropwizard.lifecycle.Managed;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * @since 1.75
 */
@Named
@Singleton
public class ApiThirdPartyScanService
    implements Managed
{
  private final CycloneDxSchemaValidator schemaValidator;

  private final Scanner scanner;

  private final ProprietaryConfigService proprietaryConfigService;

  private final InsightWork work;

  private final ErrorResponseGenerator errorResponseGenerator;

  private final ExecutorService executor = Executors.newFixedThreadPool(4, createThreadFactory());

  private static final Logger LOG = LoggerFactory.getLogger(ApiThirdPartyScanService.class);

  @Inject
  public ApiThirdPartyScanService(
      final CycloneDxSchemaValidator schemaValidator,
      final Scanner scanner,
      final ProprietaryConfigService proprietaryConfigService,
      final InsightWork work,
      final ErrorResponseGenerator errorResponseGenerator)
  {
    this.schemaValidator = schemaValidator;
    this.scanner = scanner;
    this.proprietaryConfigService = proprietaryConfigService;
    this.work = work;
    this.errorResponseGenerator = errorResponseGenerator;
  }

  @Override
  public void start() throws Exception {
  }

  @Override
  public void stop() throws Exception {
    executor.shutdown();
  }

  @Authorize(permission = Permission.READ)
  public ApiThirdPartyScanTicketDTO scanComponents(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId,
      final String source,
      final String stageId,
      final String sbom)
  {
    validateRequest(sbom);
    
    String scanRequestId = UUID.randomUUID().toString().replace("-", "");
    ApiThirdPartyScanTicketDTO scanTicketDTO = createScanTicket(applicationId, scanRequestId);
    AuditData.get().continueAsync(new ThirdPartyComponentScanTask(sbom, applicationId, stageId, scanRequestId),
        executor::submit);
    return scanTicketDTO;
  }

  private void validateRequest(final String sbom) {
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
    // TODO: It'll return 404 until is implemented
    throw new NotFoundException(String.format("Report with status id %s for application with id %s was not found.",
        scanRequestId, applicationId));
  }

  private ThreadFactory createThreadFactory() {
    return new ThreadFactoryBuilder().setNameFormat("ApiThirdPartyScanService-%d").build();
  }

  class ThirdPartyComponentScanTask
      implements Runnable
  {
    private final String sbom;

    private final String applicationId;

    private final String stageId;

    private final String scanRequestId;

    public ThirdPartyComponentScanTask(
        final String sbom,
        final String applicationId,
        final String stageId,
        final String scanRequestId)
    {
      this.sbom = sbom;
      this.applicationId = applicationId;
      this.stageId = stageId;
      this.scanRequestId = scanRequestId;
    }

    @Override
    public void run() {
      String scanId = null;
      try {
        createScanFile(sbom, applicationId);
        // TODO Hooking up into the existing CLI pipeline to scan and evaluate the sbom content
      }
      catch (Exception e) {
        LOG.error(
            "Failed to scan SBOM for app id {}, scan id {}, stageTypeId {}. The status ID of the operation is {}.",
            applicationId, scanId, stageId, scanRequestId);
        AuditData.get()
            .setException(new RuntimeException(errorResponseGenerator.mapExceptionAndLog(e).getMessageBody(), e));
      }
    }

    private ScanResult createScanFile(final String sbom, final String applicationId) {
      try {
        Application app = new ApplicationDAO().getById(applicationId);

        ProprietaryConfig proprietaryConfig =
            proprietaryConfigService.getProprietaryConfig(OwnerType.APPLICATION, app.getPublicId());
        return scanner.scanContent(sbom, work.getScanDir(app.getId()), ItemContentType.SBOM, proprietaryConfig);
      }
      catch (IOException ex) {
        LOG.error("The scan could not be performed", ex);
        throw new UncheckedIOException(ex.getMessage(), ex);
      }
    }
  }
}
