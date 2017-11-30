/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationListDTO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ApiOrganizationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiOrganizationService apiOrganizationService;

  @Test
  public void testGetAll_Authorized() {
    grantReadPermission(org.getId());
    ApiOrganizationListDTO apiOrganizationListDTO = apiOrganizationService.getAll();
    assertThat(apiOrganizationListDTO, notNullValue());
    assertThat(apiOrganizationListDTO.organizations, hasSize(1));
    assertThat(apiOrganizationListDTO.organizations.get(0).id, is(org.getId()));
    assertThat(apiOrganizationListDTO.organizations.get(0).name, is(org.getName()));
  }

  @Test
  public void testGetAll_Unauthenticated() {
    ApiOrganizationListDTO apiOrganizationListDTO = apiOrganizationService.getAll();
    assertThat(apiOrganizationListDTO, notNullValue());
    assertThat(apiOrganizationListDTO.organizations, hasSize(0));
  }

  @Test
  public void testGetAll_UnauthorizedButAuthenticated() {
    login();
    ApiOrganizationListDTO apiOrganizationListDTO = apiOrganizationService.getAll();
    assertThat(apiOrganizationListDTO, notNullValue());
    assertThat(apiOrganizationListDTO.organizations, hasSize(0));
  }

  @Test
  public void testAddOrganization_Authorized() {
    grantWritePermission();
    ApiOrganizationDTO apiOrganizationDTO = new ApiOrganizationDTO(null, "testOrganizationName");
    ApiOrganizationDTO newOrganizationDTO = apiOrganizationService.addOrganization(apiOrganizationDTO);

    OrganizationDAO organizationDAO = new OrganizationDAO();
    Organization organization = organizationDAO.getByIdNotNull(newOrganizationDTO.id);
    organizationDAO.delete(organization);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddOrganization_Unauthenticated() {
    ApiOrganizationDTO apiOrganizationDTO = new ApiOrganizationDTO(null, "testOrganizationName");
    apiOrganizationService.addOrganization(apiOrganizationDTO);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddOrganization_UnauthorizedButAuthenticated() {
    login();
    ApiOrganizationDTO apiOrganizationDTO = new ApiOrganizationDTO(null, "testOrganizationName");
    apiOrganizationService.addOrganization(apiOrganizationDTO);
  }
}
