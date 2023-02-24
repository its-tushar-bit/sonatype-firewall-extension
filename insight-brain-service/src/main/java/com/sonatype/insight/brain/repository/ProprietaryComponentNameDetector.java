/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityExistsException;
import javax.persistence.PersistenceException;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternDAO;
import com.sonatype.insight.brain.model.component.ProprietaryComponentName;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ProprietaryComponentNameDetector
{
  private static final Logger log = LoggerFactory.getLogger(ProprietaryComponentNameDetector.class);

  private final ProprietaryComponentNamePatternDAO proprietaryComponentNamePatternDAO;

  @Inject
  public ProprietaryComponentNameDetector(ProprietaryComponentNamePatternDAO proprietaryComponentNamePatternDAO) {
    this.proprietaryComponentNamePatternDAO = proprietaryComponentNamePatternDAO;
  }

  public ProprietaryComponentName findProprietaryComponentName(
      Map<String, ComponentNameMatcher> matchersByFormat,
      ComponentIdentifier componentIdentifier)
  {
    if (componentIdentifier == null) {
      return null;
    }
    PackageUrlIdentifier purlIdentifier = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);
    String namespace = purlIdentifier.getNamespace();
    String name = purlIdentifier.getName();
    return matchersByFormat.computeIfAbsent(componentIdentifier.getFormat(), this::getMatcher)
        .findMatch(namespace, name);
  }

  private ComponentNameMatcher getMatcher(String format) {
    return new ComponentNameMatcher(format, proprietaryComponentNamePatternDAO.getEnabledByFormat(format));
  }

  private ComponentNameMatcher getMatcherWithDisabledPatterns(String format) {
    return new ComponentNameMatcher(format, proprietaryComponentNamePatternDAO.getByFormat(format));
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
  }
}
