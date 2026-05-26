/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import jakarta.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.security.AntiCsrfFilter;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.brain.security.SecurityAspectControl;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ScanResourceUnitTest
{
  @Rule
  public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @Mock
  private ScanService scanService;

  @Mock
  private ErrorResponseGenerator errorResponseGenerator;

  @Mock
  private AntiCsrfFilter antiCsrfFilter;

  private final HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);

  @Before
  public void bindSecurityManager() {
    SecurityManager securityManager = mock(SecurityManager.class);
    ThreadContext.bind(securityManager);
    SecurityAspectControl.disableEnforcement();
    SimplePrincipalCollection principals = new SimplePrincipalCollection("testUser", "testRealm");
    Subject subject = new Subject.Builder(securityManager)
        .principals(principals)
        .authenticated(true)
        .buildSubject();
    ThreadContext.bind(subject);
  }

  @After
  public void unbindSecurityManager() {
    SecurityAspectControl.enableEnforcement();
    ThreadContext.unbindSubject();
    ThreadContext.unbindSecurityManager();
  }

  @Before
  public void setUp() {
    when(HdsClient.getClientUserAgent(httpServletRequest)).thenReturn("userAgent");
  }

  @Test
  public void testUploadBinary_FilenameSelection() throws Exception {
    String appPublicId = "appPublicId";
    String filename = "app01.zip";
    InputStream is = new ByteArrayInputStream(new byte[0]);

    ScanResource scanResource = new ScanResource(scanService, errorResponseGenerator, antiCsrfFilter);

    FormDataContentDisposition formDataContentDisposition = FormDataContentDisposition.name("test")
        .fileName("Content-Disposition-Filename")
        .build();
    scanResource
        .uploadBinary(appPublicId, is, formDataContentDisposition, filename, "csrfToken", null, Stage.ID_BUILD,
            false, false, httpServletRequest);

    verify(scanService).scanBinary(eq(appPublicId), eq(is), eq(filename), any(), eq(false), eq("userAgent"), eq("ui"),
        eq(httpServletRequest));
  }

  @Test
  public void testUploadBinary_ContentDispositionFallBack() throws Exception {
    String appPublicId = "appPublicId";
    String filename = "app01.zip";
    InputStream is = new ByteArrayInputStream(new byte[0]);

    ScanResource scanResource = new ScanResource(scanService, errorResponseGenerator, antiCsrfFilter);

    FormDataContentDisposition formDataContentDisposition = FormDataContentDisposition.name("test")
        .fileName(filename)
        .build();
    scanResource
        .uploadBinary(appPublicId, is, formDataContentDisposition, null, "csrfToken", null, Stage.ID_BUILD,
            false, false, httpServletRequest);

    verify(scanService).scanBinary(eq(appPublicId), eq(is), eq(filename), any(), eq(false), eq("userAgent"), eq("ui"),
        eq(httpServletRequest));
  }
}
