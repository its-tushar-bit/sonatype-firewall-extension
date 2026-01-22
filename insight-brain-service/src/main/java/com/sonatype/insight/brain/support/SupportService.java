/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.configuration.ldap.LdapService;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.violation.AbstractPolicyViolationLogger;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import io.dropwizard.core.server.DefaultServerFactory;
import io.dropwizard.logging.common.AppenderFactory;
import io.dropwizard.logging.common.DefaultLoggingFactory;
import io.dropwizard.logging.common.FileAppenderFactory;
import io.dropwizard.request.logging.LogbackAccessRequestLogFactory;
import io.dropwizard.request.logging.RequestLogFactory;
import io.dropwizard.request.logging.old.LogbackClassicRequestLogFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.OrFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.27
 */
@Named
public class SupportService
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

  static final String FILENAME_PREFIX = "support-";

  private final InsightConfig config;

  private final Configuration configuration;

  private final VersionService versionService;

  private final LdapService ldapService;

  private final JmxInfo jmxInfo;

  private final DbData dbData;

  private final SystemInfo systemInfo;

  private final ConfigurationInfo configurationInfo;

  private final SourceControlConfigurationInfo sourceControlConfigurationInfo;

  private final FeaturePropertiesInfo featurePropertiesInfo;

  private final Set<File> excludedDirs;

  private final DbDiagnostics dbDiagnostics;

  private final LdapServerDAO ldapServerDAO;

  private final LdapUserMappingDAO ldapUserMappingDAO;

  private final Set<String> clusterLogFileNames;

  private final ClusterLockManager clusterLockManager;

  @Inject
  public SupportService(final InsightConfig config,
                        final Configuration configuration,
                        final VersionService versionService,
                        final LdapService ldapService,
                        final JmxInfo jmxInfo,
                        final DbData dbData,
                        final SystemInfo systemInfo,
                        final ConfigurationInfo configurationInfo,
                        final SourceControlConfigurationInfo sourceControlConfigurationInfo,
                        final FeaturePropertiesInfo featurePropertiesInfo,
                        final InsightWork work,
                        final DbDiagnostics dbDiagnostics,
                        final LdapServerDAO ldapServerDAO,
                        final LdapUserMappingDAO ldapUserMappingDAO,
                        final ClusterLockManager clusterLockManager)
  {
    this.config = config;
    this.configuration = configuration;
    this.versionService = versionService;
    this.ldapService = ldapService;
    this.jmxInfo = jmxInfo;
    this.dbData = dbData;
    this.systemInfo = systemInfo;
    this.configurationInfo = configurationInfo;
    this.sourceControlConfigurationInfo = sourceControlConfigurationInfo;
    this.featurePropertiesInfo = featurePropertiesInfo;
    this.excludedDirs = Sets.newHashSet(
        work.getReportDir(),
        work.getClusterCacheDir(),
        work.getDataDir(),
        work.getTrashDir(),
        work.getSbomDir(),
        work.getScanDir(),
        new File(config.getClusterDirectory(), "source-control"),
        new File(config.getClusterDirectory(), "temp")
    );
    this.dbDiagnostics = dbDiagnostics;
    this.ldapServerDAO = ldapServerDAO;
    this.ldapUserMappingDAO = ldapUserMappingDAO;
    this.clusterLockManager = clusterLockManager;
    this.clusterLogFileNames = Sets.newHashSet(
        "audit.log",
        "request.log",
        "clm-server.log",
        "policy-violation.log"
    );
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
    RequestLogFactory<?> requestLogFactory = ((DefaultServerFactory) config.getServerFactory()).getRequestLogFactory();
    List<String> requestLogFilenames = null;
    if (requestLogFactory instanceof LogbackAccessRequestLogFactory) {
      requestLogFilenames = getFilenames(((LogbackAccessRequestLogFactory) requestLogFactory).getAppenders());
    }
    else if (requestLogFactory instanceof LogbackClassicRequestLogFactory) {
      requestLogFilenames = getFilenames(((LogbackClassicRequestLogFactory) requestLogFactory).getAppenders());
    }
    else {
      log.warn("Cannot get list of request files. Unexpected class type for requestLogFactory: "
          + requestLogFactory.getClass().getName());
      return null;
    }

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
    return StreamSupport.stream(loggerNode.path("appenders").spliterator(), false /* parallel */)
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
      FileUtils.write(filteredConfigYml, systemInfo.getObfuscatedYaml(input), StandardCharsets.UTF_8);
    }
    return filteredConfigYml;
  }

  private File writeTextToFile(final String text, final File outputFile) throws IOException {
    FileUtils.write(outputFile, text, StandardCharsets.UTF_8);
    return outputFile;
  }

  public static final class SupportFile
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
  private String uniqueName() {
    return FILENAME_PREFIX + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + "-" +
        COUNTER.incrementAndGet();
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  File createSupportZip(final boolean includeDb, final String requestUrl, final boolean noLimit)
      throws IOException
  {
    try (ClusterLock clusterLock = clusterLockManager.createForSupportZip()) {
      // Try to acquire the lock without waiting. If another thread/node is already generating a support zip,
      // return an error immediately rather than waiting.
      if (!clusterLock.tryLock()) {
        throw new SupportZipInProgressException("Support zip generation is already in progress. " +
            "Please wait for the current operation to complete.");
      }

      final File workDir = getWorkDir();
      Files.createDirectories(workDir.toPath());

      final String prefix = uniqueName();
      final File supportZip = new File(workDir, prefix + ".zip").getCanonicalFile();
      log.info("Creating support.zip: {}", supportZip);
      if (!supportZip.createNewFile()) {
        throw new IOException("Failed to create new support.zip: " + supportZip);
      }

      final List<SupportFile> filesToZip = new ArrayList<>();
      addLogFileIfExists(filesToZip, getServerLog(config), "clm-server.log");
      addLogFileIfExists(filesToZip, getRequestLog(config), "request.log");
      addLogFileIfExists(filesToZip, new File("stderr.log"), "stderr.log");

      // audit and policy violation log files might have sensitive information, using this flag to control adding
      if (includeDb) {
        addLogFileIfExists(filesToZip, getAuditLog(config), "audit.log");
        addLogFileIfExists(filesToZip, getPolicyViolationLog(config), "policy-violation.log");
      }

      addClusterLogFiles(filesToZip);

      addFileIfExists(filesToZip, createFilteredYml(InsightBrainService.getConfigFile(), workDir), "config.yml",
          SupportFileType.CONFIG, true);

      addFileIfExists(filesToZip,
          writeTextToFile(systemInfo.getSystemInfoJson(requestUrl), new File(workDir, "sysinfo.json")), "sysinfo.json",
          SupportFileType.INFO, true);

      addFileIfExists(filesToZip,
          writeTextToFile(systemInfo.getPropertiesJson(versionService.getProperties(), "product-version"),
              new File(workDir, "product-version.json")), "product-version", SupportFileType.INFO, true);

      addFileIfExists(filesToZip,
          writeTextToFile(systemInfo.getProductLicense(), new File(workDir, "product-license.json")), "product-license",
          SupportFileType.INFO, true);

      addFileIfExists(filesToZip, writeTextToFile(systemInfo.getThreadDump(), new File(workDir, "threads.txt")),
          "threads", SupportFileType.INFO, true);

      addFileIfExists(filesToZip, writeTextToFile(jmxInfo.getJmxInfoJson(), new File(workDir, "jmx.json")), "jmx",
          SupportFileType.INFO, true);

      final List<LdapConfig> ldapServers = new ArrayList<>();
      for (final LdapServer ldapServer : ldapServerDAO.getAll()) {
        final LdapConnection ldapConnection = ldapService.getLdapConnection(ldapServer.getId());
        final LdapUserMapping ldapUserMapping = ldapUserMappingDAO.getByServerId(ldapServer.getId());
        ldapServers.add(new LdapConfig(ldapServer, ldapConnection, ldapUserMapping));
      }
      addFileIfExists(filesToZip,
          writeTextToFile(systemInfo.getLdapConfig(ldapServers), new File(workDir, "ldap.json")), "ldap",
          SupportFileType.CONFIG, true);
      addFileIfExists(filesToZip,
          writeTextToFile(systemInfo.getProxyServerConfiguration(), new File(workDir, "proxy-server.json")),
          "proxy-server", SupportFileType.CONFIG, true);
      addFileIfExists(filesToZip, writeTextToFile(systemInfo.getSamlInfo(), new File(workDir, "saml.json")), "saml",
          SupportFileType.CONFIG, true);
      addFileIfExists(filesToZip, writeTextToFile(systemInfo.getMailConfig(), new File(workDir, "mail.json")), "mail",
          SupportFileType.CONFIG, true);

      addFileIfExists(filesToZip, writeTextToFile(dbDiagnostics.getDBFileInfo(), new File(workDir, "dbFileInfo.txt")),
          "dbFileInfo", SupportFileType.INFO, true);

      addFileIfExists(filesToZip,
          writeTextToFile(configurationInfo.getConfigurationInfo(), new File(workDir, "config.json")), "config",
          SupportFileType.CONFIG, true);

      addFileIfExists(filesToZip, writeTextToFile(sourceControlConfigurationInfo.getSourceControlConfigurationInfo(),
          new File(workDir, "scm.json")), "scm", SupportFileType.CONFIG, true);

      addFileIfExists(filesToZip, writeTextToFile(featurePropertiesInfo.getSystemConfigPropertiesJson(),
              new File(workDir, "systemConfigurationProperties.json")), "system-config-properties",
          SupportFileType.CONFIG, true);

      addFileIfExists(filesToZip, writeTextToFile(featurePropertiesInfo.getFeatureConfigPropertiesJson(),
              new File(workDir, "featuresConfigurationProperties.json")), "features-config-properties",
          SupportFileType.CONFIG, true);

      addDbData(filesToZip, workDir, dbData.getMigrationTracker());
      addDbData(filesToZip, workDir, dbData.getSystemConfiguration());
      addDbData(filesToZip, workDir, dbData.getDataRetentionPolicy());
      if (includeDb) {
        addAllDbData(filesToZip, workDir);
      }

      populateZip(prefix, supportZip, filesToZip, noLimit);
      return supportZip;
    }
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
        boolean isLogFile = fileToAdd.supportFileType == SupportFileType.LOG ||
            fileToAdd.supportFileType == SupportFileType.CLUSTER_LOG;

        final ZipEntry zipEntry = new ZipEntry(
            prefix + "/" + fileToAdd.supportFileType.getDirName() + "/" + fileToAdd.file.getName());
        zos.putNextEntry(zipEntry);

        if (noLimit || !isLogFile) {
          try (FileInputStream fis = new FileInputStream(fileToAdd.file)) {
            ByteStreams.copy(fis, zos);
          }
        }
        else {
          // Limit max size of file content we allow to copy
          try (LimitedFileInputStream lis = new LimitedFileInputStream(fileToAdd.file,
              configuration.getSupportReadLimitBytes())) {
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
      output.write(TRUNCATED_TOKEN.getBytes(StandardCharsets.UTF_8));
      output.write("\n".getBytes(StandardCharsets.UTF_8));
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
    addDbData(filesToZip, workDir, dbData.getSourceControl());
    addDbData(filesToZip, workDir, dbData.getReverseProxyAuthenticationConfiguration());
    addDbData(filesToZip, workDir, dbData.getInnerSourceRepositoriesConfiguration());
    addDbData(filesToZip, workDir, dbData.getCpeMatchingConfiguration());
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

  private void addClusterLogFiles(List<SupportFile> filesToZip) {
    if (!config.isClusterDirectorySetByUser()) {
      return;
    }
    File clusterDirectory = config.getClusterDirectory();
    String clusterLogFileRegex = configuration.getSupportClusterLogFileRegex();
    RegexFileFilter clusterLogRegexFileFilter =
        new RegexFileFilter(Pattern.compile(clusterLogFileRegex), Path::toString);

    // We only want today's logs.
    Instant todayStartTime = Instant.now().truncatedTo(ChronoUnit.DAYS);
    // We substract one millisecond because AgeFileFilter works with ">", not ">=".
    AgeFileFilter ageFileFilter = new AgeFileFilter(todayStartTime.minusMillis(1), false /* acceptOlder */);

    AndFileFilter clusterLogFileFilter = new AndFileFilter(clusterLogRegexFileFilter, ageFileFilter);

    // Append list of directories to exclude to the recursive search
    IOFileFilter excludeDirectoriesFilter = getClusterExcludedDirectoriesAtTopLevelFilter();
    clusterLogFileFilter.addFileFilter(excludeDirectoriesFilter);

    try {
      List<File> clusterLogFiles = new ArrayList<>();

      File[] clusterDirRootLogFiles =  clusterDirectory.listFiles(
          (FileFilter) getClusterWhitelistLogFilesFilter());

      // Add log files at cluster dir root level
      if (clusterDirRootLogFiles != null) {
        clusterLogFiles.addAll(List.of(clusterDirRootLogFiles));
      }

      // Get only directories at the cluster dir root level (non-recursive) excluding non-necessary ones
      File[] filteredClusterTopLevelDirs =  clusterDirectory.listFiles(((FileFilter) DirectoryFileFilter.DIRECTORY
          .and(excludeDirectoriesFilter)));

      if (filteredClusterTopLevelDirs != null) {
        // Traverse recursively to find log files
        Arrays.stream(filteredClusterTopLevelDirs).forEach( topLevelDir -> {
          clusterLogFiles.addAll( FileUtils.listFiles(topLevelDir, clusterLogFileFilter,
              excludeDirFilter() ));
        });
      }

      log.debug("Found {} cluster log files matching the regex {}.", clusterLogFiles.size(), clusterLogFileRegex);
      for (File clusterLogFile : clusterLogFiles) {
        addFileIfExists(filesToZip, clusterLogFile, clusterLogFile.getName(), SupportFileType.CLUSTER_LOG, false);
      }
    }
    catch (Exception e) {
      log.error("Unable to add cluster log files matching the regex {} to the support zip {}.", clusterLogFileRegex,
          e.getMessage(), e);
    }
  }

  private IOFileFilter getClusterExcludedDirectoriesAtTopLevelFilter() {
    AndFileFilter excludeDirectoriesAtTopLevelFilter = new AndFileFilter();
    //Exclude paths at the root level of the cluster dir
    excludedDirs.forEach(directoryToExclude -> {
      //IOFileFilter prefixFileFilter = FileFilterUtils.prefixFileFilter(directoryToExclude.getPath());
      IOFileFilter prefixFileFilter = FileFilterUtils.asFileFilter(pathname ->
          pathname.getPath().startsWith(directoryToExclude.getPath()));

      NotFileFilter excludeFileWithThisPathFilter = new NotFileFilter(prefixFileFilter);

      excludeDirectoriesAtTopLevelFilter.addFileFilter( excludeFileWithThisPathFilter );
    });

    return excludeDirectoriesAtTopLevelFilter;
  }

  private IOFileFilter getClusterWhitelistLogFilesFilter() {
    OrFileFilter listOfValidLogFiles = new OrFileFilter();
    //Exclude paths at the root level of the cluster dir
    clusterLogFileNames.forEach(logFilenameToSearch -> {
      IOFileFilter regexFileFilter = new RegexFileFilter("^.*" + logFilenameToSearch + "$");
      listOfValidLogFiles.addFileFilter( regexFileFilter );
    });

    return listOfValidLogFiles;
  }

  @VisibleForTesting
  IOFileFilter excludeDirFilter() {
    List<String> dirNamesToExclude = excludedDirs.stream().map(File::getName).toList();
    return new IOFileFilter() {

      @Override
      public boolean accept(File file) {
        return dirNamesToExclude.stream()
            .noneMatch(excludeDir -> file.getAbsolutePath().replace('\\', '/').contains("/" + excludeDir));
      }

      @Override
      public boolean accept(File dir, String name) {
        return accept(new File(dir, name));
      }
    };
  }
}
