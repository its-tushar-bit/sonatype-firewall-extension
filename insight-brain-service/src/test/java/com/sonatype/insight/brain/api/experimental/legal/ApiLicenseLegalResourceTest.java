/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightWork;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiLicenseLegalResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.LICENSE_LEGAL_RESOURCE_PATH);
  }

  @Test
  public void testGetLicenseLegalApplicationReport() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, tempEntity.uuid());
    mockReport(policyEvaluation);
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);

    HttpResponse response =
        restRequest().path(ApiLicenseLegalResource.APPLICATION_PATH).parameter(application.getPublicId()).get();

    assertResponseStatus(200, response);
    ApiLicenseLegalApplicationReportDTO
        apiLicenseLegalApplicationReportDTO = response.getBody(ApiLicenseLegalApplicationReportDTO.class);
    assertThat(apiLicenseLegalApplicationReportDTO).isNotNull();
    assertThat(apiLicenseLegalApplicationReportDTO.components).hasSize(14);
    assertThat(apiLicenseLegalApplicationReportDTO.licenseLegalMetadata).hasSize(0);
  }

  @Test
  public void testGetLicenseLegalApplicationReport_NotFound() throws Exception {
    String applicationPublicId = "doesNotExist";

    HttpResponse response =
        restRequest().path(ApiLicenseLegalResource.APPLICATION_PATH).parameter(applicationPublicId).get();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo("Could not find an application with public ID " + applicationPublicId + ".");
  }

  private void mockReport(PolicyEvaluation evaluation) {
    try {
      Path reportDir = getCLMServer().getInstance(InsightWork.class)
          .getReportDir(evaluation.getApplicationId(), evaluation.getScanId()).toPath();
      Files.createDirectories(reportDir);
      Files.write(reportDir.resolve("report.zip"), Collections.singletonList("report.zip"));
      File reportFile = reportDir.resolve("report.zip").toFile();
      try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(reportFile))) {
        zos.putNextEntry(new ZipEntry("index.html"));
      }
      String[] filenames = {
          Report.BOM_JSON_FILENAME, Report.SECURITY_JSON_FILENAME, Report.LICENSES_JSON_FILENAME,
          Report.DATA_JSON_FILENAME, Report.DEPENDENCIES_JSON_FILENAME
      };
      for (String filename : filenames) {
        File file = Report.getCacheFile(reportFile, filename);
        FileUtils.copyURLToFile(getClass().getResource("/LicenseLegalResourceTest/report/" + filename), file);
      }
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
