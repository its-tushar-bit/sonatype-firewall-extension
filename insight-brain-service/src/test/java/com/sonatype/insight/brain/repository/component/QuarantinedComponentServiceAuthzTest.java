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
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.when;

public class QuarantinedComponentServiceAuthzTest
    extends AbstractServiceAuthzTest
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
  public void testGetQuarantinedComponent_Unauthenticated() {
    when(quarantinedComponentAccessManager.getRepositoryComponentIdFromToken("token")).thenReturn("id");
    quarantinedComponentService.getQuarantinedComponent("token");
  }

  @Test
  public void testGetQuarantinedComponent_Authenticated() {
    login();
    when(quarantinedComponentAccessManager.getRepositoryComponentIdFromToken("token")).thenReturn("id");
    quarantinedComponentService.getQuarantinedComponent("token");
  }

  @Test
  public void testGetQuarantinedComponentOverview_Unauthenticated() {
    when(quarantinedComponentAccessManager.getRepositoryComponentIdFromToken("token")).thenReturn(setupTestData());
    quarantinedComponentService.getQuarantinedComponentOverview("token");
  }

  @Test
  public void testGetQuarantinedComponentOverview_Authenticated() {
    login();
    when(quarantinedComponentAccessManager.getRepositoryComponentIdFromToken("token")).thenReturn(setupTestData());
    quarantinedComponentService.getQuarantinedComponentOverview("token");
  }

  private String setupTestData() {
    Date date = new Date();
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    final RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    final Repository repository = tempEntity.newRepository(repositoryManager, "repositoryPublicId");
    final RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "path",
            "hash", componentIdentifier, date, date);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 6,
        repositoryComponent.getPathname(), false, "fail", "policyId", "policyName",
        repositoryComponent.getComponentIdentifier(), date);
    return repositoryComponent.getId();
  }
}
