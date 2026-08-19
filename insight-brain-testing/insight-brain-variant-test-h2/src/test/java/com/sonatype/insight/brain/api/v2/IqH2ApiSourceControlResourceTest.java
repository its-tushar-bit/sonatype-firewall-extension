/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.scmusermatching.FromMappingEnum;
import com.sonatype.insight.brain.api.v2.dto.scmusermatching.SCMUserMappingsDTO;
import com.sonatype.insight.brain.api.v2.dto.scmusermatching.SCMUserMappingsResponseDTO;
import com.sonatype.insight.brain.api.v2.dto.scmusermatching.SCMUserMatchingResultDTO;
import com.sonatype.insight.brain.api.v2.dto.scmusermatching.ToMappingEnum;
import com.sonatype.insight.brain.api.v2.dto.scmusermatching.UserMapping;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiSourceControlDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlAdapter;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticSourceControlConfigurationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.sourcecontrol.ScmUserMappings;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.security.TestEncryptionKeyStore;
import com.sonatype.insight.brain.testsupport.wiremock.ReusableWireMockExtension;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.nexus.scm.SourceControlProvider;
import org.assertj.core.api.Assertions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.sonatype.insight.brain.api.v2.ApiSourceControlResource.AUTOMATIC_ROLE_ASSIGNMENT_PATH;
import static com.sonatype.insight.brain.api.v2.dto.scmusermatching.FromMappingEnum.SCM_USERNAME;
import static com.sonatype.insight.brain.api.v2.dto.scmusermatching.ToMappingEnum.IQ_USERNAME;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.utils.ScmUserMappingsHelper.getRandomMappings;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Converted from the legacy {@code ApiSourceControlResourceTest}. Kept in the original package because
 * {@link ApiSourceControlResource#AUTOMATIC_ROLE_ASSIGNMENT_PATH} (and the other path constants) are package-private.
 */
@IqH2Test
class IqH2ApiSourceControlResourceTest
{
  static final String VALID_URL = "https://example.com/organization/project";

  private static final String TOKEN = new String(
      new PasswordHandler(new TestEncryptionKeyStore())
          .encryptPassword("token".toCharArray()));

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private IqTestContext ctx;

  private AutomaticSourceControlConfigurationDAO sourceControlConfigurationDAO;

  private Application app;

  private Organization org;

  private ApiSourceControlAdapter apiSourceControlAdapter;

  @RegisterExtension
  static ReusableWireMockExtension gitService = new ReusableWireMockExtension();

  @BeforeEach
  void setup() {
    gitService.stubFor(get(urlPathEqualTo("/api/v3/user"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{\"username\":\"foo\"}")
            .withStatus(HttpStatus.SC_OK)));
    gitService.stubFor(get(urlPathMatching("/api/v3/repos/.*/.*"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{ \"private\": false }")));

    sourceControlConfigurationDAO = ctx.lookup(AutomaticSourceControlConfigurationDAO.class);
    apiSourceControlAdapter = ctx.lookup(ApiSourceControlAdapter.class);
    app = ctx.tempEntity().newApplicationWithParent();
    org = ctx.tempEntity().newOrganization();
    ctx.tempEntity().newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest()
        .path(PublicApiPaths.SOURCE_CONTROL_PATH_V2)
        .auth();
  }

  @Test
  void testGetSourceControlByOwner_ByOrganization() throws Exception {
    SourceControl sourceControl = ctx.tempEntity()
        .newSourceControl(
            org.getId(), null, "token", null);
    HttpResponse response = restRequest()
        .path(ApiSourceControlResource.BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, org.getId())
        .get();
    ctx.assertResponseStatus(200, response);
    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);
    assertThat(result.id).isEqualTo(sourceControl.getId());
    assertThat(result.repositoryUrl).isEqualTo(sourceControl.getRepositoryUrl());
    assertThat(result.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
    assertThat(result.provider).isNull();
    assertThat(result.commitStatusEnabled).isNull();
    assertThat(result.manualPullRequestsEnabled).isNull();
    assertThat(result.innerSourceAutomatedUpdatesEnabled).isNull();
  }

  @Test
  void testGetSourceControlByOwner_ByApplication() throws Exception {
    SourceControl sourceControl = ctx.tempEntity()
        .newSourceControl(
            app.getId(), VALID_URL, "token", null);
    HttpResponse response = restRequest()
        .path(ApiSourceControlResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .get();
    ctx.assertResponseStatus(200, response);
    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);
    assertThat(result.id).isEqualTo(sourceControl.getId());
    assertThat(result.repositoryUrl).isEqualTo(sourceControl.getRepositoryUrl());
    assertThat(result.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
    assertThat(result.provider).isNull();
    assertThat(result.commitStatusEnabled).isNull();
    assertThat(result.manualPullRequestsEnabled).isNull();
    assertThat(result.innerSourceAutomatedUpdatesEnabled).isNull();
  }

  @Test
  void testAddSourceControlByOwner_ByOrganization() throws Exception {
    ApiSourceControlDTO sourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(org.getId())
            .setToken("token")
            .setCommitStatusEnabled(false)
            .setManualPullRequestsEnabled(false)
            .setInnerSourceAutomatedUpdatesEnabled(false)
            .build());
    HttpResponse response = restRequest()
        .path(ApiSourceControlResource.BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, org.getId())
        .body(sourceControl)
        .post();
    ctx.assertResponseStatus(200, response);
    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);

    assertThat(result.id).isNotNull();
    assertThat(result.ownerId).isEqualTo(org.getId());
    assertThat(result.repositoryUrl).isEqualTo(sourceControl.repositoryUrl);
    assertThat(result.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
    assertThat(result.provider).isNull();
    assertThat(result.commitStatusEnabled).isFalse();
    assertThat(result.manualPullRequestsEnabled).isFalse();
    assertThat(result.innerSourceAutomatedUpdatesEnabled).isFalse();
  }

  @Test
  void testAddSourceControlByOwner_ByApplication_HttpsUrl() throws Exception {
    String repoUrl = String.format("%s/organization/project", gitService.baseUrl());
    ApiSourceControlDTO sourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(app.getId())
            .setRepositoryUrl(repoUrl)
            .setToken("token")
            .setCommitStatusEnabled(false)
            .setManualPullRequestsEnabled(false)
            .setInnerSourceAutomatedUpdatesEnabled(false)
            .build());
    HttpResponse response = restRequest()
        .path(ApiSourceControlResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(sourceControl)
        .post();
    ctx.assertResponseStatus(200, response);
    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);

    assertThat(result.id).isNotNull();
    assertThat(result.ownerId).isEqualTo(app.getId());
    assertThat(result.repositoryUrl).isEqualTo(repoUrl);
    assertThat(result.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
    assertThat(result.provider).isNull();
    assertThat(result.commitStatusEnabled).isFalse();
    assertThat(result.manualPullRequestsEnabled).isFalse();
    assertThat(result.innerSourceAutomatedUpdatesEnabled).isFalse();
  }

  @Test
  void testUpdateSourceControlByOwner_ByOrganization() throws Exception {
    SourceControl sourceControl = ctx.tempEntity()
        .newSourceControl(
            org.getId(), null, "token", null);
    sourceControl.setToken("NEW_TOKEN");
    sourceControl.setCommitStatusEnabled(false);
    sourceControl.setManualPullRequestsEnabled(false);
    sourceControl.setInnerSourceAutomatedUpdatesEnabled(false);
    HttpResponse response = restRequest()
        .path(ApiSourceControlResource.BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, org.getId())
        .body(apiSourceControlAdapter.convertToDTO(sourceControl))
        .put();
    ctx.assertResponseStatus(200, response);

    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);
    assertThat(result.id).isEqualTo(sourceControl.getId());
    assertThat(result.repositoryUrl).isEqualTo(sourceControl.getRepositoryUrl());
    assertThat(result.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
    assertThat(result.provider).isNull();
    assertThat(result.commitStatusEnabled).isFalse();
    assertThat(result.manualPullRequestsEnabled).isFalse();
    assertThat(result.innerSourceAutomatedUpdatesEnabled).isFalse();
  }

  @Test
  void testUpdateSourceControlByOwner_ByApplication_HttpsUrl() throws Exception {
    String repoUrl = String.format("%s/organization/project", gitService.baseUrl());
    SourceControl sourceControl = ctx.tempEntity()
        .newSourceControl(
            app.getId(), repoUrl, "token", null);
    sourceControl.setRepositoryUrl(repoUrl);
    sourceControl.setCommitStatusEnabled(false);
    HttpResponse response = restRequest()
        .path(ApiSourceControlResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(apiSourceControlAdapter.convertToDTO(sourceControl))
        .put();
    ctx.assertResponseStatus(200, response);

    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);
    assertThat(result.id).isEqualTo(sourceControl.getId());
    assertThat(result.repositoryUrl).isEqualTo(repoUrl);
    assertThat(result.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
    assertThat(result.provider).isNull();
    assertThat(result.commitStatusEnabled).isFalse();
  }

  @Test
  void testAddSourceControlByOwner_InvalidSourceControlProvider() throws Exception {
    ApiSourceControlDTO sourceControl = apiSourceControlAdapter.convertToDTO(
        ctx.tempEntity().newSourceControl(org.getId(), null, "token", null));

    ObjectNode node = OBJECT_MAPPER.valueToTree(sourceControl);
    node.put("provider", "invalid_scm");

    HttpResponse response = restRequest()
        .path(ApiSourceControlResource.BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, org.getId())
        .body(node)
        .post();
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(
        "SourceControl provider value 'invalid_scm' is invalid,"
            + " valid options are: github, gitlab, bitbucket, azure");
  }

  @Test
  void testUpdateSourceControlByOwner_InvalidSourceControlProvider() throws Exception {
    ApiSourceControlDTO sourceControl =
        apiSourceControlAdapter.convertToDTO(ctx.tempEntity().newSourceControl(org.getId(), null, "token", null));

    ObjectNode node = (ObjectNode) OBJECT_MAPPER.valueToTree(sourceControl);
    node.put("provider", "invalid_scm");

    HttpResponse response = restRequest()
        .path(ApiSourceControlResource.BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, org.getId())
        .body(node)
        .put();
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(
        "SourceControl provider value 'invalid_scm' is invalid,"
            + " valid options are: github, gitlab, bitbucket, azure");
  }

  @Test
  void testDeleteSourceControlByOwner_ByOrganization() throws Exception {
    ctx.tempEntity()
        .newSourceControl(
            org.getId(), null, TOKEN, null);
    HttpResponse response = restRequest()
        .path(ApiSourceControlResource.BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, org.getId())
        .delete();
    ctx.assertResponseStatus(204, response);
  }

  @Test
  void testDeleteSourceControlByOwner_ByApplication() throws Exception {
    String repoUrl = String.format("%s/organization/project", gitService.baseUrl());
    ctx.tempEntity()
        .newSourceControl(
            app.getId(), repoUrl, TOKEN, null);
    HttpResponse response = restRequest()
        .path(ApiSourceControlResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .delete();
    ctx.assertResponseStatus(204, response);
  }

  @Test
  void testAddOrUpdateSourceControl_AutomaticScmEnabled_HttpUrl() throws Exception {
    // ensure organization record exists
    ctx.tempEntity().newSourceControl(app.getOrganizationId(), null, TOKEN, null);

    sourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);
    String repoUrl = String.format("%s/organization/project", gitService.baseUrl());
    HttpResponse response = restRequest()
        .query("publicId", app.getPublicId())
        .query("repositoryUrl", repoUrl)
        .post();
    ctx.assertResponseStatus(200, response);
    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);

    assertThat(result.id).isNotNull();
    assertThat(result.ownerId).isEqualTo(app.getId());
    assertThat(result.repositoryUrl).isEqualTo(repoUrl);
    assertThat(result.token).isNull();
    assertThat(result.provider).isNull();

    // now try to update with a different repo URL value
    response = restRequest()
        .query("publicId", app.getPublicId())
        .query("repositoryUrl", "https://example.com/organization/project2")
        .post();
    ctx.assertResponseStatus(200, response);
    result = response.getBody(ApiSourceControlDTO.class);

    assertThat(result.id).isNotNull();
    assertThat(result.ownerId).isEqualTo(app.getId());
    assertThat(result.repositoryUrl).isEqualTo(repoUrl); // should not change
    assertThat(result.token).isNull();
    assertThat(result.provider).isNull();
  }

  @Test
  void testAddOrUpdateSourceControl_AutomaticScmDisabled() throws Exception {
    // ensure organization record exists
    ctx.tempEntity().newSourceControl(app.getOrganizationId(), null, "token", null);

    sourceControlConfigurationDAO.setSourceControlConfigurationEnabled(false);

    HttpResponse response = restRequest()
        .query("publicId", app.getPublicId())
        .query("repositoryUrl", VALID_URL)
        .post();
    ctx.assertResponseStatus(204, response);
  }

  @Test
  void testAddOrUpdateSourceControl_InvalidPublicId() throws Exception {
    HttpResponse response = restRequest()
        .query("publicId", "abc")
        .query("repositoryUrl", VALID_URL)
        .post();

    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).startsWith("Could not find an application with public ID abc.");
  }

  @Test
  void testAddOrUpdateSourceControl_InvalidRepositoryUrl() throws Exception {
    // ensure organization record exists
    ctx.tempEntity().newSourceControl(app.getOrganizationId(), null, "token", null);

    sourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);

    HttpResponse response = restRequest()
        .query("publicId", app.getPublicId())
        .query("repositoryUrl", "https://notvalid")
        .post();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).startsWith("SourceControl repositoryUrl is invalid:");
  }

  @Test
  void testAddOrUpdateSourceControl_CannotValidateRepositoryUrl() throws Exception {
    restRequest()
        .path(ApiSourceControlResource.BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, ROOT_ORGANIZATION_ID)
        .delete();
    sourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);

    HttpResponse response = restRequest()
        .query("publicId", app.getPublicId())
        .query("repositoryUrl", "https://notvalid")
        .post();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).startsWith("Cannot validate SourceControl repositoryUrl");
  }

  @Test
  void testAddUserMappingByOrg_NoExistingMapping() throws Exception {
    List<UserMapping> userMappings =
        Arrays.asList(new UserMapping(FromMappingEnum.GITLOG_EMAIL, ToMappingEnum.IQ_EMAIL));
    SCMUserMappingsDTO scmUserMappingsDTO = new SCMUserMappingsDTO("developer", userMappings);
    HttpResponse response = restRequest()
        .path(ApiSourceControlResource.USER_MAPPING_PER_ORGANIZATION_PATH)
        .parameter(org.getId())
        .body(scmUserMappingsDTO)
        .post();
    ctx.assertResponseStatus(204, response);
  }

  @Test
  void testAddUserMappingByOrg_ExistingMapping() throws Exception {
    List<UserMapping> userMappings =
        Arrays.asList(new UserMapping(FromMappingEnum.GITLOG_EMAIL, ToMappingEnum.IQ_EMAIL));
    SCMUserMappingsDTO scmUserMappingsDTO = new SCMUserMappingsDTO(null, userMappings);
    List<Entry<String, String>> userMappingsAsEntries =
        SCMUserMappingsDTO.userMappingsAsEntries(scmUserMappingsDTO.mappings());
    ctx.tempEntity().createScmUserMappings(org.getId(), userMappingsAsEntries);

    List<UserMapping> newUserMappings =
        Arrays.asList(new UserMapping(FromMappingEnum.GITLOG_FULLNAME, ToMappingEnum.IQ_FULLNAME));
    SCMUserMappingsDTO newScmUserMappingsDTO = new SCMUserMappingsDTO("developer", newUserMappings);
    HttpResponse response = restRequest()
        .path(ApiSourceControlResource.USER_MAPPING_PER_ORGANIZATION_PATH)
        .parameter(org.getId())
        .body(newScmUserMappingsDTO)
        .post();
    ctx.assertResponseStatus(204, response);
  }

  @Test
  void testAddUserMappingByOrg_ErrorForDuplicatedMapping() throws Exception {
    List<UserMapping> userMappings =
        Arrays.asList(new UserMapping(FromMappingEnum.GITLOG_EMAIL, ToMappingEnum.IQ_EMAIL),
            new UserMapping(FromMappingEnum.GITLOG_EMAIL, ToMappingEnum.IQ_EMAIL));
    SCMUserMappingsDTO scmUserMappingsDTO = new SCMUserMappingsDTO("developer", userMappings);
    HttpResponse response = restRequest()
        .path(ApiSourceControlResource.USER_MAPPING_PER_ORGANIZATION_PATH)
        .parameter(org.getId())
        .body(scmUserMappingsDTO)
        .post();
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).startsWith(
        "There was a duplicate mapping GITLOG_EMAIL: IQ_EMAIL. Mappings should be unique.");
  }

  @Test
  void testDeleteUserMapping() throws Exception {
    List<UserMapping> userMappings =
        Arrays.asList(new UserMapping(FromMappingEnum.GITLOG_EMAIL, ToMappingEnum.IQ_EMAIL));
    SCMUserMappingsDTO scmUserMappingsDTO = new SCMUserMappingsDTO(null, userMappings);
    List<Entry<String, String>> userMappingsAsEntries =
        SCMUserMappingsDTO.userMappingsAsEntries(scmUserMappingsDTO.mappings());
    ctx.tempEntity().createScmUserMappings(org.getId(), userMappingsAsEntries);

    HttpResponse response = restRequest()
        .path(ApiSourceControlResource.USER_MAPPING_PER_ORGANIZATION_PATH)
        .parameter(org.getId())
        .delete();
    ctx.assertResponseStatus(204, response);
  }

  @Test
  void testDeleteUserMapping_NoExistingMapping() throws Exception {
    HttpResponse response = restRequest()
        .path(ApiSourceControlResource.USER_MAPPING_PER_ORGANIZATION_PATH)
        .parameter(org.getId())
        .delete();
    ctx.assertResponseStatus(204, response);
  }

  @Test
  void testDeleteUserMapping_InvalidOrgId() throws Exception {
    HttpResponse response = restRequest()
        .path(ApiSourceControlResource.USER_MAPPING_PER_ORGANIZATION_PATH)
        .parameter("invalid")
        .delete();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).startsWith("Organization with ID invalid does not exist");
  }

  @SuppressWarnings("deprecation")
  @Test
  void testAddSourceControl_DeprecatedFieldsAreUsedWhenReplacementFieldsAreNotPopulated() throws Exception {
    DeprecatedApiSourceControlDTO apiSourceControlDTO = new DeprecatedApiSourceControlDTO();
    apiSourceControlDTO.ownerId = org.getId();
    apiSourceControlDTO.token = "token";
    // Deprecated fields must be used if the replacement fields are not populated
    apiSourceControlDTO.enablePullRequests = true;
    apiSourceControlDTO.enableStatusChecks = false;

    HttpResponse response = restRequest().path(ApiSourceControlResource.BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, org.getId())
        .body(apiSourceControlDTO)
        .post();
    ctx.assertResponseStatus(200, response);
    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);

    assertThat(result.id).isNotNull();
    assertThat(result.ownerId).isEqualTo(org.getId());
    assertThat(result.repositoryUrl).isEqualTo(apiSourceControlDTO.repositoryUrl);
    assertThat(result.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
    assertThat(result.provider).isNull();

    assertThat(result.remediationPullRequestsEnabled).isTrue();
    assertThat(result.enablePullRequests).isTrue();
    assertThat(result.statusChecksEnabled).isFalse();
    assertThat(result.enableStatusChecks).isFalse();
  }

  @SuppressWarnings("deprecation")
  @Test
  void testAddSourceControl_DeprecatedFieldsAreNotUsedWhenReplacementFieldsArePopulated() throws Exception {
    ApiSourceControlDTO apiSourceControlDTO = apiSourceControlAdapter
        .convertToDTO(new SourceControl.Builder().setOwnerId(org.getId()).setToken("token").build());
    // Deprecated fields must be used if the replacement fields are not populated
    apiSourceControlDTO.remediationPullRequestsEnabled = true;
    apiSourceControlDTO.enablePullRequests = false;
    apiSourceControlDTO.statusChecksEnabled = false;
    apiSourceControlDTO.enableStatusChecks = true;

    HttpResponse response = restRequest().path(ApiSourceControlResource.BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, org.getId())
        .body(apiSourceControlDTO)
        .post();
    ctx.assertResponseStatus(200, response);
    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);

    assertThat(result.id).isNotNull();
    assertThat(result.ownerId).isEqualTo(org.getId());
    assertThat(result.repositoryUrl).isEqualTo(apiSourceControlDTO.repositoryUrl);
    assertThat(result.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
    assertThat(result.provider).isNull();

    assertThat(result.remediationPullRequestsEnabled).isTrue();
    assertThat(result.enablePullRequests).isTrue();
    assertThat(result.statusChecksEnabled).isFalse();
    assertThat(result.enableStatusChecks).isFalse();
  }

  @SuppressWarnings("deprecation")
  @Test
  void testUpdateSourceControl_DeprecatedFieldsAreUsedWhenReplacementFieldsAreNotPopulated() throws Exception {
    DeprecatedApiSourceControlDTO apiSourceControlDTO = new DeprecatedApiSourceControlDTO();
    apiSourceControlDTO.ownerId = org.getId();
    apiSourceControlDTO.token = "token";
    // Deprecated fields must be used if the replacement fields are not populated
    apiSourceControlDTO.enablePullRequests = true;
    apiSourceControlDTO.enableStatusChecks = false;

    SourceControl sourceControl = ctx.tempEntity().newSourceControl(org.getId(), null, "token", null);
    HttpResponse response = restRequest().path(ApiSourceControlResource.BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, org.getId())
        .body(apiSourceControlDTO)
        .put();
    ctx.assertResponseStatus(200, response);

    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);
    assertThat(result.id).isEqualTo(sourceControl.getId());
    assertThat(result.repositoryUrl).isEqualTo(sourceControl.getRepositoryUrl());
    assertThat(result.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
    assertThat(result.provider).isNull();

    assertThat(result.remediationPullRequestsEnabled).isTrue();
    assertThat(result.enablePullRequests).isTrue();
    assertThat(result.statusChecksEnabled).isFalse();
    assertThat(result.enableStatusChecks).isFalse();
  }

  @SuppressWarnings("deprecation")
  @Test
  void testUpdateSourceControl_DeprecatedFieldsAreNotUsedWhenReplacementFieldsArePopulated() throws Exception {
    ApiSourceControlDTO apiSourceControlDTO = apiSourceControlAdapter
        .convertToDTO(new SourceControl.Builder().setOwnerId(org.getId()).setToken("token").build());
    // Deprecated fields must be used if the replacement fields are not populated
    apiSourceControlDTO.remediationPullRequestsEnabled = true;
    apiSourceControlDTO.enablePullRequests = false;
    apiSourceControlDTO.statusChecksEnabled = false;
    apiSourceControlDTO.enableStatusChecks = true;

    SourceControl sourceControl = ctx.tempEntity().newSourceControl(org.getId(), null, "token", null);
    HttpResponse response = restRequest().path(ApiSourceControlResource.BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, org.getId())
        .body(apiSourceControlDTO)
        .put();
    ctx.assertResponseStatus(200, response);

    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);
    assertThat(result.id).isEqualTo(sourceControl.getId());
    assertThat(result.repositoryUrl).isEqualTo(sourceControl.getRepositoryUrl());
    assertThat(result.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
    assertThat(result.provider).isNull();

    assertThat(result.remediationPullRequestsEnabled).isTrue();
    assertThat(result.enablePullRequests).isTrue();
    assertThat(result.statusChecksEnabled).isFalse();
    assertThat(result.enableStatusChecks).isFalse();
  }

  @Test
  void testGetUserMappingsByOwner_NoScmUserMappingsConfigured() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization();

    HttpResponse response = restRequest().path(ApiSourceControlResource.USER_MAPPINGS_BY_OWNER_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId())
        .get();

    ctx.assertResponseStatus(204, response);
  }

  @Test
  void testGetUserMappingsByOwner_NoPathForRepository() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();

    HttpResponse response = restRequest().path(ApiSourceControlResource.USER_MAPPINGS_BY_OWNER_PATH)
        .parameter(OwnerType.REPOSITORY, repository.getId())
        .get();

    ctx.assertResponseStatus(404, response);
  }

  @Test
  void testGetUserMappingsByOwner_Success() throws Exception {
    Organization organization1 = ctx.tempEntity().newOrganization();
    Organization organization2 = ctx.tempEntity().newOrganization(organization1);
    Organization organization3 = ctx.tempEntity().newOrganization(organization2);

    Application application = ctx.tempEntity().newApplication(organization3.getId());

    ScmUserMappings existingScmUserMappings = ctx.tempEntity()
        .createScmUserMappings(Role.DEVELOPER_ROLE_ID,
            organization1.getId(), getRandomMappings());

    HttpResponse response = restRequest().path(ApiSourceControlResource.USER_MAPPINGS_BY_OWNER_PATH)
        .parameter(OwnerType.APPLICATION, application.getId())
        .get();

    List<UserMapping> existingMappings = existingScmUserMappings.getMappings().stream().map(UserMapping::new).toList();

    ctx.assertResponseStatus(200, response);
    SCMUserMappingsResponseDTO responseBody = response.getBody(SCMUserMappingsResponseDTO.class);
    Assertions.assertThat(responseBody.ownerInternalId()).isEqualTo(organization1.getId());
    Assertions.assertThat(responseBody.inherited()).isTrue();
    Assertions.assertThat(responseBody.userMapping().role()).isEqualTo("developer");
    Assertions.assertThat(responseBody.userMapping().mappings()).isEqualTo(existingMappings);
  }

  @Test
  void testAutomaticRoleAssignment_ReturnMappedUsersGivenValidAuthorizationAndPayloads() throws Exception {
    // === Given ===
    mockGithubContributorUserNamesApiCalls();

    final var organization = ctx.tempEntity().newOrganization();
    final var givenApp = ctx.tempEntity().newApplication(organization.getId());

    ctx.tempEntity()
        .newSourceControl(
            givenApp.getId(),
            gitService.baseUrl() + "/some-org/some-app",
            TOKEN,
            SourceControlProvider.GITHUB);

    ctx.tempEntity().newUser("user1");
    ctx.tempEntity().newUser("user2");

    final SCMUserMappingsDTO givenScmUserMappingsDTO = new SCMUserMappingsDTO(
        "developer",
        Lists.newArrayList(new UserMapping(SCM_USERNAME, IQ_USERNAME)));

    // === Then ===
    HttpResponse response = restRequest().path(AUTOMATIC_ROLE_ASSIGNMENT_PATH)
        .parameter(givenApp.getPublicId())
        .body(givenScmUserMappingsDTO)
        .post();

    ctx.assertResponseStatus(200, response);
    final SCMUserMatchingResultDTO scmUserMatchingResultDTO = response.getBody(SCMUserMatchingResultDTO.class);
    assertThat(scmUserMatchingResultDTO).isEqualTo(new SCMUserMatchingResultDTO(
        new UserMapping(SCM_USERNAME, IQ_USERNAME),
        Sets.newHashSet("user1", "user2")));
  }

  @Test
  void testAutomaticRoleAssignment_ReturnsUnauthorizedWhenUserIsNotLoggedIn() throws Exception {
    final SCMUserMappingsDTO givenScmUserMappingsDTO = new SCMUserMappingsDTO(
        "developer",
        Lists.newArrayList(new UserMapping(SCM_USERNAME, IQ_USERNAME)));

    HttpResponse response = restRequest().path(AUTOMATIC_ROLE_ASSIGNMENT_PATH)
        .parameter("any-id")
        .body(givenScmUserMappingsDTO)
        .anon()
        .post();

    ctx.assertResponseStatus(401, response);
  }

  @Test
  void testAutomaticRoleAssignment_ReturnsBadRequestIfUserDoesNotProvideMapping() throws Exception {
    final var organization = ctx.tempEntity().newOrganization();
    final var givenApp = ctx.tempEntity().newApplication(organization.getId());

    HttpResponse response = restRequest().path(AUTOMATIC_ROLE_ASSIGNMENT_PATH)
        .parameter(givenApp.getPublicId())
        .body(null)
        .post();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .isEqualTo("An SCMUserMappingsDTO must be provided either with the request or at the organization level");
  }

  @Test
  void testUpdateSourceControlByOwner_AuthTypeChange_GitHubAppToPat() throws Exception {
    SourceControl sourceControl = ctx.tempEntity()
        .newSourceControl(
            app.getId(), VALID_URL, null, SourceControlProvider.GITHUB);
    sourceControl.setAuthenticationType(SourceControl.AuthenticationType.GITHUB_APP);

    ApiSourceControlDTO updateDTO = apiSourceControlAdapter.convertToDTO(sourceControl);
    updateDTO.authenticationType = "PAT";
    updateDTO.token = "new-pat-token";

    HttpResponse response = restRequest()
        .path(ApiSourceControlResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(updateDTO)
        .put();

    ctx.assertResponseStatus(200, response);
    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);

    assertThat(result.id).isEqualTo(sourceControl.getId());
    assertThat(result.authenticationType).isEqualTo("PAT");
    assertThat(result.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
  }

  @Test
  void testUpdateSourceControlByOwner_AuthTypeChange_PatToGitHubApp() throws Exception {
    SourceControl sourceControl = ctx.tempEntity()
        .newSourceControl(
            app.getId(), VALID_URL, "encrypted-token", SourceControlProvider.GITHUB);
    sourceControl.setAuthenticationType(SourceControl.AuthenticationType.PAT);

    ApiSourceControlDTO updateDTO = apiSourceControlAdapter.convertToDTO(sourceControl);
    updateDTO.authenticationType = "GITHUB_APP";
    updateDTO.token = SourceControl.FAKE_SECRET_KEY;

    HttpResponse response = restRequest()
        .path(ApiSourceControlResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(updateDTO)
        .put();

    ctx.assertResponseStatus(200, response);
    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);

    assertThat(result.id).isEqualTo(sourceControl.getId());
    assertThat(result.authenticationType).isEqualTo("GITHUB_APP");
    assertThat(result.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
  }

  /**
   * This is a verbatim copy of the ApiSourceControlDTO class before some fields were deprecated.
   * It is used to test the API still works with the old API DTO.
   */
  @SuppressWarnings("unused")
  private static class DeprecatedApiSourceControlDTO
  {
    public String id;

    public String ownerId;

    public String repositoryUrl;

    public String username;

    public String token;

    public String provider;

    public String baseBranch;

    public Boolean enablePullRequests;

    public Boolean enableStatusChecks;
  }

  private void mockGithubContributorUserNamesApiCalls() throws IOException {
    gitService.stubFor(get(urlPathEqualTo("/api/v3/repos/some-org/some-app/contributors"))
        .willReturn(aResponse()
            .withBody(getResourceContents("/ApiSourceControlResourceTest/contributorsResponse.json"))));

    gitService.stubFor(get(urlEqualTo("/user"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody(getResourceContents("/ApiSourceControlResourceTest/userResponse.json"))));
  }

  private String getResourceContents(final String path) throws IOException {
    final var resource = getClass().getResource(path);
    return FileUtils.readFileToString(new File(resource.getFile()), StandardCharsets.UTF_8);
  }
}
