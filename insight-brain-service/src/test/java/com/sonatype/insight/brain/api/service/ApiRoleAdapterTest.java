/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.service;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.api.dto.ApiRoleDTO;
import com.sonatype.insight.brain.api.dto.ApiRoleListDTO;
import com.sonatype.insight.brain.model.security.Role;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;

public class ApiRoleAdapterTest
{
  private ApiRoleAdapter roleAdapter = new ApiRoleAdapter();

  @Test
  public void testConvertToDTO() {
    List<Role> roles = new ArrayList<>();
    Role role = new Role();
    role.setId("testRoleId");
    role.setName("testRoleName");
    role.setDescription("testRoleDescription");
    roles.add(role);

    ApiRoleListDTO apiRoleListDTO = roleAdapter.convertToDTO(roles);
    assertThat(apiRoleListDTO, notNullValue());
    assertThat(apiRoleListDTO.roles, hasSize(1));

    ApiRoleDTO apiRoleDTO = apiRoleListDTO.roles.get(0);
    assertThat(apiRoleDTO.id, is(role.getId()));
    assertThat(apiRoleDTO.name, is(role.getName()));
    assertThat(apiRoleDTO.description, is(role.getDescription()));
  }
}
