/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.postgres;

import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Collections;
import java.util.UUID;

import com.sonatype.insight.db.DatabaseConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

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

  public static final String IMAGE_NAME = "postgres";

  public static final String IMAGE_VERSION = "15.1-alpine";

  public static final String IMAGE = IMAGE_NAME + ":" + IMAGE_VERSION;

  public static final String DEFAULT_NAME = "testdata";

  public static final String DEFAULT_USERNAME = "testuser";

  public static final String DEFAULT_PASSWORD = "testpass";

  public static final int DEFAULT_PORT = 5432;

  private final GenericContainer<?> container;

  private final String name = DEFAULT_NAME;

  private final String username = DEFAULT_USERNAME;

  private final String password = DEFAULT_PASSWORD;

  private final String networkAlias;

  private final String containerId;

  private final String containerName;

  private final int port;

  private final String hostname;

  public PostgresServer() {
    this(null);
  }

  public PostgresServer(Network network) {
    Utils.assumeSupported();
    try {
      container = new GenericContainer<>(DockerImageName.parse(Utils.applyRegistry(IMAGE)));
      container.addExposedPort(DEFAULT_PORT);
      container.waitingFor(new WaitAllStrategy()
          .withStrategy(Wait.forListeningPort())
          .withStrategy((new LogMessageWaitStrategy())
              .withRegEx(".*database system is ready to accept connections.*\\s").withTimes(2)
              .withStartupTimeout(Duration.ofMinutes(2))));
      container.addEnv("POSTGRES_DB", name);
      container.addEnv("POSTGRES_USER", username);
      container.addEnv("POSTGRES_PASSWORD", password);
      container.addEnv("TZ", ZoneId.systemDefault().getId());
      if (network != null) {
        container.setNetwork(network);
      }
      networkAlias = "postgres-" + UUID.randomUUID().toString().substring(0, 8);
      container.setNetworkAliases(Collections.singletonList(networkAlias));
      container.start();
      container.followOutput(new Slf4jLogConsumer(log).withSeparateOutputStreams());
      containerId = container.getContainerId();
      containerName = container.getContainerName();
      port = container.getMappedPort(DEFAULT_PORT);
      hostname = container.getHost();
      log.info("Started Postgres Sever {}.", this);
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new IllegalStateException("Could not start postgres docker container.", e);
    }
  }

  @Override
  public String toString() {
    return "PostgresServer{" +
        "container=" + container +
        ", name='" + name + '\'' +
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

  public String getName() {
    return name;
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

  public String getJdbcUrl() {
    return "jdbc:postgresql://" + getHostname() + ":" + getPort() + "/" + getName();
  }

  public String getInternalJdbcUrl() {
    return "jdbc:postgresql://" + getNetworkAlias() + ":" + DEFAULT_PORT + "/" + getName();
  }

  public DatabaseConfig getDatabaseConfig() {
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDriverClassName(org.postgresql.Driver.class.getName());
    databaseConfig.setUrl(getJdbcUrl());
    databaseConfig.setUsername(getUsername());
    databaseConfig.setPassword(getPassword());
    databaseConfig.setMaxConnections(50);
    return databaseConfig;
  }

  public void loadSqlDump(Path sqlFile) {
    log.info("Loading SQL dump {}", sqlFile);
    try {
      container.copyFileToContainer(MountableFile.forHostPath(sqlFile), "/tmp/" + sqlFile.getFileName());
      String[] cmd = {
          "/usr/local/bin/psql", "--variable", "ON_ERROR_STOP=1", "--dbname", getName(), "--username",
          getUsername(), "--file", "/tmp/" + sqlFile.getFileName()
      };
      ExecResult execResult = container.execInContainer(cmd);
      if (execResult.getExitCode() != 0) {
        throw new Exception("psql returned " + execResult.getExitCode());
      }
    }
    catch (Exception e) {
      throw new IllegalStateException("Could not load SQL dump into postgres server", e);
    }
  }
}
