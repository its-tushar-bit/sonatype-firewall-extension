/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.integration.repository.FirewallIgnorePatternUpdater;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetry.RepositoryComponentTelemetryEventType;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetryCreator;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static com.sonatype.insight.brain.model.component.MatchState.EXACT;
import static com.sonatype.insight.brain.model.component.MatchState.UNKNOWN;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class RepositoryComponentDeleteServiceTest
    extends AbstractComponentTest
{
  @Inject
  private RepositoryComponentDeleteService repositoryComponentDeleteService;

  @Inject
  private RepositoryComponentDAO repositoryComponentDAO;

  @Inject
  private PolicyWaiverDAO policyWaiverDAO;

  @Inject
  private RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  @Inject
  private ComponentLabelDAO componentLabelDAO;

  @Mock
  private HdsClient hdsClientMock;

  @Mock
  private RepositoryComponentTelemetryCreator repositoryComponentTelemetryCreator;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(hdsClientMock);
    binder.bind(RepositoryComponentTelemetryCreator.class).toInstance(repositoryComponentTelemetryCreator);
    super.configure(binder);
  }

  @Test
  public void testDeleteUnknownIgnoredComponents() {
    String repositoryFormat = "maven2";
    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    Repository repository = tempEntity.newRepository("rm1", "r1", repositoryFormat);
    Policy policy = tempEntity.newPolicy();

    FirewallIgnorePatterns hdsResult = new FirewallIgnorePatterns();
    hdsResult.regexpsByRepositoryFormat = new HashMap<>();
    hdsResult.regexpsByRepositoryFormat.put(repository.getFormat(), Collections.singletonList(".*sha$"));
    when(hdsClientMock.get(eq(FirewallIgnorePatterns.class), eq(FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH)))
        .thenReturn(hdsResult);

    // The following component is unknown and matches the ignore patterns from HDS.
    // The component, all its violations, labels and waivers must be deleted.
    RepositoryComponent component = tempEntity.newRepositoryComponent(repository, "unknown/sha", UNKNOWN, "hash");
    RepositoryPolicyViolation violation = tempEntity.newRepositoryPolicyViolation(component, policy.getId());
    tempEntity.newWaiver(component.getHash(), policy.getId(), repository.getId());
    tempEntity.newComponentLabel(component, label);

    // same component label as above just at a higher org level, must be kept
    ComponentLabel componentLabel1 =
        tempEntity.newComponentLabel(Organization.ROOT_ORGANIZATION_ID, label.getId(), component.getHash());

    // We do not want to delete any policy violations or waivers that reside in other repositories
    // or unrelated to our unknown component, but reference the same policy.
    Repository repo2 = tempEntity.newRepository("rm2", "r2", repositoryFormat);
    RepositoryComponent component2 = tempEntity.newRepositoryComponent(repo2, "exact/jar", EXACT, "hash2");
    RepositoryPolicyViolation violation2 = tempEntity.newRepositoryPolicyViolation(component2, policy.getId());
    PolicyWaiver waiver2 = tempEntity.newWaiver(component2.getHash(), policy.getId(), repo2.getId());
    ComponentLabel componentLabel2 = tempEntity.newComponentLabel(component2, label);

    repositoryComponentDeleteService.deleteUnknownIgnoredComponents(repository);

    assertThat(reload(component)).isNull();
    assertThat(reload(component2)).isNotNull();

    assertThat(reload(violation)).isNull();
    assertThat(reload(violation2)).isNotNull();

    assertThat(getPolicyWaiverIdsOf(policy)).containsOnly(waiver2.getId());

    assertThat(getComponentLabelIds(label)).containsOnly(componentLabel1.getId(), componentLabel2.getId());

    verify(repositoryComponentTelemetryCreator)
        .sendRepositoryComponentTelemetry(any(), any(), eq(repository.getRepositoryManagerId()),
            eq(repository.getPublicId()), eq(RepositoryComponentTelemetryEventType.DELETE), any(), any());
  }

  @Test
  public void testDeleteUnknownIgnoredComponents_MustNotDeleteExactMatchingComponent() {
    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    Repository repository = tempEntity.newRepository("rm1", "r1", "maven2");
    Policy policy = tempEntity.newPolicy();

    FirewallIgnorePatterns hdsResult = new FirewallIgnorePatterns();
    hdsResult.regexpsByRepositoryFormat = new HashMap<>();
    hdsResult.regexpsByRepositoryFormat.put(repository.getFormat(), Collections.singletonList(".*sha$"));
    when(hdsClientMock.get(eq(FirewallIgnorePatterns.class), eq(FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH)))
        .thenReturn(hdsResult);

    RepositoryComponent component = tempEntity.newRepositoryComponent(repository, "unknown/sha", EXACT, "hash");
    RepositoryPolicyViolation violation = tempEntity.newRepositoryPolicyViolation(component, policy.getId());
    PolicyWaiver policyWaiver = tempEntity.newWaiver(component.getHash(), policy.getId(), repository.getId());
    ComponentLabel componentLabel = tempEntity.newComponentLabel(component, label);

    repositoryComponentDeleteService.deleteUnknownIgnoredComponents(repository);

    assertThat(reload(component)).isNotNull();
    assertThat(reload(violation)).isNotNull();
    assertThat(getPolicyWaiverIdsOf(policy)).containsOnly(policyWaiver.getId());
    assertThat(getComponentLabelIds(label)).containsOnly(componentLabel.getId());

    verifyNoInteractions(repositoryComponentTelemetryCreator);
  }

  @Test
  public void testDeleteUnknownIgnoredComponents_MustNotDeleteUnknownNotMatchingComponent() {
    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    Repository repository = tempEntity.newRepository("rm1", "r1", "maven2");
    Policy policy = tempEntity.newPolicy();

    FirewallIgnorePatterns hdsResult = new FirewallIgnorePatterns();
    hdsResult.regexpsByRepositoryFormat = new HashMap<>();
    hdsResult.regexpsByRepositoryFormat.put(repository.getFormat(), Collections.singletonList(".*sha$"));
    when(hdsClientMock.get(eq(FirewallIgnorePatterns.class), eq(FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH)))
        .thenReturn(hdsResult);

    RepositoryComponent component = tempEntity.newRepositoryComponent(repository, "unknown/jar", UNKNOWN, "hash");
    RepositoryPolicyViolation violation = tempEntity.newRepositoryPolicyViolation(component, policy.getId());
    PolicyWaiver policyWaiver = tempEntity.newWaiver(component.getHash(), policy.getId(), repository.getId());
    ComponentLabel componentLabel = tempEntity.newComponentLabel(component, label);

    repositoryComponentDeleteService.deleteUnknownIgnoredComponents(repository);

    assertThat(reload(component)).isNotNull();
    assertThat(reload(violation)).isNotNull();
    assertThat(getPolicyWaiverIdsOf(policy)).containsOnly(policyWaiver.getId());
    assertThat(getComponentLabelIds(label)).containsOnly(componentLabel.getId());

    verifyNoInteractions(repositoryComponentTelemetryCreator);
  }

  @Test
  public void testDeleteUnknownIgnoredComponents_MustNotDeleteKnownNotMatchingComponent() {
    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    Repository repository = tempEntity.newRepository("rm1", "r1", "maven2");
    Policy policy = tempEntity.newPolicy();

    FirewallIgnorePatterns hdsResult = new FirewallIgnorePatterns();
    hdsResult.regexpsByRepositoryFormat = new HashMap<>();
    hdsResult.regexpsByRepositoryFormat.put(repository.getFormat(), Collections.singletonList(".*sha$"));
    when(hdsClientMock.get(eq(FirewallIgnorePatterns.class), eq(FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH)))
        .thenReturn(hdsResult);

    RepositoryComponent component = tempEntity.newRepositoryComponent(repository, "unknown/jar", EXACT, "hash");
    RepositoryPolicyViolation violation = tempEntity.newRepositoryPolicyViolation(component, policy.getId());
    PolicyWaiver policyWaiver = tempEntity.newWaiver(component.getHash(), policy.getId(), repository.getId());
    ComponentLabel componentLabel = tempEntity.newComponentLabel(component, label);

    repositoryComponentDeleteService.deleteUnknownIgnoredComponents(repository);

    assertThat(reload(component)).isNotNull();
    assertThat(reload(violation)).isNotNull();
    assertThat(getPolicyWaiverIdsOf(policy)).containsOnly(policyWaiver.getId());
    assertThat(getComponentLabelIds(label)).containsOnly(componentLabel.getId());

    verifyNoInteractions(repositoryComponentTelemetryCreator);
  }

  @Test
  public void testDeleteUnknownIgnoredComponents_RepositoryFormatConsidered() {
    // Prepare data
    Repository repository = tempEntity.newRepository("rm1", "r1", "maven2");

    FirewallIgnorePatterns hdsResult = new FirewallIgnorePatterns();
    hdsResult.regexpsByRepositoryFormat = new HashMap<>();
    hdsResult.regexpsByRepositoryFormat.put("some_format_different_than_our_repos_format",
        Collections.singletonList(".*sha"));
    when(hdsClientMock.get(eq(FirewallIgnorePatterns.class), eq(FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH)))
        .thenReturn(hdsResult);

    // The following component is unknown and matches the ignore patterns,
    // but ignore patterns of a different repository format!
    RepositoryComponent unknownSha = tempEntity.newRepositoryComponent(repository, "unknown/sha", UNKNOWN, "hash");
    RepositoryPolicyViolation unknownShaViolation =
        tempEntity.newRepositoryPolicyViolation(unknownSha.getRepositoryId(), unknownSha.getPathname());

    // Action
    // This repository has no matching ignore patterns
    repositoryComponentDeleteService.deleteUnknownIgnoredComponents(repository);

    // Assertions
    assertThat(repositoryComponentDAO.getById(unknownSha.getId())).isNotNull();
    assertThat(repositoryPolicyViolationDAO.getById(unknownShaViolation.getId())).isNotNull();

    verifyNoInteractions(repositoryComponentTelemetryCreator);
  }

  @Test
  public void testDeleteUnknownIgnoredComponents_EmptyPattern() {
    // Prepare data
    Repository repository = tempEntity.newRepository("rm1", "r1", "maven2");

    FirewallIgnorePatterns hdsResult = new FirewallIgnorePatterns();
    hdsResult.regexpsByRepositoryFormat = new HashMap<>();
    hdsResult.regexpsByRepositoryFormat.put(repository.getFormat(), new ArrayList<>());
    when(hdsClientMock.get(eq(FirewallIgnorePatterns.class), eq(FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH)))
        .thenReturn(hdsResult);

    // For this repository format, there are no ignore patterns.
    RepositoryComponent unknownSha = tempEntity.newRepositoryComponent(repository, "unknown/sha", UNKNOWN, "hash");
    RepositoryPolicyViolation unknownShaViolation =
        tempEntity.newRepositoryPolicyViolation(unknownSha.getRepositoryId(), unknownSha.getPathname());

    // Action
    repositoryComponentDeleteService.deleteUnknownIgnoredComponents(repository);

    // Assertions
    assertThat(repositoryComponentDAO.getById(unknownSha.getId())).isNotNull();
    assertThat(repositoryPolicyViolationDAO.getById(unknownShaViolation.getId())).isNotNull();

    verifyNoInteractions(repositoryComponentTelemetryCreator);
  }

  @Test
  public void testDeleteUnknownIgnoredComponents_NullRepositoryFormat() {
    // Prepare data
    Repository repository = tempEntity.newRepository("rm1", "r1", null);

    // For null repository format, there are no ignore patterns.
    FirewallIgnorePatterns hdsResult = new FirewallIgnorePatterns();
    hdsResult.regexpsByRepositoryFormat = new HashMap<>();
    when(hdsClientMock.get(eq(FirewallIgnorePatterns.class), eq(FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH)))
        .thenReturn(hdsResult);
    RepositoryComponent unknownSha = tempEntity.newRepositoryComponent(repository, "unknown/sha", UNKNOWN, "hash");
    RepositoryPolicyViolation unknownShaViolation =
        tempEntity.newRepositoryPolicyViolation(unknownSha.getRepositoryId(), unknownSha.getPathname());

    // Action
    repositoryComponentDeleteService.deleteUnknownIgnoredComponents(repository);

    // Assertions
    assertThat(reload(unknownSha)).isNotNull();
    assertThat(reload(unknownShaViolation)).isNotNull();

    verifyNoInteractions(repositoryComponentTelemetryCreator);
  }

  @Test
  public void testDeleteComponent_Telemetry() {
    // setup
    String repositoryFormat = "maven2";
    Repository repository = tempEntity.newRepository("rm1", "r1", repositoryFormat);
    Policy policy = tempEntity.newPolicy();

    RepositoryComponent component = tempEntity.newRepositoryComponent(repository, "pathname", UNKNOWN, "hash");
    tempEntity.newRepositoryPolicyViolation(component, policy.getId());

    RepositoryComponent component2 = tempEntity.newRepositoryComponent(repository, "pathname2", EXACT, "hash2");

    // when: a components are deleted
    repositoryComponentDeleteService.deleteComponent(component);
    repositoryComponentDeleteService.deleteComponent(component2);

    // then: telemetry is only sent for components with violations
    verify(repositoryComponentTelemetryCreator)
        .sendRepositoryComponentTelemetry(any(), any(), eq(repository.getRepositoryManagerId()),
            eq(repository.getPublicId()), eq(RepositoryComponentTelemetryEventType.DELETE), any(), any());
    verifyNoMoreInteractions(repositoryComponentTelemetryCreator);
  }

  private RepositoryComponent reload(RepositoryComponent repositoryComponent) {
    return repositoryComponentDAO.getById(repositoryComponent.getId());
  }

  private RepositoryPolicyViolation reload(RepositoryPolicyViolation policyViolation) {
    return repositoryPolicyViolationDAO.getById(policyViolation.getId());
  }

  private List<String> getPolicyWaiverIdsOf(Policy policy) {
    return policyWaiverDAO.getByPolicyId(policy.getId()).stream().map(PolicyWaiver::getId).collect(toList());
  }

  private List<String> getComponentLabelIds(Label label) {
    return componentLabelDAO.getByLabelId(label.getId()).stream().map(ComponentLabel::getId).collect(toList());
  }
}
