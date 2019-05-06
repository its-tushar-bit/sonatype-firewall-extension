/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiProxyConfigurationDTOV2;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyConfigurationDAO;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.apache.http.HttpStatus;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiProxyConfigurationResourceV2Test
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.PROXY_CONFIG_PATH_V2);
  }

  @Test
  public void testGet_defaultValue() throws Exception {
    HttpResponse response = restRequest().get();
    assertResponseStatus(HttpStatus.SC_OK, response);

    ApiProxyConfigurationDTOV2 configuration = response.getBody(ApiProxyConfigurationDTOV2.class);
    assertThat(configuration.getProxyExcludeHosts()).isEmpty();
  }

  @Test
  public void testUpdate_single() throws Exception {
    HttpResponse response = restRequest().body(new ApiProxyConfigurationDTOV2("example.com")).put();
    assertResponseStatus(HttpStatus.SC_OK, response);

    ApiProxyConfigurationDTOV2 configuration = response.getBody(ApiProxyConfigurationDTOV2.class);
    assertThat(configuration.getProxyExcludeHosts()).containsExactly("example.com");

    ProxyConfigurationDAO proxyConfigurationDAO = new ProxyConfigurationDAO();
    assertThat(proxyConfigurationDAO.getProxyExcludeHosts()).isEqualTo("example.com");
  }

  @Test
  public void testUpdate_multiple() throws Exception {
    HttpResponse response = restRequest().body(new ApiProxyConfigurationDTOV2(
        "example.com,example.org")).put();
    assertResponseStatus(HttpStatus.SC_OK, response);

    ApiProxyConfigurationDTOV2 configuration = response.getBody(ApiProxyConfigurationDTOV2.class);
    assertThat(configuration.getProxyExcludeHosts()).isEqualTo(
        asList("example.com", "example.org"));

    ProxyConfigurationDAO proxyConfigurationDAO = new ProxyConfigurationDAO();
    assertThat(proxyConfigurationDAO.getProxyExcludeHosts())
        .isEqualTo("example.com, example.org");
  }
}
