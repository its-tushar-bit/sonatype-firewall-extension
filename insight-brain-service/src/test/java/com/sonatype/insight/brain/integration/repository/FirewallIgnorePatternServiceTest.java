/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.insight.brain.dataaccess.configuration.FirewallIgnorePatternsDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadGatewayException;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @since 1.17
 */
public class FirewallIgnorePatternServiceTest
    extends AbstractComponentTest
{
  @Inject
  private FirewallIgnorePatternService firewallIgnorePatternService;

  @Inject
  private FirewallIgnorePatternsDAO firewallIgnorePatternsDAO;

  @Mock
  private HdsClient hdsClientMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(hdsClientMock);

    super.configure(binder);
  }

  @Test
  public void testGetIgnorePatterns_NullPatterns_Updates() {
    assertFirewallIgnorePatterns(firewallIgnorePatternsDAO.get(),
        new com.sonatype.insight.brain.model.configuration.FirewallIgnorePatterns());
    FirewallIgnorePatterns expectedFirewallIgnorePatterns = createFirewallIgnorePatterns();
    when(hdsClientMock.get(FirewallIgnorePatterns.class, FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH))
        .thenReturn(expectedFirewallIgnorePatterns);

    assertThat(firewallIgnorePatternService.getIgnorePatterns()).usingRecursiveComparison()
        .isEqualTo(expectedFirewallIgnorePatterns);
    com.sonatype.insight.brain.model.configuration.FirewallIgnorePatterns firewallIgnorePatterns =
        firewallIgnorePatternsDAO.get();
    assertThat(firewallIgnorePatterns).isNotNull();
    assertThat(firewallIgnorePatterns.getFirewallIgnorePatterns()).usingRecursiveComparison()
        .isEqualTo(expectedFirewallIgnorePatterns);
    verify(hdsClientMock).get(FirewallIgnorePatterns.class, FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH);
  }

  @Test
  public void testGetIgnorePatterns_NotNullPatterns_DoesNotUpdate() {
    tempEntity.setFirewallIgnorePatterns(createFirewallIgnorePatterns());

    assertThat(firewallIgnorePatternService.getIgnorePatterns()).usingRecursiveComparison()
        .isEqualTo(firewallIgnorePatternsDAO.get().getFirewallIgnorePatterns());
    verify(hdsClientMock, never())
        .get(FirewallIgnorePatterns.class, FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH);
  }

  @Test
  public void testGetIgnorePatterns_HDS_Fails_StillUpdates() {
    FirewallIgnorePatterns expectedFirewallIgnorePatterns = createFirewallIgnorePatterns();
    when(hdsClientMock.get(FirewallIgnorePatterns.class, FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH))
        .thenThrow(new BadGatewayException("ERROR"))
        .thenReturn(expectedFirewallIgnorePatterns);
    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> firewallIgnorePatternService.getIgnorePatterns())
        .withMessageContaining("Failed to get ignore patterns from remote");
    assertFirewallIgnorePatterns(firewallIgnorePatternsDAO.get(),
        new com.sonatype.insight.brain.model.configuration.FirewallIgnorePatterns());
    verify(hdsClientMock).get(FirewallIgnorePatterns.class, FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH);

    assertThat(firewallIgnorePatternService.getIgnorePatterns()).usingRecursiveComparison()
        .isEqualTo(expectedFirewallIgnorePatterns);
    com.sonatype.insight.brain.model.configuration.FirewallIgnorePatterns firewallIgnorePatterns =
        firewallIgnorePatternsDAO.get();
    assertThat(firewallIgnorePatterns).isNotNull();
    assertThat(firewallIgnorePatterns.getFirewallIgnorePatterns()).usingRecursiveComparison()
        .isEqualTo(expectedFirewallIgnorePatterns);
    verify(hdsClientMock, times(2))
        .get(FirewallIgnorePatterns.class, FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH);
  }

  @Test
  public void testComponentPathnameMatchesIgnorePattern_NullRepositoryFormat() {
    FirewallIgnorePatterns firewallIgnorePatterns = new FirewallIgnorePatterns();
    firewallIgnorePatterns.regexpsByRepositoryFormat = new HashMap<>();
    when(hdsClientMock.get(eq(FirewallIgnorePatterns.class), eq(FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH)))
        .thenReturn(firewallIgnorePatterns);
    assertThat(firewallIgnorePatternService.componentPathnameMatchesIgnorePattern(new Repository())).rejects("any");
  }

  @Test
  public void testComponentPathnameMatchesIgnorePattern_NullRegexps() {
    Repository repository = new Repository();
    repository.setFormat("maven2");
    FirewallIgnorePatterns firewallIgnorePatterns = new FirewallIgnorePatterns();
    firewallIgnorePatterns.regexpsByRepositoryFormat = new HashMap<>();
    when(hdsClientMock.get(eq(FirewallIgnorePatterns.class), eq(FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH)))
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
    when(hdsClientMock.get(eq(FirewallIgnorePatterns.class), eq(FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH)))
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
    when(hdsClientMock.get(eq(FirewallIgnorePatterns.class), eq(FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH)))
        .thenReturn(firewallIgnorePatterns);

    assertThat(firewallIgnorePatternService.componentPathnameMatchesIgnorePattern(repository)).accepts("a", "b")
        .rejects("c");
  }

  private FirewallIgnorePatterns createFirewallIgnorePatterns() {
    FirewallIgnorePatterns firewallIgnorePatterns = new FirewallIgnorePatterns();
    firewallIgnorePatterns.regexpsByRepositoryFormat.put("format1", Arrays.asList("a", "b"));
    firewallIgnorePatterns.regexpsByRepositoryFormat.put("format2", Collections.singletonList("c"));
    return firewallIgnorePatterns;
  }

  private void assertFirewallIgnorePatterns(
      com.sonatype.insight.brain.model.configuration.FirewallIgnorePatterns actual,
      com.sonatype.insight.brain.model.configuration.FirewallIgnorePatterns expected)
  {
    assertThat(actual).isNotNull();
    assertThat(actual.getId()).isEqualTo(FirewallIgnorePatternsDAO.SINGLETON_ENTITY_ID);
    assertThat(actual.getFirewallIgnorePatterns()).usingRecursiveComparison()
        .isEqualTo(expected.getFirewallIgnorePatterns());
  }
}
