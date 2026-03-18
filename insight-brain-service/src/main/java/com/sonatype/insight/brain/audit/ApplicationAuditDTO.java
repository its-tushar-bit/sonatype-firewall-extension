/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.util.Objects;

import com.sonatype.insight.brain.model.Application;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class ApplicationAuditDTO
{
  public String applicationId;

  public String applicationPublicId;

  public String applicationName;

  public ApplicationAuditDTO() {
    // for jackson
  }

  public ApplicationAuditDTO(String applicationId, Application application) {
    this.applicationId = applicationId;
    if (application != null) {
      this.applicationPublicId = application.getPublicId();
      this.applicationName = application.getName();
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
    ApplicationAuditDTO that = (ApplicationAuditDTO) o;
    return Objects.equals(applicationId, that.applicationId) &&
        Objects.equals(applicationPublicId, that.applicationPublicId) &&
        Objects.equals(applicationName, that.applicationName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(applicationId, applicationPublicId, applicationName);
  }
}
