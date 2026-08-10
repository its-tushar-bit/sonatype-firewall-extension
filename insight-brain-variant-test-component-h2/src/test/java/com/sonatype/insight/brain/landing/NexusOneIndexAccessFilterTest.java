/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import jakarta.inject.Inject;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentH2Test
public class NexusOneIndexAccessFilterTest
    extends AbstractComponentH2Test
{
  @Inject
  private NexusOneIndexAccessFilter filter;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private FilterChain chain;

  @AfterEach
  public void tearDownPreviewFlag() {
    tempEntity.deleteSystemConfigurationProperty(
        SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.getPropertyName());
  }

  @Test
  public void shouldAllowNexusOneIndex_flagOff_returnsFalse() {
    assertThat(filter.shouldAllowNexusOneIndex()).isFalse();
  }

  @Test
  public void shouldAllowNexusOneIndex_flagOnButAnonymous_returnsFalse() {
    tempEntity.newSystemConfigurationProperty(
        SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.getPropertyName(),
        SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.getPropertyValue());
    when(subject.getPrincipal()).thenReturn(null);

    assertThat(filter.shouldAllowNexusOneIndex()).isFalse();
  }

  @Test
  public void shouldAllowNexusOneIndex_flagOnAndAuthenticated_returnsTrue() {
    tempEntity.newSystemConfigurationProperty(
        SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.getPropertyName(),
        SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.getPropertyValue());

    assertThat(filter.shouldAllowNexusOneIndex()).isTrue();
  }

  @Test
  public void doFilter_flagOnButAnonymous_redirectsToClassicIndex() throws Exception {
    tempEntity.newSystemConfigurationProperty(
        SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.getPropertyName(),
        SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.getPropertyValue());
    when(subject.getPrincipal()).thenReturn(null);
    when(request.getContextPath()).thenReturn("/iq");

    filter.doFilter(request, response, chain);

    verify(response).sendRedirect("/iq/assets/index.html");
    verify(chain, never()).doFilter(request, response);
  }

  @Test
  public void doFilter_flagOff_redirectsToClassicIndex() throws Exception {
    when(request.getContextPath()).thenReturn("/iq");

    filter.doFilter(request, response, chain);

    verify(response).sendRedirect("/iq/assets/index.html");
    verify(chain, never()).doFilter(request, response);
  }

  @Test
  public void doFilter_flagOnAndAuthenticated_continuesChain() throws Exception {
    tempEntity.newSystemConfigurationProperty(
        SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.getPropertyName(),
        SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.getPropertyValue());

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    verify(response, never()).sendRedirect(org.mockito.ArgumentMatchers.anyString());
  }
}
