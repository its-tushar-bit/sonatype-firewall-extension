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
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.IndexingContext;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.util.BytesRef;

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
    // Sort doc-values live only here, not in DocumentBuilder: a same-named field would serialize a
    // null into the OpenSearch _source and NPE on read-back. OpenSearch sorts on its keyword mapping.
    for (Document document : documents) {
      String key = document.get(FieldIdentifier.DOCUMENT_KEY.label);
      if (key != null) {
        document.add(new SortedDocValuesField(FieldIdentifier.DOCUMENT_KEY.label, new BytesRef(key)));
      }
    }
    indexWriter.addDocuments(documents);
  }
}
