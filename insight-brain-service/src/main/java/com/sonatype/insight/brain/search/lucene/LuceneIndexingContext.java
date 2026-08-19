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
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;

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

  /**
   * Facet doc-values twin for a single-valued opaque-ID facet field. Raw / case-sensitive: adds
   * the exact stored value without {@code toLowerCase}, so faceting aggregates on the raw ID bytes
   * (matches filter normalization for opaque IDs). No-op when the field is absent.
   */
  private static void addFacetDocValues(final Document document, final String fieldLabel) {
    String value = document.get(fieldLabel);
    if (value != null) {
      document.add(new SortedDocValuesField(fieldLabel, new BytesRef(value)));
    }
  }

  /**
   * Facet doc-values twin for a multi-valued opaque-ID facet field. One {@code SortedSetDocValuesField}
   * per value, raw / case-sensitive (matches filter normalization). No-op when the field is absent.
   */
  private static void addMultiFacetDocValues(final Document document, final String fieldLabel) {
    for (String value : document.getValues(fieldLabel)) {
      document.add(new SortedSetDocValuesField(fieldLabel, new BytesRef(value)));
    }
  }

  // --- Folded facet doc-values helpers (case-insensitive enum/fixed-vocabulary fields) ---
  // Distinct from the raw facet helpers above and the sort helpers; these emit lowercased facet DVs,
  // matching filter normalization for case-insensitive named/enum fields (see design principle).

  /**
   * Facet doc-values twin for a single-valued case-insensitive enum/fixed-vocabulary facet field.
   * Lowercased ({@code Locale.ROOT}) so faceting aggregates on folded bytes (matches filter
   * normalization for enum fields). No-op when the field is absent.
   */
  private static void addFoldedFacetDocValues(final Document document, final String fieldLabel) {
    String value = document.get(fieldLabel);
    if (value != null) {
      document.add(new SortedDocValuesField(fieldLabel, new BytesRef(value.toLowerCase(Locale.ROOT))));
    }
  }

  /**
   * Facet doc-values twin for a multi-valued case-insensitive enum/fixed-vocabulary facet field.
   * One {@code SortedSetDocValuesField} per value, lowercased ({@code Locale.ROOT}). No-op when the
   * field is absent.
   */
  private static void addFoldedMultiFacetDocValues(final Document document, final String fieldLabel) {
    for (String value : document.getValues(fieldLabel)) {
      document.add(new SortedSetDocValuesField(fieldLabel, new BytesRef(value.toLowerCase(Locale.ROOT))));
    }
  }

  /**
   * Numeric sort doc-values twin for a {@code float} score field (e.g. CVSS
   * {@code vulnerabilitySeverity}). The stored value is a float, so {@code longValue()} would
   * truncate (7.5&nbsp;&rarr;&nbsp;7) and destroy the ordering within an integer band; instead the
   * float is encoded via {@link NumericUtils#floatToSortableInt(float)} (order-preserving int bits)
   * and widened to long. A matching {@link org.apache.lucene.search.SortedNumericSortField} of
   * {@code Type.FLOAT} decodes it, so the sort is by true CVSS score. No-op when the field is absent
   * (unscored vuln).
   */
  private static void addFloatNumericSortDocValues(final Document document, final String fieldLabel) {
    for (IndexableField field : document.getFields(fieldLabel)) {
      Number numeric = field.numericValue();
      if (numeric != null) {
        document.add(new SortedNumericDocValuesField(
            fieldLabel, NumericUtils.floatToSortableInt(numeric.floatValue())));
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
      // Component max-threat sort twin: set only on NON_VULNERABLE_COMPONENT docs (int, no truncation).
      addNumericSortDocValues(document, FieldIdentifier.COMPONENT_MAX_POLICY_THREAT_LEVEL.label);
      // License threat level is an IntPoint + StoredField on component docs (range-queryable but not
      // sortable/aggregatable on its own), so the numeric twin lives here.
      addNumericSortDocValues(document, FieldIdentifier.COMPONENT_LICENSE_THREAT_LEVEL.label);
      // CVSS severity sort twin: FloatPoint on SECURITY_VULNERABILITY docs, so encode float-sortable.
      addFloatNumericSortDocValues(document, FieldIdentifier.VULNERABILITY_SEVERITY.label);
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

      // --- Facet doc-values for opaque-ID facet fields (raw / case-sensitive) ---
      // Keep this block visibly separate from the sort twins above, per the design principle.
      // These facet docValues are for faceting-only; sort docs remain case-folded above.
      addFacetDocValues(document, FieldIdentifier.ORGANIZATION_ID.label);
      addFacetDocValues(document, FieldIdentifier.APPLICATION_ID.label);
      addFacetDocValues(document, FieldIdentifier.COMPONENT_HASH.label);
      addFacetDocValues(document, FieldIdentifier.POLICY_WAIVER_POLICY_ID.label);
      addMultiFacetDocValues(document, FieldIdentifier.PARENT_ORGANIZATION_ID.label);
      addMultiFacetDocValues(document, FieldIdentifier.APPLICATION_CATEGORY_ID.label);

      // --- Facet doc-values for NAME fields used by termsAggregation (raw / case-preserving) ---
      // These match the applicationName sort DV pattern: raw bytes so facets aggregate on display names.
      // Multi-valued ancestor closure: each doc contributes to every ancestor-org bucket (hierarchical subtree).
      addMultiFacetDocValues(document, FieldIdentifier.PARENT_ORGANIZATION_NAME.label);
      // Multi-valued: an app can have several categories.
      addMultiFacetDocValues(document, FieldIdentifier.APPLICATION_CATEGORY_NAME.label);
      // Single-valued per waiver doc.
      addFacetDocValues(document, FieldIdentifier.POLICY_WAIVER_POLICY_NAME.label);
      // Single-valued per component doc, written as an analysed TextField, so the facet twin lives here
      // (same pattern as the applicationName sort twin above).
      addFacetDocValues(document, FieldIdentifier.COMPONENT_LICENSE_THREAT_GROUP_NAME.label);

      // --- Folded facet doc-values for case-insensitive enum fields ---
      // Keep this block visibly separate from the raw facet and sort helpers, per the design principle.
      // Single-valued folded facet fields:
      addFoldedFacetDocValues(document, FieldIdentifier.COMPONENT_FORMAT.label);
      addFoldedFacetDocValues(document, FieldIdentifier.POLICY_VIOLATION_THREAT_CATEGORY.label);
      addFoldedFacetDocValues(document, FieldIdentifier.POLICY_THREAT_CATEGORY.label);
      addFoldedFacetDocValues(document, FieldIdentifier.POLICY_VIOLATION_WAIVER_STATUS.label);
      addFoldedFacetDocValues(document, FieldIdentifier.ITEM_TYPE.label);
      addFoldedFacetDocValues(document, FieldIdentifier.VULNERABILITY_STATUS.label);
      addFoldedFacetDocValues(document, FieldIdentifier.POLICY_WAIVER_SCOPE.label);
      addFoldedFacetDocValues(document, FieldIdentifier.POLICY_WAIVER_POLICY_TYPE.label);
      addFoldedFacetDocValues(document, FieldIdentifier.POLICY_WAIVER_REQUEST_STATUS.label);
      addFoldedFacetDocValues(document, FieldIdentifier.POLICY_WAIVER_EXPIRY_STATUS.label);
      addFoldedFacetDocValues(document, FieldIdentifier.POLICY_WAIVER_AUTO.label);
      addFoldedFacetDocValues(document, FieldIdentifier.POLICY_WAIVER_IS_AUTO.label);
      // Multi-valued folded facet fields:
      addFoldedMultiFacetDocValues(document, FieldIdentifier.APPLICATION_VIOLATION_STAGE.label);
      addFoldedMultiFacetDocValues(document, FieldIdentifier.APPLICATION_VIOLATION_POLICY_TYPE.label);
      addFoldedMultiFacetDocValues(document, FieldIdentifier.APPLICATION_VIOLATION_STATE.label);
      // The component-doc equivalents of the two application twins above: a component doc carries one
      // value per distinct policy type / violation state across its violations.
      addFoldedMultiFacetDocValues(document, FieldIdentifier.COMPONENT_VIOLATION_POLICY_TYPE.label);
      addFoldedMultiFacetDocValues(document, FieldIdentifier.COMPONENT_VIOLATION_STATE.label);
    }
    indexWriter.addDocuments(documents);
  }
}
