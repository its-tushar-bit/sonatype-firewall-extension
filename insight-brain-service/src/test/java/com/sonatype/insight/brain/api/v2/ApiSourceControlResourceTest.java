/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiSourceControlDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlAdapter;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticSourceControlConfigurationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ApiSourceControlResourceTest
    extends AbstractResourceTest
{
  static final String VALID_URL = "https://example.com/organization/project";

  private static final ApiSourceControlAdapter apiSourceControlAdapter = new ApiSourceControlAdapter();

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private Application app;

  private Organization org;

  @Before
  public void setup() {
    app = tempEntity.newApplicationWithParent();
    org = tempEntity.newOrganization();
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.SOURCE_CONTROL_PATH_V2).auth();
  }

  @Test
  public void testGetSourceControlByOwner_ByOrganization() throws Exception {
    SourceControl sourceControl = tempEntity.newSourceControl(
        org.getId(), null, "token", null);
    HttpResponse response = restRequest()
        .path(ApiSourceControlResource.BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, org.getId())
        .get();
    assertResponseStatus(200, response);
    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);
    assertThat(result.id).isEqualTo(sourceControl.getId());
    assertThat(result.repositoryUrl).isEqualTo(sourceControl.getRepositoryUrl());
    assertThat(result.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
    assertThat(result.provider).isNull();
  }

  @Test
  public void testGetSourceControlByOwner_ByApplication() throws Exception {
    SourceControl sourceControl = tempEntity.newSourceControl(
        app.getId(), VALID_URL, "token", null);
    HttpResponse response = restRequest()
        .path(ApiSourceControlResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .get();
    assertResponseStatus(200, response);
    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);
    assertThat(result.id).isEqualTo(sourceControl.getId());
    assertThat(result.repositoryUrl).isEqualTo(sourceControl.getRepositoryUrl());
    assertThat(result.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
    assertThat(result.provider).isNull();
  }

  @Test
  public void testAddSourceControlByOwner_ByOrganization() throws Exception {
    ApiSourceControlDTO sourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(org.getId()).setToken("token")
            .build());
    HttpResponse response = restRequest()
        .path(ApiSourceControlResource.BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, org.getId())
        .body(sourceControl)
        .post();
    assertResponseStatus(200, response);
    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);

    assertThat(result.id).isNotNull();
    assertThat(result.ownerId).isEqualTo(org.getId());
    assertThat(result.repositoryUrl).isEqualTo(sourceControl.repositoryUrl);
    assertThat(result.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
    assertThat(result.provider).isNull();
  }

  @Test
  public void testAddSourceControlByOwner_ByApplication() throws Exception {
    ApiSourceControlDTO sourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(app.getId()).setRepositoryUrl(VALID_URL).setToken("token")
            .build());
    HttpResponse response = restRequest()
        .path(ApiSourceControlResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(sourceControl).post();
    assertResponseStatus(200, response);
    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);

    assertThat(result.id).isNotNull();
    assertThat(result.ownerId).isEqualTo(app.getId());
    assertThat(result.repositoryUrl).isEqualTo(sourceControl.repositoryUrl);
    assertThat(result.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
    assertThat(result.provider).isNull();
  }

  @Test
  public void testUpdateSourceControlByOwner_ByOrganization() throws Exception {
    SourceControl sourceControl = tempEntity.newSourceControl(
        org.getId(), null, "token", null);
    sourceControl.setToken("NEW_TOKEN");
    HttpResponse response = restRequest()
        .path(ApiSourceControlResource.BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, org.getId())
        .body(apiSourceControlAdapter.convertToDTO(sourceControl))
        .put();
    assertResponseStatus(200, response);

    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);
    assertThat(result.id).isEqualTo(sourceControl.getId());
    assertThat(result.repositoryUrl).isEqualTo(sourceControl.getRepositoryUrl());
    assertThat(result.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
    assertThat(result.provider).isNull();
  }

  @Test
  public void testUpdateSourceControlByOwner_ByApplication() throws Exception {
    SourceControl sourceControl = tempEntity.newSourceControl(
        app.getId(), VALID_URL, "token", null);
    sourceControl.setRepositoryUrl(VALID_URL + "/test/");
    HttpResponse response = restRequest()
        .path(ApiSourceControlResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(apiSourceControlAdapter.convertToDTO(sourceControl))
        .put();
    assertResponseStatus(200, response);

    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);
    assertThat(result.id).isEqualTo(sourceControl.getId());
    assertThat(result.repositoryUrl).isEqualTo(sourceControl.getRepositoryUrl());
    assertThat(result.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
    assertThat(result.provider).isNull();
  }

  @Test
  public void testAddSourceControlByOwner_InvalidSourceControlProvider()
      throws Exception
  {
    ApiSourceControlDTO sourceControl = apiSourceControlAdapter.convertToDTO(
        tempEntity.newSourceControl(org.getId(), null, "token", null));

    ObjectNode node = (ObjectNode) OBJECT_MAPPER.valueToTree(sourceControl);
    node.put("provider", "invalid_scm");

    HttpResponse response = restRequest()
        .path(ApiSourceControlResource.BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, org.getId())
        .body(node)
        .post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(
        "SourceControl provider value 'invalid_scm' is invalid,"
            + " valid options are: github, gitlab");
  }

  @Test
  public void testUpdateSourceControlByOwner_InvalidSourceControlProvider()
      throws Exception
  {
    ApiSourceControlDTO sourceControl = apiSourceControlAdapter.convertToDTO(
        tempEntity.newSourceControl(org.getId(), null, "token", null));

    ObjectNode node = (ObjectNode) OBJECT_MAPPER.valueToTree(sourceControl);
    node.put("provider", "invalid_scm");

    HttpResponse response = restRequest()
        .path(ApiSourceControlResource.BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, org.getId())
        .body(node).put();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(
        "SourceControl provider value 'invalid_scm' is invalid,"
            + " valid options are: github, gitlab");
  }

  @Test
  public void testDeleteSourceControlByOwner_ByOrganization() throws Exception {
    tempEntity.newSourceControl(
        org.getId(), null, "token", null);
    HttpResponse response = restRequest()
        .path(ApiSourceControlResource.BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, org.getId())
        .delete();
    assertResponseStatus(204, response);
  }

  @Test
  public void testDeleteSourceControlByOwner_ByApplication() throws Exception {
    tempEntity.newSourceControl(
        app.getId(), VALID_URL, "token", null);
    HttpResponse response = restRequest()
        .path(ApiSourceControlResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .delete();
    assertResponseStatus(204, response);
  }

  @Test
  public void testAddOrUpdateSourceControl_AutomaticScmEnabled() throws Exception {
    // ensure organization record exists
    tempEntity.newSourceControl(app.getOrganizationId(), null, "token", null);

    AutomaticSourceControlConfigurationDAO sourceControlConfigurationDAO = new AutomaticSourceControlConfigurationDAO();
    sourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);

    HttpResponse response = restRequest()
        .query("publicId", app.getPublicId())
        .query("repositoryUrl", VALID_URL)
        .post();
    assertResponseStatus(200, response);
    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);

    assertThat(result.id).isNotNull();
    assertThat(result.ownerId).isEqualTo(app.getId());
    assertThat(result.repositoryUrl).isEqualTo(VALID_URL);
    assertThat(result.token).isNull();
    assertThat(result.provider).isNull();

    // now try to update with a different repo URL value
    response = restRequest()
        .query("publicId", app.getPublicId())
        .query("repositoryUrl", "https://example.com/organization/project2")
        .post();
    assertResponseStatus(200, response);
    result = response.getBody(ApiSourceControlDTO.class);

    assertThat(result.id).isNotNull();
    assertThat(result.ownerId).isEqualTo(app.getId());
    assertThat(result.repositoryUrl).isEqualTo(VALID_URL); // should not change
    assertThat(result.token).isNull();
    assertThat(result.provider).isNull();
  }

  @Test
  public void testAddOrUpdateSourceControl_AutomaticScmDisabled() throws Exception {
    // ensure organization record exists
    tempEntity.newSourceControl(app.getOrganizationId(), null, "token", null);

    AutomaticSourceControlConfigurationDAO sourceControlConfigurationDAO = new AutomaticSourceControlConfigurationDAO();
    sourceControlConfigurationDAO.setSourceControlConfigurationEnabled(false);

    HttpResponse response = restRequest()
        .query("publicId", app.getPublicId())
        .query("repositoryUrl", VALID_URL)
        .post();
    assertResponseStatus(204, response);
  }

  @Test
  public void testAddOrUpdateSourceControl_InvalidPublicId() throws Exception {
    HttpResponse response = restRequest()
        .query("publicId", "abc")
        .query("repositoryUrl", VALID_URL)
        .post();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).startsWith("Could not find an application with public ID");
  }

  @Test
  public void testAddOrUpdateSourceControl_InvalidRepositoryUrl() throws Exception {
    // ensure organization record exists
    tempEntity.newSourceControl(app.getOrganizationId(), null, "token", null);

    AutomaticSourceControlConfigurationDAO sourceControlConfigurationDAO = new AutomaticSourceControlConfigurationDAO();
    sourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);

    HttpResponse response = restRequest()
        .query("publicId", app.getPublicId())
        .query("repositoryUrl", "https://not valid")
        .post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).startsWith("SourceControl repositoryUrl is invalid:");
  }

  @Test
  public void testAddOrUpdateSourceControl_CannotValidateRepositoryUrl() throws Exception {
    restRequest()
        .path(ApiSourceControlResource.BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, ROOT_ORGANIZATION_ID)
        .delete();
    AutomaticSourceControlConfigurationDAO sourceControlConfigurationDAO = new AutomaticSourceControlConfigurationDAO();
    sourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);

    HttpResponse response = restRequest()
        .query("publicId", app.getPublicId())
        .query("repositoryUrl", "https://not valid")
        .post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).startsWith("Cannot validate SourceControl repositoryUrl");
  }
}
