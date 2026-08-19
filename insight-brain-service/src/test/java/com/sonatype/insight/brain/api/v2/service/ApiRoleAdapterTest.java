/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.ApiRoleDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleListDTO;
import com.sonatype.insight.brain.model.security.Role;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiRoleAdapterTest
{
  @Test
  public void testConvertToDTO() {
    List<Role> roles = new ArrayList<>();
    Role role = new Role();
    role.setId("testRoleId");
    role.setName("testRoleName");
    role.setDescription("testRoleDescription");
    roles.add(role);

    ApiRoleListDTO apiRoleListDTO = ApiRoleAdapter.convertToDTO(roles);
    assertThat(apiRoleListDTO).isNotNull();
    assertThat(apiRoleListDTO.roles).hasSize(1);

    ApiRoleDTO apiRoleDTO = apiRoleListDTO.roles.get(0);
    assertThat(apiRoleDTO.id).isEqualTo(role.getId());
    assertThat(apiRoleDTO.name).isEqualTo(role.getName());
    assertThat(apiRoleDTO.description).isEqualTo(role.getDescription());
  }
}
