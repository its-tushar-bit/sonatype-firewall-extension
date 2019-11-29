/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @since 1.17
 */
public class FirewallIgnorePatternServiceTest
    extends AbstractComponentTest
{
  @Inject
  private FirewallIgnorePatternService firewallIgnorePatternService;

  @Mock
  private HdsClient hdsClient;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(hdsClient);
    super.configure(binder);
  }

  @Test
  public void testGetIgnorePatterns() throws Exception {
    // Prepare request and mock the HDS request
    FirewallIgnorePatterns hdsResult = new FirewallIgnorePatterns();
    hdsResult.regexpsByRepositoryFormat = new HashMap<>();
    hdsResult.regexpsByRepositoryFormat.put("foo", Collections.singletonList("bar"));
    when(hdsClient.get(eq(FirewallIgnorePatterns.class), eq(FirewallIgnorePatternService.HDS_IGNORE_PATTERNS_PATH)))
        .thenReturn(hdsResult);

    // Call the service
    FirewallIgnorePatterns firewallIgnorePatterns = firewallIgnorePatternService.getIgnorePatterns();

    assertThat(firewallIgnorePatterns).isEqualTo(hdsResult);
  }

  @Test
  public void testComponentPathnameMatchesIgnorePattern_NullRepositoryFormat() {
    FirewallIgnorePatterns firewallIgnorePatterns = new FirewallIgnorePatterns();
    firewallIgnorePatterns.regexpsByRepositoryFormat = new HashMap<>();
    when(hdsClient.get(eq(FirewallIgnorePatterns.class), eq(FirewallIgnorePatternService.HDS_IGNORE_PATTERNS_PATH)))
        .thenReturn(firewallIgnorePatterns);
    assertThat(firewallIgnorePatternService.componentPathnameMatchesIgnorePattern(new Repository())).rejects("any");
  }

  @Test
  public void testComponentPathnameMatchesIgnorePattern_NullRegexps() {
    Repository repository = new Repository();
    repository.setFormat("maven2");
    FirewallIgnorePatterns firewallIgnorePatterns = new FirewallIgnorePatterns();
    firewallIgnorePatterns.regexpsByRepositoryFormat = new HashMap<>();
    when(hdsClient.get(eq(FirewallIgnorePatterns.class), eq(FirewallIgnorePatternService.HDS_IGNORE_PATTERNS_PATH)))
        .thenReturn(firewallIgnorePatterns);

    assertThat(firewallIgnorePatternService.componentPathnameMatchesIgnorePattern(repository)).rejects("any");
  }

  @Test
  public void testComponentPathnameMatchesIgnorePattern_NonMatchingFormat() {
    Repository repository = new Repository();
    repository.setFormat("other");
    FirewallIgnorePatterns firewallIgnorePatterns = new FirewallIgnorePatterns();
    firewallIgnorePatterns.regexpsByRepositoryFormat = new HashMap<>();
    firewallIgnorePatterns.regexpsByRepositoryFormat.put("maven2", Arrays.asList("a", "b"));
    when(hdsClient.get(eq(FirewallIgnorePatterns.class), eq(FirewallIgnorePatternService.HDS_IGNORE_PATTERNS_PATH)))
        .thenReturn(firewallIgnorePatterns);

    assertThat(firewallIgnorePatternService.componentPathnameMatchesIgnorePattern(repository)).rejects("a", "b", "any");
  }

  @Test
  public void testComponentPathnameMatchesIgnorePattern_MatchingFormat() {
    Repository repository = new Repository();
    repository.setFormat("maven2");
    FirewallIgnorePatterns firewallIgnorePatterns = new FirewallIgnorePatterns();
    firewallIgnorePatterns.regexpsByRepositoryFormat = new HashMap<>();
    firewallIgnorePatterns.regexpsByRepositoryFormat.put("maven2", Arrays.asList("a", "b"));
    firewallIgnorePatterns.regexpsByRepositoryFormat.put("other", Collections.singletonList("c"));
    when(hdsClient.get(eq(FirewallIgnorePatterns.class), eq(FirewallIgnorePatternService.HDS_IGNORE_PATTERNS_PATH)))
        .thenReturn(firewallIgnorePatterns);

    assertThat(firewallIgnorePatternService.componentPathnameMatchesIgnorePattern(repository)).accepts("a", "b")
        .rejects("c");
  }
}
