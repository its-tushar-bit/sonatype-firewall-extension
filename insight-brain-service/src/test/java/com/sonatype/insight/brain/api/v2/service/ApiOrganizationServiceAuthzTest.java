/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationListDTO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiOrganizationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private ApiOrganizationService apiOrganizationService;

  @Test
  public void testGetOrganizations_Authorized() {
    grantReadPermission(org.getId());
    ApiOrganizationListDTO apiOrganizationListDTO = apiOrganizationService.getOrganizations(Collections.emptySet());
    assertThat(apiOrganizationListDTO).isNotNull();
    assertThat(apiOrganizationListDTO.organizations).hasSize(1);
    assertThat(apiOrganizationListDTO.organizations.get(0).id).isEqualTo(org.getId());
    assertThat(apiOrganizationListDTO.organizations.get(0).name).isEqualTo(org.getName());
  }

  @Test
  public void testGetOrganizations_Filtered_Authorized() {
    Organization org2 = tempEntity.newOrganization();

    ApiOrganizationListDTO apiOrganizationListDTO =
        apiOrganizationService.getOrganizations(Collections.singleton(org2.getName()));
    assertThat(apiOrganizationListDTO).isNotNull();
    assertThat(apiOrganizationListDTO.organizations).isEmpty();
  }

  @Test
  public void testGetOrganizations_Unauthenticated() {
    ApiOrganizationListDTO apiOrganizationListDTO = apiOrganizationService.getOrganizations(Collections.emptySet());
    assertThat(apiOrganizationListDTO).isNotNull();
    assertThat(apiOrganizationListDTO.organizations).isEmpty();
  }

  @Test
  public void testGetOrganizations_Unauthorized() {
    login();
    ApiOrganizationListDTO apiOrganizationListDTO = apiOrganizationService.getOrganizations(Collections.emptySet());
    assertThat(apiOrganizationListDTO).isNotNull();
    assertThat(apiOrganizationListDTO.organizations).isEmpty();
  }

  @Test
  public void testAddOrganization_Authorized() {
    grantWritePermission();
    ApiOrganizationDTO apiOrganizationDTO = new ApiOrganizationDTO(null, "testOrganizationName");
    ApiOrganizationDTO newOrganizationDTO = apiOrganizationService.addOrganization(apiOrganizationDTO);

    organizationDAO.getByIdNotNull(newOrganizationDTO.id);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddOrganization_Unauthenticated() {
    ApiOrganizationDTO apiOrganizationDTO = new ApiOrganizationDTO(null, "testOrganizationName");
    apiOrganizationService.addOrganization(apiOrganizationDTO);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddOrganization_Unauthorized() {
    login();
    ApiOrganizationDTO apiOrganizationDTO = new ApiOrganizationDTO(null, "testOrganizationName");
    apiOrganizationService.addOrganization(apiOrganizationDTO);
  }

  @Test
  public void testGetOrganizationById_Authorized() {
    grantReadPermission(org.getId());
    apiOrganizationService.getOrganizationById(org.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetOrganizationById_Unauthorized() {
    login();
    apiOrganizationService.getOrganizationById(org.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetOrganizationById_Unauthenticated() {
    apiOrganizationService.getOrganizationById(org.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteOrganization_Unauthenticated() throws Exception {
    apiOrganizationService.deleteOrganization(org.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteOrganization_Unauthorized() throws Exception {
    login();
    apiOrganizationService.deleteOrganization(org.getId());
  }

  @Test
  public void testDeleteOrganization_Authorized() throws Exception {
    grantWritePermission(org.getId());
    apiOrganizationService.deleteOrganization(org.getId());
  }
}
