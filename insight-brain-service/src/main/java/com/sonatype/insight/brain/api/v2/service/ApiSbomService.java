/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.api.v2.ApiSbomResource;
import com.sonatype.insight.brain.api.v2.dto.ApiSbomStatusDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.SbomComponentSortableField;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.SbomVersionsApplicationSortableField;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyDependencyType;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataSummaryListDTO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.thirdpartyscans.ResolvedLicenseDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.SbomComponentDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.SbomComponentListDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationPollingResultDTO;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.sbom.datastore.SbomEntity;
import com.sonatype.insight.brain.sbom.export.SbomExportParams;
import com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportSpecification;
import com.sonatype.insight.brain.sbom.export.SbomExporterProvider;
import com.sonatype.insight.brain.sbom.ingestion.SbomScanEvaluator;
import com.sonatype.insight.brain.sbom.license.ThirdPartyComponentLicenseResolutionService;
import com.sonatype.insight.brain.sbom.policy.SbomPolicyService;
import com.sonatype.insight.brain.sbom.utils.SbomDetectionResult;
import com.sonatype.insight.brain.sbom.utils.SbomFileDetector;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.thirdparty.SbomAction;
import com.sonatype.insight.brain.thirdparty.ThirdPartyPersistenceService;
import com.sonatype.insight.brain.utils.CheckedIllegalArgumentException;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.brain.utils.HttpHeaderUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.InternalServerException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.error.exception.PaymentRequiredException;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.file.ThirdPartyUtils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.cyclonedx.Version;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.BOM_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.POLICY_THREATS;

@Named
@Singleton
public class ApiSbomService
{
  private static final Logger log = LoggerFactory.getLogger(ApiSbomService.class);

  private final DateTimeFormatter dtFormatter =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").withZone(ZoneOffset.UTC);

  private static final String cannotFindVersionError = "Cannot find version %s for application with ID %s.";

  public static final String SBOM_STATE_CURRENT = "current";

  public static final String SBOM_STATE_ORIGINAL = "original";

  public static final String STATE_PARAM = "state";

  private static final List<String> EXPORT_OPTIONS_SPDX3_SOURCE =
      List.of("spdx3.0", "cyclonedx1.5", "cyclonedx1.6", "pdf");

  private static final List<String> EXPORT_OPTIONS_SPDX2_SOURCE =
      List.of("spdx2.2", "spdx2.3", "spdx3.0", "cyclonedx1.5", "cyclonedx1.6", "pdf");

  private static final List<String> EXPORT_OPTIONS_CDX_SOURCE =
      List.of("cyclonedx1.5", "cyclonedx1.6", "spdx2.2", "spdx2.3", "spdx3.0", "pdf");

  private final ThirdPartySbomMetadataDAO dao;

  private final SbomScanEvaluator sbomScanEvaluator;

  private final ApplicationDAO applicationDAO;

  private final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  private final SbomFileDetector sbomFileDetector;

  private final PolicyEvaluateService policyEvaluateService;

  private final ThirdPartyScanDAO thirdPartyScanDAO;

  private final SbomMetadataUtils sbomMetadataUtils;

  private final ProductLicense productLicense;

  private final SbomExporterProvider sbomExporterProvider;

  private final SbomPolicyService sbomPolicyService;

  private final ThirdPartyPersistenceService thirdPartyPersistenceService;

  private final ThirdPartyComponentLicenseResolutionService thirdPartyComponentLicenseResolutionService;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final LicenseDAO licenseDAO;

  private final ReportService reportService;

  @Inject
  public ApiSbomService(
      final ThirdPartySbomMetadataDAO dao,
      final SbomScanEvaluator sbomScanEvaluator,
      final ApplicationDAO applicationDAO,
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final SbomFileDetector sbomFileDetector,
      final PolicyEvaluateService policyEvaluateService,
      final ThirdPartyScanDAO thirdPartyScanDAO,
      final SbomMetadataUtils sbomMetadataUtils,
      final ProductLicense productLicense,
      final SbomExporterProvider sbomExporterProvider,
      final SbomPolicyService sbomPolicyService,
      final ThirdPartyPersistenceService thirdPartyPersistenceService,
      final ThirdPartyComponentLicenseResolutionService thirdPartyComponentLicenseResolutionService,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final LicenseDAO licenseDAO,
      final ReportService reportService)
  {
    this.dao = dao;
    this.sbomScanEvaluator = sbomScanEvaluator;
    this.applicationDAO = applicationDAO;
    this.thirdPartyFileCoordinateDAO = thirdPartyFileCoordinateDAO;
    this.sbomFileDetector = sbomFileDetector;
    this.policyEvaluateService = policyEvaluateService;
    this.thirdPartyScanDAO = thirdPartyScanDAO;
    this.sbomMetadataUtils = sbomMetadataUtils;
    this.productLicense = productLicense;
    this.sbomExporterProvider = sbomExporterProvider;
    this.sbomPolicyService = sbomPolicyService;
    this.thirdPartyPersistenceService = thirdPartyPersistenceService;
    this.thirdPartyComponentLicenseResolutionService = thirdPartyComponentLicenseResolutionService;
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.licenseDAO = licenseDAO;
    this.reportService = reportService;
  }

  @Authorize(permission = Permission.WRITE)
  public void deleteSbomVersion(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String version) throws IOException
  {
    final ThirdPartySbomMetadata thirdPartySbomMetadata = getThirdPartySbomMetadataNotNull(applicationId, version);

    AuditData.get().setSbomVersion(thirdPartySbomMetadata, SbomAction.DELETE);

    thirdPartyPersistenceService.deleteSbomMetadataAndAssociatedFiles(thirdPartySbomMetadata);
  }

  @Authorize(permission = Permission.READ)
  public Response getSbomVersion(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String version,
      String requestedSbomState,
      String targetSpecification,
      String acceptType)
  {
    if (!requestedSbomState.equals(SBOM_STATE_CURRENT) && !requestedSbomState.equals(SBOM_STATE_ORIGINAL)) {
      throw new BadRequestException("Invalid sbom state " + requestedSbomState);
    }
    if (requestedSbomState.equals(SBOM_STATE_ORIGINAL)) {
      return getOriginalSbom(applicationId, version);
    }
    targetSpecification = StringUtils.lowerCase(targetSpecification, Locale.ROOT);
    acceptType = StringUtils.lowerCase(acceptType, Locale.ROOT);
    validateRequestParams(targetSpecification, acceptType);
    return buildCurrentSbom(applicationId, version, targetSpecification, acceptType);
  }

  private Response buildCurrentSbom(
      final String applicationId,
      final String version,
      final String targetSpecification,
      final String acceptType)
  {
    final ThirdPartySbomMetadata thirdPartySbomMetadata = findSbomMetadataRecord(applicationId, version);
    ExportSpecification exportSpec = ExportSpecification.getSpecificationForRequest(targetSpecification);

    if (thirdPartySbomMetadata.getSpec().equals(SbomSpecification.CYCLONEDX.toString()) &&
        exportSpec.getSpecification().equals(SbomSpecification.CYCLONEDX))
    {
      validateCycloneDxAllowedForwardSpecVersionsOnly(thirdPartySbomMetadata, exportSpec);
    }
    else if (thirdPartySbomMetadata.getSpec().equals(SbomSpecification.SPDX.toString()) &&
        exportSpec.getSpecification().equals(SbomSpecification.SPDX))
    {
      validateSpdxAllowedForwardSpecVersionsOnly(thirdPartySbomMetadata, exportSpec);
    }

    SbomFormat sbomFormat = SbomFormat.forMimeType(acceptType);
    SbomExportParams params = SbomExportParams.newSbomExporterParams(thirdPartySbomMetadata)
        .withExportSpecification(exportSpec)
        .withTargetFormat(sbomFormat);
    return buildSbomResponse(params, applicationId, version, acceptType);
  }

  public Response buildSbomResponse(
      final SbomExportParams sbomExportParams,
      final String applicationId,
      final String sbomVersion,
      final String type)
  {
    String content = sbomExporterProvider.get(sbomExportParams).export();
    content = content != null ? content : "";
    String fileName = getExportFileName(
        applicationId,
        sbomVersion,
        sbomExportParams.getTargetFormat().toString(),
        sbomExportParams.getExportSpecification().getSpecification(),
        null);
    return Response.ok(content.getBytes(StandardCharsets.UTF_8), type)
        .header(HttpHeaders.CONTENT_DISPOSITION, HttpHeaderUtils.buildContentDispositionHeaderValue(fileName))
        .build();
  }

  private void validateCycloneDxAllowedForwardSpecVersionsOnly(
      ThirdPartySbomMetadata thirdPartySbomMetadata,
      ExportSpecification requestedSpecification)
  {
    String dbSpecVersion = thirdPartySbomMetadata.getSpecVersion();
    Version dbVersion = findCycloneDxVersionOrThrow(dbSpecVersion,
        () -> new InternalServerException("Unable to determine the original SBOM specification version"));

    String requestedVersionString = requestedSpecification.getVersion();
    Version requestedVersion = findCycloneDxVersionOrThrow(requestedVersionString,
        () -> new BadRequestException(
            String.format("requested output SBOM version %s not supported", requestedVersionString)));

    if (requestedVersion.getVersion() < dbVersion.getVersion()) {
      throw new BadRequestException("Unable to export lower SBOM specification version " + requestedVersionString +
          ". The original CycloneDX SBOM was already in version " + dbSpecVersion);
    }
  }

  private void validateSpdxAllowedForwardSpecVersionsOnly(
      ThirdPartySbomMetadata thirdPartySbomMetadata,
      ExportSpecification requestedSpecification)
  {
    String dbSpecVersion = thirdPartySbomMetadata.getSpecVersion();
    Double dbVersion = parseSpdxVersionOrThrow(dbSpecVersion,
        () -> new InternalServerException("Unable to determine the original SBOM specification version"));

    String requestedVersionString = requestedSpecification.getVersion();
    Double requestedVersion = parseSpdxVersionOrThrow(requestedVersionString,
        () -> new BadRequestException(
            String.format("requested output SBOM version %s not supported", requestedVersionString)));

    if (requestedVersion < dbVersion) {
      throw new BadRequestException("Unable to export lower SBOM specification version " + requestedVersionString +
          ". The original SPDX SBOM was already in version " + dbSpecVersion);
    }
  }

  private Version findCycloneDxVersionOrThrow(
      String versionString,
      Supplier<? extends RuntimeException> exceptionSupplier)
  {
    return Arrays.stream(Version.values())
        .filter(v -> v.getVersionString().equalsIgnoreCase(versionString))
        .findFirst()
        .orElseThrow(exceptionSupplier);
  }

  private Double parseSpdxVersionOrThrow(String version, Supplier<? extends RuntimeException> exceptionSupplier) {
    return ThirdPartyUtils.SPDX_ACCEPTED_VERSIONS.values()
        .stream()
        .filter(v -> v.equalsIgnoreCase(version))
        .findFirst()
        .map(Double::parseDouble)
        .orElseThrow(exceptionSupplier);
  }

  private void validateRequestParams(final String targetSpecification, final String acceptMediaType) {
    if (ExportSpecification.getSpecificationForRequest(targetSpecification) == null) {
      throw new BadRequestException(
          String.format("requested output specification %s not supported", targetSpecification));
    }
    if (SbomFormat.forMimeType(acceptMediaType) == null) {
      throw new BadRequestException(
          String.format("requested output format %s not supported", acceptMediaType));
    }
  }

  @Authorize(permission = Permission.READ)
  public Response getExportOptions(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String version)
  {
    ThirdPartySbomMetadata sbomMetadata = findSbomMetadataRecord(applicationId, version);
    String sourceSpec = sbomMetadata.getSpec();
    String sourceVersion = sbomMetadata.getSpecVersion();

    List<String> options;
    if (SbomSpecification.SPDX.toString().equals(sourceSpec)) {
      options = "3.0".equals(sourceVersion) ? EXPORT_OPTIONS_SPDX3_SOURCE : EXPORT_OPTIONS_SPDX2_SOURCE;
    }
    else {
      options = EXPORT_OPTIONS_CDX_SOURCE;
    }

    return Response.ok(options).build();
  }

  private Response getOriginalSbom(final String applicationId, final String version) {
    final ThirdPartySbomMetadata thirdPartySbomMetadata = findSbomMetadataRecord(applicationId, version);

    MediaType type;
    String fileName = getExportFileName(
        applicationId,
        version,
        thirdPartySbomMetadata.getSpecFormat(),
        SbomSpecification.fromValue(thirdPartySbomMetadata.getSpec()),
        thirdPartySbomMetadata.getCreatedAt());
    if (thirdPartySbomMetadata.getSpecFormat().equals(SbomFormat.JSON.toString())) {
      type = MediaType.APPLICATION_JSON_TYPE;
    }
    else {
      type = MediaType.APPLICATION_XML_TYPE;
    }

    try (InputStream sbomStream = thirdPartyPersistenceService.getSbomContentsInputStream(thirdPartySbomMetadata)) {
      return Response.ok(IOUtils.toByteArray(sbomStream), type)
          .header(HttpHeaders.CONTENT_DISPOSITION, HttpHeaderUtils.buildContentDispositionHeaderValue(fileName))
          .build();
    }
    catch (IOException e) {
      log.debug("File not found for sbom metadata with application id {}, version {}, filename {}", applicationId,
          version, thirdPartySbomMetadata.getFilename(), e);
      throw new InternalServerException(
          String.format("Internal server error trying to retrieve the original sbom for application %s version %s",
              applicationId, version));
    }
  }

  private String getExportFileName(
      final String applicationId,
      final String version,
      final String targetFormat,
      final SbomSpecification exportSpecification,
      final Date importDate)
  {
    Application application = applicationDAO.getById(applicationId);

    return String.format("%s%s_%s_%s.%s.%s",
        importDate != null ? "Original_" : "",
        application.getPublicId(),
        version,
        dtFormatter.format((importDate != null ? importDate : new Date()).toInstant()),
        (SbomSpecification.SPDX.equals(exportSpecification) ? "spdx" : "cdx"),
        targetFormat);
  }

  @NotNull
  public ThirdPartySbomMetadata findSbomMetadataRecord(final String applicationId, final String version) {
    final ThirdPartySbomMetadata thirdPartySbomMetadata =
        dao.getByApplicationIdAndSbomVersionAndStatus(applicationId, version, ThirdPartySbomMetadataStatus.ACTIVE);
    if (thirdPartySbomMetadata == null) {
      throw new NotFoundException(String.format(cannotFindVersionError, version, applicationId));
    }
    return thirdPartySbomMetadata;
  }

  @Authorize(permission = Permission.READ)
  public ThirdPartySbomMetadataSummaryListDTO getSbomMetadataSummaryForApplication(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String sortByDate,
      int pageSize,
      int page,
      SbomVersionsApplicationSortableField sortBy,
      boolean asc)
  {
    validatePagination(pageSize, page);
    return thirdPartySbomMetadataDAO.getSbomApplicationVulnerabilities(applicationId, pageSize, page,
        sortBy != null ? sortBy : SbomVersionsApplicationSortableField.IMPORT_DATE,
        sortBy != null ? asc : StringUtils.equalsIgnoreCase(sortByDate, "asc"));
  }

  @Authorize(permission = Permission.READ)
  public SbomComponentListDTO getSbomComponents(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String version,
      Set<CvssV3Severity> vulnerabilityThreatLevels,
      Set<ThirdPartyDependencyType> dependencyTypes,
      String filterText,
      SbomComponentSortableField sortBy,
      boolean asc,
      int pageSize,
      int page)
  {
    validatePagination(pageSize, page);
    ThirdPartySbomMetadata thirdPartySbomMetadata = getThirdPartySbomMetadataNotNull(applicationId, version);
    SbomComponentListDTO sbomComponentListDTO =
        thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(thirdPartySbomMetadata.getThirdPartyFileId(),
            vulnerabilityThreatLevels, dependencyTypes, filterText, sortBy, asc, pageSize, page);
    ThirdPartyScan thirdPartyScan =
        thirdPartyScanDAO.getByThirdPartyFileId(thirdPartySbomMetadata.getThirdPartyFileId());
    ReportEntry policyThreatsReportEntry;
    ReportEntry bomReportEntry;
    if (thirdPartyScan != null) {
      String scanId = thirdPartyScan.getScanId();
      policyThreatsReportEntry = reportService.processBrowseReport(applicationId, scanId, POLICY_THREATS.getName());
      bomReportEntry = reportService.processBrowseReport(applicationId, scanId, BOM_JSON.getName());
    }
    else {
      policyThreatsReportEntry = null;
      bomReportEntry = null;
    }
    sbomComponentListDTO.getResults().forEach(sbomComponentDTO -> {
      if (thirdPartyComponentLicenseResolutionService.shouldConsiderLicenseOverrides()) {
        applyLicenseOverrides(applicationId, sbomComponentDTO);
      }

      if (SystemConfigurationPropertyFeature.SBOM_POLICIES.isEnabled()) {
        try {
          PolicyThreats.Component component =
              sbomPolicyService.getPolicyViolationsByFileCoordinateIdOrHash(applicationId, version,
                  sbomComponentDTO.getComponentRef(), sbomComponentDTO.getFileCoordinateId(),
                  sbomComponentDTO.getHash(), policyThreatsReportEntry, bomReportEntry);
          if (component != null) {
            sbomComponentDTO.setPolicyViolationCount(component.activeViolations.size());
          }
          else {
            sbomComponentDTO.setPolicyViolationCount(0);
          }
        }
        catch (IOException e) {
          log.error("Policy violations report cannot be parsed", e);
          throw new InternalServerException("Policy violations report cannot be parsed", e);
        }
      }
      else {
        sbomComponentDTO.setPolicyViolationCount(null);
      }
    });
    return sbomComponentListDTO;
  }

  private void applyLicenseOverrides(final String applicationId, final SbomComponentDTO sbomComponentDTO) {
    Set<ResolvedLicenseDTO> licenses = thirdPartyComponentLicenseResolutionService.getLicenseOverrides(applicationId,
        sbomComponentDTO.getPackageUrl());
    if (CollectionUtils.isNotEmpty(licenses)) {
      licenses = licenses.stream().map(rl -> {
        // update with license short display name,if available
        License license = licenseDAO.getById(rl.licenseId());
        return new ResolvedLicenseDTO(rl.licenseId(),
            license != null ? license.getShortDisplayName() : rl.licenseId(),
            null, null, rl.overrideStatus());
      }).collect(Collectors.toSet());
      sbomComponentDTO.setLicenses(licenses);
    }
  }

  private void validatePagination(int pageSize, int page) {
    if (pageSize < 1) {
      throw new BadRequestException("pageSize must not be less than one!");
    }
    if (page < 1) {
      throw new BadRequestException("page index must not be less than one!");
    }
  }

  private ThirdPartySbomMetadata getThirdPartySbomMetadataNotNull(String applicationId, String version) {
    ThirdPartySbomMetadata thirdPartySbomMetadata = dao.getByApplicationIdAndSbomVersion(applicationId, version);
    if (thirdPartySbomMetadata == null) {
      throw new NotFoundException(String.format(cannotFindVersionError, version, applicationId));
    }
    return thirdPartySbomMetadata;
  }

  @Authorize(permission = Permission.WRITE)
  public Response importSbom(
      final @AuthzContext(Key.APPLICATION_ID) String applicationId,
      final InputStream inputStream,
      final String fileName,
      final boolean enableBinaryImport,
      final String clientUserAgent,
      final String applicationVersion,
      final boolean ignoreValidationError)
  {
    if (sbomMetadataUtils.hasMaxSbomLimitBeenReached()) {
      throw new PaymentRequiredException(
          "You have exceeded the licensed limit of " + productLicense.getMaxSboms() + " sboms.");
    }
    if (applicationVersion != null && (StringUtils.isBlank(applicationVersion) || applicationVersion.length() > 200)) {
      throw new BadRequestException("applicationVersion cannot be blank and must be between 1 and 200 characters.");
    }

    SbomEntity transientSbomEntity = null;
    try {
      transientSbomEntity = thirdPartyPersistenceService.writeToTransientStorage(inputStream, fileName);
      SbomDetectionResult sbomDetectionResult = sbomFileDetector.getSbomDetectionResult(
          transientSbomEntity.getPath(), fileName, ignoreValidationError);

      if (isSaveableSbom(sbomDetectionResult, ignoreValidationError)) {
        ThirdPartySbomMetadata sbomMetadata = thirdPartyPersistenceService.saveSbomManagerSbomOrBinary(
            transientSbomEntity,
            fileName,
            applicationId,
            applicationVersion,
            sbomDetectionResult).getLeft();

        return scanAndEvaluateSbomFile(sbomMetadata, clientUserAgent);
      }
      else if (!sbomDetectionResult.isSbom && enableBinaryImport) {
        if (SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.isEnabled()) {
          log.debug("Initiating binary SBOM import for application {}", applicationId);

          var entities = thirdPartyPersistenceService.saveSbomManagerSbomOrBinary(
              transientSbomEntity,
              fileName,
              applicationId,
              applicationVersion,
              sbomDetectionResult);

          return scanAndEvaluateBinaryFile(entities.getLeft(), entities.getRight(), clientUserAgent);
        }
        throw new BadRequestException("Importing binary files for SBOM Manager is disabled.");
      }

      throw new BadRequestException(getErrorMessage(sbomDetectionResult));
    }
    catch (IOException e) {
      throw new InternalServerException("Internal server error importing SBOM", e);
    }
    catch (CheckedIllegalArgumentException e) {
      throw new BadRequestException(e);
    }
    finally {
      try {
        if (transientSbomEntity != null) {
          thirdPartyPersistenceService.deleteSbomFromTransientStorage(transientSbomEntity);
        }
      }
      catch (IOException e) {
        log.warn("Failed to delete temporary SBOM file: {}", transientSbomEntity.getLocation(), e);
      }
    }
  }

  /**
   * @return true if an SBOM was detected and either is valid or has ignorable validation errors which the caller has
   *         opted to ignore.
   */
  private boolean isSaveableSbom(SbomDetectionResult sbomDetectionResult, boolean ignoreValidationError) {
    boolean isSbom = sbomDetectionResult.isSbom;
    boolean isValid = isSbom && sbomDetectionResult.isValid;
    boolean ignoringValidationError =
        isSbom && !isValid && ignoreValidationError && sbomDetectionResult.isValidationErrorIgnorable;

    return isSbom && (isValid || ignoringValidationError);
  }

  private String getErrorMessage(final SbomDetectionResult sbomDetectionResult) {
    List<String> messageParts = new ArrayList<>();
    messageParts.add(sbomDetectionResult.errorMessage);
    if (sbomDetectionResult.validationErrors != null) {
      messageParts.addAll(sbomDetectionResult.validationErrors);
    }
    messageParts.removeIf(Objects::isNull);
    return String.join("\n", messageParts);
  }

  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public ApiSbomStatusDTO getImportStatus(
      @AuthzContext(Key.APPLICATION_ID) String applicationId,
      String importRequestId)
  {
    PolicyEvaluationPollingResultDTO dto =
        policyEvaluateService.pollEvaluationResult(applicationDAO.getById(applicationId).getPublicId(),
            importRequestId);

    switch (dto.status) {
      case COMPLETED:
        List<ThirdPartyScan> scans =
            thirdPartyScanDAO.getByScanId(dto.scanReceipt.getScanId());

        if (scans.isEmpty() && sbomMetadataUtils.hasMaxSbomLimitBeenReached()) {
          throw new PaymentRequiredException(
              "You have exceeded the licensed limit of " + productLicense.getMaxSboms() + " sboms.");
        }

        ThirdPartySbomMetadata sbomMetadata = dao.getByThirdPartyFileId(scans.get(0).getThirdPartyFileId());

        ApiSbomStatusDTO apiSbomStatusDTO = new ApiSbomStatusDTO();
        apiSbomStatusDTO.downloadUrl = createDownloadUrl(applicationId, sbomMetadata.getSbomVersion());
        apiSbomStatusDTO.applicationId = applicationId;
        apiSbomStatusDTO.version = sbomMetadata.getSbomVersion();

        return apiSbomStatusDTO;
      case FAILED:
        return new ApiSbomStatusDTO(dto.reason);
      case PENDING:
        throw new NotFoundException("Sbom version import is still in progress");
      default:
        throw new IllegalStateException(String
            .format("Unexpected result %s with status id %s for application with id %s",
                dto.status, importRequestId, applicationId));
    }
  }

  private String createDownloadUrl(String applicationId, String sbomVersion) {
    return UriBuilder
        .fromResource(ApiSbomResource.class)
        .path(ApiSbomResource.SBOM_VERSION_PATH)
        .queryParam(STATE_PARAM, SBOM_STATE_ORIGINAL)
        .build(applicationId, sbomVersion)
        .toString();
  }

  @Authorize(permission = Permission.READ)
  public List<String> getActiveSbomVersionListByApplication(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId)
  {
    return dao.getActiveByApplicationId(applicationId)
        .stream()
        .map(ThirdPartySbomMetadata::getSbomVersion)
        .collect(Collectors.toList());
  }

  private Response scanAndEvaluateSbomFile(ThirdPartySbomMetadata sbomMetadata, String clientUserAgent) {
    ApiThirdPartyScanTicketDTO scanTicketDTO = sbomScanEvaluator.evaluateSbom(
        sbomMetadata,
        ScanTriggerType.SBOM_API,
        clientUserAgent);

    return Response.ok(Status.ACCEPTED).entity(scanTicketDTO).build();
  }

  private Response scanAndEvaluateBinaryFile(
      ThirdPartySbomMetadata sbomMetadata,
      ThirdPartyFile thirdPartyFile,
      String clientUserAgent)
  {
    ApiThirdPartyScanTicketDTO scanTicketDTO;
    try {
      scanTicketDTO = sbomScanEvaluator.evaluateBinary(
          sbomMetadata,
          thirdPartyFile,
          ScanTriggerType.SBOM_API,
          clientUserAgent);
    }
    finally {
      try {
        thirdPartyPersistenceService.deletePersistentTempBinary(sbomMetadata, thirdPartyFile);
      }
      catch (IOException e) {
        log.error("Failed to delete temporary binary file", e);
      }
    }

    return Response.ok(Status.ACCEPTED).entity(scanTicketDTO).build();
  }
}
