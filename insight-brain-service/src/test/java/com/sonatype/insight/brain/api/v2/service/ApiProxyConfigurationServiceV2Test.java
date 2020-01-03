/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiProxyConfigurationDTOV2;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.ProxyConfiguration;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiProxyConfigurationServiceV2Test
    extends AbstractComponentTest
{
  @Inject
  private ApiProxyConfigurationServiceV2 service;

  @Inject
  private ProxyConfigurationDAO dao;

  @Before
  public void before() {
    tempEntity.setProxyConfiguration("localhost", 80);
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

    ApiProxyConfigurationDTOV2 proxyConfiguration = service.get();

    assertThat(proxyConfiguration.getProxyExcludeHosts())
        .containsExactlyInAnyOrder("example.com", "example.net", "example.org");
  }

  @Test
  public void testSet_multipleValues() {
    service.update(new ApiProxyConfigurationDTOV2(Arrays.asList("example.com", "example.net", "example.org")));

    ProxyConfiguration actual = dao.get();

    assertThat(actual.getExcludeHosts()).isEqualTo("example.com, example.net, example.org");
  }

  @Test
  public void testSet_emptyList() {
    service.update(new ApiProxyConfigurationDTOV2());

    ProxyConfiguration actual = dao.get();

    assertThat(actual.getExcludeHosts()).isEqualTo("");
  }

  private void setExcludeHosts(String excludeHosts) {
    ProxyConfiguration proxyConfiguration = dao.get();
    proxyConfiguration.setExcludeHosts(excludeHosts);
    dao.set(proxyConfiguration);
  }
}
