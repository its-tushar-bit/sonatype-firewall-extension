/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.application.ApplicationSummary;
import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.client.ConfigurationClient.Context;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.SimpleAuthentication;

import org.apache.http.client.HttpResponseException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ConfigurationClientTest
    extends AbstractBrainServiceTest
{
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
      assertThat(e.getStatusCode()).isEqualTo(404);
      assertThat(e.getMessage()).isEqualTo("Resource not found, please check your request URL.");
    }
  }

  @Test
  public void testValidateConfiguration_AnonymousNotAllowed() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(null);
    try {
      new ConfigurationClient(config).validateConfiguration();
      fail("Validation should have failed due to anonymous not being allowed");
    }
    catch (HttpResponseException e) {
      assertThat(e.getStatusCode()).isEqualTo(401);
      assertThat(e.getMessage()).isEqualTo("Unauthorized");
    }
  }

  @Test
  @ManualServerInit
  public void testValidateConfiguration_AnonymousAllowed_AnonymousClientAccessAllowed() throws Exception {
    initServer(new Configurator() {
      @Override
      public void configure(final InsightConfig config) {
        config.setAnonymousClientAccessAllowed(true);
      }
    });
    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(null);
    new ConfigurationClient(config).validateConfiguration();
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
      assertThat(e.getStatusCode()).isEqualTo(401);
      assertThat(e.getMessage()).isEqualTo(ErrorResponseGenerator.MSG_LOGIN_FAILURE_DEFAULT);
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
      assertThat(e.getMessage()).startsWith("Unknown host:").contains("1234.bad.host.1234.com");
    }
  }

  @Test
  public void testValidateConfiguration_BadUrl() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerUrl("FFFF");
    try {
      new ConfigurationClient(config).validateConfiguration();
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid URL: FFFF");
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
      assertThat(e.getMessage()).containsPattern("(?i)Connection.* refused");
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
      assertThat(e.getMessage()).containsPattern("(?i)Illegal .* port");
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
      assertThat(e.getMessage()).startsWith("Unknown host: 1234.bad.host.1234.com");
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
      assertThat(e.getMessage()).containsPattern("(?i)Connection.* refused");
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
      assertThat(e.getMessage()).isEqualTo("Invalid application ID unknown-id.");
    }
  }

  private void assertApplicationSummaryList(ApplicationSummaryList actual, Application expected) {
    assertThat(actual).isNotNull();
    assertThat(actual.getApplicationSummaries()).hasSize(1);
    ApplicationSummary applicationSummary = actual.getApplicationSummaries().get(0);
    assertThat(applicationSummary.getId()).isEqualTo(expected.getId());
    assertThat(applicationSummary.getPublicId()).isEqualTo(expected.getPublicId());
    assertThat(applicationSummary.getName()).isEqualTo(expected.getName());
  }

  @Test
  public void testGetApplicationsForApplicationEvaluation() throws Exception {
    Application application = tempEntity.newApplicationWithParent("valid-id");
    Configuration config = createConfigForPerm(application.getId(), Permission.EVALUATE_APPLICATION);

    ApplicationSummaryList applicationSummaryList = new ConfigurationClient(config)
        .getApplicationsForApplicationEvaluation();
    assertApplicationSummaryList(applicationSummaryList, application);
  }

  @Test
  public void testGetApplicationsForApplicationEvaluation_BadAuth() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(SimpleAuthentication.parse("bad:auth"));
    try {
      new ConfigurationClient(config).getApplicationsForApplicationEvaluation();
      fail("Request should have failed due to bad authentication");
    }
    catch (HttpResponseException e) {
      assertThat(e.getStatusCode()).isEqualTo(401);
      assertThat(e.getMessage()).isEqualTo(ErrorResponseGenerator.MSG_LOGIN_FAILURE_DEFAULT);
    }
  }

  @Test
  public void testGetApplicationsForEvaluationSummary() throws Exception {
    Application application = tempEntity.newApplicationWithParent("valid-id");
    User user = tempEntity.newUser("username");
    Role role = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(application.getId(), role.getId(), user.getUsername());

    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(SimpleAuthentication.parse(user.getUsername() + ":" + user.getPassword()));
    ApplicationSummaryList applicationSummaryList = new ConfigurationClient(config)
        .getApplicationsForEvaluationSummary();
    assertApplicationSummaryList(applicationSummaryList, application);
  }

  @Test
  public void testGetLicensedStages_ContextAll() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    ConfigurationClient client = new ConfigurationClient(config);

    List<Stage> stages = client.getLicensedStages(Context.ALL);
    // This rest call will return the stages in the following predefined order
    assertThat(stages).hasSize(6);
    assertThat(stages.get(0).getStageTypeId()).isEqualTo(StageTypes.PROXY.getId());
    assertThat(stages.get(0).getStageName()).isEqualTo(StageTypes.PROXY.getName());
    assertThat(stages.get(1).getStageTypeId()).isEqualTo(StageTypes.DEVELOP.getId());
    assertThat(stages.get(1).getStageName()).isEqualTo(StageTypes.DEVELOP.getName());
    assertThat(stages.get(2).getStageTypeId()).isEqualTo(StageTypes.BUILD.getId());
    assertThat(stages.get(2).getStageName()).isEqualTo(StageTypes.BUILD.getName());
    assertThat(stages.get(3).getStageTypeId()).isEqualTo(StageTypes.STAGE_RELEASE.getId());
    assertThat(stages.get(3).getStageName()).isEqualTo(StageTypes.STAGE_RELEASE.getName());
    assertThat(stages.get(4).getStageTypeId()).isEqualTo(StageTypes.RELEASE.getId());
    assertThat(stages.get(4).getStageName()).isEqualTo(StageTypes.RELEASE.getName());
    assertThat(stages.get(5).getStageTypeId()).isEqualTo(StageTypes.OPERATE.getId());
    assertThat(stages.get(5).getStageName()).isEqualTo(StageTypes.OPERATE.getName());
  }

  @Test
  public void testGetLicensedStages_ContextCI() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    ConfigurationClient client = new ConfigurationClient(config);

    List<Stage> stages = client.getLicensedStages(Context.CI);
    // This rest call will return the stages in the following predefined order
    assertThat(stages).hasSize(4);
    assertThat(stages.get(0).getStageTypeId()).isEqualTo(StageTypes.BUILD.getId());
    assertThat(stages.get(0).getStageName()).isEqualTo(StageTypes.BUILD.getName());
    assertThat(stages.get(1).getStageTypeId()).isEqualTo(StageTypes.STAGE_RELEASE.getId());
    assertThat(stages.get(1).getStageName()).isEqualTo(StageTypes.STAGE_RELEASE.getName());
    assertThat(stages.get(2).getStageTypeId()).isEqualTo(StageTypes.RELEASE.getId());
    assertThat(stages.get(2).getStageName()).isEqualTo(StageTypes.RELEASE.getName());
    assertThat(stages.get(3).getStageTypeId()).isEqualTo(StageTypes.OPERATE.getId());
    assertThat(stages.get(3).getStageName()).isEqualTo(StageTypes.OPERATE.getName());
  }

  @Test
  public void testGetLicensedStages_ContextCli() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    ConfigurationClient client = new ConfigurationClient(config);

    List<Stage> stages = client.getLicensedStages(Context.CLI);
    // This rest call will return the stages in the following predefined order
    assertThat(stages).hasSize(5);
    assertThat(stages.get(0).getStageTypeId()).isEqualTo(StageTypes.DEVELOP.getId());
    assertThat(stages.get(0).getStageName()).isEqualTo(StageTypes.DEVELOP.getName());
    assertThat(stages.get(1).getStageTypeId()).isEqualTo(StageTypes.BUILD.getId());
    assertThat(stages.get(1).getStageName()).isEqualTo(StageTypes.BUILD.getName());
    assertThat(stages.get(2).getStageTypeId()).isEqualTo(StageTypes.STAGE_RELEASE.getId());
    assertThat(stages.get(2).getStageName()).isEqualTo(StageTypes.STAGE_RELEASE.getName());
    assertThat(stages.get(3).getStageTypeId()).isEqualTo(StageTypes.RELEASE.getId());
    assertThat(stages.get(3).getStageName()).isEqualTo(StageTypes.RELEASE.getName());
    assertThat(stages.get(4).getStageTypeId()).isEqualTo(StageTypes.OPERATE.getId());
    assertThat(stages.get(4).getStageName()).isEqualTo(StageTypes.OPERATE.getName());
  }

  @Test
  public void testGetLicensedStages_ContextQa() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    ConfigurationClient client = new ConfigurationClient(config);

    List<Stage> stages = client.getLicensedStages(Context.QA);
    // This rest call will return the stages in the following predefined order
    assertThat(stages).hasSize(4);
    assertThat(stages.get(0).getStageTypeId()).isEqualTo(StageTypes.BUILD.getId());
    assertThat(stages.get(0).getStageName()).isEqualTo(StageTypes.BUILD.getName());
    assertThat(stages.get(1).getStageTypeId()).isEqualTo(StageTypes.STAGE_RELEASE.getId());
    assertThat(stages.get(1).getStageName()).isEqualTo(StageTypes.STAGE_RELEASE.getName());
    assertThat(stages.get(2).getStageTypeId()).isEqualTo(StageTypes.RELEASE.getId());
    assertThat(stages.get(2).getStageName()).isEqualTo(StageTypes.RELEASE.getName());
    assertThat(stages.get(3).getStageTypeId()).isEqualTo(StageTypes.OPERATE.getId());
    assertThat(stages.get(3).getStageName()).isEqualTo(StageTypes.OPERATE.getName());
  }

  @Test
  public void testGetLicensedStages_ContextRm() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    ConfigurationClient client = new ConfigurationClient(config);

    List<Stage> stages = client.getLicensedStages(Context.RM);
    // This rest call will return the stages in the following predefined order
    assertThat(stages).hasSize(4);
    assertThat(stages.get(0).getStageTypeId()).isEqualTo(StageTypes.BUILD.getId());
    assertThat(stages.get(0).getStageName()).isEqualTo(StageTypes.BUILD.getName());
    assertThat(stages.get(1).getStageTypeId()).isEqualTo(StageTypes.STAGE_RELEASE.getId());
    assertThat(stages.get(1).getStageName()).isEqualTo(StageTypes.STAGE_RELEASE.getName());
    assertThat(stages.get(2).getStageTypeId()).isEqualTo(StageTypes.RELEASE.getId());
    assertThat(stages.get(2).getStageName()).isEqualTo(StageTypes.RELEASE.getName());
    assertThat(stages.get(3).getStageTypeId()).isEqualTo(StageTypes.OPERATE.getId());
    assertThat(stages.get(3).getStageName()).isEqualTo(StageTypes.OPERATE.getName());
  }

  @Test
  public void testGetLicensedStages_ContextMaven() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    ConfigurationClient client = new ConfigurationClient(config);

    List<Stage> stages = client.getLicensedStages(Context.MAVEN);
    // This rest call will return the stages in the following predefined order
    assertThat(stages).hasSize(5);
    assertThat(stages.get(0).getStageTypeId()).isEqualTo(StageTypes.DEVELOP.getId());
    assertThat(stages.get(0).getStageName()).isEqualTo(StageTypes.DEVELOP.getName());
    assertThat(stages.get(1).getStageTypeId()).isEqualTo(StageTypes.BUILD.getId());
    assertThat(stages.get(1).getStageName()).isEqualTo(StageTypes.BUILD.getName());
    assertThat(stages.get(2).getStageTypeId()).isEqualTo(StageTypes.STAGE_RELEASE.getId());
    assertThat(stages.get(2).getStageName()).isEqualTo(StageTypes.STAGE_RELEASE.getName());
    assertThat(stages.get(3).getStageTypeId()).isEqualTo(StageTypes.RELEASE.getId());
    assertThat(stages.get(3).getStageName()).isEqualTo(StageTypes.RELEASE.getName());
    assertThat(stages.get(4).getStageTypeId()).isEqualTo(StageTypes.OPERATE.getId());
    assertThat(stages.get(4).getStageName()).isEqualTo(StageTypes.OPERATE.getName());
  }

  @Test
  public void testGetProprietaryConfigForApplicationEvaluation() throws Exception {
    Application application = tempEntity.newApplicationWithParent("proprietary");
    Configuration clientConfig = createConfigForPerm(application.getId(), Permission.EVALUATE_APPLICATION);

    List<String> packages = Arrays.asList("org.sonatype", "com.sonatype");
    List<String> regexes = Arrays.asList("org.sonatype.*", "com.sonatype.*");
    tempEntity.newProprietaryConfig(application.getId(), packages, regexes);

    ProprietaryConfig config = new ConfigurationClient(clientConfig)
        .getProprietaryConfigForApplicationEvaluation(application.getPublicId());

    assertThat(config.getPackages()).isEqualTo(packages);
    assertThat(config.getRegexes()).isEqualTo(regexes);
  }

  @Test
  public void testGetProprietaryConfigForComponentEvaluation() throws Exception {
    Application application = tempEntity.newApplicationWithParent("proprietary");
    Configuration clientConfig = createConfigForPerm(application.getId(), Permission.EVALUATE_COMPONENT);

    List<String> packages = Arrays.asList("org.sonatype", "com.sonatype");
    List<String> regexes = Arrays.asList("org.sonatype.*", "com.sonatype.*");
    tempEntity.newProprietaryConfig(application.getId(), packages, regexes);

    ProprietaryConfig config = new ConfigurationClient(clientConfig)
        .getProprietaryConfigForComponentEvaluation(application.getPublicId());

    assertThat(config.getPackages()).isEqualTo(packages);
    assertThat(config.getRegexes()).isEqualTo(regexes);
  }

  private Configuration createConfigForPerm(String applicationId, Permission permission) {
    User user = tempEntity.newUser("username");
    Role role = tempEntity.newRole(false /* global */, permission);
    tempEntity.newMembershipMapping(applicationId, role.getId(), user.getUsername());

    Configuration clientConfig = getCLMServer().getClientConfiguration();
    clientConfig.setServerAuth(SimpleAuthentication.parse(user.getUsername() + ":" + user.getPassword()));
    return clientConfig;
  }

  @Test
  public void testGetFirewallIgnorePatterns() throws Exception {
    // Setup the mocked hds return
    FirewallIgnorePatterns hdsResult = new FirewallIgnorePatterns();
    hdsResult.regexpsByRepositoryFormat = new HashMap<>();
    hdsResult.regexpsByRepositoryFormat.put("foo", Collections.singletonList("bar"));
    hdsRespondWith(hdsResult).atUri("/rest/component/details/firewall/ignorePatterns");

    Configuration config = getCLMServer().getClientConfiguration();
    ConfigurationClient client = new ConfigurationClient(config);
    FirewallIgnorePatterns firewallIgnorePatterns = client.getFirewallIgnorePatterns();
    assertThat(firewallIgnorePatterns.regexpsByRepositoryFormat).isEqualTo(hdsResult.regexpsByRepositoryFormat);
  }

  @Test
  public void testVerifyOrCreateApplication() throws Exception {
    String appPublicId = "non-existent-app-public-id";

    Configuration config = getCLMServer().getClientConfiguration();
    ConfigurationClient client = new ConfigurationClient(config);
    AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO =
        new AutomaticApplicationsConfigurationDAO();
    automaticApplicationsConfigurationDAO.setOrganizationId(tempEntity.newOrganization().getId());
    automaticApplicationsConfigurationDAO.setEnabled(true);
    boolean result = client.verifyOrCreateApplication(appPublicId);
    assertThat(result).isTrue();
  }

  @Test
  public void testValidateServerVersion() throws Exception {
    VersionService versionService = getCLMServer().getInstance(VersionService.class);
    String currentServerVersion = versionService.getVersion();
    currentServerVersion = currentServerVersion.replace("-SNAPSHOT", "");

    Configuration config = getCLMServer().getClientConfiguration();
    ConfigurationClient client = new ConfigurationClient(config);

    // Verify current server version. There should be no exceptions.
    client.validateServerVersion(currentServerVersion);

    // Verify older server version. There should be no exceptions because the client requires a minimal server version
    // that is older than the current server version.
    String olderServerVersion = decrementVersion(currentServerVersion);
    client.validateServerVersion(olderServerVersion);

    // Verify newer server version. There should be an exception because the client requires a minimal server version
    // that is newer than the current server version.
    String newerServerVersion = incrementVersion(currentServerVersion);
    try {
      client.validateServerVersion(newerServerVersion);
      fail("Expected exception");
    }
    catch (UnsupportedServerVersionException expected) {
      assertThat(expected.getMessage()).isEqualTo("The IQ Server version " + currentServerVersion
          + " is not compatible. Supported IQ server versions are " + newerServerVersion + " or newer.");
    }
  }

  private String decrementVersion(String versionAsString) {
    int dotAt = versionAsString.indexOf(".");
    if (dotAt > 0) {
      return (Integer.valueOf(versionAsString.substring(0, dotAt)) - 1) + "." + versionAsString.substring(dotAt + 1);
    }
    return String.valueOf(Integer.valueOf(versionAsString) - 1);
  }

  private String incrementVersion(String versionAsString) {
    int dotAt = versionAsString.indexOf(".");
    if (dotAt > 0) {
      return (Integer.valueOf(versionAsString.substring(0, dotAt)) + 1) + "." + versionAsString.substring(dotAt + 1);
    }
    return String.valueOf(Integer.valueOf(versionAsString) + 1);
  }
}
