/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.search.index.FieldIdentifier;

import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch._types.mapping.KeywordProperty;
import org.opensearch.client.opensearch._types.mapping.Property;

/**
 * Verifies the OpenSearch mapping contract for the {@code allowedContextIds} permission-filter
 * field. The legacy ItemTypes and their fields are covered by existing advanced-search
 * integration tests; this class focuses on the new field's mapping shape.
 */
public class IndexMappingTest
{
  @Test
  public void allowedContextIds_isMappedAsKeyword() {
    Map<String, Property> mappings = new IndexMapping().getMappings();

    Property property = mappings.get(FieldIdentifier.ALLOWED_CONTEXT_IDS.label);

    assertThat(property).isNotNull();
    assertThat(property.isKeyword()).isTrue();
  }

  @Test
  public void allowedContextIds_docValuesUnsetInheritsOpenSearchKeywordDefaultTrue() {
    KeywordProperty keyword = keywordPropertyFor(FieldIdentifier.ALLOWED_CONTEXT_IDS.label);

    // Not explicitly set on the client mapping; OpenSearch server-side default for keyword is
    // doc_values=true. A null here means "server default" — the previous explicit setter was
    // redundant.
    assertThat(keyword.docValues()).isNull();
  }

  @Test
  public void allowedContextIds_indexUnsetInheritsOpenSearchKeywordDefaultTrue() {
    KeywordProperty keyword = keywordPropertyFor(FieldIdentifier.ALLOWED_CONTEXT_IDS.label);

    // Not explicitly set on the client mapping; OpenSearch server-side default for keyword is
    // index=true. A null here means "server default" — the previous explicit setter was redundant.
    assertThat(keyword.index()).isNull();
  }

  @Test
  public void allowedContextIds_isNotAnalyzed() {
    KeywordProperty keyword = keywordPropertyFor(FieldIdentifier.ALLOWED_CONTEXT_IDS.label);

    assertThat(keyword.normalizer()).isNull();
  }

  @Test
  public void allowedContextIds_isPresentForEveryKnownItemTypeBecauseMappingIsTypeAgnostic() {
    // The OpenSearch mapping is a single shared type for all ItemTypes (see IndexMapping
    // class-level Javadoc); confirming the field is present in the shared mapping is
    // equivalent to confirming every ItemType is permission-filterable on the new path.
    Map<String, Property> mappings = new IndexMapping().getMappings();

    assertThat(mappings).containsKey(FieldIdentifier.ALLOWED_CONTEXT_IDS.label);
  }

  /**
   * Verifies the case-sensitive (no normalizer) contract for opaque ID fields that are faceted on
   * the raw ID. These must NOT have a lowercase normalizer because facet bucket keys must match
   * the DB primary keys byte-for-byte.
   */
  @Test
  public void facetIdFields_areCaseSensitive() {
    Stream.of(
        FieldIdentifier.ORGANIZATION_ID,
        FieldIdentifier.APPLICATION_ID,
        FieldIdentifier.COMPONENT_HASH,
        FieldIdentifier.APPLICATION_CATEGORY_ID,
        FieldIdentifier.PARENT_ORGANIZATION_ID,
        FieldIdentifier.POLICY_WAIVER_POLICY_ID,
        FieldIdentifier.ALLOWED_CONTEXT_IDS,
        FieldIdentifier.DOCUMENT_KEY)
        .forEach(field -> assertThat(keywordPropertyFor(field.label).normalizer())
            .as("normalizer for %s should be null (case-sensitive)", field.label)
            .isNull());
  }

  /**
   * Pins the lowercase-normalizer contract for ID-family keyword fields that are NOT faceted on
   * the raw ID (names, public IDs, etc). A regression that strips the normalizer from any of these
   * would silently change query semantics for callers relying on the legacy behaviour.
   */
  @Test
  public void nonFacetIdFields_haveLowerCaseNormalizer() {
    Stream.of(
        FieldIdentifier.APPLICATION_PUBLIC_ID,
        FieldIdentifier.POLICY_ID,
        FieldIdentifier.POLICY_VIOLATION_POLICY_ID,
        FieldIdentifier.COMPONENT_LABEL_ID)
        .forEach(field -> assertThat(keywordPropertyFor(field.label).normalizer())
            .as("normalizer for %s", field.label)
            .isEqualTo("lowercase"));
  }

  @Test
  public void documentKey_isCaseSensitiveSortableKeyword() {
    KeywordProperty keyword = keywordPropertyFor(FieldIdentifier.DOCUMENT_KEY.label);
    // Case-sensitive (no normalizer) so the stored hash sorts byte-for-byte, matching the Lucene
    // doc-values order for the cursor tie-breaker.
    assertThat(keyword.normalizer()).isNull();
  }

  @Test
  public void policyWaiverFields_areMappedWithExpectedTypes() {
    Map<String, Property> mappings = new IndexMapping().getMappings();

    Stream.of(
        FieldIdentifier.POLICY_WAIVER_ID,
        FieldIdentifier.POLICY_WAIVER_POLICY_NAME,
        FieldIdentifier.POLICY_WAIVER_POLICY_ID,
        FieldIdentifier.POLICY_WAIVER_CREATED_AT,
        FieldIdentifier.POLICY_WAIVER_EXPIRES_AT,
        FieldIdentifier.POLICY_WAIVER_SCOPE_OWNER_ID,
        FieldIdentifier.POLICY_WAIVER_SCOPE_OWNER_TYPE,
        FieldIdentifier.POLICY_WAIVER_WAIVED_BY)
        .forEach(field -> {
          Property property = mappings.get(field.label);
          assertThat(property).as("mapping for %s", field.label).isNotNull();
          assertThat(property.isKeyword()).as("%s is keyword", field.label).isTrue();
        });

    Stream.of(FieldIdentifier.POLICY_WAIVER_REASON, FieldIdentifier.POLICY_WAIVER_COMMENT)
        .forEach(field -> {
          Property property = mappings.get(field.label);
          assertThat(property).as("mapping for %s", field.label).isNotNull();
          assertThat(property.isText()).as("%s is text", field.label).isTrue();
        });

    Property threatLevel = mappings.get(FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL.label);
    assertThat(threatLevel).isNotNull();
    assertThat(threatLevel.isInteger()).isTrue();

    // Sortable created-at / expires-at epoch-millis twins must be explicit long mappings so a fresh
    // index does not date-detect them; the created-at twin backs the WAIVER default created-desc sort.
    Property createdAtEpoch = mappings.get(FieldIdentifier.POLICY_WAIVER_CREATED_AT_EPOCH_MS.label);
    assertThat(createdAtEpoch).as("mapping for %s", FieldIdentifier.POLICY_WAIVER_CREATED_AT_EPOCH_MS.label)
        .isNotNull();
    assertThat(createdAtEpoch.isLong()).isTrue();
    Property expiresAtEpoch = mappings.get(FieldIdentifier.POLICY_WAIVER_EXPIRES_AT_EPOCH_MS.label);
    assertThat(expiresAtEpoch).as("mapping for %s", FieldIdentifier.POLICY_WAIVER_EXPIRES_AT_EPOCH_MS.label)
        .isNotNull();
    assertThat(expiresAtEpoch.isLong()).isTrue();
  }

  @Test
  public void policyWaiverRequestFields_areMappedWithExpectedTypes() {
    Map<String, Property> mappings = new IndexMapping().getMappings();

    // policyType (both waiver + request docs), request status, requester/reviewer name, review time
    // are keyword; rejection reason + note to reviewer are analyzed text.
    Stream.of(
        FieldIdentifier.POLICY_WAIVER_POLICY_TYPE,
        FieldIdentifier.POLICY_WAIVER_SCOPE,
        FieldIdentifier.POLICY_WAIVER_REQUEST_STATUS,
        FieldIdentifier.REQUESTER_NAME,
        FieldIdentifier.REVIEWER_NAME,
        FieldIdentifier.REVIEW_TIME)
        .forEach(field -> {
          Property property = mappings.get(field.label);
          assertThat(property).as("mapping for %s", field.label).isNotNull();
          assertThat(property.isKeyword()).as("%s is keyword", field.label).isTrue();
        });

    Stream.of(FieldIdentifier.REJECTION_REASON, FieldIdentifier.NOTE_TO_REVIEWER)
        .forEach(field -> {
          Property property = mappings.get(field.label);
          assertThat(property).as("mapping for %s", field.label).isNotNull();
          assertThat(property.isText()).as("%s is text", field.label).isTrue();
        });
  }

  @Test
  public void applicationEvaluationDenormFields_areMappedWithExpectedTypes() {
    Map<String, Property> mappings = new IndexMapping().getMappings();

    Property lastEval = mappings.get(FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label);
    assertThat(lastEval).as("mapping for %s", FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label)
        .isNotNull();
    // Explicit long mapping so a fresh index does not date-detect the epoch-millis into a date field.
    assertThat(lastEval.isLong()).isTrue();

    Property stageSeverity = mappings.get(FieldIdentifier.APPLICATION_STAGE_SEVERITY_COUNT.label);
    assertThat(stageSeverity).as("mapping for %s", FieldIdentifier.APPLICATION_STAGE_SEVERITY_COUNT.label)
        .isNotNull();
    assertThat(stageSeverity.isKeyword()).isTrue();
  }

  @Test
  public void componentViolationDenormFields_areMappedWithExpectedTypes() {
    Map<String, Property> mappings = new IndexMapping().getMappings();

    Stream.of(FieldIdentifier.COMPONENT_VIOLATION_POLICY_TYPE, FieldIdentifier.COMPONENT_VIOLATION_STATE)
        .forEach(field -> {
          Property property = mappings.get(field.label);
          assertThat(property).as("mapping for %s", field.label).isNotNull();
          assertThat(property.isKeyword()).as("%s is keyword", field.label).isTrue();
        });

    Property maxThreat = mappings.get(FieldIdentifier.COMPONENT_MAX_POLICY_THREAT_LEVEL.label);
    assertThat(maxThreat).as("mapping for %s", FieldIdentifier.COMPONENT_MAX_POLICY_THREAT_LEVEL.label)
        .isNotNull();
    // Integer so the policyThreatLevel range filter / sort works numerically (not date-detected).
    assertThat(maxThreat.isInteger()).isTrue();
  }

  @Test
  public void applicationViolationAggregateFields_areMappedWithExpectedTypes() {
    Map<String, Property> mappings = new IndexMapping().getMappings();

    // A4/A6 ints backing the RANGE filter and the numeric sorts.
    Stream.of(
        FieldIdentifier.APPLICATION_MAX_POLICY_THREAT_LEVEL,
        FieldIdentifier.APPLICATION_VIOLATION_STATE_SORT_ORDINAL)
        .forEach(field -> {
          Property property = mappings.get(field.label);
          assertThat(property).as("mapping for %s", field.label).isNotNull();
          assertThat(property.isInteger()).as("%s is integer", field.label).isTrue();
        });

    // A1/A2/A3 multi-valued keyword sets backing the TERMS filters.
    Stream.of(
        FieldIdentifier.APPLICATION_VIOLATION_STAGE,
        FieldIdentifier.APPLICATION_VIOLATION_POLICY_TYPE,
        FieldIdentifier.APPLICATION_VIOLATION_STATE)
        .forEach(field -> {
          Property property = mappings.get(field.label);
          assertThat(property).as("mapping for %s", field.label).isNotNull();
          assertThat(property.isKeyword()).as("%s is keyword", field.label).isTrue();
        });
  }

  private static KeywordProperty keywordPropertyFor(final String fieldName) {
    Property property = new IndexMapping().getMappings().get(fieldName);
    assertThat(property).isNotNull();
    assertThat(property.isKeyword()).isTrue();
    return property.keyword();
  }
}
