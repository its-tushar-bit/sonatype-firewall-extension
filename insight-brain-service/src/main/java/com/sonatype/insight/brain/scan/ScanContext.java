/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import com.sonatype.clm.dto.model.container.image.ContainerImageTelemetryMetrics;
import com.sonatype.insight.brain.sbom.SbomSpecification;

/**
 * This class is intended to allow us to pass any information along for a scan without having to add extra method
 * parameters everywhere. It is similar to {@link com.sonatype.insight.brain.thirdparty.ThirdPartyScanContext} but is
 * intended to be usable for any scan.
 */
public record ScanContext(
    String applicationVersion,
    boolean isValid,
    String sbomMetadataId,
    SbomSpecification containerImageSbomSpecification,
    ContainerImageTelemetryMetrics containerImageTelemetryMetrics)
{
  /**
   * This builder is intended to make constructing a {@link ScanContext} easier by not having to set all fields.
   */
  public static class Builder
  {
    private String applicationVersion;

    private String sbomMetadataId;

    private boolean isValid;

    private SbomSpecification containerImageSbomSpecification;

    private ContainerImageTelemetryMetrics containerImageTelemetryMetrics;

    public Builder applicationVersion(final String applicationVersion) {
      this.applicationVersion = applicationVersion;
      return this;
    }

    public Builder sbomMetadataId(final String sbomMetadataId) {
      this.sbomMetadataId = sbomMetadataId;
      return this;
    }

    public Builder isValid(final boolean isValid) {
      this.isValid = isValid;
      return this;
    }

    public Builder containerImageSbomSpecification(final SbomSpecification containerImageSbomSpecification) {
      this.containerImageSbomSpecification = containerImageSbomSpecification;
      return this;
    }

    public Builder containerImageTelemetryMetrics(final ContainerImageTelemetryMetrics containerImageTelemetryMetrics) {
      this.containerImageTelemetryMetrics = containerImageTelemetryMetrics;
      return this;
    }

    public ScanContext build() {
      return new ScanContext(applicationVersion, isValid, sbomMetadataId, containerImageSbomSpecification,
          containerImageTelemetryMetrics);
    }
  }
}
