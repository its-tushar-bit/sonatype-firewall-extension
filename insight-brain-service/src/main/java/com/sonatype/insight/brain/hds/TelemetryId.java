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
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.InsightConfig;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
/**
 * The telemetry ID is an ID unique to the IQ server instance and it is used to identify and link telemetry data.
 * It has two parts (separated by a dash):
 * - The first part is randomly generated and it has 5 hex digits;
 * - The second part is the SHA1 of the hostname + IQ server HTTP port + all network interface hardware addresses,
 * truncated to the first 5 hex digits.
 * 
 * The telemetry ID cannot be used to identify a customer or customer installation and it should not be linkable to a
 * customer or customer installation.
 * This means we cannot log the ID (or any parts of the ID) anywhere.
 * 
 * Note: I chose 5 as length for the two parts of the ID because it gives a collision risk of 1 in 1,048,576,
 * which is well below what we need.
 */
public class TelemetryId
{
  private static final Logger log = LoggerFactory.getLogger(TelemetryId.class);

  public static final String TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME = "TELEMETRY_GENERATED_INSTANCE_ID";

  private static final int ID_PART_LENGTH = 5;

  private SystemConfigurationPropertyDAO dao = new SystemConfigurationPropertyDAO();

  private String id;

  @Inject
  public TelemetryId(InsightConfig insightConfig) {
    // There is a requirement to not be able to link the telemetry IDs to customers.
    // This means we should not log them anywhere, because if we have the logs from a customer, then we can link the IDs
    // to the customer.

    // The generated part of the telemetry instance id is generated the first time the server is started on a machine,
    // i.e. if it wasn't generated before.
    SystemConfigurationProperty generatedIdProperty = dao.getByName(TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME);
    if (generatedIdProperty == null) {
      String generatedId = UUID.randomUUID().toString().substring(0, ID_PART_LENGTH);
      generatedIdProperty = new SystemConfigurationProperty(TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME, generatedId);
      dao.insert(generatedIdProperty);
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

    String ports = ((DefaultServerFactory) insightConfig.getServerFactory()).getApplicationConnectors().stream()
        .map(applicationConnector -> ((HttpConnectorFactory) applicationConnector).getPort()).sorted()
        .map(String::valueOf).collect(Collectors.joining(","));

    String derivedId = calculateDerivedId(hostname, ports, hardwareAddresses);
    id = generatedIdProperty.getValue() + "-" + derivedId;
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

  public String getId() {
    return id;
  }
}
