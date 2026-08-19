/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.StatusType;

import com.sonatype.insight.brain.api.v2.dto.ApiOwnerDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiOwnerRepositoryConnectionsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryConnectionDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryConnectionStatusRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryConnectionStatusResponseDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryConnectionDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.repository.RepositoryFormat;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.repository.RepositoryClient;
import com.sonatype.insight.brain.repository.client.RepositoryClientFactory;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ApiRepositoryConnectionService
{
  private static final Logger log = LoggerFactory.getLogger(ApiRepositoryConnectionService.class);

  public static final String REPOSITORY_URL_AUDIT_KEY = "repositoryBaseUrl";

  public static final String REPOSITORY_FORMAT_AUDIT_KEY = "repositoryFormat";

  public static final String REPOSITORY_CONNECTION_NOT_FOUND_ERROR =
      "no repository connections found with connection id: %s for %s having id: %s";

  public static final String OVERRIDE_BY_CHILD_AUDIT_KEY = "overrideByChild";

  public static final String ENABLED_MODE_AUDIT_KEY = "enabledMode";

  private final OwnerDAO ownerDAO;

  private final RepositoryConnectionDAO repositoryConnectionDAO;

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final PasswordHandler passwordHandler;

  private final RepositoryClientFactory repositoryClientFactory;

  @Inject
  public ApiRepositoryConnectionService(
      final OwnerDAO ownerDAO,
      final RepositoryConnectionDAO repositoryConnectionDAO,
      final ApplicationDAO applicationDAO,
      final OrganizationDAO organizationDAO,
      final PasswordHandler passwordHandler,
      final RepositoryClientFactory repositoryClientFactory)
  {
    this.ownerDAO = ownerDAO;
    this.repositoryConnectionDAO = repositoryConnectionDAO;
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.passwordHandler = passwordHandler;
    this.repositoryClientFactory = repositoryClientFactory;
  }

  @Authorize(permission = Permission.WRITE)
  public ApiRepositoryConnectionDTO addRepositoryConnection(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String ownerId,
      ApiRepositoryConnectionDTO repositoryConnectionDTO)
  {
    validateRepositoryConnection(repositoryConnectionDTO);
    if (repositoryConnectionDTO.format == null) {
      repositoryConnectionDTO.format = RepositoryFormat.GENERIC;
    }
    AuditData.get()
        .setData(REPOSITORY_URL_AUDIT_KEY, repositoryConnectionDTO.baseUrl)
        .setData(REPOSITORY_FORMAT_AUDIT_KEY, repositoryConnectionDTO.format);
    if (repositoryConnectionDAO.getByOwnerIdAndFormat(ownerId, repositoryConnectionDTO.format) != null) {
      throw new ConflictException(
          String.format("repository connection format %s configuration exists for %s with id: %s",
              repositoryConnectionDTO.format, ownerType, ownerId));
    }

    repositoryConnectionDTO.ownerType = ownerType;
    repositoryConnectionDTO.ownerId = ownerId;
    RepositoryConnection repositoryConnection = toRepositoryConnection(repositoryConnectionDTO);
    repositoryConnectionDAO.insert(repositoryConnection);
    return toRepositoryConnectionDTO(repositoryConnection);
  }

  @Authorize(permission = Permission.WRITE)
  public ApiRepositoryConnectionDTO updateRepositoryConnection(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String internalOwnerId,
      String repositoryConnectionId,
      ApiRepositoryConnectionDTO dto)
  {
    validateUpdateConnectionData(dto);
    AuditData.get()
        .setData(REPOSITORY_URL_AUDIT_KEY, dto.baseUrl)
        .setData(REPOSITORY_FORMAT_AUDIT_KEY, dto.format);
    dto.ownerType = ownerType;
    dto.ownerId = internalOwnerId;
    RepositoryConnection storedConnection =
        repositoryConnectionDAO.getByIdAndOwnerId(repositoryConnectionId, internalOwnerId);
    if (storedConnection == null) {
      throw new NotFoundException(
          String.format(REPOSITORY_CONNECTION_NOT_FOUND_ERROR, repositoryConnectionId, ownerType, internalOwnerId));
    }
    if (StringUtils.isNotBlank(dto.baseUrl)) {
      RepositoryFormat format = dto.format == null ? storedConnection.getFormat() : dto.format;
      RepositoryConnection connection =
          repositoryConnectionDAO.getByOwnerIdAndFormat(internalOwnerId, format);
      if (connection != null && !Objects.equals(connection.getId(), repositoryConnectionId)) {
        throw new ConflictException(
            String.format("repository connection format %s configuration exists for %s with id: %s", format, ownerType,
                internalOwnerId));
      }
    }

    updateRepositoryConnectionData(dto, storedConnection);
    repositoryConnectionDAO.update(storedConnection);
    return toRepositoryConnectionDTO(storedConnection);
  }

  @Authorize(permission = Permission.WRITE)
  public void updateOwnerRepositoryConnectionStatus(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.INTERNAL_ID) String ownerId,
      ApiRepositoryConnectionStatusRequestDTO dto)
  {
    if (dto == null) {
      throw new BadRequestException("missing repository connection configuration data for update");
    }
    if (Organization.ROOT_ORGANIZATION_ID.equals(ownerId) && dto.enabled == null) {
      throw new BadRequestException("root organization cannot inherit configuration");
    }
    switch (ownerType) {
      case APPLICATION:
        Application app = applicationDAO.getByIdNotNull(ownerId);
        app.setRepositoryConnectionEnabled(dto.enabled);
        applicationDAO.update(app);
        break;
      case ORGANIZATION:
        Organization org = organizationDAO.getByIdNotNull(ownerId);
        org.setRepositoryConnectionEnabled(dto.enabled);
        org.setAllowRepositoryConnectionOverride(dto.allowOverride);
        organizationDAO.update(org);
        AuditData.get().setData(OVERRIDE_BY_CHILD_AUDIT_KEY, dto.allowOverride ? "allow" : "disallow");
        break;
      default:
        throw new IllegalStateException("Unknown owner type: " + ownerType);
    }
    AuditData.get()
        .setData(ENABLED_MODE_AUDIT_KEY, dto.enabled == null ? "inherit" : dto.enabled ? "enable" : "disable");
  }

  private void validateUpdateConnectionData(final ApiRepositoryConnectionDTO dto) {
    if (dto == null || (dto.format == null && dto.isAnonymous == null &&
        StringUtils.isAllBlank(dto.baseUrl, dto.username, dto.password)))
    {
      throw new BadRequestException("missing repository connection data for update");
    }

    if (isNotCompleteAuthData(dto)) {
      throw new BadRequestException("missing username/password for repository connection");
    }
  }

  private boolean isNotCompleteAuthData(final ApiRepositoryConnectionDTO dto) {
    return (StringUtils.isNotBlank(dto.username) && StringUtils.isBlank(dto.password)) ||
        (StringUtils.isBlank(dto.username) && StringUtils.isNotBlank(dto.password));
  }

  private void updateRepositoryConnectionData(
      ApiRepositoryConnectionDTO dto,
      RepositoryConnection storedConnection)
  {
    if (!Boolean.TRUE.equals(dto.isAnonymous) &&
        StringUtils.isNotEmpty(dto.baseUrl) &&
        !dto.baseUrl.equals(storedConnection.getBaseUrl()) &&
        StringUtils.isNotEmpty(storedConnection.getUsername()))
    {
      // the existing connection has auth config. so new auth data must be provided
      if (StringUtils.isAnyBlank(dto.username, dto.password)) {
        throw new BadRequestException("missing username/password for repository connection");
      }
    }

    if (StringUtils.isNotBlank(dto.baseUrl)) {
      storedConnection.setBaseUrl(dto.baseUrl);
    }
    if (dto.format != null) {
      storedConnection.setFormat(dto.format);
    }
    if (StringUtils.isNoneBlank(dto.username, dto.password)) {
      storedConnection.setUsername(dto.username);
      storedConnection.setPassword(passwordHandler.encryptPassword(dto.password.toCharArray()));
    }
    if (Boolean.TRUE.equals(dto.isAnonymous)) {
      storedConnection.setUsername(null);
      storedConnection.setPassword(null);
    }
  }

  @Authorize(permission = Permission.WRITE)
  public void deleteRepositoryConnection(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String internalOwnerId,
      String repositoryConnectionId)
  {
    RepositoryConnection conn = repositoryConnectionDAO.getByIdAndOwnerId(repositoryConnectionId, internalOwnerId);
    if (conn == null) {
      throw new NotFoundException(
          String.format(REPOSITORY_CONNECTION_NOT_FOUND_ERROR, repositoryConnectionId, ownerType, internalOwnerId));
    }
    repositoryConnectionDAO.delete(conn);
  }

  @Authorize(permission = Permission.READ)
  public ApiRepositoryConnectionDTO getRepositoryConnection(
      @SuppressWarnings("unused") @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String internalOwnerId,
      String repositoryConnectionId)
  {
    RepositoryConnection repositoryConnection =
        repositoryConnectionDAO.getByIdAndOwnerId(repositoryConnectionId, internalOwnerId);
    if (repositoryConnection == null) {
      throw new NotFoundException(
          String.format(REPOSITORY_CONNECTION_NOT_FOUND_ERROR, repositoryConnectionId, ownerType, internalOwnerId));
    }
    return toRepositoryConnectionDTO(repositoryConnection);
  }

  @Authorize(permission = Permission.READ)
  public ApiOwnerRepositoryConnectionsDTO getOwnerRepositoryConnections(
      @SuppressWarnings("unused") @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String internalOwnerId,
      boolean inherit)
  {
    ApiOwnerRepositoryConnectionsDTO result = new ApiOwnerRepositoryConnectionsDTO();
    result.repositoryConnectionStatus = getOwnerRepositoryConnectionStatus(ownerType, internalOwnerId);
    result.ownerDTO = ApiOwnerDTO.fromOwner(ownerDAO.getById(internalOwnerId));
    String effectiveOwnerId = resolveEffectiveOwnerId(internalOwnerId, inherit, result);
    result.repositoryConnections = repositoryConnectionDAO.getByOwnerId(effectiveOwnerId)
        .stream()
        .map(this::toRepositoryConnectionDTO)
        .collect(Collectors.toList());
    return result;
  }

  private String resolveEffectiveOwnerId(
      final String internalOwnerId,
      final boolean inherit,
      final ApiOwnerRepositoryConnectionsDTO result)
  {
    if (inherit && result.repositoryConnectionStatus.inheritedFromOrganizationId != null) {
      return result.repositoryConnectionStatus.inheritedFromOrganizationId;
    }
    return internalOwnerId;
  }

  @Authorize(permission = Permission.READ)
  public StatusType testRepositoryConnection(
      @SuppressWarnings("unused") @AuthzContext(Key.TYPE) OwnerType ownerType,
      @SuppressWarnings("unused") @AuthzContext(Key.INTERNAL_ID) String internalOwnerId,
      ApiRepositoryConnectionDTO repositoryConnectionDTO)
  {
    validateRepositoryConnection(repositoryConnectionDTO);
    return testRepositoryConnection(repositoryConnectionDTO.baseUrl, repositoryConnectionDTO.format,
        repositoryConnectionDTO.username,
        repositoryConnectionDTO.password == null ? null : repositoryConnectionDTO.password.toCharArray());
  }

  @Authorize(permission = Permission.READ)
  public StatusType testRepositoryConnection(
      @SuppressWarnings("unused") @AuthzContext(Key.TYPE) OwnerType ownerType,
      @SuppressWarnings("unused") @AuthzContext(Key.INTERNAL_ID) String internalOwnerId,
      String repositoryConnectionId)
  {
    RepositoryConnection repositoryConnection =
        repositoryConnectionDAO.getByIdAndOwnerId(repositoryConnectionId, internalOwnerId);
    if (repositoryConnection == null) {
      throw new NotFoundException(
          String.format(REPOSITORY_CONNECTION_NOT_FOUND_ERROR, repositoryConnectionId, ownerType, internalOwnerId));
    }
    return testRepositoryConnection(repositoryConnection.getBaseUrl(), repositoryConnection.getFormat(),
        repositoryConnection.getUsername(),
        repositoryConnection.getPassword() == null
            ? null
            : passwordHandler.decryptPassword(
                repositoryConnection.getPassword()));
  }

  // Visible for testing
  public ApiRepositoryConnectionStatusResponseDTO getOwnerRepositoryConnectionStatus(
      OwnerType ownerType,
      String internalOwnerId)
  {
    ApiRepositoryConnectionStatusResponseDTO dto = new ApiRepositoryConnectionStatusResponseDTO();
    dto.allowChange = true;

    String parentOrgId;
    switch (ownerType) {
      case APPLICATION:
        Application app = applicationDAO.getByIdNotNull(internalOwnerId);
        dto.enabled = app.isRepositoryConnectionEnabled();
        parentOrgId = app.getOrganizationId();
        break;
      case ORGANIZATION:
        Organization org = organizationDAO.getByIdNotNull(internalOwnerId);
        dto.enabled = org.isRepositoryConnectionEnabled();
        dto.allowOverride = org.isAllowRepositoryConnectionOverride();
        parentOrgId = org.getParentOrganizationId();
        break;
      default:
        throw new IllegalStateException("Unknown owner type: " + ownerType);
    }

    while (parentOrgId != null) {
      Organization org = organizationDAO.getByIdNotNull(parentOrgId);

      if (!org.isAllowRepositoryConnectionOverride()) {
        dto.inheritedFromOrgEnabled = org.isRepositoryConnectionEnabled();
        dto.inheritedFromOrganizationId = org.getId();
        dto.inheritedFromOrganizationName = org.getName();
        dto.allowChange = false;
      }
      else if (dto.enabled == null && dto.inheritedFromOrgEnabled == null) {
        dto.inheritedFromOrganizationId = org.getId();
        dto.inheritedFromOrganizationName = org.getName();
        dto.inheritedFromOrgEnabled = org.isRepositoryConnectionEnabled();
      }

      parentOrgId = org.getParentOrganizationId();
    }

    return dto;
  }

  private StatusType testRepositoryConnection(
      String baseUrl,
      RepositoryFormat format,
      String username,
      char[] password)
  {
    AuditData.get()
        .setData(REPOSITORY_URL_AUDIT_KEY, baseUrl)
        .setData(REPOSITORY_FORMAT_AUDIT_KEY, format);
    RepositoryClient client = repositoryClientFactory.create()
        .forNexus3(baseUrl, username, password);
    try {
      return client.getServerStatus();
    }
    catch (IOException e) {
      log.debug(String.format("repository connection test failed for repository URL: %s", baseUrl), e);
      return Status.BAD_GATEWAY;
    }
  }

  private ApiRepositoryConnectionDTO toRepositoryConnectionDTO(RepositoryConnection repositoryConnection) {
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.repositoryConnectionId = repositoryConnection.getId();
    dto.ownerType =
        Optional.ofNullable(ownerDAO.getById(repositoryConnection.getOwnerId())).map(Owner::getType).orElse(null);
    dto.ownerId = repositoryConnection.getOwnerId();
    dto.format = repositoryConnection.getFormat();
    dto.baseUrl = repositoryConnection.getBaseUrl();
    dto.username = repositoryConnection.getUsername();
    dto.isAnonymous = dto.username == null;
    return dto;
  }

  private void validateRepositoryConnection(ApiRepositoryConnectionDTO dto) {
    if (dto == null || StringUtils.isBlank(dto.baseUrl)) {
      throw new BadRequestException("missing repository base URL");
    }
    // if a username is provided a password should be provided as well
    if (isNotCompleteAuthData(dto)) {
      throw new BadRequestException("missing username/password for repository connection");
    }
  }

  private RepositoryConnection toRepositoryConnection(ApiRepositoryConnectionDTO dto) {
    char[] passwordChars = dto.password == null ? null : dto.password.toCharArray();
    return new RepositoryConnection(dto.ownerId, dto.baseUrl, dto.format, dto.username,
        passwordHandler.encryptPassword(passwordChars));
  }
}
