/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.brain.dataaccess.ProprietaryConfigDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractLicenseTest;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;

import org.apache.http.client.HttpResponseException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConfigurationClientTest
    extends AbstractLicenseTest
{
  private void assertMatch(String pattern, String text) {
    assertTrue(text + " does not match pattern " + pattern, text != null && text.matches(pattern));
  }

  @After
  public void cleanup() throws Exception {
    File configFile = new File(brain.getDataDir(), "proprietary.json");
    assertTrue(configFile.delete() || !configFile.exists());
  }

  @Test
  public void testValidateConfiguration_AllGood() throws Exception {
    Configuration config = brain.getClientConfiguration();
    new ConfigurationClient(config).validateConfiguration();
  }

  @Test
  public void testValidateConfiguration_BadContextRoot() throws Exception {
    Configuration config = brain.getClientConfiguration();
    config.setServerUrl(config.getServerUrl() + "/bad");
    try {
      new ConfigurationClient(config).validateConfiguration();
      fail("Validation should have failed due to bad context root");
    }
    catch (HttpResponseException e) {
      assertEquals(401, e.getStatusCode());
      assertMatch("(?i).*not found.*", e.getMessage());
    }
  }

  @Test
  public void testValidateConfiguration_BadHost() throws Exception {
    Configuration config = brain.getClientConfiguration();
    config.setServerUrl("http://1234.bad.host.1234.com/");
    try {
      new ConfigurationClient(config).validateConfiguration();
      fail("Validation should have failed due to bad host");
    }
    catch (IOException e) {
      assertThat(e.getMessage(), startsWith("Unknown host: 1234.bad.host.1234.com"));
    }
  }

  @Test
  public void testValidateConfiguration_BadPort() throws Exception {
    Configuration config = brain.getClientConfiguration();
    config.setServerUrl("http://localhost:65535/");
    try {
      new ConfigurationClient(config).validateConfiguration();
      fail("Validation should have failed due to bad port");
    }
    catch (IOException e) {
      assertMatch("(?i).*Connection.* refused.*", e.getMessage());
    }
  }

  @Test
  public void testValidateConfiguration_InvalidPort() throws Exception {
    Configuration config = brain.getClientConfiguration();
    config.setServerUrl("http://localhost:NaN/");
    try {
      new ConfigurationClient(config).validateConfiguration();
      fail("Validation should have failed due to invalid port");
    }
    catch (Exception e) {
      assertMatch("(?i).*Illegal .* port.*", e.getMessage());
    }
  }

  @Test
  public void testValidateConfiguration_BadProxyHost() throws Exception {
    Configuration config = brain.getClientConfiguration();
    config.setProxy("1234.bad.host.1234.com");
    try {
      new ConfigurationClient(config).validateConfiguration();
      fail("Validation should have failed due to bad proxy host");
    }
    catch (IOException e) {
      assertThat(e.getMessage(), startsWith("Unknown host: 1234.bad.host.1234.com"));
    }
  }

  @Test
  public void testValidateConfiguration_BadProxyPort() throws Exception {
    Configuration config = brain.getClientConfiguration();
    config.setProxy("localhost:65535");
    try {
      new ConfigurationClient(config).validateConfiguration();
      fail("Validation should have failed due to bad proxy port");
    }
    catch (IOException e) {
      assertMatch("(?i).*Connection.* refused.*", e.getMessage());
    }
  }

  @Test
  public void testValidateApplicationId_AllGood() throws Exception {
    Application app = tempEntity.newApplicationWithParent("valid-id");

    new ConfigurationClient(brain.getClientConfiguration()).validateApplicationId(app.getPublicId());
  }

  @Test
  public void testValidateApplicationId_UnknownId() throws Exception {
    try {
      new ConfigurationClient(brain.getClientConfiguration()).validateApplicationId("unknown-id");
      fail("Validation should have failed due to bad app id");
    }
    catch (IOException e) {
      Assert.assertEquals("Invalid application id unknown-id", e.getMessage());
    }
  }

  @Test
  public void testGetApplicationIdNameMap() throws Exception {
    Application app = tempEntity.newApplicationWithParent("valid-id");

    Map<String, String> map = new ConfigurationClient(brain.getClientConfiguration()).getApplicationIdNameMap();

    assertEquals(1, map.size());
    assertTrue(map.containsKey("valid-id"));
    assertEquals(app.getName(), map.get("valid-id"));
    assertEquals(app.getName(), map.get("VALID-ID"));
  }

  @Test
  public void testGetProprietaryConfiguration() throws Exception {
    List<String> packages = Arrays.asList("org.sonatype", "com.sonatype");
    ProprietaryConfig config = new ProprietaryConfig();
    config.setPackages(packages);
    ProprietaryConfigDAO dao = new ProprietaryConfigDAO(brain.getDataDir());
    dao.update(config);

    config = new ConfigurationClient(brain.getClientConfiguration()).getProprietaryConfiguration();

    assertEquals(packages, config.getPackages());
  }
}
