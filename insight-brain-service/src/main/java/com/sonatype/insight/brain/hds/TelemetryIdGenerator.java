/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.InsightConfig;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TelemetryIdGenerator
{
  public static final Pattern TELEMETRY_ID_PATTERN = Pattern.compile("^[0-9a-f]{5}-[0-9a-f]{5}$");

  public static final String TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME = "TELEMETRY_GENERATED_INSTANCE_ID";

  private static final Logger log = LoggerFactory.getLogger(TelemetryIdGenerator.class);

  private static final String CORRUPTED_TELEMETRY_HOST = "****";

  private static final int ID_PART_LENGTH = 5;

  public static String generateId(InsightConfig insightConfig, SystemConfigurationPropertyDAO dao) {
    // There is a requirement to not be able to link the telemetry IDs to customers.
    // This means we should not log them anywhere, because if we have the logs from a customer, then we can link the IDs
    // to the customer.

    // The generated part of the telemetry instance id is generated the first time the server is started on a machine,
    // i.e. if it wasn't generated before.
    //
    // Note, there are cases where the telemetry host (first 5 chars of telemetry ID) becomes corrupted with '****',
    // which is invalid and should be corrected when encountered
    SystemConfigurationProperty generatedIdProperty = dao.getByName(TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME);
    final var configMissing = null == generatedIdProperty;
    if (configMissing || StringUtils.isBlank(generatedIdProperty.getValue())
        || generatedIdProperty.getValue().startsWith(CORRUPTED_TELEMETRY_HOST))
    {
      String generatedId = UUID.randomUUID().toString().substring(0, ID_PART_LENGTH);
      generatedIdProperty = new SystemConfigurationProperty(TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME, generatedId);
      if (configMissing) {
        dao.insert(generatedIdProperty);
      }
      else {
        dao.update(generatedIdProperty);
      }
      log.debug("Generated a new instance ID.");
    }
    else {
      log.debug("The generated instance ID already exists.");
    }

    String hostname;
    try {
      hostname = InetAddress.getLocalHost().getHostName();
    }
    catch (Exception e) {
      log.warn("Cannot get the hostname for the local machine: " + e.getMessage(), e);
      hostname = "localhost";
    }
    List<NetworkInterface> networkInterfaces;
    try {
      networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
    }
    catch (Exception e) {
      log.warn("Cannot get the network interfaces for the local machine: " + e.getMessage(), e);
      networkInterfaces = new ArrayList<>();
    }

    List<byte[]> hardwareAddresses = new ArrayList<>();
    for (NetworkInterface networkInterface : networkInterfaces) {
      try {
        if (!networkInterface.isUp()) {
          // If the interface is down, then it may not be configured properly.
          continue;
        }

        byte[] hardwareAddress = networkInterface.getHardwareAddress();
        if (hardwareAddress == null || hardwareAddress.length == 0) {
          continue;
        }

        hardwareAddresses.add(hardwareAddress);
      }
      catch (Exception e) {
        log.warn("Error loading details for a network interface: " + e.getMessage(), e);
      }
    }

    String ports = normalizePorts(insightConfig.getApplicationConnectorPorts());

    String derivedId = calculateDerivedId(hostname, ports, hardwareAddresses);
    String result = generatedIdProperty.getValue() + "-" + derivedId;
    return result;
  }

  private static String normalizePorts(String ports) {
    if (ports == null) {
      return null;
    }

    return Pattern.compile(",")
        .splitAsStream(ports)
        .map(String::trim)
        .filter(StringUtils::isNotEmpty)
        .sorted()
        .collect(Collectors.joining(","));
  }

  @SuppressWarnings("deprecation")
  static String calculateDerivedId(String hostname, String ports, List<byte[]> hardwareAddresses) {
    // Calculate the derived ID as the SHA1 of the bytes of hostname + port + all network interface hardware addresses.
    Hasher hasher = Hashing.sha1().newHasher();
    hasher.putString(hostname + ports, StandardCharsets.UTF_8);

    for (byte[] hardwareAddress : hardwareAddresses) {
      hasher.putBytes(hardwareAddress);
    }

    return hasher.hash().toString().substring(0, ID_PART_LENGTH);
  }
}
