/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.InsightConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

public class TelemetryIdTest
{
  private SystemConfigurationPropertyDAO dao = new SystemConfigurationPropertyDAO();

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
    insightConfig.getHttpConfiguration().setPort(port);

    // Initialize the telemetry ID, the generated and derived parts must be calculated.
    TelemetryId telemetryId = new TelemetryId(insightConfig);
    SystemConfigurationProperty generatedIdProperty1 = dao
        .getByName(TelemetryId.TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME);
    assertThat(generatedIdProperty1.getValue(), is(notNullValue()));
    String telemetryId1 = telemetryId.getId();
    assertThat(telemetryId1, startsWith(generatedIdProperty1.getValue() + "-"));
    assertThat(telemetryId1.length(), is(11));

    // Initialize the telemetry ID again, the generated and derived parts should not change.
    telemetryId = new TelemetryId(insightConfig);
    SystemConfigurationProperty generatedIdProperty2 = dao
        .getByName(TelemetryId.TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME);
    assertThat(generatedIdProperty2.getId(), is(generatedIdProperty1.getId()));
    assertThat(generatedIdProperty2.getValue(), is(generatedIdProperty1.getValue()));
    String telemetryId2 = telemetryId.getId();
    assertThat(telemetryId2, is(telemetryId1));

    // Initialize the telemetry ID again using a different port, the generated part should not change, but the derived
    // part should change.
    insightConfig.getHttpConfiguration().setPort(port + 1);
    telemetryId = new TelemetryId(insightConfig);
    SystemConfigurationProperty generatedIdProperty3 = dao
        .getByName(TelemetryId.TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME);
    assertThat(generatedIdProperty3.getId(), is(generatedIdProperty2.getId()));
    assertThat(generatedIdProperty3.getValue(), is(generatedIdProperty2.getValue()));
    String telemetryId3 = telemetryId.getId();
    assertThat(telemetryId3.substring(0, 6), is(telemetryId2.substring(0, 6)));
    assertThat(telemetryId3, is(not(telemetryId2)));
  }

  @Test
  public void testCalculateDerivedId() throws Exception {
    List<byte[]> hardwareAddresses = new ArrayList<>();
    hardwareAddresses.add("123456789ABC".getBytes("UTF-8"));
    assertThat(TelemetryId.calculateDerivedId("somehost", 7788, hardwareAddresses), is("e7c7e"));

    assertThat(TelemetryId.calculateDerivedId("otherhost", 7788, hardwareAddresses), is("9ee29"));

    assertThat(TelemetryId.calculateDerivedId("somehost", 8899, hardwareAddresses), is("17868"));

    hardwareAddresses.add("123456789DEF".getBytes("UTF-8"));
    assertThat(TelemetryId.calculateDerivedId("somehost", 7788, hardwareAddresses), is("e1380"));
  }

  @Test
  public void testGetId() {
    InsightConfig insightConfig = new InsightConfig();
    insightConfig.getHttpConfiguration().setPort(1234);
    TelemetryId telemetryId = new TelemetryId(insightConfig);
    SystemConfigurationProperty generatedIdProperty = dao
        .getByName(TelemetryId.TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME);
    assertThat(telemetryId.getId(), startsWith(generatedIdProperty.getValue() + "-"));
  }
}
