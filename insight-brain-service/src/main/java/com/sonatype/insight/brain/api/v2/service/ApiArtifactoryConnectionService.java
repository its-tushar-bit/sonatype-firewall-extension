/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.StatusType;

import com.sonatype.insight.brain.api.v2.dto.ApiArtifactoryConnectionStatusRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiArtifactoryConnectionStatusResponseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiOwnerDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiArtifactoryConnectionDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiOwnerArtifactoryConnectionDTO;
import com.sonatype.insight.brain.artifactory.ArtifactoryClient;
import com.sonatype.insight.brain.artifactory.ArtifactoryClientFactory;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.artifactory.ArtifactoryConnectionDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.artifactory.ArtifactoryConnection;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ApiArtifactoryConnectionService
{
  private static final Logger log = LoggerFactory.getLogger(ApiArtifactoryConnectionService.class);

  public static final String ARTIFACTORY_URL_AUDIT_KEY = "artifactoryBaseUrl";

  public static final String ARTIFACTORY_CONNECTION_NOT_FOUND_ERROR =
      "no artifactory connections found with connection id: %s for %s having id: %s";

  public static final String MISSING_CREDENTIALS_ERROR = "missing username/password for artifactory connection";

  public static final String MISSING_CONNECTION_DATA_ERROR = "missing artifactory connection data for update";

  public static final String OVERRIDE_BY_CHILD_AUDIT_KEY = "overrideByChild";

  public static final String ENABLED_MODE_AUDIT_KEY = "enabledMode";

  private final OwnerDAO ownerDAO;

  private final ArtifactoryConnectionDAO artifactoryConnectionDAO;

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final PasswordHandler passwordHandler;

  private final ArtifactoryClientFactory artifactoryClientFactory;

  @Inject
  public ApiArtifactoryConnectionService(
      final OwnerDAO ownerDAO,
      final ArtifactoryConnectionDAO artifactoryConnectionDAO,
      final ApplicationDAO applicationDAO,
      final OrganizationDAO organizationDAO,
      final PasswordHandler passwordHandler,
      final ArtifactoryClientFactory artifactoryClientFactory)
  {
    this.ownerDAO = ownerDAO;
    this.artifactoryConnectionDAO = artifactoryConnectionDAO;
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.passwordHandler = passwordHandler;
    this.artifactoryClientFactory = artifactoryClientFactory;
  }

  @Authorize(permission = Permission.WRITE)
  public ApiArtifactoryConnectionDTO addArtifactoryConnection(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String ownerId,
      ApiArtifactoryConnectionDTO artifactoryConnectionDTO)
  {
    validateArtifactoryConnection(artifactoryConnectionDTO);

    AuditData.get().setData(ARTIFACTORY_URL_AUDIT_KEY, artifactoryConnectionDTO.baseUrl);
    if (artifactoryConnectionDAO.getByOwnerId(ownerId) != null) {
      throw new ConflictException(
          String.format("artifactory connection configuration exists for %s with id: %s", ownerType, ownerId));
    }

    artifactoryConnectionDTO.ownerType = ownerType;
    artifactoryConnectionDTO.ownerId = ownerId;
    ArtifactoryConnection artifactoryConnection = toArtifactoryConnection(artifactoryConnectionDTO);
    artifactoryConnectionDAO.insert(artifactoryConnection);
    return toArtifactoryConnectionDTO(artifactoryConnection);
  }

  @Authorize(permission = Permission.WRITE)
  public ApiArtifactoryConnectionDTO updateArtifactoryConnection(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String internalOwnerId,
      String artifactoryConnectionId,
      ApiArtifactoryConnectionDTO dto)
  {
    validateUpdateConnectionData(dto);

    AuditData.get().setData(ARTIFACTORY_URL_AUDIT_KEY, dto.baseUrl);
    dto.ownerType = ownerType;
    dto.ownerId = internalOwnerId;

    ArtifactoryConnection connection =
        artifactoryConnectionDAO.getByIdAndOwnerId(artifactoryConnectionId, internalOwnerId);

    if (connection == null) {
      throw new NotFoundException(
          String.format(ARTIFACTORY_CONNECTION_NOT_FOUND_ERROR, artifactoryConnectionId, ownerType, internalOwnerId));
    }

    updateArtifactoryConnectionData(dto, connection);
    artifactoryConnectionDAO.update(connection);
    return toArtifactoryConnectionDTO(connection);
  }

  @Authorize(permission = Permission.WRITE)
  public void updateOwnerArtifactoryConnectionStatus(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String ownerId,
      ApiArtifactoryConnectionStatusRequestDTO dto)
  {
    if (dto == null) {
      throw new BadRequestException("missing artifactory connection configuration data for update");
    }
    if (Organization.ROOT_ORGANIZATION_ID.equals(ownerId) && dto.enabled == null) {
      throw new BadRequestException("root organization cannot inherit configuration");
    }
    switch (ownerType) {
      case APPLICATION:
        Application app = applicationDAO.getByIdNotNull(ownerId);
        app.setArtifactoryConnectionEnabled(dto.enabled);
        applicationDAO.update(app);
        break;
      case ORGANIZATION:
        Organization org = organizationDAO.getByIdNotNull(ownerId);
        org.setArtifactoryConnectionEnabled(dto.enabled);
        org.setAllowArtifactoryConnectionOverride(dto.allowOverride);
        organizationDAO.update(org);
        AuditData.get().setData(OVERRIDE_BY_CHILD_AUDIT_KEY, dto.allowOverride ? "allow" : "disallow");
        break;
      default:
        throw new IllegalStateException("Unknown owner type: " + ownerType);
    }
    String dtoEnableDisableString = BooleanUtils.isTrue(dto.enabled) ? "enable" : "disable";
    AuditData.get()
        .setData(ENABLED_MODE_AUDIT_KEY, dto.enabled == null ? "inherit" : dtoEnableDisableString);
  }

  private void validateUpdateConnectionData(final ApiArtifactoryConnectionDTO dto) {
    if (dto == null || (dto.isAnonymous == null &&
        StringUtils.isAllBlank(dto.baseUrl, dto.username, dto.password)))
    {
      throw new BadRequestException(MISSING_CONNECTION_DATA_ERROR);
    }

    if (isNotCompleteAuthData(dto)) {
      throw new BadRequestException(MISSING_CREDENTIALS_ERROR);
    }
  }

  private boolean isNotCompleteAuthData(final ApiArtifactoryConnectionDTO dto) {
    return (StringUtils.isNotBlank(dto.username) && StringUtils.isBlank(dto.password)) ||
        (StringUtils.isBlank(dto.username) && StringUtils.isNotBlank(dto.password));
  }

  private void updateArtifactoryConnectionData(
      ApiArtifactoryConnectionDTO dto,
      ArtifactoryConnection storedConnection)
  {
    // the existing connection has auth config. so new auth data must be provided
    if (!Boolean.TRUE.equals(dto.isAnonymous) &&
        StringUtils.isNotEmpty(dto.baseUrl) &&
        !dto.baseUrl.equals(storedConnection.getBaseUrl()) &&
        StringUtils.isNotEmpty(storedConnection.getUsername()) &&
        StringUtils.isAnyBlank(dto.username, dto.password))
    {
      throw new BadRequestException(MISSING_CREDENTIALS_ERROR);
    }

    if (StringUtils.isNotBlank(dto.baseUrl)) {
      storedConnection.setBaseUrl(dto.baseUrl);
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
  public void deleteArtifactoryConnection(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String internalOwnerId,
      String artifactoryConnectionId)
  {
    ArtifactoryConnection conn = artifactoryConnectionDAO.getByIdAndOwnerId(artifactoryConnectionId, internalOwnerId);
    if (conn == null) {
      throw new NotFoundException(
          String.format(ARTIFACTORY_CONNECTION_NOT_FOUND_ERROR, artifactoryConnectionId, ownerType, internalOwnerId));
    }
    artifactoryConnectionDAO.delete(conn);
  }

  @Authorize(permission = Permission.READ)
  public ApiArtifactoryConnectionDTO getArtifactoryConnection(
      @SuppressWarnings("unused") @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String internalOwnerId,
      String artifactoryConnectionId)
  {
    ArtifactoryConnection artifactoryConnection =
        artifactoryConnectionDAO.getByIdAndOwnerId(artifactoryConnectionId, internalOwnerId);
    if (artifactoryConnection == null) {
      throw new NotFoundException(
          String.format(ARTIFACTORY_CONNECTION_NOT_FOUND_ERROR, artifactoryConnectionId, ownerType, internalOwnerId));
    }
    return toArtifactoryConnectionDTO(artifactoryConnection);
  }

  @Authorize(permission = Permission.READ)
  public ApiOwnerArtifactoryConnectionDTO getOwnerArtifactoryConnection(
      @SuppressWarnings("unused") @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String internalOwnerId,
      boolean inherit)
  {
    ApiOwnerArtifactoryConnectionDTO result = new ApiOwnerArtifactoryConnectionDTO();
    result.artifactoryConnectionStatus = getOwnerArtifactoryConnectionStatus(ownerType, internalOwnerId);
    result.ownerDTO = ApiOwnerDTO.fromOwner(ownerDAO.getById(internalOwnerId));
    String effectiveOwnerId = resolveEffectiveOwnerId(internalOwnerId, inherit, result);
    ArtifactoryConnection connection = artifactoryConnectionDAO.getByOwnerId(effectiveOwnerId);
    result.artifactoryConnection = connection != null ? toArtifactoryConnectionDTO(connection) : null;
    return result;
  }

  private String resolveEffectiveOwnerId(
      final String internalOwnerId,
      final boolean inherit,
      final ApiOwnerArtifactoryConnectionDTO result)
  {
    if (inherit && result.artifactoryConnectionStatus.inheritedFromOrganizationId != null) {
      return result.artifactoryConnectionStatus.inheritedFromOrganizationId;
    }
    return internalOwnerId;
  }

  @Authorize(permission = Permission.READ)
  public StatusType testArtifactoryConnection(
      @SuppressWarnings("unused") @AuthzContext(Key.TYPE) OwnerType ownerType,
      @SuppressWarnings("unused") @AuthzContext(Key.INTERNAL_ID) String internalOwnerId,
      ApiArtifactoryConnectionDTO artifactoryConnectionDTO)
  {
    validateArtifactoryConnection(artifactoryConnectionDTO);
    return testArtifactoryConnection(artifactoryConnectionDTO.baseUrl, artifactoryConnectionDTO.username,
        artifactoryConnectionDTO.password == null ? null : artifactoryConnectionDTO.password.toCharArray());
  }

  @Authorize(permission = Permission.READ)
  public StatusType testArtifactoryConnection(
      @SuppressWarnings("unused") @AuthzContext(Key.TYPE) OwnerType ownerType,
      @SuppressWarnings("unused") @AuthzContext(Key.INTERNAL_ID) String internalOwnerId,
      String artifactoryConnectionId)
  {
    ArtifactoryConnection artifactoryConnection =
        artifactoryConnectionDAO.getByIdAndOwnerId(artifactoryConnectionId, internalOwnerId);
    if (artifactoryConnection == null) {
      throw new NotFoundException(
          String.format(ARTIFACTORY_CONNECTION_NOT_FOUND_ERROR, artifactoryConnectionId, ownerType, internalOwnerId));
    }
    return testArtifactoryConnection(artifactoryConnection.getBaseUrl(), artifactoryConnection.getUsername(),
        artifactoryConnection.getPassword() == null
            ? null
            : passwordHandler.decryptPassword(
                artifactoryConnection.getPassword()));
  }

  // Visible for testing
  public ApiArtifactoryConnectionStatusResponseDTO getOwnerArtifactoryConnectionStatus(
      OwnerType ownerType,
      String internalOwnerId)
  {
    ApiArtifactoryConnectionStatusResponseDTO dto = new ApiArtifactoryConnectionStatusResponseDTO();
    dto.allowChange = true;

    String parentOrgId;
    switch (ownerType) {
      case APPLICATION:
        Application app = applicationDAO.getByIdNotNull(internalOwnerId);
        dto.enabled = app.isArtifactoryConnectionEnabled();
        parentOrgId = app.getOrganizationId();
        break;
      case ORGANIZATION:
        Organization org = organizationDAO.getByIdNotNull(internalOwnerId);
        dto.enabled = org.isArtifactoryConnectionEnabled();
        dto.allowOverride = org.isAllowArtifactoryConnectionOverride();
        parentOrgId = org.getParentOrganizationId();
        break;
      default:
        throw new IllegalStateException("Unknown owner type: " + ownerType);
    }

    while (parentOrgId != null) {
      Organization org = organizationDAO.getByIdNotNull(parentOrgId);

      if (!org.isAllowArtifactoryConnectionOverride()) {
        dto.inheritedFromOrgEnabled = org.isArtifactoryConnectionEnabled();
        dto.inheritedFromOrganizationId = org.getId();
        dto.inheritedFromOrganizationName = org.getName();
        dto.allowChange = false;
      }
      else if (dto.enabled == null && dto.inheritedFromOrgEnabled == null) {
        dto.inheritedFromOrganizationId = org.getId();
        dto.inheritedFromOrganizationName = org.getName();
        dto.inheritedFromOrgEnabled = org.isArtifactoryConnectionEnabled();
      }

      parentOrgId = org.getParentOrganizationId();
    }

    return dto;
  }

  private StatusType testArtifactoryConnection(String baseUrl, String username, char[] password) {
    AuditData.get().setData(ARTIFACTORY_URL_AUDIT_KEY, baseUrl);
    ArtifactoryClient client = artifactoryClientFactory.create().forArtifactory(baseUrl, username, password);
    try {
      if (username != null) {
        return client.getServerStatusViaAQL();
      }
      else {
        return client.getServerStatusViaQueryParam();
      }
    }
    catch (IOException e) {
      log.debug(String.format("artifactory connection test failed for artifactory URL: %s", baseUrl), e);
      return Status.BAD_GATEWAY;
    }
  }

  private ApiArtifactoryConnectionDTO toArtifactoryConnectionDTO(ArtifactoryConnection artifactoryConnection) {
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.artifactoryConnectionId = artifactoryConnection.getId();
    dto.ownerType = Optional.ofNullable(ownerDAO.getById(artifactoryConnection.getOwnerId()))
        .map(Owner::getType)
        .orElse(null);
    dto.ownerId = artifactoryConnection.getOwnerId();
    dto.baseUrl = artifactoryConnection.getBaseUrl();
    dto.username = artifactoryConnection.getUsername();
    dto.isAnonymous = dto.username == null;
    return dto;
  }

  private void validateArtifactoryConnection(ApiArtifactoryConnectionDTO dto) {
    if (dto == null || StringUtils.isBlank(dto.baseUrl)) {
      throw new BadRequestException("missing artifactory base URL");
    }
    // if a username is provided a password should be provided as well
    if (isNotCompleteAuthData(dto)) {
      throw new BadRequestException(MISSING_CREDENTIALS_ERROR);
    }
  }

  private ArtifactoryConnection toArtifactoryConnection(ApiArtifactoryConnectionDTO dto) {
    char[] passwordChars = dto.password == null ? null : dto.password.toCharArray();
    return new ArtifactoryConnection(dto.ownerId, dto.baseUrl, dto.username,
        passwordHandler.encryptPassword(passwordChars));
  }
}
