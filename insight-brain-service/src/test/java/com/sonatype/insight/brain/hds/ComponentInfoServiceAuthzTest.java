/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import com.google.inject.Binder;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

public class ComponentInfoServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final ComponentIdentifier COMPONENT_IDENTIFIER = ComponentIdentifier.createMavenCoordinates("g", "a",
      "v");

  @Inject
  private ComponentInfoService componentInfoService;

  @Mock
  private HdsClient hdsClientMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(hdsClientMock);
    super.configure(binder);
  }

  @SuppressWarnings("unchecked")
  private void configureHdsClientMock() throws IOException {
    ComponentDetailsList componentDetailsList = new ComponentDetailsList();
    NamedComponentDetails namedComponentDetails = new NamedComponentDetails();
    componentDetailsList.setList(new ArrayList<ComponentDetails>());
    lenient().when(hdsClientMock.get(any(Class.class), any(String.class), any(Map.class))).thenReturn(
        componentDetailsList);
    lenient().when(hdsClientMock.relay((HttpServletRequest) any(), any(Class.class), any(String.class), any(Map.class)))
        .thenReturn(namedComponentDetails);
  }

  @Deprecated
  @Test
  public void testGetComponentDetailsList_EvaluateComponentPermission_Authorized() throws Exception {
    configureHdsClientMock();
    grantEvaluateComponentPermission(app.getId());
    componentInfoService.getComponentDetailsList_EvaluateComponentPermission(app.getPublicId(), COMPONENT_IDENTIFIER,
        MatchState.EXACT.getId());
  }

  @Deprecated
  @Test(expected = UnauthorizedException.class)
  public void testGetComponentDetailsList_EvaluateComponentPermission_Unauthorized() {
    login();
    componentInfoService.getComponentDetailsList_EvaluateComponentPermission(app.getPublicId(), COMPONENT_IDENTIFIER,
        MatchState.EXACT.getId());
  }

  @Deprecated
  @Test(expected = UnauthenticatedException.class)
  public void testGetComponentDetailsList_EvaluateComponentPermission_Unauthenticated() {
    componentInfoService.getComponentDetailsList_EvaluateComponentPermission(app.getPublicId(), COMPONENT_IDENTIFIER,
        MatchState.EXACT.getId());
  }

  @Test
  public void testGetComponentDetails_EvaluateComponentPermission_Authorized() throws Exception {
    configureHdsClientMock();
    grantEvaluateComponentPermission(app.getId());
    componentInfoService.getComponentDetails_EvaluateComponentPermission(app.getPublicId(), COMPONENT_IDENTIFIER,
        MatchState.EXACT.getId(), "hash", false /* proprietary */, null /* httpRequest */);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetComponentDetails_EvaluateComponentPermission_Unauthorized() throws Exception {
    login();
    componentInfoService.getComponentDetails_EvaluateComponentPermission(app.getPublicId(), COMPONENT_IDENTIFIER,
        MatchState.EXACT.getId(), "hash", false /* proprietary */, null /* httpRequest */);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetComponentDetails_EvaluateComponentPermission_Unauthenticated() throws Exception {
    componentInfoService.getComponentDetails_EvaluateComponentPermission(app.getPublicId(), COMPONENT_IDENTIFIER,
        MatchState.EXACT.getId(), "hash", false /* proprietary */, null /* httpRequest */);
  }

  // /

  @Deprecated
  private void testGetComponentDetailsList_ReadPermission_Authorized(final Owner owner, final String ownerId)
      throws Exception
  {
    configureHdsClientMock();
    grantReadPermission(owner.getId());
    componentInfoService.getComponentDetailsList_ReadPermission(owner.getType(), ownerId, COMPONENT_IDENTIFIER,
        MatchState.EXACT.getId());
  }

  @Deprecated
  @Test
  public void testGetComponentDetailsList_ReadPermission_Authorized_Application() throws Exception {
    testGetComponentDetailsList_ReadPermission_Authorized(app, app.getPublicId());
  }

  @Deprecated
  @Test
  public void testGetComponentDetailsList_ReadPermission_Authorized_Repository() throws Exception {
    testGetComponentDetailsList_ReadPermission_Authorized(repository, repository.getId());
  }

  @Deprecated
  private void testGetComponentDetailsList_ReadPermission_Unauthorized(final Owner owner, final String ownerId)
      throws Exception
  {
    login();
    componentInfoService.getComponentDetailsList_ReadPermission(owner.getType(), ownerId, COMPONENT_IDENTIFIER,
        MatchState.EXACT.getId());
  }

  @Deprecated
  @Test(expected = UnauthorizedException.class)
  public void testGetComponentDetailsList_ReadPermission_Unauthorized_Application() throws Exception {
    testGetComponentDetailsList_ReadPermission_Unauthorized(app, app.getPublicId());
  }

  @Deprecated
  @Test(expected = UnauthorizedException.class)
  public void testGetComponentDetailsList_ReadPermission_Unauthorized_Repository() throws Exception {
    testGetComponentDetailsList_ReadPermission_Unauthorized(repository, repository.getId());
  }

  @Deprecated
  private void testGetComponentDetailsList_ReadPermission_Unauthenticated(final Owner owner, final String ownerId) {
    componentInfoService.getComponentDetailsList_ReadPermission(owner.getType(), ownerId, COMPONENT_IDENTIFIER,
        MatchState.EXACT.getId());
  }

  @Deprecated
  @Test(expected = UnauthenticatedException.class)
  public void testGetComponentDetailsList_ReadPermission_Unauthenticated_Application() {
    testGetComponentDetailsList_ReadPermission_Unauthenticated(app, app.getPublicId());
  }

  @Deprecated
  @Test(expected = UnauthenticatedException.class)
  public void testGetComponentDetailsList_ReadPermission_Unauthenticated_Repository() {
    testGetComponentDetailsList_ReadPermission_Unauthenticated(repository, repository.getId());
  }

  private void testGetComponentDetails_ReadPermission_Authorized(final Owner owner, final String ownerId)
      throws Exception
  {
    configureHdsClientMock();
    grantReadPermission(owner.getId());
    componentInfoService.getComponentDetails_ReadPermission(owner.getType(), ownerId, COMPONENT_IDENTIFIER,
        MatchState.EXACT.getId(), "hash", false /* proprietary */, null /* httpRequest */);
  }

  @Test
  public void testGetComponentDetails_ReadPermission_Authorized_Application() throws Exception {
    testGetComponentDetails_ReadPermission_Authorized(app, app.getPublicId());
  }

  @Test
  public void testGetComponentDetails_ReadPermission_Authorized_Repository() throws Exception {
    testGetComponentDetails_ReadPermission_Authorized(repository, repository.getId());
  }

  private void testGetComponentDetails_ReadPermission_Unauthorized(final Owner owner, final String ownerId)
      throws Exception
  {
    login();
    componentInfoService.getComponentDetails_ReadPermission(owner.getType(), ownerId, COMPONENT_IDENTIFIER,
        MatchState.EXACT.getId(), "hash", false /* proprietary */, null /* httpRequest */);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetComponentDetails_ReadPermission_Unauthorized_Application() throws Exception {
    testGetComponentDetails_ReadPermission_Unauthorized(app, app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetComponentDetails_ReadPermission_Unauthorized_Repository() throws Exception {
    testGetComponentDetails_ReadPermission_Unauthorized(repository, repository.getId());
  }

  private void testGetComponentDetails_ReadPermission_Unauthenticated(final Owner owner, final String ownerId)
      throws Exception
  {
    componentInfoService.getComponentDetails_ReadPermission(owner.getType(), ownerId, COMPONENT_IDENTIFIER,
        MatchState.EXACT.getId(), "hash", false /* proprietary */, null /* httpRequest */);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetComponentDetails_ReadPermission_Unauthenticated_Application() throws Exception {
    testGetComponentDetails_ReadPermission_Unauthenticated(app, app.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetComponentDetails_ReadPermission_Unauthenticated_Repository() throws Exception {
    testGetComponentDetails_ReadPermission_Unauthenticated(repository, repository.getId());
  }

  @Test
  public void testGetLicensesApplication_Authorized() throws Exception {
    configureHdsClientMock();
    grantReadPermission(app.getId());
    componentInfoService
        .getLicenses(OwnerType.APPLICATION, app.getPublicId(), COMPONENT_IDENTIFIER, null /* httpRequest */);
  }

  @Test
  public void testGetLicensesRepository_Authorized() throws Exception {
    configureHdsClientMock();
    grantReadPermission(repository.getId());
    componentInfoService
        .getLicenses(OwnerType.REPOSITORY, repository.getId(), COMPONENT_IDENTIFIER, null /* httpRequest */);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetLicensesApplication_Unauthorized() throws Exception {
    login();
    componentInfoService
        .getLicenses(OwnerType.APPLICATION, app.getPublicId(), COMPONENT_IDENTIFIER, null /* httpRequest */);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetLicensesRepository_Unauthorized() throws Exception {
    login();
    componentInfoService
        .getLicenses(OwnerType.REPOSITORY, repository.getId(), COMPONENT_IDENTIFIER, null /* httpRequest */);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetLicensesApplication_Unauthenticated() throws Exception {
    componentInfoService
        .getLicenses(OwnerType.APPLICATION, app.getPublicId(), COMPONENT_IDENTIFIER, null /* httpRequest */);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetLicensesRepository_Unauthenticated() throws Exception {
    componentInfoService
        .getLicenses(OwnerType.REPOSITORY, repository.getId(), COMPONENT_IDENTIFIER, null /* httpRequest */);
  }

  @Test
  public void testGetSecurityVulnerabilities_Authorized() throws Exception {
    configureHdsClientMock();
    grantReadPermission(repository.getId());
    componentInfoService.getSecurityVulnerabilities(OwnerType.REPOSITORY, repository.getId(), "hash",
        COMPONENT_IDENTIFIER, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetSecurityVulnerabilities_Unauthorized() throws Exception {
    login();
    componentInfoService.getSecurityVulnerabilities(OwnerType.REPOSITORY, repository.getId(), "hash",
        COMPONENT_IDENTIFIER, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetSecurityVulnerabilities_Unauthenticated() throws Exception {
    componentInfoService.getSecurityVulnerabilities(OwnerType.REPOSITORY, repository.getId(), "hash",
        COMPONENT_IDENTIFIER, null);
  }

  @Test
  public void testGetComponentDetailsForAllVersions_EvaluateComponentPermission_Authorized() throws Exception {
    configureHdsClientMock();
    grantEvaluateComponentPermission(app.getId());
    componentInfoService.getComponentDetailsForAllVersions_EvaluateComponentPermission(app.getPublicId(),
        COMPONENT_IDENTIFIER);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetComponentDetailsForAllVersions_EvaluateComponentPermission_Unauthorized() {
    login();
    componentInfoService.getComponentDetailsForAllVersions_EvaluateComponentPermission(app.getPublicId(),
        COMPONENT_IDENTIFIER);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetComponentDetailsForAllVersions_EvaluateComponentPermission_Unauthenticated() {
    componentInfoService
        .getComponentDetailsForAllVersions_EvaluateComponentPermission(app.getPublicId(), COMPONENT_IDENTIFIER);
  }

  private void testGetComponentDetailsForAllVersions_ReadPermission_Authorized(final Owner owner, final String ownerId)
      throws Exception
  {
    configureHdsClientMock();
    grantReadPermission(owner.getId());
    componentInfoService
        .getComponentDetailsForAllVersions_ReadPermission(owner.getType(), ownerId, COMPONENT_IDENTIFIER, null, null);
  }

  @Test
  public void testGetComponentDetailsForAllVersions_ReadPermission_Authorized_Application() throws Exception {
    testGetComponentDetailsForAllVersions_ReadPermission_Authorized(app, app.getPublicId());
  }

  @Test
  public void testGetComponentDetailsForAllVersions_ReadPermission_Authorized_Repository() throws Exception {
    testGetComponentDetailsForAllVersions_ReadPermission_Authorized(repository, repository.getId());
  }

  private void testGetComponentDetailsForAllVersions_ReadPermission_Unauthorized(final Owner owner,
                                                                                 final String ownerId)
  {
    login();
    componentInfoService.getComponentDetailsForAllVersions_ReadPermission(owner.getType(), ownerId,
        COMPONENT_IDENTIFIER, null, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetComponentDetailsForAllVersions_ReadPermission_Unauthorized_Application() {
    testGetComponentDetailsForAllVersions_ReadPermission_Unauthorized(app, app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetComponentDetailsForAllVersions_ReadPermission_Unauthorized_Repository() {
    testGetComponentDetailsForAllVersions_ReadPermission_Unauthorized(repository, repository.getId());
  }

  private void testGetComponentDetailsForAllVersions_ReadPermission_Unauthenticated(final Owner owner,
                                                                                    final String ownerId)
  {
    componentInfoService.getComponentDetailsForAllVersions_ReadPermission(owner.getType(), ownerId,
        COMPONENT_IDENTIFIER, null, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetComponentDetailsForAllVersions_ReadPermission_Unauthenticated_Application() {
    testGetComponentDetailsForAllVersions_ReadPermission_Unauthenticated(app, app.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetComponentDetailsForAllVersions_ReadPermission_Unauthenticated_Repository() {
    testGetComponentDetailsForAllVersions_ReadPermission_Unauthenticated(repository, repository.getId());
  }
}
