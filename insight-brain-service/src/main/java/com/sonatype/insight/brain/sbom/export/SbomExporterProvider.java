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
import com.sonatype.insight.brain.sbom.spdx.Spdx3VersionHandler;
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

  private final Provider<SpdxToSpdx3Exporter> spdxToSpdx3ExporterProvider;

  private final Provider<CycloneDxToSpdx3Exporter> cycloneDxToSpdx3ExporterProvider;

  private final Provider<Spdx3ToCycloneDxExporter> spdx3ToCycloneDxExporterProvider;

  private final Provider<Spdx3ToPdfExporter> spdx3ToPdfExporterProvider;

  @Inject
  public SbomExporterProvider(
      final Provider<CycloneDxToCycloneDxExporter> cycloneDxToCycloneDxExporterProvider,
      final Provider<SpdxToSpdxExporter> spdxToSpdxExporterProvider,
      final Provider<SpdxToCycloneDxExporter> spdxToCycloneDxExporterProvider,
      final Provider<CycloneDxToSpdxExporter> cycloneDxToSpdxExporterProvider,
      final Provider<CycloneDxToPdfExporter> cycloneDxToPdfExporterProvider,
      final Provider<SpdxToPdfExporter> spdxToPdfExporterProvider,
      final Provider<SpdxToSpdx3Exporter> spdxToSpdx3ExporterProvider,
      final Provider<CycloneDxToSpdx3Exporter> cycloneDxToSpdx3ExporterProvider,
      final Provider<Spdx3ToCycloneDxExporter> spdx3ToCycloneDxExporterProvider,
      final Provider<Spdx3ToPdfExporter> spdx3ToPdfExporterProvider)
  {
    this.cycloneDxToCycloneDxExporterProvider = cycloneDxToCycloneDxExporterProvider;
    this.spdxToSpdxExporterProvider = spdxToSpdxExporterProvider;
    this.cycloneDxToSpdxExporterProvider = cycloneDxToSpdxExporterProvider;
    this.spdxToCycloneDxExporterProvider = spdxToCycloneDxExporterProvider;
    this.cycloneDxToPdfExporterProvider = cycloneDxToPdfExporterProvider;
    this.spdxToPdfExporterProvider = spdxToPdfExporterProvider;
    this.spdxToSpdx3ExporterProvider = spdxToSpdx3ExporterProvider;
    this.cycloneDxToSpdx3ExporterProvider = cycloneDxToSpdx3ExporterProvider;
    this.spdx3ToCycloneDxExporterProvider = spdx3ToCycloneDxExporterProvider;
    this.spdx3ToPdfExporterProvider = spdx3ToPdfExporterProvider;
  }

  public SbomExporter get(SbomExportParams exportParams) {
    Objects.requireNonNull(exportParams);
    SbomSpecification inputSpec = SbomSpecification.fromValue(exportParams.sbomMetadata.getSpec());
    SbomSpecification outputSpec = exportParams.exportSpecification.getSpecification();
    String targetVersion = exportParams.exportSpecification.getVersion();
    String sourceVersion = exportParams.sbomMetadata.getSpecVersion();

    validateNoDowngrade(inputSpec, sourceVersion, outputSpec, targetVersion);

    AbstractSbomExporter exporter = provideInstance(inputSpec, sourceVersion, outputSpec, targetVersion);
    exporter.setExportParams(exportParams);
    return exporter;
  }

  private void validateNoDowngrade(
      SbomSpecification inputSpec,
      String sourceVersion,
      SbomSpecification outputSpec,
      String targetVersion)
  {
    if (SPDX.equals(inputSpec) && Spdx3VersionHandler.SPEC_VERSION.equals(sourceVersion)) {
      if (SPDX.equals(outputSpec) && targetVersion != null && !targetVersion.startsWith("3")) {
        throw new BadRequestException("Downgrading from SPDX 3.0 to SPDX " + targetVersion + " is not supported");
      }
    }
  }

  private AbstractSbomExporter provideInstance(
      final SbomSpecification inputSpec,
      final String sourceVersion,
      final SbomSpecification outputSpec,
      final String targetVersion)
  {
    if (CYCLONEDX.equals(inputSpec) && CYCLONEDX.equals(outputSpec)) {
      return cycloneDxToCycloneDxExporterProvider.get();
    }

    if (SPDX.equals(inputSpec) && SPDX.equals(outputSpec)) {
      if (Spdx3VersionHandler.SPEC_VERSION.equals(targetVersion)) {
        return spdxToSpdx3ExporterProvider.get();
      }
      return spdxToSpdxExporterProvider.get();
    }

    if (CYCLONEDX.equals(inputSpec) && SPDX.equals(outputSpec)) {
      if (Spdx3VersionHandler.SPEC_VERSION.equals(targetVersion)) {
        return cycloneDxToSpdx3ExporterProvider.get();
      }
      return cycloneDxToSpdxExporterProvider.get();
    }

    if (SPDX.equals(inputSpec) && CYCLONEDX.equals(outputSpec)) {
      if (Spdx3VersionHandler.SPEC_VERSION.equals(sourceVersion)) {
        return spdx3ToCycloneDxExporterProvider.get();
      }
      return spdxToCycloneDxExporterProvider.get();
    }

    if (CYCLONEDX.equals(inputSpec) && outputSpec == null) {
      return cycloneDxToPdfExporterProvider.get();
    }

    if (SPDX.equals(inputSpec) && outputSpec == null) {
      if (Spdx3VersionHandler.SPEC_VERSION.equals(sourceVersion)) {
        return spdx3ToPdfExporterProvider.get();
      }
      return spdxToPdfExporterProvider.get();
    }

    throw new BadRequestException(
        String.format("Exporting from existing %s sbom to a %s specification is not supported", inputSpec, outputSpec));
  }
}
