/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.util.Objects;

/**
 * @since 1.81
 */
public class ThirdPartyLicenseDTO
    implements Comparable<ThirdPartyLicenseDTO>

{
  public String id;

  public String name;

  public String url;

  @Override
  public int compareTo(ThirdPartyLicenseDTO o) {
    return id.compareTo(o.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, url);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ThirdPartyLicenseDTO)) {
      return false;
    }
    ThirdPartyLicenseDTO other = (ThirdPartyLicenseDTO) obj;
    return Objects.equals(id, other.id) && Objects.equals(name, other.name) && Objects.equals(url, other.url);
  }
}
