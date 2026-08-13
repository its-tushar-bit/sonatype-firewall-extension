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
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MTIQ variant conversion of {@code MtiqDefaultApiConfigurationResourceTest} (which extended
 * {@code AbstractMultiTenantBaseIntegrationResourceTest}). Pure tenant-scoped REST config CRUD: no base
 * class, an injected {@link MtiqTestContext} supplies the reused multi-tenant server, a fresh per-test
 * tenant, and REST/lookup access. Requests route to the test tenant via the shared tenant slug.
 */
@MtiqTest
class MtiqConfigurationResourceSpikeTest
{
  // Injected by MtiqServerExtension: the reused multi-tenant server + a fresh per-test tenant context.
  private MtiqTestContext ctx;

  private HttpRequest configRequest() {
    return ctx.restRequest().path(PublicApiPaths.CONFIG_RESOURCE_PATH_V2);
  }

  @Test
  void test_getConfiguration() throws Exception {
    final SystemConfigurationPropertyDAO dao = ctx.lookup(SystemConfigurationPropertyDAO.class);
    String expectedBaseUrl = "http://baseUrl/";
    String expectedMessage = "This message is set.";
    dao.set(SystemConfigurationProperty.BASE_URL, expectedBaseUrl);
    dao.set(SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE, expectedMessage);

    HttpResponse response = configRequest().query("property", SystemConfigurationProperty.BASE_URL,
        SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE).get();

    ctx.assertResponseStatus(200, response);
    Map<String, Object> result = response.getBody(Map.class);
    assertThat(result).isNotNull()
        .containsEntry(SystemConfigurationProperty.BASE_URL, expectedBaseUrl)
        .containsEntry(SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE, expectedMessage);
  }

  @Test
  void test_putConfiguration_quarantineMessage_shouldSucceed() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE, "This message is set.");

    HttpResponse response = configRequest().body(properties).put();
    ctx.assertResponseStatus(204, response);

    HttpResponse getResponse = configRequest()
        .query("property", SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE)
        .get();
    ctx.assertResponseStatus(200, getResponse);
    Map<String, Object> result = getResponse.getBody(Map.class);
    assertThat(result).containsEntry(
        SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE, "This message is set.");
  }

  @Test
  void test_deleteConfiguration_quarantineMessage_shouldSucceed() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE, "To be deleted.");
    configRequest().body(properties).put();

    HttpResponse response = configRequest()
        .query("property", SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE)
        .delete();
    ctx.assertResponseStatus(204, response);
  }

  @Test
  void test_putConfiguration_nonAllowedProperty_shouldBeRejected() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");

    HttpResponse response = configRequest().body(properties).put();
    ctx.assertResponseStatus(400, response);
  }

  @Test
  void test_putConfiguration_mixedAllowedAndNonAllowed_shouldBeRejected() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");
    properties.put(SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE, "This message is set.");

    HttpResponse response = configRequest().body(properties).put();
    ctx.assertResponseStatus(400, response);
  }

  @Test
  void test_deleteConfiguration_nonAllowedProperty_shouldBeRejected() throws Exception {
    HttpResponse response = configRequest()
        .query("property", SystemConfigurationProperty.BASE_URL)
        .delete();
    ctx.assertResponseStatus(400, response);
  }

  @Test
  void test_deleteConfiguration_mixedAllowedAndNonAllowed_shouldBeRejected() throws Exception {
    HttpResponse response = configRequest().query("property", SystemConfigurationProperty.BASE_URL,
        SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE).delete();
    ctx.assertResponseStatus(400, response);
  }
}
