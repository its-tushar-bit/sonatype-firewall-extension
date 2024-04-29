/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.scan.file.SbomFormat;

import static com.sonatype.insight.brain.sbom.SbomSpecification.CYCLONEDX;
import static com.sonatype.insight.brain.sbom.SbomSpecification.SPDX;

public class SbomExportParams
{
  public enum ExportOption
  {
    ANNOTATED_VULNERABILITIES_ONLY, NO_VULNERABILITIES
  }

  public enum ExportSpecification
  {
    DEFAULT("1.5", CYCLONEDX), CYCLONEDX_15("1.5", CYCLONEDX), SPDX_23("2.3", SPDX);

    private final String version;

    private final SbomSpecification specification;

    ExportSpecification(final String version, SbomSpecification specification) {
      this.version = version;
      this.specification = specification;
    }

    public String getVersion() {
      return version;
    }

    public SbomSpecification getSpecification() {
      return specification;
    }
  }

  final ThirdPartySbomMetadata sbomMetadata;

  SbomFormat targetFormat = SbomFormat.XML;

  Set<ExportOption> sbomExportOptions = new HashSet<>();

  ExportSpecification exportSpecification = ExportSpecification.DEFAULT;

  private SbomExportParams(final ThirdPartySbomMetadata sbomMetadata) {
    this.sbomMetadata = sbomMetadata;
  }

  public static SbomExportParams newSbomExporterParams(ThirdPartySbomMetadata sbomMetadata) {
    Objects.requireNonNull(sbomMetadata);
    return new SbomExportParams(sbomMetadata);
  }

  public SbomExportParams withExportOptions(ExportOption... options) {
    if (options != null) {
      this.sbomExportOptions.addAll(Arrays.asList(options));
    }
    return this;
  }

  public SbomExportParams withExportSpecification(ExportSpecification exportSpecification) {
    this.exportSpecification = exportSpecification;
    return this;
  }

  public SbomExportParams withTargetFormat(SbomFormat sbomFormat) {
    this.targetFormat = sbomFormat;
    return this;
  }
}
