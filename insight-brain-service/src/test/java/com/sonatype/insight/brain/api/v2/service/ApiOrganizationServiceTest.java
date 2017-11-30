/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationDTO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;

public class ApiOrganizationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiOrganizationService apiOrganizationService;

  @Inject
  private OrganizationDAO organizationDAO;

  @Test
  public void testAddOrganization() {
    final String ORGANIZATION_NAME = "testName";

    ApiOrganizationDTO apiOrganizationDTO = new ApiOrganizationDTO(null, ORGANIZATION_NAME);
    ApiOrganizationDTO newOrganizationDTO = apiOrganizationService.addOrganization(apiOrganizationDTO);

    Organization organization = organizationDAO.getByIdNotNull(newOrganizationDTO.id);
    tempEntity.register(organization);

    assertThat(organization.getName(), is(ORGANIZATION_NAME));
    assertThat(organization.getParentOrganizationId(), is(Organization.ROOT_ORGANIZATION_ID));

    assertThat(newOrganizationDTO.id, not(isEmptyOrNullString()));
    assertThat(newOrganizationDTO.name, is(ORGANIZATION_NAME));
    assertThat(newOrganizationDTO.tags, hasSize(0));
  }

  @Test
  public void testAddOrganization_TagsNotSupported() {
    final String ORGANIZATION_NAME = "testName";

    ApiOrganizationDTO apiOrganizationDTO = new ApiOrganizationDTO(null, ORGANIZATION_NAME);
    apiOrganizationDTO.tags = Collections.emptyList();

    try {
      apiOrganizationService.addOrganization(apiOrganizationDTO);
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Organization must not have tags set on creation."));
    }

    assertThat(organizationDAO.getByName(ORGANIZATION_NAME), nullValue());
  }

  @Test
  public void testAddOrganization_IdNotSupported() {
    final String ORGANIZATION_NAME = "testName";
    
    ApiOrganizationDTO apiOrganizationDTO = new ApiOrganizationDTO("testId", ORGANIZATION_NAME);

    try {
      apiOrganizationService.addOrganization(apiOrganizationDTO);
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Organization must not have an ID set on creation."));
    }

    assertThat(organizationDAO.getByName(ORGANIZATION_NAME), nullValue());
  }
}
