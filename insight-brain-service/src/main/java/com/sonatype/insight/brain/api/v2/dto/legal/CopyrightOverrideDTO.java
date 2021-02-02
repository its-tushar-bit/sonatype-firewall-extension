/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.Objects;

import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.CopyrightOverride;

/**
 * @since 1.105
 */
public class CopyrightOverrideDTO
{
  private String id;

  private String originalContentHash;

  private String content;

  private ComponentLegalPartStatus status;

  public CopyrightOverrideDTO() {
  }

  public CopyrightOverrideDTO(
      final String id,
      final String originalContentHash,
      final String content,
      final ComponentLegalPartStatus status)
  {
    this.id = id;
    this.originalContentHash = originalContentHash;
    this.content = content;
    this.status = status;
  }

  public static CopyrightOverrideDTO fromCopyrightOverride(CopyrightOverride copyrightOverride) {
    return new CopyrightOverrideDTO(
        copyrightOverride.getId(),
        copyrightOverride.getOriginalContentHash(),
        copyrightOverride.getContent(),
        copyrightOverride.getStatus());
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getOriginalContentHash() {
    return originalContentHash;
  }

  public void setOriginalContentHash(final String originalContentHash) {
    this.originalContentHash = originalContentHash;
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
    CopyrightOverrideDTO that = (CopyrightOverrideDTO) o;
    return Objects.equals(getId(), that.getId()) &&
        Objects.equals(getOriginalContentHash(), that.getOriginalContentHash()) &&
        Objects.equals(getContent(), that.getContent()) && getStatus() == that.getStatus();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getOriginalContentHash(), getContent(), getStatus());
  }
}
