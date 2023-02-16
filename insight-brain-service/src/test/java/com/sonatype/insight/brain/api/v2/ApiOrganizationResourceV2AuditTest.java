/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.security.AbstractMembershipMappingAuditTest;

import org.junit.Test;

public class ApiOrganizationResourceV2AuditTest
    extends AbstractMembershipMappingAuditTest
{
  @Test
  public void testAddOrganization() throws Exception {
    ApiOrganizationDTO organizationDto = new ApiOrganizationDTO(null, "new-organization");
    organizationApiRequest().body(organizationDto).post();
    Organization organization = new OrganizationDAO().getByName(organizationDto.name);
    tempEntity.register(organization);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_ORGANIZATION, null);
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  public void testAddOrganization_Unauthorized() throws Exception {
    ApiOrganizationDTO organizationDto = new ApiOrganizationDTO(null, "new-organization");
    organizationApiRequest().with(unauthorizedUser()).body(organizationDto).post();

    assertAuditLog(AuditEvent.CREATE_ORGANIZATION, "unauthorized");
  }

  private HttpRequest organizationApiRequest() {
    return restRequest().path(PublicApiPaths.ORG_RESOURCE_PATH);
  }
}
