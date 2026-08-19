/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.legal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

import org.apache.commons.lang3.StringUtils;

/**
 * @since 1.105
 */
@Entity
@Table(name = "copyright_override")
public class CopyrightOverride
    implements HasStringId
{
  @Id
  @Column(name = "copyright_override_id")
  private String id;

  @Column(name = "original_content_hash")
  private String originalContentHash;

  @Column(name = "content_hash")
  private String contentHash;

  @Column(name = "content")
  private String content;

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private ComponentLegalPartStatus status;

  @Column(name = "component_copyright_id")
  private String componentCopyrightId;

  public CopyrightOverride() {
  }

  public CopyrightOverride(
      String originalContentHash,
      String contentHash,
      String content,
      ComponentLegalPartStatus status,
      String componentCopyrightId)
  {
    this.originalContentHash = originalContentHash;
    this.contentHash = contentHash;
    this.content = content;
    this.status = status;
    this.componentCopyrightId = componentCopyrightId;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getOriginalContentHash() {
    return originalContentHash;
  }

  public void setOriginalContentHash(String originalContentHash) {
    this.originalContentHash = originalContentHash;
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

  public String getComponentCopyrightId() {
    return componentCopyrightId;
  }

  public void setComponentCopyrightId(String componentCopyrightId) {
    this.componentCopyrightId = componentCopyrightId;
  }

  /**
   * Returns true if this CopyrightOverride is a custom entry created by the user, that is a copyright statement not
   * found in HDS.
   *
   * @return true if user created, false otherwise.
   */
  public boolean isUserCreated() {
    return StringUtils.isBlank(originalContentHash);
  }
}
