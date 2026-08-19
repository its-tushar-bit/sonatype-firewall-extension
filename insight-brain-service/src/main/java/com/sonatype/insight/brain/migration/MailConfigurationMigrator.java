/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.util.Arrays;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightMail;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.83
 */
@Named
public class MailConfigurationMigrator
{
  private static final Logger log = LoggerFactory.getLogger(MailConfigurationMigrator.class);

  static final String OBSOLETE_CONFIG_MESSAGE = "The mail is now configured using the UI or the REST API. "
      + "The configuration in the config.yml or via system properties is obsolete.";

  static final String MIGRATION_ID = "mail-config";

  private final MigrationTrackerDAO migrationTrackerDAO;

  private final MailConfigurationDAO mailConfigurationDAO;

  private final InsightConfig insightConfig;

  private final InsightMail insightMail;

  @Inject
  public MailConfigurationMigrator(
      MigrationTrackerDAO migrationTrackerDAO,
      MailConfigurationDAO mailConfigurationDAO,
      InsightConfig insightConfig,
      InsightMail insightMail)
  {
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.mailConfigurationDAO = mailConfigurationDAO;
    this.insightConfig = insightConfig;
    this.insightMail = insightMail;
  }

  void migrate() {
    MailConfig fileConfig = insightConfig.getMailConfig();
    if (fileConfig != null) {
      log.warn(OBSOLETE_CONFIG_MESSAGE);
    }

    if (migrationTrackerDAO.isTrackerPresent(MIGRATION_ID)) {
      log.debug("Mail configuration already migrated.");
      return;
    }

    log.debug("Migrating mail configuration to database...");
    if (fileConfig == null) {
      // retain default configuration that was applied in old versions
      fileConfig = new MailConfig();
    }
    try (TransactionContext tx = mailConfigurationDAO.createTransactionContext()) {
      tx.begin();
      MailConfiguration dbConfig = new MailConfiguration();
      dbConfig.setHostname(fileConfig.getHostname());
      dbConfig.setPort(fileConfig.getPort());
      dbConfig.setUsername(fileConfig.getUsername());
      if (fileConfig.getPassword() != null) {
        dbConfig.setPassword(insightMail.encryptPassword(fileConfig.getPassword()));
        fileConfig.clearPassword();
      }
      dbConfig.setSslEnabled(fileConfig.isSsl());
      dbConfig.setStartTlsEnabled(fileConfig.isTls());
      dbConfig.setSystemEmail(fileConfig.getSystemEmail());
      try {
        mailConfigurationDAO.insert(tx, dbConfig);
      }
      catch (BadRequestException e) {
        log.warn("The current mail configuration is invalid and cannot be migrated", e);
      }
      migrationTrackerDAO.insertTracker(tx, MIGRATION_ID);
      tx.commit();
    }

    log.info("Migrated mail configuration to database.");
  }

  /**
   * Custom {@link com.sonatype.insight.mail.MailConfig} with updated defaults. We used to set them externally in
   * InsightConfig, but if someone chose to customize one of the properties then the newly deserialized class would not
   * include our changes. Setting them in the constructor means they always get applied first.
   *
   * This class should be used only for the migration of the mail configuration from config.yml to the db.
   * We need to keep this class because customers could specify only some values for the mail configuration (in
   * config.yml or system properties) and rely on other values from the default mail configuration.
   */
  public static class MailConfig
  {
    private String hostname;

    private int port;

    private boolean disableSend;

    private boolean debug;

    private boolean ssl;

    private boolean tls;

    private String username;

    private char[] password;

    private String systemEmail;

    private String systemPersonal;

    public MailConfig() {
      setHostname("127.0.0.1");
      setPort(587);
      setSystemEmail("NexusIQServer@localhost");
      setSystemPersonal("Nexus IQ Server");
    }

    public void setHostname(String hostname) {
      this.hostname = hostname;
    }

    public void setPort(int port) {
      this.port = port;
    }

    public String getHostname() {
      return hostname;
    }

    public int getPort() {
      return port;
    }

    public boolean isDisableSend() {
      return disableSend;
    }

    public boolean isDebug() {
      return debug;
    }

    public boolean isSsl() {
      return ssl;
    }

    public void setSsl(boolean ssl) {
      this.ssl = ssl;
    }

    public boolean isTls() {
      return tls;
    }

    public void setTls(boolean tls) {
      this.tls = tls;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public char[] getPassword() {
      return password;
    }

    public void setPassword(char[] password) {
      this.password = password;
    }

    public String getSystemEmail() {
      return systemEmail;
    }

    public void setSystemEmail(String systemEmail) {
      this.systemEmail = systemEmail;
    }

    public String getSystemPersonal() {
      return systemPersonal;
    }

    public void setSystemPersonal(String systemPersonal) {
      this.systemPersonal = systemPersonal;
    }

    public void clearPassword() {
      if (password != null) {
        Arrays.fill(password, '0');
      }
    }
  }
}
