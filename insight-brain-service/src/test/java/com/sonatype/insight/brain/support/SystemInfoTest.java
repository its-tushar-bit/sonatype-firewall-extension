/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.file.FileStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.inject.Inject;

import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.policy.violation.AbstractPolicyViolationLogger;
import com.sonatype.insight.brain.product.license.LicenseInfo;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.support.SystemInfo.NetworkInterfaceWrapper;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.dropwizard.logging.DefaultLoggingFactory;
import io.dropwizard.logging.FileAppenderFactory;
import io.dropwizard.request.logging.LogbackAccessRequestLogFactory;
import io.dropwizard.server.DefaultServerFactory;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import static com.sonatype.insight.brain.support.LimitedFileInputStreamTest.CONFIG_YML;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 1.27
 */
public class SystemInfoTest
    extends AbstractComponentTest
{
  private final String lineSeparator = System.lineSeparator();

  private static final String SERVER_LOG_FILENAME = "myServerLogFilename";

  private static final String REQUEST_LOG_FILENAME = "myRequestLogFilename";

  private static final String AUDIT_LOG_FILENAME = "myAuditLogFilename";

  private static final String POLICY_VIOLATION_LOG_FILENAME = "myPolicyViolationLogFilename";

  @Inject
  private SystemInfo systemInfo;

  @Inject
  @Override
  protected void customizeConfig(final InsightConfig config) {
    DefaultLoggingFactory defaultLoggingFactory = (DefaultLoggingFactory) config.getLoggingFactory();
    FileAppenderFactory<ILoggingEvent> serverFileAppenderFactory = new FileAppenderFactory<>();
    serverFileAppenderFactory.setCurrentLogFilename(SERVER_LOG_FILENAME);
    defaultLoggingFactory.setAppenders(Collections.singletonList(serverFileAppenderFactory));

    DefaultServerFactory defaultServerFactory = (DefaultServerFactory) config.getServerFactory();
    LogbackAccessRequestLogFactory logbackAccessRequestLogFactory =
        (LogbackAccessRequestLogFactory) defaultServerFactory.getRequestLogFactory();
    FileAppenderFactory<IAccessEvent> requestFileAppenderFactory = new FileAppenderFactory<>();
    requestFileAppenderFactory.setCurrentLogFilename(REQUEST_LOG_FILENAME);
    logbackAccessRequestLogFactory.setAppenders(ImmutableList.of(requestFileAppenderFactory));

    defaultLoggingFactory.setLoggers(getLoggers());
  }

  private Map<String, JsonNode> getLoggers() {
    Map<String, JsonNode> loggers = new HashMap<>();
    loggers.put(AuditRecorder.BASE_LOGGER_NAME, getLogger(AUDIT_LOG_FILENAME));
    loggers.put(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME, getLogger(POLICY_VIOLATION_LOG_FILENAME));
    return loggers;
  }

  private JsonNode getLogger(String logFileName) {
    try {
      return new ObjectMapper()
          .readTree("{ \"appenders\": [{\"type\": \"file\", \"currentLogFilename\": \"" + logFileName + "\" }] }");
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Test
  public void testIsSensitiveKey() {
    assertThat(systemInfo.isSensitiveKey("myPasswordLikePropertyName")).isTrue();
    assertThat(systemInfo.isSensitiveKey("myPassPhrasePropertyName")).isTrue();
    assertThat(systemInfo.isSensitiveKey("normalProp")).isFalse();
    assertThat(systemInfo.isSensitiveKey("")).isFalse();
  }

  @Test
  public void testObfuscateValue() {
    assertThat(systemInfo.obfuscateValue("myPasswordLikePropertyName", "yadda")).isEqualTo(SystemInfo.MASK);
    assertThat(systemInfo.obfuscateValue("myPassPhrasePropertyName", "yadda")).isEqualTo(SystemInfo.MASK);
    assertThat(systemInfo.obfuscateValue("normalProp", "normalValue")).isEqualTo("normalValue");
    assertThat(systemInfo.obfuscateValue("", "normalValue")).isEqualTo("normalValue");
  }

  @Test
  public void testGetObfuscatedProperties_MaskPassword() {
    verifyGetObfuscatedProperties_MaskKeyValue("myPasswordLikePropertyName");
    verifyGetObfuscatedProperties_MaskKeyValue("myPassPhrasePropertyName");
  }

  private void verifyGetObfuscatedProperties_MaskKeyValue(final String keyName) {
    System.setProperty(keyName, "yadda");
    try {
      final Entry<String, SortedMap<String, Object>> obfuscatedProps = systemInfo.getObfuscatedSystemProperties();
      assertThat(obfuscatedProps.getKey()).isEqualTo("system-properties");
      assertThat(obfuscatedProps.getValue()).containsEntry(keyName, SystemInfo.MASK);
    }
    finally {
      System.getProperties().remove(keyName);
    }
  }

  @Test
  public void testGetObfuscatedSystemProperties() {
    final Entry<String, SortedMap<String, Object>> entry = systemInfo.getObfuscatedSystemProperties();
    assertThat(entry.getKey()).isEqualTo("system-properties");

    final SortedMap<String, Object> entries = entry.getValue();
    assertThat(entries.get("awt.toolkit")).isNotNull();
    assertThat(entries.get("user.dir")).isNotNull();
    assertThat(entries.get("user.name")).isNotNull();
    assertThat(entries.get("user.timezone")).isNotNull();
  }

  @Test
  public void testGetObfuscatedEnvironment() {
    final Entry<String, SortedMap<String, Object>> entry = systemInfo.getObfuscatedEnvironment();
    assertThat(entry.getKey()).isEqualTo("system-environment");

    final SortedMap<String, Object> entries = entry.getValue();
    assertThat(entries).isNotEmpty().hasSameSizeAs(System.getenv());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetObfuscatedYaml() throws Exception {
    final File configYml = new File(LimitedFileInputStream.class.getResource(CONFIG_YML).getFile());
    assertThat(configYml.exists()).isTrue();

    final String obfuscatedYaml;
    try (final InputStream reader = new FileInputStream(configYml)) {
      obfuscatedYaml = systemInfo.getObfuscatedYaml(reader);
    }
    final Map<String, Object> obufscatedMap = (Map<String, Object>) new Yaml().load(obfuscatedYaml);

    assertThat(obufscatedMap.get("sonatypeWork")).isEqualTo("./sonatype-work/clm-server");

    final Map<String, Object> entryServer = (Map<String, Object>) obufscatedMap.get("server");
    Map<String, Object> entryApplicationConnectors = (Map<String, Object>) ((ArrayList<Object>) entryServer
        .get("applicationConnectors")).get(0);
    final Map<String, Object> entryAdminConnectors = (Map<String, Object>) ((ArrayList<Object>) entryServer
        .get("adminConnectors")).get(0);
    assertThat(entryApplicationConnectors).containsEntry("port", 8070);
    assertThat(entryAdminConnectors).containsEntry("port", 8071);
    entryApplicationConnectors = (Map<String, Object>) ((ArrayList<Object>) entryServer
        .get("applicationConnectors")).get(1);
    assertThat(entryApplicationConnectors).containsEntry("keyStorePassword", SystemInfo.MASK)
        .containsEntry("trustStorePassword", SystemInfo.MASK).containsEntry("keyManagerPassword", SystemInfo.MASK);

    final Map<String, Object> entryHttpRequest = (Map<String, Object>) entryServer.get("requestLog");
    final ArrayList<Object> entryHttpRequestAppenders = (ArrayList<Object>) entryHttpRequest.get("appenders");
    assertThat(entryHttpRequestAppenders).hasSize(1);
    final Map<String, Object> entryFileHttpRequestAppender = (Map<String, Object>) entryHttpRequestAppenders.get(0);

    assertThat(entryFileHttpRequestAppender).containsEntry("type", "file")
        .containsEntry("currentLogFilename", "./log/request.log")
        .containsEntry("archivedLogFilenamePattern", "./log/request-%d.log.gz").containsEntry("archivedFileCount", 50)
        .hasSize(4);

    // validate obfuscation
    assertThat(obufscatedMap).containsEntry("testPassword", SystemInfo.MASK);

    final Map<String, Object> entryPassphraseMap = (Map<String, Object>) obufscatedMap.get("testphrase");
    assertThat(entryPassphraseMap).containsEntry("mypassphrasearray", SystemInfo.MASK);

    final Map<String, Object> entryTestMap = (Map<String, Object>) obufscatedMap.get("testmap");
    assertThat(entryTestMap).containsEntry("json_map-passwords", SystemInfo.MASK);
    assertThat(entryTestMap).containsEntry("json_seq-passwords", SystemInfo.MASK);
  }

  @Test
  public void testGetSystemRuntime() {
    final Entry<String, SortedMap<String, Object>> entry = systemInfo.getSystemRuntime();
    assertThat(entry.getKey()).isEqualTo("system-runtime");

    final SortedMap<String, Object> entries = entry.getValue();
    assertThat(entries.get("availableProcessors")).isNotNull();
    assertThat(entries.get("freeMemory")).isNotNull();
    assertThat(entries.get("maxMemory")).isNotNull();
    assertThat(entries.get("threads")).isNotNull();
    assertThat(entries.get("totalMemory")).isNotNull();
    assertThat(entries).hasSize(5);
  }

  @Test
  public void testGetReportTime() {
    final Entry<String, SortedMap<String, Object>> entry = systemInfo.getReportTime();
    assertThat(entry.getKey()).isEqualTo("system-time");

    final SortedMap<String, Object> entries = entry.getValue();
    assertThat(entries.get("timezone")).isNotNull();
    assertThat(entries.get("current")).isNotNull();
    assertThat(entries.get("iso8601")).isNotNull();
    assertThat(entries).hasSize(3);
  }

  @Test
  public void testGetInstallInfo() {
    final File originalConfigFile = InsightBrainService.getConfigFile();
    final File expectedConfigFile = new File("myConfig.yml");
    final Entry<String, SortedMap<String, Object>> entry;
    try {
      InsightBrainService.setConfigFile(expectedConfigFile);
      entry = systemInfo.getInstallInfo();
    }
    finally {
      InsightBrainService.setConfigFile(originalConfigFile);
    }
    assertThat(entry.getKey()).isEqualTo("install-info");

    final SortedMap<String, Object> entries = entry.getValue();

    assertThat(entries.get("application-jar").toString())
        .endsWith(InsightBrainService.class.getSimpleName() + ".class");

    assertThat(entries.get("configfile").toString()).isEqualTo(expectedConfigFile.getAbsolutePath());

    assertThat(entries.get("instanceId").toString()).isEqualTo(InsightBrainService.getInstanceId());
    assertThat(entries.get("hostname-ip").toString()).isEqualTo(InsightBrainService.getLocalHostString());

    final File workDir = new File(entries.get("sonatypeWork").toString());
    assertThat(workDir).isDirectory();
    assertThat(workDir).isAbsolute();
    assertThat(entries.get("sonatypeWorkContent")).isNotNull();

    final File auditDir = new File(entries.get("auditDir").toString());
    assertThat(auditDir).isAbsolute();
    assertThat(entries.get("auditDirContent")).isNull();

    assertThat(entries.get("downloadsDirContent")).isNull();

    final String serverLog = (String) entries.get("serverLog");
    assertThat(serverLog).endsWith(SERVER_LOG_FILENAME);
    final File serverFile = new File(serverLog);
    assertThat(serverFile).isAbsolute();

    final String requestValue = (String) entries.get("requestLog");
    assertThat(requestValue).endsWith(REQUEST_LOG_FILENAME);
    final File requestFile = new File(requestValue);
    assertThat(requestFile).isAbsolute();

    final String auditLog = (String) entries.get("auditLog");
    assertThat(auditLog).endsWith(AUDIT_LOG_FILENAME);
    final File auditFile = new File(auditLog);
    assertThat(auditFile).isAbsolute();

    final String policyViolationLog = (String) entries.get("policyViolationLog");
    assertThat(policyViolationLog).endsWith(POLICY_VIOLATION_LOG_FILENAME);
    final File policyViolationFile = new File(policyViolationLog);
    assertThat(policyViolationFile).isAbsolute();

    assertThat(entries).hasSize(13);
  }

  @Test
  public void testGetFileStores() {
    final Entry<String, SortedMap<String, Object>> entry = systemInfo.getFileStores();
    assertThat(entry.getKey()).isEqualTo("system-filestores");

    final SortedMap<String, Object> entries = entry.getValue();
    assertThat(entries).isNotEmpty();

    @SuppressWarnings("unchecked") final Map<String, Object> fileStoresEntry = (Map<String, Object>) entries
        .get(entries.firstKey());
    assertThat(fileStoresEntry.get("description")).isNotNull();
    assertThat(fileStoresEntry.get("type")).isNotNull();
    assertThat(fileStoresEntry.get("totalSpace")).isNotNull();
    assertThat(fileStoresEntry.get("usableSpace")).isNotNull();
    assertThat(fileStoresEntry.get("unallocatedSpace")).isNotNull();
    assertThat(fileStoresEntry.get("readOnly")).isNotNull();
  }

  @Test
  public void testGetFileStoreWithException() throws Exception {
    final FileStore fileStore = mock(FileStore.class);
    when(fileStore.type()).thenReturn("myType");
    when(fileStore.getUnallocatedSpace()).thenThrow(new IOException("testException"));
    final TreeMap<String, Object> map = systemInfo.getFileStore(fileStore);
    assertThat(map).hasSize(5);
    assertThat(map.get("description")).isNotNull();
    assertThat(map.get("type")).isEqualTo("myType");
    assertThat(map.get("totalSpace")).isNotNull();
    assertThat(map.get("usableSpace")).isNotNull();
    assertThat(map.get("unallocatedSpace")).isNull();
    assertThat(map.get("readOnly")).isNotNull();

    when(fileStore.getTotalSpace()).thenThrow(new IOException("testException2"));
    final TreeMap<String, Object> map2 = systemInfo.getFileStore(fileStore);
    assertThat(map2).hasSize(3);
    assertThat(map2.get("description")).isNotNull();
    assertThat(map2.get("type")).isEqualTo("myType");
    assertThat(map2.get("totalSpace")).isNull();
    assertThat(map2.get("usableSpace")).isNull();
    assertThat(map2.get("unallocatedSpace")).isNull();
    assertThat(map2.get("readOnly")).isNotNull();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetNetworkInterfaces() {
    final Entry<String, SortedMap<String, Object>> entry = systemInfo.getNetworkInterfaces();
    assertThat(entry.getKey()).isEqualTo("system-network");

    final SortedMap<String, Object> entries = entry.getValue();
    assertThat(entries).isNotEmpty();

    final Map<String, Object> networkInterfacesEntry = (Map<String, Object>) entries.get(entries.firstKey());
    assertThat(networkInterfacesEntry.get("displayName")).isNotNull();
    assertThat(networkInterfacesEntry.get("up")).isNotNull();
    assertThat(networkInterfacesEntry.get("virtual")).isNotNull();
    assertThat(networkInterfacesEntry.get("multicast")).isNotNull();
    assertThat(networkInterfacesEntry.get("loopback")).isNotNull();
    assertThat(networkInterfacesEntry.get("ptp")).isNotNull();
    assertThat(networkInterfacesEntry.get("mtu")).isNotNull();
    assertThat(networkInterfacesEntry.get("addresses")).isNotNull();
    assertThat(networkInterfacesEntry).hasSize(8);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetNetworkInterfaceWithWrapperWithException() throws Exception {
    final NetworkInterfaceWrapper networkInterface = mock(NetworkInterfaceWrapper.class);
    when(networkInterface.getDisplayName()).thenReturn("myDisplayName");
    when(networkInterface.getMTU()).thenThrow(new SocketException("testException"));

    final class InetAddressEnumeration
        implements Enumeration<InetAddress>
    {
      @Override
      public boolean hasMoreElements() {
        return false;
      }

      @Override
      public InetAddress nextElement() {
        return null;
      }
    }

    when(networkInterface.getInetAddresses()).thenReturn(new InetAddressEnumeration());

    final TreeMap<String, Object> map = systemInfo.getNetworkInterfaceWithWrapper(networkInterface);
    assertThat(map.get("displayName")).isNotNull();
    assertThat(map.get("up")).isNotNull();
    assertThat(map.get("virtual")).isNotNull();
    assertThat(map.get("multicast")).isNotNull();
    assertThat(map.get("loopback")).isNotNull();
    assertThat(map.get("ptp")).isNotNull();
    assertThat(map.get("mtu")).isNull();
    assertThat((TreeSet<String>) map.get("addresses")).isEmpty();
    assertThat(map).hasSize(7);

    when(networkInterface.isUp()).thenThrow(new SocketException("testException2"));
    final TreeMap<String, Object> map2 = systemInfo.getNetworkInterfaceWithWrapper(networkInterface);
    assertThat(map2.get("displayName")).isNotNull();
    assertThat(map2.get("up")).isNull();
    assertThat(map2.get("virtual")).isNull();
    assertThat(map2.get("multicast")).isNull();
    assertThat(map2.get("loopback")).isNull();
    assertThat(map2.get("ptp")).isNull();
    assertThat(map2.get("mtu")).isNull();
    assertThat((TreeSet<String>) map2.get("addresses")).isEmpty();
    assertThat(map2).hasSize(2);
  }

  @Test
  public void testGetSystemInfo() {
    final List<Entry<String, SortedMap<String, Object>>> list = systemInfo.getSystemInfo(null);
    assertThat(list).extracting(Entry::getKey).containsExactly("system-time", "install-info", "system-properties",
        "system-environment", "system-runtime", "system-network", "system-filestores", "client-info");
  }

  @Test
  public void testGetPropertiesJson() {
    final Properties properties = new Properties();
    properties.put("c", "3");
    properties.put("a", "1");
    properties.put("b", "2");

    final String json = systemInfo.getPropertiesJson(properties, "parentName");
    assertThat(json).isEqualTo("{" + lineSeparator +
        "  \"parentName\" : {" + lineSeparator +
        "    \"a\" : \"1\"," + lineSeparator +
        "    \"b\" : \"2\"," + lineSeparator +
        "    \"c\" : \"3\"" + lineSeparator +
        "  }" + lineSeparator +
        "}");
  }

  @Test
  public void testGetProduceLicense() {
    final LicenseInfo licenseInfo = new LicenseInfo("fprint", -1, -2, -3, -4, "Contact Name",
        "Contact Company", "contact@example.com", new String[]{"Pro+"}, "edition");

    final String json = systemInfo.getProductLicense(licenseInfo);
    assertThat(json).isEqualTo("{" + lineSeparator +
        "  \"productEdition\" : \"edition\"," + lineSeparator +
        "  \"fingerprint\" : \"fprint\"," + lineSeparator +
        "  \"expiryTimestamp\" : -1," + lineSeparator +
        "  \"licensedUsersToDisplay\" : -2," + lineSeparator +
        "  \"applicationLimitToDisplay\" : -4," + lineSeparator +
        "  \"firewallUsersToDisplay\" : -3," + lineSeparator +
        "  \"contactName\" : \"Contact Name\"," + lineSeparator +
        "  \"contactCompany\" : \"Contact Company\"," + lineSeparator +
        "  \"contactEmail\" : \"contact@example.com\"," + lineSeparator +
        "  \"products\" : [ \"Pro+\" ]" + lineSeparator +
        "}");
  }

  @Test
  public void testGetThreadDump() throws Exception {
    final String text = systemInfo.getThreadDump();
    assertThat(text).contains("ThreadDump");
  }

  @Test
  public void testGetLdapConfigEmpty() {
    final List<LdapConfig> ldapServers = new ArrayList<>();
    assertThat(systemInfo.getLdapConfig(ldapServers)).isEqualTo("[ ]");
  }

  @Test
  public void testGetClientInfo() {
    final String requestUrl = "myRequestUrl";
    final Entry<String, SortedMap<String, Object>> entry = systemInfo.getClientInfo(requestUrl);
    final SortedMap<String, Object> entries = entry.getValue();
    assertThat(entries.get("requestUrl").toString()).isEqualTo(requestUrl);
  }
}
