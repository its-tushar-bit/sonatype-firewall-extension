/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.Objects;

import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.SourceLinkOverride;

/**
 * @since 1.133
 */
public class SourceLinkOverrideDTO
{
  private String id;

  private String content;

  private ComponentLegalPartStatus status;

  public SourceLinkOverrideDTO() {
  }

  public SourceLinkOverrideDTO(
      final String id,
      final String content,
      final ComponentLegalPartStatus status)
  {
    this.id = id;
    this.content = content;
    this.status = status;
  }

  public static SourceLinkOverrideDTO fromSourceLinkOverride(SourceLinkOverride sourceLinkOverride) {
    return new SourceLinkOverrideDTO(
        sourceLinkOverride.getId(), sourceLinkOverride.getContent(), sourceLinkOverride.getStatus());
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getContent() {
    return content;
  }

  public void setContent(final String content) {
    this.content = content;
  }

  public ComponentLegalPartStatus getStatus() {
    return status;
  }

  public void setStatus(final ComponentLegalPartStatus status) {
    this.status = status;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SourceLinkOverrideDTO that = (SourceLinkOverrideDTO) o;
    return Objects.equals(getId(), that.getId()) &&
        Objects.equals(getContent(), that.getContent()) && getStatus() == that.getStatus();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getContent(), getStatus());
  }
}
