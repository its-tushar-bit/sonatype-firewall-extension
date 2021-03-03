/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.legal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

import org.apache.commons.lang3.StringUtils;

/**
 * @since 1.105
 */
@Entity
@Table(name = "legal_file_override")
public class LegalFileOverride
    implements HasStringId
{
  @Id
  @Column(name = "legal_file_override_id")
  private String id;

  @Column(name = "type")
  @Enumerated(EnumType.STRING)
  private LegalFileType type;

  @Column(name = "original_content_hash")
  private String originalContentHash;

  @Column(name = "content_hash")
  private String contentHash;

  @Column(name = "content")
  private String content;

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private ComponentLegalPartStatus status;

  @Column(name = "component_legal_file_id")
  private String componentLegalFileId;

  public LegalFileOverride() {
  }

  public LegalFileOverride(
      LegalFileType type,
      String originalContentHash,
      String contentHash,
      String content,
      ComponentLegalPartStatus status,
      String componentLegalFileId)
  {
    this.type = type;
    this.originalContentHash = originalContentHash;
    this.contentHash = contentHash;
    this.content = content;
    this.status = status;
    this.componentLegalFileId = componentLegalFileId;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public LegalFileType getType() {
    return type;
  }

  public void setType(LegalFileType type) {
    this.type = type;
  }

  public String getOriginalContentHash() {
    return originalContentHash;
  }

  public String getContentHash() {
    return contentHash;
  }

  public void setContentHash(String contentHash) {
    this.contentHash = contentHash;
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

  public String getComponentLegalFileId() {
    return componentLegalFileId;
  }

  public void setComponentLegalFileId(String componentLegalFileId) {
    this.componentLegalFileId = componentLegalFileId;
  }

  public boolean isUserCreated() {
    return StringUtils.isBlank(originalContentHash);
  }
}
