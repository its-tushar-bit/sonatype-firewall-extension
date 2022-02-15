/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.api.v2.dto.ApiUserDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiUserListDTO;

/**
 * Resource for API User
 */
public interface ApiUserResource
{
  ApiUserListDTO getAll(String realmId);

  ApiUserDTO get(String username);

  void add(ApiUserDTO userDTO);

  ApiUserDTO update(String username, ApiUserDTO userDTO);

  void delete(String username, String realmId);
}
