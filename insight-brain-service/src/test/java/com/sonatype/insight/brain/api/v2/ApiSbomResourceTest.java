/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.nio.file.Path;
import javax.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.service.ApiSbomService;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;
import com.sonatype.insight.brain.utils.SbomTestsHelper;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiSbomResourceTest
    extends AbstractResourceTest
{
  private ThirdPartySbomMetadataDAO dao;

  private InsightWork insightWork;

  @Before
  public void setUp() throws Exception {
    dao = lookup(ThirdPartySbomMetadataDAO.class);
    insightWork = lookup(InsightWork.class);
    setFeatures(LicensedFeature.SBOM_MANAGER);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.SBOM_RESOURCE_PATH);
  }

  @Test
  public void testDeleteSbomVersion() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Path fileInWorkDirPath =
        SbomTestsHelper.createTestFileForSbomMetadata(insightWork.getSbomDir(app.getId()),
            getClass().getResource("/" + getClass().getSimpleName() + "/third-party-simple-bom.xml"));
    ThirdPartySbomMetadata thirdPartySbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withFilename(fileInWorkDirPath.getFileName().toString())
        .build();

    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_VERSIONS_PATH)
        .parameter(thirdPartySbomMetadata.getApplicationId(), thirdPartySbomMetadata.getSbomVersion()).delete();
    assertResponseStatus(204, response);

    ThirdPartySbomMetadata retrievedSbomMetadata =
        dao.getByApplicationIdAndSbomVersion(thirdPartySbomMetadata.getApplicationId(),
            thirdPartySbomMetadata.getSbomVersion());
    assertThat(retrievedSbomMetadata).isNull();
  }

  @Test
  public void testGetSbomVersion_Xml() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Path fileInWorkDirPath =
        SbomTestsHelper.createTestFileForSbomMetadata(insightWork.getSbomDir(app.getId()),
            getClass().getResource(
                "/" + getClass().getSimpleName() + "/cb4e10e0f3a94fd98bee955b53f9474c7343830902282944835.xml.gz"));
    ThirdPartySbomMetadata thirdPartySbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withFilename(fileInWorkDirPath.getFileName().toString())
        .build();

    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_VERSIONS_PATH)
        .parameter(thirdPartySbomMetadata.getApplicationId(), thirdPartySbomMetadata.getSbomVersion())
        .query("state=" + ApiSbomService.SBOM_STATE_ORIGINAL)
        .get();
    assertResponseStatus(Status.OK.getStatusCode(), response);
    assertThat(response.getContentType()).isEqualTo("application/xml");
    assertThat(response.getBodyBytes()).hasSizeGreaterThan(0);

    String contentHeader = response.getHeader("Content-Disposition");
    String actualFilename = contentHeader.substring(contentHeader.indexOf("=") + 1).split(";")[0].replaceAll("\"", "");
    assertThat(actualFilename).isEqualTo(app.getName() + "_" + thirdPartySbomMetadata.getSbomVersion() + ".xml");
  }

  @Test
  public void testGetSbomVersion_Json() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Path fileInWorkDirPath =
        SbomTestsHelper.createTestFileForSbomMetadata(insightWork.getSbomDir(app.getId()),
            getClass().getResource(
                "/" + getClass().getSimpleName() + "/668bbb2087354637b030de2bc1a3faf76935110932971722768.json.gz"));
    ThirdPartySbomMetadata thirdPartySbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withJsonSpecFormat()
        .withFilename(fileInWorkDirPath.getFileName().toString())
        .build();

    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_VERSIONS_PATH)
        .parameter(thirdPartySbomMetadata.getApplicationId(), thirdPartySbomMetadata.getSbomVersion())
        .query("state=" + ApiSbomService.SBOM_STATE_ORIGINAL)
        .get();
    assertResponseStatus(Status.OK.getStatusCode(), response);
    assertThat(response.getContentType()).isEqualTo("application/json");
    assertThat(response.getBodyBytes()).hasSizeGreaterThan(0);

    String contentHeader = response.getHeader("Content-Disposition");
    String actualFilename = contentHeader.substring(contentHeader.indexOf("=") + 1).split(";")[0].replaceAll("\"", "");
    assertThat(actualFilename).isEqualTo(app.getName() + "_" + thirdPartySbomMetadata.getSbomVersion() + ".json");
  }
}
