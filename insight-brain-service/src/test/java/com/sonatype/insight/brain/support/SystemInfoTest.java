/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

import ch.qos.logback.access.common.spi.IAccessEvent;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyServerConfigurationDAO;
import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.policy.violation.AbstractPolicyViolationLogger;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.security.SamlDeploymentManager;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.HdsMockServerRule;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.support.SystemInfo.NetworkInterfaceWrapper;
import com.sonatype.insight.brain.support.SystemInfo.SamlInfo;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.test.productlicense.ProductLicenseConfig;
import org.sonatype.licensing.LicensingException;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
import io.dropwizard.logging.common.DefaultLoggingFactory;
import io.dropwizard.logging.common.FileAppenderFactory;
import io.dropwizard.request.logging.LogbackAccessRequestLogFactory;
import io.dropwizard.core.server.DefaultServerFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mock;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 1.27
 */
public class SystemInfoTest
    extends AbstractComponentTest
{
  @ClassRule
  public static HdsMockServerRule hdsMockServer = new HdsMockServerRule();

  @Inject
  private CLMLicenseManager clmLicenseManager;

  @Inject
  private SamlConfigurationService samlConfigurationService;

  @Inject
  private MailConfigurationDAO mailConfigurationDAO;

  @Inject
  private ProxyServerConfigurationDAO proxyServerConfigurationDAO;

  @Inject
  private SystemInfo systemInfo;

  @Inject
  private SamlDeploymentManager samlDeploymentManager;

  @Inject
  private PasswordHandler passwordHandler;

  @Inject
  private TestProductLicenseManager licenseManager;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Mock
  private VersionService versionService;

  private final String lineSeparator = System.lineSeparator();

  private static final String SERVER_LOG_FILENAME = "myServerLogFilename";

  private static final String REQUEST_LOG_FILENAME = "myRequestLogFilename";

  private static final String AUDIT_LOG_FILENAME = "myAuditLogFilename";

  private static final String POLICY_VIOLATION_LOG_FILENAME = "myPolicyViolationLogFilename";

  @Before
  public void before() throws Exception {
    try (InputStream in = getClass().getResourceAsStream("/productlicense/licensing-keystore-hds.p12")) {
      assert in != null;
      Files.copy(in, new File(tempDir.getRoot(), "hds.p12").toPath());
    }

    hdsMockServer.reset();
    setHdsUrl(hdsMockServer.getHttpUrl());
  }

  @Override
  public void configure(Binder binder) {
    ProductLicenseConfig productLicenseConfig = new ProductLicenseConfig();
    productLicenseConfig.setKeyStorePath(new File(tempDir.getRoot(), "hds.p12").getAbsolutePath());
    productLicenseConfig.setKeyStoreAliasGroup("licensing-key-test");
    binder.bind(ProductLicenseConfig.class).toInstance(productLicenseConfig);
    binder.bind(TaskScheduler.class).toInstance(taskSchedulerMock);
    binder.bind(VersionService.class).toInstance(versionService);
    super.configure(binder);
  }

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
    logbackAccessRequestLogFactory.setAppenders(Collections.singletonList(requestFileAppenderFactory));

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
    assertThat(SystemInfo.isSensitiveKey("myPasswordLikePropertyName")).isTrue();
    assertThat(SystemInfo.isSensitiveKey("myPassPhrasePropertyName")).isTrue();
    assertThat(SystemInfo.isSensitiveKey("normalProp")).isFalse();
    assertThat(SystemInfo.isSensitiveKey("")).isFalse();
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
    assertThat(entries.get("user.dir")).isNotNull();
    assertThat(entries.get("user.name")).isNotNull();
    assertThat(entries.get("user.timezone")).isNotNull();
  }

  @Test
  public void testGetObfuscatedSystemProperties_WithValidFilter() {
    final Entry<String, SortedMap<String, Object>> entry =
        systemInfo.getObfuscatedSystemProperties("java", "java-info");
    assertThat(entry.getKey()).isEqualTo("java-info");

    final SortedMap<String, Object> entries = entry.getValue();
    assertThat(entries.get("java.runtime.version")).isNotNull();
    assertThat(entries.get("java.vendor")).isNotNull();
    assertThat(entries.get("java.vm.name")).isNotNull();
    assertThat(entries.get("java.vm.specification.version")).isNotNull();
  }

  @Test
  public void testGetObfuscatedSystemProperties_WithInvalidFilter() {
    final Entry<String, SortedMap<String, Object>> entry =
        systemInfo.getObfuscatedSystemProperties("invalid", "invalid-info");
    assertThat(entry.getKey()).isEqualTo("invalid-info");

    final SortedMap<String, Object> entries = entry.getValue();
    assertThat(entries).isEmpty();
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
    final File configYml =
        new File(getClass().getResource("/" + getClass().getSimpleName() + "/config-support-test.yml").getFile());
    assertThat(configYml.exists()).isTrue();

    final String obfuscatedYaml;
    try (final InputStream reader = Files.newInputStream(configYml.toPath())) {
      obfuscatedYaml = systemInfo.getObfuscatedYaml(reader);
    }
    final Map<String, Object> obufscatedMap = new Yaml(new SafeConstructor(new LoaderOptions())).load(obfuscatedYaml);

    assertThat(obufscatedMap.get("sonatypeWork")).isEqualTo("./sonatype-work/clm-server");
    assertThat(obufscatedMap.get("clusterDirectory")).isEqualTo("./sonatype-work/cluster-directory");

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
        .containsEntry("trustStorePassword", SystemInfo.MASK)
        .containsEntry("keyManagerPassword", SystemInfo.MASK);

    final Map<String, Object> entryHttpRequest = (Map<String, Object>) entryServer.get("requestLog");
    final ArrayList<Object> entryHttpRequestAppenders = (ArrayList<Object>) entryHttpRequest.get("appenders");
    assertThat(entryHttpRequestAppenders).hasSize(1);
    final Map<String, Object> entryFileHttpRequestAppender = (Map<String, Object>) entryHttpRequestAppenders.get(0);

    assertThat(entryFileHttpRequestAppender).containsEntry("type", "file")
        .containsEntry("currentLogFilename", "./log/request.log")
        .containsEntry("archivedLogFilenamePattern", "./log/request-%d.log.gz")
        .containsEntry("archivedFileCount", 50)
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
  public void testGetObfuscatedYaml_PreservesBlockFlowFormatting() throws Exception {
    File configYml = new File(getClass()
        .getResource(
            "/" + getClass().getSimpleName() + "/testGetObfuscatedYaml_PreservesBlockFlowFormatting-config.yml")
        .getFile());
    assertThat(configYml.exists()).isTrue();
    String configYmlContent = FileUtils.readFileToString(configYml, StandardCharsets.UTF_8).replaceAll("\r\n", "\n");

    String obfuscatedYaml;
    try (final InputStream reader = Files.newInputStream(configYml.toPath())) {
      obfuscatedYaml = systemInfo.getObfuscatedYaml(reader).replaceAll("\r\n", "\n");
    }
    assertThat(obfuscatedYaml).isEqualTo(configYmlContent);
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

    final File clusterDirectory = new File(entries.get("clusterDirectory").toString());
    assertThat(clusterDirectory).isDirectory();
    assertThat(clusterDirectory).isAbsolute();
    assertThat(entries.get("clusterDirectoryContent")).isNotNull();

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

    assertThat(entries).hasSize(15);
  }

  @Test
  public void testGetFileStores() {
    final Entry<String, SortedMap<String, Object>> entry = systemInfo.getFileStores();
    assertThat(entry.getKey()).isEqualTo("system-filestores");

    final SortedMap<String, Object> entries = entry.getValue();
    assertThat(entries).isNotEmpty();

    @SuppressWarnings("unchecked")
    final Map<String, Object> fileStoresEntry = (Map<String, Object>) entries
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
    assertThat(list).extracting(Entry::getKey)
        .containsExactly("system-time", "install-info", "system-properties",
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
  public void testGetProductLicense() throws IOException {
    doReturn("1.180.0")
        .when(versionService)
        .getVersion();
    doReturn(0)
        .when(versionService)
        .compare(anyString(), anyString());

    final String json = systemInfo.getProductLicense();

    ObjectMapper objectMapper = new ObjectMapper();

    SupportZipLicenseInfo supportZipLicenseInfo = objectMapper.readValue(json, SupportZipLicenseInfo.class);

    assertThat(supportZipLicenseInfo.licenseInfo.productEdition).isEqualTo("Lifecycle");
    assertThat(supportZipLicenseInfo.licenseInfo.fingerprint).isEqualTo("1234");
    assertThat(supportZipLicenseInfo.licenseInfo.licensedUsersToDisplay).isEqualTo(50);
    assertThat(supportZipLicenseInfo.licenseInfo.applicationLimitToDisplay).isNull();
    assertThat(supportZipLicenseInfo.licenseInfo.sbomLimitToDisplay).isNull();
    assertThat(supportZipLicenseInfo.licenseInfo.firewallUsersToDisplay).isEqualTo(45);
    assertThat(supportZipLicenseInfo.licenseInfo.contactName).isEqualTo("Billy");
    assertThat(supportZipLicenseInfo.licenseInfo.contactCompany).isEqualTo("Acme");
    assertThat(supportZipLicenseInfo.licenseInfo.contactEmail).isEqualTo("billy@example.com");
    assertThat(supportZipLicenseInfo.licenseInfo.products).containsExactlyInAnyOrder("Sonatype Lifecycle",
        "Sonatype Repository Firewall", "Sonatype Firewall for Artifactory", "Sonatype Lifecycle Cloud",
        "Sonatype Lifecycle Firewall Cloud", "Sonatype Lifecycle SaaS", "Sonatype Lifecycle Firewall SaaS",
        "Sonatype Lifecycle Foundation SaaS", "Sonatype Auditor SaaS", "Sonatype Developer");
    assertThat(supportZipLicenseInfo.licenseInfo.expiryTimestamp).isPositive();

    Collection<String> features = supportZipLicenseInfo.features;
    assertThat(features).hasSizeGreaterThan(15).contains(LicensedFeature.CI_INTEGRATION.getId());
    assertThat(supportZipLicenseInfo.applicationCountLimit).isEqualTo(100);
    assertThat(supportZipLicenseInfo.stageIds).containsExactlyInAnyOrder("proxy", "operate", "build", "release",
        "develop", "source", "stage-release", "compliance");
    assertThat(supportZipLicenseInfo.licensingModels).containsExactlyInAnyOrder("LEGACY");
  }

  @Test
  public void testGetProductLicense_multipleLicenseModels() throws IOException {
    doReturn("1.180.0")
        .when(versionService)
        .getVersion();
    doReturn(0)
        .when(versionService)
        .compare(anyString(), anyString());

    List<String> licensingModels = Arrays.asList(
        ProductLicenseDetails.LICENSING_SBOM_BASED,
        ProductLicenseDetails.LICENSING_APP_BASED,
        ProductLicenseDetails.LICENSING_USER_BASED);

    String licensingModelsString = String.join(",", licensingModels);
    licenseManager.setProperty(ProductLicenseDetails.PROPERTY_LICENSING_MODEL, licensingModelsString);
    licenseManager.setApplicationLimit(100);
    licenseManager.setMaxUsers(8765);
    licenseManager.setMaxFirewallUsers(4321);
    licenseManager.setMaxSboms(50);

    installLicense();

    final String json = systemInfo.getProductLicense();
    ObjectMapper objectMapper = new ObjectMapper();
    SupportZipLicenseInfo supportZipLicenseInfo = objectMapper.readValue(json, SupportZipLicenseInfo.class);

    assertThat(supportZipLicenseInfo.licenseInfo.productEdition).isEqualTo("Lifecycle");
    assertThat(supportZipLicenseInfo.licenseInfo.fingerprint).isEqualTo("1234");
    assertThat(supportZipLicenseInfo.licenseInfo.licensedUsersToDisplay).isEqualTo(8765);
    assertThat(supportZipLicenseInfo.licenseInfo.applicationLimitToDisplay).isEqualTo(100);
    assertThat(supportZipLicenseInfo.licenseInfo.sbomLimitToDisplay).isEqualTo(50);
    assertThat(supportZipLicenseInfo.licenseInfo.firewallUsersToDisplay).isEqualTo(4321);
    assertThat(supportZipLicenseInfo.licenseInfo.contactName).isEqualTo("Billy");
    assertThat(supportZipLicenseInfo.licenseInfo.contactCompany).isEqualTo("Acme");
    assertThat(supportZipLicenseInfo.licenseInfo.contactEmail).isEqualTo("billy@example.com");
    assertThat(supportZipLicenseInfo.licenseInfo.products).containsExactlyInAnyOrder("Sonatype Lifecycle",
        "Sonatype Repository Firewall", "Sonatype Firewall for Artifactory", "Sonatype Lifecycle Cloud",
        "Sonatype Lifecycle Firewall Cloud", "Sonatype Lifecycle SaaS", "Sonatype Lifecycle Firewall SaaS",
        "Sonatype Lifecycle Foundation SaaS", "Sonatype Auditor SaaS", "Sonatype Developer");
    assertThat(supportZipLicenseInfo.licenseInfo.expiryTimestamp).isPositive();

    Collection<String> features = supportZipLicenseInfo.features;
    assertThat(features).hasSizeGreaterThan(15).contains(LicensedFeature.CI_INTEGRATION.getId());
    assertThat(supportZipLicenseInfo.applicationCountLimit).isEqualTo(100);
    assertThat(supportZipLicenseInfo.stageIds).containsExactlyInAnyOrder("proxy", "operate", "build", "release",
        "develop", "source", "stage-release");

    assertThat(supportZipLicenseInfo.licensingModels).containsExactlyInAnyOrder("USER_BASED", "APP_BASED",
        "SBOM_BASED");
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
  public void testGetSamlInfo_NotConfigured() {
    assertThat(systemInfo.getSamlInfo()).isEqualTo("null");
  }

  @Test
  public void testGetSamlInfo_Configured() throws Exception {
    SamlConfiguration samlConfig = tempEntity.newSamlConfiguration(null, null);
    samlConfigurationService.insert(samlConfig);
    samlConfig.setIdentityProviderMetadataXml(IOUtils.toString(
        getClass().getResourceAsStream("/" + getClass().getSimpleName() + "/saml-identity-provider-metadata.xml"),
        StandardCharsets.UTF_8));
    samlConfigurationService.update(samlConfig);
    samlDeploymentManager.updateFromConfiguration();

    SamlInfo samlInfo = new ObjectMapper().readValue(systemInfo.getSamlInfo(), SamlInfo.class);

    assertThat(samlInfo).isNotNull();

    assertThat(samlInfo.samlConfiguration.getId()).isEqualTo(samlConfig.getId());
    assertThat(samlInfo.samlConfiguration.getIdentityProviderMetadataXml())
        .isEqualTo(samlConfig.getIdentityProviderMetadataXml());
    assertThat(samlInfo.samlConfiguration.getFirstNameAttributeName())
        .isEqualTo(samlConfig.getFirstNameAttributeName());
    assertThat(samlInfo.samlConfiguration.getLastNameAttributeName()).isEqualTo(samlConfig.getLastNameAttributeName());
    assertThat(samlInfo.samlConfiguration.getEmailAttributeName()).isEqualTo(samlConfig.getEmailAttributeName());
    assertThat(samlInfo.samlConfiguration.getUsernameAttributeName()).isEqualTo(samlConfig.getUsernameAttributeName());
    assertThat(samlInfo.samlConfiguration.getGroupsAttributeName()).isEqualTo(samlConfig.getGroupsAttributeName());
    assertThat(samlInfo.samlConfiguration.getCertificate()).isNull();
    assertThat(samlInfo.samlConfiguration.getDecryptionKey()).isNull();
    assertThat(samlInfo.samlConfiguration.getSigningKeyPair()).isNull();

    assertThat(samlInfo.samlDeployment.get("autodetectBearerOnly")).isEqualTo(true);
    assertThat(samlInfo.samlDeployment.get("configured")).isEqualTo(false);
    assertThat(samlInfo.samlDeployment.get("forceAuthentication")).isEqualTo(false);
    assertThat(samlInfo.samlDeployment.get("isPassive")).isEqualTo(false);
    assertThat(samlInfo.samlDeployment.get("nameIDPolicyFormat"))
        .isEqualTo("urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified");
    assertThat(samlInfo.samlDeployment.get("principalNamePolicy")).isEqualTo("FROM_NAME_ID");
    assertThat(samlInfo.samlDeployment.get("signatureAlgorithm")).isEqualTo("RSA_SHA256");
    assertThat(samlInfo.samlDeployment.get("signatureCanonicalizationMethod"))
        .isEqualTo("http://www.w3.org/2001/10/xml-exc-c14n#");
    assertThat(samlInfo.samlDeployment.get("sslRequired")).isEqualTo("EXTERNAL");
    assertThat(samlInfo.samlDeployment.get("turnOffChangeSessionIdOnLogin")).isEqualTo(false);

    assertThat(samlInfo.samlDeployment.get("idp")).isNotNull();
    @SuppressWarnings("unchecked")
    Map<String, Object> idpProps = (Map<String, Object>) samlInfo.samlDeployment.get("idp");
    assertThat(idpProps.get("entityID")).isEqualTo("idp-entity-id");
    assertThat(idpProps.get("minTimeBetweenDescriptorRequests")).isEqualTo(0);
    assertThat(idpProps.get("singleSignOnService")).isNotNull();
    assertThat(idpProps.get("singleLogoutService")).isNotNull();

    @SuppressWarnings("unchecked")
    Map<String, Object> singleSignOnServiceProps = (Map<String, Object>) idpProps.get("singleSignOnService");
    assertThat(singleSignOnServiceProps.get("requestBinding")).isEqualTo("POST");
    assertThat(singleSignOnServiceProps.get("requestBindingUrl")).isEqualTo("http://localhost:8080/sso");
    assertThat(singleSignOnServiceProps.get("signRequest")).isEqualTo(true);
    assertThat(singleSignOnServiceProps.get("validateAssertionSignature")).isEqualTo(true);
    assertThat(singleSignOnServiceProps.get("validateResponseSignature")).isEqualTo(true);

    @SuppressWarnings("unchecked")
    Map<String, Object> singleLogoutServiceProps = (Map<String, Object>) idpProps.get("singleLogoutService");
    assertThat(singleLogoutServiceProps.get("requestBinding")).isEqualTo("REDIRECT");
    assertThat(singleLogoutServiceProps.get("requestBindingUrl")).isEqualTo("http://localhost:8080/slo");
    assertThat(singleLogoutServiceProps.get("responseBinding")).isEqualTo("REDIRECT");
    assertThat(singleLogoutServiceProps.get("responseBindingUrl")).isEqualTo("http://localhost:8080/slo");
    assertThat(singleLogoutServiceProps.get("signRequest")).isEqualTo(true);
    assertThat(singleLogoutServiceProps.get("signResponse")).isEqualTo(true);
    assertThat(singleLogoutServiceProps.get("validateRequestSignature")).isEqualTo(true);
    assertThat(singleLogoutServiceProps.get("validateResponseSignature")).isEqualTo(true);
  }

  @Test
  public void testGetMailConfig_NotConfigured() {
    assertThat(systemInfo.getMailConfig()).isEqualTo("null");
  }

  @Test
  public void testGetMailConfig_Configured() throws Exception {
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("testHostname");
    mailConfiguration.setPort(4567);
    mailConfiguration.setUsername("testUsername");
    mailConfiguration.setPassword("testPassword".toCharArray());
    mailConfiguration.setSslEnabled(true);
    mailConfiguration.setStartTlsEnabled(true);
    mailConfiguration.setSystemEmail("test@example.com");
    mailConfigurationDAO.set(mailConfiguration);

    mailConfiguration = new ObjectMapper().readValue(systemInfo.getMailConfig(), MailConfiguration.class);

    assertThat(mailConfiguration).isNotNull();
    assertThat(mailConfiguration.getId()).isEqualTo(MailConfigurationDAO.SINGLETON_ENTITY_ID);
    assertThat(mailConfiguration.getHostname()).isEqualTo("testHostname");
    assertThat(mailConfiguration.getPort()).isEqualTo(4567);
    assertThat(mailConfiguration.getUsername()).isEqualTo("testUsername");
    assertThat(mailConfiguration.getPassword()).isEqualTo(SystemInfo.MASK.toCharArray());
    assertThat(mailConfiguration.isSslEnabled()).isTrue();
    assertThat(mailConfiguration.isStartTlsEnabled()).isTrue();
    assertThat(mailConfiguration.getSystemEmail()).isEqualTo("test@example.com");
  }

  @Test
  public void testGetMailConfig_Configured_NoPassword() throws Exception {
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("testHostname");
    mailConfiguration.setPort(4567);
    mailConfiguration.setUsername("testUsername");
    mailConfiguration.setPassword(null);
    mailConfiguration.setSslEnabled(true);
    mailConfiguration.setStartTlsEnabled(true);
    mailConfiguration.setSystemEmail("test@example.com");
    mailConfigurationDAO.set(mailConfiguration);

    mailConfiguration = new ObjectMapper().readValue(systemInfo.getMailConfig(), MailConfiguration.class);

    assertThat(mailConfiguration).isNotNull();
    assertThat(mailConfiguration.getId()).isEqualTo(MailConfigurationDAO.SINGLETON_ENTITY_ID);
    assertThat(mailConfiguration.getHostname()).isEqualTo("testHostname");
    assertThat(mailConfiguration.getPort()).isEqualTo(4567);
    assertThat(mailConfiguration.getUsername()).isEqualTo("testUsername");
    assertThat(mailConfiguration.getPassword()).isNull();
    assertThat(mailConfiguration.isSslEnabled()).isTrue();
    assertThat(mailConfiguration.isStartTlsEnabled()).isTrue();
    assertThat(mailConfiguration.getSystemEmail()).isEqualTo("test@example.com");
  }

  @Test
  public void testGetClientInfo() {
    final String requestUrl = "myRequestUrl";
    final Entry<String, SortedMap<String, Object>> entry = systemInfo.getClientInfo(requestUrl);
    final SortedMap<String, Object> entries = entry.getValue();
    assertThat(entries.get("requestUrl").toString()).isEqualTo(requestUrl);
  }

  @Test
  public void testGetProxyServerConfiguration_NotConfigured() {
    assertThat(systemInfo.getProxyServerConfiguration()).isEqualTo("null");
  }

  @Test
  public void testGetProxyServerConfiguration_Configured() throws Exception {
    ProxyServerConfiguration proxyServerConfiguration = new ProxyServerConfiguration();
    proxyServerConfiguration.setHostname("testHostname");
    proxyServerConfiguration.setPort(4567);
    proxyServerConfiguration.setUsername("testUsername");
    proxyServerConfiguration.setPassword(passwordHandler.encryptPassword("testPassword".toCharArray()));
    proxyServerConfiguration.setExcludeHosts("host1,host2");
    proxyServerConfigurationDAO.set(proxyServerConfiguration);

    proxyServerConfiguration =
        new ObjectMapper().readValue(systemInfo.getProxyServerConfiguration(), ProxyServerConfiguration.class);

    assertThat(proxyServerConfiguration).isNotNull();
    assertThat(proxyServerConfiguration.getId()).isEqualTo(ProxyServerConfigurationDAO.SINGLETON_ENTITY_ID);
    assertThat(proxyServerConfiguration.getHostname()).isEqualTo("testHostname");
    assertThat(proxyServerConfiguration.getPort()).isEqualTo(4567);
    assertThat(proxyServerConfiguration.getUsername()).isEqualTo("testUsername");
    assertThat(proxyServerConfiguration.getPassword()).isEqualTo(SystemInfo.MASK.toCharArray());
    assertThat(proxyServerConfiguration.getExcludeHosts()).isEqualTo("host1,host2");
  }

  @Test
  public void testGetProxyServerConfiguration_Configured_NoPassword() throws Exception {
    ProxyServerConfiguration proxyServerConfiguration = new ProxyServerConfiguration();
    proxyServerConfiguration.setHostname("testHostname");
    proxyServerConfiguration.setPort(4567);
    proxyServerConfiguration.setUsername("testUsername");
    proxyServerConfiguration.setPassword(null);
    proxyServerConfiguration.setExcludeHosts("host1,host2");
    proxyServerConfigurationDAO.set(proxyServerConfiguration);

    proxyServerConfiguration =
        new ObjectMapper().readValue(systemInfo.getProxyServerConfiguration(), ProxyServerConfiguration.class);

    assertThat(proxyServerConfiguration).isNotNull();
    assertThat(proxyServerConfiguration.getId()).isEqualTo(ProxyServerConfigurationDAO.SINGLETON_ENTITY_ID);
    assertThat(proxyServerConfiguration.getHostname()).isEqualTo("testHostname");
    assertThat(proxyServerConfiguration.getPort()).isEqualTo(4567);
    assertThat(proxyServerConfiguration.getUsername()).isEqualTo("testUsername");
    assertThat(proxyServerConfiguration.getPassword()).isNull();
    assertThat(proxyServerConfiguration.getExcludeHosts()).isEqualTo("host1,host2");
  }

  @Test
  public void testGetProxyServerConfiguration_Configured_NoExcludeHosts() throws Exception {
    ProxyServerConfiguration proxyServerConfiguration = new ProxyServerConfiguration();
    proxyServerConfiguration.setHostname("testHostname");
    proxyServerConfiguration.setPort(4567);
    proxyServerConfiguration.setUsername("testUsername");
    proxyServerConfiguration.setPassword(passwordHandler.encryptPassword("testPassword".toCharArray()));
    proxyServerConfiguration.setExcludeHosts(null);
    proxyServerConfigurationDAO.set(proxyServerConfiguration);

    proxyServerConfiguration =
        new ObjectMapper().readValue(systemInfo.getProxyServerConfiguration(), ProxyServerConfiguration.class);

    assertThat(proxyServerConfiguration).isNotNull();
    assertThat(proxyServerConfiguration.getId()).isEqualTo(ProxyServerConfigurationDAO.SINGLETON_ENTITY_ID);
    assertThat(proxyServerConfiguration.getHostname()).isEqualTo("testHostname");
    assertThat(proxyServerConfiguration.getPort()).isEqualTo(4567);
    assertThat(proxyServerConfiguration.getUsername()).isEqualTo("testUsername");
    assertThat(proxyServerConfiguration.getPassword()).isEqualTo(SystemInfo.MASK.toCharArray());
    assertThat(proxyServerConfiguration.getExcludeHosts()).isNull();
  }

  private void installLicense() throws IOException, LicensingException {
    clmLicenseManager.installLicense(new ByteArrayInputStream(new byte[1]));
  }
}
