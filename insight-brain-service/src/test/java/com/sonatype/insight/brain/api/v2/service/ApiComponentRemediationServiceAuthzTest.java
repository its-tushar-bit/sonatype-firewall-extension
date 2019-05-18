/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.Map;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
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
    lenient().when(hdsClientMock.get(eq(ComponentDetailsList.class), any(String.class), any(Map.class),
        any(String.class))).thenReturn(componentDetailsList);

    lenient().when(hdsClientMock.get(eq(ComponentSummary.class), eq("rest/component/summary"), anyMap()))
        .thenReturn(ComponentSummary.create(true));
  }

  private void testGetSuggestedRemediationForComponent_ReadPermission_Authorized(final Owner owner,
                                                                                 final String ownerId)
  {
    configureHdsClientMock();
    grantEvaluateComponentPermission(owner.getId());
    apiComponentRemediationService
        .getSuggestedRemediationForComponent(API_COMPONENT_DTOV2, owner.getType(), ownerId, DevelopStageType.ID);
  }

  @Test
  public void testGetSuggestedRemediationForComponent_ReadPermission_Authorized_Application() {
    testGetSuggestedRemediationForComponent_ReadPermission_Authorized(app, app.getId());
  }

  @Test
  public void testGetSuggestedRemediationForComponent_ReadPermission_Authorized_Organization() {
    testGetSuggestedRemediationForComponent_ReadPermission_Authorized(org, org.getId());
  }

  private void testGetSuggestedRemediationForComponent_ReadPermission_Unauthorized(final Owner owner,
                                                                                   final String ownerId)
  {
    login();

    apiComponentRemediationService
        .getSuggestedRemediationForComponent(API_COMPONENT_DTOV2, owner.getType(), ownerId, DevelopStageType.ID);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetSuggestedRemediationForComponent_ReadPermission_Unauthorized_Application() {
    testGetSuggestedRemediationForComponent_ReadPermission_Unauthorized(app, app.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetSuggestedRemediationForComponent_ReadPermission_Unauthorized_Organization() {
    testGetSuggestedRemediationForComponent_ReadPermission_Unauthorized(org, org.getId());
  }

  private void testGetSuggestedRemediationForComponent_ReadPermission_Unauthenticated(final Owner owner,
                                                                                      final String ownerId)
  {
    apiComponentRemediationService
        .getSuggestedRemediationForComponent(API_COMPONENT_DTOV2, owner.getType(), ownerId, DevelopStageType.ID);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetSuggestedRemediationForComponent_ReadPermission_Unauthenticated_Application() {
    testGetSuggestedRemediationForComponent_ReadPermission_Unauthenticated(app, app.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetSuggestedRemediationForComponent_ReadPermission_Unauthenticated_Organization() {
    testGetSuggestedRemediationForComponent_ReadPermission_Unauthenticated(org, org.getId());
  }

  private static ApiComponentDTOV2 createApiComponentDTOV2() {
    ApiComponentDTOV2 dto = new ApiComponentDTOV2();
    dto.componentIdentifier =
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g1", "a1",
            "v1", "", "jar"));
    return dto;
  }
}
