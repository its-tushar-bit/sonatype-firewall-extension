/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.HashMap;
import java.util.Map;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiConfigurationResourceTest
    extends AbstractResourceTest
{
  private SystemConfigurationPropertyDAO dao;

  @Before
  public void setUp() {
    dao = lookup(SystemConfigurationPropertyDAO.class);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.CONFIG_RESOURCE_PATH_V2);
  }

  @Test
  public void testGetConfiguration_Licensed() throws Exception {
    testGetConfiguration();
  }

  @Test
  public void testGetConfiguration_Unlicensed() throws Exception {
    uninstallLicense();
    testGetConfiguration();
  }

  private void testGetConfiguration() throws Exception {
    dao.set(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");
    dao.set(SystemConfigurationProperty.FORCE_BASE_URL, String.valueOf(Boolean.TRUE));

    HttpResponse response = restRequest().query("property", SystemConfigurationProperty.BASE_URL,
        SystemConfigurationProperty.FORCE_BASE_URL).get();

    assertResponseStatus(200, response);
    Map<String, Object> result = response.getBody(Map.class);
    assertThat(result).isNotNull()
        .containsEntry(SystemConfigurationProperty.BASE_URL, "http://baseUrl/")
        .containsEntry(SystemConfigurationProperty.FORCE_BASE_URL, true);
  }

  @Test
  public void testGetConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().get();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains(ApiConfigurationService.NO_PROPERTIES_ERROR_MSG);
  }

  @Test
  public void testSetConfiguration_Licensed() throws Exception {
    testSetConfiguration();
  }

  @Test
  public void testSetConfiguration_Unlicensed() throws Exception {
    uninstallLicense();
    testSetConfiguration();
  }

  private void testSetConfiguration() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");
    properties.put(SystemConfigurationProperty.FORCE_BASE_URL, true);

    HttpResponse response = restRequest().body(properties).put();

    assertResponseStatus(204, response);
    assertThat(dao.get(SystemConfigurationProperty.BASE_URL)).isEqualTo(
        properties.get(SystemConfigurationProperty.BASE_URL));
    assertThat(dao.get(SystemConfigurationProperty.FORCE_BASE_URL)).isEqualTo(
        String.valueOf(properties.get(SystemConfigurationProperty.FORCE_BASE_URL)));
  }

  @Test
  public void testSetConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains(ApiConfigurationService.NO_PROPERTIES_ERROR_MSG);
  }

  @Test
  public void testDeleteConfiguration_Licensed() throws Exception {
    testDeleteConfiguration();
  }

  @Test
  public void testDeleteConfiguration_Unlicensed() throws Exception {
    uninstallLicense();
    testDeleteConfiguration();
  }

  private void testDeleteConfiguration() throws Exception {
    dao.set(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");
    dao.set(SystemConfigurationProperty.FORCE_BASE_URL, String.valueOf(Boolean.TRUE));

    HttpResponse response = restRequest().query("property", SystemConfigurationProperty.BASE_URL,
        SystemConfigurationProperty.FORCE_BASE_URL).delete();

    assertResponseStatus(204, response);
    assertThat(dao.getByName(SystemConfigurationProperty.BASE_URL)).isNull();
    assertThat(dao.getByName(SystemConfigurationProperty.FORCE_BASE_URL)).isNull();
  }

  @Test
  public void testDeleteConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().delete();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains(ApiConfigurationService.NO_PROPERTIES_ERROR_MSG);
  }
}
