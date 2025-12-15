/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyServerConfigurationDAO;
import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.ProductLicensingModel;
import com.sonatype.insight.brain.security.SamlDeploymentManager;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.Feature;

import com.codahale.metrics.jvm.ThreadDump;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

/**
 * @since 1.27
 */
@Named
@Singleton
public class SystemInfo
{
  private static final Logger log = LoggerFactory.getLogger(SystemInfo.class);

  public static final String MASK = "****";

  private static Entry<String, SortedMap<String, Object>> wrapEntry(final String entryName,
                                                                    final SortedMap<String, Object> objectToPut)
  {
    return new AbstractMap.SimpleImmutableEntry<>(entryName, objectToPut);
  }

  private final InsightConfig insightConfig;

  private final InsightWork insightWork;

  private final ProductLicense productLicense;

  private final CLMLicenseManager clmLicenseManager;

  private final SamlDeploymentManager samlDeploymentManager;

  private final SamlConfigurationService samlConfigurationService;

  private final MailConfigurationDAO mailConfigurationDAO;

  private final ProxyServerConfigurationDAO proxyServerConfigurationDAO;

  @Inject
  SystemInfo(
      final InsightConfig insightConfig,
      final InsightWork insightWork,
      final ProductLicense productLicense,
      final CLMLicenseManager clmLicenseManager,
      final SamlConfigurationService samlConfigurationService,
      final MailConfigurationDAO mailConfigurationDAO,
      final ProxyServerConfigurationDAO proxyServerConfigurationDAO,
      SamlDeploymentManager samlDeploymentManager)
  {
    this.insightConfig = insightConfig;
    this.insightWork = insightWork;
    this.productLicense = productLicense;
    this.clmLicenseManager = clmLicenseManager;
    this.samlConfigurationService = samlConfigurationService;
    this.mailConfigurationDAO = mailConfigurationDAO;
    this.proxyServerConfigurationDAO = proxyServerConfigurationDAO;
    this.samlDeploymentManager = samlDeploymentManager;
  }

  static boolean isSensitiveKey(final String key) {
    final String lowercaseKey = key.toLowerCase(Locale.ENGLISH);
    return lowercaseKey.contains("password") || lowercaseKey.contains("passphrase");
  }

  Object obfuscateValue(final String key, final Object value) {
    if (isSensitiveKey(key)) {
      return MASK;
    }
    else {
      return value;
    }
  }

  Entry<String, SortedMap<String, Object>> getObfuscatedSystemProperties() {
    final SortedMap<String, Object> entries = new TreeMap<>();

    final Properties iterationSafeCopy = (Properties) System.getProperties().clone();
    for (final Entry<Object, Object> entry : iterationSafeCopy.entrySet()) {
      final String key = entry.getKey() + "";
      entries.put(key, obfuscateValue(key, entry.getValue()));
    }

    return wrapEntry("system-properties", entries);
  }

  /**
   * Gets a Map.Entry of system properties and applies filter to each key of the map
   *
   * @param filter    The filter to be applied to the map
   * @param entryName The entry name for the Map.Entry with the system properties
   * @return The Map.Entry with the filter applied
   */
  Entry<String, SortedMap<String, Object>> getObfuscatedSystemProperties(String filter, String entryName) {
    final SortedMap<String, Object> entries = new TreeMap<>();
    final Properties iterationSafeCopy = (Properties) System.getProperties().clone();

    for (final Entry<Object, Object> entry : iterationSafeCopy.entrySet()) {
      if (entry.getKey().toString().startsWith(filter)) {
        final String key = entry.getKey() + "";
        entries.put(key, obfuscateValue(key, entry.getValue()));
      }
    }

    return wrapEntry(entryName, entries);
  }

  Entry<String, SortedMap<String, Object>> getObfuscatedEnvironment() {
    final SortedMap<String, Object> entries = new TreeMap<>();

    final Map<String, String> systemEnvironment = System.getenv();
    for (final Entry<String, String> entry : systemEnvironment.entrySet()) {
      final String key = entry.getKey() + "";
      entries.put(key, obfuscateValue(key, entry.getValue()));
    }

    return wrapEntry("system-environment", entries);
  }

  String getObfuscatedYaml(final InputStream input) {
    DumperOptions dumperOptions = new DumperOptions();
    dumperOptions.setDefaultFlowStyle(FlowStyle.BLOCK);
    final Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()), new Representer(dumperOptions), dumperOptions);
    Object yamlObject = yaml.load(input);
    obfuscateYaml(yamlObject);
    return yaml.dump(yamlObject);
  }

  private void obfuscateYaml(Object yamlObject) {
    if (yamlObject instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) yamlObject;
      for (Map.Entry<String, Object> entry : map.entrySet()) {
        if (isSensitiveKey(entry.getKey())) {
          entry.setValue(SystemInfo.MASK);
        }
        else {
          obfuscateYaml(entry.getValue());
        }
      }
    }
    else if (yamlObject instanceof Iterable) {
      Iterable<?> iterable = (Iterable<?>) yamlObject;
      for (Object element : iterable) {
        obfuscateYaml(element);
      }
    }
  }

  Entry<String, SortedMap<String, Object>> getReportTime() {
    final SortedMap<String, Object> entries = new TreeMap<>();

    final Date now = new Date();
    entries.put("timezone", TimeZone.getDefault().getID());
    entries.put("current", now.getTime() + "");
    entries.put("iso8601", new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ss.SSSZ").format(now));

    return wrapEntry("system-time", entries);
  }

  Entry<String, SortedMap<String, Object>> getInstallInfo() {
    final SortedMap<String, Object> entries = new TreeMap<>();

    final Class<?> brainClass = InsightBrainService.class;
    entries.put("application-jar", brainClass.getResource('/' + brainClass.getName().replace('.', '/') + ".class"));

    entries.put("configfile", getAbsoluteLogPath(InsightBrainService.getConfigFile()));

    entries.put("instanceId", InsightBrainService.getInstanceId());
    entries.put("hostname-ip", InsightBrainService.getLocalHostString());

    final File sonatypeWork = insightConfig.getSonatypeWork();
    entries.put("sonatypeWork", sonatypeWork.getAbsolutePath());
    entries.put("sonatypeWorkContent", sonatypeWork.list());

    final File clusterDirectory = insightConfig.getClusterDirectory();
    entries.put("clusterDirectory", clusterDirectory.getAbsolutePath());
    entries.put("clusterDirectoryContent", clusterDirectory.list());

    final File auditDir = insightWork.getAuditDir();
    entries.put("auditDir", auditDir.getAbsolutePath());
    entries.put("auditDirContent", auditDir.list());

    final File downloads = new File(sonatypeWork, "downloads");
    entries.put("downloadsDirContent", downloads.list());

    entries.put("serverLog", getAbsoluteLogPath(SupportService.getServerLog(insightConfig)));
    entries.put("requestLog", getAbsoluteLogPath(SupportService.getRequestLog(insightConfig)));
    entries.put("auditLog", getAbsoluteLogPath(SupportService.getAuditLog(insightConfig)));
    entries.put("policyViolationLog", getAbsoluteLogPath(SupportService.getPolicyViolationLog(insightConfig)));

    return wrapEntry("install-info", entries);
  }

  private String getAbsoluteLogPath(final File logFile) {
    if (logFile == null) {
      return null;
    }
    return logFile.getAbsolutePath();
  }

  Entry<String, SortedMap<String, Object>> getSystemRuntime() {
    final SortedMap<String, Object> entries = new TreeMap<>();
    final Runtime runtime = Runtime.getRuntime();

    entries.put("availableProcessors", runtime.availableProcessors());
    entries.put("freeMemory", runtime.freeMemory());
    entries.put("totalMemory", runtime.totalMemory());
    entries.put("maxMemory", runtime.maxMemory());
    entries.put("threads", Thread.activeCount());

    return wrapEntry("system-runtime", entries);
  }

  Entry<String, SortedMap<String, Object>> getFileStores() {
    final SortedMap<String, Object> entries = new TreeMap<>();
    final FileSystem fileSystem = FileSystems.getDefault();
    for (final FileStore fileStore : fileSystem.getFileStores()) {
      final TreeMap<String, Object> items = getFileStore(fileStore);
      entries.put(fileStore.name(), items);
    }

    return wrapEntry("system-filestores", entries);
  }

  TreeMap<String, Object> getFileStore(final FileStore fileStore) {
    final TreeMap<String, Object> items = new TreeMap<>();
    items.put("description", fileStore.toString());
    items.put("type", fileStore.type());
    try {
      items.put("totalSpace", fileStore.getTotalSpace());
      items.put("usableSpace", fileStore.getUsableSpace());
      items.put("unallocatedSpace", fileStore.getUnallocatedSpace());
    }
    catch (IOException e) {
      log.warn("Could not read all FileStore information", e);
    }
    items.put("readOnly", fileStore.isReadOnly());

    return items;
  }

  Entry<String, SortedMap<String, Object>> getNetworkInterfaces() {
    final SortedMap<String, Object> entries = new TreeMap<>();

    final Enumeration<NetworkInterface> networkInterfaces;
    try {
      networkInterfaces = NetworkInterface.getNetworkInterfaces();
      while (networkInterfaces.hasMoreElements()) {
        final NetworkInterface networkInterface = networkInterfaces.nextElement();
        final TreeMap<String, Object> items = getNetworkInterface(networkInterface);
        entries.put(networkInterface.getName(), items);
      }
    }
    catch (SocketException e) {
      log.warn("Could not read all NetworkInterface information", e);
    }

    return wrapEntry("system-network", entries);
  }

  static class NetworkInterfaceWrapper
  {
    private final NetworkInterface networkInterface;

    NetworkInterfaceWrapper(final NetworkInterface networkInterface) {
      this.networkInterface = networkInterface;
    }

    String getDisplayName() {
      return networkInterface.getDisplayName();
    }

    boolean isUp() throws SocketException {
      return networkInterface.isUp();
    }

    boolean isVirtual() {
      return networkInterface.isVirtual();
    }

    boolean supportsMulticast() throws SocketException {
      return networkInterface.supportsMulticast();
    }

    boolean isLoopback() throws SocketException {
      return networkInterface.isLoopback();
    }

    boolean isPointToPoint() throws SocketException {
      return networkInterface.isPointToPoint();
    }

    int getMTU() throws SocketException {
      return networkInterface.getMTU();
    }

    Enumeration<InetAddress> getInetAddresses() {
      return networkInterface.getInetAddresses();
    }
  }

  private TreeMap<String, Object> getNetworkInterface(final NetworkInterface networkInterface) {
    return getNetworkInterfaceWithWrapper(new NetworkInterfaceWrapper(networkInterface));
  }

  TreeMap<String, Object> getNetworkInterfaceWithWrapper(final NetworkInterfaceWrapper networkInterface) {
    final TreeMap<String, Object> items = new TreeMap<>();
    items.put("displayName", networkInterface.getDisplayName());
    try {
      items.put("up", networkInterface.isUp());
      items.put("virtual", networkInterface.isVirtual());
      items.put("multicast", networkInterface.supportsMulticast());
      items.put("loopback", networkInterface.isLoopback());
      items.put("ptp", networkInterface.isPointToPoint());
      items.put("mtu", networkInterface.getMTU());
    }
    catch (SocketException e) {
      log.warn("Could not read all NetworkInterface information", e);
    }
    final TreeSet<String> addresses = new TreeSet<>();
    final Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
    while (inetAddresses.hasMoreElements()) {
      addresses.add(inetAddresses.nextElement().toString());
    }
    items.put("addresses", addresses);
    return items;
  }

  List<Entry<String, SortedMap<String, Object>>> getSystemInfo(final String requestUrl) {
    final List<Entry<String, SortedMap<String, Object>>> entries = new ArrayList<>();

    entries.add(getReportTime());
    entries.add(getInstallInfo());
    entries.add(getObfuscatedSystemProperties());
    entries.add(getObfuscatedEnvironment());
    entries.add(getSystemRuntime());
    entries.add(getNetworkInterfaces());
    entries.add(getFileStores());
    entries.add(getClientInfo(requestUrl));

    return entries;
  }

  String getSystemInfoJson(final String requestUrl) {
    final List<Entry<String, SortedMap<String, Object>>> entries = getSystemInfo(requestUrl);
    return JsonUtils.format(entries);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  String getPropertiesJson(final Properties properties, final String parentObjectName) {
    final SortedMap<String, Object> entries = new TreeMap<>();
    entries.putAll((Map) properties);

    final Map<String, SortedMap<String, Object>> mapEntry = new HashMap<>();
    mapEntry.put(parentObjectName, entries);
    return JsonUtils.format(mapEntry);
  }

  String getProductLicense() {
    SupportZipLicenseInfo supportZipLicenseInfo =
        new SupportZipLicenseInfo(clmLicenseManager.getLicenseInfo(),
            productLicense.getFeatures().stream().map(Feature::getId).collect(Collectors.toSet()),
            productLicense.getStageTypes().stream().map(StageType::getId).collect(Collectors.toSet()),
            productLicense.getLicensingModels().stream().map(ProductLicensingModel::name).collect(Collectors.toSet()),
            productLicense.getMaxApplications());
    return JsonUtils.format(supportZipLicenseInfo);
  }

  String getThreadDump() throws IOException {
    try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      new ThreadDump(ManagementFactory.getThreadMXBean()).dump(outputStream);
      return outputStream.toString();
    }
  }

  String getLdapConfig(final List<LdapConfig> ldapServers) {
    return JsonUtils.format(ldapServers);
  }

  String getSamlInfo() {
    SamlInfo samlInfo = new SamlInfo();
    samlInfo.samlConfiguration = samlConfigurationService.get();
    if (samlInfo.samlConfiguration == null) {
      return "null";
    }
    Set<String> excluded = Sets.newHashSet("decryptionKey", "signingKeyPair", "signatureValidationKeyLocator");
    ExclusionStrategy exclusionStrategy = new ExclusionStrategy()
    {
      @Override
      public boolean shouldSkipField(FieldAttributes fieldAttributes) {
        return excluded.stream().anyMatch(e -> e.equalsIgnoreCase(fieldAttributes.getName()));
      }

      @Override
      public boolean shouldSkipClass(Class<?> aClass) {
        return false;
      }
    };
    try {
      String json =
          new GsonBuilder().setExclusionStrategies(exclusionStrategy).create().toJson(samlDeploymentManager.get());
      samlInfo.samlDeployment = new ObjectMapper().readValue(json, new TypeReference<HashMap<String, Object>>()
      {
      });
    }
    catch (Exception e) {
      log.warn("Failed to serialize samlDeployment.", e);
    }
    return JsonUtils.format(samlInfo);
  }

  String getMailConfig() {
    MailConfiguration mailConfiguration = mailConfigurationDAO.get();
    if (mailConfiguration == null) {
      return "null";
    }

    if (mailConfiguration.getPassword() != null) {
      mailConfiguration.setPassword(MASK.toCharArray());
    }
    return JsonUtils.format(mailConfiguration);
  }

  Entry<String, SortedMap<String, Object>> getClientInfo(final String requestUrl) {
    final SortedMap<String, Object> entries = new TreeMap<>();

    entries.put("requestUrl", requestUrl);

    return wrapEntry("client-info", entries);
  }

  static class SamlInfo
  {
    @JsonProperty
    SamlConfiguration samlConfiguration;

    @JsonProperty
    Map<String, Object> samlDeployment;
  }

  String getProxyServerConfiguration() {
    ProxyServerConfiguration proxyServerConfiguration = proxyServerConfigurationDAO.get();
    if (proxyServerConfiguration == null) {
      return "null";
    }

    if (proxyServerConfiguration.getPassword() != null) {
      proxyServerConfiguration.setPassword(MASK.toCharArray());
    }
    return JsonUtils.format(proxyServerConfiguration);
  }
}
