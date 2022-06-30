/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @deprecated The tested class is deprecated. To be removed when the Repository Results View migration to React is
 * completed (Epic: https://issues.sonatype.org/browse/CLM-20597)
 */
@Deprecated
public class RepositoryReportDetailTest
{
  private RepositoryReportDetail repositoryReportDetail;

  @Test
  public void testBuildComponentDisplayText_NullSafe() {
    RepositoryComponent component = new RepositoryComponent();
    assertThat(RepositoryReportDetail.buildComponentDisplayText(component)).isNull();
  }

  @Test
  public void testBuildComponentDisplayText_UseIdentifierWhenAvailable() {
    RepositoryComponent component = new RepositoryComponent();
    component.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    assertThat(RepositoryReportDetail.buildComponentDisplayText(component)).isEqualTo("g : a : v");
  }

  @Test
  public void testBuildComponentDisplayText_UsePathnameWhenIdentifierLacksFormat() {
    RepositoryComponent component = new RepositoryComponent();
    component.setPathname("some/dir/test-1.2.zip");
    component.setComponentIdentifier(new ComponentIdentifier());
    assertThat(RepositoryReportDetail.buildComponentDisplayText(component))
        .isEqualTo("test-1.2.zip (some/dir/test-1.2.zip)");
  }

  @Test
  public void testBuildComponentDisplayText_UsePathnameWhenNoIdentifierAvailable() {
    RepositoryComponent component = new RepositoryComponent();
    component.setPathname("some/dir/test-1.2.zip");
    assertThat(RepositoryReportDetail.buildComponentDisplayText(component))
        .isEqualTo("test-1.2.zip (some/dir/test-1.2.zip)");
  }

  @Test
  public void testBuildComponentDisplayText_UsePathnameWhenNoIdentifierAvailable_NoParentDir() {
    RepositoryComponent component = new RepositoryComponent();
    component.setPathname("test-1.2.zip");
    assertThat(RepositoryReportDetail.buildComponentDisplayText(component)).isEqualTo("test-1.2.zip (test-1.2.zip)");
  }

  @Test
  public void testCreateWithViolation() {
    final ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    final String pathname = "pathname";
    final String hash = "hash";
    final String matchStateId = MatchState.EXACT.getId();
    final Date quarantineTime = new Date();
    final boolean waived = true;
    final int threatLevel = 5;
    final String policyName = "policyName";

    final RepositoryComponent component = new RepositoryComponent(null, pathname, null, hash, componentId,
        matchStateId, null, null);
    component.setQuarantineTime(quarantineTime);

    final RepositoryPolicyViolation violation = new RepositoryPolicyViolation(null, pathname, null, null, policyName,
        threatLevel, null, hash, componentId, "dummy-ConstraintFactsJson");
    violation.setWaived(waived);

    repositoryReportDetail = RepositoryReportDetail.create(component, violation, false);

    assertThat(repositoryReportDetail.getComponentIdentifier()).isEqualByComparingTo(componentId);
    assertThat(repositoryReportDetail.getComponentDisplayText()).isEqualTo("g : a : v");
    assertThat(repositoryReportDetail.getPathname()).isEqualTo(pathname);
    assertThat(repositoryReportDetail.getHash()).isEqualTo(hash);
    assertThat(repositoryReportDetail.getMatchState()).isEqualTo(matchStateId);
    assertThat(repositoryReportDetail.isQuarantined()).isTrue();
    assertThat(repositoryReportDetail.isWaived()).isTrue();
    assertThat(repositoryReportDetail.getThreatLevel()).isEqualTo(threatLevel);
    assertThat(repositoryReportDetail.isHighestThreatLevel()).isFalse();
    assertThat(repositoryReportDetail.getPolicyName()).isEqualTo(policyName);
  }

  @Test
  public void testCreateWithoutViolation() {
    final ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    final String pathname = "pathname";
    final String hash = "hash";
    final String matchStateId = MatchState.UNKNOWN.getId();
    final Date quarantineTime = new Date();

    final RepositoryComponent component = new RepositoryComponent(null, pathname, null, hash, componentId,
        matchStateId, null, null);
    component.setQuarantineTime(quarantineTime);

    repositoryReportDetail = RepositoryReportDetail.create(component);

    assertThat(repositoryReportDetail.getComponentIdentifier()).isEqualTo(componentId);
    assertThat(repositoryReportDetail.getComponentDisplayText()).isEqualTo("g : a : v");
    assertThat(repositoryReportDetail.getPathname()).isEqualTo(pathname);
    assertThat(repositoryReportDetail.getHash()).isEqualTo(hash);
    assertThat(repositoryReportDetail.getMatchState()).isEqualTo(matchStateId);
    assertThat(repositoryReportDetail.isQuarantined()).isTrue();
    assertThat(repositoryReportDetail.isWaived()).isFalse();
    assertThat(repositoryReportDetail.getThreatLevel()).isEqualTo(0);
    assertThat(repositoryReportDetail.isHighestThreatLevel()).isTrue();
    assertThat(repositoryReportDetail.getPolicyName()).isNull();
  }
}
