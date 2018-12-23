/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.webhook.WebhookDAO;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType.POLICY_MANAGEMENT;
import static org.assertj.core.api.Assertions.assertThat;

public class DbDataTest
    extends AbstractComponentTest
{
  @Inject
  private DbData dbData;

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
}
