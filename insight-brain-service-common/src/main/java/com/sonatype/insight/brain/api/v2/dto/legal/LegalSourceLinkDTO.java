/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.Objects;

import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.SourceLinkOverride;

public class LegalSourceLinkDTO
{
  public String id;

  public String sourceLink;

  public ComponentLegalPartStatus status;

  public LegalSourceLinkDTO() {
    //for jackson
  }

  public LegalSourceLinkDTO(final String id, final String sourceLink, final ComponentLegalPartStatus status) {
    this.id = id;
    this.sourceLink = sourceLink;
    this.status = status;
  }

  public LegalSourceLinkDTO(final String sourceLink) {
    this(null, sourceLink, ComponentLegalPartStatus.ENABLED);
  }

  public LegalSourceLinkDTO(final SourceLinkOverride sourceLink) {
    this(sourceLink.getId(), sourceLink.getContent(), sourceLink.getStatus());
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LegalSourceLinkDTO that = (LegalSourceLinkDTO) o;
    return Objects.equals(id, that.id) && Objects.equals(sourceLink, that.sourceLink) &&
        status == that.status;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, sourceLink, status);
  }

  @Override
  public String toString() {
    return "LegalSourceLinkDTO{" +
        "id='" + id + '\'' +
        ", sourceLink='" + sourceLink + '\'' +
        ", status=" + status +
        '}';
  }
}
