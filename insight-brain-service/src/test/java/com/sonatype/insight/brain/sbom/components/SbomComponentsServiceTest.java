/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.components;

import java.util.Arrays;
import javax.inject.Inject;

import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SbomComponentsServiceTest
    extends AbstractComponentTest
{
  @Inject
  private SbomComponentsService service;

  @Test
  @PostgresTest
  public void testGetSbomMetadataSuccessful() {
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .withSpecVersion("1.5")
        .withThirdPartyFileId(thirdPartyScan.getThirdPartyFileId())
        .build();
    BomPageMetadataDTO resultDto = service.getBomPageMetadata(application.getId(), sbomMetadata.getSbomVersion());
    assertThat(resultDto.fileFormat).isEqualTo(sbomMetadata.getSpecFormat());
    assertThat(resultDto.specification).isEqualTo(sbomMetadata.getSpec());
    assertThat(resultDto.specVersion).isEqualTo(sbomMetadata.getSpecVersion());
    assertThat(resultDto.author).isEqualTo(Arrays.asList("John Doe"));
    assertThat(resultDto.supplier).isEqualTo(Arrays.asList("John Doe", "Jane Doe"));
    assertThat(resultDto.manufacturer).isEqualTo(Arrays.asList("John Doe", "Jane Doe"));
    assertThat(resultDto.scanId).isEqualTo(thirdPartyScan.getScanId());

    // Test SPDX Format
    ThirdPartySbomMetadata sbomSPDXMetadata = SbomMetadataBuilder.newSbomSPDXMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .withSpecVersion("1.5")
        .withThirdPartyFileId(thirdPartyScan.getThirdPartyFileId())
        .build();
    BomPageMetadataDTO spdxResultDto = service.getBomPageMetadata(
        application.getId(), sbomSPDXMetadata.getSbomVersion()
    );
    assertThat(spdxResultDto.person).isEqualTo(Arrays.asList("John Doe", "Jane Doe"));
    assertThat(spdxResultDto.organization).isEqualTo(Arrays.asList("Example Organization"));
    assertThat(spdxResultDto.fileFormat).isEqualTo(sbomSPDXMetadata.getSpecFormat());
    assertThat(spdxResultDto.specification).isEqualTo(sbomSPDXMetadata.getSpec());
    assertThat(spdxResultDto.specVersion).isEqualTo(sbomSPDXMetadata.getSpecVersion());
  }
}
