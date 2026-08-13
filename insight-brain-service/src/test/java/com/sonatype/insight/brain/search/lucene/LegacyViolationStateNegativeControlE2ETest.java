/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.util.function.Function;

import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.opensearch.LuceneToOpenSearchQueryAdapter;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch._types.query_dsl.Query.Kind;

import static java.time.Instant.now;
import static java.util.Collections.emptyMap;
import static java.util.Date.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * Live, both-backend negative control for the distinct {@code legacy} componentViolationState.
 * <p>
 * The four violation shapes (active / pure-legacy / waived / waived+legacy) are classified through the
 * REAL production derivation ({@link DocumentBuilderHelper#componentViolationRollupByHash}, which calls
 * {@code deriveWaiverStatus} then {@code componentViolationState}) and indexed as real
 * {@link StringField} componentViolationState values into a real Lucene index, exactly as
 * {@code DocumentBuilder.setComponentViolationStates} does.
 * <ul>
 * <li><b>Lucene backend</b>: the production {@link LuceneComponents#newQueryParser()} compiles the
 * Components-tab {@code componentViolationState:"legacy"} filter fragment; only the pure-legacy
 * component is returned, the waived+legacy component surfaces under {@code waived}, and the active one
 * under {@code open}. The <b>negative control</b> is the same corpus classified with the pre-fix
 * else&rarr;waived mapping: the legacy filter then returns zero and the legacy component wrongly appears
 * under waived.</li>
 * <li><b>OpenSearch backend</b>: the SAME production-compiled Lucene query is translated through the
 * REAL {@link LuceneToOpenSearchQueryAdapter}; it emits a {@code term} filter on
 * {@code componentViolationState = "legacy"} &mdash; the identical term the Lucene index matched &mdash;
 * so the two backends resolve the legacy facet identically.</li>
 * </ul>
 */
public class LegacyViolationStateNegativeControlE2ETest
{
  private static final String STATE_FIELD = FieldIdentifier.COMPONENT_VIOLATION_STATE.label;

  private static PolicyViolation violation(final String hash, final boolean waived, final boolean legacy) {
    PolicyViolation v = mock(PolicyViolation.class);
    lenient().when(v.getHash()).thenReturn(hash);
    lenient().when(v.getThreatLevel()).thenReturn(5);
    lenient().when(v.getThreatCategory()).thenReturn(null);
    lenient().when(v.getWaiveTime()).thenReturn(waived ? from(now()) : null);
    lenient().when(v.getAutoPolicyWaiverId()).thenReturn(null);
    lenient().when(v.isLegacyViolation()).thenReturn(legacy);
    return v;
  }

  /** Production-derived component violation state for a single-violation component. */
  private static String productionState(final boolean waived, final boolean legacy) {
    var rollup = DocumentBuilderHelper.componentViolationRollupByHash(
        java.util.List.of(violation("h", waived, legacy)));
    return rollup.get("h").states().iterator().next();
  }

  private static Directory buildIndex(final Function<int[], String> stateFor) throws Exception {
    Directory directory = new ByteBuffersDirectory();
    Analyzer analyzer = new PerFieldAnalyzerWrapper(new LowerCaseKeywordAnalyzer(), emptyMap());
    try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
      // waived flag, legacy flag per component
      writer.addDocument(componentDoc("comp-active", stateFor.apply(new int[]{0, 0})));
      writer.addDocument(componentDoc("comp-legacy", stateFor.apply(new int[]{0, 1})));
      writer.addDocument(componentDoc("comp-waived", stateFor.apply(new int[]{1, 0})));
      writer.addDocument(componentDoc("comp-waived-legacy", stateFor.apply(new int[]{1, 1})));
      writer.commit();
    }
    return directory;
  }

  private static Document componentDoc(final String name, final String state) {
    Document doc = new Document();
    doc.add(new TextField(FieldIdentifier.ITEM_TYPE.label, "non_vulnerable_component", Store.YES));
    doc.add(new TextField(FieldIdentifier.COMPONENT_NAME.label, name, Store.YES));
    doc.add(new StringField(STATE_FIELD, state, Store.YES));
    return doc;
  }

  private static java.util.List<String> componentsMatching(final Directory dir, final Query query) throws Exception {
    try (IndexReader reader = DirectoryReader.open(dir)) {
      IndexSearcher searcher = new IndexSearcher(reader);
      TopDocs top = searcher.search(query, 25);
      java.util.List<String> names = new java.util.ArrayList<>();
      for (ScoreDoc sd : top.scoreDocs) {
        names.add(searcher.storedFields().document(sd.doc).get(FieldIdentifier.COMPONENT_NAME.label));
      }
      return names;
    }
  }

  @Test
  public void luceneBackend_legacyIsADistinctState_withNegativeControl() throws Exception {
    // Sanity: the production derivation produces the distinct legacy state (and waiver precedence).
    assertThat(productionState(false, false)).isEqualTo("open");
    assertThat(productionState(false, true)).isEqualTo("legacy");
    assertThat(productionState(true, false)).isEqualTo("waived");
    assertThat(productionState(true, true)).isEqualTo("waived");

    Function<String, Query> production = new LuceneComponents(null).newQueryParser();

    // --- WITH the fix: real production classification (open/waived/legacy) ---
    try (Directory dir = buildIndex(flags -> productionState(flags[0] == 1, flags[1] == 1))) {
      assertThat(componentsMatching(dir, production.apply(STATE_FIELD + ":\"legacy\"")))
          .as("legacy filter returns only the pure-legacy component")
          .containsExactly("comp-legacy");
      assertThat(componentsMatching(dir, production.apply(STATE_FIELD + ":\"waived\"")))
          .as("waived filter returns waived AND waived+legacy")
          .containsExactlyInAnyOrder("comp-waived", "comp-waived-legacy");
      assertThat(componentsMatching(dir, production.apply(STATE_FIELD + ":\"open\"")))
          .as("open filter returns only the active component")
          .containsExactly("comp-active");
    }

    // --- NEGATIVE CONTROL: pre-fix else->waived mapping (no distinct legacy state) ---
    Function<int[], String> preFix = flags -> {
      boolean waived = flags[0] == 1;
      boolean legacy = flags[1] == 1;
      if (!waived && !legacy) {
        return "open";
      }
      // pre-fix: everything non-active (including pure-legacy) classified as waived
      return "waived";
    };
    try (Directory dir = buildIndex(preFix)) {
      assertThat(componentsMatching(dir, production.apply(STATE_FIELD + ":\"legacy\"")))
          .as("pre-fix: legacy filter returns zero (the defect)")
          .isEmpty();
      assertThat(componentsMatching(dir, production.apply(STATE_FIELD + ":\"waived\"")))
          .as("pre-fix: the pure-legacy component wrongly appears under waived")
          .contains("comp-legacy");
    }
  }

  @Test
  public void openSearchBackend_legacyFilterTranslatesToSameTerm() {
    Function<String, Query> production = new LuceneComponents(null).newQueryParser();
    Query luceneLegacy = production.apply(STATE_FIELD + ":\"legacy\"");

    org.opensearch.client.opensearch._types.query_dsl.Query os =
        LuceneToOpenSearchQueryAdapter.toOpenSearch(luceneLegacy);

    // A single-term keyword filter compiles to a Term query on both backends: OpenSearch resolves the
    // same componentViolationState = "legacy" facet the Lucene index matched.
    assertThat(unwrapTerm(luceneLegacy)).isEqualTo(new org.apache.lucene.index.Term(STATE_FIELD, "legacy"));
    assertThat(os._kind()).isEqualTo(Kind.Term);
    assertThat(os.term().field()).isEqualTo(STATE_FIELD);
    assertThat(os.term().value().stringValue()).isEqualTo("legacy");
  }

  private static org.apache.lucene.index.Term unwrapTerm(final Query q) {
    if (q instanceof TermQuery tq) {
      return tq.getTerm();
    }
    throw new AssertionError("expected a single TermQuery, got " + q.getClass().getSimpleName() + ": " + q);
  }
}
