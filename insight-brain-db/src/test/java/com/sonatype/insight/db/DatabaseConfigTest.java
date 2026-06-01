/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.db;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DatabaseConfigTest
{
  @Test
  public void testGetUrl_directUrl() {
    DatabaseConfig config = new DatabaseConfig();
    config.setUrl("jdbc:postgresql://myhost:5432/mydb");
    assertThat(config.getUrl()).isEqualTo("jdbc:postgresql://myhost:5432/mydb");
  }

  @Test
  public void testGetUrl_fieldChangesOverrideUrl() {
    DatabaseConfig config = new DatabaseConfig();
    config.setUrl("jdbc:postgresql://myhost:5432/mydb");
    config.setHostname("otherhost");
    config.setPort(9999);
    config.setName("otherdb");
    assertThat(config.getUrl()).isEqualTo("jdbc:postgresql://otherhost:9999/otherdb");
  }

  @Test
  public void testGetUrl_fromFields_noPort() {
    DatabaseConfig config = new DatabaseConfig();
    config.setHostname("localhost");
    config.setName("test-db");
    assertThat(config.getUrl()).isEqualTo("jdbc:postgresql://localhost/test-db");
  }

  @Test
  public void testGetUrl_fromFields_withPort() {
    DatabaseConfig config = new DatabaseConfig();
    config.setHostname("localhost");
    config.setPort(6543);
    config.setName("test-db");
    assertThat(config.getUrl()).isEqualTo("jdbc:postgresql://localhost:6543/test-db");
  }

  @Test
  public void testGetUrl_fromFields_withParameters() {
    Map<String, String> params = new LinkedHashMap<>();
    params.put("sslmode", "require");
    params.put("key1", "value1");
    DatabaseConfig config = new DatabaseConfig();
    config.setHostname("localhost");
    config.setName("test-db");
    config.setParameters(params);
    assertThat(config.getUrl()).isEqualTo("jdbc:postgresql://localhost/test-db?sslmode=require&key1=value1");
  }

  @Test
  public void testGetUrl_fromFields_parametersFilterOutUserAndPassword() {
    Map<String, String> params = new LinkedHashMap<>();
    params.put("user", "paramuser");
    params.put("password", "parampass");
    params.put("key1", "value1");
    DatabaseConfig config = new DatabaseConfig();
    config.setHostname("localhost");
    config.setName("test-db");
    config.setParameters(params);
    assertThat(config.getUrl()).isEqualTo("jdbc:postgresql://localhost/test-db?key1=value1");
  }

  @Test
  public void testGetUrl_neitherUrlNorHostname_returnsNull() {
    DatabaseConfig config = new DatabaseConfig();
    assertThat(config.getUrl()).isNull();
  }

  @Test
  public void testGetFields_populatesFieldsFromUrl() {
    DatabaseConfig config = new DatabaseConfig();
    config.setUrl("jdbc:postgresql://myhost:5432/mydb");

    assertThat(config.getHostname()).isEqualTo("myhost");
    assertThat(config.getPort()).isEqualTo(5432);
    assertThat(config.getName()).isEqualTo("mydb");
  }

  @Test
  public void testGetFields_populatesFieldsFromUrl_noPort() {
    DatabaseConfig config = new DatabaseConfig();
    config.setUrl("jdbc:postgresql://myhost/mydb");

    assertThat(config.getHostname()).isEqualTo("myhost");
    assertThat(config.getPort()).isEqualTo(5432);
    assertThat(config.getName()).isEqualTo("mydb");
  }

  @Test
  public void testGetFields_urlOverridesExistingFields() {
    DatabaseConfig config = new DatabaseConfig();
    config.setHostname("existinghost");
    config.setPort(9999);
    config.setName("existingdb");
    config.setUrl("jdbc:postgresql://myhost:5432/mydb");

    assertThat(config.getHostname()).isEqualTo("myhost");
    assertThat(config.getPort()).isEqualTo(5432);
    assertThat(config.getName()).isEqualTo("mydb");
  }

  @Test
  public void testSetField_afterSetUrl_syncsOtherFieldsFromUrl() {
    DatabaseConfig config = new DatabaseConfig();
    config.setUrl("jdbc:postgresql://urlhost:5432/urldb");
    config.setHostname("fieldhost");

    assertThat(config.getHostname()).isEqualTo("fieldhost");
    assertThat(config.getName()).isEqualTo("urldb");
    assertThat(config.getPort()).isEqualTo(5432);
    assertThat(config.getUrl()).isEqualTo("jdbc:postgresql://fieldhost:5432/urldb");
  }

  @Test
  public void testGetFields_parsesParametersFromUrl() {
    DatabaseConfig config = new DatabaseConfig();
    config.setUrl("jdbc:postgresql://myhost:5432/mydb?sslmode=require&key1=val1");

    assertThat(config.getParameters()).containsEntry("sslmode", "require");
    assertThat(config.getParameters()).containsEntry("key1", "val1");
  }

  @Test
  public void testCopyConstructor_producesEqualIndependentCopy() {
    Map<String, String> params = new LinkedHashMap<>();
    params.put("sslmode", "require");

    DatabaseConfig config = new DatabaseConfig();
    config.setDriverClassName("org.postgresql.Driver");
    config.setType("postgresql");
    config.setHostname("myhost");
    config.setPort(5432);
    config.setName("mydb");
    config.setParameters(params);
    config.setUsername("user");
    config.setPassword("pass");
    config.setMaxConnections(50);
    config.setMaxIdleConnections(10);
    config.setReadOnly(true);
    config.setApplicationName("IQ");
    config.setSessionVariables("SET search_path TO public");
    config.setOptions("-c statement_timeout=5000");
    config.setMaxRetryAttempts(3);
    config.setMaxRetryDurationSeconds(60);
    config.setMaxConnectionLifetimeSeconds(300);
    config.setConnectionValidationTimeoutSeconds(5);
    config.setMaxWaitSeconds(10);

    DatabaseConfig copy = new DatabaseConfig(config);

    assertThat(copy).usingRecursiveComparison().isEqualTo(config);
    assertThat(copy).isNotSameAs(config);
    assertThat(copy.getParameters()).isNotSameAs(config.getParameters());
  }

  @Test
  public void testCopyConstructor_mutatingCopyDoesNotAffectOriginal() {
    DatabaseConfig config = new DatabaseConfig();
    config.setHostname("myhost");
    config.setName("mydb");
    config.setMaxConnections(50);

    DatabaseConfig copy = new DatabaseConfig(config);
    copy.setMaxConnections(99);
    copy.setHostname("changed");

    assertThat(config.getMaxConnections()).isEqualTo(50);
    assertThat(config.getHostname()).isEqualTo("myhost");
  }

  @Test
  public void testSetParameters_overridesUrlParameters() {
    Map<String, String> newParams = new LinkedHashMap<>();
    newParams.put("existing", "value");

    DatabaseConfig config = new DatabaseConfig();
    config.setUrl("jdbc:postgresql://myhost:5432/mydb?sslmode=require");
    config.setParameters(newParams);

    assertThat(config.getParameters()).containsEntry("existing", "value");
    assertThat(config.getParameters()).doesNotContainKey("sslmode");
  }

  @Test
  public void testSetUrl_updatesFields() {
    DatabaseConfig config = new DatabaseConfig();
    config.setUrl("jdbc:postgresql://oldhost:5432/olddb");

    assertThat(config.getHostname()).isEqualTo("oldhost");
    assertThat(config.getName()).isEqualTo("olddb");

    config.setUrl("jdbc:postgresql://newhost:9999/newdb");

    assertThat(config.getHostname()).isEqualTo("newhost");
    assertThat(config.getPort()).isEqualTo(9999);
    assertThat(config.getName()).isEqualTo("newdb");
  }

  @Test
  public void testGetUrl_reflectsFieldChanges() {
    DatabaseConfig config = new DatabaseConfig();
    config.setHostname("host1");
    config.setName("db1");

    assertThat(config.getUrl()).isEqualTo("jdbc:postgresql://host1/db1");

    config.setHostname("host2");

    assertThat(config.getUrl()).isEqualTo("jdbc:postgresql://host2/db1");
  }

  @Test
  public void testSetPort_updatesUrl() {
    DatabaseConfig config = new DatabaseConfig();
    config.setUrl("jdbc:postgresql://myhost:5432/mydb");

    config.setPort(9999);

    assertThat(config.getUrl()).isEqualTo("jdbc:postgresql://myhost:9999/mydb");
  }

  @Test
  public void testSetName_updatesUrl() {
    DatabaseConfig config = new DatabaseConfig();
    config.setUrl("jdbc:postgresql://myhost:5432/mydb");

    config.setName("otherdb");

    assertThat(config.getUrl()).isEqualTo("jdbc:postgresql://myhost:5432/otherdb");
  }

  @Test
  public void testSetParameters_updatesUrl() {
    DatabaseConfig config = new DatabaseConfig();
    config.setUrl("jdbc:postgresql://myhost/mydb");

    Map<String, String> params = new LinkedHashMap<>();
    params.put("sslmode", "require");
    config.setParameters(params);

    assertThat(config.getUrl()).isEqualTo("jdbc:postgresql://myhost:5432/mydb?sslmode=require");
  }

  @Test
  public void testIsValidConnectionConfig_urlWithCredentials() {
    DatabaseConfig config = new DatabaseConfig();
    config.setUrl("jdbc:postgresql://myhost:5432/mydb");
    config.setUsername("user");
    config.setPassword("pass");

    assertThat(config.isValidConnectionConfig()).isTrue();
  }

  @Test
  public void testIsValidConnectionConfig_fieldsWithCredentials() {
    DatabaseConfig config = new DatabaseConfig();
    config.setHostname("myhost");
    config.setName("mydb");
    config.setUsername("user");
    config.setPassword("pass");

    assertThat(config.isValidConnectionConfig()).isTrue();
  }

  @Test
  public void testIsValidConnectionConfig_missingConnectionTarget() {
    DatabaseConfig config = new DatabaseConfig();
    config.setUsername("user");
    config.setPassword("pass");

    assertThat(config.isValidConnectionConfig()).isFalse();
  }

  @Test
  public void testIsValidConnectionConfig_missingUsername() {
    DatabaseConfig config = new DatabaseConfig();
    config.setUrl("jdbc:postgresql://myhost:5432/mydb");
    config.setPassword("pass");

    assertThat(config.isValidConnectionConfig()).isFalse();
  }

  @Test
  public void testIsValidConnectionConfig_missingPassword() {
    DatabaseConfig config = new DatabaseConfig();
    config.setUrl("jdbc:postgresql://myhost:5432/mydb");
    config.setUsername("user");

    assertThat(config.isValidConnectionConfig()).isFalse();
  }

  @Test
  public void testIsValidConnectionConfig_hostnameWithoutName() {
    DatabaseConfig config = new DatabaseConfig();
    config.setHostname("myhost");
    config.setUsername("user");
    config.setPassword("pass");

    assertThat(config.isValidConnectionConfig()).isFalse();
  }

  @Test
  public void testGetFields_urlParametersExcludeCredentials() {
    DatabaseConfig config = new DatabaseConfig();
    config.setUrl("jdbc:postgresql://myhost/mydb?user=foo&password=secret&sslmode=require");

    assertThat(config.getParameters()).containsEntry("sslmode", "require");
    assertThat(config.getParameters()).doesNotContainKey("user");
    assertThat(config.getParameters()).doesNotContainKey("password");
  }
}
