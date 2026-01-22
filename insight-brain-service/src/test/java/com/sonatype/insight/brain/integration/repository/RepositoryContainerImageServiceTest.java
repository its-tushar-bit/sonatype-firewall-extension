/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.container.image.ContainerImageTelemetryMetrics;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationSummary;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.clm.dto.model.repository.container.image.FirewallContainerImageEvaluationResponse;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationPollingResultDTO;
import com.sonatype.insight.brain.scan.ScanContext;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.google.inject.Binder;
import com.google.inject.matcher.Matchers;
import org.cyclonedx.Version;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Component.Type;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Property;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static com.sonatype.clm.dto.model.container.image.ContainerImageTelemetryMetrics.BASE_OS_PROPERTY_NAME;
import static com.sonatype.clm.dto.model.container.image.ContainerImageTelemetryMetrics.COMPONENTS_COUNT_PROPERTY_NAME;
import static com.sonatype.clm.dto.model.container.image.ContainerImageTelemetryMetrics.MANIFEST_TYPE_PROPERTY_NAME;
import static com.sonatype.clm.dto.model.container.image.ContainerImageTelemetryMetrics.SCAN_DURATION_MILLISECONDS_PROPERTY_NAME;
import static com.sonatype.clm.dto.model.repository.container.image.FirewallContainerImageUtils.SONATYPE_NEXUS_REPOSITORY_BASE_URL_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RepositoryContainerImageServiceTest
    extends AbstractComponentTest
{
  private static final String CONTAINER_IMAGE_PURL = PackageUrlIdentifier.toPackageUrl(
      ComponentIdentifier.createContainerCoordinates("test-namespace", "test-image", "1.0.0"));

  @Inject
  private RepositoryDAO repositoryDAO;

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Inject
  private ApplicationDAO applicationDAO;

  @Mock
  private PolicyEvaluateService policyEvaluateServiceMock;

  @Captor
  private ArgumentCaptor<ScanContext> scanContextCaptor;

  @Inject
  private RepositoryContainerImageService service;

  @Override
  public void configure(final Binder binder) {
    super.configure(binder);

    binder.bindInterceptor(Matchers.subclassesOf(PolicyEvaluateService.class), Matchers.any(), invocation -> {
      String methodName = invocation.getMethod().getName();
      if (methodName.equals("pollEvaluationResult") || methodName.equals("evaluateWithPolling")) {
        return invocation.getMethod().invoke(policyEvaluateServiceMock, invocation.getArguments());
      }
      return invocation.proceed();
    });
  }

  @Test
  public void testIsContainerImageQuarantined_repositoryManagerNotExists() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.isContainerImageQuarantined("fake-repo-manager", "fake-repo", "fake-image"));
  }

  @Test
  public void testIsContainerImageQuarantined_repositoryNotExists() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> service.isContainerImageQuarantined(repositoryManager.getInstanceId(), "fake-repo", "fake-image"));
  }

  @Test
  public void testIsContainerImageQuarantined_repositoryNotProxy() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.hosted, "docker");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> service
        .isContainerImageQuarantined(repositoryManager.getInstanceId(), repository.getName(), "fake-image"))
        .withMessage("The repository must be of type proxy and format docker");
  }

  @Test
  public void testIsContainerImageQuarantined_repositoryNotDocker() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "maven2");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> service
        .isContainerImageQuarantined(repositoryManager.getInstanceId(), repository.getName(), "fake-image"))
        .withMessage("The repository must be of type proxy and format docker");
  }

  @Test
  public void testIsContainerImageQuarantined_containerImageNotExists() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> service
        .isContainerImageQuarantined(repositoryManager.getInstanceId(), repository.getName(), "fake-image"))
        .withMessage("No container image was found with ID fake-image");
  }

  @Test
  public void testIsContainerImageQuarantined_containerImageNotFromSameRepository() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");
    Application application = tempEntity.newApplicationWithParent();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.isContainerImageQuarantined(repositoryManager.getInstanceId(), repository.getName(),
            application.getPublicId()))
        .withMessage("No container image was found with ID " + application.getPublicId());
  }

  @Test
  public void testIsContainerImageQuarantined_notInQuarantineNoPolicyViolations() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");
    Application application = tempEntity.newApplicationWithParent();
    repository.setRelatedOrganizationId(application.getOrganizationId());
    repositoryDAO.update(repository);

    boolean result = service.isContainerImageQuarantined(repositoryManager.getInstanceId(), repository.getName(),
        application.getPublicId());

    assertThat(result).isFalse();
  }

  @Test
  public void testIsContainerImageQuarantined_notInQuarantineNoPolicyViolationsFailingAtProxy() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");
    Application application = tempEntity.newApplicationWithParent();
    repository.setRelatedOrganizationId(application.getOrganizationId());
    repositoryDAO.update(repository);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_PROXY, "scanId");
    Policy policy = tempEntity.newPolicy(application.getOrganizationId());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    policyViolation.setActionTypeId(Action.ID_WARN);
    policyViolationDAO.update(policyViolation);

    boolean result = service.isContainerImageQuarantined(repositoryManager.getInstanceId(), repository.getName(),
        application.getPublicId());

    assertThat(result).isFalse();
  }

  @Test
  public void testIsContainerImageQuarantined_inQuarantine() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");
    Application application = tempEntity.newApplicationWithParent();
    repository.setRelatedOrganizationId(application.getOrganizationId());
    repositoryDAO.update(repository);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_PROXY, "scanId");
    Policy policy = tempEntity.newPolicy(application.getOrganizationId());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    policyViolation.setActionTypeId(Action.ID_FAIL);
    policyViolationDAO.update(policyViolation);

    boolean result = service.isContainerImageQuarantined(repositoryManager.getInstanceId(), repository.getName(),
        application.getPublicId());

    assertThat(result).isTrue();
  }

  @Test
  public void testEvaluateContainerImage_nullBom() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManagerWithBaseUrl("https://nexus.example.com");
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.evaluateContainerImage(repositoryManager.getInstanceId(), repository.getPublicId(),
            null, null))
        .withMessage("BOM for the container image to evaluate must be provided");
  }

  @Test
  public void testEvaluateContainerImage_emptyBom() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManagerWithBaseUrl("https://nexus.example.com");
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.evaluateContainerImage(repositoryManager.getInstanceId(), repository.getPublicId(),
            "", null))
        .withMessage("BOM for the container image to evaluate must be provided");
  }

  @Test
  public void testEvaluateContainerImage_invalidJson() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManagerWithBaseUrl("https://nexus.example.com");
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.evaluateContainerImage(repositoryManager.getInstanceId(), repository.getPublicId(),
            "invalid json", null))
        .withMessageStartingWith("Error parsing the provided BOM:");
  }

  @Test
  public void testEvaluateContainerImage_noMetadata() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManagerWithBaseUrl("https://nexus.example.com");
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.evaluateContainerImage(repositoryManager.getInstanceId(), repository.getPublicId(),
            toJson(createBomWithNoMetadata()), null))
        .withMessage("BOM must contain metadata");
  }

  @Test
  public void testEvaluateContainerImage_noComponent() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManagerWithBaseUrl("https://nexus.example.com");
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.evaluateContainerImage(repositoryManager.getInstanceId(), repository.getPublicId(),
            toJson(createBomWithNoComponent()), null))
        .withMessage("BOM must contain a component of type " + Type.CONTAINER + " in metadata");
  }

  @Test
  public void testEvaluateContainerImage_wrongComponentType() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManagerWithBaseUrl("https://nexus.example.com");
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.evaluateContainerImage(repositoryManager.getInstanceId(), repository.getPublicId(),
            toJson(createBomWithWrongComponentType()), null))
        .withMessage("BOM must contain a component of type " + Type.CONTAINER + " in metadata");
  }

  @Test
  public void testEvaluateContainerImage_noPurl() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManagerWithBaseUrl("https://nexus.example.com");
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.evaluateContainerImage(repositoryManager.getInstanceId(), repository.getPublicId(),
            toJson(createBomWithNoPurl()), null))
        .withMessage("BOM must contain a purl in metadata's component");
  }

  @Test
  public void testEvaluateContainerImage_invalidPurlFormat() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManagerWithBaseUrl("https://nexus.example.com");
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.evaluateContainerImage(repositoryManager.getInstanceId(), repository.getPublicId(),
            toJson(createBomWithInvalidPurlFormat()), null))
        .withMessage("BOM must contain a purl in metadata's component of type container");
  }

  @Test
  public void testEvaluateContainerImage_noBaseUrl() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManagerWithBaseUrl("https://nexus.example.com");
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.evaluateContainerImage(repositoryManager.getInstanceId(), repository.getPublicId(),
            toJson(createBomWithNoBaseUrl()), null))
        .withMessage("BOM must contain a property " + SONATYPE_NEXUS_REPOSITORY_BASE_URL_PROPERTY_NAME);
  }

  @Test
  public void testEvaluateContainerImage_repositoryManagerNotExists() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.evaluateContainerImage("fake-repo-manager", "fake-repo",
            toJson(createValidBom()), null))
        .withMessageContaining("Cannot find a repository with repositoryManagerInstanceId=fake-repo-manager" +
            " and publicId=fake-repo.");
  }

  @Test
  public void testEvaluateContainerImage_repositoryNotExists() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManagerWithBaseUrl("https://nexus.example.com");

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.evaluateContainerImage(repositoryManager.getInstanceId(), "fake-repo",
            toJson(createValidBom()), null))
        .withMessageContaining("Cannot find a repository");
  }

  @Test
  public void testEvaluateContainerImage_success() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManagerWithBaseUrl("https://nexus.example.com");
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-proxy", RepositoryType.proxy, "docker");
    String clientUserAgent = "Nexus/3.50.0";

    FirewallContainerImageEvaluationResponse response = service.evaluateContainerImage(
        repositoryManager.getInstanceId(),
        repository.getPublicId(),
        toJson(createValidBom(true)),
        clientUserAgent);

    assertThat(response).isNotNull();
    assertThat(response.getStatusId()).isNotBlank();
    assertThat(response.getStatusUrl()).isNotBlank();
    assertThat(response.getStatusUrl()).contains(response.getContainerImagePublicId());
    assertThat(response.getStatusUrl()).contains(response.getStatusId());

    // Verify application was created
    Application application = applicationDAO.getByPublicId(response.getContainerImagePublicId());
    assertThat(application).isNotNull();
    assertThat(application.getId()).isEqualTo(response.getContainerImageId());
    assertThat(application.getPublicId()).isEqualTo(response.getContainerImagePublicId());

    // Verify ContainerImageTelemetryMetrics were captured and passed to policy evaluation
    verify(policyEvaluateServiceMock).evaluateWithPolling(anyString(), any(), any(), any(), any(), any(), anyString(),
        eq(clientUserAgent), isNull(), scanContextCaptor.capture());

    ScanContext capturedScanContext = scanContextCaptor.getValue();
    assertThat(capturedScanContext).isNotNull();
    assertThat(capturedScanContext.containerImageTelemetryMetrics()).isNotNull();

    ContainerImageTelemetryMetrics telemetryMetrics = capturedScanContext.containerImageTelemetryMetrics();
    assertThat(telemetryMetrics.getBaseOs()).isEqualTo("alpine:3.18");
    assertThat(telemetryMetrics.getComponentsCount()).isEqualTo(150L);
    assertThat(telemetryMetrics.getManifestMediaType()).isEqualTo("test-media-type");
    assertThat(telemetryMetrics.getScanDurationMilliseconds()).isEqualTo(5000L);
  }

  @Test
  public void testPollContainerImageEvaluationResult() {
    String containerImagePublicId = "containerImagePublicId";
    String statusId = "statusId";
    PolicyEvaluationPollingResultDTO expectedDto = new PolicyEvaluationPollingResultDTO();
    expectedDto.status = PolicyEvaluationStatus.COMPLETED;

    when(policyEvaluateServiceMock.pollEvaluationResult(containerImagePublicId, statusId)).thenReturn(expectedDto);

    PolicyEvaluationPollingResult result =
        service.pollContainerImageEvaluationResult(containerImagePublicId, statusId);

    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo(expectedDto.status);
    verify(policyEvaluateServiceMock).pollEvaluationResult(containerImagePublicId, statusId);
  }

  @Test
  public void testGetContainerImageReportUrl_repositoryManagerNotExists() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.getContainerImageReportUrl("fake-repo-manager", "fake-repo", "fake-image"));
  }

  @Test
  public void testGetContainerImageReportUrl_repositoryNotExists() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> service.getContainerImageReportUrl(repositoryManager.getInstanceId(), "fake-repo", "fake-image"));
  }

  @Test
  public void testGetContainerImageReportUrl_repositoryNotProxy() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.hosted, "docker");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> service
            .getContainerImageReportUrl(repositoryManager.getInstanceId(), repository.getPublicId(), "fake-image"))
        .withMessage("The repository must be of type proxy and format docker");
  }

  @Test
  public void testGetContainerImageReportUrl_repositoryNotDocker() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "maven2");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> service
            .getContainerImageReportUrl(repositoryManager.getInstanceId(), repository.getPublicId(), "fake-image"))
        .withMessage("The repository must be of type proxy and format docker");
  }

  @Test
  public void testGetContainerImageReportUrl_containerImageNotExists() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> service
            .getContainerImageReportUrl(repositoryManager.getInstanceId(), repository.getPublicId(), "fake-image"))
        .withMessage("No container image was found with ID fake-image");
  }

  @Test
  public void testGetContainerImageReportUrl_containerImageNotFromSameOrganization() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");
    Application application = tempEntity.newApplicationWithParent();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.getContainerImageReportUrl(repositoryManager.getInstanceId(),
            repository.getPublicId(), application.getPublicId()))
        .withMessage("No container image was found with ID " + application.getPublicId());
  }

  @Test
  public void testGetContainerImageReportUrl_noPolicyEvaluation() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");
    Application application = tempEntity.newApplicationWithParent();
    repository.setRelatedOrganizationId(application.getOrganizationId());
    repositoryDAO.update(repository);

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.getContainerImageReportUrl(repositoryManager.getInstanceId(),
            repository.getPublicId(), application.getPublicId()))
        .withMessage(
            "No policy evaluation was found for the container image with public ID " + application.getPublicId());
  }

  @Test
  public void testGetContainerImageReportUrl_successWithPublicId() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");
    Application application = tempEntity.newApplicationWithParent();
    repository.setRelatedOrganizationId(application.getOrganizationId());
    repositoryDAO.update(repository);

    String scanId = "scan-123";
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_PROXY, scanId);

    PolicyEvaluationSummary summary = service.getContainerImageReportUrl(repositoryManager.getInstanceId(),
        repository.getPublicId(), application.getPublicId());

    assertThat(summary).isNotNull();
    assertThat(summary.getReportUrl()).isNotNull();
    assertThat(summary.getReportUrl()).contains(application.getPublicId());
    assertThat(summary.getReportUrl()).contains(scanId);
  }

  @Test
  public void testGetContainerImageReportUrl_successWithId() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");
    Application application = tempEntity.newApplicationWithParent();
    repository.setRelatedOrganizationId(application.getOrganizationId());
    repositoryDAO.update(repository);

    String scanId = "scan-123";
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_PROXY, scanId);

    PolicyEvaluationSummary summary = service.getContainerImageReportUrl(repositoryManager.getInstanceId(),
        repository.getPublicId(), application.getId());

    assertThat(summary).isNotNull();
    assertThat(summary.getReportUrl()).isNotNull();
    assertThat(summary.getReportUrl()).contains(application.getPublicId());
    assertThat(summary.getReportUrl()).contains(scanId);
  }

  private static String toJson(Bom bom) {
    try {
      return new BomJsonGenerator(bom, Version.VERSION_16).toJsonString();
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to generate BOM JSON", e);
    }
  }

  private static Bom createValidBom() {
    return createValidBom(false);
  }

  private static Bom createValidBom(boolean includeTelemetry) {
    Bom bom = new Bom();
    Metadata metadata = new Metadata();

    Component component = new Component();
    component.setType(Component.Type.CONTAINER);
    component.setPurl(CONTAINER_IMAGE_PURL);

    metadata.setComponent(component);

    List<Property> properties = new ArrayList<>();
    Property baseUrlProperty = new Property();
    baseUrlProperty.setName(SONATYPE_NEXUS_REPOSITORY_BASE_URL_PROPERTY_NAME);
    baseUrlProperty.setValue("https://nexus.example.com");
    properties.add(baseUrlProperty);

    if (includeTelemetry) {
      Property baseOsProperty = new Property();
      baseOsProperty.setName(BASE_OS_PROPERTY_NAME);
      baseOsProperty.setValue("alpine:3.18");
      properties.add(baseOsProperty);

      Property componentsCountProperty = new Property();
      componentsCountProperty.setName(COMPONENTS_COUNT_PROPERTY_NAME);
      componentsCountProperty.setValue("150");
      properties.add(componentsCountProperty);

      Property manifestTypeProperty = new Property();
      manifestTypeProperty.setName(MANIFEST_TYPE_PROPERTY_NAME);
      manifestTypeProperty.setValue("test-media-type");
      properties.add(manifestTypeProperty);

      Property scanDurationProperty = new Property();
      scanDurationProperty.setName(SCAN_DURATION_MILLISECONDS_PROPERTY_NAME);
      scanDurationProperty.setValue("5000");
      properties.add(scanDurationProperty);
    }

    metadata.setProperties(properties);
    bom.setMetadata(metadata);
    return bom;
  }

  private static Bom createBomWithNoMetadata() {
    return new Bom();
  }

  private static Bom createBomWithNoComponent() {
    Bom bom = new Bom();
    Metadata metadata = new Metadata();

    Property property = new Property();
    property.setName(SONATYPE_NEXUS_REPOSITORY_BASE_URL_PROPERTY_NAME);
    property.setValue("https://nexus.example.com");
    metadata.setProperties(Collections.singletonList(property));

    bom.setMetadata(metadata);
    return bom;
  }

  private static Bom createBomWithWrongComponentType() {
    Bom bom = new Bom();
    Metadata metadata = new Metadata();

    Component component = new Component();
    component.setType(Component.Type.LIBRARY);
    component.setPurl(CONTAINER_IMAGE_PURL);

    metadata.setComponent(component);

    Property property = new Property();
    property.setName(SONATYPE_NEXUS_REPOSITORY_BASE_URL_PROPERTY_NAME);
    property.setValue("https://nexus.example.com");
    metadata.setProperties(Collections.singletonList(property));

    bom.setMetadata(metadata);
    return bom;
  }

  private static Bom createBomWithNoPurl() {
    Bom bom = new Bom();
    Metadata metadata = new Metadata();

    Component component = new Component();
    component.setType(Component.Type.CONTAINER);
    component.setName("alpine");
    component.setVersion("3.18.0");

    metadata.setComponent(component);

    Property property = new Property();
    property.setName(SONATYPE_NEXUS_REPOSITORY_BASE_URL_PROPERTY_NAME);
    property.setValue("https://nexus.example.com");
    metadata.setProperties(Collections.singletonList(property));

    bom.setMetadata(metadata);
    return bom;
  }

  private static Bom createBomWithInvalidPurlFormat() {
    Bom bom = new Bom();
    Metadata metadata = new Metadata();

    Component component = new Component();
    component.setType(Component.Type.CONTAINER);
    component.setPurl("pkg:npm/alpine@3.18.0");

    metadata.setComponent(component);

    Property property = new Property();
    property.setName(SONATYPE_NEXUS_REPOSITORY_BASE_URL_PROPERTY_NAME);
    property.setValue("https://nexus.example.com");
    metadata.setProperties(Collections.singletonList(property));

    bom.setMetadata(metadata);
    return bom;
  }

  private static Bom createBomWithNoBaseUrl() {
    Bom bom = new Bom();
    Metadata metadata = new Metadata();

    Component component = new Component();
    component.setType(Component.Type.CONTAINER);
    component.setPurl(CONTAINER_IMAGE_PURL);

    metadata.setComponent(component);
    bom.setMetadata(metadata);
    return bom;
  }
}
