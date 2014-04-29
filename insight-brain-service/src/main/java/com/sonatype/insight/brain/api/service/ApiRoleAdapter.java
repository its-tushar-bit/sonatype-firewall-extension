/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.service;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Named;

import com.sonatype.insight.brain.api.dto.ApiRoleDTO;
import com.sonatype.insight.brain.api.dto.ApiRoleListDTO;
import com.sonatype.insight.brain.model.security.Role;

/**
 * @since 1.11.0
 */
@Named
public class ApiRoleAdapter
{
  public ApiRoleListDTO convertToDTO(Collection<Role> roles) {
    ApiRoleListDTO roleListDTO = new ApiRoleListDTO();
    roleListDTO.roles = new ArrayList<>();
    for (Role role : roles) {
      roleListDTO.roles.add(convertToDTO(role));
    }
    return roleListDTO;
  }

  private ApiRoleDTO convertToDTO(Role role) {
    ApiRoleDTO roleDTO = new ApiRoleDTO();
    roleDTO.id = role.getId();
    roleDTO.name = role.getName();
    roleDTO.description = role.getDescription();
    return roleDTO;
  }
}
