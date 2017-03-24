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
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import com.sonatype.insight.brain.product.license.CLMLicenseManager.LicenseSummary;
import com.sonatype.insight.brain.support.SystemInfo.NetworkInterfaceWrapper;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import org.hamcrest.core.Is;
import org.junit.Test;

import static com.sonatype.insight.brain.support.LimitedFileInputStreamTest.CONFIG_YML;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
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
{
  private final String lineSeparator = System.lineSeparator();

  @Test
  public void testIsSensitiveKey() {
    assertThat(SystemInfo.isSensitiveKey("myPasswordLikePropertyName"), is(true));
    assertThat(SystemInfo.isSensitiveKey("myPassPhrasePropertyName"), is(true));
    assertThat(SystemInfo.isSensitiveKey("normalProp"), is(false));
    assertThat(SystemInfo.isSensitiveKey(""), is(false));
  }

  @Test
  public void testObfuscateValue() {
    assertThat(SystemInfo.obfuscateValue("myPasswordLikePropertyName", "yadda"), Is.<Object>is(SystemInfo.MASK));
    assertThat(SystemInfo.obfuscateValue("myPassPhrasePropertyName", "yadda"), Is.<Object>is(SystemInfo.MASK));
    assertThat(SystemInfo.obfuscateValue("normalProp", "normalValue"), Is.<Object>is("normalValue"));
    assertThat(SystemInfo.obfuscateValue("", "normalValue"), Is.<Object>is("normalValue"));
  }

  @Test
  public void testGetObfuscatedProperties_MaskPassword() {
    verifyGetObfuscatedProperties_MaskKeyValue("myPasswordLikePropertyName");
    verifyGetObfuscatedProperties_MaskKeyValue("myPassPhrasePropertyName");
  }

  private void verifyGetObfuscatedProperties_MaskKeyValue(final String keyName) {
    System.setProperty(keyName, "yadda");
    try {
      final SortedMap<String, Object> obfuscatedProps = SystemInfo.getObfuscatedSystemProperties()
          .get("system-properties");
      assertThat(obfuscatedProps.get(keyName), Is.<Object>is(SystemInfo.MASK));
    }
    finally {
      System.getProperties().remove(keyName);
    }
  }

  @Test
  public void testGetObfuscatedSystemProperties() throws Exception {
    final Map<String, SortedMap<String, Object>> map = SystemInfo.getObfuscatedSystemProperties();
    assertThat(map.size(), is(1));

    final SortedMap<String, Object> entries = map.get("system-properties");
    assertThat(entries.get("awt.toolkit"), notNullValue());
    assertThat(entries.get("user.dir"), notNullValue());
    assertThat(entries.get("user.name"), notNullValue());
    assertThat(entries.get("user.timezone"), notNullValue());
  }

  @Test
  public void testGetObfuscatedEnvironment() throws Exception {
    final Map<String, SortedMap<String, Object>> map = SystemInfo.getObfuscatedEnvironment();
    assertThat(map.size(), is(1));

    final SortedMap<String, Object> entries = map.get("system-environment");
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
      obfuscatedYaml = SystemInfo.getObfuscatedYaml(reader);
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

    final Map<String, Map<String, Object>> entryPassphraseMap = (Map<String, Map<String, Object>>) obufscatedMap
        .get("testphrase");
    assertThat(entryPassphraseMap.get("mypassphrasearray"), Is.<Object>is(SystemInfo.MASK));

    final Map<String, Map<String, Object>> entryTestMap = (Map<String, Map<String, Object>>) obufscatedMap
        .get("testmap");
    assertThat(entryTestMap.get("json_map-passwords"), Is.<Object>is(SystemInfo.MASK));
    assertThat(entryTestMap.get("json_seq-passwords"), Is.<Object>is(SystemInfo.MASK));
  }

  @Test
  public void testGetSystemRuntime() throws Exception {
    final Map<String, SortedMap<String, Object>> map = SystemInfo.getSystemRuntime();
    assertThat(map.size(), is(1));

    final SortedMap<String, Object> entries = map.get("system-runtime");
    assertThat(entries.get("availableProcessors"), notNullValue());
    assertThat(entries.get("freeMemory"), notNullValue());
    assertThat(entries.get("maxMemory"), notNullValue());
    assertThat(entries.get("threads"), notNullValue());
    assertThat(entries.get("totalMemory"), notNullValue());
    assertThat(entries + "", entries.size(), is(5));
  }

  @Test
  public void testGetReportTime() throws Exception {
    final Map<String, SortedMap<String, Object>> map = SystemInfo.getReportTime();
    assertThat(map.size(), is(1));

    final SortedMap<String, Object> entries = map.get("system-time");
    assertThat(entries.get("timezone"), notNullValue());
    assertThat(entries.get("current"), notNullValue());
    assertThat(entries.get("iso8601"), notNullValue());
    assertThat(entries + "", entries.size(), is(3));
  }

  @Test
  public void testGetFileStores() throws Exception {
    final Map<String, SortedMap<String, Object>> map = SystemInfo.getFileStores();
    assertThat(map.size(), is(1));

    final SortedMap<String, Object> entries = map.get("system-filestores");
    assertThat(entries.size(), greaterThan(0));

    @SuppressWarnings("unchecked")
    final Map<String, Object> entry = (Map<String, Object>) entries.get(entries.firstKey());
    assertThat(entry.get("description"), notNullValue());
    assertThat(entry.get("type"), notNullValue());
    assertThat(entry.get("totalSpace"), notNullValue());
    assertThat(entry.get("usableSpace"), notNullValue());
    assertThat(entry.get("unallocatedSpace"), notNullValue());
    assertThat(entry.get("readOnly"), notNullValue());
  }

  @Test
  public void testGetFileStoreWithException() throws Exception {
    final FileStore fileStore = mock(FileStore.class);
    when(fileStore.type()).thenReturn("myType");
    when(fileStore.getUnallocatedSpace()).thenThrow(new IOException("testException"));
    final TreeMap<String, Object> map = SystemInfo.getFileStore(fileStore);
    assertThat(map.size(), is(5));

    assertThat(map.get("description"), notNullValue());
    assertThat((String) map.get("type"), is("myType"));
    assertThat(map.get("totalSpace"), notNullValue());
    assertThat(map.get("usableSpace"), notNullValue());
    assertThat(map.get("unallocatedSpace"), nullValue());
    assertThat(map.get("readOnly"), notNullValue());

    when(fileStore.getTotalSpace()).thenThrow(new IOException("testException2"));
    final TreeMap<String, Object> map2 = SystemInfo.getFileStore(fileStore);
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
    final Map<String, SortedMap<String, Object>> map = SystemInfo.getNetworkInterfaces();
    assertThat(map.size(), is(1));

    final SortedMap<String, Object> entries = map.get("system-network");
    assertThat(entries.toString(), entries.size(), greaterThan(0));

    final Map<String, Object> entry = (Map<String, Object>) entries.get(entries.firstKey());
    assertThat(entry.get("displayName"), notNullValue());
    assertThat(entry.get("up"), notNullValue());
    assertThat(entry.get("virtual"), notNullValue());
    assertThat(entry.get("multicast"), notNullValue());
    assertThat(entry.get("loopback"), notNullValue());
    assertThat(entry.get("ptp"), notNullValue());
    assertThat(entry.get("mtu"), notNullValue());
    assertThat(entry.get("addresses"), notNullValue());
    assertThat(entry + "", entry.size(), is(8));
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

    final TreeMap<String, Object> map = SystemInfo.getNetworkInterfaceWithWrapper(networkInterface);
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
    final TreeMap<String, Object> map2 = SystemInfo.getNetworkInterfaceWithWrapper(networkInterface);
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
    final List<Map<String, SortedMap<String, Object>>> list = SystemInfo.getSystemInfo();
    assertThat(list.get(0).keySet().iterator().next(), is("system-time"));
    assertThat(list.get(1).keySet().iterator().next(), is("system-properties"));
    assertThat(list.get(2).keySet().iterator().next(), is("system-environment"));
    assertThat(list.get(3).keySet().iterator().next(), is("system-runtime"));
    assertThat(list.get(4).keySet().iterator().next(), is("system-network"));
    assertThat(list.get(5).keySet().iterator().next(), is("system-filestores"));
    assertThat(list.size(), is(6));
  }

  @Test
  public void testGetPropertiesJson() throws Exception {
    final Properties properties = new Properties();
    properties.put("c", "3");
    properties.put("a", "1");
    properties.put("b", "2");

    final String json = SystemInfo.getPropertiesJson(properties, "parentName");
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
    final String json = SystemInfo.getProductLicense(licenseSummary);
    assertThat(json, is("{" + lineSeparator +
        "  \"fingerprint\" : \"fprint\"," + lineSeparator +
        "  \"expiryTimestamp\" : -1," + lineSeparator +
        "  \"features\" : null," + lineSeparator +
        "  \"productEdition\" : \"edition\"" + lineSeparator +
        "}"));
  }

  @Test
  public void testGetThreadDump() throws Exception {
    final String text = SystemInfo.getThreadDump();
    assertTrue(text.contains("VirtualMachineMetrics"));
  }

  @Test
  public void testGetLdapConfigEmpty() throws Exception {
    final List<LdapConfig> ldapServers = new ArrayList<>();
    assertThat(SystemInfo.getLdapConfig(ldapServers), is("[ ]"));
  }
}
