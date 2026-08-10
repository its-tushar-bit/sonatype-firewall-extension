/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import java.io.File;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class SuggestionIndexCleanerTest
    extends AbstractComponentH2Test
{
  @Inject
  private InsightWork insightWork;

  @Inject
  private SuggestionIndexCleaner suggestionIndexCleaner;

  @Test
  public void testStart_SearchIndexFolderExists() throws Exception {
    File searchSuggesterDir = suggestionIndexCleaner.getSearchSuggesterDir();
    File suggestionFile = new File(searchSuggesterDir, "suggestion-index.file");

    File searchIndexDir = insightWork.getSearchIndexDir();
    File searchIndexFile = new File(searchIndexDir, "search-index.file");

    searchSuggesterDir.mkdirs();
    suggestionFile.createNewFile();

    searchIndexDir.mkdirs();
    searchIndexFile.createNewFile();

    suggestionIndexCleaner.register();

    // Assert suggestion index directory and files are deleted.
    assertThat(searchSuggesterDir).doesNotExist();

    // Assert search index directory and files are not deleted.
    assertThat(searchIndexFile).exists();
  }

  @Test
  public void testStart_SearchIndexFolderDoesNotExist() throws Exception {
    File searchIndexDir = insightWork.getSearchIndexDir();
    File searchIndexFile = new File(searchIndexDir, "search-index.file");

    searchIndexDir.mkdirs();
    searchIndexFile.createNewFile();

    suggestionIndexCleaner.register();

    // We do not want to delete any unrelated files
    assertThat(searchIndexFile).exists();
  }
}
