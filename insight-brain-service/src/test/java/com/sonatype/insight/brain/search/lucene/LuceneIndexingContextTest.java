/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.junit.Test;

import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.index.FieldIdentifier;

import static org.apache.lucene.document.Field.Store.YES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link LuceneIndexingContext} facet docValues indexing.
 */
public class LuceneIndexingContextTest
{
  @Test
  public void addDocuments_addsFacetDocValuesForOpaqueIdFields_rawCaseSensitive() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      LuceneIndexingContext context = new LuceneIndexingContext(
          mock(OwnerDAO.class),
          writer,
          mock(ConversionHelper.class));

      // Build a document with opaque ID fields, including mixed-case values to prove RAW preservation
      Document doc = new Document();
      doc.add(new StringField(FieldIdentifier.ORGANIZATION_ID.label, "abc123def456", YES));
      doc.add(new StringField(FieldIdentifier.APPLICATION_ID.label, "app-id-mixed-CASE", YES));
      doc.add(new StringField(FieldIdentifier.COMPONENT_HASH.label, "hashABC123", YES));
      // Multi-valued parentOrganizationId: one lowercase hex, one uppercase sentinel
      doc.add(new StringField(FieldIdentifier.PARENT_ORGANIZATION_ID.label, "parent-org-id-123", YES));
      doc.add(new StringField(FieldIdentifier.PARENT_ORGANIZATION_ID.label, "ROOT_ORGANIZATION_ID", YES));
      // Name and category facet twins are raw/case-preserving too: names are the display keys the rails
      // show, and a category id is opaque.
      doc.add(new StringField(FieldIdentifier.PARENT_ORGANIZATION_NAME.label, "Child Org", YES));
      doc.add(new StringField(FieldIdentifier.PARENT_ORGANIZATION_NAME.label, "Root Organization", YES));
      doc.add(new StringField(FieldIdentifier.APPLICATION_CATEGORY_NAME.label, "Distributed", YES));
      doc.add(new StringField(FieldIdentifier.APPLICATION_CATEGORY_NAME.label, "Hosted", YES));
      doc.add(new StringField(FieldIdentifier.APPLICATION_CATEGORY_ID.label, "cat-Id-MixedCase", YES));
      // Single-valued per waiver document, and a display key, so its twin is raw like the other names.
      doc.add(new StringField(FieldIdentifier.POLICY_WAIVER_POLICY_NAME.label, "Security - Critical", YES));
      // Also include a documentKey to avoid null
      doc.add(new StringField(FieldIdentifier.DOCUMENT_KEY.label, "doc-key-1", YES));

      context.addDocuments(List.of(doc));
      writer.commit();

      // Read back and assert facet docValues
      try (DirectoryReader reader = DirectoryReader.open(directory)) {
        assertThat(reader.numDocs()).isEqualTo(1);

        // Single-valued facet docValues (SortedDocValues) - raw values preserved
        assertSortedDocValue(reader, FieldIdentifier.ORGANIZATION_ID.label, "abc123def456");
        assertSortedDocValue(reader, FieldIdentifier.APPLICATION_ID.label, "app-id-mixed-CASE");
        assertSortedDocValue(reader, FieldIdentifier.COMPONENT_HASH.label, "hashABC123");

        // Multi-valued facet docValues (SortedSetDocValues) - raw values including uppercase sentinel
        assertSortedSetDocValues(reader, FieldIdentifier.PARENT_ORGANIZATION_ID.label,
            "ROOT_ORGANIZATION_ID", "parent-org-id-123");

        // Name facets aggregate on display names, so their bytes stay raw; the ancestor closure is
        // multi-valued, as is an app's category list.
        assertSortedSetDocValues(reader, FieldIdentifier.PARENT_ORGANIZATION_NAME.label,
            "Child Org", "Root Organization");
        assertSortedSetDocValues(reader, FieldIdentifier.APPLICATION_CATEGORY_NAME.label,
            "Distributed", "Hosted");
        assertSortedSetDocValues(reader, FieldIdentifier.APPLICATION_CATEGORY_ID.label,
            "cat-Id-MixedCase");
        assertSortedDocValue(reader, FieldIdentifier.POLICY_WAIVER_POLICY_NAME.label, "Security - Critical");

        // Existing doc key sort docValues unaffected
        assertSortedDocValue(reader, FieldIdentifier.DOCUMENT_KEY.label, "doc-key-1");
      }
    }
  }

  @Test
  public void addDocuments_addsFoldedFacetDocValuesForEnumFields_lowercaseCasing() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      LuceneIndexingContext context = new LuceneIndexingContext(
          mock(OwnerDAO.class),
          writer,
          mock(ConversionHelper.class));

      // Build a document with mixed/upper-case enum values to prove FOLDED normalization
      Document doc = new Document();
      doc.add(new StringField(FieldIdentifier.DOCUMENT_KEY.label, "doc-folded-1", YES));
      // Single-valued enum fields with mixed-case values
      doc.add(new StringField(FieldIdentifier.COMPONENT_FORMAT.label, "Maven", YES));
      doc.add(new StringField(FieldIdentifier.POLICY_THREAT_CATEGORY.label, "SECURITY", YES));
      doc.add(new StringField(FieldIdentifier.POLICY_VIOLATION_THREAT_CATEGORY.label, "LICENSE", YES));
      doc.add(new StringField(FieldIdentifier.POLICY_VIOLATION_WAIVER_STATUS.label, "WAIVED", YES));
      doc.add(new StringField(FieldIdentifier.ITEM_TYPE.label, "POLICY_VIOLATION", YES));
      doc.add(new StringField(FieldIdentifier.VULNERABILITY_STATUS.label, "OPEN", YES));
      doc.add(new StringField(FieldIdentifier.POLICY_WAIVER_SCOPE.label, "APPLICATION", YES));
      doc.add(new StringField(FieldIdentifier.POLICY_WAIVER_POLICY_TYPE.label, "Quality", YES));
      doc.add(new StringField(FieldIdentifier.POLICY_WAIVER_REQUEST_STATUS.label, "APPROVED", YES));
      doc.add(new StringField(FieldIdentifier.POLICY_WAIVER_EXPIRY_STATUS.label, "Active", YES));
      doc.add(new StringField(FieldIdentifier.POLICY_WAIVER_AUTO.label, "TRUE", YES));
      doc.add(new StringField(FieldIdentifier.POLICY_WAIVER_IS_AUTO.label, "FALSE", YES));
      // Multi-valued enum fields with mixed-case values
      doc.add(new StringField(FieldIdentifier.APPLICATION_VIOLATION_STAGE.label, "BUILD", YES));
      doc.add(new StringField(FieldIdentifier.APPLICATION_VIOLATION_STAGE.label, "Stage-Release", YES));
      doc.add(new StringField(FieldIdentifier.APPLICATION_VIOLATION_POLICY_TYPE.label, "SECURITY", YES));
      doc.add(new StringField(FieldIdentifier.APPLICATION_VIOLATION_POLICY_TYPE.label, "License", YES));
      doc.add(new StringField(FieldIdentifier.APPLICATION_VIOLATION_STATE.label, "OPEN", YES));
      doc.add(new StringField(FieldIdentifier.APPLICATION_VIOLATION_STATE.label, "Waived", YES));

      context.addDocuments(List.of(doc));
      writer.commit();

      // Read back and assert facet docValues are lowercased (folded)
      try (DirectoryReader reader = DirectoryReader.open(directory)) {
        assertThat(reader.numDocs()).isEqualTo(1);

        // Single-valued folded facet docValues (SortedDocValues) - lowercased
        assertSortedDocValue(reader, FieldIdentifier.COMPONENT_FORMAT.label, "maven");
        assertSortedDocValue(reader, FieldIdentifier.POLICY_THREAT_CATEGORY.label, "security");
        assertSortedDocValue(reader, FieldIdentifier.POLICY_VIOLATION_THREAT_CATEGORY.label, "license");
        assertSortedDocValue(reader, FieldIdentifier.POLICY_VIOLATION_WAIVER_STATUS.label, "waived");
        assertSortedDocValue(reader, FieldIdentifier.ITEM_TYPE.label, "policy_violation");
        assertSortedDocValue(reader, FieldIdentifier.VULNERABILITY_STATUS.label, "open");
        assertSortedDocValue(reader, FieldIdentifier.POLICY_WAIVER_SCOPE.label, "application");
        assertSortedDocValue(reader, FieldIdentifier.POLICY_WAIVER_POLICY_TYPE.label, "quality");
        assertSortedDocValue(reader, FieldIdentifier.POLICY_WAIVER_REQUEST_STATUS.label, "approved");
        assertSortedDocValue(reader, FieldIdentifier.POLICY_WAIVER_EXPIRY_STATUS.label, "active");
        assertSortedDocValue(reader, FieldIdentifier.POLICY_WAIVER_AUTO.label, "true");
        assertSortedDocValue(reader, FieldIdentifier.POLICY_WAIVER_IS_AUTO.label, "false");

        // Multi-valued folded facet docValues (SortedSetDocValues) - lowercased, sorted
        assertSortedSetDocValues(reader, FieldIdentifier.APPLICATION_VIOLATION_STAGE.label,
            "build", "stage-release");
        assertSortedSetDocValues(reader, FieldIdentifier.APPLICATION_VIOLATION_POLICY_TYPE.label,
            "security", "license");
        assertSortedSetDocValues(reader, FieldIdentifier.APPLICATION_VIOLATION_STATE.label,
            "open", "waived");
      }
    }
  }

  @Test
  public void addDocuments_addsFacetDocValuesForPolicyWaiverPolicyId_rawCaseSensitive() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      LuceneIndexingContext context = new LuceneIndexingContext(
          mock(OwnerDAO.class),
          writer,
          mock(ConversionHelper.class));

      // Build a waiver document with a policy ID (opaque ID, raw/case-sensitive)
      Document doc = new Document();
      doc.add(new StringField(FieldIdentifier.DOCUMENT_KEY.label, "doc-waiver-1", YES));
      doc.add(new StringField(FieldIdentifier.POLICY_WAIVER_POLICY_ID.label, "policy-uuid-ABC123", YES));

      context.addDocuments(List.of(doc));
      writer.commit();

      // Read back and assert facet docValues for policyWaiverPolicyId (raw, not folded)
      try (DirectoryReader reader = DirectoryReader.open(directory)) {
        assertThat(reader.numDocs()).isEqualTo(1);

        // Raw facet docValues for policyWaiverPolicyId - exact case preserved
        assertSortedDocValue(reader, FieldIdentifier.POLICY_WAIVER_POLICY_ID.label, "policy-uuid-ABC123");
      }
    }
  }

  @Test
  public void addDocuments_facetDocValuesAreAbsentWhenFieldNotPresent() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      LuceneIndexingContext context = new LuceneIndexingContext(
          mock(OwnerDAO.class),
          writer,
          mock(ConversionHelper.class));

      // Minimal document with only documentKey (no facet fields)
      Document doc = new Document();
      doc.add(new StringField(FieldIdentifier.DOCUMENT_KEY.label, "doc-key-2", YES));

      context.addDocuments(List.of(doc));
      writer.commit();

      try (DirectoryReader reader = DirectoryReader.open(directory)) {
        assertThat(reader.numDocs()).isEqualTo(1);

        // Facet docValues should NOT be present when fields are absent
        // (fields absent from stored fields also absent from docValues)
        assertThat(reader.leaves().get(0).reader().getSortedDocValues(FieldIdentifier.ORGANIZATION_ID.label))
            .isNull();
        assertThat(reader.leaves().get(0).reader().getSortedDocValues(FieldIdentifier.APPLICATION_ID.label))
            .isNull();
        assertThat(reader.leaves().get(0).reader().getSortedDocValues(FieldIdentifier.COMPONENT_HASH.label))
            .isNull();
        assertThat(reader.leaves().get(0).reader().getSortedSetDocValues(FieldIdentifier.PARENT_ORGANIZATION_ID.label))
            .isNull();

        // Existing doc key sort docValues still present
        assertSortedDocValue(reader, FieldIdentifier.DOCUMENT_KEY.label, "doc-key-2");
      }
    }
  }

  /**
   * Component docs carry the license-threat and violation policy-type/state columns the Components and
   * Legal rails aggregate and sort on.
   */
  @Test
  public void addDocuments_addsFacetDocValuesForComponentViolationAndLicenseThreatFields() throws Exception {
    try (Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new LowerCaseKeywordAnalyzer();
        IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)))
    {
      LuceneIndexingContext context = new LuceneIndexingContext(
          mock(OwnerDAO.class),
          writer,
          mock(ConversionHelper.class));

      Document doc = new Document();
      doc.add(new StringField(FieldIdentifier.DOCUMENT_KEY.label, "doc-component-1", YES));
      // Multi-valued: a component doc carries one value per distinct policy type / state.
      doc.add(new StringField(FieldIdentifier.COMPONENT_VIOLATION_POLICY_TYPE.label, "SECURITY", YES));
      doc.add(new StringField(FieldIdentifier.COMPONENT_VIOLATION_POLICY_TYPE.label, "Quality", YES));
      doc.add(new StringField(FieldIdentifier.COMPONENT_VIOLATION_STATE.label, "OPEN", YES));
      doc.add(new StringField(FieldIdentifier.COMPONENT_VIOLATION_STATE.label, "Waived", YES));
      // Threat group name is an analysed TextField; its facet twin keeps the display casing.
      doc.add(new TextField(FieldIdentifier.COMPONENT_LICENSE_THREAT_GROUP_NAME.label, "Banned", YES));
      // Threat level is an IntPoint plus a StoredField; the numeric twin comes from the stored value.
      doc.add(new IntPoint(FieldIdentifier.COMPONENT_LICENSE_THREAT_LEVEL.label, 8));
      doc.add(new StoredField(FieldIdentifier.COMPONENT_LICENSE_THREAT_LEVEL.label, 8));

      context.addDocuments(List.of(doc));
      writer.commit();

      try (DirectoryReader reader = DirectoryReader.open(directory)) {
        assertThat(reader.numDocs()).isEqualTo(1);

        // Folded, multi-valued: the Components policy-type and violation-state rails aggregate on these.
        assertSortedSetDocValues(reader, FieldIdentifier.COMPONENT_VIOLATION_POLICY_TYPE.label,
            "security", "quality");
        assertSortedSetDocValues(reader, FieldIdentifier.COMPONENT_VIOLATION_STATE.label,
            "open", "waived");
        // Raw / case-preserving so the Legal rail facets on the display name.
        assertSortedDocValue(reader, FieldIdentifier.COMPONENT_LICENSE_THREAT_GROUP_NAME.label, "Banned");

        SortedNumericDocValues threatLevel = reader.leaves()
            .get(0)
            .reader()
            .getSortedNumericDocValues(FieldIdentifier.COMPONENT_LICENSE_THREAT_LEVEL.label);
        assertThat(threatLevel).as("Expected SortedNumericDocValues for componentLicenseThreatLevel").isNotNull();
        assertThat(threatLevel.advanceExact(0)).isTrue();
        assertThat(threatLevel.nextValue()).isEqualTo(8L);
      }
    }
  }

  private void assertSortedDocValue(
      DirectoryReader reader,
      String fieldLabel,
      String expectedValue) throws IOException
  {
    SortedDocValues dv = reader.leaves().get(0).reader().getSortedDocValues(fieldLabel);
    assertThat(dv).as("Expected SortedDocValues for " + fieldLabel).isNotNull();
    assertThat(dv.getValueCount()).isEqualTo(1);
    BytesRef value = dv.lookupOrd(0);
    assertThat(value.utf8ToString()).isEqualTo(expectedValue);
  }

  private void assertSortedSetDocValues(
      DirectoryReader reader,
      String fieldLabel,
      String... expectedValues) throws IOException
  {
    SortedSetDocValues dv = reader.leaves().get(0).reader().getSortedSetDocValues(fieldLabel);
    assertThat(dv).as("Expected SortedSetDocValues for " + fieldLabel).isNotNull();
    assertThat(dv.getValueCount()).isEqualTo(expectedValues.length);

    java.util.Set<String> actualValues = new java.util.HashSet<>();
    for (int i = 0; i < dv.getValueCount(); i++) {
      actualValues.add(dv.lookupOrd(i).utf8ToString());
    }
    assertThat(actualValues).containsExactlyInAnyOrder(expectedValues);
  }
}
