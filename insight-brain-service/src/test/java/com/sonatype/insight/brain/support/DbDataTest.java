/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.configuration.webhook.WebhookDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsService;

import org.junit.Test;

import static com.sonatype.insight.brain.hds.TelemetryId.TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.AUTOMATIC_APPLICATION_CREATION_ENABLED;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.AUTOMATIC_APPLICATION_CREATION_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_ENABLED;
import static com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType.POLICY_MANAGEMENT;
import static com.sonatype.nexus.scm.SourceControlProvider.BITBUCKET;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

public class DbDataTest
    extends AbstractComponentTest
{
  @Inject
  private DbData dbData;

  @Inject
  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private Webhook getWebhook() {
    @SuppressWarnings("unchecked") final List<Webhook> webhooks = (List<Webhook>) dbData.getWebhook().getValue();
    assertThat(webhooks).hasSize(1);
    return webhooks.get(0);
  }

  @Test
  public void testGetWebhook_maskSecret() throws Exception {
    tempEntity.newWebhookWithSecret(Collections.singleton(POLICY_MANAGEMENT));

    assertThat(getWebhook().getSecretKey()).isEqualTo(SystemInfo.MASK);
  }

  @Test
  public void testGetWebhook_secretEmpty() throws Exception {
    final Webhook tempWebhook = tempEntity.newWebhook(Collections.singleton(POLICY_MANAGEMENT));
    tempWebhook.setSecretKey("");
    new WebhookDAO().update(tempWebhook);

    assertThat(getWebhook().getSecretKey()).isEqualTo("");
  }

  @Test
  public void testGetWebhook_secretNull() throws Exception {
    tempEntity.newWebhook(Collections.singleton(POLICY_MANAGEMENT));

    assertThat(getWebhook().getSecretKey()).isNull();
  }

  @Test
  public void testGetSourceControl_maskToken() {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, "user", "token", BITBUCKET, true, true,
        "base_branch", new Date());

    @SuppressWarnings({"unchecked"})
    List<SourceControl> sourceControls = (List<SourceControl>) dbData.getSourceControl().getValue();

    assertThat(sourceControls).singleElement()
        .satisfies(sourceControl -> assertThat(sourceControl.getToken()).isEqualTo(SystemInfo.MASK));
  }

  @Test
  public void testGetSourceControl_tokenEmpty() {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, "user", "", BITBUCKET, true, true,
        "base_branch", new Date());

    @SuppressWarnings({"unchecked"})
    List<SourceControl> sourceControls = (List<SourceControl>) dbData.getSourceControl().getValue();

    assertThat(sourceControls).singleElement()
        .satisfies(sourceControl -> assertThat(sourceControl.getToken()).isEqualTo(""));
  }

  @Test
  public void testGetSourceControl_tokenNull() {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, "user", null, BITBUCKET, true, true,
        "base_branch", new Date());

    @SuppressWarnings({"unchecked"})
    List<SourceControl> sourceControls = (List<SourceControl>) dbData.getSourceControl().getValue();

    assertThat(sourceControls).singleElement()
        .satisfies(sourceControl -> assertThat(sourceControl.getToken()).isNull());
  }

  @Test
  public void testGetSourceControl_repositoryUrlDoesNotContainCredentials() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, null, BITBUCKET, true, true, "master", new Date());
    tempEntity.newSourceControl(application.getId(), "https://example.com/scm/project/repo.git",
        "admin", "admin", null, true, true, "base_branch", new Date());

    @SuppressWarnings({"unchecked"})
    List<SourceControl> sourceControls = (List<SourceControl>) dbData.getSourceControl().getValue();

    assertThat(sourceControls).extracting(SourceControl::getRepositoryUrl).filteredOn(Objects::nonNull)
        .containsOnly("https://example.com/scm/project/repo.git");  //.git is preserved as well
  }

  @Test
  public void testGetSourceControl_repositoryUrlContainsCredentials() {
    //given: a stored url with embedded credentials
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, null, BITBUCKET, true, true, "master", new Date());
    tempEntity.newSourceControl(application.getId(), "https://foo:bar@example.com/scm/project/repo",
        "admin", "admin", null, true, true, "base_branch", new Date());

    //when: querying SourceControl records
    @SuppressWarnings({"unchecked"})
    List<SourceControl> sourceControls = (List<SourceControl>) dbData.getSourceControl().getValue();
    
    //then: embedded credentials are stripped from the value included in support information
    assertThat(sourceControls).extracting(SourceControl::getRepositoryUrl).filteredOn(Objects::nonNull)
        .containsOnly("https://****:****@example.com/scm/project/repo");
  }

  @Test
  public void testGetSourceControl_repositoryUrlContainsUsername() {
    //given: a stored url with embedded username
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, null, BITBUCKET, true, true, "master", new Date());
    tempEntity.newSourceControl(application.getId(), "https://foo@example.com/scm/project/repo",
        "admin", "admin", null, true, true, "base_branch", new Date());

    //when: querying SourceControl records
    @SuppressWarnings({"unchecked"})
    List<SourceControl> sourceControls = (List<SourceControl>) dbData.getSourceControl().getValue();

    //then: embedded username is stripped from the value included in support information
    assertThat(sourceControls).extracting(SourceControl::getRepositoryUrl).filteredOn(Objects::nonNull)
        .containsOnly("https://****:****@example.com/scm/project/repo");
  }

  @Test
  public void testGetSystemConfiguration() {
    if (systemConfigurationPropertyDAO.getByName(TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME) == null) {
      // We need TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME which is not available by default in sample data
      tempEntity.newSystemConfigurationProperty(TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME, "Sensitive. Must be masked.");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    List<SystemConfigurationProperty> sysConfigs = (List) dbData.getSystemConfiguration().getValue();
    Map<String, String> sysProps =
        sysConfigs.stream().collect(toMap(SystemConfigurationProperty::getName, SystemConfigurationProperty::getValue));

    assertThat(sysProps)
        .containsEntry(AUTOMATIC_APPLICATION_CREATION_ENABLED, "false")
        .containsEntry(AUTOMATIC_APPLICATION_CREATION_ORGANIZATION_ID, "")
        .containsEntry(SuccessMetricsService.PROPERTY_ENABLED, "true")
        .containsEntry(TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME, SystemInfo.MASK)
        .containsEntry(AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_ENABLED, "false")
        .containsEntry(ADVANCED_SEARCH_ENABLED, "false");
  }
}
