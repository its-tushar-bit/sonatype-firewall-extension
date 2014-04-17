/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.service;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.dto.ApiOrganizationListDTO;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

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
    assertThat(apiOrganizationListDTO.getOrganizations(), hasSize(1));
    assertThat(apiOrganizationListDTO.getOrganizations().get(0).getId(), is(org.getId()));
    assertThat(apiOrganizationListDTO.getOrganizations().get(0).getName(), is(org.getName()));
  }

  @Test
  public void testGetAll_Unauthenticated() {
    ApiOrganizationListDTO apiOrganizationListDTO = apiOrganizationService.getAll();
    assertThat(apiOrganizationListDTO, notNullValue());
    assertThat(apiOrganizationListDTO.getOrganizations(), hasSize(0));
  }

  @Test
  public void testGetAll_UnauthorizedButAuthenticated() {
    login();
    ApiOrganizationListDTO apiOrganizationListDTO = apiOrganizationService.getAll();
    assertThat(apiOrganizationListDTO, notNullValue());
    assertThat(apiOrganizationListDTO.getOrganizations(), hasSize(0));
  }
}
