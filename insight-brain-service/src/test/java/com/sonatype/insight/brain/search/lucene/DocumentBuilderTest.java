/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.service.InsightWork;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;
import org.junit.Test;

public class DocumentBuilderTest
{
  @Test
  public void build_writesDocumentKeyAsSingleStoredKeyword() {
    // Only one documentKey field; the sort doc-values twin is added later in LuceneIndexingContext.
    Document doc = new DocumentBuilder(ItemType.APPLICATION)
        .setApplicationId("app-1")
        .setApplicationName("Acme")
        .build();

    List<IndexableField> keyFields = doc.getFields()
        .stream()
        .filter(f -> FieldIdentifier.DOCUMENT_KEY.label.equals(f.name()))
        .toList();

    assertThat(keyFields).extracting(Object::getClass).containsExactly(StringField.class);
    assertThat(doc.get(FieldIdentifier.DOCUMENT_KEY.label)).isNotBlank();
  }

  @Test
  public void documentKey_survivesOpenSearchRoundTripWithoutNpe() {
    // Regression: a same-named doc-values twin used to serialize documentKey as [hash, null],
    // NPEing on mapToDocument read-back and 500ing every OpenSearch query.
    ConversionHelper conversionHelper = new ConversionHelper(new LuceneComponents(mock(InsightWork.class)));
    Document built = new DocumentBuilder(ItemType.APPLICATION)
        .setApplicationId("app-1")
        .setApplicationName("Acme")
        .build();
    String originalKey = built.get(FieldIdentifier.DOCUMENT_KEY.label);

    Map<String, Object> source = conversionHelper.documentToMap(built);

    assertThat(source.get(FieldIdentifier.DOCUMENT_KEY.label)).isEqualTo(originalKey);
    Document[] roundTripped = new Document[1];
    assertThatCode(() -> roundTripped[0] = conversionHelper.mapToDocument(source)).doesNotThrowAnyException();
    assertThat(roundTripped[0].get(FieldIdentifier.DOCUMENT_KEY.label)).isEqualTo(originalKey);
  }

  @Test
  public void policyWaiverDates_surviveOpenSearchRoundTripWithoutNullInList() {
    // Regression: a same-named doc-values twin on the date fields used to serialize them as
    // [iso8601, null] into _source, NPEing on mapToDocument read-back.
    ConversionHelper conversionHelper = new ConversionHelper(new LuceneComponents(mock(InsightWork.class)));
    Document built = new DocumentBuilder(ItemType.POLICY_WAIVER)
        .setPolicyWaiverCreatedAt("2024-01-01T00:00:00.000Z")
        .setPolicyWaiverExpiresAt("2024-06-01T00:00:00.000Z")
        .build();

    List<IndexableField> createdFields = built.getFields()
        .stream()
        .filter(f -> FieldIdentifier.POLICY_WAIVER_CREATED_AT.label.equals(f.name()))
        .toList();
    assertThat(createdFields).extracting(Object::getClass).containsExactly(StringField.class);

    Map<String, Object> source = conversionHelper.documentToMap(built);
    assertThat(source.get(FieldIdentifier.POLICY_WAIVER_CREATED_AT.label)).isEqualTo("2024-01-01T00:00:00.000Z");
    assertThat(source.get(FieldIdentifier.POLICY_WAIVER_EXPIRES_AT.label)).isEqualTo("2024-06-01T00:00:00.000Z");
    assertThatCode(() -> conversionHelper.mapToDocument(source)).doesNotThrowAnyException();
  }

  @Test
  public void documentKey_isDeterministicForIdenticalContent() {
    String a = keyOf(new DocumentBuilder(ItemType.APPLICATION)
        .setApplicationId("app-1")
        .setApplicationName("Acme")
        .build());
    String b = keyOf(new DocumentBuilder(ItemType.APPLICATION)
        .setApplicationId("app-1")
        .setApplicationName("Acme")
        .build());

    assertThat(a).isEqualTo(b);
  }

  @Test
  public void documentKey_differsForDifferentContent() {
    String a = keyOf(new DocumentBuilder(ItemType.APPLICATION)
        .setApplicationId("app-1")
        .setApplicationName("Acme")
        .build());
    String b = keyOf(new DocumentBuilder(ItemType.APPLICATION)
        .setApplicationId("app-2")
        .setApplicationName("Acme")
        .build());
    String differentType = keyOf(new DocumentBuilder(ItemType.ORGANIZATION)
        .setOrganizationId("app-1")
        .setOrganizationName("Acme")
        .build());

    assertThat(a).isNotEqualTo(b);
    assertThat(a).isNotEqualTo(differentType);
  }

  @Test
  public void documentKey_ignoresAllowedContextIdsClosure() {
    // The permission closure can change without the document's identity changing, so it is excluded from the key.
    String withoutClosure = keyOf(new DocumentBuilder(ItemType.APPLICATION)
        .setApplicationId("app-1")
        .setApplicationName("Acme")
        .build());
    String withClosure = keyOf(new DocumentBuilder(ItemType.APPLICATION)
        .setApplicationId("app-1")
        .setApplicationName("Acme")
        .setAllowedContextIds(List.of("app-1", "org-1"))
        .build());

    assertThat(withoutClosure).isEqualTo(withClosure);
  }

  private static String keyOf(final Document doc) {
    return doc.get(FieldIdentifier.DOCUMENT_KEY.label);
  }
}
