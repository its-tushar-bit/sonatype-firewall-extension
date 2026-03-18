/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.container.images;

import com.sonatype.clm.dto.model.container.image.ContainerImageTelemetryMetrics;

import org.apache.commons.lang3.StringUtils;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.clm.dto.model.container.image.ContainerImageTelemetryMetrics.BASE_OS_PROPERTY_NAME;
import static com.sonatype.clm.dto.model.container.image.ContainerImageTelemetryMetrics.COMPONENTS_COUNT_PROPERTY_NAME;
import static com.sonatype.clm.dto.model.container.image.ContainerImageTelemetryMetrics.MANIFEST_TYPE_PROPERTY_NAME;
import static com.sonatype.clm.dto.model.container.image.ContainerImageTelemetryMetrics.POLICY_EVALUATION_DURATION_MILLISECONDS_PROPERTY_NAME;
import static com.sonatype.clm.dto.model.container.image.ContainerImageTelemetryMetrics.SCAN_DURATION_MILLISECONDS_PROPERTY_NAME;

public class ContainerImageUtils
{
  private static final Logger log = LoggerFactory.getLogger(ContainerImageUtils.class);

  private ContainerImageUtils() {
  }

  public static ContainerImageTelemetryMetrics buildContainerImageTelemetryMetrics(Metadata metadata) {
    ContainerImageTelemetryMetrics telemetryMetrics = new ContainerImageTelemetryMetrics();

    if (metadata != null && metadata.getProperties() != null) {
      for (Property property : metadata.getProperties()) {
        String name = property.getName();
        String value = property.getValue();

        switch (name) {
          case BASE_OS_PROPERTY_NAME -> telemetryMetrics.setBaseOs(value);
          case COMPONENTS_COUNT_PROPERTY_NAME -> telemetryMetrics.setComponentsCount(stringToLong(value));
          case MANIFEST_TYPE_PROPERTY_NAME -> telemetryMetrics.setManifestMediaType(value);
          case SCAN_DURATION_MILLISECONDS_PROPERTY_NAME -> telemetryMetrics
              .setScanDurationMilliseconds(stringToLong(value));
          case POLICY_EVALUATION_DURATION_MILLISECONDS_PROPERTY_NAME -> telemetryMetrics
              .setPolicyEvaluationDurationMilliseconds(stringToLong(value));
          default -> log.warn("Unknown property {} in CycloneDx metadata with value {}", name, value);
        }
      }
    }

    return telemetryMetrics;
  }

  private static Long stringToLong(final String value) {
    if (StringUtils.isBlank(value)) {
      return null;
    }
    try {
      return Long.parseLong(value.trim());
    }
    catch (NumberFormatException e) {
      log.warn("Unable to parse value {} to long", value);
      return null;
    }
  }
}
