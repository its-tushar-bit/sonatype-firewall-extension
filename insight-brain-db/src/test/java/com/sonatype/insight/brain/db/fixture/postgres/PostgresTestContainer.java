/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.fixture.postgres;

import java.time.ZoneId;
import java.util.UUID;
import java.util.function.Consumer;

import com.github.dockerjava.api.command.CreateContainerCmd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Custom extension of the testcontainers.org {@link PostgreSQLContainer} which retains our legacy database name,
 * username, and password. Additionally, we set the time zone of the database to the that of the system.
 */
class PostgresTestContainer
    extends PostgreSQLContainer
{
  private static final Logger log = LoggerFactory.getLogger(PostgresTestContainer.class);

  private static final String DEFAULT_NAME = "testdata";

  private static final String DEFAULT_USERNAME = "testuser";

  private static final String DEFAULT_PASSWORD = "testpass";

  public PostgresTestContainer(String imageVersion) {
    super(PostgreSQLContainer.IMAGE + ":" + imageVersion);

    // use legacy names
    withDatabaseName(DEFAULT_NAME);
    withUsername(DEFAULT_USERNAME);
    withPassword(DEFAULT_PASSWORD);

    // assign a static container name for easier access from the console
    withCreateContainerCmdModifier((Consumer<CreateContainerCmd>) PostgresTestContainer::assignContainerName);

    configure();

    addEnv("TZ", ZoneId.systemDefault().getId());
    log.info("Started Postgres Test Cluster.");
  }

  /**
   * The docker container will start with the name `iq-test-db`. Connect to it with `psql` using this command:
   *
   * <pre>
   * docker exec -it $(docker ps --quiet --filter name=iq-test-db) psql -U testuser -d testdata
   * </pre>
   *
   * Use standard psql commands to connect to the current test database. `\l` will list databases. `\c databasename`
   * will connect to the given database.
   */
  private static void assignContainerName(CreateContainerCmd createContainerCmd) {
    createContainerCmd.withName("iq-test-db-" + UUID.randomUUID().toString().substring(0, 5).toLowerCase());
  }

  /**
   * Get a JDBC url using the specified database name
   */
  public String getJdbcUrl(final String databaseName) {
    String additionalUrlParams = constructUrlParameters("?", "&");
    return "jdbc:postgresql://" + getHost() + ":" + getMappedPort(POSTGRESQL_PORT) + "/" + databaseName +
        additionalUrlParams;
  }
}
