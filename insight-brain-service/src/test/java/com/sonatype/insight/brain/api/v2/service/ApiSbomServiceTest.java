/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.nio.file.Path;
import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.SbomTestsHelper;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiSbomServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiSbomService service;

  @Inject
  private ThirdPartySbomMetadataDAO dao;

  @Inject
  private InsightWork insightWork;

  @Test
  public void testDeleteSbomVersion() throws IOException {
    Application app = tempEntity.newApplicationWithParent();
    Path fileInWorkDirPath =
        SbomTestsHelper.createTestFileForSbomMetadata(insightWork.getSbomDir(app.getId()),
            getClass().getResource("/" + getClass().getSimpleName() + "/third-party-simple-bom.xml"));
    final ThirdPartySbomMetadata sbomMetadata =
        tempEntity.createSbomMetadata(app.getId(), null, fileInWorkDirPath.getFileName().toString());

    service.deleteSbomVersion(sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion());

    final ThirdPartySbomMetadata retrievedSbomMetadata =
        dao.getByApplicationIdAndSbomVersion(sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion());

    assertThat(retrievedSbomMetadata).isNull();
    assertThat(fileInWorkDirPath).doesNotExist();
  }

  @Test
  public void testDeleteSbomVersion_NotFoundInvalidVersion() {
    ThirdPartySbomMetadata sbomMetadata = tempEntity.createSbomMetadata();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.deleteSbomVersion(sbomMetadata.getApplicationId(), "invalidVersion"))
        .withMessage(
            "Cannot find version invalidVersion for application with ID " + sbomMetadata.getApplicationId() + ".");
  }
}
