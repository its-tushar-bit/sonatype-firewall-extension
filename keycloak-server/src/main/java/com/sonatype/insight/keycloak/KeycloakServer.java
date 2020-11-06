/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.keycloak;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerClient.LogsParam;
import com.spotify.docker.client.LogMessage;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.exceptions.ImageNotFoundException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;
import org.junit.AssumptionViolatedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps a dockerized Keycloak server for testing.
 */
class KeycloakServer
    implements AutoCloseable
{
  private static final Logger log = LoggerFactory.getLogger(KeycloakServer.class);

  private static Optional<Exception> dockerError;

  private String hostname;

  private final int port;

  public static final String USERNAME = "admin";

  public static final String PASSWORD = "admin";

  private final DockerClient dockerClient;

  private final String containerId;

  private final DockerLogger containerLog;

  private static boolean isSupported() {
    if (dockerError == null) {
      try (DockerClient dockerClient =
               DefaultDockerClient.fromEnv().connectTimeoutMillis(TimeUnit.SECONDS.toMillis(3)).build()) {
        log.info("Docker: {}", dockerClient.version());
        dockerError = Optional.empty();
      }
      catch (Exception e) {
        log.warn("Docker not available", e);
        dockerError = Optional.of(e);
      }
    }
    return !dockerError.isPresent();
  }

  private static void assumeSupported() {
    if (!isSupported()) {
      if (Boolean.getBoolean("docker.optional")) {
        throw new AssumptionViolatedException("Docker unavailable", dockerError.get());
      }
      throw new AssertionError("Docker unavailable, either start docker daemon"
          + " or set system property docker.optional=true to skip docker-based tests", dockerError.get());
    }
  }

  public KeycloakServer() {
    assumeSupported();
    try {
      dockerClient = DefaultDockerClient.fromEnv().apiVersion("v1.30").build();
      hostname = dockerClient.getHost();
      String image = applyRegistry("jboss/keycloak:7.0.0");
      log.info("Creating keycloak server from image {}", image);
      try {
        dockerClient.inspectImage(image);
      }
      catch (ImageNotFoundException e) {
        dockerClient.pull(image);
      }
      containerId = dockerClient.createContainer(ContainerConfig.builder() //
          .image(image) //
          .env("KEYCLOAK_USER=" + USERNAME, "KEYCLOAK_PASSWORD=" + PASSWORD, "DB_VENDOR=h2") //
          .hostConfig(HostConfig.builder() //
              .autoRemove(true) //
              .portBindings(Collections.singletonMap("8080/tcp", Arrays.asList(PortBinding.randomPort(null)))) //
              .build())
          .build(), "keycloak-" + UUID.randomUUID().toString().substring(0, 8)).id();
      log.info("Starting keycloak server");
      dockerClient.startContainer(containerId);
      ContainerInfo containerInfo = dockerClient.inspectContainer(containerId);
      port = Integer.parseInt(containerInfo.networkSettings().ports().get("8080/tcp").get(0).hostPort());
      LogStream logStream = dockerClient.logs(containerId, LogsParam.follow(), LogsParam.stdout(), LogsParam.stderr());
      containerLog = new DockerLogger("keycloak", logStream);
    }
    catch (Exception e) {
      throw new IllegalStateException("Could not start keycloak server", e);
    }
    awaitServerPort();
    if ("localhost".equalsIgnoreCase(hostname) || "127.0.0.1".equals(hostname)) {
      // for the server to be reachable from other containers, it needs to have a non-loopback address
      hostname = findHostIpAddress();
    }
    log.info("Started keycloak server {}:{}", hostname, port);
  }

  private static String applyRegistry(String image) {
    String registry = System.getProperty("docker.registry", "");
    return (registry.isEmpty() ? "" : registry + '/') + image;
  }

  private void awaitServerPort() {
    log.info("Awaiting keycloak server {}:{}", hostname, port);
    for (long start = System.currentTimeMillis(); ; ) {
      try {
        testConnection(hostname, port);
        break;
      }
      catch (IOException e) {
        if (System.currentTimeMillis() - start > TimeUnit.SECONDS.toMillis(120)) {
          throw new IllegalStateException("Could not connect to keycloak server", e);
        }
        // port not yet ready, keep trying
      }
    }
  }

  private String findHostIpAddress() {
    try {
      for (Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
          netInterfaces.hasMoreElements();) {
        NetworkInterface netInterface = netInterfaces.nextElement();
        try {
          if (netInterface.isUp() && !netInterface.isLoopback()) {
            for (Enumeration<InetAddress> addresses = netInterface.getInetAddresses(); addresses.hasMoreElements();) {
              InetAddress address = addresses.nextElement();
              try {
                if (!address.isLoopbackAddress()) {
                  testConnection(address.getHostAddress(), port);
                  return address.getHostAddress();
                }
              }
              catch (IOException ignored) {
                // try the next address
              }
            }
          }
        }
        catch (IOException ignored) {
          // try the next interface
        }
      }
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    throw new IllegalStateException("Could not determine IP address");
  }

  private static void testConnection(String hostname, int port) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) new URL("http://" + hostname + ":" + port).openConnection();
    connection.setConnectTimeout(1000);
    connection.setReadTimeout(1000);
    try (InputStream is = connection.getInputStream()) {
      return;
    }
    finally {
      connection.disconnect();
    }
  }

  public String getHostname() {
    return hostname;
  }

  public int getPort() {
    return port;
  }

  @Override
  public void close() {
    if (dockerClient != null) {
      try {
        try {
          if (containerId != null) {
            log.info("Stopping keycloak server {}:{}", hostname, port);
            dockerClient.stopContainer(containerId, 1);
          }
        }
        catch (DockerException | InterruptedException e) {
          log.warn("Failed to stop keycloak server", e);
        }
        finally {
          if (containerLog != null) {
            containerLog.close();
          }
        }
      }
      finally {
        dockerClient.close();
      }
    }
  }

  public String getUrl() {
    return String.format("http://%s:%d/auth/", hostname, port);
  }

  /**
   * Forwards the stdout/stderr from the docker container to the test log to aid troubleshooting.
   */
  private static class DockerLogger
      extends Thread
      implements AutoCloseable
  {
    private final LogStream logStream;

    private volatile boolean closed;

    public DockerLogger(String processName, LogStream logStream) {
      this.logStream = logStream;
      setName(processName);
      setDaemon(true);
      start();
    }

    @Override
    public void close() {
      closed = true;
      interrupt();
    }

    @Override
    public void run() {
      try {
        while (!closed && logStream.hasNext()) {
          String logMessage = getLogMessageText(logStream.next());
          log.info(logMessage);
        }
      }
      catch (RuntimeException t) {
        log.warn("Failed to read server log", t);
      }
      finally {
        logStream.close();
      }
    }

    private static String getLogMessageText(final LogMessage logMessage) {
      ByteBuffer buffer = logMessage.content();
      byte[] bytes = new byte[buffer.remaining()];
      buffer.get(bytes);
      int length = bytes.length;
      while (length > 0 && (bytes[length - 1] == '\n' || bytes[length - 1] == '\r')) {
        length--;
      }
      return new String(bytes, 0, length, StandardCharsets.UTF_8);
    }
  }
}
