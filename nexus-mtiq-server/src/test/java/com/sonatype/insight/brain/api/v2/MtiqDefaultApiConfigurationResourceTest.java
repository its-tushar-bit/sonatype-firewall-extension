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
  public void test_putConfiguration_shouldBeBanned() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");
    properties.put(SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE, "This message is set.");

    HttpResponse response = restRequest().body(properties).put();
    assertResponseStatus(404, response);
  }

  @Test
  public void test_seleteConfiguration_shouldBeBanned() throws Exception {
    HttpResponse response = restRequest().query("property", SystemConfigurationProperty.BASE_URL,
        SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE).delete();
    assertResponseStatus(404, response);
  }
}
