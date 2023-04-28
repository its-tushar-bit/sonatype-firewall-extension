/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Set;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationListDTO;

/**
 * Resource for API Organizations
 */
public interface ApiOrganizationResourceV2
{
  ApiOrganizationListDTO getOrganizations(Set<String> organizationNames);

  /**
   * @since 1.81
   */
  ApiOrganizationDTO getOrganization(String organizationId);

  /**
   * @since 1.42
   */
  ApiOrganizationDTO addOrganization(ApiOrganizationDTO organizationDTO);

  /**
   * @since 1.161
   */
  Response moveOrganization(String orgId, String newParentOrgId, boolean failEarlyOnError);
}
