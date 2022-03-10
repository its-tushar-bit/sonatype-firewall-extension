/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.component;

import java.util.Date;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.repository.QuarantinedComponentAccessDAO;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import com.google.inject.Binder;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class QuarantinedComponentServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private DbQuarantinedComponentAccessManager quarantinedComponentAccessManager;

  @Inject
  private QuarantinedComponentService quarantinedComponentService;

  @Mock
  private ComponentInfoService componentInfoServiceMock;

  private String token;

  @Override
  public void configure(Binder binder) {
    binder.bind(ComponentInfoService.class).toInstance(componentInfoServiceMock);
    super.configure(binder);
  }

  @Before
  public void setupTestData() throws Exception {
    Date date = new Date();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "testpath", "testhash", componentIdentifier, date, date);
    token = quarantinedComponentAccessManager.createToken(repositoryComponent);
  }

  @Test
  public void testGetQuarantinedComponent_AnonymousEnabled() throws Exception {
    quarantinedComponentService.getQuarantinedComponent(token);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetQuarantinedComponent_AnonymousDisabled_Unauthenticated() throws Exception {
    new QuarantinedComponentAccessDAO().setAnonymousAccess(false);
    quarantinedComponentService.getQuarantinedComponent(token);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetQuarantinedComponent_AnonymousDisabled_Unauthorized() throws Exception {
    new QuarantinedComponentAccessDAO().setAnonymousAccess(false);
    login();
    quarantinedComponentService.getQuarantinedComponent(token);
  }

  @Test
  public void testGetQuarantinedComponent_AnonymousDisabled_Authorized() throws Exception {
    new QuarantinedComponentAccessDAO().setAnonymousAccess(false);
    grantReadPermission(repository.getId());
    quarantinedComponentService.getQuarantinedComponent(token);
  }

  @Test
  public void testGetQuarantinedComponentOverview_AnonymousEnabled() throws Exception {
    quarantinedComponentService.getQuarantinedComponentOverview(token);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetQuarantinedComponentOverview_AnonymousDisabled_Unauthenticated() throws Exception {
    new QuarantinedComponentAccessDAO().setAnonymousAccess(false);
    quarantinedComponentService.getQuarantinedComponentOverview(token);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetQuarantinedComponentOverview_AnonymousDisabled_Unauthorized() throws Exception {
    new QuarantinedComponentAccessDAO().setAnonymousAccess(false);
    login();
    quarantinedComponentService.getQuarantinedComponentOverview(token);
  }

  @Test
  public void testGetQuarantinedComponentOverview_AnonymousDisabled_Authorized() throws Exception {
    new QuarantinedComponentAccessDAO().setAnonymousAccess(false);
    grantReadPermission(repository.getId());
    quarantinedComponentService.getQuarantinedComponentOverview(token);
  }

  @Test
  public void testGetQuarantinedComponentPolicyViolations_AnonymousEnabled() throws Exception {
    quarantinedComponentService.getQuarantinedComponentPolicyViolations(token);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetQuarantinedComponentPolicyViolations_AnonymousDisabled_Unauthenticated() throws Exception {
    new QuarantinedComponentAccessDAO().setAnonymousAccess(false);
    quarantinedComponentService.getQuarantinedComponentPolicyViolations(token);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetQuarantinedComponentPolicyViolations_AnonymousDisabled_Unauthorized() throws Exception {
    new QuarantinedComponentAccessDAO().setAnonymousAccess(false);
    login();
    quarantinedComponentService.getQuarantinedComponentPolicyViolations(token);
  }

  @Test
  public void testGetQuarantinedComponentPolicyViolations_AnonymousDisabled_Authorized() throws Exception {
    new QuarantinedComponentAccessDAO().setAnonymousAccess(false);
    grantReadPermission(repository.getId());
    quarantinedComponentService.getQuarantinedComponentPolicyViolations(token);
  }

  @Test
  public void testGetQuarantinedComponentOtherVersions_AnonymousEnabled() throws Exception {
    quarantinedComponentService.getQuarantinedComponentOtherVersions(token, 1, 2, true);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetQuarantinedComponentOtherVersions_AnonymousDisabled_Unauthenticated() throws Exception {
    new QuarantinedComponentAccessDAO().setAnonymousAccess(false);
    quarantinedComponentService.getQuarantinedComponentOtherVersions(token, 1, 2, true);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetQuarantinedComponentOtherVersions_AnonymousDisabled_Unauthorized() throws Exception {
    new QuarantinedComponentAccessDAO().setAnonymousAccess(false);
    login();
    quarantinedComponentService.getQuarantinedComponentOtherVersions(token, 1, 2, true);
  }

  @Test
  public void testGetQuarantinedComponentOtherVersions_AnonymousDisabled_Authorized() throws Exception {
    new QuarantinedComponentAccessDAO().setAnonymousAccess(false);
    grantReadPermission(repository.getId());
    quarantinedComponentService.getQuarantinedComponentOtherVersions(token, 1, 2, true);
  }

  @Test
  public void testGetComponentVersionDetails_AnonymousEnabled() throws Exception {
    quarantinedComponentService.getComponentVersionDetails(token, null /* httpRequest */, "testVersion");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetComponentVersionDetails_AnonymousDisabled_Unauthenticated() throws Exception {
    new QuarantinedComponentAccessDAO().setAnonymousAccess(false);
    quarantinedComponentService.getComponentVersionDetails(token, null /* httpRequest */, "testVersion");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetComponentVersionDetails_AnonymousDisabled_Unauthorized() throws Exception {
    new QuarantinedComponentAccessDAO().setAnonymousAccess(false);
    login();
    quarantinedComponentService.getComponentVersionDetails(token, null /* httpRequest */, "testVersion");
  }

  @Test
  public void testGetComponentVersionDetails_AnonymousDisabled_Authorized() throws Exception {
    new QuarantinedComponentAccessDAO().setAnonymousAccess(false);
    grantReadPermission(repository.getId());
    quarantinedComponentService.getComponentVersionDetails(token, null /* httpRequest */, "testVersion");
  }

  @Test
  public void testGetQuarantineComponentVersionRemediation_AnonymousEnabled() throws Exception {
    quarantinedComponentService.getQuarantineComponentVersionRemediation(token);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetQuarantineComponentVersionRemediation_AnonymousDisabled_Unauthenticated() throws Exception {
    new QuarantinedComponentAccessDAO().setAnonymousAccess(false);
    quarantinedComponentService.getQuarantineComponentVersionRemediation(token);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetQuarantineComponentVersionRemediation_AnonymousDisabled_Unauthorized() throws Exception {
    new QuarantinedComponentAccessDAO().setAnonymousAccess(false);
    login();
    quarantinedComponentService.getQuarantineComponentVersionRemediation(token);
  }

  @Test
  public void testGetQuarantineComponentVersionRemediation_AnonymousDisabled_Authorized() throws Exception {
    new QuarantinedComponentAccessDAO().setAnonymousAccess(false);
    grantReadPermission(repository.getId());
    quarantinedComponentService.getQuarantineComponentVersionRemediation(token);
  }
}
