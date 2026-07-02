/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.mail.internet.InternetAddress;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlWithFallbackDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.security.RotatableSecrets;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.MailConfiguration.MAIL_CONFIGURATION;

/**
 * @since 1.83
 */
@Named
@Singleton
public class MailConfigurationDAO
    extends AbstractOperationalSqlWithFallbackDAO<MailConfiguration>
    implements RotatableSecrets
{
  public static final String SINGLETON_ENTITY_ID = "mail-configuration";

  @Inject
  public MailConfigurationDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  /**
   * @return The mail server configuration or {@code null} if none.
   */
  public MailConfiguration get() {
    return getById(SINGLETON_ENTITY_ID);
  }

  /**
   * The REST endpoints used by the frontend should not fallback to using the default (global tenant) configuration
   * when running as multi-tenant. This allows the ui to only show the mail configuration configured for the tenant and
   * if no configuration will appear un-configured.
   */
  public MailConfiguration getWithoutFallback() {
    try (TransactionContext tx = createTransactionContext()) {
      return getWithoutFallback(tx);
    }
  }

  private MailConfiguration getWithoutFallback(TransactionContext tx) {
    return getById(tx, true);
  }

  @Override
  public MailConfiguration getById(TransactionContext tx, String id) {
    return getById(tx, false);
  }

  private MailConfiguration getById(TransactionContext tx, boolean noFallback) {
    return getWithGlobalFallback(tx, this::fetchMailConfiguration, noFallback);
  }

  /**
   * Fetches the mail configuration from the database using jOOQ.
   * This method is protected to allow test mocking.
   */
  protected MailConfiguration fetchMailConfiguration(TransactionContext tx) {
    return toEntity(tx.dsl()
        .selectFrom(MAIL_CONFIGURATION)
        .where(MAIL_CONFIGURATION.MAIL_CONFIGURATION_ID.eq(SINGLETON_ENTITY_ID))
        .fetchOne());
  }

  public void set(MailConfiguration mailConfiguration) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      MailConfiguration existing = getWithoutFallback(tx);
      if (existing == null) {
        insert(tx, mailConfiguration);
      }
      else {
        update(tx, mailConfiguration);
      }
      tx.commit();
    }
  }

  @Override
  public int insert(TransactionContext tx, MailConfiguration mailConfiguration) {
    validate(mailConfiguration);
    mailConfiguration.setId(SINGLETON_ENTITY_ID);
    return super.insert(tx, mailConfiguration);
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
      new InternetAddress(mailConfiguration.getSystemEmail(), true);
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

  @Override
  public List<MailConfiguration> getAll(TransactionContext tx) {
    return tx.dsl()
        .selectFrom(MAIL_CONFIGURATION)
        .fetch(this::toEntity);
  }

  @Override
  public Table<?> getJooqTable() {
    return MAIL_CONFIGURATION;
  }

  @Override
  public Class<MailConfiguration> getEntityClass() {
    return MailConfiguration.class;
  }
}
