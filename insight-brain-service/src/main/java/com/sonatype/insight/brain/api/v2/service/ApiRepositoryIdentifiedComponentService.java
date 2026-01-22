/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.component.RepositoryIdentifiedComponentCache;
import com.sonatype.insight.brain.dataaccess.component.RepositoryIdentifiedComponentDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.lang3.StringUtils;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ApiRepositoryIdentifiedComponentService
    implements InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(ApiRepositoryIdentifiedComponentService.class);

  // Visible for testing
  static final String TASK_NAME = "DeleteRepositoryIdentifiedComponent";

  static final String TASK_PARAM_CLEAR_ALL = "CLEAR_ALL";

  // Visible for testing
  static final String TASK_PARAM_HASH = "hash";

  // Visible for testing
  static final String TASK_PARAM_COMPONENT_IDENTIFIER = "componentIdentifier";

  private final RepositoryIdentifiedComponentDAO repositoryIdentifiedComponentDAO;

  private final RepositoryIdentifiedComponentCache repositoryIdentifiedComponentCache;

  private final TaskScheduler taskScheduler;

  @Inject
  public ApiRepositoryIdentifiedComponentService(
      RepositoryIdentifiedComponentDAO repositoryIdentifiedComponentDAO,
      RepositoryIdentifiedComponentCache repositoryIdentifiedComponentCache,
      TaskScheduler taskScheduler)
  {
    this.repositoryIdentifiedComponentDAO = repositoryIdentifiedComponentDAO;
    this.repositoryIdentifiedComponentCache = repositoryIdentifiedComponentCache;
    this.taskScheduler = taskScheduler;
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
    AuditData.get().setData("hash", hash).setComponentIdentifier(componentIdentifier).setData("packageUrl", packageUrl);
    if (packageUrl != null) {
      componentIdentifier = new PackageUrlIdentifier(packageUrl).toComponentIdentifier();
    }
    deleteRepositoryIdentifiedComponent(hash, componentIdentifier);
    deleteRepositoryIdentifiedComponentFromMemoryOnAllOtherClusterNodes(hash, componentIdentifier);
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void deleteAllRepositoryIdentifiedComponents() {
    try {
      clearAll();
      clearCacheFromMemoryOnAllOtherClusterNodes();
    }
    catch (Exception e) {
      log.warn("There was an error deleting all repository identified components.", e);
      throw new IllegalStateException("There was an error deleting all repository identified components.");
    }
  }

  private void deleteRepositoryIdentifiedComponent(String hash, ComponentIdentifier componentIdentifier) {
    if (hash != null) {
      deleteByHash(hash);
    }
    else {
      deleteByComponentIdentifier(componentIdentifier);
    }
  }

  // Visible for testing
  void deleteRepositoryIdentifiedComponentFromMemory(String hash, ComponentIdentifier componentIdentifier) {
    if (hash != null) {
      deleteByHashFromMemory(hash);
    }
    else {
      deleteByComponentIdentifierFromMemory(componentIdentifier);
    }
  }

  private long countNonNulls(Object... objects) {
    return Arrays.stream(objects).filter(Objects::nonNull).count();
  }

  private void deleteByHash(String hash) {
    deleteByHashFromDatabase(hash);
    deleteByHashFromMemory(hash);
  }

  private void deleteByHashFromDatabase(String hash) {
    if (repositoryIdentifiedComponentDAO.deleteByHash(hash) > 0) {
      log.debug("Removed repository identified component with hash {} from the database.", hash);
    }
  }

  private void deleteByHashFromMemory(String hash) {
    if (repositoryIdentifiedComponentCache.removeByHash(hash) != null) {
      log.debug("Removed repository identified component with hash {} from memory.", hash);
    }
  }

  private void deleteByComponentIdentifier(ComponentIdentifier componentIdentifier) {
    deleteByComponentIdentifierFromDatabase(componentIdentifier);
    deleteByComponentIdentifierFromMemory(componentIdentifier);
  }

  void clearAll() {
    clearAllFromDatabase();
    clearAllFromMemory();
  }

  private void clearAllFromDatabase() {
    int componentsRemovedFromDB = repositoryIdentifiedComponentDAO.deleteAll();
    if (componentsRemovedFromDB > 0) {
      log.debug("{} repository identified components were removed from the database cache.", componentsRemovedFromDB);
    }
  }

  private void clearAllFromMemory() {
    long componentsRemovedFromMemory = repositoryIdentifiedComponentCache.removeAll();
    if (componentsRemovedFromMemory > 0) {
      log.debug("{} repository identified components were removed from memory cache.", componentsRemovedFromMemory);
    }
  }

  private void deleteByComponentIdentifierFromDatabase(ComponentIdentifier componentIdentifier) {
    if (repositoryIdentifiedComponentDAO.deleteByComponentIdentifier(componentIdentifier) > 0) {
      log.debug("Removed repository identified components with component identifier {} from the database.",
          componentIdentifier);
    }
  }

  private void deleteByComponentIdentifierFromMemory(ComponentIdentifier componentIdentifier) {
    if (repositoryIdentifiedComponentCache.removeByComponentIdentifier(componentIdentifier) > 0) {
      log.debug("Removed repository identified components with component identifier {} from memory.",
          componentIdentifier);
    }
  }

  public void deleteRepositoryIdentifiedComponentFromMemoryOnAllOtherClusterNodes(
      String hash,
      ComponentIdentifier componentIdentifier)
  {
    Map<String, String> parameters = new HashMap<>();
    if (hash != null) {
      parameters.put(TASK_PARAM_HASH, hash);
    }
    if (componentIdentifier != null) {
      parameters.put(TASK_PARAM_COMPONENT_IDENTIFIER, JsonUtils.writeUnformatted(componentIdentifier));
    }
    taskScheduler.scheduleOneTimeTaskForAllOtherNodes(this, parameters);
  }

  private void clearCacheFromMemoryOnAllOtherClusterNodes() {
    Map<String, String> parameters = new HashMap<>();
    parameters.put(TASK_PARAM_CLEAR_ALL, String.valueOf(true));
    taskScheduler.scheduleOneTimeTaskForAllOtherNodes(this, parameters);
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forSystem()) {
      JobDataMap mergedJobDataMap = context.getMergedJobDataMap();

      String clearAll = mergedJobDataMap.getString(TASK_PARAM_CLEAR_ALL);

      //If the parameter TASK_PARAM_CLEAR_ALL exists, it's a request to clear all the cache,
      //otherwise it's a single request
      if (StringUtils.isNotBlank(clearAll)) {
        clearAllFromMemory();
      }
      else {
        clearSingleEntryFromMemory(mergedJobDataMap);
      }
    }
    catch (Exception e) {
      log.error("Error when deleting repository identified component: {}", e.getMessage(), e);
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational
      // at this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(1);
    }
  }

  private void clearSingleEntryFromMemory(JobDataMap mergedJobDataMap) throws IOException {
    String hash = mergedJobDataMap.getString(TASK_PARAM_HASH);
    String componentIdentifierJson = mergedJobDataMap.getString(TASK_PARAM_COMPONENT_IDENTIFIER);
    ComponentIdentifier componentIdentifier = null;
    if (componentIdentifierJson != null) {
      componentIdentifier = JsonUtils.parse(componentIdentifierJson, ComponentIdentifier.class);
    }
    deleteRepositoryIdentifiedComponentFromMemory(hash, componentIdentifier);
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }
}
