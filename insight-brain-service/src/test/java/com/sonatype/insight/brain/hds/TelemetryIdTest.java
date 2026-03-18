/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.brain.hds.util.TelemetryTestUtils;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.DatabaseConfig;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.telemetry.ClusterIdentificationService;

import io.dropwizard.core.server.DefaultServerFactory;
import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.jetty.HttpsConnectorFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.sonatype.insight.brain.hds.TelemetryIdGenerator.TELEMETRY_ID_PATTERN;
import static org.assertj.core.api.Assertions.assertThat;

public class TelemetryIdTest
    extends AbstractDatabaseTest
{
  @Mock
  private ClusterIdentificationService mockClusterIdentificationService;

  private SystemConfigurationPropertyDAO dao;

  @Before
  public void cleanup() {
    dao = new SystemConfigurationPropertyDAO(databaseRule.getOperationalDataStore());
    SystemConfigurationProperty generatedIdProperty = dao
        .getByName(TelemetryId.TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME);
    if (generatedIdProperty != null) {
      dao.delete(generatedIdProperty);
    }
  }

  @Before
  public void setup() {
    mockClusterIdentificationService = TelemetryTestUtils.setupReflectiveMockClusterIdentificationService();
  }

  @Test
  public void testInitialize() {
    int port = 8700;
    InsightConfig insightConfig = new InsightConfig();
    setFirstApplicationConnectorPort(insightConfig, port);

    // Initialize the telemetry ID, the generated and derived parts must be calculated.
    TelemetryId telemetryId = new TelemetryId(insightConfig, dao, mockClusterIdentificationService);

    // when:
    final var telemetryId1 = telemetryId.getId();

    SystemConfigurationProperty generatedIdProperty1 = dao
        .getByName(TelemetryId.TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME);

    assertThat(generatedIdProperty1.getValue()).isNotNull();
    assertThat(telemetryId1).startsWith(generatedIdProperty1.getValue() + "-").hasSize(11);

    // Initialize the telemetry ID again, the generated and derived parts should not change.
    telemetryId = new TelemetryId(insightConfig, dao, mockClusterIdentificationService);
    final var telemetryId2 = telemetryId.getId();

    SystemConfigurationProperty generatedIdProperty2 = dao
        .getByName(TelemetryId.TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME);

    assertThat(generatedIdProperty2.getId()).isEqualTo(generatedIdProperty1.getId());
    assertThat(generatedIdProperty2.getValue()).isEqualTo(generatedIdProperty1.getValue());
    assertThat(telemetryId2).isEqualTo(telemetryId1);

    // Initialize the telemetry ID again using a different port, the generated part should not change, but the derived
    // part should change.
    setFirstApplicationConnectorPort(insightConfig, port + 1);
    telemetryId = new TelemetryId(insightConfig, dao, mockClusterIdentificationService);

    final var telemetryId3 = telemetryId.getId();

    SystemConfigurationProperty generatedIdProperty3 = dao
        .getByName(TelemetryId.TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME);

    assertThat(generatedIdProperty3.getId()).isEqualTo(generatedIdProperty2.getId());
    assertThat(generatedIdProperty3.getValue()).isEqualTo(generatedIdProperty2.getValue());
    assertThat(telemetryId3.substring(0, 6)).isEqualTo(telemetryId2.substring(0, 6));
    assertThat(telemetryId3).isNotEqualTo(telemetryId2);
  }

  @Test
  public void testInitialize_MultiplePorts() {
    InsightConfig insightConfig = new InsightConfig();
    setApplicationHttpConnectors(insightConfig, 8090, 7080, 8080);

    // Initialize the telemetry ID, the generated and derived parts must be calculated.
    TelemetryId telemetryId = new TelemetryId(insightConfig, dao, mockClusterIdentificationService);
    final var telemetryIdValue = telemetryId.getId();

    SystemConfigurationProperty generatedIdProperty = dao
        .getByName(TelemetryId.TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME);

    assertValidTelemetryId(generatedIdProperty, telemetryIdValue);

    // Initialize the telemetry ID again, the generated and derived parts should not change.
    assertTelemetryIdSame(generatedIdProperty, telemetryId, new TelemetryId(insightConfig, dao,
        mockClusterIdentificationService));

    // Initialize the telemetry ID again with a different port order, the generated and derived parts should not change.
    setApplicationHttpConnectors(insightConfig, 8080, 7080, 8090);
    assertTelemetryIdSame(generatedIdProperty, telemetryId, new TelemetryId(insightConfig, dao,
        mockClusterIdentificationService));

    // Initialize the telemetry ID again using a different port, the generated part should not change, but the derived
    // part should change.
    setApplicationHttpConnectors(insightConfig, 8081, 7080, 8090);
    assertTelemetryIdDifferentDerived(generatedIdProperty, telemetryId, new TelemetryId(insightConfig, dao,
        mockClusterIdentificationService));

    // Initialize the telemetry ID again using different ports, the generated part should not change, but the derived
    // part should change.
    setApplicationHttpConnectors(insightConfig, 8081, 7081, 8091);
    assertTelemetryIdDifferentDerived(generatedIdProperty, telemetryId, new TelemetryId(insightConfig, dao,
        mockClusterIdentificationService));

    // Initialize the telemetry ID again adding a port, the generated part should not change, but the derived part
    // should change.
    setApplicationHttpConnectors(insightConfig, 8090, 7080, 8080, 8060);
    assertTelemetryIdDifferentDerived(generatedIdProperty, telemetryId, new TelemetryId(insightConfig, dao,
        mockClusterIdentificationService));

    // Initialize the telemetry ID again removing a port, the generated part should not change, but the derived part
    // should change.
    setApplicationHttpConnectors(insightConfig, 8090, 8080);
    assertTelemetryIdDifferentDerived(generatedIdProperty, telemetryId, new TelemetryId(insightConfig, dao,
        mockClusterIdentificationService));

    // Initialize the telemetry ID again changing ports to have the same sorted concatenation without a separator, the
    // generated part should not change, but the derived part should change.
    setApplicationHttpConnectors(insightConfig, 8090, 70, 80, 8080);
    assertTelemetryIdDifferentDerived(generatedIdProperty, telemetryId, new TelemetryId(insightConfig, dao,
        mockClusterIdentificationService));

    // Initialize the telemetry ID again with https ports, the generated and derived parts should not change.
    setApplicationHttpsConnectors(insightConfig, 8090, 7080, 8080);
    assertTelemetryIdSame(generatedIdProperty, telemetryId, new TelemetryId(insightConfig, dao,
        mockClusterIdentificationService));
  }

  private void assertValidTelemetryId(SystemConfigurationProperty generatedIdProperty, String telemetryId) {
    assertThat(generatedIdProperty.getValue()).isNotNull();
    assertThat(telemetryId).matches(TELEMETRY_ID_PATTERN).startsWith(generatedIdProperty.getValue());
  }

  private void assertTelemetryIdSame(
      SystemConfigurationProperty generatedIdProperty,
      TelemetryId telemetryId,
      TelemetryId newTelemetryId)
  {
    final var telemetryIdValue = telemetryId.getId();
    final var newTelemetryIdValue = newTelemetryId.getId();

    SystemConfigurationProperty newGeneratedIdProperty = dao
        .getByName(TelemetryId.TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME);

    assertThat(newGeneratedIdProperty.getId()).isEqualTo(generatedIdProperty.getId());
    assertThat(newGeneratedIdProperty.getValue()).isEqualTo(generatedIdProperty.getValue());
    assertThat(telemetryIdValue).isEqualTo(newTelemetryIdValue);
  }

  private void assertTelemetryIdDifferentDerived(
      SystemConfigurationProperty generatedIdProperty,
      TelemetryId telemetryId,
      TelemetryId newTelemetryId)
  {
    var telemetryIdValue = telemetryId.getId();
    var newTelemetryIdValue = newTelemetryId.getId();

    SystemConfigurationProperty newGeneratedIdProperty = dao
        .getByName(TelemetryId.TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME);

    assertThat(newGeneratedIdProperty.getId()).isEqualTo(generatedIdProperty.getId());
    assertThat(newGeneratedIdProperty.getValue()).isEqualTo(generatedIdProperty.getValue());
    assertThat(newTelemetryIdValue.substring(0, 6)).isEqualTo(telemetryIdValue.substring(0, 6));
    assertThat(newTelemetryIdValue).isNotEqualTo(telemetryIdValue);
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
  public void testGetId() {
    // given: telemetry ID with configuration
    var insightConfig = new InsightConfig();
    setFirstApplicationConnectorPort(insightConfig, 1234);
    var telemetryId = new TelemetryId(insightConfig, dao, mockClusterIdentificationService);

    // when:
    final var actualId = telemetryId.getId();

    // then:
    final var generatedIdProperty = dao.getByName(TelemetryId.TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME);
    assertThat(actualId).matches(TELEMETRY_ID_PATTERN).startsWith(generatedIdProperty.getValue());
  }

  private void setFirstApplicationConnectorPort(InsightConfig insightConfig, int port) {
    ((HttpConnectorFactory) ((DefaultServerFactory) insightConfig.getServerFactory()).getApplicationConnectors().get(0))
        .setPort(port);
  }

  @Test
  public void testInitialize_ClusterIdIncludedForExternalDatabase() {
    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setDatabase(getSampleDatabaseConfig());
    TelemetryId telemetryId = new TelemetryId(insightConfig, dao, mockClusterIdentificationService);
    assertThat(telemetryId.getClusterId()).isEqualTo(getSampleDatabaseConfigFingerPrint());
  }

  @Test
  public void testInitialize_ClusterIdNullForEmbeddedDatabase() {
    InsightConfig insightConfig = new InsightConfig();
    TelemetryId telemetryId = new TelemetryId(insightConfig, dao, mockClusterIdentificationService);
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
