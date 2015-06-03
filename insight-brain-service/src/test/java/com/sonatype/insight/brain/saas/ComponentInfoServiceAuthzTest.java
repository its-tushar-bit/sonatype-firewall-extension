/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import com.google.inject.Binder;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ComponentInfoServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final ComponentIdentifier COMPONENT_IDENTIFIER = ComponentIdentifier.createMavenCoordinates("g", "a",
      "v");

  @Inject
  private ComponentInfoService componentInfoService;

  private SaasClient saasClientMock = mock(SaasClient.class);

  @Override
  public void configure(Binder binder) {
    binder.bind(SaasClient.class).toInstance(saasClientMock);
    super.configure(binder);
  }

  @SuppressWarnings("unchecked")
  private void configureSaasClientMock() throws IOException {
    ComponentDetailsList componentDetailsList = new ComponentDetailsList();
    NamedComponentDetails namedComponentDetails = new NamedComponentDetails();
    componentDetailsList.setList(new ArrayList<ComponentDetails>());
    when(saasClientMock.get(any(HttpServletRequest.class), any(Class.class), any(String.class))).thenReturn(
        componentDetailsList);
    when(saasClientMock.get(any(HttpServletRequest.class), any(Class.class), any(String.class), any(Map.class)))
        .thenReturn(namedComponentDetails);
  }

  @Test
  public void testGetComponentDetailsList_Authorized() throws Exception {
    configureSaasClientMock();
    grantEvaluateComponentPermission(app.getId());
    componentInfoService.getComponentDetailsList(app.getPublicId(), COMPONENT_IDENTIFIER, MatchState.EXACT.getId(),
        null /* httpRequest */);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetComponentDetailsList_Unauthorized() throws Exception {
    login();
    componentInfoService.getComponentDetailsList(app.getPublicId(), COMPONENT_IDENTIFIER, MatchState.EXACT.getId(),
        null /* httpRequest */);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetComponentDetailsList_Unauthenticated() throws Exception {
    componentInfoService.getComponentDetailsList(app.getPublicId(), COMPONENT_IDENTIFIER, MatchState.EXACT.getId(),
        null /* httpRequest */);
  }

  @Test
  public void testGetComponentDetails_Authorized() throws Exception {
    configureSaasClientMock();
    grantEvaluateComponentPermission(app.getId());
    componentInfoService.getComponentDetails(app.getPublicId(), COMPONENT_IDENTIFIER, MatchState.EXACT.getId(),
        "hash", false /* proprietary */, null /* httpRequest */);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetComponentDetails_Unauthorized() throws Exception {
    login();
    componentInfoService.getComponentDetails(app.getPublicId(), COMPONENT_IDENTIFIER, MatchState.EXACT.getId(),
        "hash", false /* proprietary */, null /* httpRequest */);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetComponentDetails_Unauthenticated() throws Exception {
    componentInfoService.getComponentDetails(app.getPublicId(), COMPONENT_IDENTIFIER, MatchState.EXACT.getId(),
        "hash", false /* proprietary */, null /* httpRequest */);
  }

  @Test
  public void testGetLicenses_Authorized() throws Exception {
    configureSaasClientMock();
    grantReadPermission(app.getId());
    componentInfoService.getLicenses(app.getPublicId(), COMPONENT_IDENTIFIER, null /* httpRequest */);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetLicenses_Unauthorized() throws Exception {
    login();
    componentInfoService.getLicenses(app.getPublicId(), COMPONENT_IDENTIFIER, null /* httpRequest */);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetLicenses_Unauthenticated() throws Exception {
    componentInfoService.getLicenses(app.getPublicId(), COMPONENT_IDENTIFIER, null /* httpRequest */);
  }
}
