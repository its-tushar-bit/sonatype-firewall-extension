/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.scan.model.ClientScanType;

import com.google.inject.Binder;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScanHandlerAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ScanHandler scanHandler;

  @Mock
  private HdsClient hdsClient;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    binder.bind(HdsClient.class).toInstance(hdsClient);
  }

  @Test
  public void testHandle_Authorized_ApplicationExists() throws Exception {
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);

    testHandle(app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testHandle_Unauthorized_ApplicationExists() throws Exception {
    login();
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);

    scanHandler.handle(servletRequest, app.getPublicId(), ClientScanType.SONATYPE);
  }

  @Test
  public void testHandle_Authorized_ApplicationDoesNotExist_AutomaticApplicationCreationEnabled() throws Exception {
    String appPublicId = "NoSuchAppPublicID";
    tempEntity.registerAppPublicId(appPublicId);

    Organization org = tempEntity.newOrganization();
    AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO = new AutomaticApplicationsConfigurationDAO();
    automaticApplicationsConfigurationDAO.setOrganizationId(org.getId());
    automaticApplicationsConfigurationDAO.setEnabled(true);
    // The application will be created when the scan is handled and it will be owned by the organization configured
    // above for automatic app creation.
    // We grant the required permission to this organization, which will be inherited by all its applications.
    grantPermission(org.getId(), Permission.EVALUATE_APPLICATION);

    testHandle(appPublicId);
  }

  @Test(expected = UnauthorizedException.class)
  public void testHandle_Unauthorized_ApplicationDoesNotExist_AutomaticApplicationCreationEnabled() throws Exception {
    String appPublicId = "NoSuchAppPublicID";
    tempEntity.registerAppPublicId(appPublicId);

    Organization org = tempEntity.newOrganization();
    AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO = new AutomaticApplicationsConfigurationDAO();
    automaticApplicationsConfigurationDAO.setOrganizationId(org.getId());
    automaticApplicationsConfigurationDAO.setEnabled(true);
    login();
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);

    scanHandler.handle(servletRequest, appPublicId, ClientScanType.SONATYPE);
  }

  private void testHandle(String appPublicId) throws IOException {
    ScanReceipt scanReceipt = new ScanReceipt();
    String scanId = "test-scan-id";
    scanReceipt.setScanId(scanId);
    String scanFileContent = "test scan file content";
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    when(servletRequest.getInputStream()).thenReturn(new ServletInputStreamImpl(scanFileContent));
    when(hdsClient.get(eq(servletRequest), any(HdsClientAnalytics.class), eq(ScanReceipt.class), any(String.class),
        eq((Map<String, String>) null), any(String[].class))).thenReturn(scanReceipt);

    scanReceipt = scanHandler.handle(servletRequest, appPublicId, ClientScanType.SONATYPE);
    assertThat(scanReceipt.getScanId(), is(scanId));
  }
}
