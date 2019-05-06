/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiProxyConfigurationDTOV2;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyConfigurationDAO;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiProxyConfigurationServiceV2Test
    extends AbstractComponentTest
{
  @Inject
  ApiProxyConfigurationServiceV2 service;

  @Inject
  ProxyConfigurationDAO dao;

  @Test
  public void testGet_singleValue() {
    dao.setProxyExcludeHosts("example.com");

    ApiProxyConfigurationDTOV2 proxyConfiguration = service.get();

    assertThat(proxyConfiguration.getProxyExcludeHosts()).containsExactly("example.com");
  }

  @Test
  public void testGet_multipleValues() {
    dao.setProxyExcludeHosts("example.com,example.net, example.org");

    ApiProxyConfigurationDTOV2 proxyConfiguration = service.get();

    assertThat(proxyConfiguration.getProxyExcludeHosts())
        .containsExactlyInAnyOrder("example.com", "example.net", "example.org");
  }

  @Test
  public void testSet_multipleValues() {
    service.update(new ApiProxyConfigurationDTOV2("example.com,example.net, example.org"));

    String actual = dao.getProxyExcludeHosts();

    assertThat(actual).isEqualTo("example.com, example.net, example.org");
  }

  @Test
  public void testSet_ignoresWhitespaceAndEmpty() {
    service.update(new ApiProxyConfigurationDTOV2("  ,, ,\t  ,example.com,,example.net, \texample.org"));

    String actual = dao.getProxyExcludeHosts();

    assertThat(actual).isEqualTo("example.com, example.net, example.org");
  }

  @Test
  public void testSet_emptyList() {
    service.update(new ApiProxyConfigurationDTOV2());

    String actual = dao.getProxyExcludeHosts();

    assertThat(actual).isEqualTo("");
  }
}
