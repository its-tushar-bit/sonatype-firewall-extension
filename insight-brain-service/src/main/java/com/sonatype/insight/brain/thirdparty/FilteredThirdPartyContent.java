/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.util.Collections;
import java.util.List;

import com.sonatype.insight.scan.model.ProjectScanItem;

/**
 * @since 1.130
 */
public class FilteredThirdPartyContent
{
  /**
   * filtered thirdparty content
   */
  private final String content;

  /**
   * recognized modules and their dependency graphs.
   */
  private final List<ProjectScanItem> moduleDependencies;

  public FilteredThirdPartyContent(String content) {
    this(content, Collections.emptyList());
  }

  public FilteredThirdPartyContent(String content, List<ProjectScanItem> moduleDependencies) {
    this.content = content;
    this.moduleDependencies = moduleDependencies;
  }

  public String getContent() {
    return content;
  }

  public List<ProjectScanItem> getModuleDependencies() {
    return moduleDependencies;
  }
}
