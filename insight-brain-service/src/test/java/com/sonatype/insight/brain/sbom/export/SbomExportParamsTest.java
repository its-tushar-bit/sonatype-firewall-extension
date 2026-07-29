/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportSpecification;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SbomExportParamsTest
{
  @Test
  public void testGetLatestVersionForSbomSpecification() {
    assertThat(ExportSpecification.values()).containsExactlyInAnyOrder(
        ExportSpecification.DEFAULT,
        ExportSpecification.CYCLONEDX_17,
        ExportSpecification.CYCLONEDX_16,
        ExportSpecification.CYCLONEDX_15,
        ExportSpecification.SPDX_22,
        ExportSpecification.SPDX_23,
        ExportSpecification.SPDX_30,
        ExportSpecification.PDF);
    ExportSpecification result;

    result = ExportSpecification.getLatestVersionForSbomSpecification(SbomSpecification.CYCLONEDX);
    assertThat(result.getSpecification()).isEqualTo(SbomSpecification.CYCLONEDX);
    assertThat(result.getVersion()).isEqualTo("1.7");

    result = ExportSpecification.getLatestVersionForSbomSpecification(SbomSpecification.SPDX);
    assertThat(result.getSpecification()).isEqualTo(SbomSpecification.SPDX);
    assertThat(result.getVersion()).isEqualTo("3.0");
  }
}
