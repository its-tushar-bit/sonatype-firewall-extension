/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentsInQuarantineDTO;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class ApiComponentsInQuarantineReportingServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApiComponentsInQuarantineReportingService apiComponentsInQuarantineReportingService;

  @Test
  public void testGetComponentsInQuarantine() {
    Repository repository1 = tempEntity.newRepository();
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    tempEntity.newRepositoryComponent(repository1.getId(), MatchState.EXACT,
        componentIdentifier1, true /* quarantined */);
    Repository repository2 = tempEntity.newRepository();
    ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2");
    tempEntity.newRepositoryComponent(repository2.getId(), MatchState.EXACT,
        componentIdentifier1, true /* quarantined */);

    // No permissions on any repistory.
    ApiComponentsInQuarantineDTO result = apiComponentsInQuarantineReportingService.getComponentsInQuarantine();
    assertThat(result.componentsInQuarantine).isEmpty();

    // Read permission on the first repository.
    grantReadPermission(repository1.getId());
    result = apiComponentsInQuarantineReportingService.getComponentsInQuarantine();
    assertThat(result.componentsInQuarantine).hasSize(1);
    assertThat(result.componentsInQuarantine.get(0).repository.repositoryId).isEqualTo(repository1.getId());
    assertThat(result.componentsInQuarantine.get(0).components).hasSize(1);
    assertThat(
        result.componentsInQuarantine.get(0).components.get(0).component.componentIdentifier.toComponentIdentifier())
            .isEqualTo(componentIdentifier1);

    // Read permission on the second repository.
    grantReadPermission(repository2.getId());
    result = apiComponentsInQuarantineReportingService.getComponentsInQuarantine();
    assertThat(result.componentsInQuarantine).extracting(component -> component.repository.repositoryId)
        .containsExactlyInAnyOrder(repository1.getId(), repository2.getId());
  }
}
