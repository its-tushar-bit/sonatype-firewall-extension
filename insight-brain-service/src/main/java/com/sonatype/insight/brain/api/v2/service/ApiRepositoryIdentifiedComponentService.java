/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.component.RepositoryIdentifiedComponentCache;
import com.sonatype.insight.brain.dataaccess.component.RepositoryIdentifiedComponentDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ApiRepositoryIdentifiedComponentService
{
  private static final Logger log = LoggerFactory.getLogger(ApiRepositoryIdentifiedComponentService.class);

  private final RepositoryIdentifiedComponentDAO repositoryIdentifiedComponentDAO;

  private final RepositoryIdentifiedComponentCache repositoryIdentifiedComponentCache;

  @Inject
  public ApiRepositoryIdentifiedComponentService(
      RepositoryIdentifiedComponentDAO repositoryIdentifiedComponentDAO,
      RepositoryIdentifiedComponentCache repositoryIdentifiedComponentCache)
  {
    this.repositoryIdentifiedComponentDAO = repositoryIdentifiedComponentDAO;
    this.repositoryIdentifiedComponentCache = repositoryIdentifiedComponentCache;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void deleteRepositoryIdentifiedComponent(
      String hash,
      ComponentIdentifier componentIdentifier,
      String packageUrl)
  {
    long nonNulls = countNonNulls(hash, componentIdentifier, packageUrl);
    if (nonNulls == 0) {
      throw new BadRequestException("You must specify one of either hash or componentIdentifier or packageUrl.");
    }
    else if (nonNulls > 1) {
      throw new BadRequestException("Only one of either hash or componentIdentifier or packageUrl must be specified.");
    }
    if (hash != null) {
      AuditData.get().setData("hash", hash);
      deleteByHash(hash);
    }
    else if (componentIdentifier != null) {
      AuditData.get().setComponentIdentifier(componentIdentifier);
      deleteByComponentIdentifier(componentIdentifier);
    }
    else {
      AuditData.get().setData("packageUrl", packageUrl);
      deleteByComponentIdentifier(new PackageUrlIdentifier(packageUrl).toComponentIdentifier());
    }
  }

  private long countNonNulls(Object... objects) {
    return Arrays.stream(objects).filter(Objects::nonNull).count();
  }

  private void deleteByHash(String hash) {
    boolean deletedByHashFromDatabase = deleteByHashFromDatabase(hash);
    boolean deletedByHashFromMemory = deleteByHashFromMemory(hash);
    if (!deletedByHashFromDatabase && !deletedByHashFromMemory) {
      throw new NotFoundException(String.format("Repository identified component with hash %s was not found.", hash));
    }
  }

  private boolean deleteByHashFromDatabase(String hash) {
    if (repositoryIdentifiedComponentDAO.deleteByHash(hash) > 0) {
      log.debug("Removed repository identified component with hash {} from the database.", hash);
      return true;
    }
    return false;
  }

  private boolean deleteByHashFromMemory(String hash) {
    if (repositoryIdentifiedComponentCache.removeByHash(hash) != null) {
      log.debug("Removed repository identified component with hash {} from memory.", hash);
      return true;
    }
    return false;
  }

  private void deleteByComponentIdentifier(ComponentIdentifier componentIdentifier) {
    boolean deletedByComponentIdentifierFromDatabase = deleteByComponentIdentifierFromDatabase(componentIdentifier);
    boolean deletedByComponentIdentifierFromMemory = deleteByComponentIdentifierFromMemory(componentIdentifier);
    if (!deletedByComponentIdentifierFromDatabase && !deletedByComponentIdentifierFromMemory) {
      throw new NotFoundException(
          String.format("Repository identified components with component identifier %s were not found.",
              componentIdentifier));
    }
  }

  private boolean deleteByComponentIdentifierFromDatabase(ComponentIdentifier componentIdentifier) {
    if (repositoryIdentifiedComponentDAO.deleteByComponentIdentifier(componentIdentifier) > 0) {
      log.debug("Removed repository identified components with component identifier {} from the database.",
          componentIdentifier);
      return true;
    }
    return false;
  }

  private boolean deleteByComponentIdentifierFromMemory(ComponentIdentifier componentIdentifier) {
    if (repositoryIdentifiedComponentCache.removeByComponentIdentifier(componentIdentifier) > 0) {
      log.debug("Removed repository identified components with component identifier {} from memory.",
          componentIdentifier);
      return true;
    }
    return false;
  }
}
