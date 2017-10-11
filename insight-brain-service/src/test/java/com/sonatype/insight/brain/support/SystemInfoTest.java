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
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.file.FileStore;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.inject.Inject;

import com.sonatype.insight.brain.product.license.CLMLicenseManager.LicenseSummary;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.support.SystemInfo.NetworkInterfaceWrapper;

import com.yammer.dropwizard.config.LoggingConfiguration;
import com.yammer.dropwizard.config.LoggingConfiguration.FileConfiguration;
import com.yammer.dropwizard.config.RequestLogConfiguration;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import static com.sonatype.insight.brain.support.LimitedFileInputStreamTest.CONFIG_YML;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertTrue;
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

  @Inject
  private SystemInfo systemInfo;

  @Inject
  @Override
  protected void customizeConfig(final InsightConfig config) {
    final LoggingConfiguration serverLoggingConfiguration = new LoggingConfiguration();
    final FileConfiguration serverFileConfiguration = new FileConfiguration();
    serverFileConfiguration.setCurrentLogFilename(SERVER_LOG_FILENAME);
    serverLoggingConfiguration.setFileConfiguration(serverFileConfiguration);
    config.setLoggingConfiguration(serverLoggingConfiguration);

    final RequestLogConfiguration requestLoggingConfiguration = new RequestLogConfiguration();
    final FileConfiguration requestFileConfiguration = new FileConfiguration();
    requestFileConfiguration.setCurrentLogFilename(REQUEST_LOG_FILENAME);
    requestLoggingConfiguration.setFileConfiguration(requestFileConfiguration);
    config.getHttpConfiguration().setRequestLogConfiguration(requestLoggingConfiguration);
  }

    @Test
  public void testIsSensitiveKey() {
    assertThat(systemInfo.isSensitiveKey("myPasswordLikePropertyName"), is(true));
    assertThat(systemInfo.isSensitiveKey("myPassPhrasePropertyName"), is(true));
    assertThat(systemInfo.isSensitiveKey("normalProp"), is(false));
    assertThat(systemInfo.isSensitiveKey(""), is(false));
  }

  @Test
  public void testObfuscateValue() {
    assertThat(systemInfo.obfuscateValue("myPasswordLikePropertyName", "yadda"), Is.<Object>is(SystemInfo.MASK));
    assertThat(systemInfo.obfuscateValue("myPassPhrasePropertyName", "yadda"), Is.<Object>is(SystemInfo.MASK));
    assertThat(systemInfo.obfuscateValue("normalProp", "normalValue"), Is.<Object>is("normalValue"));
    assertThat(systemInfo.obfuscateValue("", "normalValue"), Is.<Object>is("normalValue"));
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
      assertThat(obfuscatedProps.getKey(), is("system-properties"));
      assertThat(obfuscatedProps.getValue().get(keyName), Is.<Object>is(SystemInfo.MASK));
    }
    finally {
      System.getProperties().remove(keyName);
    }
  }

  @Test
  public void testGetObfuscatedSystemProperties() throws Exception {
    final Entry<String, SortedMap<String, Object>> entry = systemInfo.getObfuscatedSystemProperties();
    assertThat(entry.getKey(), is("system-properties"));

    final SortedMap<String, Object> entries = entry.getValue();
    assertThat(entries.get("awt.toolkit"), notNullValue());
    assertThat(entries.get("user.dir"), notNullValue());
    assertThat(entries.get("user.name"), notNullValue());
    assertThat(entries.get("user.timezone"), notNullValue());
  }

  @Test
  public void testGetObfuscatedEnvironment() throws Exception {
    final Entry<String, SortedMap<String, Object>> entry = systemInfo.getObfuscatedEnvironment();
    assertThat(entry.getKey(), is("system-environment"));

    final SortedMap<String, Object> entries = entry.getValue();
    assertThat(entries.size(), greaterThan(0));
    assertThat(entries.size(), is(System.getenv().size()));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetObfuscatedYaml() throws Exception {
    final File configYml = new File(LimitedFileInputStream.class.getResource(CONFIG_YML).getFile());
    assertThat(configYml.exists(), is(true));

    final String obfuscatedYaml;
    try (final InputStream reader = new FileInputStream(configYml)) {
      obfuscatedYaml = systemInfo.getObfuscatedYaml(reader);
    }
    final Map<String, Object> obufscatedMap = (Map<String, Object>) new Yaml().load(obfuscatedYaml);

    assertThat(obufscatedMap.get("sonatypeWork"), Is.<Object>is("./sonatype-work/clm-server"));

    final Map<String, Object> entryHttp = (Map<String, Object>) obufscatedMap.get("http");
    assertThat(entryHttp.get("port"), Is.<Object>is(8070));
    assertThat(entryHttp.get("adminPort"), Is.<Object>is(8071));

    final Map<String, Map<String, Object>> entryHttpRequest = (Map<String, Map<String, Object>>) entryHttp
        .get("requestLog");
    assertThat(entryHttpRequest.get("console").get("enabled"), Is.<Object>is(false));
    assertThat(entryHttpRequest.get("console").size(), Is.<Object>is(1));

    assertThat(entryHttpRequest.get("file").get("enabled"), Is.<Object>is(true));
    assertThat(entryHttpRequest.get("file").get("currentLogFilename"), Is.<Object>is("./log/request.log"));
    assertThat(entryHttpRequest.get("file").get("archivedLogFilenamePattern"),
        Is.<Object>is("./log/request-%d.log.gz"));
    assertThat(entryHttpRequest.get("file").get("archivedFileCount"), Is.<Object>is(50));
    assertThat(entryHttpRequest.get("file").size(), Is.<Object>is(4));

    // validate obfuscation
    assertThat(obufscatedMap.get("testPassword"), Is.<Object>is(SystemInfo.MASK));

    final Map<String, Object> entryPassphraseMap = (Map<String, Object>) obufscatedMap.get("testphrase");
    assertThat(entryPassphraseMap.get("mypassphrasearray"), Is.<Object>is(SystemInfo.MASK));

    final Map<String, Object> entryTestMap = (Map<String, Object>) obufscatedMap.get("testmap");
    assertThat(entryTestMap.get("json_map-passwords"), Is.<Object>is(SystemInfo.MASK));
    assertThat(entryTestMap.get("json_seq-passwords"), Is.<Object>is(SystemInfo.MASK));
  }

  @Test
  public void testGetSystemRuntime() throws Exception {
    final Entry<String, SortedMap<String, Object>> entry = systemInfo.getSystemRuntime();
    assertThat(entry.getKey(), is("system-runtime"));

    final SortedMap<String, Object> entries = entry.getValue();
    assertThat(entries.get("availableProcessors"), notNullValue());
    assertThat(entries.get("freeMemory"), notNullValue());
    assertThat(entries.get("maxMemory"), notNullValue());
    assertThat(entries.get("threads"), notNullValue());
    assertThat(entries.get("totalMemory"), notNullValue());
    assertThat(entries + "", entries.size(), is(5));
  }

  @Test
  public void testGetReportTime() throws Exception {
    final Entry<String, SortedMap<String, Object>> entry = systemInfo.getReportTime();
    assertThat(entry.getKey(), is("system-time"));

    final SortedMap<String, Object> entries = entry.getValue();
    assertThat(entries.get("timezone"), notNullValue());
    assertThat(entries.get("current"), notNullValue());
    assertThat(entries.get("iso8601"), notNullValue());
    assertThat(entries + "", entries.size(), is(3));
  }

  @Test
  public void testGetInstallInfo() throws Exception {
    final File originalConfigFile = InsightBrainService.getConfigFile();
    final File expectedConfigFile = new File("myConfig.yml");
    final Entry<String, SortedMap<String, Object>> entry;
    try {
      InsightBrainService.setConfigFile(expectedConfigFile);
      entry = systemInfo.getInstallInfo();
    } finally {
      InsightBrainService.setConfigFile(originalConfigFile);
    }
    assertThat(entry.getKey(), is("install-info"));

    final SortedMap<String, Object> entries = entry.getValue();

    assertThat(entries.get("application-jar").toString(),
        endsWith(InsightBrainService.class.getSimpleName() + ".class"));

    assertThat(entries.get("configfile").toString(), is(expectedConfigFile.getAbsolutePath()));

    assertThat(entries.get("instanceId").toString(), is(InsightBrainService.getInstanceId()));
    assertThat(entries.get("hostname-ip").toString(), is(InsightBrainService.getLocalHostString()));

    final File workDir = new File(entries.get("sonatypeWork").toString());
    assertThat(workDir.isDirectory(), is(true));
    assertThat(workDir.isAbsolute(), is(true));
    assertThat(entries.get("sonatypeWorkContent"), notNullValue());

    final File auditDir = new File(entries.get("auditDir").toString());
    assertThat(auditDir.isAbsolute(), is(true));
    assertThat(entries.get("auditDirContent"), nullValue());

    assertThat(entries.get("downloadsDirContent"), nullValue());

    final String serverLog = (String) entries.get("serverLog");
    assertThat(serverLog, endsWith(SERVER_LOG_FILENAME));
    final File serverFile = new File(serverLog);
    assertThat(serverFile.isAbsolute(), is(true));

    final String requestValue = (String) entries.get("requestLog");
    assertThat(requestValue, endsWith(REQUEST_LOG_FILENAME));
    final File requestFile = new File(requestValue);
    assertThat(requestFile.isAbsolute(), is(true));

    assertThat(entries.toString(), entries.size(), is(11));
  }

  @Test
  public void testGetFileStores() throws Exception {
    final Entry<String, SortedMap<String, Object>> entry = systemInfo.getFileStores();
    assertThat(entry.getKey(), is("system-filestores"));

    final SortedMap<String, Object> entries = entry.getValue();
    assertThat(entries.size(), greaterThan(0));

    @SuppressWarnings("unchecked")
    final Map<String, Object> fileStoresEntry = (Map<String, Object>) entries.get(entries.firstKey());
    assertThat(fileStoresEntry.get("description"), notNullValue());
    assertThat(fileStoresEntry.get("type"), notNullValue());
    assertThat(fileStoresEntry.get("totalSpace"), notNullValue());
    assertThat(fileStoresEntry.get("usableSpace"), notNullValue());
    assertThat(fileStoresEntry.get("unallocatedSpace"), notNullValue());
    assertThat(fileStoresEntry.get("readOnly"), notNullValue());
  }

  @Test
  public void testGetFileStoreWithException() throws Exception {
    final FileStore fileStore = mock(FileStore.class);
    when(fileStore.type()).thenReturn("myType");
    when(fileStore.getUnallocatedSpace()).thenThrow(new IOException("testException"));
    final TreeMap<String, Object> map = systemInfo.getFileStore(fileStore);
    assertThat(map.size(), is(5));

    assertThat(map.get("description"), notNullValue());
    assertThat((String) map.get("type"), is("myType"));
    assertThat(map.get("totalSpace"), notNullValue());
    assertThat(map.get("usableSpace"), notNullValue());
    assertThat(map.get("unallocatedSpace"), nullValue());
    assertThat(map.get("readOnly"), notNullValue());

    when(fileStore.getTotalSpace()).thenThrow(new IOException("testException2"));
    final TreeMap<String, Object> map2 = systemInfo.getFileStore(fileStore);
    assertThat(map2.size(), is(3));
    assertThat(map2.get("description"), notNullValue());
    assertThat((String) map2.get("type"), is("myType"));
    assertThat(map2.get("totalSpace"), nullValue());
    assertThat(map2.get("usableSpace"), nullValue());
    assertThat(map2.get("unallocatedSpace"), nullValue());
    assertThat(map2.get("readOnly"), notNullValue());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetNetworkInterfaces() throws Exception {
    final Entry<String, SortedMap<String, Object>> entry = systemInfo.getNetworkInterfaces();
    assertThat(entry.getKey(), is("system-network"));

    final SortedMap<String, Object> entries = entry.getValue();
    assertThat(entries.toString(), entries.size(), greaterThan(0));

    final Map<String, Object> networkInterfacesEntry = (Map<String, Object>) entries.get(entries.firstKey());
    assertThat(networkInterfacesEntry.get("displayName"), notNullValue());
    assertThat(networkInterfacesEntry.get("up"), notNullValue());
    assertThat(networkInterfacesEntry.get("virtual"), notNullValue());
    assertThat(networkInterfacesEntry.get("multicast"), notNullValue());
    assertThat(networkInterfacesEntry.get("loopback"), notNullValue());
    assertThat(networkInterfacesEntry.get("ptp"), notNullValue());
    assertThat(networkInterfacesEntry.get("mtu"), notNullValue());
    assertThat(networkInterfacesEntry.get("addresses"), notNullValue());
    assertThat(networkInterfacesEntry + "", networkInterfacesEntry.size(), is(8));
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
    assertThat(map.get("displayName"), notNullValue());
    assertThat(map.get("up"), notNullValue());
    assertThat(map.get("virtual"), notNullValue());
    assertThat(map.get("multicast"), notNullValue());
    assertThat(map.get("loopback"), notNullValue());
    assertThat(map.get("ptp"), notNullValue());
    assertThat(map.get("mtu"), nullValue());
    assertThat(((TreeSet<String>) map.get("addresses")).size(), is(0));
    assertThat(map + "", map.size(), is(7));

    when(networkInterface.isUp()).thenThrow(new SocketException("testException2"));
    final TreeMap<String, Object> map2 = systemInfo.getNetworkInterfaceWithWrapper(networkInterface);
    assertThat(map2.get("displayName"), notNullValue());
    assertThat(map2.get("up"), nullValue());
    assertThat(map2.get("virtual"), nullValue());
    assertThat(map2.get("multicast"), nullValue());
    assertThat(map2.get("loopback"), nullValue());
    assertThat(map2.get("ptp"), nullValue());
    assertThat(map2.get("mtu"), nullValue());
    assertThat(((TreeSet<String>) map2.get("addresses")).size(), is(0));
    assertThat(map + "", map2.size(), is(2));
  }

  @Test
  public void testGetSystemInfo() throws Exception {
    final List<Entry<String, SortedMap<String, Object>>> list = systemInfo.getSystemInfo(null);
    int i = 0;
    assertThat(list.get(i++).getKey(), is("system-time"));
    assertThat(list.get(i++).getKey(), is("install-info"));
    assertThat(list.get(i++).getKey(), is("system-properties"));
    assertThat(list.get(i++).getKey(), is("system-environment"));
    assertThat(list.get(i++).getKey(), is("system-runtime"));
    assertThat(list.get(i++).getKey(), is("system-network"));
    assertThat(list.get(i++).getKey(), is("system-filestores"));
    assertThat(list.get(i).getKey(), is("client-info"));
    assertThat(list, hasSize(8));
  }

  @Test
  public void testGetPropertiesJson() throws Exception {
    final Properties properties = new Properties();
    properties.put("c", "3");
    properties.put("a", "1");
    properties.put("b", "2");

    final String json = systemInfo.getPropertiesJson(properties, "parentName");
    assertThat(json, is("{" + lineSeparator +
        "  \"parentName\" : {" + lineSeparator +
        "    \"a\" : \"1\"," + lineSeparator +
        "    \"b\" : \"2\"," + lineSeparator +
        "    \"c\" : \"3\"" + lineSeparator +
        "  }" + lineSeparator +
        "}"));
  }

  @Test
  public void testGetProduceLicense() throws Exception {
    final LicenseSummary licenseSummary = new LicenseSummary("fprint", -1, null, "edition");
    final String json = systemInfo.getProductLicense(licenseSummary);
    assertThat(json, is("{" + lineSeparator +
        "  \"fingerprint\" : \"fprint\"," + lineSeparator +
        "  \"expiryTimestamp\" : -1," + lineSeparator +
        "  \"features\" : null," + lineSeparator +
        "  \"productEdition\" : \"edition\"" + lineSeparator +
        "}"));
  }

  @Test
  public void testGetThreadDump() throws Exception {
    final String text = systemInfo.getThreadDump();
    assertTrue(text.contains("VirtualMachineMetrics"));
  }

  @Test
  public void testGetLdapConfigEmpty() throws Exception {
    final List<LdapConfig> ldapServers = new ArrayList<>();
    assertThat(systemInfo.getLdapConfig(ldapServers), is("[ ]"));
  }

  @Test
  public void testGetClientInfo() throws Exception {
    final String requestUrl = "myRequestUrl";
    final Entry<String, SortedMap<String, Object>> entry = systemInfo.getClientInfo(requestUrl);
    final SortedMap<String, Object> entries = entry.getValue();
    assertThat(entries.get("requestUrl").toString(), is(requestUrl));
  }
}
