/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.Optional;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.component.DependencyType;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ManualPullRequestServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final String VALID_STAGE = ComplianceStageType.ID;

  private static final DependencyType VALID_DEPENDENCY_TYPE = DependencyType.DIRECT;

  @Inject
  private ManualPullRequestService manualPullRequestService;

  @Test
  public void testIsManualPullRequestPossible_Authorized() {
    grantPermission(app.getId(), Permission.CREATE_PULL_REQUESTS);
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");

    Optional<ManualPullRequestImpossibilityReason> result =
        manualPullRequestService.isManualPullRequestPossible(componentIdentifier, VALID_STAGE, VALID_DEPENDENCY_TYPE,
            app, null);

    assertThat(result).isPresent().contains(ManualPullRequestImpossibilityReason.NO_REMEDIATION_VERSION_AVAILABLE);
  }

  @Test
  public void testIsManualPullRequestPossible_Unauthorized() {
    login();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");

    Optional<ManualPullRequestImpossibilityReason> result =
        manualPullRequestService.isManualPullRequestPossible(componentIdentifier, VALID_STAGE, VALID_DEPENDENCY_TYPE,
            app, null);

    assertThat(result).isPresent().contains(ManualPullRequestImpossibilityReason.INSUFFICIENT_PERMISSIONS);
  }

  @Test
  public void testIsManualPullRequestPossible_Unauthenticated() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");

    Optional<ManualPullRequestImpossibilityReason> result =
        manualPullRequestService.isManualPullRequestPossible(componentIdentifier, VALID_STAGE, VALID_DEPENDENCY_TYPE,
            app, null);

    assertThat(result).isPresent().contains(ManualPullRequestImpossibilityReason.INSUFFICIENT_PERMISSIONS);
  }
}
