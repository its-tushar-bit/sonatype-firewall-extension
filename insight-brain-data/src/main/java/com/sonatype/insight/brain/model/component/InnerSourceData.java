/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class InnerSourceData
{
  private String ownerApplicationName;

  private String ownerApplicationId;

  @JsonInclude(Include.NON_NULL)
  private String innerSourceComponentPurl;

  public InnerSourceData() {
  }

  public InnerSourceData(
      final String ownerApplicationName,
      final String ownerApplicationId,
      final String innerSourceComponentPurl)
  {
    this.ownerApplicationName = ownerApplicationName;
    this.ownerApplicationId = ownerApplicationId;
    this.innerSourceComponentPurl = innerSourceComponentPurl;
  }

  public String getOwnerApplicationName() {
    return ownerApplicationName;
  }

  public String getOwnerApplicationId() {
    return ownerApplicationId;
  }

  public String getInnerSourceComponentPurl() {
    return innerSourceComponentPurl;
  }

  public void setOwnerApplicationName(final String ownerApplicationName) {
    this.ownerApplicationName = ownerApplicationName;
  }

  public void setOwnerApplicationId(final String ownerApplicationId) {
    this.ownerApplicationId = ownerApplicationId;
  }

  public void setInnerSourceComponentPurl(final String ownerComponentName) {
    this.innerSourceComponentPurl = ownerComponentName;
  }
}
