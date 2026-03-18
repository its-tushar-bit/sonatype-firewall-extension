/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.tag;

import java.util.List;

public class ApplicationTagData
{
  private String appId;

  private List<String> categories;

  public ApplicationTagData() {
    // for serialization
  }

  public ApplicationTagData(String appId, List<String> categories) {
    this.appId = appId;
    this.categories = categories;
  }

  public String getAppId() {
    return appId;
  }

  public List<String> getCategories() {
    return categories;
  }
}
