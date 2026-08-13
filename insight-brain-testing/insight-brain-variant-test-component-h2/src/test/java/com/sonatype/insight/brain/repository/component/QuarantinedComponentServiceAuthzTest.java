/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.component;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.repository.QuarantinedComponentAccessDAO;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import jakarta.inject.Inject;
import java.util.Date;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class QuarantinedComponentServiceAuthzTest
    extends AbstractComponentH2AuthzTest
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

  @BeforeEach
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

  @Test
  public void testGetQuarantinedComponent_AnonymousDisabled_Unauthenticated() {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    assertThrows(UnauthenticatedException.class, () -> quarantinedComponentService.getQuarantinedComponent(token));
  }

  @Test
  public void testGetQuarantinedComponent_AnonymousDisabled_Unauthorized() {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    login();
    assertThrows(UnauthorizedException.class, () -> quarantinedComponentService.getQuarantinedComponent(token));
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

  @Test
  public void testGetQuarantinedComponentOverview_AnonymousDisabled_Unauthenticated() {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    assertThrows(UnauthenticatedException.class,
        () -> quarantinedComponentService.getQuarantinedComponentOverview(token));
  }

  @Test
  public void testGetQuarantinedComponentOverview_AnonymousDisabled_Unauthorized() {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    login();
    assertThrows(UnauthorizedException.class,
        () -> quarantinedComponentService.getQuarantinedComponentOverview(token));
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

  @Test
  public void testGetQuarantinedComponentPolicyViolations_AnonymousDisabled_Unauthenticated() {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    assertThrows(UnauthenticatedException.class,
        () -> quarantinedComponentService.getQuarantinedComponentPolicyViolations(token));
  }

  @Test
  public void testGetQuarantinedComponentPolicyViolations_AnonymousDisabled_Unauthorized() {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    login();
    assertThrows(UnauthorizedException.class,
        () -> quarantinedComponentService.getQuarantinedComponentPolicyViolations(token));
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

  @Test
  public void testGetQuarantinedComponentOtherVersions_AnonymousDisabled_Unauthenticated() {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    assertThrows(UnauthenticatedException.class,
        () -> quarantinedComponentService.getQuarantinedComponentOtherVersions(token, 1, 2, true));
  }

  @Test
  public void testGetQuarantinedComponentOtherVersions_AnonymousDisabled_Unauthorized() {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    login();
    assertThrows(UnauthorizedException.class,
        () -> quarantinedComponentService.getQuarantinedComponentOtherVersions(token, 1, 2, true));
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

  @Test
  public void testGetQuarantinedComponentVersionDetails_AnonymousDisabled_Unauthenticated() throws Exception {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    assertThrows(UnauthenticatedException.class, () -> quarantinedComponentService
        .getQuarantinedComponentVersionDetails(token, null /* httpRequest */, "testVersion"));
  }

  @Test
  public void testGetQuarantinedComponentVersionDetails_AnonymousDisabled_Unauthorized() throws Exception {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    login();
    assertThrows(UnauthorizedException.class, () -> quarantinedComponentService
        .getQuarantinedComponentVersionDetails(token, null /* httpRequest */, "testVersion"));
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

  @Test
  public void testGetQuarantineComponentVersionRemediation_AnonymousDisabled_Unauthenticated() {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    assertThrows(UnauthenticatedException.class,
        () -> quarantinedComponentService.getQuarantineComponentVersionRemediation(token));
  }

  @Test
  public void testGetQuarantineComponentVersionRemediation_AnonymousDisabled_Unauthorized() {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    login();
    assertThrows(UnauthorizedException.class,
        () -> quarantinedComponentService.getQuarantineComponentVersionRemediation(token));
  }

  @Test
  public void testGetQuarantineComponentVersionRemediation_AnonymousDisabled_Authorized() {
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    grantReadPermission(repository.getId());
    quarantinedComponentService.getQuarantineComponentVersionRemediation(token);
  }
}
