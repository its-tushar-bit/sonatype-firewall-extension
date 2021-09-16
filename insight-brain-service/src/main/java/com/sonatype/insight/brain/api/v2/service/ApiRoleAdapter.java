/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.Collection;

import com.sonatype.insight.brain.api.v2.dto.ApiRoleDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleListDTO;
import com.sonatype.insight.brain.model.security.Role;

/**
 * @since 1.11.0
 */
public class ApiRoleAdapter
{
  public static ApiRoleListDTO convertToDTO(Collection<Role> roles) {
    ApiRoleListDTO roleListDTO = new ApiRoleListDTO();
    roleListDTO.roles = new ArrayList<>();
    for (Role role : roles) {
      roleListDTO.roles.add(convertToDTO(role));
    }
    return roleListDTO;
  }

  private static ApiRoleDTO convertToDTO(Role role) {
    ApiRoleDTO roleDTO = new ApiRoleDTO();
    roleDTO.id = role.getId();
    roleDTO.name = role.getName();
    roleDTO.description = role.getDescription();
    return roleDTO;
  }
}
