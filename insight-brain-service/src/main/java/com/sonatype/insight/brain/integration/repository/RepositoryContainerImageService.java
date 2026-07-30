/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationSummary;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.clm.dto.model.repository.container.image.FirewallContainerImageEvaluationResponse;
import com.sonatype.insight.brain.api.v2.dto.ApiVerifyOrCreateApplicationForContainerImageFirewallDTO;
import com.sonatype.insight.brain.container.images.ContainerImageUtils;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.integration.ApplicationForContainerImageFirewallService;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationPollingResultDTO;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.scan.ScanContext;
import com.sonatype.insight.brain.scan.ScanResult;
import com.sonatype.insight.brain.scan.Scanner;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.InternalServerException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.application.ScannerDriver;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.scan.model.ItemContentType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Component.Type;
import org.cyclonedx.model.Property;
import org.cyclonedx.parsers.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.clm.dto.model.repository.container.image.FirewallContainerImageUtils.SONATYPE_NEXUS_REPOSITORY_BASE_URL_PROPERTY_NAME;
import static com.sonatype.clm.dto.model.repository.container.image.FirewallContainerImageUtils.SONATYPE_NEXUS_REPOSITORY_WITH_QUARANTINE_PROPERTY_NAME;

@Named
@Singleton
public class RepositoryContainerImageService
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryContainerImageService.class);

  private final RepositoryDAO repositoryDAO;

  private final ApplicationDAO applicationDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ApplicationForContainerImageFirewallService applicationForContainerImageFirewallService;

  private final Scanner scanner;

  private final PolicyEvaluateService policyEvaluateService;

  @Inject
  public RepositoryContainerImageService(
      RepositoryDAO repositoryDAO,
      ApplicationDAO applicationDAO,
      PolicyViolationDAO policyViolationDAO,
      PolicyEvaluationDAO policyEvaluationDAO,
      ApplicationForContainerImageFirewallService applicationForContainerImageFirewallService,
      Scanner scanner,
      PolicyEvaluateService policyEvaluateService)
  {
    this.repositoryDAO = repositoryDAO;
    this.applicationDAO = applicationDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.applicationForContainerImageFirewallService = applicationForContainerImageFirewallService;
    this.scanner = scanner;
    this.policyEvaluateService = policyEvaluateService;
  }

  public boolean isContainerImageQuarantined(
      String repositoryManagerInstanceId,
      String repositoryPublicId,
      String containerImagePublicId)
  {
    Repository repository = repositoryDAO
        .getByRepositoryManagerInstanceIdAndPublicIdNotNull(repositoryManagerInstanceId, repositoryPublicId);
    return isContainerImageQuarantined(repository, containerImagePublicId);
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  boolean isContainerImageQuarantined(
      @AuthzContext(Key.REPOSITORY) Repository repository,
      String containerImagePublicId)
  {
    Application application = getApplicationForContainerImage(repository, containerImagePublicId);

    log.debug("Finding out if the container image {} is in quarantine", containerImagePublicId);

    List<PolicyViolation> activeFailingViolations = policyViolationDAO
        .getActiveByOwnerIdAndStageIdAndActionId(application.getId(), Stage.ID_PROXY, Action.ID_FAIL);

    boolean isInQuarantine = !activeFailingViolations.isEmpty();

    log.debug("Found {} active policy violations failing in at the proxy stage. Quarantine result: {}",
        activeFailingViolations.size(), isInQuarantine);

    return isInQuarantine;
  }

  FirewallContainerImageEvaluationResponse evaluateContainerImage(
      String repositoryManagerInstanceId,
      String repositoryPublicId,
      String cycloneDxBomJson,
      String clientUserAgent)
  {
    Bom bom = parseContainerImageBom(cycloneDxBomJson);
    PackageUrlIdentifier containerImagePurl = getContainerImagePackageUrlIdentifier(bom);

    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO verifyOrCreateApplicationDTO =
        buildVerifyOrCreateApplicationDTO(repositoryManagerInstanceId, repositoryPublicId, bom, clientUserAgent,
            containerImagePurl);

    Repository repository = repositoryDAO
        .getByRepositoryManagerInstanceIdAndPublicIdNotNull(repositoryManagerInstanceId, repositoryPublicId);

    String applicationPublicIdForContainerImage =
        applicationForContainerImageFirewallService.verifyOrCreateApplicationForContainerImage(repository,
            verifyOrCreateApplicationDTO);

    Application application = applicationDAO.getByPublicId(applicationPublicIdForContainerImage);
    ScanResult scanResult = createScanFileForContainerImage(application, cycloneDxBomJson, containerImagePurl);

    String scanRequestId = UUID.randomUUID().toString().replace("-", "");

    log.debug("Submitting container image {} for policy evaluation with scan request ID {}",
        applicationPublicIdForContainerImage, scanRequestId);

    ScanContext scanContext = new ScanContext.Builder()
        .containerImageSbomSpecification(SbomSpecification.CYCLONEDX)
        .containerImageTelemetryMetrics(ContainerImageUtils.buildContainerImageTelemetryMetrics(bom.getMetadata()))
        .build();

    policyEvaluateService.evaluateWithPolling(scanRequestId, application, ClientScanType.SONATYPE_THIRD_PARTY,
        new Stage(Stage.ID_PROXY), ScanTriggerType.SONATYPE_CONTAINER_IMAGE_SCANNER_API, scanResult.getScanEntity(),
        ScannerDriver.THIRD_PARTY_API.getValue(), clientUserAgent, null, scanContext);

    FirewallContainerImageEvaluationResponse response = new FirewallContainerImageEvaluationResponse();
    response.setContainerImageId(application.getId());
    response.setContainerImagePublicId(application.getPublicId());
    response.setStatusId(scanRequestId);
    response.setStatusUrl(UriBuilder
        .fromResource(RepositoryResource.class)
        .path(RepositoryResource.EVALUATION_STATUS_CONTAINER_IMAGE_PATH)
        .build(applicationPublicIdForContainerImage, scanRequestId)
        .toString());

    return response;
  }

  PolicyEvaluationPollingResult pollContainerImageEvaluationResult(String containerImagePublicId, String statusId) {
    PolicyEvaluationPollingResultDTO dto = policyEvaluateService.pollEvaluationResult(containerImagePublicId, statusId);
    PolicyEvaluationPollingResult result = new PolicyEvaluationPollingResult();
    result.setStatus(dto.status);
    result.setSubStatus(dto.subStatus);
    result.setReason(dto.reason);
    result.setResult(dto.result);
    result.setScanReceipt(dto.scanReceipt);
    result.setStatusId(dto.statusId);
    result.setNextPollingIntervalInSeconds(dto.nextPollingIntervalInSeconds);
    return result;
  }

  PolicyEvaluationSummary getContainerImageReportUrl(
      String repositoryManagerInstanceId,
      String repositoryPublicId,
      String containerImagePublicId)
  {
    Repository repository = repositoryDAO
        .getByRepositoryManagerInstanceIdAndPublicIdNotNull(repositoryManagerInstanceId, repositoryPublicId);
    return getContainerImageReportUrl(repository, containerImagePublicId);
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  PolicyEvaluationSummary getContainerImageReportUrl(
      @AuthzContext(Key.REPOSITORY) Repository repository,
      String containerImageIdOrPublicId)
  {
    Application application = getApplicationForContainerImage(repository, containerImageIdOrPublicId);

    PolicyEvaluation policyEvaluation =
        policyEvaluationDAO.getLastByOwnerIdAndStageId(application.getId(), Stage.ID_PROXY);

    if (policyEvaluation == null) {
      throw new NotFoundException(
          "No policy evaluation was found for the container image with public ID " + application.getPublicId());
    }

    String reportUrl = UserInterfaceLinksHelper.getFirewallContainerImageEvaluationReportUrl(application.getPublicId(),
        policyEvaluation.getScanId());

    PolicyEvaluationSummary summary = new PolicyEvaluationSummary();
    summary.setReportUrl(reportUrl);
    return summary;
  }

  private Application getApplicationForContainerImage(Repository repository, String containerImageIdOrPublicId) {
    if (repository.getRepositoryType() != RepositoryType.proxy || !"docker".equals(repository.getFormat())) {
      throw new BadRequestException("The repository must be of type proxy and format docker");
    }

    Application application = applicationDAO.getByIdOrPublicId(containerImageIdOrPublicId);

    if (application == null || !application.getOrganizationId().equals(repository.getRelatedOrganizationId())) {
      throw new NotFoundException("No container image was found with ID " + containerImageIdOrPublicId);
    }

    return application;
  }

  private static Bom parseContainerImageBom(String cycloneDxBomJson) {
    if (StringUtils.isEmpty(cycloneDxBomJson)) {
      throw new BadRequestException("BOM for the container image to evaluate must be provided");
    }

    try {
      Bom bom = new JsonParser().parse(cycloneDxBomJson.getBytes(StandardCharsets.UTF_8));

      if (bom == null) {
        throw new BadRequestException("BOM for the container image to evaluate must be provided");
      }
      if (bom.getMetadata() == null) {
        throw new BadRequestException("BOM must contain metadata");
      }

      return bom;
    }
    catch (ParseException e) {
      throw new BadRequestException("Error parsing the provided BOM: " + e.getMessage(), e);
    }
  }

  private static PackageUrlIdentifier getContainerImagePackageUrlIdentifier(Bom bom) {
    Component containerImageComponent = bom.getMetadata().getComponent();

    if (containerImageComponent == null || containerImageComponent.getType() != Type.CONTAINER) {
      throw new BadRequestException("BOM must contain a component of type " + Type.CONTAINER + " in metadata");
    }

    String containerImagePurlString = containerImageComponent.getPurl();
    if (StringUtils.isBlank(containerImagePurlString)) {
      throw new BadRequestException("BOM must contain a purl in metadata's component");
    }

    PackageUrlIdentifier containerImagePurl = new PackageUrlIdentifier(containerImagePurlString);
    if (!ComponentIdentifier.FORMAT_CONTAINER.equals(containerImagePurl.toComponentIdentifier().getFormat())) {
      throw new BadRequestException(
          "BOM must contain a purl in metadata's component of type " + ComponentIdentifier.FORMAT_CONTAINER);
    }

    if (StringUtils.isAnyBlank(containerImagePurl.getNamespace(), containerImagePurl.getName(),
        containerImagePurl.getVersion()))
    {
      throw new BadRequestException("BOM must contain a purl in metadata's component with namespace, name and version");
    }

    return containerImagePurl;
  }

  private ApiVerifyOrCreateApplicationForContainerImageFirewallDTO buildVerifyOrCreateApplicationDTO(
      String repositoryManagerInstanceId,
      String repositoryPublicId,
      Bom bom,
      String clientUserAgent,
      PackageUrlIdentifier containerImagePurl)
  {
    String baseUrl = CollectionUtils.emptyIfNull(bom.getMetadata().getProperties())
        .stream()
        .filter(property -> SONATYPE_NEXUS_REPOSITORY_BASE_URL_PROPERTY_NAME.equals(property.getName()) &&
            StringUtils.isNotBlank(property.getValue()))
        .map(Property::getValue)
        .findFirst()
        .orElseThrow(() -> new BadRequestException(
            "BOM must contain a property " + SONATYPE_NEXUS_REPOSITORY_BASE_URL_PROPERTY_NAME));

    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto =
        new ApiVerifyOrCreateApplicationForContainerImageFirewallDTO(
            repositoryManagerInstanceId,
            repositoryPublicId,
            baseUrl,
            containerImagePurl.getNamespace(),
            containerImagePurl.getName(),
            containerImagePurl.getVersion(),
            clientUserAgent);

    // Resolve withQuarantine from BOM metadata and default to true when missing/invalid.
    boolean withQuarantine = true;
    String withQuarantineValue = CollectionUtils.emptyIfNull(bom.getMetadata().getProperties())
        .stream()
        .filter(property -> SONATYPE_NEXUS_REPOSITORY_WITH_QUARANTINE_PROPERTY_NAME.equals(property.getName()))
        .map(Property::getValue)
        .findFirst()
        .orElse(null);
    if (withQuarantineValue == null || StringUtils.isBlank(withQuarantineValue)) {
      log.warn("BOM is missing {} property or has blank value, defaulting to true",
          SONATYPE_NEXUS_REPOSITORY_WITH_QUARANTINE_PROPERTY_NAME);
    }
    else {
      if ("true".equalsIgnoreCase(withQuarantineValue) || "false".equalsIgnoreCase(withQuarantineValue)) {
        withQuarantine = Boolean.parseBoolean(withQuarantineValue);
      }
      else {
        log.debug("Ignoring non-boolean property {} with value {}",
            SONATYPE_NEXUS_REPOSITORY_WITH_QUARANTINE_PROPERTY_NAME,
            withQuarantineValue);
      }
    }

    dto.setQuarantineEnabled(withQuarantine);

    return dto;
  }

  private ScanResult createScanFileForContainerImage(
      Application application,
      String cycloneDxBomJson,
      PackageUrlIdentifier containerImagePurl)
  {
    String source = String.format("%s:%s:%s",
        containerImagePurl.getNamespace(),
        containerImagePurl.getName(),
        containerImagePurl.getVersion());

    try {
      return scanner.scanThirdPartyContent(cycloneDxBomJson, application.getId(),
          ItemContentType.CONTAINER_URI_SONATYPE, source, SbomFormat.JSON, null,
          ScannerDriver.THIRD_PARTY_API.getValue());
    }
    catch (Exception e) {
      log.error("Error scanning container image {}: {}", source, e.getMessage(), e);
      throw new InternalServerException(
          String.format("Could not process container image %s", source), e);
    }
  }
}
