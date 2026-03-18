/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.export;

import java.util.Arrays;
import java.util.List;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import static com.sonatype.insight.brain.search.export.SearchRowFactory.Header.*;

@Named
@Singleton
public class LifecycleSearchRowFactory
    extends SearchRowFactory
{
  private static final List<Header> EXPORT_SEARCH_HEADERS = Arrays.asList(
      ITEM_TYPE, ORGANIZATION, ORGANIZATION_LINK, APPLICATION, APPLICATION_LINK, APPLICATION_CATEGORY,
      APPLICATION_CATEGORY_LINK, COMPONENT_LABEL, COMPONENT_LABEL_LINK, POLICY, THREAT, POLICY_LINK, COMPONENT_NAME,
      REPORT, SECURITY_ISSUE, STAGE);

  public LifecycleSearchRowFactory() {
    super(EXPORT_SEARCH_HEADERS);
  }
}
