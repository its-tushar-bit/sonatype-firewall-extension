/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

public class InnerSourceData
{
  private String ownerApplicationName;

  private String ownerApplicationId;

  private String ownerComponentName;

  private boolean innerSource;

  public InnerSourceData() {
  }

  public InnerSourceData(
      final String ownerApplicationName,
      final String ownerApplicationId,
      final String ownerComponentName)
  {
    this.ownerApplicationName = ownerApplicationName;
    this.ownerApplicationId = ownerApplicationId;
    this.ownerComponentName = ownerComponentName;
  }

  public InnerSourceData(
      final String ownerApplicationName,
      final String ownerApplicationId,
      final String ownerComponentName,
      final boolean innerSource)
  {
    this.ownerApplicationName = ownerApplicationName;
    this.ownerApplicationId = ownerApplicationId;
    this.ownerComponentName = ownerComponentName;
    this.innerSource = innerSource;
  }

  public String getOwnerApplicationName() {
    return ownerApplicationName;
  }

  public String getOwnerApplicationId() {
    return ownerApplicationId;
  }

  public String getOwnerComponentName() {
    return ownerComponentName;
  }

  public boolean isInnerSource() {
    return innerSource;
  }

  public void setOwnerApplicationName(final String ownerApplicationName) {
    this.ownerApplicationName = ownerApplicationName;
  }

  public void setOwnerApplicationId(final String ownerApplicationId) {
    this.ownerApplicationId = ownerApplicationId;
  }

  public void setInnerSource(final boolean innerSource) {
    this.innerSource = innerSource;
  }

  public void setOwnerComponentName(final String ownerComponentName) {
    this.ownerComponentName = ownerComponentName;
  }
}
