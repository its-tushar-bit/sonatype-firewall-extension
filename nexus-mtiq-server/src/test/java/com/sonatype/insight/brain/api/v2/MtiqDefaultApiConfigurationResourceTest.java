/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.HashMap;
import java.util.Map;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class MtiqDefaultApiConfigurationResourceTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.CONFIG_RESOURCE_PATH_V2);
  }

  @Test
  public void test_getConfiguration() throws Exception {
    final SystemConfigurationPropertyDAO dao = getCLMServer().getInstance(SystemConfigurationPropertyDAO.class);
    String expectedBaseUrl = "http://baseUrl/";
    String expectedMessage = "This message is set.";
    dao.set(SystemConfigurationProperty.BASE_URL, expectedBaseUrl);
    dao.set(SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE, expectedMessage);

    HttpResponse response = restRequest().query("property", SystemConfigurationProperty.BASE_URL,
        SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE).get();

    assertResponseStatus(200, response);
    Map<String, Object> result = response.getBody(Map.class);
    assertThat(result).isNotNull()
        .containsEntry(SystemConfigurationProperty.BASE_URL, expectedBaseUrl)
        .containsEntry(SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE, expectedMessage);
  }

  @Test
  public void test_putConfiguration_quarantineMessage_shouldSucceed() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE, "This message is set.");

    HttpResponse response = restRequest().body(properties).put();
    assertResponseStatus(204, response);

    HttpResponse getResponse = restRequest()
        .query("property", SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE)
        .get();
    assertResponseStatus(200, getResponse);
    Map<String, Object> result = getResponse.getBody(Map.class);
    assertThat(result).containsEntry(
        SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE, "This message is set.");
  }

  @Test
  public void test_deleteConfiguration_quarantineMessage_shouldSucceed() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE, "To be deleted.");
    restRequest().body(properties).put();

    HttpResponse response = restRequest()
        .query("property", SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE)
        .delete();
    assertResponseStatus(204, response);
  }

  @Test
  public void test_putConfiguration_nonAllowedProperty_shouldBeRejected() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");

    HttpResponse response = restRequest().body(properties).put();
    assertResponseStatus(400, response);
  }

  @Test
  public void test_putConfiguration_mixedAllowedAndNonAllowed_shouldBeRejected() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");
    properties.put(SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE, "This message is set.");

    HttpResponse response = restRequest().body(properties).put();
    assertResponseStatus(400, response);
  }

  @Test
  public void test_deleteConfiguration_nonAllowedProperty_shouldBeRejected() throws Exception {
    HttpResponse response = restRequest()
        .query("property", SystemConfigurationProperty.BASE_URL)
        .delete();
    assertResponseStatus(400, response);
  }

  @Test
  public void test_deleteConfiguration_mixedAllowedAndNonAllowed_shouldBeRejected() throws Exception {
    HttpResponse response = restRequest().query("property", SystemConfigurationProperty.BASE_URL,
        SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE).delete();
    assertResponseStatus(400, response);
  }
}
