/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.util.Objects;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.error.exception.BadRequestException;

import static com.sonatype.insight.brain.sbom.SbomSpecification.CYCLONEDX;
import static com.sonatype.insight.brain.sbom.SbomSpecification.SPDX;

@Named
@Singleton
public class SbomExporterProvider
{
  private final Provider<CycloneDxToCycloneDxExporter> cycloneDxToCycloneDxExporterProvider;

  private final Provider<SpdxToSpdxExporter> spdxToSpdxExporterProvider;

  private final Provider<CycloneDxToSpdxExporter> cycloneDxToSpdxExporterProvider;

  private final Provider<SpdxToCycloneDxExporter> spdxToCycloneDxExporterProvider;

  private final Provider<CycloneDxToPdfExporter> cycloneDxToPdfExporterProvider;

  private final Provider<SpdxToPdfExporter> spdxToPdfExporterProvider;

  @Inject
  public SbomExporterProvider(
      final Provider<CycloneDxToCycloneDxExporter> cycloneDxToCycloneDxExporterProvider,
      final Provider<SpdxToSpdxExporter> spdxToSpdxExporterProvider,
      final Provider<SpdxToCycloneDxExporter> spdxToCycloneDxExporterProvider,
      final Provider<CycloneDxToSpdxExporter> cycloneDxToSpdxExporterProvider,
      final Provider<CycloneDxToPdfExporter> cycloneDxToPdfExporterProvider,
      final Provider<SpdxToPdfExporter> spdxToPdfExporterProvider)
  {
    this.cycloneDxToCycloneDxExporterProvider = cycloneDxToCycloneDxExporterProvider;
    this.spdxToSpdxExporterProvider = spdxToSpdxExporterProvider;
    this.cycloneDxToSpdxExporterProvider = cycloneDxToSpdxExporterProvider;
    this.spdxToCycloneDxExporterProvider = spdxToCycloneDxExporterProvider;
    this.cycloneDxToPdfExporterProvider = cycloneDxToPdfExporterProvider;
    this.spdxToPdfExporterProvider = spdxToPdfExporterProvider;
  }

  public SbomExporter get(SbomExportParams exportParams) {
    Objects.requireNonNull(exportParams);
    SbomSpecification inputSpec = SbomSpecification.fromValue(exportParams.sbomMetadata.getSpec());
    SbomSpecification outputSpec = exportParams.exportSpecification.getSpecification();
    AbstractSbomExporter exporter = provideInstance(inputSpec, outputSpec);
    exporter.setExportParams(exportParams);
    return exporter;
  }

  private AbstractSbomExporter provideInstance(final SbomSpecification inputSpec, final SbomSpecification outputSpec) {
    if (CYCLONEDX.equals(inputSpec) && CYCLONEDX.equals(outputSpec)) {
      return cycloneDxToCycloneDxExporterProvider.get();
    }

    if (SPDX.equals(inputSpec) && SPDX.equals(outputSpec)) {
      return spdxToSpdxExporterProvider.get();
    }

    if (CYCLONEDX.equals(inputSpec) && SPDX.equals(outputSpec)) {
      return cycloneDxToSpdxExporterProvider.get();
    }

    if (SPDX.equals(inputSpec) && CYCLONEDX.equals(outputSpec)) {
      return spdxToCycloneDxExporterProvider.get();
    }

    if (CYCLONEDX.equals(inputSpec) && outputSpec == null) {
      return cycloneDxToPdfExporterProvider.get();
    }

    if (SPDX.equals(inputSpec) && outputSpec == null) {
      return spdxToPdfExporterProvider.get();
    }

    throw new BadRequestException(
        String.format("Exporting from existing %s sbom to a %s specification is not supported", inputSpec, outputSpec));
  }
}
