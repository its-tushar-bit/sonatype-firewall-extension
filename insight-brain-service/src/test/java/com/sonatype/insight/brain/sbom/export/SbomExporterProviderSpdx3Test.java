/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportSpecification;
import com.sonatype.insight.error.exception.BadRequestException;

import jakarta.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SbomExporterProviderSpdx3Test
{
  private SbomExporterProvider exporterProvider;

  private Provider<SpdxToSpdx3Exporter> spdxToSpdx3ExporterProvider;

  private Provider<CycloneDxToSpdx3Exporter> cycloneDxToSpdx3ExporterProvider;

  private Provider<Spdx3ToCycloneDxExporter> spdx3ToCycloneDxExporterProvider;

  private Provider<Spdx3ToPdfExporter> spdx3ToPdfExporterProvider;

  @SuppressWarnings("unchecked")
  @BeforeEach
  public void setUp() {
    Provider<CycloneDxToCycloneDxExporter> cdxToCdx = mock(Provider.class);
    Provider<SpdxToSpdxExporter> spdxToSpdx = mock(Provider.class);
    Provider<SpdxToCycloneDxExporter> spdxToCdx = mock(Provider.class);
    Provider<CycloneDxToSpdxExporter> cdxToSpdx = mock(Provider.class);
    Provider<CycloneDxToPdfExporter> cdxToPdf = mock(Provider.class);
    Provider<SpdxToPdfExporter> spdxToPdf = mock(Provider.class);
    spdxToSpdx3ExporterProvider = mock(Provider.class);
    cycloneDxToSpdx3ExporterProvider = mock(Provider.class);
    spdx3ToCycloneDxExporterProvider = mock(Provider.class);
    spdx3ToPdfExporterProvider = mock(Provider.class);

    SpdxToSpdx3Exporter mockSpdxToSpdx3 = mock(SpdxToSpdx3Exporter.class);
    CycloneDxToSpdx3Exporter mockCdxToSpdx3 = mock(CycloneDxToSpdx3Exporter.class);
    Spdx3ToCycloneDxExporter mockSpdx3ToCdx = mock(Spdx3ToCycloneDxExporter.class);
    Spdx3ToPdfExporter mockSpdx3ToPdf = mock(Spdx3ToPdfExporter.class);
    when(spdxToSpdx3ExporterProvider.get()).thenReturn(mockSpdxToSpdx3);
    when(cycloneDxToSpdx3ExporterProvider.get()).thenReturn(mockCdxToSpdx3);
    when(spdx3ToCycloneDxExporterProvider.get()).thenReturn(mockSpdx3ToCdx);
    when(spdx3ToPdfExporterProvider.get()).thenReturn(mockSpdx3ToPdf);

    SpdxToSpdxExporter mockSpdxToSpdx = mock(SpdxToSpdxExporter.class);
    when(spdxToSpdx.get()).thenReturn(mockSpdxToSpdx);

    CycloneDxToSpdxExporter mockCdxToSpdx = mock(CycloneDxToSpdxExporter.class);
    when(cdxToSpdx.get()).thenReturn(mockCdxToSpdx);

    exporterProvider = new SbomExporterProvider(cdxToCdx, spdxToSpdx, spdxToCdx, cdxToSpdx, cdxToPdf, spdxToPdf,
        spdxToSpdx3ExporterProvider, cycloneDxToSpdx3ExporterProvider, spdx3ToCycloneDxExporterProvider,
        spdx3ToPdfExporterProvider);
  }

  @Test
  public void get_spdxToSpdx30_routesToSpdx3Exporter() {
    SbomExportParams params = createParams("SPDX", "2.3", ExportSpecification.SPDX_30);
    SbomExporter exporter = exporterProvider.get(params);
    assertNotNull(exporter);
    assertTrue(exporter instanceof SpdxToSpdx3Exporter);
  }

  @Test
  public void get_cycloneDxToSpdx30_routesToCdxSpdx3Exporter() {
    SbomExportParams params = createParams("CycloneDx", "1.6", ExportSpecification.SPDX_30);
    SbomExporter exporter = exporterProvider.get(params);
    assertNotNull(exporter);
    assertTrue(exporter instanceof CycloneDxToSpdx3Exporter);
  }

  @Test
  public void get_spdxToSpdx23_routesToLegacyExporter() {
    SbomExportParams params = createParams("SPDX", "2.2", ExportSpecification.SPDX_23);
    SbomExporter exporter = exporterProvider.get(params);
    assertNotNull(exporter);
    assertTrue(exporter instanceof SpdxToSpdxExporter);
  }

  @Test
  public void get_cycloneDxToSpdx23_routesToLegacyExporter() {
    SbomExportParams params = createParams("CycloneDx", "1.6", ExportSpecification.SPDX_23);
    SbomExporter exporter = exporterProvider.get(params);
    assertNotNull(exporter);
    assertTrue(exporter instanceof CycloneDxToSpdxExporter);
  }

  @Test
  public void get_spdx30ToSpdx23_throwsDowngradeError() {
    SbomExportParams params = createParams("SPDX", "3.0", ExportSpecification.SPDX_23);
    assertThrows(BadRequestException.class, () -> exporterProvider.get(params));
  }

  @Test
  public void get_spdx30ToCycloneDx_routesToSpdx3CdxExporter() {
    SbomExportParams params = createParams("SPDX", "3.0", ExportSpecification.CYCLONEDX_16);
    SbomExporter exporter = exporterProvider.get(params);
    assertNotNull(exporter);
    assertTrue(exporter instanceof Spdx3ToCycloneDxExporter);
  }

  @Test
  public void get_spdx30ToPdf_routesToSpdx3PdfExporter() {
    SbomExportParams params = createParams("SPDX", "3.0", ExportSpecification.PDF);
    SbomExporter exporter = exporterProvider.get(params);
    assertNotNull(exporter);
    assertTrue(exporter instanceof Spdx3ToPdfExporter);
  }

  private SbomExportParams createParams(String spec, String specVersion, ExportSpecification exportSpec) {
    ThirdPartySbomMetadata metadata = new ThirdPartySbomMetadata();
    metadata.setSpec(spec);
    metadata.setSpecVersion(specVersion);
    metadata.setSpecFormat("json");
    SbomExportParams params = SbomExportParams.newSbomExporterParams(metadata);
    params.withExportSpecification(exportSpec);
    return params;
  }
}
