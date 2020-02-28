/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.search.docs.DocumentBuilder.FieldIdentifier;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AnalyzerProviderTest
    extends AbstractComponentTest
{
  @Inject
  private Analyzer analyzer;

  private List<String> tokens(String fieldName, String text) {
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

  private void testTokenStream_SingleTokenField(String fieldName) {
    assertThat(tokens(fieldName, "The Single Token")).containsExactly("the single token");
    assertThat(tokens(fieldName, "The-Single-Token")).containsExactly("the-single-token");
    assertThat(tokens(fieldName, "The_Single_Token")).containsExactly("the_single_token");
    assertThat(tokens(fieldName, "The.Single.Token")).containsExactly("the.single.token");
  }

  private void testTokenStream_MultiTokenField(String fieldName) {
    assertThat(tokens(fieldName, "The Many Tokens")).containsExactly("the", "many", "tokens");
    assertThat(tokens(fieldName, "The-Many-Tokens")).containsExactly("the", "many", "tokens");
    assertThat(tokens(fieldName, "The_Many_Tokens")).containsExactly("the_many_tokens");
    assertThat(tokens(fieldName, "The.Many.Tokens")).containsExactly("the.many.tokens");
  }

  @Test
  public void testTokenStream_ItemType() {
    testTokenStream_SingleTokenField(FieldIdentifier.ITEM_TYPE.label);
  }

  @Test
  public void testTokenStream_OrganizationId() {
    testTokenStream_SingleTokenField(FieldIdentifier.ORGANIZATION_ID.label);
  }

  @Test
  public void testTokenStream_OrganizationName() {
    testTokenStream_SingleTokenField(FieldIdentifier.ORGANIZATION_NAME.label);
  }

  @Test
  public void testTokenStream_ApplicationId() {
    testTokenStream_SingleTokenField(FieldIdentifier.APPLICATION_ID.label);
  }

  @Test
  public void testTokenStream_ApplicationPublicId() {
    testTokenStream_SingleTokenField(FieldIdentifier.APPLICATION_PUBLIC_ID.label);
  }

  @Test
  public void testTokenStream_ApplicationName() {
    testTokenStream_SingleTokenField(FieldIdentifier.APPLICATION_NAME.label);
  }

  @Test
  public void testTokenStream_ReportId() {
    testTokenStream_SingleTokenField(FieldIdentifier.REPORT_ID.label);
  }

  @Test
  public void testTokenStream_PolicyEvaluationStage() {
    testTokenStream_SingleTokenField(FieldIdentifier.POLICY_EVALUATION_STAGE.label);
  }

  @Test
  public void testTokenStream_PolicyId() {
    testTokenStream_SingleTokenField(FieldIdentifier.POLICY_ID.label);
  }

  @Test
  public void testTokenStream_PolicyName() {
    testTokenStream_SingleTokenField(FieldIdentifier.POLICY_NAME.label);
  }

  @Test
  public void testTokenStream_PolicyThreatCategory() {
    testTokenStream_SingleTokenField(FieldIdentifier.POLICY_THREAT_CATEGORY.label);
  }

  @Test
  public void testTokenStream_PolicyThreatLevel() {
    testTokenStream_SingleTokenField(FieldIdentifier.POLICY_THREAT_LEVEL.label);
  }

  @Test
  public void testTokenStream_ApplicationCategoryId() {
    testTokenStream_SingleTokenField(FieldIdentifier.APPLICATION_CATEGORY_ID.label);
  }

  @Test
  public void testTokenStream_ApplicationCategoryName() {
    testTokenStream_SingleTokenField(FieldIdentifier.APPLICATION_CATEGORY_NAME.label);
  }

  @Test
  public void testTokenStream_ApplicationCategoryColor() {
    testTokenStream_SingleTokenField(FieldIdentifier.APPLICATION_CATEGORY_COLOR.label);
  }

  @Test
  public void testTokenStream_ApplicationCategoryDescription() {
    testTokenStream_MultiTokenField(FieldIdentifier.APPLICATION_CATEGORY_DESCRIPTION.label);
  }

  @Test
  public void testTokenStream_ComponentLabelId() {
    testTokenStream_SingleTokenField(FieldIdentifier.COMPONENT_LABEL_ID.label);
  }

  @Test
  public void testTokenStream_ComponentLabelName() {
    testTokenStream_SingleTokenField(FieldIdentifier.COMPONENT_LABEL_NAME.label);
  }

  @Test
  public void testTokenStream_ComponentLabelColor() {
    testTokenStream_SingleTokenField(FieldIdentifier.COMPONENT_LABEL_COLOR.label);
  }

  @Test
  public void testTokenStream_ComponentLabelDescription() {
    testTokenStream_MultiTokenField(FieldIdentifier.COMPONENT_LABEL_DESCRIPTION.label);
  }

  @Test
  public void testTokenStream_ComponentHash() {
    testTokenStream_SingleTokenField(FieldIdentifier.COMPONENT_HASH.label);
  }

  @Test
  public void testTokenStream_ComponentFormat() {
    testTokenStream_SingleTokenField(FieldIdentifier.COMPONENT_FORMAT.label);
  }

  @Test
  public void testTokenStream_ComponentCoordinate() {
    testTokenStream_SingleTokenField(FieldIdentifier.COMPONENT_COORDINATE + "ArtifactId");
  }

  @Test
  public void testTokenStream_ComponentName() {
    testTokenStream_SingleTokenField(FieldIdentifier.COMPONENT_NAME.label);
  }

  @Test
  public void testTokenStream_VulnerabilityId() {
    testTokenStream_SingleTokenField(FieldIdentifier.VULNERABILITY_ID.label);
  }

  @Test
  public void testTokenStream_VulnerabilityStatus() {
    testTokenStream_SingleTokenField(FieldIdentifier.VULNERABILITY_STATUS.label);
  }

  @Test
  public void testTokenStream_VulnerabilityDescription() {
    testTokenStream_MultiTokenField(FieldIdentifier.VULNERABILITY_DESCRIPTION.label);
  }
}
