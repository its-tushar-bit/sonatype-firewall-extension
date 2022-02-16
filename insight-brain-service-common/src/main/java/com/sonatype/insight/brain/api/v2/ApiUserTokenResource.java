/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import javax.naming.NamingException;

import com.sonatype.insight.brain.api.v2.dto.ApiUserTokenDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiUserTokenExistsDTO;

/**
 * Resource for API User Tokens
 */
public interface ApiUserTokenResource
{
  /**
   * @since 1.102
   */
  ApiUserTokenExistsDTO getUserTokenExistsForCurrentUser();

  /**
   * Only returns ApiUserTokenDTO#userCode populated - passCode is not returned.
   *
   * @param createdAfter  Expected format: yyyy-MM-dd (For example: 2019-09-03)
   * @param createdBefore Expected format: yyyy-MM-dd (For example: 2019-09-03)
   * @since 1.87
   */
  List<ApiUserTokenDTO> getUserTokensByCreatedBetweenAndRealmId(
      String createdAfter,
      String createdBefore,
      String realmId);

  /**
   * @since 1.133
   */
  ApiUserTokenDTO getUserTokenByUsernameAndRealmId(String username, String realmId);

  ApiUserTokenDTO createUserToken();

  void purgeUserTokens() throws NamingException;

  void deleteCurrentUserToken();

  /**
   * @since 1.87
   */
  void deleteUserTokenByUserCode(String userCode);
}
