/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

public final class AdvancedSearchExportPaths
{
  private AdvancedSearchExportPaths() {}

  public static final String EXPORT_FILE_NAME = "advanced_search.csv";

  public static final String[] EXPORT_SEARCH_HEADERS = {
      "Item Type", "Organization", "Organization Link", "Application", "Application Link", "Application Category",
      "Application Category Link", "Component Label", "Component Label Link", "Policy", "Threat",
      "Policy Link", "Component Name", "Report", "Security Issue", "Stage"
  };

  public static final String ORGANIZATION_PATH_VARIABLE = "organization";

  public static final String APPLICATION_PATH_VARIABLE = "application";

  public static final String APPLICATION_CATEGORY_PATH_VARIABLE = "category";

  public static final String LABEL_PATH_VARIABLE = "label";

  public static final String POLICY_PATH_VARIABLE = "policy";
}
