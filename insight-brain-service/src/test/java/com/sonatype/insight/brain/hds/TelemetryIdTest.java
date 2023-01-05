/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.DatabaseConfig;
import com.sonatype.insight.brain.service.InsightConfig;

import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.jetty.HttpsConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TelemetryIdTest
{
  private final SystemConfigurationPropertyDAO dao = new SystemConfigurationPropertyDAO();

  @After
  @Before
  public void cleanup() {
    SystemConfigurationProperty generatedIdProperty = dao
        .getByName(TelemetryId.TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME);
    if (generatedIdProperty != null) {
      dao.delete(generatedIdProperty);
    }
  }

  @Test
  public void testInitialize() {
    int port = 8700;
    InsightConfig insightConfig = new InsightConfig();
    setFirstApplicationConnectorPort(insightConfig, port);

    // Initialize the telemetry ID, the generated and derived parts must be calculated.
    TelemetryId telemetryId = new TelemetryId(insightConfig);
    SystemConfigurationProperty generatedIdProperty1 = dao
        .getByName(TelemetryId.TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME);
    assertThat(generatedIdProperty1.getValue()).isNotNull();
    String telemetryId1 = telemetryId.getId();
    assertThat(telemetryId1).startsWith(generatedIdProperty1.getValue() + "-").hasSize(11);

    // Initialize the telemetry ID again, the generated and derived parts should not change.
    telemetryId = new TelemetryId(insightConfig);
    SystemConfigurationProperty generatedIdProperty2 = dao
        .getByName(TelemetryId.TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME);
    assertThat(generatedIdProperty2.getId()).isEqualTo(generatedIdProperty1.getId());
    assertThat(generatedIdProperty2.getValue()).isEqualTo(generatedIdProperty1.getValue());
    String telemetryId2 = telemetryId.getId();
    assertThat(telemetryId2).isEqualTo(telemetryId1);

    // Initialize the telemetry ID again using a different port, the generated part should not change, but the derived
    // part should change.
    setFirstApplicationConnectorPort(insightConfig, port + 1);
    telemetryId = new TelemetryId(insightConfig);
    SystemConfigurationProperty generatedIdProperty3 = dao
        .getByName(TelemetryId.TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME);
    assertThat(generatedIdProperty3.getId()).isEqualTo(generatedIdProperty2.getId());
    assertThat(generatedIdProperty3.getValue()).isEqualTo(generatedIdProperty2.getValue());
    String telemetryId3 = telemetryId.getId();
    assertThat(telemetryId3.substring(0, 6)).isEqualTo(telemetryId2.substring(0, 6));
    assertThat(telemetryId3).isNotEqualTo(telemetryId2);
  }

  @Test
  public void testInitialize_MultiplePorts() {
    InsightConfig insightConfig = new InsightConfig();
    setApplicationHttpConnectors(insightConfig, 8090, 7080, 8080);

    // Initialize the telemetry ID, the generated and derived parts must be calculated.
    TelemetryId telemetryId = new TelemetryId(insightConfig);
    SystemConfigurationProperty generatedIdProperty = dao
        .getByName(TelemetryId.TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME);
    assertValidTelemetryId(generatedIdProperty, telemetryId);

    // Initialize the telemetry ID again, the generated and derived parts should not change.
    assertTelemetryIdSame(generatedIdProperty, telemetryId, new TelemetryId(insightConfig));

    // Initialize the telemetry ID again with a different port order, the generated and derived parts should not change.
    setApplicationHttpConnectors(insightConfig, 8080, 7080, 8090);
    assertTelemetryIdSame(generatedIdProperty, telemetryId, new TelemetryId(insightConfig));

    // Initialize the telemetry ID again using a different port, the generated part should not change, but the derived
    // part should change.
    setApplicationHttpConnectors(insightConfig, 8081, 7080, 8090);
    assertTelemetryIdDifferentDerived(generatedIdProperty, telemetryId, new TelemetryId(insightConfig));

    // Initialize the telemetry ID again using different ports, the generated part should not change, but the derived
    // part should change.
    setApplicationHttpConnectors(insightConfig, 8081, 7081, 8091);
    assertTelemetryIdDifferentDerived(generatedIdProperty, telemetryId, new TelemetryId(insightConfig));

    // Initialize the telemetry ID again adding a port, the generated part should not change, but the derived part
    // should change.
    setApplicationHttpConnectors(insightConfig, 8090, 7080, 8080, 8060);
    assertTelemetryIdDifferentDerived(generatedIdProperty, telemetryId, new TelemetryId(insightConfig));

    // Initialize the telemetry ID again removing a port, the generated part should not change, but the derived part
    // should change.
    setApplicationHttpConnectors(insightConfig, 8090, 8080);
    assertTelemetryIdDifferentDerived(generatedIdProperty, telemetryId, new TelemetryId(insightConfig));

    // Initialize the telemetry ID again changing ports to have the same sorted concatenation without a separator, the 
    // generated part should not change, but the derived part should change.
    setApplicationHttpConnectors(insightConfig, 8090, 70, 80, 8080);
    assertTelemetryIdDifferentDerived(generatedIdProperty, telemetryId, new TelemetryId(insightConfig));

    // Initialize the telemetry ID again with https ports, the generated and derived parts should not change.
    setApplicationHttpsConnectors(insightConfig, 8090, 7080, 8080);
    assertTelemetryIdSame(generatedIdProperty, telemetryId, new TelemetryId(insightConfig));
  }

  private void assertValidTelemetryId(SystemConfigurationProperty generatedIdProperty, TelemetryId telemetryId) {
    assertThat(generatedIdProperty.getValue()).isNotNull();
    String telemetryId1 = telemetryId.getId();
    assertThat(telemetryId1).startsWith(generatedIdProperty.getValue() + "-").hasSize(11);
  }

  private void assertTelemetryIdSame(SystemConfigurationProperty generatedIdProperty,
                                     TelemetryId telemetryId,
                                     TelemetryId newTelemetryId)
  {
    SystemConfigurationProperty newGeneratedIdProperty = dao
        .getByName(TelemetryId.TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME);
    assertThat(newGeneratedIdProperty.getId()).isEqualTo(generatedIdProperty.getId());
    assertThat(newGeneratedIdProperty.getValue()).isEqualTo(generatedIdProperty.getValue());
    assertThat(newTelemetryId.getId()).isEqualTo(telemetryId.getId());
  }

  private void assertTelemetryIdDifferentDerived(SystemConfigurationProperty generatedIdProperty,
                                                 TelemetryId telemetryId,
                                                 TelemetryId newTelemetryId)
  {
    SystemConfigurationProperty newGeneratedIdProperty = dao
        .getByName(TelemetryId.TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME);
    assertThat(newGeneratedIdProperty.getId()).isEqualTo(generatedIdProperty.getId());
    assertThat(newGeneratedIdProperty.getValue()).isEqualTo(generatedIdProperty.getValue());
    assertThat(newTelemetryId.getId().substring(0, 6)).isEqualTo(telemetryId.getId().substring(0, 6));
    assertThat(newTelemetryId.getId()).isNotEqualTo(telemetryId.getId());
  }

  private void setApplicationHttpConnectors(InsightConfig insightConfig, int... ports) {
    DefaultServerFactory serverFactory = (DefaultServerFactory) insightConfig.getServerFactory();
    List<ConnectorFactory> applicationConnectors = new ArrayList<>();
    for (int port : ports) {
      HttpConnectorFactory httpConnectorFactory = new HttpConnectorFactory();
      httpConnectorFactory.setPort(port);
      applicationConnectors.add(httpConnectorFactory);
    }
    serverFactory.setApplicationConnectors(applicationConnectors);
  }

  private void setApplicationHttpsConnectors(InsightConfig insightConfig, int... ports) {
    DefaultServerFactory serverFactory = (DefaultServerFactory) insightConfig.getServerFactory();
    List<ConnectorFactory> applicationConnectors = new ArrayList<>();
    for (int port : ports) {
      HttpsConnectorFactory httpsConnectorFactory = new HttpsConnectorFactory();
      httpsConnectorFactory.setPort(port);
      applicationConnectors.add(httpsConnectorFactory);
    }
    serverFactory.setApplicationConnectors(applicationConnectors);
  }

  @Test
  public void testCalculateClusterId() {
    String sampleConfigFingerPrint = getSampleDatabaseConfigFingerPrint();

    DatabaseConfig databaseConfig = getSampleDatabaseConfig();
    assertThat(TelemetryId.calculateClusterId(databaseConfig)).isEqualTo(sampleConfigFingerPrint);

    // changing hostname changes the fingerprint
    databaseConfig = getSampleDatabaseConfig();
    databaseConfig.setHostname(databaseConfig.getHostname() + "-changed");
    assertThat(TelemetryId.calculateClusterId(databaseConfig)).isNotEqualTo(sampleConfigFingerPrint);

    // changing port changes the fingerprint
    databaseConfig = getSampleDatabaseConfig();
    databaseConfig.setPort(databaseConfig.getPort() + 1);
    assertThat(TelemetryId.calculateClusterId(databaseConfig)).isNotEqualTo(sampleConfigFingerPrint);

    // changing name changes the fingerprint
    databaseConfig = getSampleDatabaseConfig();
    databaseConfig.setName(databaseConfig.getName() + "-changed");
    assertThat(TelemetryId.calculateClusterId(databaseConfig)).isNotEqualTo(sampleConfigFingerPrint);

    // changing username does not modify the fingerprint
    databaseConfig = getSampleDatabaseConfig();
    databaseConfig.setUsername(databaseConfig.getUsername() + "-changed");
    assertThat(TelemetryId.calculateClusterId(databaseConfig)).isEqualTo(sampleConfigFingerPrint);

    // changing password does not modify the fingerprint
    databaseConfig = getSampleDatabaseConfig();
    databaseConfig.setPassword(databaseConfig.getPassword() + "-changed");
    assertThat(TelemetryId.calculateClusterId(databaseConfig)).isEqualTo(sampleConfigFingerPrint);
  }

  @Test
  public void testCalculateDerivedId() {
    List<byte[]> hardwareAddresses = new ArrayList<>();
    hardwareAddresses.add("123456789ABC".getBytes(StandardCharsets.UTF_8));
    assertThat(TelemetryId.calculateDerivedId("somehost", "7788", hardwareAddresses)).isEqualTo("e7c7e");

    assertThat(TelemetryId.calculateDerivedId("otherhost", "7788", hardwareAddresses)).isEqualTo("9ee29");

    assertThat(TelemetryId.calculateDerivedId("somehost", "8899", hardwareAddresses)).isEqualTo("17868");

    hardwareAddresses.add("123456789DEF".getBytes(StandardCharsets.UTF_8));
    assertThat(TelemetryId.calculateDerivedId("somehost", "7788", hardwareAddresses)).isEqualTo("e1380");
  }

  @Test
  public void testGetId() {
    InsightConfig insightConfig = new InsightConfig();
    setFirstApplicationConnectorPort(insightConfig, 1234);
    TelemetryId telemetryId = new TelemetryId(insightConfig);
    SystemConfigurationProperty generatedIdProperty = dao
        .getByName(TelemetryId.TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME);
    assertThat(telemetryId.getId()).startsWith(generatedIdProperty.getValue() + "-");
  }

  private void setFirstApplicationConnectorPort(InsightConfig insightConfig, int port) {
    ((HttpConnectorFactory) ((DefaultServerFactory) insightConfig.getServerFactory()).getApplicationConnectors().get(0))
        .setPort(port);
  }

  @Test
  public void testInitialize_ClusterIdIncludedForExternalDatabase() {
    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setDatabase(getSampleDatabaseConfig());
    TelemetryId telemetryId = new TelemetryId(insightConfig);
    assertThat(telemetryId.getClusterId()).isEqualTo(getSampleDatabaseConfigFingerPrint());
  }

  @Test
  public void testInitialize_ClusterIdNullForEmbeddedDatabase() {
    InsightConfig insightConfig = new InsightConfig();
    TelemetryId telemetryId = new TelemetryId(insightConfig);
    assertThat(telemetryId.getClusterId()).isNull();
  }

  private DatabaseConfig getSampleDatabaseConfig() {
    DatabaseConfig database = new DatabaseConfig();
    database.setHostname("aws-postgres");
    database.setPort(1234);
    database.setUsername("username");
    database.setPassword("password");
    return database;
  }

  private String getSampleDatabaseConfigFingerPrint() {
    return "38062673f41e45363a51161d8d0b91dcf6ff81cb0636743161ad2f73796420af5e62dd7f43d9f49f957724a64c6971399aab937" +
        "a0cdb05467720ae053c113a57";
  }
}
