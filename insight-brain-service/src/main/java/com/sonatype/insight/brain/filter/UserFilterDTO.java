/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.filter;

import java.io.IOException;
import java.io.UncheckedIOException;

import com.sonatype.insight.brain.model.filter.UserFilter;
import com.sonatype.insight.brain.model.filter.UserFilterType;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;

import static com.sonatype.insight.brain.model.filter.UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD;

public class UserFilterDTO
{
  private String name;

  private String basedOnFilterName;

  private UserFilterData filter;

  private UserFilterType type;

  public UserFilterDTO(UserFilter userFilter) {
    name = userFilter.getName();
    basedOnFilterName = userFilter.getBasedOnFilterName();
    type = userFilter.getType();
    filter = buildFilter(userFilter.getFilter());
  }

  public UserFilterDTO(String name, String basedOnFilterName, UserFilterType type, UserFilterData filter) {
    this.name = name;
    this.basedOnFilterName = basedOnFilterName;
    this.filter = filter;
    this.type = type;
  }

  @JsonCreator
  public UserFilterDTO(
      @JsonProperty("name") String name,
      @JsonProperty("basedOnFilterName") String basedOnFilterName,
      @JsonProperty("type") UserFilterType type,
      @JsonProperty("filter") JsonNode filter)
  {
    this.name = name;
    this.basedOnFilterName = basedOnFilterName;
    this.type = type;
    this.filter = buildFilter(filter.toString());
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getBasedOnFilterName() {
    return basedOnFilterName;
  }

  public void setBasedOnFilterName(String basedOnFilterName) {
    this.basedOnFilterName = basedOnFilterName;
  }

  public UserFilterData getFilter() {
    return filter;
  }

  public UserFilterType getType() {
    return type;
  }

  public void setType(UserFilterType type) {
    this.type = type;
  }

  private UserFilterData buildFilter(String jsonFilter) {
    if (StringUtils.isNoneBlank(jsonFilter)) {
      try {
        if (type.equals(ADVANCED_LEGAL_PACK_DASHBOARD)) {
          return JsonUtils.parse(jsonFilter, AdvancedLegalPackDashboardFilter.class);
        }
        else {
          throw new IllegalArgumentException("Invalid user filter type: " + type);
        }
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    return null;
  }
}
