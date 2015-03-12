/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
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
import com.sonatype.clm.dto.model.application.ApplicationSummary;
import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.client.ConfigurationClient.Context;
import com.sonatype.insight.brain.dataaccess.ProprietaryConfigDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.SimpleAuthentication;

import org.apache.http.client.HttpResponseException;
import org.hamcrest.MatcherAssert;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConfigurationClientTest
    extends AbstractBrainServiceTest
{
  private void assertMatch(String pattern, String text) {
    assertTrue(text + " does not match pattern " + pattern, text != null && text.matches(pattern));
  }

  @After
  public void cleanup() throws Exception {
    File configFile = new File(getCLMServer().getDataDir(), "proprietary.json");
    assertTrue(configFile.delete() || !configFile.exists());
  }

  @Test
  public void testValidateConfiguration_AllGood() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    new ConfigurationClient(config).validateConfiguration();
  }

  @Test
  public void testValidateConfiguration_BadContextRoot() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerUrl(config.getServerUrl() + "/bad");
    try {
      new ConfigurationClient(config).validateConfiguration();
      fail("Validation should have failed due to bad context root");
    }
    catch (HttpResponseException e) {
      assertEquals(401, e.getStatusCode());
      assertThat(e.getMessage(), is("Unauthorized"));
    }
  }

  @Test
  public void testValidateConfiguration_BadAuth() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(SimpleAuthentication.parse("bad:auth"));
    try {
      new ConfigurationClient(config).validateConfiguration();
      fail("Validation should have failed due to bad authentication");
    }
    catch (HttpResponseException e) {
      assertThat(e.getStatusCode(), is(401));
      assertThat(e.getMessage(), is("Unauthorized"));
    }
  }

  @Test
  public void testValidateConfiguration_BadHost() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
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
    Configuration config = getCLMServer().getClientConfiguration();
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
    Configuration config = getCLMServer().getClientConfiguration();
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
    Configuration config = getCLMServer().getClientConfiguration();
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
    Configuration config = getCLMServer().getClientConfiguration();
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

    new ConfigurationClient(getCLMServer().getClientConfiguration()).validateApplicationId(app.getPublicId());
  }

  @Test
  public void testValidateApplicationId_UnknownId() throws Exception {
    try {
      new ConfigurationClient(getCLMServer().getClientConfiguration()).validateApplicationId("unknown-id");
      fail("Validation should have failed due to bad app id");
    }
    catch (IOException e) {
      Assert.assertEquals("Invalid application ID unknown-id.", e.getMessage());
    }
  }

  @Test
  public void testGetApplicationIdNameMap() throws Exception {
    Application app = tempEntity.newApplicationWithParent("valid-id");

    @SuppressWarnings("deprecation")
    Map<String, String> map = new ConfigurationClient(getCLMServer().getClientConfiguration())
        .getApplicationIdNameMap();

    assertEquals(1, map.size());
    assertTrue(map.containsKey("valid-id"));
    assertEquals(app.getName(), map.get("valid-id"));
    assertEquals(app.getName(), map.get("VALID-ID"));
  }

  @Test
  public void testGetApplications() throws Exception {
    Application application = tempEntity.newApplicationWithParent("valid-id");

    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(SimpleAuthentication.parse("admin:admin123"));
    ApplicationSummaryList applicationSummaryList = new ConfigurationClient(config).getApplications();

    assertThat(applicationSummaryList, notNullValue());
    assertThat(applicationSummaryList.getApplicationSummaries(), hasSize(1));
    ApplicationSummary applicationSummary = applicationSummaryList.getApplicationSummaries().get(0);
    assertThat(applicationSummary.getId(), is(application.getId()));
    assertThat(applicationSummary.getPublicId(), is(application.getPublicId()));
    assertThat(applicationSummary.getName(), is(application.getName()));
  }

  @Test
  public void testGetApplications_BadAuth() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(SimpleAuthentication.parse("bad:auth"));
    try {
      new ConfigurationClient(config).getApplications();
      fail("Request should have failed due to bad authentication");
    }
    catch (HttpResponseException e) {
      assertThat(e.getStatusCode(), is(401));
      assertThat(e.getMessage(), is("Unauthorized"));
    }
  }


  @Test
  public void testGetLicensedStages_ContextAll() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    ConfigurationClient client = new ConfigurationClient(config);

    List<Stage> stages = client.getLicensedStages(Context.ALL);
    // This rest call will return the stages in the following predefined order
    MatcherAssert.assertThat(stages, hasSize(5));
    MatcherAssert.assertThat(stages.get(0).getStageTypeId(), is(StageTypes.DEVELOP.getId()));
    MatcherAssert.assertThat(stages.get(0).getStageName(), is(StageTypes.DEVELOP.getName()));
    MatcherAssert.assertThat(stages.get(1).getStageTypeId(), is(StageTypes.BUILD.getId()));
    MatcherAssert.assertThat(stages.get(1).getStageName(), is(StageTypes.BUILD.getName()));
    MatcherAssert.assertThat(stages.get(2).getStageTypeId(), is(StageTypes.STAGE_RELEASE.getId()));
    MatcherAssert.assertThat(stages.get(2).getStageName(), is(StageTypes.STAGE_RELEASE.getName()));
    MatcherAssert.assertThat(stages.get(3).getStageTypeId(), is(StageTypes.RELEASE.getId()));
    MatcherAssert.assertThat(stages.get(3).getStageName(), is(StageTypes.RELEASE.getName()));
    MatcherAssert.assertThat(stages.get(4).getStageTypeId(), is(StageTypes.OPERATE.getId()));
    MatcherAssert.assertThat(stages.get(4).getStageName(), is(StageTypes.OPERATE.getName()));
  }

  @Test
  public void testGetLicensedStages_ContextCi() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    ConfigurationClient client = new ConfigurationClient(config);

    List<Stage> stages = client.getLicensedStages(Context.CI);
    // This rest call will return the stages in the following predefined order
    MatcherAssert.assertThat(stages, hasSize(4));
    MatcherAssert.assertThat(stages.get(0).getStageTypeId(), is(StageTypes.BUILD.getId()));
    MatcherAssert.assertThat(stages.get(0).getStageName(), is(StageTypes.BUILD.getName()));
    MatcherAssert.assertThat(stages.get(1).getStageTypeId(), is(StageTypes.STAGE_RELEASE.getId()));
    MatcherAssert.assertThat(stages.get(1).getStageName(), is(StageTypes.STAGE_RELEASE.getName()));
    MatcherAssert.assertThat(stages.get(2).getStageTypeId(), is(StageTypes.RELEASE.getId()));
    MatcherAssert.assertThat(stages.get(2).getStageName(), is(StageTypes.RELEASE.getName()));
    MatcherAssert.assertThat(stages.get(3).getStageTypeId(), is(StageTypes.OPERATE.getId()));
    MatcherAssert.assertThat(stages.get(3).getStageName(), is(StageTypes.OPERATE.getName()));
  }

  @Test
  public void testGetLicensedStages_ContextCli() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    ConfigurationClient client = new ConfigurationClient(config);

    List<Stage> stages = client.getLicensedStages(Context.ALL);
    // This rest call will return the stages in the following predefined order
    MatcherAssert.assertThat(stages, hasSize(5));
    MatcherAssert.assertThat(stages.get(0).getStageTypeId(), is(StageTypes.DEVELOP.getId()));
    MatcherAssert.assertThat(stages.get(0).getStageName(), is(StageTypes.DEVELOP.getName()));
    MatcherAssert.assertThat(stages.get(1).getStageTypeId(), is(StageTypes.BUILD.getId()));
    MatcherAssert.assertThat(stages.get(1).getStageName(), is(StageTypes.BUILD.getName()));
    MatcherAssert.assertThat(stages.get(2).getStageTypeId(), is(StageTypes.STAGE_RELEASE.getId()));
    MatcherAssert.assertThat(stages.get(2).getStageName(), is(StageTypes.STAGE_RELEASE.getName()));
    MatcherAssert.assertThat(stages.get(3).getStageTypeId(), is(StageTypes.RELEASE.getId()));
    MatcherAssert.assertThat(stages.get(3).getStageName(), is(StageTypes.RELEASE.getName()));
    MatcherAssert.assertThat(stages.get(4).getStageTypeId(), is(StageTypes.OPERATE.getId()));
    MatcherAssert.assertThat(stages.get(4).getStageName(), is(StageTypes.OPERATE.getName()));
  }

  @Test
  public void testGetLicensedStages_ContextQa() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    ConfigurationClient client = new ConfigurationClient(config);

    List<Stage> stages = client.getLicensedStages(Context.CI);
    // This rest call will return the stages in the following predefined order
    MatcherAssert.assertThat(stages, hasSize(4));
    MatcherAssert.assertThat(stages.get(0).getStageTypeId(), is(StageTypes.BUILD.getId()));
    MatcherAssert.assertThat(stages.get(0).getStageName(), is(StageTypes.BUILD.getName()));
    MatcherAssert.assertThat(stages.get(1).getStageTypeId(), is(StageTypes.STAGE_RELEASE.getId()));
    MatcherAssert.assertThat(stages.get(1).getStageName(), is(StageTypes.STAGE_RELEASE.getName()));
    MatcherAssert.assertThat(stages.get(2).getStageTypeId(), is(StageTypes.RELEASE.getId()));
    MatcherAssert.assertThat(stages.get(2).getStageName(), is(StageTypes.RELEASE.getName()));
    MatcherAssert.assertThat(stages.get(3).getStageTypeId(), is(StageTypes.OPERATE.getId()));
    MatcherAssert.assertThat(stages.get(3).getStageName(), is(StageTypes.OPERATE.getName()));
  }

  @Test
  public void testGetLicensedStages_ContextRm() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    ConfigurationClient client = new ConfigurationClient(config);

    List<Stage> stages = client.getLicensedStages(Context.CI);
    // This rest call will return the stages in the following predefined order
    MatcherAssert.assertThat(stages, hasSize(4));
    MatcherAssert.assertThat(stages.get(0).getStageTypeId(), is(StageTypes.BUILD.getId()));
    MatcherAssert.assertThat(stages.get(0).getStageName(), is(StageTypes.BUILD.getName()));
    MatcherAssert.assertThat(stages.get(1).getStageTypeId(), is(StageTypes.STAGE_RELEASE.getId()));
    MatcherAssert.assertThat(stages.get(1).getStageName(), is(StageTypes.STAGE_RELEASE.getName()));
    MatcherAssert.assertThat(stages.get(2).getStageTypeId(), is(StageTypes.RELEASE.getId()));
    MatcherAssert.assertThat(stages.get(2).getStageName(), is(StageTypes.RELEASE.getName()));
    MatcherAssert.assertThat(stages.get(3).getStageTypeId(), is(StageTypes.OPERATE.getId()));
    MatcherAssert.assertThat(stages.get(3).getStageName(), is(StageTypes.OPERATE.getName()));
  }

  @Test
  public void testGetLicensedStages_ContextMaven() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    ConfigurationClient client = new ConfigurationClient(config);

    List<Stage> stages = client.getLicensedStages(Context.ALL);
    // This rest call will return the stages in the following predefined order
    MatcherAssert.assertThat(stages, hasSize(5));
    MatcherAssert.assertThat(stages.get(0).getStageTypeId(), is(StageTypes.DEVELOP.getId()));
    MatcherAssert.assertThat(stages.get(0).getStageName(), is(StageTypes.DEVELOP.getName()));
    MatcherAssert.assertThat(stages.get(1).getStageTypeId(), is(StageTypes.BUILD.getId()));
    MatcherAssert.assertThat(stages.get(1).getStageName(), is(StageTypes.BUILD.getName()));
    MatcherAssert.assertThat(stages.get(2).getStageTypeId(), is(StageTypes.STAGE_RELEASE.getId()));
    MatcherAssert.assertThat(stages.get(2).getStageName(), is(StageTypes.STAGE_RELEASE.getName()));
    MatcherAssert.assertThat(stages.get(3).getStageTypeId(), is(StageTypes.RELEASE.getId()));
    MatcherAssert.assertThat(stages.get(3).getStageName(), is(StageTypes.RELEASE.getName()));
    MatcherAssert.assertThat(stages.get(4).getStageTypeId(), is(StageTypes.OPERATE.getId()));
    MatcherAssert.assertThat(stages.get(4).getStageName(), is(StageTypes.OPERATE.getName()));
  }

  @Test
  public void testGetProprietaryConfiguration() throws Exception {
    List<String> packages = Arrays.asList("org.sonatype", "com.sonatype");
    List<String> regexes = Arrays.asList("org.sonatype.*", "com.sonatype.*");
    ProprietaryConfig config = new ProprietaryConfig();
    config.setPackages(packages);
    config.setRegexes(regexes);
    ProprietaryConfigDAO dao = new ProprietaryConfigDAO(getCLMServer().getDataDir());
    dao.update(config);

    config = new ConfigurationClient(getCLMServer().getClientConfiguration()).getProprietaryConfiguration();

    assertEquals(packages, config.getPackages());
    assertEquals(regexes, config.getRegexes());
  }

  @Test
  public void testValidateAuthentication_ValidLogin() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(SimpleAuthentication.parse("admin:admin123"));
    ConfigurationClient client = new ConfigurationClient(config);
    client.validateAuthentication();
  }

  @Test
  public void testValidateAuthentication_InvalidPassword() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(SimpleAuthentication.parse("admin:invalidpassword"));
    ConfigurationClient client = new ConfigurationClient(config);
    try {
      client.validateAuthentication();
      fail("Expected an HttpResponseException for Unauthorized");
    }
    catch (HttpResponseException e) {
      MatcherAssert.assertThat(e.getMessage(), is("Unauthorized"));
    }
  }

  @Test
  public void testValidateAuthentication_InvalidUser() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(SimpleAuthentication.parse("invaliduser:invalidpassword"));
    ConfigurationClient client = new ConfigurationClient(config);
    try {
      client.validateAuthentication();
      fail("Expected an HttpResponseException for Unauthorized");
    }
    catch (HttpResponseException e) {
      MatcherAssert.assertThat(e.getMessage(), is("Unauthorized"));
    }
  }

  @Test
  public void testValidateAuthentication_NoAuthProvided() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    ConfigurationClient client = new ConfigurationClient(config);
    try {
      client.validateAuthentication();
      fail("Expected an HttpResponseException for Unauthorized");
    }
    catch (HttpResponseException e) {
      MatcherAssert.assertThat(e.getMessage(), is("Unauthorized"));
    }
  }
}
