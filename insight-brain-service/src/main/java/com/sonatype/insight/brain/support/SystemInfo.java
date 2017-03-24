/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;

import com.sonatype.insight.brain.product.license.CLMLicenseManager.LicenseSummary;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import com.yammer.metrics.core.VirtualMachineMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.27
 */
class SystemInfo
{
  private static final Logger log = LoggerFactory.getLogger(SystemInfo.class);

  static final String MASK = "****";

  static boolean isSensitiveKey(final String key) {
    final String lowercaseKey = key.toLowerCase(Locale.ENGLISH);
    return lowercaseKey.contains("password") || lowercaseKey.contains("passphrase");
  }

  static Object obfuscateValue(final String key, final Object value) {
    if (isSensitiveKey(key)) {
      return MASK;
    }
    else {
      return value;
    }
  }

  static Map<String, SortedMap<String, Object>> getObfuscatedSystemProperties() {
    final SortedMap<String, Object> entries = new TreeMap<>();

    final Properties iterationSafeCopy = (Properties) System.getProperties().clone();
    for (final Entry<Object, Object> entry : iterationSafeCopy.entrySet()) {
      final String key = entry.getKey() + "";
      entries.put(key, obfuscateValue(key, entry.getValue()));
    }

    final Map<String, SortedMap<String, Object>> mapEntry = new HashMap<>();
    mapEntry.put("system-properties", entries);
    return mapEntry;
  }

  static Map<String, SortedMap<String, Object>> getObfuscatedEnvironment() {
    final SortedMap<String, Object> entries = new TreeMap<>();

    final Map<String, String> systemEnvironment = System.getenv();
    for (final Entry<String, String> entry : systemEnvironment.entrySet()) {
      final String key = entry.getKey() + "";
      entries.put(key, obfuscateValue(key, entry.getValue()));
    }

    final Map<String, SortedMap<String, Object>> mapEntry = new HashMap<>();
    mapEntry.put("system-environment", entries);
    return mapEntry;
  }

  static String getObfuscatedYaml(final InputStream input) {

    final Yaml yaml = new Yaml();

    @SuppressWarnings("unchecked")
    final Map<String, Object> map = (Map<String, Object>) yaml.load(input);

    // Recurse nested entries
    for (final Entry<String, Object> entry : map.entrySet()) {
      obfuscateNestedMap(entry);
    }

    return yaml.dump(map);
  }

  private static void obfuscateNestedMap(final Entry<String, Object> entry) {
    if (SystemInfo.isSensitiveKey(entry.getKey())) {
      entry.setValue(SystemInfo.MASK);
    }

    if (entry.getValue() instanceof Map) {
      @SuppressWarnings("unchecked")
      final Map<String, Object> entryMap = (Map<String, Object>) entry.getValue();
      for (final Entry<String, Object> entrySub : entryMap.entrySet()) {
        obfuscateNestedMap(entrySub);
      }
    }
  }

  static Map<String, SortedMap<String, Object>> getReportTime() {
    final SortedMap<String, Object> entries = new TreeMap<>();

    final Date now = new Date();
    entries.put("timezone", TimeZone.getDefault().getID());
    entries.put("current", now.getTime() + "");
    entries.put("iso8601", new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ss.SSSZ").format(now));

    final Map<String, SortedMap<String, Object>> mapEntry = new HashMap<>();
    mapEntry.put("system-time", entries);
    return mapEntry;
  }

  static Map<String, SortedMap<String, Object>> getSystemRuntime() {
    final SortedMap<String, Object> entries = new TreeMap<>();
    final Runtime runtime = Runtime.getRuntime();

    entries.put("availableProcessors", runtime.availableProcessors());
    entries.put("freeMemory", runtime.freeMemory());
    entries.put("totalMemory", runtime.totalMemory());
    entries.put("maxMemory", runtime.maxMemory());
    entries.put("threads", Thread.activeCount());

    final Map<String, SortedMap<String, Object>> mapEntry = new HashMap<>();
    mapEntry.put("system-runtime", entries);
    return mapEntry;
  }

  static Map<String, SortedMap<String, Object>> getFileStores() {
    final SortedMap<String, Object> entries = new TreeMap<>();
    final FileSystem fileSystem = FileSystems.getDefault();
    for (final FileStore fileStore : fileSystem.getFileStores()) {
      final TreeMap<String, Object> items = getFileStore(fileStore);
      entries.put(fileStore.name(), items);
    }
    final Map<String, SortedMap<String, Object>> mapEntry = new HashMap<>();
    mapEntry.put("system-filestores", entries);
    return mapEntry;
  }

  static TreeMap<String, Object> getFileStore(final FileStore fileStore) {
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

  static Map<String, SortedMap<String, Object>> getNetworkInterfaces() {
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

    final Map<String, SortedMap<String, Object>> map = new HashMap<>();
    map.put("system-network", entries);
    return map;
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

  private static TreeMap<String, Object> getNetworkInterface(final NetworkInterface networkInterface) {
    return getNetworkInterfaceWithWrapper(new NetworkInterfaceWrapper(networkInterface));
  }

  static TreeMap<String, Object> getNetworkInterfaceWithWrapper(final NetworkInterfaceWrapper networkInterface) {
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

  static List<Map<String, SortedMap<String, Object>>> getSystemInfo() {
    final List<Map<String, SortedMap<String, Object>>> entries = new ArrayList<>();

    entries.add(getReportTime());
    entries.add(getObfuscatedSystemProperties());
    entries.add(getObfuscatedEnvironment());
    entries.add(getSystemRuntime());
    entries.add(getNetworkInterfaces());
    entries.add(getFileStores());

    return entries;
  }

  static String getSystemInfoJson() {
    final List<Map<String, SortedMap<String, Object>>> entries = getSystemInfo();
    return JsonUtils.format(entries);
  }

  @SuppressWarnings("unchecked")
  static String getPropertiesJson(final Properties properties, final String parentObjectName)
  {
    final SortedMap<String, Object> entries = new TreeMap<>();
    entries.putAll(((Map) properties));

    final Map<String, SortedMap<String, Object>> mapEntry = new HashMap<>();
    mapEntry.put(parentObjectName, entries);
    return JsonUtils.format(mapEntry);
  }

  static String getProductLicense(final LicenseSummary licenseSummary)
  {
    return JsonUtils.format(licenseSummary);
  }

  static String getThreadDump() throws IOException {
    try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      VirtualMachineMetrics.getInstance().threadDump(outputStream);
      return outputStream.toString();
    }
  }

  static String getLdapConfig(final List<LdapConfig> ldapServers) {
    return JsonUtils.format(ldapServers);
  }
}
