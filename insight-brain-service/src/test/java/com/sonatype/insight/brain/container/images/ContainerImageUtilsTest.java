/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.container.images;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.container.image.ContainerImageTelemetryMetrics;

import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Property;
import org.junit.jupiter.api.Test;

import static com.sonatype.clm.dto.model.container.image.ContainerImageTelemetryMetrics.BASE_OS_PROPERTY_NAME;
import static com.sonatype.clm.dto.model.container.image.ContainerImageTelemetryMetrics.COMPONENTS_COUNT_PROPERTY_NAME;
import static com.sonatype.clm.dto.model.container.image.ContainerImageTelemetryMetrics.MANIFEST_TYPE_PROPERTY_NAME;
import static com.sonatype.clm.dto.model.container.image.ContainerImageTelemetryMetrics.POLICY_EVALUATION_DURATION_MILLISECONDS_PROPERTY_NAME;
import static com.sonatype.clm.dto.model.container.image.ContainerImageTelemetryMetrics.SCAN_DURATION_MILLISECONDS_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class ContainerImageUtilsTest
{
  @Test
  public void testBuildContainerImageTelemetryMetrics_withAllValidProperties() {
    Metadata metadata = new Metadata();
    List<Property> properties = new ArrayList<>();
    properties.add(createProperty(BASE_OS_PROPERTY_NAME, "alpine:3.18"));
    properties.add(createProperty(COMPONENTS_COUNT_PROPERTY_NAME, "150"));
    properties.add(createProperty(MANIFEST_TYPE_PROPERTY_NAME, "test-manifest-type"));
    properties.add(createProperty(SCAN_DURATION_MILLISECONDS_PROPERTY_NAME, "5000"));
    properties.add(createProperty(POLICY_EVALUATION_DURATION_MILLISECONDS_PROPERTY_NAME, "2500"));
    metadata.setProperties(properties);

    ContainerImageTelemetryMetrics result = ContainerImageUtils.buildContainerImageTelemetryMetrics(metadata);

    assertThat(result).isNotNull();
    assertThat(result.getBaseOs()).isEqualTo("alpine:3.18");
    assertThat(result.getComponentsCount()).isEqualTo(150L);
    assertThat(result.getManifestMediaType()).isEqualTo("test-manifest-type");
    assertThat(result.getScanDurationMilliseconds()).isEqualTo(5000L);
    assertThat(result.getPolicyEvaluationDurationMilliseconds()).isEqualTo(2500L);
  }

  @Test
  public void testBuildContainerImageTelemetryMetrics_withNullMetadata() {
    ContainerImageTelemetryMetrics result = ContainerImageUtils.buildContainerImageTelemetryMetrics(null);

    assertThat(result).isNotNull();
    assertThat(result.getBaseOs()).isNull();
    assertThat(result.getComponentsCount()).isNull();
    assertThat(result.getManifestMediaType()).isNull();
    assertThat(result.getScanDurationMilliseconds()).isNull();
    assertThat(result.getPolicyEvaluationDurationMilliseconds()).isNull();
  }

  @Test
  public void testBuildContainerImageTelemetryMetrics_withNullProperties() {
    Metadata metadata = new Metadata();
    metadata.setProperties(null);

    ContainerImageTelemetryMetrics result = ContainerImageUtils.buildContainerImageTelemetryMetrics(metadata);

    assertThat(result).isNotNull();
    assertThat(result.getBaseOs()).isNull();
    assertThat(result.getComponentsCount()).isNull();
    assertThat(result.getManifestMediaType()).isNull();
    assertThat(result.getScanDurationMilliseconds()).isNull();
    assertThat(result.getPolicyEvaluationDurationMilliseconds()).isNull();
  }

  @Test
  public void testBuildContainerImageTelemetryMetrics_withEmptyProperties() {
    Metadata metadata = new Metadata();
    metadata.setProperties(new ArrayList<>());

    ContainerImageTelemetryMetrics result = ContainerImageUtils.buildContainerImageTelemetryMetrics(metadata);

    assertThat(result).isNotNull();
    assertThat(result.getBaseOs()).isNull();
    assertThat(result.getComponentsCount()).isNull();
    assertThat(result.getManifestMediaType()).isNull();
    assertThat(result.getScanDurationMilliseconds()).isNull();
    assertThat(result.getPolicyEvaluationDurationMilliseconds()).isNull();
  }

  @Test
  public void testBuildContainerImageTelemetryMetrics_withUnknownProperty() {
    Metadata metadata = new Metadata();
    List<Property> properties = new ArrayList<>();
    properties.add(createProperty("unknown.property", "someValue"));
    properties.add(createProperty(BASE_OS_PROPERTY_NAME, "ubuntu:22.04"));
    metadata.setProperties(properties);

    ContainerImageTelemetryMetrics result = ContainerImageUtils.buildContainerImageTelemetryMetrics(metadata);

    assertThat(result).isNotNull();
    assertThat(result.getBaseOs()).isEqualTo("ubuntu:22.04");
  }

  @Test
  public void testBuildContainerImageTelemetryMetrics_withInvalidNumericValue() {
    Metadata metadata = new Metadata();
    List<Property> properties = new ArrayList<>();
    properties.add(createProperty(COMPONENTS_COUNT_PROPERTY_NAME, "not-a-number"));
    properties.add(createProperty(BASE_OS_PROPERTY_NAME, "debian:11"));
    metadata.setProperties(properties);

    ContainerImageTelemetryMetrics result = ContainerImageUtils.buildContainerImageTelemetryMetrics(metadata);

    // Invalid numeric value should return null, but other properties should be processed
    assertThat(result).isNotNull();
    assertThat(result.getComponentsCount()).isNull();
    assertThat(result.getBaseOs()).isEqualTo("debian:11");
  }

  @Test
  public void testBuildContainerImageTelemetryMetrics_withNumericValueWithWhitespace() {
    Metadata metadata = new Metadata();
    List<Property> properties = new ArrayList<>();
    properties.add(createProperty(COMPONENTS_COUNT_PROPERTY_NAME, " 123 "));
    metadata.setProperties(properties);

    ContainerImageTelemetryMetrics result = ContainerImageUtils.buildContainerImageTelemetryMetrics(metadata);

    assertThat(result).isNotNull();
    assertThat(result.getComponentsCount()).isEqualTo(123L);
  }

  private Property createProperty(String name, String value) {
    Property property = new Property();
    property.setName(name);
    property.setValue(value);
    return property;
  }
}
