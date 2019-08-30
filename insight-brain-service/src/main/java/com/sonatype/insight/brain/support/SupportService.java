/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.configuration.ldap.LdapService;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.violation.AbstractPolicyViolationLogger;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.google.common.io.ByteStreams;
import io.dropwizard.logging.AppenderFactory;
import io.dropwizard.logging.DefaultLoggingFactory;
import io.dropwizard.logging.FileAppenderFactory;
import io.dropwizard.request.logging.LogbackAccessRequestLogFactory;
import io.dropwizard.server.DefaultServerFactory;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.27
 */
@Named
class SupportService
{
  private static final Logger log = LoggerFactory.getLogger(SupportService.class);

  /**
   * Counter used to generate unique names.
   */
  private static final AtomicLong COUNTER = new AtomicLong();

  /**
   * Token added to files to indicate truncation has occurred
   */
  static final String TRUNCATED_TOKEN = "** TRUNCATED **";

  private final InsightConfig config;

  private final VersionService versionService;

  private final LdapService ldapService;

  private final JmxInfo jmxInfo;

  private final DbData dbData;

  private final SystemInfo systemInfo;

  @Inject
  public SupportService(final InsightConfig config,
                        final VersionService versionService,
                        final LdapService ldapService,
                        final JmxInfo jmxInfo,
                        final DbData dbData,
                        final SystemInfo systemInfo)
  {
    this.config = config;
    this.versionService = versionService;
    this.ldapService = ldapService;
    this.jmxInfo = jmxInfo;
    this.dbData = dbData;
    this.systemInfo = systemInfo;
  }

  File getWorkDir() {
    return new File(config.getSonatypeWork(), "downloads");
  }

  static File getServerLog(final InsightConfig config) {
    List<String> configuredLogFilenames = getFilenames(
        ((DefaultLoggingFactory) config.getLoggingFactory()).getAppenders());
    if (configuredLogFilenames.isEmpty()) {
      return null;
    }
    if (configuredLogFilenames.size() > 1) {
      log.warn("Multiple server log files {}", configuredLogFilenames);
    }
    return new File(configuredLogFilenames.get(0));
  }

  static File getRequestLog(final InsightConfig config) {
    List<String> requestLogFilenames = getFilenames(
        ((LogbackAccessRequestLogFactory) ((DefaultServerFactory) config.getServerFactory()).getRequestLogFactory())
            .getAppenders());
    if (requestLogFilenames.isEmpty()) {
      return null;
    }
    if (requestLogFilenames.size() > 1) {
      log.warn("Multiple request log files {}", requestLogFilenames);
    }
    return new File(requestLogFilenames.get(0));
  }

  static File getAuditLog(final InsightConfig config) {
    return getLogFile(config, AuditRecorder.BASE_LOGGER_NAME);
  }

  static File getPolicyViolationLog(final InsightConfig config) {
    return getLogFile(config, AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);
  }

  private static File getLogFile(final InsightConfig config, final String loggerName) {
    DefaultLoggingFactory loggingFactory = (DefaultLoggingFactory) config.getLoggingFactory();
    Map<String, JsonNode> loggers = loggingFactory.getLoggers();
    JsonNode loggerNode = loggers.getOrDefault(loggerName, MissingNode.getInstance());
    return StreamSupport.stream(loggerNode.path("appenders").spliterator(), false)
        .map(appender -> appender.path("currentLogFilename")).filter(JsonNode::isTextual)
        .map(nameNode -> new File(nameNode.asText())).findFirst().orElse(null);
  }

  private static List<String> getFilenames(List<? extends AppenderFactory<?>> appenderFactories) {
    return appenderFactories
        .stream()
        .filter(appenderFactory -> appenderFactory instanceof FileAppenderFactory)
        .map(appenderFactory -> ((FileAppenderFactory<?>) appenderFactory).getCurrentLogFilename())
        .collect(Collectors.toList());
  }

  private File createFilteredYml(final File rawYml, final File workDir) throws IOException {
    if (rawYml == null || !rawYml.exists()) {
      return null;
    }

    final File filteredConfigYml = new File(workDir, "filtered-" + rawYml.getName());

    try (final FileInputStream input = new FileInputStream(rawYml)) {
      FileUtils.write(filteredConfigYml, systemInfo.getObfuscatedYaml(input), "UTF-8");
    }
    return filteredConfigYml;
  }

  private File writeTextToFile(final String text, final File outputFile) throws IOException {
    FileUtils.write(outputFile, text, "UTF-8");
    return outputFile;
  }

  static final class SupportFile
  {
    final SupportFileType supportFileType;

    final File file;

    final boolean isDeleteAfterZipped;

    SupportFile(final SupportFileType supportFileType, final File file, final boolean isDeleteAfterZipped) {
      this.supportFileType = supportFileType;
      this.file = file;
      this.isDeleteAfterZipped = isDeleteAfterZipped;
    }
  }

  private void addLogFileIfExists(final List<SupportFile> filesToAdd,
                                  final File fileToAdd,
                                  final String skipDescription)
  {
    addFileIfExists(filesToAdd, fileToAdd, skipDescription, SupportFileType.LOG, false);
  }

  private void addFileIfExists(final List<SupportFile> files,
                               final File fileToAdd,
                               final String skipDescription,
                               final SupportFileType supportFileType,
                               final boolean isDeleteAfterZipped)
  {
    if (fileToAdd != null && fileToAdd.exists()) {
      log.info("Generating support {} file: {}", supportFileType, fileToAdd.getName());
      files.add(new SupportFile(supportFileType, fileToAdd, isDeleteAfterZipped));
    }
    else {
      log.info("Skip support {} file: {}", supportFileType, skipDescription);
    }
  }

  /**
   * Generate a unique file prefix.
   */
  private String uniqueName(final String prefix) {
    return prefix + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + "-" + COUNTER.incrementAndGet();
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  synchronized File createSupportZip(final boolean includeDb, final String requestUrl, final boolean noLimit)
      throws IOException
  {
    final File workDir = getWorkDir();
    if (!workDir.exists()) {
      if (!workDir.mkdirs()) {
        log.warn("Failed to mkdirs for: {}", workDir.getAbsolutePath());
      }
    }

    final String prefix = uniqueName("support-");
    final File supportZip = new File(workDir, prefix + ".zip").getCanonicalFile();
    log.info("Creating support.zip: {}", supportZip);
    if (!supportZip.createNewFile()) {
      throw new IOException("Failed to create new support.zip: " + supportZip);
    }

    final List<SupportFile> filesToZip = new ArrayList<>();
    addLogFileIfExists(filesToZip, getServerLog(config), "clm-server.log");
    addLogFileIfExists(filesToZip, getRequestLog(config), "request.log");

    // audit and policy violation log files might have sensitive information, using this flag to control adding
    if (includeDb) {
      addLogFileIfExists(filesToZip, getAuditLog(config), "audit.log");
      addLogFileIfExists(filesToZip, getPolicyViolationLog(config), "policy-violation.log");
    }

    addFileIfExists(filesToZip, createFilteredYml(InsightBrainService.getConfigFile(), workDir), "config.yml",
        SupportFileType.CONFIG, true);

    addFileIfExists(filesToZip,
        writeTextToFile(systemInfo.getSystemInfoJson(requestUrl), new File(workDir, "sysinfo.json")),
        "sysinfo.json", SupportFileType.INFO, true);

    addFileIfExists(filesToZip,
        writeTextToFile(systemInfo.getPropertiesJson(versionService.getProperties(), "product-version"),
            new File(workDir, "product-version.json")), "product-version", SupportFileType.INFO, true);

    addFileIfExists(filesToZip,
        writeTextToFile(systemInfo.getProductLicense(),
            new File(workDir, "product-license.json")), "product-license", SupportFileType.INFO, true);

    addFileIfExists(filesToZip,
        writeTextToFile(systemInfo.getThreadDump(), new File(workDir, "threads.txt")), "threads", SupportFileType.INFO,
        true);

    addFileIfExists(filesToZip,
        writeTextToFile(jmxInfo.getJmxInfoJson(), new File(workDir, "jmx.json")), "jmx", SupportFileType.INFO,
        true);

    final List<LdapConfig> ldapServers = new ArrayList<>();
    final LdapUserMappingDAO userDao = new LdapUserMappingDAO();
    for (final LdapServer ldapServer : new LdapServerDAO().getAll()) {
      final LdapConnection ldapConnection = ldapService.loadConnection(ldapServer.getId());
      final LdapUserMapping ldapUserMapping = userDao.getByServerId(ldapServer.getId());
      ldapServers.add(new LdapConfig(ldapServer, ldapConnection, ldapUserMapping));
    }
    addFileIfExists(filesToZip,
        writeTextToFile(systemInfo.getLdapConfig(ldapServers), new File(workDir, "ldap.json")), "ldap",
        SupportFileType.CONFIG,
        true);
    addFileIfExists(filesToZip,
        writeTextToFile(systemInfo.getSamlInfo(), new File(workDir, "saml.json")), "saml",
        SupportFileType.CONFIG,
        true);

    addFileIfExists(filesToZip,
        writeTextToFile(DbDiagnostics.getDBFileInfo(), new File(workDir, "dbFileInfo.txt")),
        "dbFileInfo",
        SupportFileType.INFO,
        true);

    addDbData(filesToZip, workDir, dbData.getMigrationTracker());
    addDbData(filesToZip, workDir, dbData.getSystemConfiguration());
    if (includeDb) {
      addAllDbData(filesToZip, workDir);
    }

    populateZip(prefix, supportZip, filesToZip, noLimit);
    return supportZip;
  }

  void populateZip(final String prefix,
                   final File supportZip,
                   final List<SupportFile> filesToZip,
                   final boolean noLimit) throws IOException
  {
    log.info("Populating support.zip: {}, noLimit: {}", supportZip, noLimit);
    try (final ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(supportZip))) {

      boolean isTruncated = false;

      for (final SupportFile fileToAdd : filesToZip) {

        final ZipEntry zipEntry = new ZipEntry(
            prefix + "/" + fileToAdd.supportFileType.getDirName() + "/" + fileToAdd.file.getName());
        zos.putNextEntry(zipEntry);

        if (noLimit) {
          try (FileInputStream fis = new FileInputStream(fileToAdd.file)) {
            ByteStreams.copy(fis, zos);
          }
        }
        else {
          // Limit max size of file content we allow to copy
          try (LimitedFileInputStream lis = new LimitedFileInputStream(fileToAdd.file,
              config.getSupportConfig().getReadLimitBytes())) {
            copyLimited(lis, zos);
            if (lis.isReadLimitMet()) {
              isTruncated = true;
            }
          }
        }

        zos.closeEntry();
        if (fileToAdd.isDeleteAfterZipped) {
          if (fileToAdd.file.exists()) {
            if (!fileToAdd.file.delete()) {
              log.warn("Failed to delete temporary support file: {}", fileToAdd.file.getAbsolutePath());
            }
          }
        }
      }
      if (isTruncated) {
        final ZipEntry zipEntry = new ZipEntry(prefix + "/truncated");
        zos.putNextEntry(zipEntry);
        zos.close();
      }
    }
    log.info("Created support.zip: {}", supportZip);
  }

  private static void copyLimited(final LimitedFileInputStream input, final OutputStream output) throws IOException {
    if (input.isToBeTruncated()) {
      output.write(TRUNCATED_TOKEN.getBytes("UTF-8"));
      output.write("\n".getBytes("UTF-8"));
    }
    ByteStreams.copy(input, output);
  }

  void addAllDbData(final List<SupportFile> filesToZip, final File workDir) throws IOException {
    addDbData(filesToZip, workDir, dbData.getRepositoryManager());
    addDbData(filesToZip, workDir, dbData.getRepository());
    addDbData(filesToZip, workDir, dbData.getOrganization());
    addDbData(filesToZip, workDir, dbData.getApplication());
    addDbData(filesToZip, workDir, dbData.getProprietaryConfig());
    addDbData(filesToZip, workDir, dbData.getUser());
    addDbData(filesToZip, workDir, dbData.getRole());
    addDbData(filesToZip, workDir, dbData.getRolePermission());
    addDbData(filesToZip, workDir, dbData.getMembershipMapping());
    addDbData(filesToZip, workDir, dbData.getWebhook());
    addDbData(filesToZip, workDir, dbData.getSystemNotice());
    addDbData(filesToZip, workDir, dbData.getLabel());
    addDbData(filesToZip, workDir, dbData.getComponentLabel());
    addDbData(filesToZip, workDir, dbData.getTag());
    addDbData(filesToZip, workDir, dbData.getApplicationTag());
    addDbData(filesToZip, workDir, dbData.getPolicyTag());
    addDbData(filesToZip, workDir, dbData.getSecurityVulnerabilityOverride());
    addDbData(filesToZip, workDir, dbData.getLicenseThreatGroup());
    addDbData(filesToZip, workDir, dbData.getMultiLicense());
    addDbData(filesToZip, workDir, dbData.getLicense());
    addDbData(filesToZip, workDir, dbData.getLicenseThreatGroupLicense());
    addDbData(filesToZip, workDir, dbData.getPolicy());
    addDbData(filesToZip, workDir, dbData.getPolicyMonitoring());
  }

  private void addDbData(final List<SupportFile> filesToZip,
                         final File workDir,
                         final Entry<String, Object> entry)
      throws IOException
  {
    final String keyname = entry.getKey();
    addFileIfExists(filesToZip, writeTextToFile(JsonUtils.format(entry), new File(workDir, keyname + ".json")),
        keyname, SupportFileType.DB, true);
  }
}

