/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.util.Objects;

import com.sonatype.insight.brain.model.Organization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class OrganizationAuditDTO
{
  public String organizationId;

  public String organizationName;

  public OrganizationAuditDTO() {
    // for jackson
  }

  public OrganizationAuditDTO(String organizationId, Organization organization) {
    this.organizationId = organizationId;
    if (organization != null) {
      this.organizationName = organization.getName();
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OrganizationAuditDTO that = (OrganizationAuditDTO) o;
    return Objects.equals(organizationId, that.organizationId) &&
        Objects.equals(organizationName, that.organizationName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(organizationId, organizationName);
  }
}
