/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.component;

import java.util.Date;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

public class QuarantinedComponentServiceTest
    extends AbstractComponentTest
{
  @Mock
  private DbQuarantinedComponentAccessManager quarantinedComponentAccessManager;

  @Inject
  private QuarantinedComponentService quarantinedComponentService;

  @Override
  public void configure(Binder binder) {
    binder.bind(DbQuarantinedComponentAccessManager.class).toInstance(quarantinedComponentAccessManager);
    super.configure(binder);
  }

  @Test
  public void testGetQuarantinedComponent() {
    when(quarantinedComponentAccessManager.getRepositoryComponentIdFromToken("token")).thenReturn("id");

    QuarantinedComponentDto quarantinedComponentDto = quarantinedComponentService.getQuarantinedComponent("token");

    assertThat(quarantinedComponentDto).isNotNull();
    assertThat(quarantinedComponentDto.success).isTrue();
    assertThat(quarantinedComponentDto.repositoryComponentId).isEqualTo("id");
  }

  @Test
  public void testGetQuarantinedComponentOverview() {
    //setup
    Date date = new Date();
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    when(quarantinedComponentAccessManager.getRepositoryComponentIdFromToken("token")).thenReturn(
        setupTestData(componentIdentifier, date, date, null));

    //when
    QuarantinedComponentOverviewDto quarantinedComponentOverviewDto =
        quarantinedComponentService.getQuarantinedComponentOverview("token");

    //then
    assertThat(quarantinedComponentOverviewDto).isNotNull();
    assertThat(quarantinedComponentOverviewDto.componentDisplayName).isEqualTo("g : a : v");
    assertThat(quarantinedComponentOverviewDto.isQuarantined).isEqualTo(true);
    assertThat(quarantinedComponentOverviewDto.quarantinedPolicyViolationsCount).isEqualTo(2);
    assertThat(quarantinedComponentOverviewDto.repositoryName).isEqualTo("repositoryPublicId");
    assertThat(quarantinedComponentOverviewDto.quarantinedDate).isEqualTo(date);
    assertThat(quarantinedComponentOverviewDto.cataloguedDate).isEqualTo(date);
  }

  @Test
  public void testGetQuarantinedComponentOverview_quarantinedTimeNull() {
    //setup
    Date date = new Date();
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    when(quarantinedComponentAccessManager.getRepositoryComponentIdFromToken("token")).thenReturn(
        setupTestData(componentIdentifier, date, null, null));

    //when
    QuarantinedComponentOverviewDto quarantinedComponentOverviewDto =
        quarantinedComponentService.getQuarantinedComponentOverview("token");

    //then
    assertThat(quarantinedComponentOverviewDto).isNotNull();
    assertThat(quarantinedComponentOverviewDto.isQuarantined).isEqualTo(false);
  }

  @Test
  public void testGetQuarantinedComponentOverview_unquarantinedTimeNotNull() {
    //setup
    Date date = new Date();
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    when(quarantinedComponentAccessManager.getRepositoryComponentIdFromToken("token")).thenReturn(
        setupTestData(componentIdentifier, date, date, date));

    //when
    QuarantinedComponentOverviewDto quarantinedComponentOverviewDto =
        quarantinedComponentService.getQuarantinedComponentOverview("token");

    //then
    assertThat(quarantinedComponentOverviewDto).isNotNull();
    assertThat(quarantinedComponentOverviewDto.isQuarantined).isEqualTo(false);
  }

  @Test
  public void testGetQuarantinedComponentOverview_componentIdentifierNull() {
    //setup
    Date date = new Date();
    when(quarantinedComponentAccessManager.getRepositoryComponentIdFromToken("token")).thenReturn(
        setupTestData(null, date, date, null));

    assertThatThrownBy(() -> {
      quarantinedComponentService.getQuarantinedComponentOverview("token");
    }).isInstanceOf(BadRequestException.class)
        .hasMessage("The component identifier for the requested component does not exist.");
  }

  private String setupTestData(
      ComponentIdentifier componentIdentifier,
      Date time,
      Date quarantinedTime,
      Date unquarantinedTime)
  {
    final RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    final Repository repository = tempEntity.newRepository(repositoryManager, "repositoryPublicId");
    final RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "path",
            "hash", componentIdentifier, time, quarantinedTime, unquarantinedTime);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 6,
        repositoryComponent.getPathname(), false, "fail", "policyId", "policyName",
        repositoryComponent.getComponentIdentifier(), time);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 6,
        repositoryComponent.getPathname(), false, "fail", "policyId", "policyName",
        repositoryComponent.getComponentIdentifier(), time);
    return repositoryComponent.getId();
  }
}
