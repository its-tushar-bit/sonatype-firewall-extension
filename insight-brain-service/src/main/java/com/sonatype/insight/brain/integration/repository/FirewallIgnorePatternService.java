/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.error.exception.BadGatewayException;

import com.google.common.base.Suppliers;

@Named
@Singleton
public class FirewallIgnorePatternService
{
  public static final String HDS_IGNORE_PATTERNS_PATH = "rest/component/details/firewall/ignorePatterns";

  private final Supplier<FirewallIgnorePatterns> ignorePatternsCache;

  private final HdsClient hdsClient;

  public boolean disableCacheForTesting;

  @Inject
  public FirewallIgnorePatternService(HdsClient hdsClient) {
    this.hdsClient = hdsClient;
    this.ignorePatternsCache = Suppliers.memoizeWithExpiration(this::fetchFirewallIgnorePatterns, 6, TimeUnit.HOURS);
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
    return disableCacheForTesting ? fetchFirewallIgnorePatterns() : ignorePatternsCache.get();
  }

  private FirewallIgnorePatterns fetchFirewallIgnorePatterns() {
    try {
      return hdsClient.get(FirewallIgnorePatterns.class, HDS_IGNORE_PATTERNS_PATH);
    }
    catch (BadGatewayException e) {
      throw new RuntimeException("Failed to get ignore patterns from remote: " + e.getMessage(), e);
    }
  }
}
