/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.api.v2.dto.ApiVerifyOrCreateApplicationForContainerImageFirewallDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryContainerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.db.IdUtil;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.integration.ApplicationForContainerImageFirewallService.ORGANIZATION_NAME_FIREWALL_FOR_DOCKER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ComponentH2Test
public class ApplicationForContainerImageFirewallServiceTest
    extends AbstractComponentH2Test
{
  @Inject
  private ApplicationForContainerImageFirewallService service;

  @Inject
  private RepositoryContainerDAO repositoryContainerDAO;

  @Inject
  private RepositoryManagerDAO repositoryManagerDAO;

  @Inject
  private RepositoryDAO repositoryDAO;

  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private MembershipMappingDAO membershipMappingDAO;

  @Test
  public void testVerifyOrCreateApplicationForContainerImage_missingParameters() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.verifyOrCreateApplicationForContainerImage(null, null))
        .withMessage("No parameters were send.");

    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto =
        new ApiVerifyOrCreateApplicationForContainerImageFirewallDTO();

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.verifyOrCreateApplicationForContainerImage(null, dto))
        .withMessage("Repository Manager Instance ID is required.");

    dto.setRepositoryManagerInstanceId("repository-manager-id");
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.verifyOrCreateApplicationForContainerImage(null, dto))
        .withMessage("Repository Public ID is required.");

    dto.setRepositoryPublicId("repository-public-id");
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.verifyOrCreateApplicationForContainerImage(null, dto))
        .withMessage("Repository Manager Base URL is required.");

    dto.setBaseUrl("repository-base-url");
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.verifyOrCreateApplicationForContainerImage(null, dto))
        .withMessage("Container Image Namespace is required.");

    dto.setContainerImageNamespace("container-image-namespace");
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.verifyOrCreateApplicationForContainerImage(null, dto))
        .withMessage("Container Image Name is required.");

    dto.setContainerImageName("container-image-name");
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.verifyOrCreateApplicationForContainerImage(null, dto))
        .withMessage("Container Image Version is required.");

    dto.setContainerImageVersion("container-image-version");
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.verifyOrCreateApplicationForContainerImage(null, dto))
        .withMessage("Repository must be of type proxy and format docker");
  }

  @Test
  public void testVerifyOrCreateApplicationForContainerImage_invalidRepository() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManagerWithBaseUrl("base-url");
    Repository hostedRepository =
        tempEntity.newRepository(repositoryManager, "hosted-docker-repository", RepositoryType.hosted, "docker");

    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto =
        new ApiVerifyOrCreateApplicationForContainerImageFirewallDTO(
            repositoryManager.getInstanceId(),
            hostedRepository.getPublicId(),
            repositoryManager.getBaseUrl(),
            "containerImageNamespace",
            "containerImageName",
            "containerImageVersion");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.verifyOrCreateApplicationForContainerImage(hostedRepository, dto))
        .withMessage("Repository must be of type proxy and format docker");

    Repository proxyRepository =
        tempEntity.newRepository(repositoryManager, "proxy-maven-repository", RepositoryType.proxy, "maven2");
    dto.setRepositoryPublicId(proxyRepository.getPublicId());

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.verifyOrCreateApplicationForContainerImage(hostedRepository, dto))
        .withMessage("Repository must be of type proxy and format docker");
  }

  @Test
  public void testVerifyOrCreateApplicationForContainerImage_repositoryManagerNotExists() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManagerWithBaseUrl("base-url");
    Repository repository =
        tempEntity.newRepository(repositoryManager, "proxy-docker-repository", RepositoryType.proxy, "docker");

    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto =
        new ApiVerifyOrCreateApplicationForContainerImageFirewallDTO(
            "fake-repository-manager-id",
            repository.getPublicId(),
            repositoryManager.getBaseUrl(),
            "containerImageNamespace",
            "containerImageName",
            "containerImageVersion");

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.verifyOrCreateApplicationForContainerImage(repository, dto))
        .withMessage("Cannot find a repository manager with instance ID fake-repository-manager-id.");
  }

  @Test
  public void testVerifyOrCreateApplicationForContainerImage_repositoryUnderDifferentRepositoryManager() {
    RepositoryManager repositoryManager1 = tempEntity.newRepositoryManagerWithBaseUrl("base-url-1");
    RepositoryManager repositoryManager2 = tempEntity.newRepositoryManagerWithBaseUrl("base-url-2");
    Repository repository =
        tempEntity.newRepository(repositoryManager1, "proxy-docker-repository", RepositoryType.proxy, "docker");

    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto =
        new ApiVerifyOrCreateApplicationForContainerImageFirewallDTO(
            repositoryManager2.getInstanceId(),
            repository.getPublicId(),
            repositoryManager2.getBaseUrl(),
            "containerImageNamespace",
            "containerImageName",
            "containerImageVersion");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.verifyOrCreateApplicationForContainerImage(repository, dto))
        .withMessage("Repository " + repository.getId() + " not part of repository manager instance "
            + repositoryManager2.getInstanceId());
  }

  @Test
  public void testVerifyOrCreateApplicationForContainerImage_applicationExistsButWrongOrganizationAndRepository() {
    HierarchyHolder hierarchyHolder = new HierarchyHolder();

    hierarchyHolder.organizationForRepository.setRelatedRepositoryId(tempEntity.newRepository().getId());
    organizationDAO.update(hierarchyHolder.organizationForRepository);

    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto = hierarchyHolder.toDto();

    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(() -> service.verifyOrCreateApplicationForContainerImage(hierarchyHolder.repository, dto))
        .withMessage(
            "Repository " + hierarchyHolder.repository.getId() + " with invalid configuration for container images");
  }

  @Test
  public void testVerifyOrCreateApplicationForContainerImage_applicationExists() {
    HierarchyHolder hierarchyHolder = new HierarchyHolder();
    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto = hierarchyHolder.toDto();

    String result = service.verifyOrCreateApplicationForContainerImage(hierarchyHolder.repository, dto);
    assertThat(result).isEqualTo(hierarchyHolder.application.getPublicId());
  }

  @Test
  public void testVerifyOrCreateApplicationForContainerImage_applicationNotExistsAndRepoWithWrongRelatedOrgId() {
    HierarchyHolder hierarchyHolder = new HierarchyHolder();
    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto = hierarchyHolder.toDto();

    applicationDAO.delete(hierarchyHolder.application);
    hierarchyHolder.repository.setRelatedOrganizationId(tempEntity.newOrganization().getId());
    repositoryDAO.update(hierarchyHolder.repository);

    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(() -> service.verifyOrCreateApplicationForContainerImage(hierarchyHolder.repository, dto))
        .withMessage(
            "Repository " + hierarchyHolder.repository.getId() + " with invalid configuration for container images: "
                + hierarchyHolder.repository.getRelatedOrganizationId());
  }

  @Test
  public void testVerifyOrCreateApplicationForContainerImage_appNotExistsAndRepoManagerWithWrongRelatedOrgId() {
    HierarchyHolder hierarchyHolder = new HierarchyHolder();
    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto = hierarchyHolder.toDto();

    applicationDAO.delete(hierarchyHolder.application);
    hierarchyHolder.repositoryManager.setRelatedOrganizationId(tempEntity.newOrganization().getId());
    repositoryManagerDAO.update(hierarchyHolder.repositoryManager);

    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(() -> service.verifyOrCreateApplicationForContainerImage(hierarchyHolder.repository, dto))
        .withMessage("Repository manager instance " + hierarchyHolder.repositoryManager.getInstanceId()
            + " with invalid configuration for container images: "
            + hierarchyHolder.repositoryManager.getRelatedOrganizationId());
  }

  @Test
  public void testVerifyOrCreateApplicationForContainerImage_appNotExistsAndOrgForRepoManagerWithWrongParentId() {
    HierarchyHolder hierarchyHolder = new HierarchyHolder();
    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto = hierarchyHolder.toDto();

    applicationDAO.delete(hierarchyHolder.application);
    hierarchyHolder.organizationForRepoManager.setParentOrganizationId(tempEntity.newOrganization().getId());
    organizationDAO.update(hierarchyHolder.organizationForRepoManager);

    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(() -> service.verifyOrCreateApplicationForContainerImage(hierarchyHolder.repository, dto))
        .withMessage("Repository manager instance " + hierarchyHolder.repositoryManager.getInstanceId()
            + " with invalid configuration for container images: "
            + hierarchyHolder.repositoryManager.getRelatedOrganizationId());
  }

  @Test
  public void testVerifyOrCreateApplicationForContainerImage_ApplicationAndOrganizationsNotExists() {
    String newBaseUrl = "https://repo-test.sonatype.com/" + IdUtil.newUUID();
    RepositoryManager repositoryManager = tempEntity.newRepositoryManagerWithBaseUrl("base-url");
    Repository repository =
        tempEntity.newRepository(repositoryManager, "proxy-docker-repository", RepositoryType.proxy, "docker");

    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto =
        new ApiVerifyOrCreateApplicationForContainerImageFirewallDTO(
            repositoryManager.getInstanceId(),
            repository.getPublicId(),
            newBaseUrl,
            "containerImageNamespace",
            "containerImageName",
            "containerImageVersion");

    String result = service.verifyOrCreateApplicationForContainerImage(repository, dto);
    assertHierarchy(result, repositoryManager, repository, newBaseUrl);
  }

  @Test
  public void testVerifyOrCreateApplicationForContainerImage_OrganizationForRepositoryContainerExistsButNotRelated() {
    String newBaseUrl = "https://repo-test.sonatype.com/" + IdUtil.newUUID();
    RepositoryManager repositoryManager = tempEntity.newRepositoryManagerWithBaseUrl("base-url");
    Repository repository =
        tempEntity.newRepository(repositoryManager, "proxy-docker-repository", RepositoryType.proxy, "docker");

    tempEntity.newOrganization(ORGANIZATION_NAME_FIREWALL_FOR_DOCKER);

    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto =
        new ApiVerifyOrCreateApplicationForContainerImageFirewallDTO(
            repositoryManager.getInstanceId(),
            repository.getPublicId(),
            newBaseUrl,
            "containerImageNamespace",
            "containerImageName",
            "containerImageVersion");

    String result = service.verifyOrCreateApplicationForContainerImage(repository, dto);
    assertHierarchy(result, repositoryManager, repository, newBaseUrl);
  }

  @Test
  public void testVerifyOrCreateApplicationForContainerImage_OnlyOrganizationForRepositoryContainerExists() {
    String newBaseUrl = "https://repo-test.sonatype.com/?id=" + IdUtil.newUUID();
    RepositoryManager repositoryManager = tempEntity.newRepositoryManagerWithBaseUrl("base-url");
    Repository repository =
        tempEntity.newRepository(repositoryManager, "proxy-docker-repository", RepositoryType.proxy, "docker");

    Organization organizationForRepositoryContainer = tempEntity.newOrganization();
    organizationForRepositoryContainer.setName(ORGANIZATION_NAME_FIREWALL_FOR_DOCKER);
    organizationForRepositoryContainer.setRelatedRepositoryContainerId(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    organizationDAO.update(organizationForRepositoryContainer);
    repositoryContainerDAO.setRelatedOrganizationIdNotNull(organizationForRepositoryContainer.getId());

    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto =
        new ApiVerifyOrCreateApplicationForContainerImageFirewallDTO(
            repositoryManager.getInstanceId(),
            repository.getPublicId(),
            newBaseUrl,
            "containerImageNamespace",
            "containerImageName",
            "containerImageVersion");

    String result = service.verifyOrCreateApplicationForContainerImage(repository, dto);
    assertHierarchy(result, repositoryManager, repository, newBaseUrl);
  }

  @Test
  public void testVerifyOrCreateApplicationForContainerImage_OnlyOrganizationForRepositoryContainerAndManagerExist() {
    String newBaseUrl = "https://repo-test.sonatype.com#" + IdUtil.newUUID();
    RepositoryManager repositoryManager = tempEntity.newRepositoryManagerWithBaseUrl("base-url");
    Repository repository =
        tempEntity.newRepository(repositoryManager, "proxy-docker-repository", RepositoryType.proxy, "docker");

    Organization organizationForRepositoryContainer = tempEntity.newOrganization();
    organizationForRepositoryContainer.setName(ORGANIZATION_NAME_FIREWALL_FOR_DOCKER);
    organizationForRepositoryContainer.setRelatedRepositoryContainerId(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    organizationDAO.update(organizationForRepositoryContainer);
    repositoryContainerDAO.setRelatedOrganizationIdNotNull(organizationForRepositoryContainer.getId());

    Organization organizationForRepositoryManager = tempEntity.newOrganization(organizationForRepositoryContainer);
    organizationForRepositoryManager.setName(repositoryManager.getId());
    organizationForRepositoryManager.setRelatedRepositoryManagerId(repositoryManager.getId());
    organizationDAO.update(organizationForRepositoryManager);
    repositoryManager.setRelatedOrganizationId(organizationForRepositoryManager.getId());
    repositoryManagerDAO.update(repositoryManager);

    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto =
        new ApiVerifyOrCreateApplicationForContainerImageFirewallDTO(repositoryManager.getInstanceId(),
            repository.getPublicId(), newBaseUrl, "containerImageNamespace", "containerImageName",
            "containerImageVersion");

    String result = service.verifyOrCreateApplicationForContainerImage(repository, dto);
    assertHierarchy(result, repositoryManager, repository, newBaseUrl);
  }

  @Test
  public void testVerifyOrCreateApplicationForContainerImage_allOrganizationsExistButNoApplication() {
    HierarchyHolder hierarchyHolder = new HierarchyHolder();
    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto = hierarchyHolder.toDto();

    applicationDAO.delete(hierarchyHolder.application);

    String result = service.verifyOrCreateApplicationForContainerImage(hierarchyHolder.repository, dto);
    assertHierarchy(result, hierarchyHolder.repositoryManager, hierarchyHolder.repository, "base-url");
  }

  @Test
  public void testVerifyOrCreateApplicationForContainerImage_allOrganizationsExistButRepositoryNotConfigured() {
    HierarchyHolder hierarchyHolder = new HierarchyHolder();
    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto = hierarchyHolder.toDto();

    hierarchyHolder.repository.setAuditEnabled(false);
    hierarchyHolder.repository.setQuarantineEnabled(false);
    hierarchyHolder.repository.setFormat(null);
    applicationDAO.delete(hierarchyHolder.application);

    String result = service.verifyOrCreateApplicationForContainerImage(hierarchyHolder.repository, dto);
    assertHierarchy(result, hierarchyHolder.repositoryManager, hierarchyHolder.repository, "base-url");

    Repository repository = repositoryDAO.getById(hierarchyHolder.repository.getId());
    assertThat(repository.isAuditEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isTrue();
    assertThat(repository.getFormat()).isEqualTo("docker");
  }

  @Test
  public void testVerifyOrCreateApplicationForContainerImage_auditAndQuarantineEnabledByDefault() {
    // When withQuarantine is absent, default should be true.
    RepositoryManager repositoryManager = tempEntity.newRepositoryManagerWithBaseUrl("base-url");
    Repository repository =
        tempEntity.newRepository(repositoryManager, "proxy-docker-repository", RepositoryType.proxy, "docker");

    // Ensure repository starts with both audit and quarantine disabled
    repository.setAuditEnabled(false);
    repository.setQuarantineEnabled(false);
    repositoryDAO.update(repository);

    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto =
        new ApiVerifyOrCreateApplicationForContainerImageFirewallDTO(
            repositoryManager.getInstanceId(),
            repository.getPublicId(),
            repositoryManager.getBaseUrl(),
            "containerImageNamespace",
            "containerImageName",
            "containerImageVersion");

    String result = service.verifyOrCreateApplicationForContainerImage(repository, dto);
    assertThat(result).isNotBlank();

    Repository updatedRepository = repositoryDAO.getById(repository.getId());
    assertThat(updatedRepository.isAuditEnabled()).isTrue();
    assertThat(updatedRepository.isQuarantineEnabled()).isTrue();
  }

  @Test
  public void testVerifyOrCreateApplicationForContainerImage_quarantineEnabledWhenProvided() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManagerWithBaseUrl("base-url");
    Repository repository =
        tempEntity.newRepository(repositoryManager, "proxy-docker-repository", RepositoryType.proxy, "docker");

    repository.setAuditEnabled(false);
    repository.setQuarantineEnabled(false);
    repositoryDAO.update(repository);

    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto =
        new ApiVerifyOrCreateApplicationForContainerImageFirewallDTO(
            repositoryManager.getInstanceId(),
            repository.getPublicId(),
            repositoryManager.getBaseUrl(),
            "containerImageNamespace",
            "containerImageName",
            "containerImageVersion");
    dto.setQuarantineEnabled(true);

    String result = service.verifyOrCreateApplicationForContainerImage(repository, dto);
    assertThat(result).isNotBlank();

    Repository updatedRepository = repositoryDAO.getById(repository.getId());
    assertThat(updatedRepository.isAuditEnabled()).isTrue();
    assertThat(updatedRepository.isQuarantineEnabled()).isTrue();
  }

  @Test
  public void testVerifyOrCreateApplicationForContainerImage_quarantineDisabledWhenProvided() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManagerWithBaseUrl("base-url");
    Repository repository =
        tempEntity.newRepository(repositoryManager, "proxy-docker-repository", RepositoryType.proxy, "docker");

    repository.setAuditEnabled(false);
    repository.setQuarantineEnabled(true);
    repositoryDAO.update(repository);

    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto =
        new ApiVerifyOrCreateApplicationForContainerImageFirewallDTO(
            repositoryManager.getInstanceId(),
            repository.getPublicId(),
            repositoryManager.getBaseUrl(),
            "containerImageNamespace",
            "containerImageName",
            "containerImageVersion");
    dto.setQuarantineEnabled(false);

    String result = service.verifyOrCreateApplicationForContainerImage(repository, dto);
    assertThat(result).isNotBlank();

    Repository updatedRepository = repositoryDAO.getById(repository.getId());
    assertThat(updatedRepository.isAuditEnabled()).isTrue();
    assertThat(updatedRepository.isQuarantineEnabled()).isFalse();
  }

  @Test
  public void testVerifyOrCreateApplicationForContainerImage_withValidClientUserAgent() {
    String newBaseUrl = "https://repo-test.sonatype.com/" + IdUtil.newUUID();
    String clientUserAgent = "Nexus/3.50.0-SNAPSHOT (PRO; Mac OS X; 10.11.5; x86_64; 1.8.0_92)";
    RepositoryManager repositoryManager = tempEntity.newRepositoryManagerWithBaseUrl("base-url");
    Repository repository =
        tempEntity.newRepository(repositoryManager, "proxy-docker-repository", RepositoryType.proxy, "docker");

    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto =
        new ApiVerifyOrCreateApplicationForContainerImageFirewallDTO(
            repositoryManager.getInstanceId(),
            repository.getPublicId(),
            newBaseUrl,
            "containerImageNamespace",
            "containerImageName",
            "containerImageVersion",
            clientUserAgent);

    String result = service.verifyOrCreateApplicationForContainerImage(repository, dto);
    assertHierarchy(result, repositoryManager, repository, newBaseUrl, clientUserAgent);
  }

  @Test
  public void testVerifyOrCreateApplicationForContainerImage_withInvalidClientUserAgent() {
    String newBaseUrl = "https://repo-test.sonatype.com/" + IdUtil.newUUID();
    String invalidClientUserAgent = "invalid-user-agent";
    RepositoryManager repositoryManager = tempEntity.newRepositoryManagerWithBaseUrl("base-url");
    Repository repository =
        tempEntity.newRepository(repositoryManager, "proxy-docker-repository", RepositoryType.proxy, "docker");

    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto =
        new ApiVerifyOrCreateApplicationForContainerImageFirewallDTO(
            repositoryManager.getInstanceId(),
            repository.getPublicId(),
            newBaseUrl,
            "containerImageNamespace",
            "containerImageName",
            "containerImageVersion",
            invalidClientUserAgent);

    String result = service.verifyOrCreateApplicationForContainerImage(repository, dto);
    assertHierarchy(result, repositoryManager, repository, newBaseUrl);

    // User agent should not be set since it's invalid
    RepositoryManager updatedRepositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());
    assertThat(updatedRepositoryManager.getUserAgent()).isNull();
  }

  @Test
  public void testVerifyOrCreateApplicationForContainerImage_updateBaseUrlAndUserAgent() {
    HierarchyHolder hierarchyHolder = new HierarchyHolder();
    String newBaseUrl = "https://new-repo-url.sonatype.com/" + IdUtil.newUUID();
    String clientUserAgent = "Nexus/3.50.0-SNAPSHOT (PRO; Mac OS X; 10.11.5; x86_64; 1.8.0_92)";

    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto =
        new ApiVerifyOrCreateApplicationForContainerImageFirewallDTO(
            hierarchyHolder.repositoryManager.getInstanceId(),
            hierarchyHolder.repository.getPublicId(),
            newBaseUrl,
            hierarchyHolder.containerImageNamespace,
            hierarchyHolder.containerImageName,
            hierarchyHolder.containerImageVersion,
            clientUserAgent);

    String result = service.verifyOrCreateApplicationForContainerImage(hierarchyHolder.repository, dto);
    assertThat(result).isEqualTo(NameHelper.convertContainerImageToApplicationPublicIdAndName(newBaseUrl,
        hierarchyHolder.repository.getPublicId(), hierarchyHolder.containerImageNamespace,
        hierarchyHolder.containerImageName, hierarchyHolder.containerImageVersion));

    RepositoryManager updatedRepositoryManager =
        repositoryManagerDAO.getById(hierarchyHolder.repositoryManager.getId());
    assertThat(updatedRepositoryManager.getBaseUrl()).isEqualTo(newBaseUrl);
    assertThat(updatedRepositoryManager.getUserAgent()).isEqualTo(clientUserAgent);
  }

  private void assertHierarchy(
      String applicationPublicIdResult,
      RepositoryManager repositoryManager,
      Repository repository,
      String baseUrl)
  {
    assertHierarchy(applicationPublicIdResult, repositoryManager, repository, baseUrl, null);
  }

  private void assertHierarchy(
      String applicationPublicIdResult,
      RepositoryManager repositoryManager,
      Repository repository,
      String baseUrl,
      String userAgent)
  {
    assertThat(applicationPublicIdResult).isNotBlank();

    Application applicationResult = applicationDAO.getByPublicId(applicationPublicIdResult);
    assertThat(applicationResult).isNotNull();

    Organization organizationForRepositoryResult = organizationDAO.getById(applicationResult.getParentOwnerId());
    assertThat(organizationForRepositoryResult.getRelatedRepositoryId()).isEqualTo(repository.getId());

    Map<String, String> membershipsInOrganizationForRepositoryResult =
        membershipMappingDAO.getByContextId(organizationForRepositoryResult.getId())
            .stream()
            .collect(Collectors.toMap(MembershipMapping::getRoleId, MembershipMapping::getMemberName));

    Map<String, String> membershipsInRepository = membershipMappingDAO.getByContextId(repository.getId())
        .stream()
        .collect(Collectors.toMap(MembershipMapping::getRoleId, MembershipMapping::getMemberName));

    assertThat(membershipsInOrganizationForRepositoryResult)
        .containsExactlyInAnyOrderEntriesOf(membershipsInRepository);

    Organization organizationForRepositoryManagerResult =
        organizationDAO.getById(organizationForRepositoryResult.getParentOwnerId());
    assertThat(organizationForRepositoryManagerResult.getRelatedRepositoryManagerId())
        .isEqualTo(repositoryManager.getId());
    assertThat(organizationForRepositoryManagerResult.getName()).isEqualTo(repositoryManager.getId());

    Map<String, String> membershipsInOrganizationForRepositoryManagerResult =
        membershipMappingDAO.getByContextId(organizationForRepositoryManagerResult.getId())
            .stream()
            .collect(Collectors.toMap(MembershipMapping::getRoleId, MembershipMapping::getMemberName));

    Map<String, String> membershipsInRepositoryManager = membershipMappingDAO.getByContextId(repositoryManager.getId())
        .stream()
        .collect(Collectors.toMap(MembershipMapping::getRoleId, MembershipMapping::getMemberName));

    assertThat(membershipsInOrganizationForRepositoryManagerResult)
        .containsExactlyInAnyOrderEntriesOf(membershipsInRepositoryManager);

    Organization organizationForRepositoryContainerResult =
        organizationDAO.getById(organizationForRepositoryManagerResult.getParentOwnerId());
    assertThat(organizationForRepositoryContainerResult.getId())
        .isEqualTo(repositoryContainerDAO.getRelatedOrganizationId());
    assertThat(organizationForRepositoryContainerResult.getName()).startsWith(ORGANIZATION_NAME_FIREWALL_FOR_DOCKER);
    assertThat(organizationForRepositoryContainerResult.getRelatedRepositoryContainerId())
        .isEqualTo(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    Map<String, String> membershipsInOrganizationForRepositoryContainerResult =
        membershipMappingDAO.getByContextId(organizationForRepositoryContainerResult.getId())
            .stream()
            .collect(Collectors.toMap(MembershipMapping::getRoleId, MembershipMapping::getMemberName));

    Map<String, String> membershipsInRepositoryContainer =
        membershipMappingDAO.getByContextId(RepositoryContainer.REPOSITORY_CONTAINER_ID)
            .stream()
            .collect(Collectors.toMap(MembershipMapping::getRoleId, MembershipMapping::getMemberName));

    assertThat(membershipsInOrganizationForRepositoryContainerResult)
        .containsExactlyInAnyOrderEntriesOf(membershipsInRepositoryContainer);

    RepositoryManager updatedRepositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());
    assertThat(updatedRepositoryManager.getBaseUrl()).isEqualTo(baseUrl);
    if (userAgent != null) {
      assertThat(updatedRepositoryManager.getUserAgent()).isEqualTo(userAgent);
    }
  }

  private class HierarchyHolder
  {
    private final String containerImageNamespace;

    private final String containerImageName;

    private final String containerImageVersion;

    private final RepositoryManager repositoryManager;

    private final Repository repository;

    private final Organization organizationForRepoManager;

    private final Organization organizationForRepository;

    private final Application application;

    private HierarchyHolder() {
      this("containerImageNamespace", "containerImageName", "containerImageVersion");
    }

    private HierarchyHolder(
        String containerImageNamespace,
        String containerImageName,
        String containerImageVersion)
    {
      this.containerImageNamespace = containerImageNamespace;
      this.containerImageName = containerImageName;
      this.containerImageVersion = containerImageVersion;

      Role roleRepositoryContainer =
          tempEntity.newRole("test-repositoryContainer", false, Permission.EVALUATE_COMPONENT);
      tempEntity.newMembershipMapping(RepositoryContainer.REPOSITORY_CONTAINER_ID, roleRepositoryContainer.getId(),
          USERNAME);

      repositoryManager = tempEntity.newRepositoryManagerWithBaseUrl("base-url");

      Role roleRepositoryManager = tempEntity.newRole("test-repositoryManager", false, Permission.EVALUATE_COMPONENT);
      tempEntity.newMembershipMapping(repositoryManager.getId(), roleRepositoryManager.getId(), USERNAME);

      repository =
          tempEntity.newRepository(repositoryManager, "proxy-docker-repository", RepositoryType.proxy, "docker");

      Role roleRepository = tempEntity.newRole("test-repository", false, Permission.EVALUATE_COMPONENT);
      tempEntity.newMembershipMapping(repository.getId(), roleRepository.getId(), USERNAME);

      Organization organizationForRepoContainer = tempEntity.newOrganization(ORGANIZATION_NAME_FIREWALL_FOR_DOCKER);
      organizationForRepoContainer.setRelatedRepositoryContainerId(RepositoryContainer.REPOSITORY_CONTAINER_ID);
      organizationDAO.update(organizationForRepoContainer);
      repositoryContainerDAO.setRelatedOrganizationIdNotNull(organizationForRepoContainer.getId());

      tempEntity.newMembershipMapping(organizationForRepoContainer.getId(), roleRepositoryContainer.getId(), USERNAME);

      organizationForRepoManager = tempEntity.newOrganization(organizationForRepoContainer);
      repositoryManager.setRelatedOrganizationId(organizationForRepoManager.getId());
      repositoryManagerDAO.update(repositoryManager);
      organizationForRepoManager.setRelatedRepositoryManagerId(repositoryManager.getId());
      organizationForRepoManager.setName(repositoryManager.getId());
      organizationDAO.update(organizationForRepoManager);

      tempEntity.newMembershipMapping(organizationForRepoManager.getId(), roleRepositoryManager.getId(), USERNAME);

      organizationForRepository = tempEntity.newOrganization(organizationForRepoManager);
      repository.setRelatedOrganizationId(organizationForRepository.getId());
      repositoryDAO.update(repository);
      organizationForRepository.setRelatedRepositoryId(repository.getId());
      organizationDAO.update(organizationForRepository);

      tempEntity.newMembershipMapping(organizationForRepository.getId(), roleRepository.getId(), USERNAME);

      application = tempEntity.newApplicationWithParent(organizationForRepository);
      application.setPublicId(NameHelper.convertContainerImageToApplicationPublicIdAndName(
          repositoryManager.getBaseUrl(), repository.getPublicId(), this.containerImageNamespace,
          this.containerImageName, this.containerImageVersion));
      applicationDAO.update(application);
    }

    private ApiVerifyOrCreateApplicationForContainerImageFirewallDTO toDto() {
      return new ApiVerifyOrCreateApplicationForContainerImageFirewallDTO(
          repositoryManager.getInstanceId(),
          repository.getPublicId(),
          repositoryManager.getBaseUrl(),
          containerImageNamespace,
          containerImageName,
          containerImageVersion);
    }
  }
}
