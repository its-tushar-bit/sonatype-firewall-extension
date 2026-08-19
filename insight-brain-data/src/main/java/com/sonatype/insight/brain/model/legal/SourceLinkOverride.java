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

/**
 * @since 1.133
 */
@Entity
@Table(name = "source_link_override")
public class SourceLinkOverride
    implements HasStringId
{
  @Id
  @Column(name = "source_link_override_id")
  private String id;

  @Column(name = "content")
  private String content;

  @Column(name = "original_content")
  private String originalContent;

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private ComponentLegalPartStatus status;

  @Column(name = "component_source_link_id")
  private String componentSourceLinkId;

  public SourceLinkOverride() {
  }

  public SourceLinkOverride(
      String content,
      ComponentLegalPartStatus status,
      String componentSourceLinkId)
  {
    this.content = content;
    this.originalContent = content;
    this.status = status;
    this.componentSourceLinkId = componentSourceLinkId;
  }

  public SourceLinkOverride(
      String content,
      String originalContent,
      ComponentLegalPartStatus status,
      String componentSourceLinkId)
  {
    this.content = content;
    this.originalContent = originalContent;
    this.status = status;
    this.componentSourceLinkId = componentSourceLinkId;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getOriginalContent() {
    return originalContent;
  }

  public void setOriginalContent(String originalContent) {
    this.originalContent = originalContent;
  }

  public ComponentLegalPartStatus getStatus() {
    return status;
  }

  public void setStatus(ComponentLegalPartStatus status) {
    this.status = status;
  }

  public String getComponentSourceLinkId() {
    return componentSourceLinkId;
  }

  public void setComponentSourceLinkId(String componentSourceLinkId) {
    this.componentSourceLinkId = componentSourceLinkId;
  }

  /**
   * Returns always true because SourceLinkOverride always is a custom entry created by the user, that is a Source Link
   * statement not found in HDS.
   *
   * @return true, always is user created.
   */
  public boolean isUserCreated() {
    return true;
  }
}
