/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.LegalFileOverride;

/**
 * @since 1.107
 */
public class LegalFileOverrideDTO
{
  private String id;

  private String originalContentHash;

  private String content;

  private ComponentLegalPartStatus status;

  public LegalFileOverrideDTO() {
    // for jackson
  }

  public LegalFileOverrideDTO(
      String originalContentHash,
      String content,
      ComponentLegalPartStatus status)
  {
    this.originalContentHash = originalContentHash;
    this.content = content;
    this.status = status;
  }

  public LegalFileOverrideDTO(LegalFileOverride legalFileOverride) {
    id = legalFileOverride.getId();
    originalContentHash = legalFileOverride.getOriginalContentHash();
    content = legalFileOverride.getContent();
    status = legalFileOverride.getStatus();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getOriginalContentHash() {
    return originalContentHash;
  }

  public void setOriginalContentHash(String originalContentHash) {
    this.originalContentHash = originalContentHash;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public ComponentLegalPartStatus getStatus() {
    return status;
  }

  public void setStatus(ComponentLegalPartStatus status) {
    this.status = status;
  }
}
