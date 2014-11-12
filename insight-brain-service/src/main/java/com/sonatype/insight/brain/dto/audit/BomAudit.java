/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dto.audit;


import com.sonatype.clm.dto.model.component.ComponentIdentifier;

/**
 * DTO class for records in the BOM audit logs.
 * 
 * @since 1.6
 */
public class BomAudit
  extends Auditable
{
  private boolean modified;

  public BomAudit() {
  }

  public BomAudit(final ComponentIdentifier componentIdentifier, final boolean modified) {
    setComponentIdentifier(componentIdentifier);
    this.modified = modified;
  }

  public boolean isModified() {
    return modified;
  }

  public void setModified(boolean modified) {
    this.modified = modified;
  }
}
