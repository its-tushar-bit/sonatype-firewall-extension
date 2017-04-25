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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RepositoryReportDetailTest
{
  private RepositoryReportDetail repositoryReportDetail;

  @Test
  public void testBuildComponentDisplayText_NullSafe() {
    RepositoryComponent component = new RepositoryComponent();
    assertEquals(null, RepositoryReportDetail.buildComponentDisplayText(component));
  }

  @Test
  public void testBuildComponentDisplayText_UseIdentifierWhenAvailable() {
    RepositoryComponent component = new RepositoryComponent();
    component.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    assertEquals("g : a : v", RepositoryReportDetail.buildComponentDisplayText(component));
  }

  @Test
  public void testBuildComponentDisplayText_UsePathnameWhenIdentifierLacksFormat() {
    RepositoryComponent component = new RepositoryComponent();
    component.setPathname("some/dir/test-1.2.zip");
    component.setComponentIdentifier(new ComponentIdentifier());
    assertEquals("test-1.2.zip (some/dir/test-1.2.zip)", RepositoryReportDetail.buildComponentDisplayText(component));
  }

  @Test
  public void testBuildComponentDisplayText_UsePathnameWhenNoIdentifierAvailable() {
    RepositoryComponent component = new RepositoryComponent();
    component.setPathname("some/dir/test-1.2.zip");
    assertEquals("test-1.2.zip (some/dir/test-1.2.zip)", RepositoryReportDetail.buildComponentDisplayText(component));
  }

  @Test
  public void testBuildComponentDisplayText_UsePathnameWhenNoIdentifierAvailable_NoParentDir() {
    RepositoryComponent component = new RepositoryComponent();
    component.setPathname("test-1.2.zip");
    assertEquals("test-1.2.zip (test-1.2.zip)", RepositoryReportDetail.buildComponentDisplayText(component));
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
        matchStateId, null, null, false);
    component.setQuarantineTime(quarantineTime);

    final RepositoryPolicyViolation violation = new RepositoryPolicyViolation(null, pathname, null, null, policyName,
        threatLevel, null, hash, componentId, "dummy-ConstraintFactsJson");
    violation.setWaived(waived);

    repositoryReportDetail = RepositoryReportDetail.create(component, violation, false);

    assertEquals(componentId, repositoryReportDetail.getComponentIdentifier());
    assertEquals("g : a : v", repositoryReportDetail.getComponentDisplayText());
    assertEquals(pathname, repositoryReportDetail.getPathname());
    assertEquals(hash, repositoryReportDetail.getHash());
    assertEquals(matchStateId, repositoryReportDetail.getMatchState());
    assertTrue(repositoryReportDetail.isQuarantined());
    assertTrue(repositoryReportDetail.isWaived());
    assertEquals(threatLevel, repositoryReportDetail.getThreatLevel());
    assertFalse(repositoryReportDetail.isHighestThreatLevel());
    assertEquals(policyName, repositoryReportDetail.getPolicyName());
  }

  @Test
  public void testCreateWithoutViolation() {
    final ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    final String pathname = "pathname";
    final String hash = "hash";
    final String matchStateId = MatchState.UNKNOWN.getId();
    final Date quarantineTime = new Date();

    final RepositoryComponent component = new RepositoryComponent(null, pathname, null, hash, componentId,
        matchStateId, null, null, false);
    component.setQuarantineTime(quarantineTime);

    repositoryReportDetail = RepositoryReportDetail.create(component);

    assertEquals(componentId, repositoryReportDetail.getComponentIdentifier());
    assertEquals("g : a : v", repositoryReportDetail.getComponentDisplayText());
    assertEquals(pathname, repositoryReportDetail.getPathname());
    assertEquals(hash, repositoryReportDetail.getHash());
    assertEquals(matchStateId, repositoryReportDetail.getMatchState());
    assertTrue(repositoryReportDetail.isQuarantined());
    assertFalse(repositoryReportDetail.isWaived());
    assertEquals(0, repositoryReportDetail.getThreatLevel());
    assertTrue(repositoryReportDetail.isHighestThreatLevel());
    assertNull(null, repositoryReportDetail.getPolicyName());
  }
}
