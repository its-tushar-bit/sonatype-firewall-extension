/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.aop.MethodInvocation;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HasFeatureMethodInterceptorTest
    extends AbstractComponentTest
{
  private MethodInvocation invoc;

  private HasFeatureMethodInterceptor interceptor;

  @HasFeature(SystemConfigurationPropertyFeature.CODE_INSIGHTS)
  public String stubMethod(String arg0) {
    return arg0;
  }

  @Before
  public void init() {
    invoc = mock(MethodInvocation.class);
    interceptor = new HasFeatureMethodInterceptor();
  }

  @Test
  public void testInvoke_Pass() throws Throwable {
    when(invoc.getMethod()).thenReturn(getClass().getMethod("stubMethod", String.class));
    when(invoc.proceed()).thenReturn("test");

    SystemConfigurationPropertyFeature.CODE_INSIGHTS.setEnabled(true);
    assertThat(SystemConfigurationPropertyFeature.CODE_INSIGHTS.isEnabled()).isTrue();

    assertThat(interceptor.invoke(invoc)).isEqualTo("test");
  }

  @Test
  public void testInvoke_FailWithException() throws Throwable {
    when(invoc.getMethod()).thenReturn(getClass().getMethod("stubMethod", String.class));

    SystemConfigurationPropertyFeature.CODE_INSIGHTS.setEnabled(false);
    assertThat(SystemConfigurationPropertyFeature.CODE_INSIGHTS.isEnabled()).isFalse();

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> interceptor.invoke(invoc))
        .withMessage("Feature not supported.");
  }
}
