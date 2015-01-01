/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;
import java.util.Date;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ComponentSummaryServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ComponentSummaryService componentSummaryService;

  private Organization org;
  private Application app1;
  private Application app2;
  private Policy orgPolicy;
  private Policy app1Policy;
  private PolicyEvaluation app1PolicyEvaluation;
  private PolicyEvaluation app2PolicyEvaluation;
  private PolicyViolation orgPolicyViolation;
  private PolicyViolation app1PolicyViolation;
  private PolicyViolation app2PolicyViolation;
  private Tag tag1;
  private Tag tag2;

  @Before
  public void setup() {
    org = tempEntity.newOrganization();
    app1 = tempEntity.newApplication("app1", "app1", org.getId());
    app2 = tempEntity.newApplication("app2", "app2", org.getId());
    orgPolicy = tempEntity.newPolicy(org.getId(), "org owned policy", 3);
    app1Policy = tempEntity.newPolicy(app1.getId(), "app owned policy", 5);
    long time = System.currentTimeMillis() - 1000;
    app1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "test scan app1 id",
        new Date(time));
    app2PolicyEvaluation = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "test scan app2 id",
        new Date(time + 1));
    orgPolicyViolation = tempEntity.newPolicyViolation(app1PolicyEvaluation, orgPolicy);
    app1PolicyViolation = tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy);
    app2PolicyViolation = tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy);
    tempEntity.newFirstOccurrencePolicyViolation(orgPolicyViolation.getId(), app1.getId(), BuildStageType.ID);
    tempEntity.newFirstOccurrencePolicyViolation(app1PolicyViolation.getId(), app1.getId(), BuildStageType.ID);
    tempEntity.newFirstOccurrencePolicyViolation(app2PolicyViolation.getId(), app2.getId(), BuildStageType.ID);
    tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, "hash-1",
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"));
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, "hash-3", MatchState.SIMILAR, false);
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, "hash-4", MatchState.UNKNOWN, false);
    tempEntity.newApplicationComponent(app2.getId(), BuildStageType.ID, "hash-2",
        ComponentIdentifier.createMavenCoordinates("g", "a", "2"));
    tag1 = tempEntity.newTag(org.getId());
    tag2 = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app1.getId(), tag1.getId());
    tempEntity.newApplicationTag(app1.getId(), tag2.getId());
  }

  @Test
  public void testGetComponentSummary_NoFilter() throws Exception {
    ComponentSummaryDTO summary = componentSummaryService.getComponentSummary(null, null, null);
    assertThat(summary.total, is(4));
    assertThat(summary.exact, is(2));
    assertThat(summary.similar, is(1));
    assertThat(summary.unknown, is(1));
  }

  @Test
  public void testGetComponentSummary_FilterByApp() throws Exception {
    ComponentSummaryDTO summary = componentSummaryService.getComponentSummary(Collections.singleton(app1.getId()),
        null, null);
    assertThat(summary.total, is(3));
    assertThat(summary.exact, is(1));
    assertThat(summary.similar, is(1));
    assertThat(summary.unknown, is(1));
  }

  @Test
  public void testGetComponentSummary_FilterByTag() throws Exception {
    Tag app2Tag = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app2.getId(), app2Tag.getId());

    ComponentSummaryDTO summary = componentSummaryService.getComponentSummary(null, null,
        Collections.singleton(app2Tag.getId()));
    assertThat(summary.total, is(1));
    assertThat(summary.exact, is(1));
    assertThat(summary.similar, is(0));
    assertThat(summary.unknown, is(0));
  }

  @Test
  public void testGetComponentSummary_FilterByStage() throws Exception {
    ComponentSummaryDTO summary = componentSummaryService.getComponentSummary(null,
        Collections.singleton(ReleaseStageType.ID), null);
    assertThat(summary.total, is(2));
    assertThat(summary.exact, is(0));
    assertThat(summary.similar, is(1));
    assertThat(summary.unknown, is(1));
  }

  @Test
  public void testGetComponentSummary_ExcludesProprietaryComponents() throws Exception {
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, "hash-x", MatchState.SIMILAR, true);

    ComponentSummaryDTO summary = componentSummaryService.getComponentSummary(null, null, null);
    assertThat(summary.total, is(4));
    assertThat(summary.exact, is(2));
    assertThat(summary.similar, is(1));
    assertThat(summary.unknown, is(1));
  }

  @Test
  public void testGetComponentSummary_UsesMostRecentMatchState() throws Exception {
    tempEntity.newApplicationComponent(app2.getId(), ReleaseStageType.ID, "hash-1", MatchState.SIMILAR, false);

    ComponentSummaryDTO summary = componentSummaryService.getComponentSummary(null, null, null);
    assertThat(summary.total, is(4));
    assertThat(summary.exact, is(1));
    assertThat(summary.similar, is(2));
    assertThat(summary.unknown, is(1));
  }
}
