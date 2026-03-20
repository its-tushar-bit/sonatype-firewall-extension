/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.keycloak;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Collections;
import java.util.UUID;

import com.sonatype.insight.docker.utils.DockerUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class KeycloakServer
    implements AutoCloseable
{
  private static final Logger log = LoggerFactory.getLogger(KeycloakServer.class);

  // Available docker images at https://quay.io/repository/keycloak/keycloak?tab=tags
  public static final String IMAGE_NAME = "keycloak/keycloak";

  // IMPORTANT: Keep this version in sync with keycloak.version in pom.xml
  public static final String IMAGE_VERSION = "26.4.7";

  public static final String IMAGE = IMAGE_NAME + ":" + IMAGE_VERSION;

  public static final String DEFAULT_USERNAME = "admin";

  public static final String DEFAULT_PASSWORD = "admin";

  public static final int DEFAULT_PORT = 8080;

  private final GenericContainer<?> container;

  private final String username = DEFAULT_USERNAME;

  private final String password = DEFAULT_PASSWORD;

  private final String networkAlias;

  private final String containerId;

  private final String containerName;

  private final int port;

  private final String hostname;

  public KeycloakServer() {
    this(null);
  }

  public KeycloakServer(Network network) {
    DockerUtils.assumeSupported();
    try {
      container = new GenericContainer<>(DockerImageName.parse(DockerUtils.applyRegistry(IMAGE)));
      container.addExposedPort(DEFAULT_PORT);
      container.waitingFor(Wait.forLogMessage(".*Keycloak .* started in.*", 1));
      container.withStartupTimeout(Duration.ofMinutes(2));
      container.addEnv("DB_VENDOR", "h2");
      container.addEnv("KEYCLOAK_ADMIN", username);
      container.addEnv("KEYCLOAK_ADMIN_PASSWORD", password);
      container.addEnv("TZ", ZoneId.systemDefault().getId());
      if (network != null) {
        container.setNetwork(network);
      }
      networkAlias = "keycloak-" + UUID.randomUUID().toString().substring(0, 8);
      container.setNetworkAliases(Collections.singletonList(networkAlias));
      container.setCommand("start-dev");
      container.start();
      container.followOutput(new Slf4jLogConsumer(log).withSeparateOutputStreams());
      containerId = container.getContainerId();
      containerName = container.getContainerName();
      port = container.getMappedPort(DEFAULT_PORT);
      hostname = DockerUtils.getHostname(container.getHost(), port);
      log.info("Started Keycloak Sever {}.", this);
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new IllegalStateException("Could not start keycloak docker container.", e);
    }
  }

  @Override
  public String toString() {
    return "KeycloakServer{" +
        "container=" + container +
        ", username='" + username + '\'' +
        ", password='" + password + '\'' +
        ", networkAlias='" + networkAlias + '\'' +
        ", containerId='" + containerId + '\'' +
        ", containerName='" + containerName + '\'' +
        ", port=" + port +
        ", hostname='" + hostname + '\'' +
        '}';
  }

  @Override
  public void close() {
    if (container != null) {
      container.stop();
    }
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public GenericContainer<?> getContainer() {
    return container;
  }

  public String getNetworkAlias() {
    return networkAlias;
  }

  public String getContainerId() {
    return containerId;
  }

  public String getContainerName() {
    return containerName;
  }

  public int getPort() {
    return port;
  }

  public String getHostname() {
    return hostname;
  }

  public String getBaseUrl() {
    return "http://" + getHostname() + ":" + getPort();
  }

  public String getInternalBaseUrl() {
    return "http://" + getNetworkAlias() + ":" + DEFAULT_PORT;
  }
}
