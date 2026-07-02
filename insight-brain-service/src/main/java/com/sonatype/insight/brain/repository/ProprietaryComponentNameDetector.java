/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.ArrayList;
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

  /**
   * Matchers built from <em>all</em> patterns for a format (enabled and disabled), used only to deduplicate incoming
   * patterns in {@link #addPatterns(String, Collection)}. This is distinct from {@link #matchersByFormat}, which holds
   * enabled-only matchers used for matching in {@link #findProprietaryComponentName}. Cached with the same TTL so the
   * high-volume add path does not reload all patterns from the database on every request.
   */
  private final TenantReference<ConcurrentMap<String, ComponentNameMatcher>> allPatternMatchersByFormat =
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

  /**
   * Returns the cached matcher built from all patterns (enabled and disabled) for the format, rebuilding it from the
   * database when stale. Mirrors the caching of {@link #getMatcher(String)} but over the full pattern set so the
   * high-volume add path does not reload all patterns from the database on every request.
   * <p>
   * The {@code HoldingLock} suffix encodes the precondition that callers must already hold the per-format lock from
   * {@link #locksByFormat}: unlike {@link #getMatcher(String)}, this method does no double-checked locking of its own,
   * so calling it without the lock could race two concurrent rebuilds.
   */
  private ComponentNameMatcher getOrBuildAllPatternMatcherHoldingLock(String format) {
    ComponentNameMatcher matcher = allPatternMatchersByFormat.get().get(format);
    if (isMatcherStale(matcher)) {
      long start = System.currentTimeMillis();
      Collection<ProprietaryComponentNamePattern> allPatterns =
          proprietaryComponentNamePatternDAO.getByFormat(format);
      matcher = new ComponentNameMatcher(format, allPatterns);
      log.debug("Created all-pattern matcher for {} proprietary component names ({}) in {} ms", allPatterns.size(),
          format, System.currentTimeMillis() - start);
      allPatternMatchersByFormat.get().put(format, matcher);
    }
    return matcher;
  }

  /**
   * This method is intended to be called by NXRM/Artifactory when they push Namespace Confusion Protection patterns to
   * IQ. The patterns may already exists in IQ and they may be disabled.
   * This method should not change the enabled/disabled state of the existing patterns.
   * <p>
   * Deduplication (against the cached all-patterns matcher) and the insert run under the per-format lock so that, for a
   * given format, "decide which patterns are new" and "persist them" are atomic with respect to concurrent
   * {@link #addPatterns} calls. A concurrent {@link #removePatterns} deletes by repository, not format, so it is not
   * serialized by this lock; it clears the caches so the read path always reloads from the database (the source of
   * truth). Re-inserts never duplicate rows (database unique constraint + ignore-on-duplicate), though a re-push that
   * races a delete may be treated as already-present until the cache is next rebuilt.
   *
   * @return The number of patterns actually persisted to the database. A pattern that this node's stale cache
   *         considered new but that another node had already persisted is silently skipped by the ignore-on-duplicate
   *         insert and is <em>not</em> counted, so the result reflects DB-confirmed inserts rather than dedup guesses.
   */
  public int addPatterns(String format, Collection<ProprietaryComponentNamePattern> patterns) {
    int inserted;
    // This is the same per-format lock used by getMatcher() on the read path. The read path only enters the lock when
    // its own (enabled-only) matcher is stale, so in steady state there is no contention; the windows overlap only
    // when a matcher is cold/just-invalidated while an add is in progress. Keep the critical section minimal: it must
    // cover the dedup-then-insert so they are atomic, but cross-node invalidation is intentionally left outside it.
    synchronized (locksByFormat.get().computeIfAbsent(format, key -> new Object())) {
      try {
        ComponentNameMatcher matcher = getOrBuildAllPatternMatcherHoldingLock(format);
        // matcher.add() mutates the cached all-pattern matcher in place, and can throw partway through a batch (e.g.
        // ComponentNameMatcher.add() rejects a format mismatch after recording earlier patterns). Keep it inside the
        // try so any such throw evicts the matcher rather than leaving never-persisted patterns cached as "present".
        Collection<ProprietaryComponentNamePattern> newlyAdded = matcher.add(patterns);
        if (newlyAdded.isEmpty()) {
          return 0;
        }
        log.debug("Adding {} new proprietary component names ({})", newlyAdded.size(), format);

        // ignoreDuplicateKey covers the race where another node (with a fresher view) already inserted a pattern that
        // our cached matcher considered new; the duplicate is silently ignored at the database in a single round-trip.
        // insertBatch returns the number of rows actually written, excluding any such silently-skipped duplicate.
        inserted = proprietaryComponentNamePatternDAO.insertBatch(new ArrayList<>(newlyAdded), true);
      }
      catch (RuntimeException e) {
        // The cached matcher may have been mutated to include patterns that did not persist (insert failure) or were
        // recorded before a mid-batch throw. Drop it so the next add rebuilds from the database rather than treating
        // the never-persisted patterns as already present.
        allPatternMatchersByFormat.get().remove(format);
        throw e;
      }
    }
    // Invalidate other nodes outside the per-format lock: it is a fire-and-forget cross-node notification that does
    // its own database work (scheduling a Quartz task) and is not part of the dedup/insert atomicity invariant. Gate
    // it on a confirmed insert so a stale-cache batch where the database skipped every row as a duplicate (another
    // node already persisted them) does not trigger a spurious invalidation, and the resulting cache-rebuild thrash,
    // on the other nodes.
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
    allPatternMatchersByFormat.get().clear();
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }
}
