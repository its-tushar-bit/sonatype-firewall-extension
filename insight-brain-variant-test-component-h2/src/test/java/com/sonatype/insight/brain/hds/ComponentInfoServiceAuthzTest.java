/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.insight.brain.hds.HdsClient.RelayResponse;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.telemetry.NonBreakingRecommendationTelemetryStats.SourceEndpoint;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class ComponentInfoServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  private static final ComponentIdentifier COMPONENT_IDENTIFIER = ComponentIdentifier.createMavenCoordinates("g", "a",
      "v");

  @Inject
  private ComponentInfoService componentInfoService;

  @Mock
  private HdsClient hdsClientMock;

  @SuppressWarnings("unchecked")
  private void configureHdsClientMock() throws IOException {
    ComponentDetailsList componentDetailsList = new ComponentDetailsList();
    NamedComponentDetails namedComponentDetails = new NamedComponentDetails();
    componentDetailsList.setList(new ArrayList<>());
    lenient().when(hdsClientMock.get(any(Class.class), any(String.class), any(Map.class)))
        .thenReturn(
            componentDetailsList);
    lenient().when(hdsClientMock.relay(any(), any(Class.class), any(String.class), any(Map.class)))
        .thenReturn(new RelayResponse<>(namedComponentDetails));
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
  @Test
  public void testGetComponentDetailsList_EvaluateComponentPermission_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> componentInfoService.getComponentDetailsList_EvaluateComponentPermission(app.getPublicId(),
            COMPONENT_IDENTIFIER, MatchState.EXACT.getId()));
  }

  @Deprecated
  @Test
  public void testGetComponentDetailsList_EvaluateComponentPermission_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> componentInfoService.getComponentDetailsList_EvaluateComponentPermission(app.getPublicId(),
            COMPONENT_IDENTIFIER, MatchState.EXACT.getId()));
  }

  @Test
  public void testGetComponentDetails_EvaluateComponentPermission_Authorized() throws Exception {
    configureHdsClientMock();
    grantEvaluateComponentPermission(app.getId());
    componentInfoService.getComponentDetails_EvaluateComponentPermission(app.getPublicId(), COMPONENT_IDENTIFIER,
        MatchState.EXACT.getId(), "hash", false /* proprietary */, null /* httpRequest */);
  }

  @Test
  public void testGetComponentDetails_EvaluateComponentPermission_Unauthorized() throws Exception {
    login();
    assertThrows(UnauthorizedException.class,
        () -> componentInfoService.getComponentDetails_EvaluateComponentPermission(app.getPublicId(),
            COMPONENT_IDENTIFIER, MatchState.EXACT.getId(), "hash", false /* proprietary */, null /* httpRequest */));
  }

  @Test
  public void testGetComponentDetails_EvaluateComponentPermission_Unauthenticated() throws Exception {
    assertThrows(UnauthenticatedException.class,
        () -> componentInfoService.getComponentDetails_EvaluateComponentPermission(app.getPublicId(),
            COMPONENT_IDENTIFIER, MatchState.EXACT.getId(), "hash", false /* proprietary */, null /* httpRequest */));
  }

  // /

  @Deprecated
  private void testGetComponentDetailsList_ReadPermission_Authorized(
      final Owner owner,
      final String ownerId) throws Exception
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
  private void testGetComponentDetailsList_ReadPermission_Unauthorized(final Owner owner, final String ownerId) {
    login();
    componentInfoService.getComponentDetailsList_ReadPermission(owner.getType(), ownerId, COMPONENT_IDENTIFIER,
        MatchState.EXACT.getId());
  }

  @Deprecated
  @Test
  public void testGetComponentDetailsList_ReadPermission_Unauthorized_Application() throws Exception {
    assertThrows(UnauthorizedException.class,
        () -> testGetComponentDetailsList_ReadPermission_Unauthorized(app, app.getPublicId()));
  }

  @Deprecated
  @Test
  public void testGetComponentDetailsList_ReadPermission_Unauthorized_Repository() throws Exception {
    assertThrows(UnauthorizedException.class,
        () -> testGetComponentDetailsList_ReadPermission_Unauthorized(repository, repository.getId()));
  }

  @Deprecated
  private void testGetComponentDetailsList_ReadPermission_Unauthenticated(final Owner owner, final String ownerId) {
    componentInfoService.getComponentDetailsList_ReadPermission(owner.getType(), ownerId, COMPONENT_IDENTIFIER,
        MatchState.EXACT.getId());
  }

  @Deprecated
  @Test
  public void testGetComponentDetailsList_ReadPermission_Unauthenticated_Application() {
    assertThrows(UnauthenticatedException.class,
        () -> testGetComponentDetailsList_ReadPermission_Unauthenticated(app, app.getPublicId()));
  }

  @Deprecated
  @Test
  public void testGetComponentDetailsList_ReadPermission_Unauthenticated_Repository() {
    assertThrows(UnauthenticatedException.class,
        () -> testGetComponentDetailsList_ReadPermission_Unauthenticated(repository, repository.getId()));
  }

  private void testGetComponentDetails_ReadPermission_Authorized(
      final Owner owner,
      final String ownerId) throws Exception
  {
    configureHdsClientMock();
    grantReadPermission(owner.getId());
    componentInfoService.getComponentDetails_ReadPermission(owner.getType(), ownerId, COMPONENT_IDENTIFIER,
        MatchState.EXACT.getId(), "hash", false /* proprietary */, null /* httpRequest */,
        null /* identificationSource */, null /* scanId */, null /* dependencyType */);
  }

  @Test
  public void testGetComponentDetails_ReadPermission_Authorized_Application() throws Exception {
    testGetComponentDetails_ReadPermission_Authorized(app, app.getPublicId());
  }

  @Test
  public void testGetComponentDetails_ReadPermission_Authorized_Repository() throws Exception {
    testGetComponentDetails_ReadPermission_Authorized(repository, repository.getId());
  }

  private void testGetComponentDetails_ReadPermission_Unauthorized(
      final Owner owner,
      final String ownerId) throws Exception
  {
    login();
    componentInfoService.getComponentDetails_ReadPermission(owner.getType(), ownerId, COMPONENT_IDENTIFIER,
        MatchState.EXACT.getId(), "hash", false /* proprietary */, null /* httpRequest */,
        null /* identificationSource */, null /* scanId */, null /* dependencyType */);
  }

  @Test
  public void testGetComponentDetails_ReadPermission_Unauthorized_Application() throws Exception {
    assertThrows(UnauthorizedException.class,
        () -> testGetComponentDetails_ReadPermission_Unauthorized(app, app.getPublicId()));
  }

  @Test
  public void testGetComponentDetails_ReadPermission_Unauthorized_Repository() throws Exception {
    assertThrows(UnauthorizedException.class,
        () -> testGetComponentDetails_ReadPermission_Unauthorized(repository, repository.getId()));
  }

  private void testGetComponentDetails_ReadPermission_Unauthenticated(
      final Owner owner,
      final String ownerId) throws Exception
  {
    componentInfoService.getComponentDetails_ReadPermission(owner.getType(), ownerId, COMPONENT_IDENTIFIER,
        MatchState.EXACT.getId(), "hash", false /* proprietary */, null /* httpRequest */,
        null /* identificationSource */, null /* scanId */, null /* dependencyType */);
  }

  @Test
  public void testGetComponentDetails_ReadPermission_Unauthenticated_Application() throws Exception {
    assertThrows(UnauthenticatedException.class,
        () -> testGetComponentDetails_ReadPermission_Unauthenticated(app, app.getPublicId()));
  }

  @Test
  public void testGetComponentDetails_ReadPermission_Unauthenticated_Repository() throws Exception {
    assertThrows(UnauthenticatedException.class,
        () -> testGetComponentDetails_ReadPermission_Unauthenticated(repository, repository.getId()));
  }

  @Test
  public void testGetLicensesApplication_Authorized() throws Exception {
    configureHdsClientMock();
    grantReadPermission(app.getId());
    componentInfoService.getLicenses(OwnerType.APPLICATION, app.getPublicId(), COMPONENT_IDENTIFIER,
        null /* httpRequest */, null, null);
  }

  @Test
  public void testGetLicensesRepository_Authorized() throws Exception {
    configureHdsClientMock();
    grantReadPermission(repository.getId());
    componentInfoService.getLicenses(OwnerType.REPOSITORY, repository.getId(), COMPONENT_IDENTIFIER,
        null /* httpRequest */, null, null);
  }

  @Test
  public void testGetLicensesApplication_Unauthorized() throws Exception {
    login();
    assertThrows(UnauthorizedException.class,
        () -> componentInfoService.getLicenses(OwnerType.APPLICATION, app.getPublicId(), COMPONENT_IDENTIFIER,
            null /* httpRequest */, null, null));
  }

  @Test
  public void testGetLicensesRepository_Unauthorized() throws Exception {
    login();
    assertThrows(UnauthorizedException.class,
        () -> componentInfoService.getLicenses(OwnerType.REPOSITORY, repository.getId(), COMPONENT_IDENTIFIER,
            null /* httpRequest */, null, null));
  }

  @Test
  public void testGetLicensesApplication_Unauthenticated() throws Exception {
    assertThrows(UnauthenticatedException.class,
        () -> componentInfoService.getLicenses(OwnerType.APPLICATION, app.getPublicId(), COMPONENT_IDENTIFIER,
            null /* httpRequest */, null, null));
  }

  @Test
  public void testGetLicensesRepository_Unauthenticated() throws Exception {
    assertThrows(UnauthenticatedException.class,
        () -> componentInfoService.getLicenses(OwnerType.REPOSITORY, repository.getId(), COMPONENT_IDENTIFIER,
            null /* httpRequest */, null, null));
  }

  @Test
  public void testGetMultiLicensesForRead_ApplicationAuthorized() throws Exception {
    configureHdsClientMock();
    grantReadPermission(app.getId());
    componentInfoService.getMultiLicensesForRead(OwnerType.APPLICATION, app.getPublicId(), COMPONENT_IDENTIFIER,
        null /* httpRequest */, null, null);
  }

  @Test
  public void testGetMultiLicenses_RepositoryAuthorized() throws Exception {
    configureHdsClientMock();
    grantReadPermission(repository.getId());
    componentInfoService.getMultiLicensesForRead(OwnerType.REPOSITORY, repository.getId(), COMPONENT_IDENTIFIER,
        null /* httpRequest */, null, null);
  }

  @Test
  public void testGetMultiLicensesForRead_ApplicationUnauthorized() throws Exception {
    login();
    assertThrows(UnauthorizedException.class,
        () -> componentInfoService.getMultiLicensesForRead(OwnerType.APPLICATION, app.getPublicId(),
            COMPONENT_IDENTIFIER, null /* httpRequest */, null, null));
  }

  @Test
  public void testGetMultiLicensesForRead_RepositoryUnauthorized() throws Exception {
    login();
    assertThrows(UnauthorizedException.class,
        () -> componentInfoService.getMultiLicensesForRead(OwnerType.REPOSITORY, repository.getId(),
            COMPONENT_IDENTIFIER, null /* httpRequest */, null, null));
  }

  @Test
  public void testGetMultiLicensesForRead_ApplicationUnauthenticated() throws Exception {
    assertThrows(UnauthenticatedException.class,
        () -> componentInfoService.getMultiLicensesForRead(OwnerType.APPLICATION, app.getPublicId(),
            COMPONENT_IDENTIFIER, null /* httpRequest */, null, null));
  }

  @Test
  public void testGetMultiLicensesForRead_RepositoryUnauthenticated() throws Exception {
    assertThrows(UnauthenticatedException.class,
        () -> componentInfoService.getMultiLicensesForRead(OwnerType.REPOSITORY, repository.getId(),
            COMPONENT_IDENTIFIER, null /* httpRequest */, null, null));
  }

  @Test
  public void testGetMultiLicensesForLegalReviewer_ApplicationAuthorized() throws Exception {
    configureHdsClientMock();
    grantPermission(app.getId(), Permission.LEGAL_REVIEWER);
    componentInfoService.getMultiLicensesForLegalReviewer(OwnerType.APPLICATION, app.getPublicId(),
        COMPONENT_IDENTIFIER, null /* httpRequest */, null, null);
  }

  @Test
  public void testGetMultiLicensesForLegalReviewer_RepositoryAuthorized() throws Exception {
    configureHdsClientMock();
    grantPermission(repository.getId(), Permission.LEGAL_REVIEWER);
    componentInfoService.getMultiLicensesForLegalReviewer(OwnerType.REPOSITORY, repository.getId(),
        COMPONENT_IDENTIFIER, null /* httpRequest */, null, null);
  }

  @Test
  public void testGetMultiLicensesForLegalReviewer_ApplicationUnauthorized() throws Exception {
    login();
    assertThrows(UnauthorizedException.class,
        () -> componentInfoService.getMultiLicensesForLegalReviewer(OwnerType.APPLICATION, app.getPublicId(),
            COMPONENT_IDENTIFIER, null /* httpRequest */, null, null));
  }

  @Test
  public void testGetMultiLicensesForLegalReviewer_RepositoryUnauthorized() throws Exception {
    login();
    assertThrows(UnauthorizedException.class,
        () -> componentInfoService.getMultiLicensesForLegalReviewer(OwnerType.REPOSITORY, repository.getId(),
            COMPONENT_IDENTIFIER, null /* httpRequest */, null, null));
  }

  @Test
  public void testGetMultiLicensesForLegalReviewer_ApplicationUnauthenticated() throws Exception {
    assertThrows(UnauthenticatedException.class,
        () -> componentInfoService.getMultiLicensesForLegalReviewer(OwnerType.APPLICATION, app.getPublicId(),
            COMPONENT_IDENTIFIER, null /* httpRequest */, null, null));
  }

  @Test
  public void testGetMultiLicensesForLegalReviewer_RepositoryUnauthenticated() throws Exception {
    assertThrows(UnauthenticatedException.class,
        () -> componentInfoService.getMultiLicensesForLegalReviewer(OwnerType.REPOSITORY, repository.getId(),
            COMPONENT_IDENTIFIER, null /* httpRequest */, null, null));
  }

  @Test
  public void testGetSecurityVulnerabilities_Authorized() throws Exception {
    configureHdsClientMock();
    grantReadPermission(repository.getId());
    componentInfoService.getSecurityVulnerabilities(OwnerType.REPOSITORY, repository.getId(), "hash",
        COMPONENT_IDENTIFIER, null, null, null);
  }

  @Test
  public void testGetSecurityVulnerabilities_Unauthorized() throws Exception {
    login();
    assertThrows(UnauthorizedException.class,
        () -> componentInfoService.getSecurityVulnerabilities(OwnerType.REPOSITORY, repository.getId(), "hash",
            COMPONENT_IDENTIFIER, null, null, null));
  }

  @Test
  public void testGetSecurityVulnerabilities_Unauthenticated() throws Exception {
    assertThrows(UnauthenticatedException.class,
        () -> componentInfoService.getSecurityVulnerabilities(OwnerType.REPOSITORY, repository.getId(), "hash",
            COMPONENT_IDENTIFIER, null, null, null));
  }

  @Test
  public void testGetComponentVersionInfo_EvaluateComponentPermission_Authorized() throws Exception {
    configureHdsClientMock();
    grantEvaluateComponentPermission(app.getId());
    componentInfoService.getComponentVersionInfo_EvaluateComponentPermission(app.getPublicId(),
        COMPONENT_IDENTIFIER, SourceEndpoint.IDE);
  }

  @Test
  public void testGetComponentVersionInfo_EvaluateComponentPermission_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> componentInfoService.getComponentVersionInfo_EvaluateComponentPermission(app.getPublicId(),
            COMPONENT_IDENTIFIER, SourceEndpoint.IDE));
  }

  @Test
  public void testGetComponentVersionInfo_EvaluateComponentPermission_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> componentInfoService
            .getComponentVersionInfo_EvaluateComponentPermission(
                app.getPublicId(), COMPONENT_IDENTIFIER, SourceEndpoint.IDE));
  }

  private void testGetComponentVersionInfo_Authorized_ReadPermission(
      final Owner owner,
      final String ownerId) throws Exception
  {
    configureHdsClientMock();
    grantReadPermission(owner.getId());
    componentInfoService.getComponentVersionInfo(owner.getType(), ownerId, COMPONENT_IDENTIFIER, null, null, null,
        null);
  }

  @Test
  public void testGetComponentVersionInfo_Authorized_ReadPermission_Application() throws Exception {
    testGetComponentVersionInfo_Authorized_ReadPermission(app, app.getPublicId());
  }

  @Test
  public void testGetComponentVersionInfo_Authorized_ReadPermission_Repository() throws Exception {
    testGetComponentVersionInfo_Authorized_ReadPermission(repository, repository.getId());
  }

  private void testGetComponentVersionInfo_Unauthorized(final Owner owner, final String ownerId) {
    login();
    componentInfoService.getComponentVersionInfo(owner.getType(), ownerId,
        COMPONENT_IDENTIFIER, null, null, null, null);
  }

  @Test
  public void testGetComponentVersionInfo_Unauthorized_Application() {
    assertThrows(UnauthorizedException.class,
        () -> testGetComponentVersionInfo_Unauthorized(app, app.getPublicId()));
  }

  @Test
  public void testGetComponentVersionInfo_Unauthorized_Repository() {
    assertThrows(UnauthorizedException.class,
        () -> testGetComponentVersionInfo_Unauthorized(repository, repository.getId()));
  }

  private void testGetComponentVersionInfo_Unauthenticated(final Owner owner, final String ownerId) {
    componentInfoService.getComponentVersionInfo(owner.getType(), ownerId,
        COMPONENT_IDENTIFIER, null, null, null, null);
  }

  @Test
  public void testGetComponentVersionInfo_Unauthenticated_Application() {
    assertThrows(UnauthenticatedException.class,
        () -> testGetComponentVersionInfo_Unauthenticated(app, app.getPublicId()));
  }

  @Test
  public void testGetComponentVersionInfo_Unauthenticated_Repository() {
    assertThrows(UnauthenticatedException.class,
        () -> testGetComponentVersionInfo_Unauthenticated(repository, repository.getId()));
  }

  private void testGetComponentVersionInfo_Authorized_EvaluateComponentPermission(
      Owner owner,
      String ownerId) throws Exception
  {
    configureHdsClientMock();
    grantEvaluateComponentPermission(owner.getId());
    componentInfoService.getComponentVersionInfo(owner.getType(), ownerId, COMPONENT_IDENTIFIER, null, null, null,
        null);
  }

  @Test
  public void testGetComponentVersionInfo_Authorized_EvaluateComponentPermission_Application() throws Exception {
    testGetComponentVersionInfo_Authorized_EvaluateComponentPermission(app, app.getPublicId());
  }

  @Test
  public void testGetComponentVersionInfo_Authorized_EvaluateComponentPermission_Repository() throws Exception {
    testGetComponentVersionInfo_Authorized_EvaluateComponentPermission(repository, repository.getId());
  }
}
