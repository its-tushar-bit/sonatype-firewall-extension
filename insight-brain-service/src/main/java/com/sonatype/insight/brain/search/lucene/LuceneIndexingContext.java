/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.io.IOException;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.index.IndexingContext;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;

public class LuceneIndexingContext
    extends IndexingContext
{
  private final IndexWriter indexWriter;

  public LuceneIndexingContext(
      final OwnerDAO ownerDAO,
      final IndexWriter indexWriter,
      final ConversionHelper conversionHelper)
  {
    super(ownerDAO, conversionHelper);
    this.indexWriter = indexWriter;
  }

  @Override
  public void deleteDocuments(final String query) throws IOException {
    indexWriter.deleteDocuments(getConversionHelper().stringToQuery(query));
  }

  @Override
  public void addDocuments(final List<Document> documents) throws IOException {
    indexWriter.addDocuments(documents);
  }
}
