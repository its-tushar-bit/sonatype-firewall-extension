/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.scan.model.ClientScanType;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class ScanHandlerAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ScanHandler scanHandler;

  @Mock
  private HdsClient hdsClient;

  @Test
  public void testHandle_Authorized() throws Exception {
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);

    testHandle(app.getPublicId());
  }

  @Test
  public void testHandle_Unauthorized() {
    login();
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);

    assertThrows(UnauthorizedException.class,
        () -> scanHandler.handle(servletRequest, app.getPublicId(), ClientScanType.SONATYPE));
  }

  @Test
  public void testHandle_Unauthenticated() {
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);

    assertThrows(UnauthenticatedException.class,
        () -> scanHandler.handle(servletRequest, app.getPublicId(), ClientScanType.SONATYPE));
  }

  private void testHandle(String appPublicId) throws IOException {
    ScanReceipt scanReceipt = new ScanReceipt();
    String scanId = "test-scan-id";
    scanReceipt.setScanId(scanId);
    String scanFileContent = "test scan file content";
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    when(servletRequest.getInputStream()).thenReturn(new ServletInputStreamImpl(scanFileContent));
    when(hdsClient.put(any(HdsClientAnalytics.class), eq(ScanReceipt.class), eq(null), any(String.class),
        any(ScanEntity.class), anyMap())) //
            .thenReturn(scanReceipt);

    scanReceipt = scanHandler.handle(servletRequest, appPublicId, ClientScanType.SONATYPE);
    assertThat(scanReceipt.getScanId()).isEqualTo(scanId);
  }
}
