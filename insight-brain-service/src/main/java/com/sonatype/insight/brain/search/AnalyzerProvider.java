/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Provider;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import static com.sonatype.insight.brain.search.docs.DocumentBuilder.FieldIdentifier.APPLICATION_NAME;
import static com.sonatype.insight.brain.search.docs.DocumentBuilder.FieldIdentifier.ORGANIZATION_NAME;
import static com.sonatype.insight.brain.search.docs.DocumentBuilder.FieldIdentifier.VULNERABILITY_DESCRIPTION;

@Named
public class AnalyzerProvider
    implements Provider<Analyzer>
{
  @Override
  public Analyzer get() {
    Analyzer standardAnalyzer = new StandardAnalyzer();
    Map<String, Analyzer> fieldAnalyzers = new HashMap<>();
    fieldAnalyzers.put(VULNERABILITY_DESCRIPTION.label, standardAnalyzer);
    fieldAnalyzers.put(APPLICATION_NAME.label, standardAnalyzer);
    fieldAnalyzers.put(ORGANIZATION_NAME.label, standardAnalyzer);
    return new PerFieldAnalyzerWrapper(new LowerCaseKeywordAnalyzer(), fieldAnalyzers);
  }
}
