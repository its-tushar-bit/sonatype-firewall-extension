/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.webhook;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import com.sonatype.insight.brain.configuration.webhook.Webhook;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang.StringUtils;

public class WebhookDAO
    extends AbstractOperationalSqlDAO<Webhook>
{
  @Override
  public Webhook getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM Webhook entity" +
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public Webhook getByIdNotNull(String id) {
    Webhook webhook = getById(id);
    if (webhook == null) {
      throw new NotFoundException("Could not find a webhook with ID " + id + ".");
    }
    return webhook;
  }

  public List<Webhook> getAll() {
    String sQuery = "SELECT entity FROM Webhook entity";
    return getList(sQuery);
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

