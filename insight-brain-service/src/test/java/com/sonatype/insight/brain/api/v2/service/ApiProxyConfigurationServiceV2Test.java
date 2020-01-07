/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiProxyConfigurationDTOV2;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyServerConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

public class ApiProxyConfigurationServiceV2Test
    extends AbstractComponentTest
{
  @Inject
  private ApiProxyConfigurationServiceV2 service;

  @Inject
  private ProxyServerConfigurationDAO dao;

  @Mock
  private ProxyServerConfigurationListener proxyServerConfigurationListener;

  @Override
  public void configure(Binder binder) {
    binder.bind(ProxyServerConfigurationListener.class).toInstance(proxyServerConfigurationListener);
    super.configure(binder);
  }

  @Before
  public void before() {
    tempEntity.setProxyServerConfiguration("localhost", 80);
  }

  @Test
  public void testGet_singleValue() {
    setExcludeHosts("example.com");

    ApiProxyConfigurationDTOV2 proxyConfiguration = service.get();

    assertThat(proxyConfiguration.getProxyExcludeHosts()).containsExactly("example.com");
  }

  @Test
  public void testGet_multipleValues() {
    setExcludeHosts("example.com,example.net, example.org");

    ApiProxyConfigurationDTOV2 proxyServerConfiguration = service.get();

    assertThat(proxyServerConfiguration.getProxyExcludeHosts())
        .containsExactlyInAnyOrder("example.com", "example.net", "example.org");
  }

  @Test
  public void testSet_multipleValues() {
    service.update(new ApiProxyConfigurationDTOV2(Arrays.asList("example.com", "example.net", "example.org")));

    ProxyServerConfiguration actual = dao.get();

    assertThat(actual.getExcludeHosts()).isEqualTo("example.com, example.net, example.org");
  }

  @Test
  public void testSet_emptyList() {
    service.update(new ApiProxyConfigurationDTOV2());

    ProxyServerConfiguration actual = dao.get();

    assertThat(actual.getExcludeHosts()).isEqualTo("");
  }

  @Test
  public void testSet_InvokeListeners() {
    service.update(new ApiProxyConfigurationDTOV2());

    verify(proxyServerConfigurationListener).proxyServerConfigurationChanged();
  }

  private void setExcludeHosts(String excludeHosts) {
    ProxyServerConfiguration proxyServerConfiguration = dao.get();
    proxyServerConfiguration.setExcludeHosts(excludeHosts);
    dao.set(proxyServerConfiguration);
  }
}
