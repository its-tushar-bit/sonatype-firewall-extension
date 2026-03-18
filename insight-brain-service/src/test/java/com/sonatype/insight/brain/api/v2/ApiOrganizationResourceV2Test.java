/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiTagDTO;
import com.sonatype.insight.brain.api.v2.dto.MoveOrganizationResponseDTO;
import com.sonatype.insight.brain.api.v2.dto.MoveOrganizationResponseDTO.ValidationError.MoveOrganizationValidationErrorType;
import com.sonatype.insight.brain.configuration.ldap.TestLdapServer;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiOrganizationResourceV2Test
    extends AbstractResourceTest
{
  private OrganizationDAO organizationDAO;

  private Organization organization;

  @Rule
  public TestLdapServer embeddedLdapServer = new TestLdapServer();

  @Before
  public void setUp() {
    organizationDAO = lookup(OrganizationDAO.class);
    organization = tempEntity.newOrganization("test-org");
  }

  @Test
  public void testGetOrganizations() throws Exception {
    Tag tag = tempEntity.newTag(organization.getId());

    final HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);
    final ApiOrganizationListDTO organizationListDTO = response.getBody(ApiOrganizationListDTO.class);
    assertThat(organizationListDTO).isNotNull();

    // One that was created for the test and one for the root org
    assertThat(organizationListDTO.organizations).hasSize(2);

    ApiOrganizationDTO retrievedOrg = organizationListDTO.organizations.get(0);
    if (Organization.ROOT_ORGANIZATION_ID.equals(retrievedOrg.id)) {
      retrievedOrg = organizationListDTO.organizations.get(1);
    }
    assertThat(retrievedOrg.id).isEqualTo(organization.getId());
    assertThat(retrievedOrg.name).isEqualTo(organization.getName());

    assertThat(retrievedOrg.tags).hasSize(1);

    ApiTagDTO retrievedTag = retrievedOrg.tags.get(0);
    assertThat(retrievedTag.id).isEqualTo(tag.getId());
    assertThat(retrievedTag.name).isEqualTo(tag.getName());
    assertThat(retrievedTag.description).isEqualTo(tag.getDescription());
    assertThat(retrievedTag.color).isEqualTo(tag.getColor());
  }

  @Test
  public void testGetOrganizations_OrgName() throws Exception {
    Organization organization = tempEntity.newOrganization();
    Tag tag = tempEntity.newTag(organization.getId());

    HttpResponse response = restRequest().query("organizationName", organization.getName()).get();

    assertResponseStatus(200, response);
    ApiOrganizationListDTO organizationListDTO = response.getBody(ApiOrganizationListDTO.class);
    assertThat(organizationListDTO).isNotNull();
    assertThat(organizationListDTO.organizations).hasSize(1);
    assertOrganizationData(organizationListDTO.organizations.get(0), organization, Collections.singletonList(tag));
  }

  @Test
  public void testGetOrganizations_Unlicensed() throws Exception {
    uninstallLicense();
    final HttpResponse response = restRequest().get();
    assertResponseStatus(402, response);
  }

  @Test
  public void testGetOrganization() throws Exception {
    Organization organization = tempEntity.newOrganization();
    Tag tag = tempEntity.newTag(organization.getId());

    HttpResponse response =
        restRequest().path(ApiOrganizationResourceV2.ORGANIZATION_ID).parameter(organization.getId()).get();

    assertResponseStatus(200, response);
    ApiOrganizationDTO apiOrganizationDTO = response.getBody(ApiOrganizationDTO.class);
    assertThat(apiOrganizationDTO).isNotNull();
    assertOrganizationData(apiOrganizationDTO, organization, Collections.singletonList(tag));
  }

  @Test
  public void testGetOrganization_NotFound() throws Exception {
    HttpResponse response =
        restRequest().path(ApiOrganizationResourceV2.ORGANIZATION_ID).parameter("doesNotExist").get();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Organization with ID doesNotExist does not exist.");
  }

  @Test
  public void testAddOrganization() throws Exception {
    ApiOrganizationDTO requestBody = new ApiOrganizationDTO(null, "test-create-organization");

    HttpRequest request = restRequest().body(requestBody);
    HttpResponse response = request.post();
    assertResponseStatus(200, response);

    ApiOrganizationDTO responseBody = response.getBody(ApiOrganizationDTO.class);
    assertThat(responseBody.id).isNotEmpty();

    Organization organization = organizationDAO.getByIdNotNull(responseBody.id);

    assertThat(responseBody.name).isEqualTo(requestBody.name);
    assertThat(responseBody.tags).isEmpty();

    assertThat(organization.getName()).isEqualTo(requestBody.name);
  }

  @Test
  public void testMoveOrganization() throws Exception {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 0);
    Organization organization = tempEntity.newOrganization();

    HttpResponse response = restRequest().path(ApiOrganizationResourceV2.MOVE_ORGANIZATION_PATH)
        .parameter(organizations.get(0).getId(), organization.getId())
        .query("failEarlyOnError", true)
        .put();

    assertResponseStatus(HttpStatus.SC_OK, response);
    assertThat(response.getBodyBytes()).isNotNull();

    MoveOrganizationResponseDTO moveOrganizationResponseDTO = response.getBody(MoveOrganizationResponseDTO.class);
    assertThat(moveOrganizationResponseDTO).isNotNull();
  }

  @Test
  public void testMoveOrganization_BadRequest() throws Exception {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 0);
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organizations.get(0).getId());
    Tag tag = tempEntity.newTag(organizations.get(1).getId());
    tempEntity.newApplicationTag(application.getId(), tag.getId());

    HttpResponse response = restRequest()
        .path(ApiOrganizationResourceV2.MOVE_ORGANIZATION_PATH)
        .parameter(organizations.get(0).getId(), organization.getId())
        .query("failEarlyOnError", false)
        .put();

    MoveOrganizationResponseDTO moveOrganizationResponseDTO = response.getBody(MoveOrganizationResponseDTO.class);
    assertResponseStatus(HttpStatus.SC_CONFLICT, response);
    assertThat(moveOrganizationResponseDTO.errors).isNotEmpty();
    assertThat(moveOrganizationResponseDTO.errors.get(0).type).isEqualTo(MoveOrganizationValidationErrorType.TAG);
    assertThat(moveOrganizationResponseDTO.errors.get(0).message)
        .contains("Missing application categories for new parent org " + organization.getName())
        .contains(tag.getName());
  }

  @Test
  public void testMoveOrganization_BadRequestFailEarlyOnError() throws Exception {
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(1, 2, 0);
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organizations.get(0).getId());
    Tag tag = tempEntity.newTag(organizations.get(1).getId());
    tempEntity.newApplicationTag(application.getId(), tag.getId());

    HttpResponse response = restRequest()
        .path(ApiOrganizationResourceV2.MOVE_ORGANIZATION_PATH)
        .parameter(organizations.get(0).getId(), organization.getId())
        .query("failEarlyOnError", true)
        .put();

    assertResponseStatus(HttpStatus.SC_CONFLICT, response);
    assertThat(response.getBodyText()).isEqualTo(
        String.format("Missing application categories for new parent org %s: %s", organization.getName(),
            tag.getName()));
  }

  @Test
  public void testMoveOrganization_Unlicensed() throws Exception {
    uninstallLicense();

    HttpResponse response = restRequest()
        .path(ApiOrganizationResourceV2.MOVE_ORGANIZATION_PATH)
        .parameter("org-id-does-not-matter", "destination-org-id-does-not-matter")
        .query("failEarlyOnError", true)
        .put();
    assertResponseStatus(HttpStatus.SC_PAYMENT_REQUIRED, response);
  }

  @Test
  public void testDeleteOrganization() throws Exception {
    Organization organization = tempEntity.newOrganization();
    Organization otherOrganization = tempEntity.newOrganization();

    HttpResponse response = restRequest()
        .path(ApiOrganizationResourceV2.ORGANIZATION_ID)
        .parameter(organization.getId())
        .delete();

    assertResponseStatus(HttpStatus.SC_NO_CONTENT, response);
    assertThat(organizationDAO.getById(organization.getId())).isNull();
    assertThat(organizationDAO.getById(otherOrganization.getId())).isNotNull();
  }

  private void assertOrganizationData(
      ApiOrganizationDTO apiOrganizationDTO,
      Organization organization,
      List<Tag> tags)
  {
    assertThat(apiOrganizationDTO.id).isEqualTo(organization.getId());
    assertThat(apiOrganizationDTO.name).isEqualTo(organization.getName());
    assertThat(apiOrganizationDTO.tags).hasSize(tags.size());
    apiOrganizationDTO.tags.forEach(apiTagDTO -> assertTagData(apiTagDTO, tags));
  }

  private void assertTagData(ApiTagDTO apiTagDTO, List<Tag> tags) {
    Tag tag = tags.stream().filter(t -> t.getId().equals(apiTagDTO.id)).findFirst().orElse(null);
    assertThat(tag).isNotNull();
    assertThat(apiTagDTO.id).isEqualTo(tag.getId());
    assertThat(apiTagDTO.name).isEqualTo(tag.getName());
    assertThat(apiTagDTO.description).isEqualTo(tag.getDescription());
    assertThat(apiTagDTO.color).isEqualTo(tag.getColor());
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.ORG_RESOURCE_PATH);
  }
}
