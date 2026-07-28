/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.IndexingContext;
import com.sonatype.insight.brain.search.index.ItemType;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexableField;
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

  /**
   * Keyword sort doc-values twin, lower-cased to match the OpenSearch {@code keyword} mapping's
   * {@code lowercase} normalizer (see {@code IndexMapping.createProperty}). Both backends must sort
   * on the same normalized bytes so a name/stage sort orders identically regardless of case, and so
   * the {@code searchAfter} boundary from one backend anchors the same row on the other. No-op when
   * the field is absent (never-set field on this doc type).
   */
  private static void addSortDocValues(final Document document, final String fieldLabel) {
    String value = document.get(fieldLabel);
    if (value != null) {
      document.add(new SortedDocValuesField(fieldLabel, new BytesRef(value.toLowerCase(Locale.ROOT))));
    }
  }

  /**
   * Numeric sort doc-values twin for an epoch-millis long field. Reads the stored numeric value
   * (LongPoint alone is not stored/retrievable) so the field is sortable in Lucene; OpenSearch
   * sorts on its {@code long} mapping. No-op when the field is absent (never-evaluated app).
   */
  private static void addNumericSortDocValues(final Document document, final String fieldLabel) {
    for (IndexableField field : document.getFields(fieldLabel)) {
      Number numeric = field.numericValue();
      if (numeric != null) {
        document.add(new SortedNumericDocValuesField(fieldLabel, numeric.longValue()));
        return;
      }
    }
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
      addSortDocValues(document, FieldIdentifier.POLICY_WAIVER_CREATED_AT.label);
      addSortDocValues(document, FieldIdentifier.POLICY_WAIVER_EXPIRES_AT.label);
      // Keyword sort twins for the allowlisted name/stage sort keys. DocumentBuilder writes these as
      // analysed TextFields (no doc-values), so the sortable twin lives here; only one of these
      // labels is populated per doc (by item type), so the null-check in addSortDocValues no-ops the rest.
      addSortDocValues(document, FieldIdentifier.APPLICATION_NAME.label);
      addSortDocValues(document, FieldIdentifier.POLICY_EVALUATION_STAGE.label);
      addSortDocValues(document, FieldIdentifier.COMPONENT_NAME.label);
      addSortDocValues(document, FieldIdentifier.VULNERABILITY_ID.label);
      addSortDocValues(document, FieldIdentifier.POLICY_VIOLATION_POLICY_NAME.label);
      addNumericSortDocValues(document, FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label);
      addNumericSortDocValues(document, FieldIdentifier.POLICY_WAIVER_CREATED_AT_EPOCH_MS.label);
      final String itemType = document.get(FieldIdentifier.ITEM_TYPE.label);
      // Threat level is set only on POLICY_VIOLATION docs; skip the field scan on the other item types.
      if (ItemType.POLICY_VIOLATION.name().equals(itemType)) {
        addNumericSortDocValues(document, FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label);
      }
      // The denormalized violation-aggregate int twins are set only on APPLICATION docs; guard the scan
      // to that type so the max-threat (desc) and violation-state-ordinal (asc) sorts are sortable.
      if (ItemType.APPLICATION.name().equals(itemType)) {
        addNumericSortDocValues(document, FieldIdentifier.APPLICATION_MAX_POLICY_THREAT_LEVEL.label);
        addNumericSortDocValues(document, FieldIdentifier.APPLICATION_VIOLATION_STATE_SORT_ORDINAL.label);
      }
      // Waiver + waiver-request docs back the WAIVER threat (descending) and expiration (ascending)
      // sorts on their numeric epoch/level twins, mirroring the POLICY_VIOLATION threat-level guard.
      // Gated to these item types so the field scan does not run on unrelated docs.
      if (ItemType.POLICY_WAIVER.name().equals(itemType)
          || ItemType.POLICY_WAIVER_REQUEST.name().equals(itemType))
      {
        addNumericSortDocValues(document, FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL.label);
        addNumericSortDocValues(document, FieldIdentifier.POLICY_WAIVER_EXPIRES_AT_EPOCH_MS.label);
      }
    }
    indexWriter.addDocuments(documents);
  }
}
