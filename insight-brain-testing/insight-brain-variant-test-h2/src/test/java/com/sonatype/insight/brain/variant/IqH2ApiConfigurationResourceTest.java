/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.HashMap;
import java.util.Map;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiConfigurationResourceTest
{
  private IqTestContext ctx;

  private SystemConfigurationPropertyDAO dao;

  @BeforeEach
  void setUp() {
    dao = ctx.lookup(SystemConfigurationPropertyDAO.class);
  }

  @AfterEach
  void resetBaseUrlConfiguration() {
    // testGetConfiguration/testSetConfiguration set deployment-global BASE_URL/FORCE_BASE_URL on the shared
    // reused server without cleanup; un-force it (BASE_URL=null, FORCE_BASE_URL=false) so later tests on the
    // same server (e.g. redirect base-URL/forwarded-proto behavior) are unaffected.
    ctx.setBaseUrl(null);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.CONFIG_RESOURCE_PATH_V2);
  }

  @Test
  void testGetConfiguration_Licensed() throws Exception {
    testGetConfiguration();
  }

  @Test
  void testGetConfiguration_Unlicensed() throws Exception {
    ctx.uninstallLicense();
    testGetConfiguration();
  }

  private void testGetConfiguration() throws Exception {
    dao.set(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");
    dao.set(SystemConfigurationProperty.FORCE_BASE_URL, String.valueOf(Boolean.TRUE));

    HttpResponse response = restRequest().query("property", SystemConfigurationProperty.BASE_URL,
        SystemConfigurationProperty.FORCE_BASE_URL).get();

    ctx.assertResponseStatus(200, response);
    Map<String, Object> result = response.getBody(Map.class);
    assertThat(result).isNotNull()
        .containsEntry(SystemConfigurationProperty.BASE_URL, "http://baseUrl/")
        .containsEntry(SystemConfigurationProperty.FORCE_BASE_URL, true);
  }

  @Test
  void testGetConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().get();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains(ApiConfigurationService.NO_PROPERTIES_ERROR_MSG);
  }

  @Test
  void testSetConfiguration_Licensed() throws Exception {
    testSetConfiguration();
  }

  @Test
  void testSetConfiguration_Unlicensed() throws Exception {
    ctx.uninstallLicense();
    testSetConfiguration();
  }

  private void testSetConfiguration() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");
    properties.put(SystemConfigurationProperty.FORCE_BASE_URL, true);

    HttpResponse response = restRequest().body(properties).put();

    ctx.assertResponseStatus(204, response);
    assertThat(dao.get(SystemConfigurationProperty.BASE_URL)).isEqualTo(
        properties.get(SystemConfigurationProperty.BASE_URL));
    assertThat(dao.get(SystemConfigurationProperty.FORCE_BASE_URL)).isEqualTo(
        String.valueOf(properties.get(SystemConfigurationProperty.FORCE_BASE_URL)));
  }

  @Test
  void testSetConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().put();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains(ApiConfigurationService.NO_PROPERTIES_ERROR_MSG);
  }

  @Test
  void testDeleteConfiguration_Licensed() throws Exception {
    testDeleteConfiguration();
  }

  @Test
  void testDeleteConfiguration_Unlicensed() throws Exception {
    ctx.uninstallLicense();
    testDeleteConfiguration();
  }

  private void testDeleteConfiguration() throws Exception {
    dao.set(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");
    dao.set(SystemConfigurationProperty.FORCE_BASE_URL, String.valueOf(Boolean.TRUE));

    HttpResponse response = restRequest().query("property", SystemConfigurationProperty.BASE_URL,
        SystemConfigurationProperty.FORCE_BASE_URL).delete();

    ctx.assertResponseStatus(204, response);
    assertThat(dao.getByName(SystemConfigurationProperty.BASE_URL)).isNull();
    assertThat(dao.getByName(SystemConfigurationProperty.FORCE_BASE_URL)).isNull();
  }

  @Test
  void testDeleteConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().delete();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains(ApiConfigurationService.NO_PROPERTIES_ERROR_MSG);
  }
}
