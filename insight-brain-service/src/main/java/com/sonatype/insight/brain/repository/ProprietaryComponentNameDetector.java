/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityExistsException;
import javax.persistence.PersistenceException;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternDAO;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ProprietaryComponentNameDetector
{
  private static final Logger log = LoggerFactory.getLogger(ProprietaryComponentNameDetector.class);

  private final ConcurrentMap<String, ComponentNameMatcher> matchersByFormat = new ConcurrentHashMap<>();

  private final ConcurrentMap<String, Object> locksByFormat = new ConcurrentHashMap<>();

  private final ProprietaryComponentNamePatternDAO proprietaryComponentNamePatternDAO;

  @Inject
  public ProprietaryComponentNameDetector(ProprietaryComponentNamePatternDAO proprietaryComponentNamePatternDAO) {
    this.proprietaryComponentNamePatternDAO = proprietaryComponentNamePatternDAO;
  }

  public String findProprietaryComponentName(ComponentIdentifier componentIdentifier) {
    if (componentIdentifier == null) {
      return null;
    }
    PackageUrlIdentifier purlIdentifier = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);
    String namespace = purlIdentifier.getNamespace();
    String name = purlIdentifier.getName();
    return findProprietaryComponentName(componentIdentifier.getFormat(), namespace, name);
  }

  private String findProprietaryComponentName(String format, String namespace, String name) {
    return getMatcher(format).findMatch(namespace, name);
  }

  private ComponentNameMatcher getMatcher(String format) {
    ComponentNameMatcher matcher = matchersByFormat.get(format);
    if (isMatcherStale(matcher)) {
      synchronized (locksByFormat.computeIfAbsent(format, key -> new Object())) {
        matcher = matchersByFormat.get(format);
        if (isMatcherStale(matcher)) {
          long start = System.currentTimeMillis();
          Collection<ProprietaryComponentNamePattern> patterns = proprietaryComponentNamePatternDAO.getByFormat(format);
          matcher = new ComponentNameMatcher(format, patterns);
          log.debug("Created matcher for {} proprietary component names ({}) in {} ms", patterns.size(), format,
              System.currentTimeMillis() - start);
          matchersByFormat.put(format, matcher);
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
        && System.currentTimeMillis() - matcher.getCreateTime() > 60_000 * 3) {
      return true;
    }
    return false;
  }

  public int addPatterns(String format, Collection<ProprietaryComponentNamePattern> patterns) {
    Collection<ProprietaryComponentNamePattern> newlyAdded = getMatcher(format).add(patterns);
    if (!newlyAdded.isEmpty()) {
      log.debug("Adding {} new proprietary component names ({})", newlyAdded.size(), format);
    }
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
    return inserted;
  }

  public void removePatterns(String repositoryManagerInstanceId, String repositoryPublicId) {
    if ("*".equals(repositoryPublicId)) {
      log.debug("Deleting proprietary component names from all repositories of instance {}",
          repositoryManagerInstanceId);
      proprietaryComponentNamePatternDAO.deleteByRepositoryManager(repositoryManagerInstanceId);
    }
    else {
      log.debug("Deleting proprietary component names from repository {} of instance {}", repositoryPublicId,
          repositoryManagerInstanceId);
      proprietaryComponentNamePatternDAO.deleteByRepository(repositoryManagerInstanceId, repositoryPublicId);
    }
    invalidateMatchers();
  }

  private void invalidateMatchers() {
    matchersByFormat.clear();
  }
}
