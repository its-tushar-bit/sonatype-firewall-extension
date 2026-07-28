/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LuceneComponentsTest
    extends AbstractComponentTest
{
  @Inject
  private LuceneComponents luceneComponents;

  private List<String> tokens(Analyzer analyzer, String fieldName, String text) {
    List<String> tokens = new ArrayList<>();
    try (TokenStream tokenStream = analyzer.tokenStream(fieldName, text)) {
      CharTermAttribute attribute = tokenStream.addAttribute(CharTermAttribute.class);
      tokenStream.reset();
      while (tokenStream.incrementToken()) {
        tokens.add(attribute.toString());
      }
      tokenStream.end();
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return tokens;
  }

  private void testNewAnalyzerForSearch_SingleTokenField(String fieldName) {
    Analyzer analyzer = luceneComponents.newAnalyzerForSearch();
    assertThat(tokens(analyzer, fieldName, "The Single Token")).containsExactly("the single token");
    assertThat(tokens(analyzer, fieldName, "The-Single-Token")).containsExactly("the-single-token");
    assertThat(tokens(analyzer, fieldName, "The_Single_Token")).containsExactly("the_single_token");
    assertThat(tokens(analyzer, fieldName, "The.Single.Token")).containsExactly("the.single.token");
  }

  private void testNewAnalyzerForSearch_MultiTokenField(String fieldName) {
    Analyzer analyzer = luceneComponents.newAnalyzerForSearch();
    assertThat(tokens(analyzer, fieldName, "The Many Tokens")).containsExactly("the", "many", "tokens");
    assertThat(tokens(analyzer, fieldName, "The-Many-Tokens")).containsExactly("the", "many", "tokens");
    assertThat(tokens(analyzer, fieldName, "The_Many_Tokens")).containsExactly("the_many_tokens");
    assertThat(tokens(analyzer, fieldName, "The.Many.Tokens")).containsExactly("the.many.tokens");
  }

  @Test
  public void testNewAnalyzerForSearch_ItemType() {
    testNewAnalyzerForSearch_SingleTokenField(FieldIdentifier.ITEM_TYPE.label);
  }

  @Test
  public void testNewAnalyzerForSearch_OrganizationId() {
    testNewAnalyzerForSearch_SingleTokenField(FieldIdentifier.ORGANIZATION_ID.label);
  }

  @Test
  public void testNewAnalyzerForSearch_OrganizationName() {
    testNewAnalyzerForSearch_SingleTokenField(FieldIdentifier.ORGANIZATION_NAME.label);
  }

  @Test
  public void testNewAnalyzerForSearch_ApplicationId() {
    testNewAnalyzerForSearch_SingleTokenField(FieldIdentifier.APPLICATION_ID.label);
  }

  @Test
  public void testNewAnalyzerForSearch_ApplicationPublicId() {
    testNewAnalyzerForSearch_SingleTokenField(FieldIdentifier.APPLICATION_PUBLIC_ID.label);
  }

  @Test
  public void testNewAnalyzerForSearch_ApplicationName() {
    testNewAnalyzerForSearch_SingleTokenField(FieldIdentifier.APPLICATION_NAME.label);
  }

  @Test
  public void testNewAnalyzerForSearch_ReportId() {
    testNewAnalyzerForSearch_SingleTokenField(FieldIdentifier.REPORT_ID.label);
  }

  @Test
  public void testNewAnalyzerForSearch_PolicyEvaluationStage() {
    testNewAnalyzerForSearch_SingleTokenField(FieldIdentifier.POLICY_EVALUATION_STAGE.label);
  }

  @Test
  public void testNewAnalyzerForSearch_PolicyId() {
    testNewAnalyzerForSearch_SingleTokenField(FieldIdentifier.POLICY_ID.label);
  }

  @Test
  public void testNewAnalyzerForSearch_PolicyName() {
    testNewAnalyzerForSearch_SingleTokenField(FieldIdentifier.POLICY_NAME.label);
  }

  @Test
  public void testNewAnalyzerForSearch_PolicyThreatCategory() {
    testNewAnalyzerForSearch_SingleTokenField(FieldIdentifier.POLICY_THREAT_CATEGORY.label);
  }

  @Test
  public void testNewAnalyzerForSearch_PolicyThreatLevel() {
    testNewAnalyzerForSearch_SingleTokenField(FieldIdentifier.POLICY_THREAT_LEVEL.label);
  }

  @Test
  public void testNewAnalyzerForSearch_ApplicationCategoryId() {
    testNewAnalyzerForSearch_SingleTokenField(FieldIdentifier.APPLICATION_CATEGORY_ID.label);
  }

  @Test
  public void testNewAnalyzerForSearch_ApplicationCategoryName() {
    testNewAnalyzerForSearch_SingleTokenField(FieldIdentifier.APPLICATION_CATEGORY_NAME.label);
  }

  @Test
  public void testNewAnalyzerForSearch_ApplicationCategoryColor() {
    testNewAnalyzerForSearch_SingleTokenField(FieldIdentifier.APPLICATION_CATEGORY_COLOR.label);
  }

  @Test
  public void testNewAnalyzerForSearch_ApplicationCategoryDescription() {
    testNewAnalyzerForSearch_MultiTokenField(FieldIdentifier.APPLICATION_CATEGORY_DESCRIPTION.label);
  }

  @Test
  public void testNewAnalyzerForSearch_ComponentLabelId() {
    testNewAnalyzerForSearch_SingleTokenField(FieldIdentifier.COMPONENT_LABEL_ID.label);
  }

  @Test
  public void testNewAnalyzerForSearch_ComponentLabelName() {
    testNewAnalyzerForSearch_SingleTokenField(FieldIdentifier.COMPONENT_LABEL_NAME.label);
  }

  @Test
  public void testNewAnalyzerForSearch_ComponentLabelColor() {
    testNewAnalyzerForSearch_SingleTokenField(FieldIdentifier.COMPONENT_LABEL_COLOR.label);
  }

  @Test
  public void testNewAnalyzerForSearch_ComponentLabelDescription() {
    testNewAnalyzerForSearch_MultiTokenField(FieldIdentifier.COMPONENT_LABEL_DESCRIPTION.label);
  }

  @Test
  public void testNewAnalyzerForSearch_ComponentHash() {
    testNewAnalyzerForSearch_SingleTokenField(FieldIdentifier.COMPONENT_HASH.label);
  }

  @Test
  public void testNewAnalyzerForSearch_ComponentFormat() {
    testNewAnalyzerForSearch_SingleTokenField(FieldIdentifier.COMPONENT_FORMAT.label);
  }

  @Test
  public void testNewAnalyzerForSearch_ComponentCoordinate() {
    testNewAnalyzerForSearch_SingleTokenField(FieldIdentifier.COMPONENT_COORDINATE + "ArtifactId");
  }

  @Test
  public void testNewAnalyzerForSearch_ComponentName() {
    testNewAnalyzerForSearch_SingleTokenField(FieldIdentifier.COMPONENT_NAME.label);
  }

  @Test
  public void testNewAnalyzerForSearch_VulnerabilityId() {
    testNewAnalyzerForSearch_SingleTokenField(FieldIdentifier.VULNERABILITY_ID.label);
  }

  @Test
  public void testNewAnalyzerForSearch_VulnerabilityStatus() {
    testNewAnalyzerForSearch_SingleTokenField(FieldIdentifier.VULNERABILITY_STATUS.label);
  }

  @Test
  public void testNewAnalyzerForSearch_VulnerabilityDescription() {
    testNewAnalyzerForSearch_MultiTokenField(FieldIdentifier.VULNERABILITY_DESCRIPTION.label);
  }

  @Test
  public void testNewAnalyzerForSearch_RejectionReason() {
    testNewAnalyzerForSearch_MultiTokenField(FieldIdentifier.REJECTION_REASON.label);
  }

  @Test
  public void testNewAnalyzerForSearch_NoteToReviewer() {
    testNewAnalyzerForSearch_MultiTokenField(FieldIdentifier.NOTE_TO_REVIEWER.label);
  }

  @Test
  public void testNewQueryParser_DefaultField() {
    assertThat(luceneComponents.newQueryParser().apply("value"))
        .isEqualTo(new TermQuery(new Term(FieldIdentifier.VULNERABILITY_ID.label, "value")));
  }
}
