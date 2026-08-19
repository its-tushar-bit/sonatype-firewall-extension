/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.insight.brain.dataaccess.configuration.FirewallIgnorePatternsDAO;
import com.sonatype.insight.brain.model.repository.Repository;

@Named
@Singleton
public class FirewallIgnorePatternService
{
  private final FirewallIgnorePatternsDAO firewallIgnorePatternsDAO;

  private final FirewallIgnorePatternUpdater firewallIgnorePatternUpdater;

  @Inject
  public FirewallIgnorePatternService(
      FirewallIgnorePatternsDAO firewallIgnorePatternsDAO,
      FirewallIgnorePatternUpdater firewallIgnorePatternUpdater)
  {
    this.firewallIgnorePatternsDAO = firewallIgnorePatternsDAO;
    this.firewallIgnorePatternUpdater = firewallIgnorePatternUpdater;
  }

  public Predicate<String> componentPathnameMatchesIgnorePattern(Repository repository) {
    FirewallIgnorePatterns firewallIgnorePatterns = getIgnorePatterns();

    List<String> regexForRepository = firewallIgnorePatterns.regexpsByRepositoryFormat.get(repository.getFormat());
    if (regexForRepository == null) {
      return componentPathname -> false;
    }

    List<Pattern> patterns = regexForRepository.stream().map(Pattern::compile).collect(Collectors.toList());
    return componentPathname -> patterns.stream().anyMatch(pattern -> pattern.matcher(componentPathname).matches());
  }

  public FirewallIgnorePatterns getIgnorePatterns() {
    com.sonatype.insight.brain.model.configuration.FirewallIgnorePatterns firewallIgnorePatterns =
        firewallIgnorePatternsDAO.get();
    if (firewallIgnorePatterns.getFirewallIgnorePatterns() == null) {
      firewallIgnorePatternUpdater.updateFirewallIgnorePatterns();
      firewallIgnorePatterns = firewallIgnorePatternsDAO.get();
    }
    return firewallIgnorePatterns.getFirewallIgnorePatterns();
  }
}
