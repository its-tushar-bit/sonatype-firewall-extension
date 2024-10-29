/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.remediation;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiComponentOverrideOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiPolicyWaiverOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiSuggestedVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @since 1.64
 */
public class ApiComponentRemediationValueDTO
{
  public List<ApiVersionChangeOptionDTO> versionChanges = new ArrayList<>();

  @JsonInclude(Include.NON_NULL)
  public ApiSuggestedVersionChangeOptionDTO suggestedVersionChange;

  // ignoring the other fields until we release those remediation types
  @JsonIgnore
  public List<ApiPolicyWaiverOptionDTO> policyWaivers = new ArrayList<>();

  @JsonIgnore
  public List<ApiComponentOverrideOptionDTO> componentOverrides = new ArrayList<>();

  /**
   * Return the version type for the remediation suggestion which matches the specified
   * version.
   */
  public ApiVersionChangeOptionType getRemediationType(String version) {
    if (suggestedVersionChange != null && suggestedVersionChange.getData() != null &&
        version.equals(suggestedVersionChange.getData().retrieveVersion())) {
      return suggestedVersionChange.getType();
    }
    for (ApiVersionChangeOptionDTO versionChange : versionChanges) {
      if (versionChange != null && versionChange.getData() != null &&
          version.equals(versionChange.getData().retrieveVersion())) {
        return versionChange.getType();
      }
    }
    return null;
  }
}
