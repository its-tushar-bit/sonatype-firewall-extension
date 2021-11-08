/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryConnectionDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryConnectionDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.StringUtils;

@Named
@Singleton
public class ApiRepositoryConnectionService
{
  public static final String REPOSITORY_URL_AUDIT_KEY = "repositoryBaseUrl";

  private final RepositoryConnectionDAO repositoryConnectionDAO;

  private final PasswordHandler passwordHandler;

  @Inject
  public ApiRepositoryConnectionService(
      final RepositoryConnectionDAO repositoryConnectionDAO,
      final PasswordHandler passwordHandler)
  {
    this.repositoryConnectionDAO = repositoryConnectionDAO;
    this.passwordHandler = passwordHandler;
  }

  @Authorize(permission = Permission.WRITE)
  public ApiRepositoryConnectionDTO addRepositoryConnection(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String ownerId,
      ApiRepositoryConnectionDTO repositoryConnectionDTO)
  {
    validateRepositoryConnection(repositoryConnectionDTO);
    AuditData.get().setData(REPOSITORY_URL_AUDIT_KEY, repositoryConnectionDTO.baseUrl);
    if (repositoryConnectionDAO.getByOwnerIdAndBaseUrl(ownerId, repositoryConnectionDTO.baseUrl) != null) {
      throw new ConflictException(
          String.format("base URL configuration already exist for %s with id: %s", ownerType, ownerId));
    }

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
    AuditData.get().setData(REPOSITORY_URL_AUDIT_KEY, dto.baseUrl);
    dto.ownerId = internalOwnerId;
    if (StringUtils.isNotBlank(dto.baseUrl)) {
      RepositoryConnection connection =
          repositoryConnectionDAO.getByOwnerIdAndBaseUrl(internalOwnerId, dto.baseUrl);
      if (connection != null && !Objects.equals(connection.getId(), repositoryConnectionId)) {
        throw new ConflictException(String.format("repository connection URL configuration exist for %s with id: %s",
            ownerType, internalOwnerId));
      }
    }

    RepositoryConnection storedConnection = repositoryConnectionDAO.getById(repositoryConnectionId);
    if (storedConnection == null) {
      throw new NotFoundException(
          String.format("no repository connections found with connection id: %s for %s having id: %s",
              repositoryConnectionId, ownerType, internalOwnerId));
    }

    updateRepositoryConnectionData(dto, storedConnection);
    repositoryConnectionDAO.update(storedConnection);
    return toRepositoryConnectionDTO(storedConnection);
  }

  private void validateUpdateConnectionData(final ApiRepositoryConnectionDTO dto) {
    if (dto == null || StringUtils.isAllBlank(dto.baseUrl, dto.username, dto.password)) {
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
    if (StringUtils.isNoneEmpty(dto.baseUrl, storedConnection.getUsername())) {
      //the existing connection has auth config. so new auth data must be provided
      if (StringUtils.isAnyBlank(dto.username, dto.password)) {
        throw new BadRequestException("missing username/password for repository connection");
      }
    }

    if (StringUtils.isNotBlank(dto.baseUrl)) {
      storedConnection.setBaseUrl(dto.baseUrl);
    }
    if (StringUtils.isNoneBlank(dto.username, dto.password)) {
      storedConnection.setUsername(dto.username);
      storedConnection.setPassword(passwordHandler.encryptPassword(dto.password.toCharArray()));
    }
  }

  @Authorize(permission = Permission.WRITE)
  public void deleteRepositoryConnection(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String internalOwnerId,
      String repositoryConnectionId)
  {
    RepositoryConnection conn = repositoryConnectionDAO.getById(repositoryConnectionId);
    if (conn == null || !internalOwnerId.equals(conn.getOwnerId())) {
      throw new NotFoundException(
          String.format("no repository connections found with connection id: %s for %s having id: %s",
              repositoryConnectionId, ownerType, internalOwnerId));
    }
    repositoryConnectionDAO.delete(conn);
  }

  @Authorize(permission = Permission.READ)
  public List<ApiRepositoryConnectionDTO> getRepositoryConnections(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String internalOwnerId)
  {
    return repositoryConnectionDAO.getByOwnerId(internalOwnerId).stream()
        .map(this::toRepositoryConnectionDTO)
        .collect(Collectors.toList());
  }

  private ApiRepositoryConnectionDTO toRepositoryConnectionDTO(RepositoryConnection repositoryConnection) {
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.repositoryConnectionId = repositoryConnection.getId();
    dto.ownerId = repositoryConnection.getOwnerId();
    dto.baseUrl = repositoryConnection.getBaseUrl();
    dto.username = repositoryConnection.getUsername();
    return dto;
  }

  private void validateRepositoryConnection(ApiRepositoryConnectionDTO dto) {
    if (dto == null || StringUtils.isBlank(dto.baseUrl)) {
      throw new BadRequestException("missing repository base URL");
    }
    //if a username is provided a password should be provided as well
    if (isNotCompleteAuthData(dto)) {
      throw new BadRequestException("missing username/password for repository connection");
    }
  }

  private RepositoryConnection toRepositoryConnection(ApiRepositoryConnectionDTO dto) {
    char[] passwordChars = dto.password == null ? null : dto.password.toCharArray();
    return new RepositoryConnection(dto.ownerId, dto.baseUrl, dto.username,
        passwordHandler.encryptPassword(passwordChars));
  }
}
