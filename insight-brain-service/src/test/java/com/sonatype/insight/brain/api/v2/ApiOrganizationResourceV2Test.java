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
import com.sonatype.insight.brain.configuration.ldap.TestLdapServer;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiOrganizationResourceV2Test
    extends AbstractResourceTest
{
  private Organization organization;

  @Rule
  public TestLdapServer embeddedLdapServer = new TestLdapServer();

  @Before
  public void setUp() {
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
        restRequest().path(DefaultApiOrganizationResourceV2.ORGANIZATION_ID).parameter(organization.getId()).get();

    assertResponseStatus(200, response);
    ApiOrganizationDTO apiOrganizationDTO = response.getBody(ApiOrganizationDTO.class);
    assertThat(apiOrganizationDTO).isNotNull();
    assertOrganizationData(apiOrganizationDTO, organization, Collections.singletonList(tag));
  }

  @Test
  public void testGetOrganization_NotFound() throws Exception {
    HttpResponse response =
        restRequest().path(DefaultApiOrganizationResourceV2.ORGANIZATION_ID).parameter("doesNotExist").get();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Cannot find organization with ID doesNotExist.");
  }

  @Test
  public void testAddOrganization() throws Exception {
    OrganizationDAO organizationDAO = new OrganizationDAO();

    ApiOrganizationDTO requestBody = new ApiOrganizationDTO(null, "test-create-organization");

    HttpRequest request = restRequest().body(requestBody);
    HttpResponse response = request.post();
    assertResponseStatus(200, response);

    ApiOrganizationDTO responseBody = response.getBody(ApiOrganizationDTO.class);
    assertThat(responseBody.id).isNotEmpty();

    Organization organization = organizationDAO.getByIdNotNull(responseBody.id);
    tempEntity.register(organization);

    assertThat(responseBody.name).isEqualTo(requestBody.name);
    assertThat(responseBody.tags).isEmpty();

    assertThat(organization.getName()).isEqualTo(requestBody.name);
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
