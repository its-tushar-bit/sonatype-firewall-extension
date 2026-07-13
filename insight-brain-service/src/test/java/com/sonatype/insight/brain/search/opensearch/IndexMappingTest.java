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
import org.junit.Test;
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
   * Pins the lowercase-normalizer contract for the major ID-family keyword fields. A regression
   * that strips the normalizer from any of these (or, conversely, adds one to ALLOWED_CONTEXT_IDS)
   * would silently change query semantics for callers relying on the legacy behaviour.
   */
  @Test
  public void idFamilyKeywordFields_haveLowerCaseNormalizer() {
    Stream.of(
        FieldIdentifier.ORGANIZATION_ID,
        FieldIdentifier.APPLICATION_ID,
        FieldIdentifier.APPLICATION_PUBLIC_ID,
        FieldIdentifier.PARENT_ORGANIZATION_ID,
        FieldIdentifier.POLICY_ID,
        FieldIdentifier.POLICY_VIOLATION_POLICY_ID,
        FieldIdentifier.COMPONENT_LABEL_ID,
        FieldIdentifier.APPLICATION_CATEGORY_ID)
        .forEach(field -> assertThat(keywordPropertyFor(field.label).normalizer())
            .as("normalizer for %s", field.label)
            .isEqualTo("lowercase"));

    assertThat(keywordPropertyFor(FieldIdentifier.ALLOWED_CONTEXT_IDS.label).normalizer())
        .as("ALLOWED_CONTEXT_IDS must not pick up the lowercase normalizer")
        .isNull();
  }

  private static KeywordProperty keywordPropertyFor(final String fieldName) {
    Property property = new IndexMapping().getMappings().get(fieldName);
    assertThat(property).isNotNull();
    assertThat(property.isKeyword()).isTrue();
    return property.keyword();
  }
}
