/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.insight.brain.api.v2.ApiSbomResource;
import com.sonatype.insight.brain.api.v2.dto.ApiSbomStatusDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.SbomComponentSortableField;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyDependencyType;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataSummaryListDTO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.thirdpartyscans.SbomComponentListDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.sbom.export.SbomExportParams;
import com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportSpecification;
import com.sonatype.insight.brain.sbom.export.SbomExporterProvider;
import com.sonatype.insight.brain.sbom.ingestion.SbomScanEvaluator;
import com.sonatype.insight.brain.sbom.policy.SbomPolicyService;
import com.sonatype.insight.brain.sbom.utils.SbomDetectionResult;
import com.sonatype.insight.brain.sbom.utils.SbomFileDetector;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.thirdparty.SbomAction;
import com.sonatype.insight.brain.thirdparty.SbomStatus;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.brain.utils.HttpHeaderUtils;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.InternalServerException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.error.exception.PaymentRequiredException;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.file.ThirdPartyUtils;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.cyclonedx.Version;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ApiSbomService
{
  private static final Logger log = LoggerFactory.getLogger(ApiSbomService.class);

  private static final String cannotFindVersionError = "Cannot find version %s for application with ID %s.";

  public static final String SBOM_STATE_CURRENT = "current";

  public static final String SBOM_STATE_ORIGINAL = "original";

  public static final String STATE_PARAM = "state";

  public static final String SBOM_VALIDATED_HEADER = "X-SBOM-Validated";

  private final ThirdPartySbomMetadataDAO dao;

  private final ThirdPartyFileDAO thirdPartyFileDAO;

  private final SbomScanEvaluator sbomScanEvaluator;

  private final InsightWork insightWork;

  private final ApplicationDAO applicationDAO;

  private final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  private final SbomFileDetector sbomFileDetector;

  private final PolicyEvaluateService policyEvaluateService;

  private final ThirdPartyScanDAO thirdPartyScanDAO;

  private final SbomMetadataUtils sbomMetadataUtils;

  private final ProductLicense productLicense;

  private final SbomExporterProvider sbomExporterProvider;

  private final SbomPolicyService sbomPolicyService;

  @Inject
  public ApiSbomService(
      final ThirdPartySbomMetadataDAO dao,
      final ThirdPartyFileDAO thirdPartyFileDAO,
      final SbomScanEvaluator sbomScanEvaluator,
      final InsightWork insightWork,
      final ApplicationDAO applicationDAO,
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final SbomFileDetector sbomFileDetector,
      final PolicyEvaluateService policyEvaluateService,
      final ThirdPartyScanDAO thirdPartyScanDAO,
      final SbomMetadataUtils sbomMetadataUtils,
      final ProductLicense productLicense,
      final SbomExporterProvider sbomExporterProvider,
      final SbomPolicyService sbomPolicyService)
  {
    this.dao = dao;
    this.thirdPartyFileDAO = thirdPartyFileDAO;
    this.sbomScanEvaluator = sbomScanEvaluator;
    this.insightWork = insightWork;
    this.applicationDAO = applicationDAO;
    this.thirdPartyFileCoordinateDAO = thirdPartyFileCoordinateDAO;
    this.sbomFileDetector = sbomFileDetector;
    this.policyEvaluateService = policyEvaluateService;
    this.thirdPartyScanDAO = thirdPartyScanDAO;
    this.sbomMetadataUtils = sbomMetadataUtils;
    this.productLicense = productLicense;
    this.sbomExporterProvider = sbomExporterProvider;
    this.sbomPolicyService = sbomPolicyService;
  }

  @Authorize(permission = Permission.WRITE)
  public void deleteSbomVersion(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String version) throws IOException
  {
    final ThirdPartySbomMetadata thirdPartySbomMetadata = getThirdPartySbomMetadataNotNull(applicationId, version);

    AuditData.get().setSbomVersion(thirdPartySbomMetadata, SbomAction.DELETE);

    try (TransactionContext tx = thirdPartyFileDAO.createTransactionContext()) {
      tx.begin();
      thirdPartyFileDAO.delete(tx, thirdPartySbomMetadata.getThirdPartyFileId());
      Files.delete(new File(insightWork.getSbomDir(applicationId), thirdPartySbomMetadata.getFilename()).toPath());
      tx.commit();
    }
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
        exportSpec.getSpecification().equals(SbomSpecification.CYCLONEDX)) {
      validateCycloneDxAllowedForwardSpecVersionsOnly(thirdPartySbomMetadata, exportSpec);
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
    boolean validity = validateAndLogAnyErrors(
        content,
        applicationId,
        sbomVersion,
        sbomExportParams.getExportSpecification(),
        sbomExportParams.getTargetFormat()
    );
    content = content != null ? content : "";
    String fileName = getExportFileName(
        applicationId,
        sbomVersion,
        sbomExportParams.getTargetFormat().toString()
    );
    return Response.ok(content.getBytes(StandardCharsets.UTF_8), type)
        .header(SBOM_VALIDATED_HEADER, String.valueOf(validity))
        .header(HttpHeaders.CONTENT_DISPOSITION, HttpHeaderUtils.buildContentDispositionHeaderValue(fileName))
        .build();
  }

  private void validateCycloneDxAllowedForwardSpecVersionsOnly(
       ThirdPartySbomMetadata thirdPartySbomMetadata,
       ExportSpecification requestedSpecification)
  {
    String dbSpecVersion = thirdPartySbomMetadata.getSpecVersion();
    Version dbVersion = Arrays.stream(Version.values()).filter(v -> v.getVersionString()
        .equalsIgnoreCase(dbSpecVersion)).findFirst().orElseThrow(
            () -> new InternalServerException("Unable to determine the original SBOM specification version"));
    String requestedVersionString = requestedSpecification.getVersion();
    Version requestedVersion = Arrays.stream(Version.values()).filter(v -> v.getVersionString()
        .equalsIgnoreCase(requestedVersionString)).findFirst().orElseThrow(() -> new BadRequestException(
            String.format("requested output SBOM version %s not supported", requestedVersionString)));

    if (requestedVersion.getVersion() < dbVersion.getVersion()) {
      throw new BadRequestException("Unable to export lower SBOM specification version" + requestedVersionString +
          ". The original SBOM was already in version " + dbSpecVersion);
    }
  }

  @VisibleForTesting
  boolean validateAndLogAnyErrors(
      String content,
      final String applicationId,
      final String version,
      ExportSpecification exportSpec, SbomFormat sbomFormat)
  {
    try {
      if (SbomSpecification.CYCLONEDX.equals(exportSpec.getSpecification())) {
        ThirdPartyUtils.parseAndValidateCycloneDx(content, sbomFormat);
      }
      else if (SbomSpecification.SPDX.equals(exportSpec.getSpecification())) {
        ThirdPartyUtils.parseAndValidateSpdx(content, sbomFormat);
      }
      return true;
    }
    catch (Exception e) {
      log.debug("Invalid SBOM generated for application {}, version {}, spec {}, format {}", applicationId, version,
          exportSpec.getSpecification(), sbomFormat, e);
      return false;
    }
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

  private Response getOriginalSbom(final String applicationId, final String version) {
    final ThirdPartySbomMetadata thirdPartySbomMetadata = findSbomMetadataRecord(applicationId, version);

    MediaType type;
    String fileName = getExportFileName(applicationId, version, thirdPartySbomMetadata.getSpecFormat());
    if (thirdPartySbomMetadata.getSpecFormat().equals(SbomFormat.JSON.toString())) {
      type = MediaType.APPLICATION_JSON_TYPE;
    }
    else {
      type = MediaType.APPLICATION_XML_TYPE;
    }

    File sbomDir = insightWork.getSbomDir(applicationId);

    try (FileInputStream fileInputStream = new FileInputStream(
        new File(sbomDir, thirdPartySbomMetadata.getFilename()))) {
      GzipCompressorInputStream gzipInputStream = new GzipCompressorInputStream(fileInputStream);
      return Response.ok(IOUtils.toByteArray(gzipInputStream), type)
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

  @NotNull
  private String getExportFileName(
      final String applicationId,
      final String version,
      final String targetFormat)
  {
    return applicationDAO.getById(applicationId).getName() + "_" + version + "." + targetFormat;
  }

  @NotNull
  public ThirdPartySbomMetadata findSbomMetadataRecord(final String applicationId, final String version) {
    final ThirdPartySbomMetadata thirdPartySbomMetadata =
        dao.getByApplicationIdAndSbomVersionAndStatus(applicationId, version, SbomStatus.ACTIVE.name());
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
      int page)
  {
    validatePagination(pageSize, page);
    return thirdPartyFileCoordinateDAO.getSbomApplicationVulnerabilities(applicationId, sortByDate, pageSize, page);
  }

  @Authorize(permission = Permission.READ)
  public SbomComponentListDTO getSbomComponents(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String version,
      Set<CvssV3Severity> vulnerabilityThreatLevels,
      Set<ThirdPartyDependencyType> dependencyTypes,
      String componentName,
      SbomComponentSortableField sortBy,
      boolean asc,
      int pageSize,
      int page)
  {
    validatePagination(pageSize, page);
    ThirdPartySbomMetadata thirdPartySbomMetadata = getThirdPartySbomMetadataNotNull(applicationId, version);
    SbomComponentListDTO sbomComponentListDTO =
        thirdPartyFileCoordinateDAO.getSbomComponentsByThirdPartyFileId(thirdPartySbomMetadata.getThirdPartyFileId(),
        vulnerabilityThreatLevels, dependencyTypes, componentName, sortBy, asc, pageSize, page);
    sbomComponentListDTO.getResults().forEach(sbomComponentDTO -> {
      if (SystemConfigurationPropertyFeature.SBOM_POLICIES.isEnabled()) {
        try {
          PolicyThreats.Component component =
              sbomPolicyService.getPolicyViolationsByFileCoordinateId(applicationId, version,
                  sbomComponentDTO.getFileCoordinateId());
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
      final String applicationVersion)
  {
    if (sbomMetadataUtils.hasMaxSbomLimitBeenReached()) {
      throw new PaymentRequiredException(
          "You have exceeded the licensed limit of " + productLicense.getMaxSboms() + " sboms.");
    }
    if (applicationVersion != null && (StringUtils.isBlank(applicationVersion) || applicationVersion.length() > 200)) {
      throw new BadRequestException("applicationVersion cannot be blank and must be between 1 and 200 characters.");
    }
    File clientFile = saveInputStreamAsFile(inputStream, fileName);
    SbomDetectionResult sbomDetectionResult = sbomFileDetector.getSbomDetectionResult(clientFile);
    if (sbomDetectionResult.isSbom) {
      return scanAndEvaluateSbomFile(applicationId, clientFile, sbomDetectionResult, clientUserAgent,
          applicationVersion);
    }
    else if (enableBinaryImport && sbomDetectionResult.isBinary) {
      if (SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.isEnabled()) {
        log.debug("Initiating binary SBOM import for application {}", applicationId);
        return scanAndEvaluateBinaryFile(applicationId, clientUserAgent, clientFile, applicationVersion);
      }
      throw new BadRequestException("Importing binary files for SBOM Manager is disabled.");
    }
    throw new BadRequestException(sbomDetectionResult.errorMessage);
  }

  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public ApiSbomStatusDTO getImportStatus(
      @AuthzContext(Key.APPLICATION_ID) String applicationId, String importRequestId)
  {
    PolicyEvaluationPollingResult policyEvaluationPollingResult =
        policyEvaluateService.pollEvaluationResult(applicationDAO.getById(applicationId).getPublicId(),
            importRequestId);

    switch (policyEvaluationPollingResult.getStatus()) {
      case COMPLETED:
        List<ThirdPartyScan> scans =
            thirdPartyScanDAO.getByScanId(policyEvaluationPollingResult.getScanReceipt().getScanId());

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
        return new ApiSbomStatusDTO(policyEvaluationPollingResult.getReason());
      case PENDING:
        throw new NotFoundException("Sbom version import is still in progress");
      default:
        throw new IllegalStateException(String
            .format("Unexpected result %s with status id %s for application with id %s",
                policyEvaluationPollingResult.getStatus(), importRequestId, applicationId));
    }
  }

  private String createDownloadUrl(String applicationId, String sbomVersion) {
    return UriBuilder
        .fromResource(ApiSbomResource.class)
        .path(ApiSbomResource.SBOM_VERSION_PATH)
        .queryParam(STATE_PARAM, SBOM_STATE_ORIGINAL)
        .build(applicationId, sbomVersion).toString();
  }

  @Authorize(permission = Permission.READ)
  public List<String> getActiveSbomVersionListByApplication(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId)
  {
    return dao.getActiveByApplicationId(applicationId).stream()
        .map(ThirdPartySbomMetadata::getSbomVersion)
        .collect(Collectors.toList());
  }

  private Response scanAndEvaluateSbomFile(
      String applicationId,
      File sbomFile,
      SbomDetectionResult sbomDetectionResult,
      String clientUserAgent,
      String applicationVersion)
  {
    ApiThirdPartyScanTicketDTO scanTicketDTO =
        sbomScanEvaluator.evaluateSbom(applicationId, sbomFile, SbomFormat.forMimeType(sbomDetectionResult.mimeType),
            sbomMetadataUtils.determineItemContentType(sbomDetectionResult.summary.specification),
            ScanTriggerType.SBOM_API, clientUserAgent, applicationVersion);
    return Response.ok(Status.ACCEPTED)
        .entity(scanTicketDTO)
        .build();
  }

  private File saveInputStreamAsFile(InputStream inputStream, String fileName) {
    try (InputStream ignored = inputStream) {
      File clientFile = new File(Files.createTempDirectory(null).toFile(), fileName);
      log.debug("Saving file to {}", clientFile);
      try {
        Files.copy(inputStream, clientFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        log.debug("Saved client file at {}", clientFile.getPath());
        return clientFile;
      }
      catch (IOException e) {
        FileUtils.deleteDirectory(clientFile.getParentFile());
        log.debug("Failed to store client file");
        throw e;
      }
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Response scanAndEvaluateBinaryFile(
      String applicationId,
      String clientUserAgent,
      File clientFile,
      String applicationVersion)
  {
    ApiThirdPartyScanTicketDTO scanTicketDTO = sbomScanEvaluator.evaluateBinary(applicationId, clientFile,
        ScanTriggerType.SBOM_API, clientUserAgent, applicationVersion);
    return Response.ok(Status.ACCEPTED)
        .entity(scanTicketDTO)
        .build();
  }
}
