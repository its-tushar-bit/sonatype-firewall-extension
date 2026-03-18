/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiReverseProxyAuthenticationConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiReverseProxyAuthenticationConfigurationService;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.configuration.ReverseProxyAuthenticationConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.ReverseProxyAuthenticationConfiguration;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiReverseProxyAuthenticationConfigurationResourceTest
    extends AbstractResourceTest
{
  private ReverseProxyAuthenticationConfigurationDAO dao;

  @Before
  public void setUp() {
    dao = lookup(ReverseProxyAuthenticationConfigurationDAO.class);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.REVERSE_PROXY_AUTHENTICATION_CONFIG_RESOURCE_PATH_V2);
  }

  @Test
  public void testGetConfiguration() throws Exception {
    ReverseProxyAuthenticationConfiguration config = tempEntity.newReverseProxyAuthenticationConfiguration();

    HttpResponse response = restRequest().get();

    assertResponseStatus(200, response);
    assertThat(response.getBody(ApiReverseProxyAuthenticationConfigurationDTO.class)).usingRecursiveComparison()
        .ignoringExpectedNullFields()
        .isEqualTo(config);
  }

  @Test
  public void testGetConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().get();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).contains(ReverseProxyAuthenticationConfigurationDAO.NOT_FOUND_ERROR_MSG);
  }

  @Test
  public void testSetConfiguration() throws Exception {
    ApiReverseProxyAuthenticationConfigurationDTO dto = new ApiReverseProxyAuthenticationConfigurationDTO();
    dto.enabled = true;
    dto.usernameHeader = "usernameHeader";
    dto.csrfProtectionDisabled = true;
    dto.logoutUrl = "logoutUrl";

    HttpResponse response = restRequest().body(dto).put();

    assertResponseStatus(204, response);
    assertThat(dao.get()).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .ignoringFields("id")
        .isEqualTo(dto);
  }

  @Test
  public void testSetConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains(ApiReverseProxyAuthenticationConfigurationService.NO_DTO_ERROR_MSG);
  }

  @Test
  public void testDeleteConfiguration() throws Exception {
    tempEntity.newReverseProxyAuthenticationConfiguration();

    HttpResponse response = restRequest().delete();

    assertResponseStatus(204, response);
    assertThat(dao.get()).isNull();
  }

  @Test
  public void testDeleteConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().delete();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).contains(ReverseProxyAuthenticationConfigurationDAO.NOT_FOUND_ERROR_MSG);
  }
}
