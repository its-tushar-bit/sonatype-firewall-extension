/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.sonatype.insight.brain.sbom.SbomSpecification;

import static com.sonatype.insight.brain.sbom.SbomSpecification.CYCLONEDX;
import static com.sonatype.insight.brain.sbom.SbomSpecification.SPDX;

@Named
@Singleton
public class SbomExporterProvider
{
  private final Provider<CycloneDxToCycloneDxExporter> cycloneDxToCycloneDxExporterProvider;

  private final Provider<SpdxToSpdxExporter> spdxToSpdxExporterProvider;

  @Inject
  public SbomExporterProvider(
      final Provider<CycloneDxToCycloneDxExporter> cycloneDxToCycloneDxExporterProvider,
      final Provider<SpdxToSpdxExporter> spdxToSpdxExporterProvider)
  {
    this.cycloneDxToCycloneDxExporterProvider = cycloneDxToCycloneDxExporterProvider;
    this.spdxToSpdxExporterProvider = spdxToSpdxExporterProvider;
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

    //TODO other providers
    return null;
  }
}
