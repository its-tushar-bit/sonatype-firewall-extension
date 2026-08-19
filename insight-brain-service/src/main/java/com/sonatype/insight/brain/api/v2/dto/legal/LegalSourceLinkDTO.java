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

  public String content;

  public String originalContent;

  public ComponentLegalPartStatus status;

  public LegalSourceLinkDTO() {
    // for jackson
  }

  public LegalSourceLinkDTO(
      final String id,
      final String content,
      final String originalContent,
      final ComponentLegalPartStatus status)
  {
    this.id = id;
    this.content = content;
    this.originalContent = originalContent;
    this.status = status;
  }

  public LegalSourceLinkDTO(final String content) {
    this(null, content, content, ComponentLegalPartStatus.ENABLED);
  }

  public LegalSourceLinkDTO(final SourceLinkOverride sourceLink) {
    this(sourceLink.getId(), sourceLink.getContent(), sourceLink.getOriginalContent(), sourceLink.getStatus());
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
    return Objects.equals(id, that.id) && Objects.equals(content, that.content) &&
        Objects.equals(originalContent, that.originalContent) && status == that.status;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, content, originalContent, status);
  }

  @Override
  public String toString() {
    return "LegalSourceLinkDTO{" +
        "id='" + id + '\'' +
        ", content='" + content + '\'' +
        ", originalContent='" + originalContent + '\'' +
        ", status=" + status +
        '}';
  }
}
