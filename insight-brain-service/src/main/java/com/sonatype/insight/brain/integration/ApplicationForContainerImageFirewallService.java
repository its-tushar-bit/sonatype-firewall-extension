/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.api.v2.dto.ApiVerifyOrCreateApplicationForContainerImageFirewallDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryContainerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.integration.repository.RepositoryService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.telemetry.SonatypeUserAgentUtil;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ApplicationForContainerImageFirewallService
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationForContainerImageFirewallService.class);

  static final String ORGANIZATION_NAME_FIREWALL_FOR_DOCKER = "Firewall for Docker";

  private final RepositoryManagerDAO repositoryManagerDAO;

  private final RepositoryDAO repositoryDAO;

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final RepositoryContainerDAO repositoryContainerDAO;

  private final MembershipMappingDAO membershipMappingDAO;

  private final RepositoryService repositoryService;

  @Inject
  public ApplicationForContainerImageFirewallService(
      RepositoryManagerDAO repositoryManagerDAO,
      RepositoryDAO repositoryDAO,
      ApplicationDAO applicationDAO,
      OrganizationDAO organizationDAO,
      RepositoryContainerDAO repositoryContainerDAO,
      MembershipMappingDAO membershipMappingDAO,
      RepositoryService repositoryService)
  {
    this.repositoryManagerDAO = repositoryManagerDAO;
    this.repositoryDAO = repositoryDAO;
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.repositoryContainerDAO = repositoryContainerDAO;
    this.membershipMappingDAO = membershipMappingDAO;
    this.repositoryService = repositoryService;
  }

  /**
   * Verifies or creates an application for a Container Image being evaluated in Firewall.
   *
   * @param dto the request DTO
   * @return the application public ID that exists or was created for the container image
   */
  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  public String verifyOrCreateApplicationForContainerImage(
      @AuthzContext(Key.REPOSITORY) Repository repository,
      ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto)
  {
    validate(dto);

    if (repository == null || repository.getRepositoryType() != RepositoryType.proxy
        || !StringUtils.equalsAny(repository.getFormat(), "docker", null))
    {
      throw new BadRequestException("Repository must be of type proxy and format docker");
    }

    try (TransactionContext tx = applicationDAO.createTransactionContext()) {
      tx.begin();

      RepositoryManager repositoryManager =
          repositoryManagerDAO.getByInstanceIdNotNull(tx, dto.getRepositoryManagerInstanceId());

      if (!repositoryManager.getId().equals(repository.getRepositoryManagerId())) {
        throw new BadRequestException("Repository " + repository.getId() + " not part of repository manager instance "
            + repositoryManager.getInstanceId());
      }

      boolean withQuarantine = dto.getQuarantineEnabled() == null || dto.getQuarantineEnabled();
      repositoryService.configureAuditAndQuarantineInRepository(tx, repositoryManager.getInstanceId(), repository,
          "docker", withQuarantine, true);

      String applicationPublicId = NameHelper.convertContainerImageToApplicationPublicIdAndName(
          dto.getBaseUrl(),
          dto.getRepositoryPublicId(),
          dto.getContainerImageNamespace(),
          dto.getContainerImageName(),
          dto.getContainerImageVersion());

      Application application = getExistingApplication(tx, repository, applicationPublicId);

      if (application == null) {
        application = createApplicationHierarchy(tx, repositoryManager, repository, applicationPublicId);
      }

      boolean needToUpdateRepositoryManager = false;

      if (!dto.getBaseUrl().equals(repositoryManager.getBaseUrl())) {
        repositoryManager.setBaseUrl(dto.getBaseUrl());
        needToUpdateRepositoryManager = true;
      }
      if (StringUtils.isNotBlank(dto.getClientUserAgent())
          && !dto.getClientUserAgent().equals(repositoryManager.getUserAgent())
          && SonatypeUserAgentUtil.parse(dto.getClientUserAgent()) != null)
      {
        repositoryManager.setUserAgent(dto.getClientUserAgent());
        needToUpdateRepositoryManager = true;
      }
      if (needToUpdateRepositoryManager) {
        repositoryManagerDAO.update(tx, repositoryManager);
      }

      tx.commit();
      return application.getPublicId();
    }
  }

  private void validate(ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto) {
    if (dto == null) {
      throw new BadRequestException("No parameters were send.");
    }
    if (StringUtils.isBlank(dto.getRepositoryManagerInstanceId())) {
      throw new BadRequestException("Repository Manager Instance ID is required.");
    }
    if (StringUtils.isBlank(dto.getRepositoryPublicId())) {
      throw new BadRequestException("Repository Public ID is required.");
    }
    if (StringUtils.isBlank(dto.getBaseUrl())) {
      throw new BadRequestException("Repository Manager Base URL is required.");
    }
    if (StringUtils.isBlank(dto.getContainerImageNamespace())) {
      throw new BadRequestException("Container Image Namespace is required.");
    }
    if (StringUtils.isBlank(dto.getContainerImageName())) {
      throw new BadRequestException("Container Image Name is required.");
    }
    if (StringUtils.isBlank(dto.getContainerImageVersion())) {
      throw new BadRequestException("Container Image Version is required.");
    }
  }

  private Application getExistingApplication(TransactionContext tx, Repository repository, String applicationPublicId) {
    Application application = applicationDAO.getByPublicId(tx, applicationPublicId);

    if (application != null) {
      Organization organization = organizationDAO.getByIdNotNull(tx, application.getParentOwnerId());

      if (!repository.getId().equals(organization.getRelatedRepositoryId())) {
        throw new ConflictException(
            "Repository " + repository.getId() + " with invalid configuration for container images");
      }

      return application;
    }

    return null;
  }

  private Application createApplicationHierarchy(
      TransactionContext tx,
      RepositoryManager repositoryManager,
      Repository repository,
      String applicationPublicId)
  {
    Triple<Organization, Organization, Organization> existingOrganizations =
        getExistingOrganizations(tx, repositoryManager, repository);
    Organization organizationForRepositoryContainer = existingOrganizations.getLeft();
    Organization organizationForRepositoryManager = existingOrganizations.getMiddle();
    Organization organizationForRepository = existingOrganizations.getRight();

    // Create Organizations not found above

    if (organizationForRepositoryContainer == null) {
      String organizationForRepositoryContainerName = ORGANIZATION_NAME_FIREWALL_FOR_DOCKER;

      while (organizationDAO.getByName(tx, organizationForRepositoryContainerName) != null) {
        organizationForRepositoryContainerName += "-" + RandomStringUtils.insecure().nextAlphanumeric(8);
      }

      organizationForRepositoryContainer = new Organization();
      organizationForRepositoryContainer.setName(organizationForRepositoryContainerName);
      organizationForRepositoryContainer.setParentOrganizationId(Organization.ROOT_ORGANIZATION_ID);
      organizationForRepositoryContainer.setRelatedRepositoryContainerId(RepositoryContainer.REPOSITORY_CONTAINER_ID);
      organizationDAO.insert(tx, organizationForRepositoryContainer);

      repositoryContainerDAO.setRelatedOrganizationIdNotNull(tx, organizationForRepositoryContainer.getId());

      copyMembershipMapping(tx, RepositoryContainer.REPOSITORY_CONTAINER_ID,
          organizationForRepositoryContainer.getId());

      log.info("Repository container configured for container images: {}", organizationForRepositoryContainer.getId());
    }

    if (organizationForRepositoryManager == null) {
      organizationForRepositoryManager = new Organization();
      organizationForRepositoryManager.setName(repositoryManager.getId());
      organizationForRepositoryManager.setParentOrganizationId(organizationForRepositoryContainer.getId());
      organizationForRepositoryManager.setRelatedRepositoryManagerId(repositoryManager.getId());
      organizationDAO.insert(tx, organizationForRepositoryManager);

      repositoryManager.setRelatedOrganizationId(organizationForRepositoryManager.getId());
      repositoryManagerDAO.update(tx, repositoryManager);

      copyMembershipMapping(tx, repositoryManager.getId(), organizationForRepositoryManager.getId());

      log.info("Repository manager {} configured for container images: {}", repositoryManager.getId(),
          organizationForRepositoryManager.getId());
    }

    if (organizationForRepository == null) {
      organizationForRepository = new Organization();
      organizationForRepository.setName(repository.getId());
      organizationForRepository.setParentOrganizationId(organizationForRepositoryManager.getId());
      organizationForRepository.setRelatedRepositoryId(repository.getId());
      organizationDAO.insert(tx, organizationForRepository);

      repository.setRelatedOrganizationId(organizationForRepository.getId());
      repositoryDAO.update(tx, repository);

      copyMembershipMapping(tx, repository.getId(), organizationForRepository.getId());

      log.info("Repository {} configured for container images: {}", repository.getId(),
          organizationForRepository.getId());
    }

    Application application = new Application();
    application.setPublicId(applicationPublicId);
    application.setName(applicationPublicId);
    application.setOrganizationId(organizationForRepository.getId());
    applicationDAO.insert(tx, application);

    log.info("Container image configured: {}", applicationPublicId);

    return application;
  }

  private Triple<Organization, Organization, Organization> getExistingOrganizations(
      TransactionContext tx,
      RepositoryManager repositoryManager,
      Repository repository)
  {
    Organization organizationForRepository = null;
    Organization organizationForRepositoryManager = null;
    Organization organizationForRepositoryContainer = null;
    String organizationIdForRepositoryContainer = repositoryContainerDAO.getRelatedOrganizationId(tx);

    // Check if an Organization exists for the Repository
    if (repository.getRelatedOrganizationId() != null) {
      organizationForRepository = organizationDAO.getByIdNotNull(tx, repository.getRelatedOrganizationId());

      organizationForRepositoryManager =
          organizationDAO.getByIdNotNull(tx, organizationForRepository.getParentOrganizationId());

      if (organizationForRepositoryManager.getRelatedRepositoryManagerId() == null
          || !organizationForRepositoryManager.getRelatedRepositoryManagerId().equals(repositoryManager.getId()))
      {
        throw new ConflictException("Repository " + repository.getId()
            + " with invalid configuration for container images: " + repository.getRelatedOrganizationId());
      }
    }

    // Check if an Organization exists for the Repository Manager
    if (organizationForRepositoryManager == null && repositoryManager.getRelatedOrganizationId() != null) {
      organizationForRepositoryManager =
          organizationDAO.getByIdNotNull(tx, repositoryManager.getRelatedOrganizationId());
    }

    if (organizationForRepositoryManager != null) {
      if (!repositoryManager.getId().equals(organizationForRepositoryManager.getRelatedRepositoryManagerId())
          || repositoryManager.getRelatedOrganizationId() == null
          || !repositoryManager.getRelatedOrganizationId().equals(organizationForRepositoryManager.getId())
          || !StringUtils.equals(organizationForRepositoryManager.getParentOrganizationId(),
              organizationIdForRepositoryContainer))
      {
        throw new ConflictException("Repository manager instance " + repositoryManager.getInstanceId()
            + " with invalid configuration for container images: " + repositoryManager.getRelatedOrganizationId());
      }

      organizationForRepositoryContainer =
          organizationDAO.getByIdNotNull(tx, organizationForRepositoryManager.getParentOrganizationId());
    }

    // Check if an Organization exists for the Repository Container
    if (organizationForRepositoryContainer == null && organizationIdForRepositoryContainer != null) {
      organizationForRepositoryContainer = organizationDAO.getByIdNotNull(tx, organizationIdForRepositoryContainer);
    }

    return Triple.of(organizationForRepositoryContainer, organizationForRepositoryManager, organizationForRepository);
  }

  private void copyMembershipMapping(TransactionContext tx, String originContextId, String targetContextId) {
    membershipMappingDAO.getByContextId(tx, originContextId).forEach(membership -> {
      MembershipMapping newMembership = new MembershipMapping(
          targetContextId, membership.getRoleId(), membership.getMemberName(), membership.getMemberType());
      membershipMappingDAO.insert(tx, newMembership);
    });
  }
}
