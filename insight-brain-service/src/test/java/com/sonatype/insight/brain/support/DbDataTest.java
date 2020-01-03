/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.configuration.webhook.WebhookDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsService;

import org.junit.Test;

import static com.sonatype.insight.brain.hds.TelemetryId.TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.AUTOMATIC_APPLICATION_CREATION_ENABLED;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.AUTOMATIC_APPLICATION_CREATION_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_ENABLED;
import static com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType.POLICY_MANAGEMENT;
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
        .containsEntry(AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_ENABLED, "false");
  }
}
