/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import java.io.File;
import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.tenancy.TenantManaged;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.92
 */
@Named
@Singleton
public class SuggestionIndexCleaner
    implements TenantManaged
{
  private static final Logger log = LoggerFactory.getLogger(SuggestionIndexCleaner.class);

  private final InsightWork insightWork;

  private final FileCleaner fileCleaner;

  @Inject
  public SuggestionIndexCleaner(InsightWork insightWork, FileCleaner fileCleaner) {
    this.insightWork = insightWork;
    this.fileCleaner = fileCleaner;
  }

  @Override
  public void register() {
    File searchSuggesterDir = getSearchSuggesterDir();

    try {
      fileCleaner.delete(searchSuggesterDir);
    }
    catch (IOException e) {
      log.warn("Could not clear suggestion index. Delete {} manually.", searchSuggesterDir.getAbsolutePath(), e);
    }
  }

  File getSearchSuggesterDir() {
    return new File(insightWork.getSearchDir(), "suggester");
  }
}
