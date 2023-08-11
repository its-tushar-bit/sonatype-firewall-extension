/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.owner;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dto.OwnerHierarchyDTO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OwnerResourceTest
    extends AbstractResourceTest
{
  private final OwnerDAO ownerDAO = new OwnerDAO();

  protected HttpRequest restRequest(Owner owner) {
    return restRequest().path(OwnerResource.RESOURCE_PATH).parameter(owner.getType(), owner.getPublicId());
  }

  @Test
  public void testGetHierarchy_RootOrganization() throws Exception {
    doTestGetHierarchy_RootOrganization("");
  }

  @Test
  public void testGetHierarchyForLegalReviewer_RootOrganization() throws Exception {
    doTestGetHierarchy_RootOrganization(OwnerResource.LEGAL_REVIEWER_PATH);
  }

  private void doTestGetHierarchy_RootOrganization(String path) throws Exception {
    Owner rootOrganization = ownerDAO.getById(Organization.ROOT_ORGANIZATION_ID);

    HttpResponse response = restRequest(rootOrganization).path(path).get();

    assertResponseStatus(200, response);
    OwnerHierarchyDTO rootHierarchy = response.getBody(OwnerHierarchyDTO.class);
    assertThat(rootHierarchy).isNotNull();
    assertThat(rootHierarchy.getId()).isEqualTo(rootOrganization.getId());
    assertThat(rootHierarchy.getPublicId()).isEqualTo(rootOrganization.getPublicId());
    assertThat(rootHierarchy.getName()).isEqualTo(rootOrganization.getName());
    assertThat(rootHierarchy.getType()).isEqualTo(rootOrganization.getType());
    assertThat(rootHierarchy.getChildren()).isNull();
  }

  @Test
  public void testGetHierarchy_Organization() throws Exception {
    doTestGetHierarchy_Organization("");
  }

  @Test
  public void testGetHierarchyForLegalReviewer_Organization() throws Exception {
    doTestGetHierarchy_Organization(OwnerResource.LEGAL_REVIEWER_PATH);
  }

  private void doTestGetHierarchy_Organization(String path) throws Exception {
    Owner rootOrganization = ownerDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    Owner organization = tempEntity.newOrganization();

    HttpResponse response = restRequest(organization).path(path).get();

    assertResponseStatus(200, response);
    OwnerHierarchyDTO rootHierarchy = response.getBody(OwnerHierarchyDTO.class);
    assertThat(rootHierarchy).isNotNull();
    assertThat(rootHierarchy.getId()).isEqualTo(rootOrganization.getPublicId());
    assertThat(rootHierarchy.getPublicId()).isEqualTo(rootOrganization.getPublicId());
    assertThat(rootHierarchy.getName()).isEqualTo(rootOrganization.getName());
    assertThat(rootHierarchy.getType()).isEqualTo(rootOrganization.getType());
    assertThat(rootHierarchy.getChildren()).hasSize(1);
    OwnerHierarchyDTO orgHierarchy = rootHierarchy.getChildren().get(0);
    assertThat(orgHierarchy.getId()).isEqualTo(organization.getId());
    assertThat(orgHierarchy.getPublicId()).isEqualTo(organization.getPublicId());
    assertThat(orgHierarchy.getName()).isEqualTo(organization.getName());
    assertThat(orgHierarchy.getType()).isEqualTo(organization.getType());
    assertThat(orgHierarchy.getChildren()).isNull();
  }

  @Test
  public void testGetHierarchy_Application() throws Exception {
    doTestGetHierarchy_Application("");
  }

  @Test
  public void testGetHierarchyForLegalReviewer_Application() throws Exception {
    doTestGetHierarchy_Application(OwnerResource.LEGAL_REVIEWER_PATH);
  }

  private void doTestGetHierarchy_Application(String path) throws Exception {
    Owner rootOrganization = ownerDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    Owner organization = tempEntity.newOrganization();
    Owner application = tempEntity.newApplication(organization.getId());

    HttpResponse response = restRequest(application).path(path).get();

    assertResponseStatus(200, response);
    OwnerHierarchyDTO rootHierarchy = response.getBody(OwnerHierarchyDTO.class);
    assertThat(rootHierarchy).isNotNull();
    assertThat(rootHierarchy.getId()).isEqualTo(rootOrganization.getId());
    assertThat(rootHierarchy.getPublicId()).isEqualTo(rootOrganization.getPublicId());
    assertThat(rootHierarchy.getName()).isEqualTo(rootOrganization.getName());
    assertThat(rootHierarchy.getType()).isEqualTo(rootOrganization.getType());
    assertThat(rootHierarchy.getChildren()).hasSize(1);
    OwnerHierarchyDTO orgHierarchy = rootHierarchy.getChildren().get(0);
    assertThat(orgHierarchy.getId()).isEqualTo(organization.getId());
    assertThat(orgHierarchy.getPublicId()).isEqualTo(organization.getPublicId());
    assertThat(orgHierarchy.getName()).isEqualTo(organization.getName());
    assertThat(orgHierarchy.getType()).isEqualTo(organization.getType());
    assertThat(orgHierarchy.getChildren()).hasSize(1);
    OwnerHierarchyDTO appHierarchy = orgHierarchy.getChildren().get(0);
    assertThat(appHierarchy.getId()).isEqualTo(application.getId());
    assertThat(appHierarchy.getPublicId()).isEqualTo(appHierarchy.getPublicId());
    assertThat(appHierarchy.getName()).isEqualTo(application.getName());
    assertThat(appHierarchy.getType()).isEqualTo(application.getType());
    assertThat(appHierarchy.getChildren()).isNull();
  }
}
