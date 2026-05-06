/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.sonatype.insight.brain.api.v2.dto.ApiReportPolicyDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.scan.file.SbomFormat;

import com.google.common.collect.ImmutableMap;
import org.apache.maven.artifact.versioning.ComparableVersion;

import static com.sonatype.insight.brain.sbom.SbomSpecification.CYCLONEDX;
import static com.sonatype.insight.brain.sbom.SbomSpecification.SPDX;

public class SbomExportParams
{
  public enum ExportOption
  {
    ANNOTATED_VULNERABILITIES_ONLY,
    NO_VULNERABILITIES
  }

  public enum ExportSpecification
  {
    DEFAULT("1.6", CYCLONEDX),
    CYCLONEDX_16("1.6", CYCLONEDX),
    CYCLONEDX_15("1.5", CYCLONEDX),
    SPDX_22("2.2", SPDX),
    SPDX_23("2.3", SPDX),
    PDF("pdf", null);

    private static final Map<String, ExportSpecification> SUPPORTED_EXPORT_SPECIFICATIONS =
        ImmutableMap.of("cyclonedx1.6", CYCLONEDX_16, "cyclonedx1.5", CYCLONEDX_15, "spdx2.2", SPDX_22, "spdx2.3",
            SPDX_23);

    private final String version;

    private final ComparableVersion comparableVersion;

    private final SbomSpecification specification;

    ExportSpecification(final String version, SbomSpecification specification) {
      this.version = version;
      this.comparableVersion = new ComparableVersion(version);
      this.specification = specification;
    }

    public String getVersion() {
      return version;
    }

    public ComparableVersion getComparableVersion() {
      return comparableVersion;
    }

    public SbomSpecification getSpecification() {
      return specification;
    }

    public static ExportSpecification getSpecificationForRequest(String requestSpecification) {
      return SUPPORTED_EXPORT_SPECIFICATIONS.get(requestSpecification);
    }

    public static ExportSpecification getLatestVersionForSbomSpecification(final SbomSpecification sbomSpecification) {
      return Arrays.stream(values())
          .filter(e -> sbomSpecification == e.specification)
          .max(Comparator.comparing(ExportSpecification::getComparableVersion))
          .orElse(null);
    }
  }

  final ThirdPartySbomMetadata sbomMetadata;

  SbomFormat targetFormat = SbomFormat.XML;

  Set<ExportOption> sbomExportOptions = new HashSet<>();

  ExportSpecification exportSpecification = ExportSpecification.DEFAULT;

  ApiReportRawDataDTOV2 reportRawData = null;

  ApiReportPolicyDataDTOV2 policyData = null;

  private SbomExportParams(final ThirdPartySbomMetadata sbomMetadata) {
    this.sbomMetadata = sbomMetadata;
  }

  public ExportSpecification getExportSpecification() {
    return exportSpecification;
  }

  public SbomFormat getTargetFormat() {
    return targetFormat;
  }

  public ApiReportRawDataDTOV2 getReportRawData() {
    return reportRawData;
  }

  public ApiReportPolicyDataDTOV2 getPolicyData() {
    return policyData;
  }

  public static SbomExportParams newSbomExporterParams(ThirdPartySbomMetadata sbomMetadata) {
    Objects.requireNonNull(sbomMetadata);
    return new SbomExportParams(sbomMetadata);
  }

  public SbomExportParams withReportRawData(ApiReportRawDataDTOV2 reportRawData) {
    this.reportRawData = reportRawData;
    return this;
  }

  public SbomExportParams withPolicyData(ApiReportPolicyDataDTOV2 policyData) {
    this.policyData = policyData;
    return this;
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
