/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.postgres;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
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
 * Wraps a dockerized Postgres server for testing.
 * 
 * <pre>
 * try (PostgresServer postgres = new PostgresServer()) {
 *   // use server, data is discarded when server is closed
 * }
 * </pre>
 */
public class PostgresServer
    implements AutoCloseable
{
  private static final Logger log = LoggerFactory.getLogger(PostgresServer.class);

  private static Optional<Exception> dockerError;

  private final String hostname;

  private final int port;

  private final String username;

  private final String password;

  private final String databaseName;

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
      if (Boolean.getBoolean("postgres.optional")) {
        throw new AssumptionViolatedException("Docker unavailable", dockerError.get());
      }
      throw new AssertionError("Docker unavailable, either start docker daemon"
          + " or set system property postgres.optional=true to skip tests for Postgres", dockerError.get());
    }
  }

  public PostgresServer() {
    assumeSupported();
    username = "testuser";
    password = "testpass";
    databaseName = "testdata";
    try {
      dockerClient = DefaultDockerClient.fromEnv().apiVersion("v1.30").build();
      hostname = dockerClient.getHost();
      log.info("Creating postgres server");
      String image = "postgres:10.7-alpine";
      try {
        dockerClient.inspectImage(image);
      }
      catch (ImageNotFoundException e) {
        dockerClient.pull(image);
      }
      containerId = dockerClient.createContainer(ContainerConfig.builder() //
          .image(image) //
          .env("POSTGRES_DB=" + databaseName, //
              "POSTGRES_USER=" + username, //
              "POSTGRES_PASSWORD=" + password) //
          .hostConfig(HostConfig.builder() //
              .autoRemove(true) //
              .portBindings(Collections.singletonMap("5432/tcp", Arrays.asList(PortBinding.randomPort(null)))) //
              .build())
          .build(), "postgres-" + UUID.randomUUID().toString().substring(0, 8)).id();
      log.info("Starting postgres server");
      dockerClient.startContainer(containerId);
      ContainerInfo containerInfo = dockerClient.inspectContainer(containerId);
      port = Integer.parseInt(containerInfo.networkSettings().ports().get("5432/tcp").get(0).hostPort());
      LogStream logStream = dockerClient.logs(containerId, LogsParam.follow(), LogsParam.stdout(), LogsParam.stderr());
      containerLog = new DockerLogger(logStream);
    }
    catch (Exception e) {
      throw new IllegalStateException("Could not start postgres server", e);
    }
    awaitServerPort();
    log.info("Started postgres server {}:{}", hostname, port);
  }

  private void awaitServerPort() {
    log.info("Awaiting postgres server {}:{}", hostname, port);
    for (long start = System.currentTimeMillis();;) {
      try (Connection connection = DriverManager.getConnection(getJdbcUrl(), getUsername(), getPassword())) {
        return;
      }
      catch (SQLException e) {
        if (System.currentTimeMillis() - start > TimeUnit.SECONDS.toMillis(60)) {
          throw new IllegalStateException("Could not connect to postgres server", e);
        }
        // port not yet ready, keep trying
      }
    }
  }

  public String getJdbcUrl() {
    return "jdbc:postgresql://" + getHostname() + ":" + getPort() + "/" + getDatabaseName();
  }

  public String getHostname() {
    return hostname;
  }

  public int getPort() {
    return port;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getDatabaseName() {
    return databaseName;
  }

  @Override
  public void close() {
    if (dockerClient != null) {
      try {
        try {
          if (containerId != null) {
            log.info("Stopping postgres server {}:{}", hostname, port);
            dockerClient.stopContainer(containerId, 1);
          }
        }
        catch (DockerException | InterruptedException e) {
          log.warn("Failed to stop postgres server", e);
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

  /**
   * Forwards the stdout/stderr from the docker container to the test log to aid troubleshooting.
   */
  private static class DockerLogger
      extends Thread
      implements AutoCloseable
  {
    private final LogStream logStream;

    private volatile boolean closed;

    public DockerLogger(LogStream logStream) {
      this.logStream = logStream;
      setName("postgres");
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
      catch (Throwable t) {
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
