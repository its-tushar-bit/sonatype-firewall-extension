/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.Map;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import com.google.inject.Binder;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

public class ApiComponentRemediationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final ApiComponentDTOV2 API_COMPONENT_DTOV2 = createApiComponentDTOV2();

  @Inject
  private ApiComponentRemediationService apiComponentRemediationService;

  @Mock
  private HdsClient hdsClientMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(hdsClientMock);
    super.configure(binder);
  }

  @SuppressWarnings("unchecked")
  private void configureHdsClientMock() {
    ComponentDetailsList componentDetailsList = new ComponentDetailsList();
    componentDetailsList.setList(new ArrayList<>());
    lenient().when(hdsClientMock.get(eq(ComponentDetailsList.class), any(String.class), any(Map.class)))
        .thenReturn(componentDetailsList);

    lenient().when(hdsClientMock.get(eq(ComponentSummary.class), eq("rest/component/summary"), anyMap()))
        .thenReturn(ComponentSummary.create(true));
  }

  private void testGetSuggestedRemediationForComponent_Authorized(Owner owner) {
    configureHdsClientMock();
    grantEvaluateComponentPermission(owner.getId());
    apiComponentRemediationService.getSuggestedRemediationForComponent(API_COMPONENT_DTOV2, owner.getType(),
        owner.getId(), DevelopStageType.ID, null /* identificationSource */, null /* scanId */, null);
  }

  @Test
  public void testGetSuggestedRemediationForComponent_Authorized_Application() {
    testGetSuggestedRemediationForComponent_Authorized(app);
  }

  @Test
  public void testGetSuggestedRemediationForComponent_Authorized_Organization() {
    testGetSuggestedRemediationForComponent_Authorized(org);
  }

  @Test
  public void testGetSuggestedRemediationForComponent_Authorized_Repository() {
    configureHdsClientMock();
    grantEvaluateComponentPermission(repository.getId());
    apiComponentRemediationService.getSuggestedRemediationForComponent(API_COMPONENT_DTOV2, OwnerType.REPOSITORY,
        repository.getId(), ProxyStageType.ID, null /* identificationSource */, null /* scanId */, null);
  }

  private void testGetSuggestedRemediationForComponent_Unauthorized(Owner owner) {
    login();

    apiComponentRemediationService.getSuggestedRemediationForComponent(API_COMPONENT_DTOV2, owner.getType(),
        owner.getId(), DevelopStageType.ID, null /* identificationSource */, null /* scanId */, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetSuggestedRemediationForComponent_Unauthorized_Application() {
    testGetSuggestedRemediationForComponent_Unauthorized(app);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetSuggestedRemediationForComponent_Unauthorized_Organization() {
    testGetSuggestedRemediationForComponent_Unauthorized(org);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetSuggestedRemediationForComponent_Unauthorized_Repository() {
    login();
    apiComponentRemediationService.getSuggestedRemediationForComponent(API_COMPONENT_DTOV2, OwnerType.REPOSITORY,
        repository.getId(), ProxyStageType.ID, null /* identificationSource */, null /* scanId */, null);
  }

  private void testGetSuggestedRemediationForComponent_Unauthenticated(Owner owner) {
    apiComponentRemediationService.getSuggestedRemediationForComponent(API_COMPONENT_DTOV2, owner.getType(),
        owner.getId(), DevelopStageType.ID, null /* identificationSource */, null /* scanId */, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetSuggestedRemediationForComponent_Unauthenticated_Application() {
    testGetSuggestedRemediationForComponent_Unauthenticated(app);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetSuggestedRemediationForComponent_Unauthenticated_Organization() {
    testGetSuggestedRemediationForComponent_Unauthenticated(org);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetSuggestedRemediationForComponent_Unauthenticated_Repository() {
    apiComponentRemediationService.getSuggestedRemediationForComponent(API_COMPONENT_DTOV2, OwnerType.REPOSITORY,
        repository.getId(), ProxyStageType.ID, null /* identificationSource */, null /* scanId */, null);
  }

  private static ApiComponentDTOV2 createApiComponentDTOV2() {
    ApiComponentDTOV2 dto = new ApiComponentDTOV2();
    dto.componentIdentifier =
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g1", "a1",
            "v1", "", "jar"));
    return dto;
  }
}
