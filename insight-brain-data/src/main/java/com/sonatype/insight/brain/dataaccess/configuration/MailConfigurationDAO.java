/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import javax.mail.internet.InternetAddress;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

/**
 * @since MIGRATE_MAIL_CONFIG
 */
public class MailConfigurationDAO
    extends AbstractOperationalSqlDAO<MailConfiguration>
{
  public static final String SINGLETON_ENTITY_ID = "mail-configuration";

  /**
   * @return The mail server configuration or {@code null} if none.
   */
  public MailConfiguration get() {
    return getById(SINGLETON_ENTITY_ID);
  }

  @Override
  protected MailConfiguration getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM MailConfiguration entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, SINGLETON_ENTITY_ID);
  }

  public void set(MailConfiguration mailConfiguration) {
    update(mailConfiguration);
  }

  @Override
  public void insert(TransactionContext tx, MailConfiguration mailConfiguration) {
    validate(mailConfiguration);
    mailConfiguration.setId(SINGLETON_ENTITY_ID);
    super.insert(tx, mailConfiguration);
  }

  @Override
  public void update(TransactionContext tx, MailConfiguration mailConfiguration) {
    validate(mailConfiguration);
    mailConfiguration.setId(SINGLETON_ENTITY_ID);
    super.update(tx, mailConfiguration);
  }

  public void validate(MailConfiguration mailConfiguration) {
    if (StringUtils.isBlank(mailConfiguration.getHostname())) {
      throw new BadRequestException("The SMTP host is required.");
    }
    if (mailConfiguration.getPort() <= 0 || mailConfiguration.getPort() > 65535) {
      throw new BadRequestException("The SMTP port must be from the range 1 - 65535.");
    }
    if (StringUtils.isBlank(mailConfiguration.getSystemEmail())) {
      throw new BadRequestException("The system email address is required.");
    }
    try {
      new InternetAddress(mailConfiguration.getSystemEmail());
    }
    catch (Exception e) {
      throw new BadRequestException("The system email address is malformed: " + e.getMessage(), e);
    }
  }

  public void delete() {
    MailConfiguration mailConfiguration = get();
    if (mailConfiguration != null) {
      delete(mailConfiguration);
    }
  }
}
