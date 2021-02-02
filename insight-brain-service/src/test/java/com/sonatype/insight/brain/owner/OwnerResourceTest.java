/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.owner;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dto.ApplicableContext;
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
    return restRequest().path(OwnerResource.RESOURCE_PATH).parameter(owner.getId());
  }

  @Test
  public void testGetHierarchy_RootOrganization() throws Exception {
    Owner rootOrganization = ownerDAO.getById(Organization.ROOT_ORGANIZATION_ID);

    HttpResponse response = restRequest(rootOrganization).get();

    assertResponseStatus(200, response);
    ApplicableContext rootApplicableContext = response.getBody(ApplicableContext.class);
    assertThat(rootApplicableContext).isNotNull();
    assertThat(rootApplicableContext.getId()).isEqualTo(rootOrganization.getPublicId());
    assertThat(rootApplicableContext.getName()).isEqualTo(rootOrganization.getName());
    assertThat(rootApplicableContext.getType()).isEqualTo(rootOrganization.getType());
    assertThat(rootApplicableContext.getChildren()).isNull();
  }

  @Test
  public void testGetHierarchy_Organization() throws Exception {
    Owner rootOrganization = ownerDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    Owner organization = tempEntity.newOrganization();

    HttpResponse response = restRequest(organization).get();

    assertResponseStatus(200, response);
    ApplicableContext rootApplicableContext = response.getBody(ApplicableContext.class);
    assertThat(rootApplicableContext).isNotNull();
    assertThat(rootApplicableContext.getId()).isEqualTo(rootOrganization.getPublicId());
    assertThat(rootApplicableContext.getName()).isEqualTo(rootOrganization.getName());
    assertThat(rootApplicableContext.getType()).isEqualTo(rootOrganization.getType());
    assertThat(rootApplicableContext.getChildren()).hasSize(1);
    ApplicableContext orgApplicableContext = rootApplicableContext.getChildren().get(0);
    assertThat(orgApplicableContext.getId()).isEqualTo(organization.getPublicId());
    assertThat(orgApplicableContext.getName()).isEqualTo(organization.getName());
    assertThat(orgApplicableContext.getType()).isEqualTo(organization.getType());
    assertThat(orgApplicableContext.getChildren()).isNull();
  }

  @Test
  public void testGetHierarchy_Application() throws Exception {
    Owner rootOrganization = ownerDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    Owner organization = tempEntity.newOrganization();
    Owner application = tempEntity.newApplication(organization.getId());

    HttpResponse response = restRequest(application).get();

    assertResponseStatus(200, response);
    ApplicableContext rootApplicableContext = response.getBody(ApplicableContext.class);
    assertThat(rootApplicableContext).isNotNull();
    assertThat(rootApplicableContext.getId()).isEqualTo(rootOrganization.getPublicId());
    assertThat(rootApplicableContext.getName()).isEqualTo(rootOrganization.getName());
    assertThat(rootApplicableContext.getType()).isEqualTo(rootOrganization.getType());
    assertThat(rootApplicableContext.getChildren()).hasSize(1);
    ApplicableContext orgApplicableContext = rootApplicableContext.getChildren().get(0);
    assertThat(orgApplicableContext.getId()).isEqualTo(organization.getPublicId());
    assertThat(orgApplicableContext.getName()).isEqualTo(organization.getName());
    assertThat(orgApplicableContext.getType()).isEqualTo(organization.getType());
    assertThat(orgApplicableContext.getChildren()).hasSize(1);
    ApplicableContext appApplicableContext = orgApplicableContext.getChildren().get(0);
    assertThat(appApplicableContext.getId()).isEqualTo(application.getPublicId());
    assertThat(appApplicableContext.getName()).isEqualTo(application.getName());
    assertThat(appApplicableContext.getType()).isEqualTo(application.getType());
    assertThat(appApplicableContext.getChildren()).isNull();
  }
}
