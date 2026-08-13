/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiReverseProxyAuthenticationConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiReverseProxyAuthenticationConfigurationService;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.configuration.ReverseProxyAuthenticationConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.ReverseProxyAuthenticationConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiReverseProxyAuthenticationConfigurationResourceTest
{
  private IqTestContext ctx;

  private ReverseProxyAuthenticationConfigurationDAO dao;

  @BeforeEach
  void setUp() {
    dao = ctx.lookup(ReverseProxyAuthenticationConfigurationDAO.class);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.REVERSE_PROXY_AUTHENTICATION_CONFIG_RESOURCE_PATH_V2);
  }

  @Test
  void testGetConfiguration() throws Exception {
    ReverseProxyAuthenticationConfiguration config = ctx.tempEntity().newReverseProxyAuthenticationConfiguration();

    HttpResponse response = restRequest().get();

    ctx.assertResponseStatus(200, response);
    assertThat(response.getBody(ApiReverseProxyAuthenticationConfigurationDTO.class)).usingRecursiveComparison()
        .ignoringExpectedNullFields()
        .isEqualTo(config);
  }

  @Test
  void testGetConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().get();

    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).contains(ReverseProxyAuthenticationConfigurationDAO.NOT_FOUND_ERROR_MSG);
  }

  @Test
  void testSetConfiguration() throws Exception {
    ApiReverseProxyAuthenticationConfigurationDTO dto = new ApiReverseProxyAuthenticationConfigurationDTO();
    dto.enabled = true;
    dto.usernameHeader = "usernameHeader";
    dto.csrfProtectionDisabled = true;
    dto.logoutUrl = "logoutUrl";

    HttpResponse response = restRequest().body(dto).put();

    ctx.assertResponseStatus(204, response);
    assertThat(dao.get()).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .ignoringFields("id")
        .isEqualTo(dto);
  }

  @Test
  void testSetConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().put();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains(ApiReverseProxyAuthenticationConfigurationService.NO_DTO_ERROR_MSG);
  }

  @Test
  void testDeleteConfiguration() throws Exception {
    ctx.tempEntity().newReverseProxyAuthenticationConfiguration();

    HttpResponse response = restRequest().delete();

    ctx.assertResponseStatus(204, response);
    assertThat(dao.get()).isNull();
  }

  @Test
  void testDeleteConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().delete();

    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).contains(ReverseProxyAuthenticationConfigurationDAO.NOT_FOUND_ERROR_MSG);
  }
}
