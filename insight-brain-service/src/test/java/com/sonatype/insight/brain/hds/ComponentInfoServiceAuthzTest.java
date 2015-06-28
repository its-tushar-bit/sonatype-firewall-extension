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
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.inject.Binder;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
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

  private HdsClient hdsClientMock = mock(HdsClient.class);

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(hdsClientMock);
    super.configure(binder);
  }

  @SuppressWarnings("unchecked")
  private void configureSaasClientMock() throws IOException {
    ComponentDetailsList componentDetailsList = new ComponentDetailsList();
    NamedComponentDetails namedComponentDetails = new NamedComponentDetails();
    componentDetailsList.setList(new ArrayList<ComponentDetails>());
    when(hdsClientMock.get(any(HttpServletRequest.class), any(Class.class), any(String.class))).thenReturn(
        componentDetailsList);
    when(hdsClientMock.get(any(HttpServletRequest.class), any(Class.class), any(String.class), any(Map.class)))
        .thenReturn(namedComponentDetails);
  }

  @Test
  public void testGetComponentDetailsList_EvaluateComponentPermission_Authorized() throws Exception {
    configureSaasClientMock();
    grantEvaluateComponentPermission(app.getId());
    componentInfoService.getComponentDetailsList_EvaluateComponentPermission(app.getPublicId(), COMPONENT_IDENTIFIER,
        MatchState.EXACT.getId(), null /* httpRequest */);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetComponentDetailsList_EvaluateComponentPermission_Unauthorized() throws Exception {
    login();
    componentInfoService.getComponentDetailsList_EvaluateComponentPermission(app.getPublicId(), COMPONENT_IDENTIFIER,
        MatchState.EXACT.getId(), null /* httpRequest */);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetComponentDetailsList_EvaluateComponentPermission_Unauthenticated() throws Exception {
    componentInfoService.getComponentDetailsList_EvaluateComponentPermission(app.getPublicId(), COMPONENT_IDENTIFIER,
        MatchState.EXACT.getId(), null /* httpRequest */);
  }

  @Test
  public void testGetComponentDetails_EvaluateComponentPermission_Authorized() throws Exception {
    configureSaasClientMock();
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

  @Test
  public void testGetComponentDetailsList_ReadPermission_Authorized() throws Exception {
    configureSaasClientMock();
    grantReadPermission(app.getId());
    try {
      componentInfoService.getComponentDetailsList_ReadPermission(app.getPublicId(), "reportId",
          COMPONENT_IDENTIFIER, MatchState.EXACT.getId(), null /* httpRequest */);
      fail("Expected NotFoundException");
    }
    catch (NotFoundException expected) {
      assertThat(expected.getMessage(), is("Cannot find a report with ID 'reportId'."));
    }
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetComponentDetailsList_ReadPermission_Unauthorized() throws Exception {
    login();
    componentInfoService.getComponentDetailsList_ReadPermission(app.getPublicId(), null /* reportId */,
        COMPONENT_IDENTIFIER, MatchState.EXACT.getId(), null /* httpRequest */);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetComponentDetailsList_ReadPermission_Unauthenticated() throws Exception {
    componentInfoService.getComponentDetailsList_ReadPermission(app.getPublicId(), null /* reportId */,
        COMPONENT_IDENTIFIER, MatchState.EXACT.getId(), null /* httpRequest */);
  }

  @Test
  public void testGetComponentDetails_ReadPermission_Authorized() throws Exception {
    configureSaasClientMock();
    grantReadPermission(app.getId());
    try {
      componentInfoService.getComponentDetails_ReadPermission(app.getPublicId(), "reportId", COMPONENT_IDENTIFIER,
          MatchState.EXACT.getId(), "hash", false /* proprietary */, null /* httpRequest */);
      fail("Expected NotFoundException");
    }
    catch (NotFoundException expected) {
      assertThat(expected.getMessage(), is("Cannot find a report with ID 'reportId'."));
    }
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetComponentDetails_ReadPermission_Unauthorized() throws Exception {
    login();
    componentInfoService.getComponentDetails_ReadPermission(app.getPublicId(), "reportId",
        COMPONENT_IDENTIFIER, MatchState.EXACT.getId(), "hash", false /* proprietary */, null /* httpRequest */);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetComponentDetails_ReadPermission_Unauthenticated() throws Exception {
    componentInfoService.getComponentDetails_ReadPermission(app.getPublicId(), null /* reportId */,
        COMPONENT_IDENTIFIER, MatchState.EXACT.getId(), "hash", false /* proprietary */, null /* httpRequest */);
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
