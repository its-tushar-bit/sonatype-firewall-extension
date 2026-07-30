/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

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

import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.ProxyRepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.integration.repository.FirewallIgnorePatternUpdater;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.ProxyRepositoryComponentTelemetry.RepositoryComponentTelemetryEventType;
import com.sonatype.insight.brain.telemetry.ProxyRepositoryComponentTelemetryCreator;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.junit.Test;
import org.mockito.Mock;

public class ProxyRepositoryComponentDeleteServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ProxyRepositoryComponentDeleteService proxyRepositoryComponentDeleteService;

  @Inject
  private ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  @Inject
  private PolicyWaiverDAO policyWaiverDAO;

  @Inject
  private ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO;

  @Inject
  private ComponentLabelDAO componentLabelDAO;

  @Mock
  private HdsClient hdsClientMock;

  @Mock
  private ProxyRepositoryComponentTelemetryCreator proxyRepositoryComponentTelemetryCreator;

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
    ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(repository, "unknown/sha", UNKNOWN, "hash");
    ProxyRepositoryPolicyViolation violation = tempEntity.newRepositoryPolicyViolation(component, policy.getId());
    tempEntity.newWaiver(component.getHash(), policy.getId(), repository.getId());
    tempEntity.newComponentLabel(component, label);

    // same component label as above just at a higher org level, must be kept
    ComponentLabel componentLabel1 =
        tempEntity.newComponentLabel(Organization.ROOT_ORGANIZATION_ID, label.getId(), component.getHash());

    // We do not want to delete any policy violations or waivers that reside in other repositories
    // or unrelated to our unknown component, but reference the same policy.
    Repository repo2 = tempEntity.newRepository("rm2", "r2", repositoryFormat);
    ProxyRepositoryComponent component2 = tempEntity.newRepositoryComponent(repo2, "exact/jar", EXACT, "hash2");
    ProxyRepositoryPolicyViolation violation2 = tempEntity.newRepositoryPolicyViolation(component2, policy.getId());
    PolicyWaiver waiver2 = tempEntity.newWaiver(component2.getHash(), policy.getId(), repo2.getId());
    ComponentLabel componentLabel2 = tempEntity.newComponentLabel(component2, label);

    proxyRepositoryComponentDeleteService.deleteUnknownIgnoredComponents(repository);

    assertThat(reload(component)).isNull();
    assertThat(reload(component2)).isNotNull();

    assertThat(reload(violation)).isNull();
    assertThat(reload(violation2)).isNotNull();

    assertThat(getPolicyWaiverIdsOf(policy)).containsOnly(waiver2.getId());

    assertThat(getComponentLabelIds(label)).containsOnly(componentLabel1.getId(), componentLabel2.getId());

    verify(proxyRepositoryComponentTelemetryCreator)
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

    ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(repository, "unknown/sha", EXACT, "hash");
    ProxyRepositoryPolicyViolation violation = tempEntity.newRepositoryPolicyViolation(component, policy.getId());
    PolicyWaiver policyWaiver = tempEntity.newWaiver(component.getHash(), policy.getId(), repository.getId());
    ComponentLabel componentLabel = tempEntity.newComponentLabel(component, label);

    proxyRepositoryComponentDeleteService.deleteUnknownIgnoredComponents(repository);

    assertThat(reload(component)).isNotNull();
    assertThat(reload(violation)).isNotNull();
    assertThat(getPolicyWaiverIdsOf(policy)).containsOnly(policyWaiver.getId());
    assertThat(getComponentLabelIds(label)).containsOnly(componentLabel.getId());

    verifyNoInteractions(proxyRepositoryComponentTelemetryCreator);
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

    ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(repository, "unknown/jar", UNKNOWN, "hash");
    ProxyRepositoryPolicyViolation violation = tempEntity.newRepositoryPolicyViolation(component, policy.getId());
    PolicyWaiver policyWaiver = tempEntity.newWaiver(component.getHash(), policy.getId(), repository.getId());
    ComponentLabel componentLabel = tempEntity.newComponentLabel(component, label);

    proxyRepositoryComponentDeleteService.deleteUnknownIgnoredComponents(repository);

    assertThat(reload(component)).isNotNull();
    assertThat(reload(violation)).isNotNull();
    assertThat(getPolicyWaiverIdsOf(policy)).containsOnly(policyWaiver.getId());
    assertThat(getComponentLabelIds(label)).containsOnly(componentLabel.getId());

    verifyNoInteractions(proxyRepositoryComponentTelemetryCreator);
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

    ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(repository, "unknown/jar", EXACT, "hash");
    ProxyRepositoryPolicyViolation violation = tempEntity.newRepositoryPolicyViolation(component, policy.getId());
    PolicyWaiver policyWaiver = tempEntity.newWaiver(component.getHash(), policy.getId(), repository.getId());
    ComponentLabel componentLabel = tempEntity.newComponentLabel(component, label);

    proxyRepositoryComponentDeleteService.deleteUnknownIgnoredComponents(repository);

    assertThat(reload(component)).isNotNull();
    assertThat(reload(violation)).isNotNull();
    assertThat(getPolicyWaiverIdsOf(policy)).containsOnly(policyWaiver.getId());
    assertThat(getComponentLabelIds(label)).containsOnly(componentLabel.getId());

    verifyNoInteractions(proxyRepositoryComponentTelemetryCreator);
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
    ProxyRepositoryComponent unknownSha = tempEntity.newRepositoryComponent(repository, "unknown/sha", UNKNOWN, "hash");
    ProxyRepositoryPolicyViolation unknownShaViolation =
        tempEntity.newRepositoryPolicyViolation(unknownSha.getRepositoryId(), unknownSha.getPathname());

    // Action
    // This repository has no matching ignore patterns
    proxyRepositoryComponentDeleteService.deleteUnknownIgnoredComponents(repository);

    // Assertions
    assertThat(proxyRepositoryComponentDAO.getById(unknownSha.getId())).isNotNull();
    assertThat(proxyRepositoryPolicyViolationDAO.getById(unknownShaViolation.getId())).isNotNull();

    verifyNoInteractions(proxyRepositoryComponentTelemetryCreator);
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
    ProxyRepositoryComponent unknownSha = tempEntity.newRepositoryComponent(repository, "unknown/sha", UNKNOWN, "hash");
    ProxyRepositoryPolicyViolation unknownShaViolation =
        tempEntity.newRepositoryPolicyViolation(unknownSha.getRepositoryId(), unknownSha.getPathname());

    // Action
    proxyRepositoryComponentDeleteService.deleteUnknownIgnoredComponents(repository);

    // Assertions
    assertThat(proxyRepositoryComponentDAO.getById(unknownSha.getId())).isNotNull();
    assertThat(proxyRepositoryPolicyViolationDAO.getById(unknownShaViolation.getId())).isNotNull();

    verifyNoInteractions(proxyRepositoryComponentTelemetryCreator);
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
    ProxyRepositoryComponent unknownSha = tempEntity.newRepositoryComponent(repository, "unknown/sha", UNKNOWN, "hash");
    ProxyRepositoryPolicyViolation unknownShaViolation =
        tempEntity.newRepositoryPolicyViolation(unknownSha.getRepositoryId(), unknownSha.getPathname());

    // Action
    proxyRepositoryComponentDeleteService.deleteUnknownIgnoredComponents(repository);

    // Assertions
    assertThat(reload(unknownSha)).isNotNull();
    assertThat(reload(unknownShaViolation)).isNotNull();

    verifyNoInteractions(proxyRepositoryComponentTelemetryCreator);
  }

  @Test
  public void testDeleteComponent_Telemetry() {
    // setup
    String repositoryFormat = "maven2";
    Repository repository = tempEntity.newRepository("rm1", "r1", repositoryFormat);
    Policy policy = tempEntity.newPolicy();

    ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(repository, "pathname", UNKNOWN, "hash");
    tempEntity.newRepositoryPolicyViolation(component, policy.getId());

    ProxyRepositoryComponent component2 = tempEntity.newRepositoryComponent(repository, "pathname2", EXACT, "hash2");

    // when: a components are deleted
    proxyRepositoryComponentDeleteService.deleteComponent(component);
    proxyRepositoryComponentDeleteService.deleteComponent(component2);

    // then: telemetry is only sent for components with violations
    verify(proxyRepositoryComponentTelemetryCreator)
        .sendRepositoryComponentTelemetry(any(), any(), eq(repository.getRepositoryManagerId()),
            eq(repository.getPublicId()), eq(RepositoryComponentTelemetryEventType.DELETE), any(), any());
    verifyNoMoreInteractions(proxyRepositoryComponentTelemetryCreator);
  }

  private ProxyRepositoryComponent reload(ProxyRepositoryComponent proxyRepositoryComponent) {
    return proxyRepositoryComponentDAO.getById(proxyRepositoryComponent.getId());
  }

  private ProxyRepositoryPolicyViolation reload(ProxyRepositoryPolicyViolation policyViolation) {
    return proxyRepositoryPolicyViolationDAO.getById(policyViolation.getId());
  }

  private List<String> getPolicyWaiverIdsOf(Policy policy) {
    return policyWaiverDAO.getByPolicyId(policy.getId()).stream().map(PolicyWaiver::getId).collect(toList());
  }

  private List<String> getComponentLabelIds(Label label) {
    return componentLabelDAO.getByLabelId(label.getId()).stream().map(ComponentLabel::getId).collect(toList());
  }
}
