/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternDAO;
import com.sonatype.insight.brain.model.component.ProprietaryComponentName;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.PersistenceException;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ProprietaryComponentNameDetector
    implements InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(ProprietaryComponentNameDetector.class);

  private final ProprietaryComponentNamePatternDAO proprietaryComponentNamePatternDAO;

  private final TenantReference<ConcurrentMap<String, ComponentNameMatcher>> matchersByFormat =
      new TenantReference<>(ConcurrentHashMap::new);

  private final TenantReference<ConcurrentMap<String, Object>> locksByFormat =
      new TenantReference<>(ConcurrentHashMap::new);

  // Visible for testing
  static final String TASK_NAME = "InvalidateComponentNameMatchers";

  private final TaskScheduler taskScheduler;

  @Inject
  public ProprietaryComponentNameDetector(
      ProprietaryComponentNamePatternDAO proprietaryComponentNamePatternDAO,
      TaskScheduler taskScheduler)
  {
    this.proprietaryComponentNamePatternDAO = proprietaryComponentNamePatternDAO;
    this.taskScheduler = taskScheduler;
  }

  public ProprietaryComponentName findProprietaryComponentName(ComponentIdentifier componentIdentifier) {
    if (componentIdentifier == null) {
      return null;
    }
    PackageUrlIdentifier purlIdentifier = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);
    String namespace = purlIdentifier.getNamespace();
    String name = purlIdentifier.getName();
    return findProprietaryComponentName(componentIdentifier.getFormat(), namespace, name);
  }

  private ProprietaryComponentName findProprietaryComponentName(String format, String namespace, String name) {
    return getMatcher(format).findMatch(namespace, name);
  }

  @VisibleForTesting
  ComponentNameMatcher getMatcher(String format) {
    ComponentNameMatcher matcher = matchersByFormat.get().get(format);
    if (isMatcherStale(matcher)) {
      synchronized (locksByFormat.get().computeIfAbsent(format, key -> new Object())) {
        matcher = matchersByFormat.get().get(format);
        if (isMatcherStale(matcher)) {
          long start = System.currentTimeMillis();
          Collection<ProprietaryComponentNamePattern> patterns =
              proprietaryComponentNamePatternDAO.getEnabledByFormat(format);
          matcher = new ComponentNameMatcher(format, patterns);
          log.debug("Created matcher for {} proprietary component names ({}) in {} ms", patterns.size(), format,
              System.currentTimeMillis() - start);
          matchersByFormat.get().put(format, matcher);
        }
      }
    }
    return matcher;
  }

  private boolean isMatcherStale(ComponentNameMatcher matcher) {
    if (matcher == null) {
      return true;
    }
    if (!proprietaryComponentNamePatternDAO.isDatabaseEmbedded()
        && System.currentTimeMillis() - matcher.getCreateTime() > 60_000 * 3)
    {
      return true;
    }
    return false;
  }

  private ComponentNameMatcher getMatcherWithDisabledPatterns(String format) {
    ComponentNameMatcher componentNameMatcher =
        new ComponentNameMatcher(format, proprietaryComponentNamePatternDAO.getByFormat(format));

    return componentNameMatcher;
  }

  /**
   * This method is intended to be called by NXRM/Artifactory when they push Namespace Confusion Protection patterns to
   * IQ. The patterns may already exists in IQ and they may be disabled.
   * This method should not change the enabled/disabled state of the existing patterns.
   *
   * @return The number of new patterns added
   */
  public int addPatterns(String format, Collection<ProprietaryComponentNamePattern> patterns) {
    Collection<ProprietaryComponentNamePattern> newlyAdded = getMatcherWithDisabledPatterns(format).add(patterns);
    log.debug("Adding {} new proprietary component names ({})", newlyAdded.size(), format);

    int inserted = 0;
    for (ProprietaryComponentNamePattern pattern : newlyAdded) {
      try {
        proprietaryComponentNamePatternDAO.insert(pattern);
        inserted++;
      }
      catch (PersistenceException e) {
        if (e.getCause() instanceof EntityExistsException) {
          // another request/node was faster
        }
        throw e;
      }
    }
    if (inserted > 0) {
      invalidateMatchersOnOtherNodes();
    }
    return inserted;
  }

  public void removePatterns(String repositoryId) {
    log.debug("Deleting proprietary component names from repository ID {}", repositoryId);
    proprietaryComponentNamePatternDAO.deleteByRepository(repositoryId);
    invalidateMatchers();
    invalidateMatchersOnOtherNodes();
  }

  public void invalidateMatchersOnOtherNodes() {
    taskScheduler.scheduleOneTimeTaskForAllOtherNodes(this);
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    execute(this::invalidateMatchers, log, "Failed to invalidate proprietary component name matchers.");
  }

  void invalidateMatchers() {
    matchersByFormat.get().clear();
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }
}
