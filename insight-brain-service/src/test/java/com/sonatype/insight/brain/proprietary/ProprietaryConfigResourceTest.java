/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.proprietary;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ProprietaryConfigResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(ProprietaryConfigResource.SERVICE_PATH);
  }

  @After
  public void cleanup() throws Exception {
    File configFile = new File(getCLMServer().getDataDir(), "proprietary.json");
    assertTrue(configFile.delete() || !configFile.exists());
  }

  @Test
  public void testGet_InitialConfig() throws Exception {
    Response response = restRequest().get();
    assertResponseStatus(200, response);
    ProprietaryConfig config = fromJson(response, ProprietaryConfig.class);
    assertNotNull(config);
    assertEquals(0, config.getPackages().size());
  }

  @Test
  public void testUpdate() throws Exception {
    List<String> packages = Arrays.asList("org.sonatype", "com.sonatype");
    List<String> regexes = Arrays.asList(".*\\.zip");
    ProprietaryConfig config = new ProprietaryConfig();
    config.setPackages(packages);
    config.setRegexes(regexes);
    Response response = restRequest().path("update").body(config).put();
    assertResponseStatus(204, response);

    response = restRequest().get();
    assertResponseStatus(200, response);
    config = fromJson(response, ProprietaryConfig.class);
    assertEquals(packages, config.getPackages());
  }

  @Test
  public void testInvalidRegex() throws Exception {
    assertInvalidRegex(Arrays.asList("*"), "Dangling meta character '*' near index 0\n*\n^");
  }

  @Test
  public void testInvalidRegexNPE() throws Exception {
    List<String> regexes = new ArrayList<>();
    regexes.add(null);
    assertInvalidRegex(regexes, "null");
  }

  @Test
  public void testInvalidRegexBlacklisted() throws Exception {
    assertInvalidRegex(Arrays.asList(".*", "^.*$"), "This regex is specifically disallowed: .*\nThis regex is " +
        "specifically disallowed: ^.*$");
  }

  private void assertInvalidRegex(final List<String> regexes,
      final String expectedMessage) throws Exception {
    ProprietaryConfig config = new ProprietaryConfig();
    config.setRegexes(regexes);
    Response response = restRequest().path("update").body(config).put();
    assertResponseStatus(400, response);
    assertEquals(expectedMessage, response.getResponseBody());
  }
}
