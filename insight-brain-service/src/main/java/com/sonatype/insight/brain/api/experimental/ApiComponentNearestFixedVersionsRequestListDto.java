/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ApiComponentNearestFixedVersionsRequestListDto
{
  private Set<ApiComponentNearestFixedVersionsRequestDto> components = new HashSet<>();

  public Set<ApiComponentNearestFixedVersionsRequestDto> getComponents() {
    return components;
  }

  public void setComponents(Set<ApiComponentNearestFixedVersionsRequestDto> components) {
    this.components = components;
  }

  @Override
  public String toString() {
    return "ApiComponentNearestFixedVersionsRequestListDto [components=" + components + "]";
  }

  public static class ApiComponentNearestFixedVersionsRequestDto
  {
    private String packageUrl;

    public String getPackageUrl() {
      return packageUrl;
    }

    public void setPackageUrl(String packageUrl) {
      this.packageUrl = packageUrl;
    }

    @Override
    public int hashCode() {
      return Objects.hash(packageUrl);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof ApiComponentNearestFixedVersionsRequestDto)) {
        return false;
      }
      ApiComponentNearestFixedVersionsRequestDto other = (ApiComponentNearestFixedVersionsRequestDto) obj;
      return Objects.equals(packageUrl, other.packageUrl);
    }

    @Override
    public String toString() {
      return "ApiComponentNearestFixedVersionsRequestDto [packageUrl=" + packageUrl + "]";
    }
  }
}
