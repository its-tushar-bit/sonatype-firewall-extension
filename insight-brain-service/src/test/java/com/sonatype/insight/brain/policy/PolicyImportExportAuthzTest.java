/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.net.URI;

import javax.inject.Inject;
import javax.ws.rs.core.UriInfo;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightConfig;

import com.google.inject.Binder;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PolicyImportExportAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private PolicyImportExport policyImportExport;

  @Mock
  private UriInfo uriInfo;

  private InsightConfig insightConfig;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    insightConfig = new InsightConfig();
    insightConfig.setBaseUrl("base");
    when(uriInfo.getRequestUri()).thenReturn(URI.create("whatever"));
    binder.bind(BaseUrl.class).toInstance(new BaseUrl(insightConfig, uriInfo));
  }

  @Test(expected = UnauthorizedException.class)
  public void testExportApplication_Unauthorized() throws Exception {
    login();
    policyImportExport.exportApplication(app);
  }

  @Test
  public void testExportApplication_Authorized() throws Exception {
    grantReadPermission(app.getId());
    policyImportExport.exportApplication(app);
  }

  @Test(expected = UnauthorizedException.class)
  public void testExportOrganization_Unauthorized() throws Exception {
    login();
    policyImportExport.exportOrganization(org);
  }

  @Test
  public void testExportOrganization_Authorized() throws Exception {
    grantReadPermission(org.getId());
    policyImportExport.exportOrganization(org);
  }

  @Test(expected = UnauthorizedException.class)
  public void testImportApplication_Unauthorized() throws Exception {
    login();
    policyImportExport.importApplication(app, new PolicyExportResult());
  }

  @Test
  public void testImportApplication_Authorized() throws Exception {
    grantWritePermission(app.getId());
    policyImportExport.importApplication(app, new PolicyExportResult());
  }

  @Test(expected = UnauthorizedException.class)
  public void testImportOrganization_Unauthorized() throws Exception {
    login();
    policyImportExport.importOrganization(org, new PolicyExportResult());
  }

  @Test
  public void testImportOrganization_Authorized() throws Exception {
    grantWritePermission(org.getId());
    policyImportExport.importOrganization(org, new PolicyExportResult());
  }
}
