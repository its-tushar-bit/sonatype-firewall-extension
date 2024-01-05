/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.webhook;

import java.net.URI;
import java.net.URISyntaxException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

@Named
@Singleton
public class WebhookDAO
    extends AbstractOperationalSqlDAO<Webhook>
{
  @Inject
  public WebhookDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public void insert(final TransactionContext tx, final Webhook webhook) {
    validate(webhook);

    super.insert(tx, webhook);
  }

  @Override
  public void update(final TransactionContext tx, final Webhook webhook) {
    validate(webhook);

    super.update(tx, webhook);
  }

  private void validate(final Webhook webhook) {
    if (StringUtils.isBlank(webhook.getUrl())) {
      throw new BadRequestException("Webhook URL is required");
    }
    if (!webhook.getUrl().startsWith("http://") && !webhook.getUrl().startsWith("https://")) {
      throw new BadRequestException("Webhook URL must start with http:// or https://");
    }
    try {
      new URI(webhook.getUrl());
    }
    catch (URISyntaxException e) {
      throw new BadRequestException("Webhook URL is invalid: " + e.getMessage(), e);
    }
  }
}

