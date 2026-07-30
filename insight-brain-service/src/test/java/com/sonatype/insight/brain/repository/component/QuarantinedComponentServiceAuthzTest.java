/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.component;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.repository.QuarantinedComponentAccessDAO;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import jakarta.inject.Inject;
import java.util.Date;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class QuarantinedComponentServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private QuarantinedComponentAccessDAO quarantinedComponentAccessDAO;

  @Inject
  private DbQuarantinedComponentAccessManager quarantinedComponentAccessManager;

  @Inject
  private QuarantinedComponentService quarantinedComponentService;

  @Mock
  private ComponentInfoService componentInfoServiceMock;

  private String token;

  @Before
  public void setupTestData() {
    Date date = new Date();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    ProxyRepositoryComponent proxyRepositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
            "testpath", "testhash", componentIdentifier, date, date);
    token = quarantinedComponentAccessManager.createToken(proxyRepositoryComponent);
  }

  @Test
  public void testGetQuarantinedComponent_AnonymousEnabled() {
    quarantinedComponentService.getQuarantinedComponent(token);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetQuarantinedComponent_AnonymousDisabled_Unauthenticated() {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    quarantinedComponentService.getQuarantinedComponent(token);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetQuarantinedComponent_AnonymousDisabled_Unauthorized() {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    login();
    quarantinedComponentService.getQuarantinedComponent(token);
  }

  @Test
  public void testGetQuarantinedComponent_AnonymousDisabled_Authorized() {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    grantReadPermission(repository.getId());
    quarantinedComponentService.getQuarantinedComponent(token);
  }

  @Test
  public void testGetQuarantinedComponentOverview_AnonymousEnabled() {
    quarantinedComponentService.getQuarantinedComponentOverview(token);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetQuarantinedComponentOverview_AnonymousDisabled_Unauthenticated() {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    quarantinedComponentService.getQuarantinedComponentOverview(token);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetQuarantinedComponentOverview_AnonymousDisabled_Unauthorized() {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    login();
    quarantinedComponentService.getQuarantinedComponentOverview(token);
  }

  @Test
  public void testGetQuarantinedComponentOverview_AnonymousDisabled_Authorized() {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    grantReadPermission(repository.getId());
    quarantinedComponentService.getQuarantinedComponentOverview(token);
  }

  @Test
  public void testGetQuarantinedComponentPolicyViolations_AnonymousEnabled() {
    quarantinedComponentService.getQuarantinedComponentPolicyViolations(token);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetQuarantinedComponentPolicyViolations_AnonymousDisabled_Unauthenticated() {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    quarantinedComponentService.getQuarantinedComponentPolicyViolations(token);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetQuarantinedComponentPolicyViolations_AnonymousDisabled_Unauthorized() {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    login();
    quarantinedComponentService.getQuarantinedComponentPolicyViolations(token);
  }

  @Test
  public void testGetQuarantinedComponentPolicyViolations_AnonymousDisabled_Authorized() {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    grantReadPermission(repository.getId());
    quarantinedComponentService.getQuarantinedComponentPolicyViolations(token);
  }

  @Test
  public void testGetQuarantinedComponentOtherVersions_AnonymousEnabled() {
    quarantinedComponentService.getQuarantinedComponentOtherVersions(token, 1, 2, true);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetQuarantinedComponentOtherVersions_AnonymousDisabled_Unauthenticated() {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    quarantinedComponentService.getQuarantinedComponentOtherVersions(token, 1, 2, true);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetQuarantinedComponentOtherVersions_AnonymousDisabled_Unauthorized() {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    login();
    quarantinedComponentService.getQuarantinedComponentOtherVersions(token, 1, 2, true);
  }

  @Test
  public void testGetQuarantinedComponentOtherVersions_AnonymousDisabled_Authorized() {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    grantReadPermission(repository.getId());
    quarantinedComponentService.getQuarantinedComponentOtherVersions(token, 1, 2, true);
  }

  @Test
  public void testGetQuarantinedComponentVersionDetails_AnonymousEnabled() throws Exception {
    quarantinedComponentService.getQuarantinedComponentVersionDetails(token, null /* httpRequest */, "testVersion");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetQuarantinedComponentVersionDetails_AnonymousDisabled_Unauthenticated() throws Exception {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    quarantinedComponentService.getQuarantinedComponentVersionDetails(token, null /* httpRequest */, "testVersion");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetQuarantinedComponentVersionDetails_AnonymousDisabled_Unauthorized() throws Exception {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    login();
    quarantinedComponentService.getQuarantinedComponentVersionDetails(token, null /* httpRequest */, "testVersion");
  }

  @Test
  public void testGetQuarantinedComponentVersionDetails_AnonymousDisabled_Authorized() throws Exception {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    grantReadPermission(repository.getId());
    quarantinedComponentService.getQuarantinedComponentVersionDetails(token, null /* httpRequest */, "testVersion");
  }

  @Test
  public void testGetQuarantineComponentVersionRemediation_AnonymousEnabled() {
    quarantinedComponentService.getQuarantineComponentVersionRemediation(token);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetQuarantineComponentVersionRemediation_AnonymousDisabled_Unauthenticated() {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    quarantinedComponentService.getQuarantineComponentVersionRemediation(token);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetQuarantineComponentVersionRemediation_AnonymousDisabled_Unauthorized() {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    login();
    quarantinedComponentService.getQuarantineComponentVersionRemediation(token);
  }

  @Test
  public void testGetQuarantineComponentVersionRemediation_AnonymousDisabled_Authorized() {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    grantReadPermission(repository.getId());
    quarantinedComponentService.getQuarantineComponentVersionRemediation(token);
  }
}
