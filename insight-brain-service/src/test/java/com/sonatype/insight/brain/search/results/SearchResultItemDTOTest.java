/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.results;

import com.sonatype.insight.brain.search.index.FieldIdentifier;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.Field.Store;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SearchResultItemDTOTest
{
  @Test
  public void constructor_parsesNumericVulnerabilitySeverity() {
    Document document = new Document();
    document.add(new StringField(FieldIdentifier.VULNERABILITY_SEVERITY.label, "7.5", Store.YES));

    SearchResultItemDTO item = new SearchResultItemDTO(document);

    assertThat(item.vulnerabilitySeverity).isEqualTo(7.5f);
  }

  @Test
  public void constructor_ignoresMalformedVulnerabilitySeverity() {
    Document document = new Document();
    document.add(new StringField(FieldIdentifier.VULNERABILITY_SEVERITY.label, "not-a-float", Store.YES));

    SearchResultItemDTO item = new SearchResultItemDTO(document);

    assertThat(item.vulnerabilitySeverity).isNull();
  }

  @Test
  public void parseFloatOrNull_returnsNullForBlankMalformedAndNull() {
    assertThat(SearchResultItemDTO.parseFloatOrNull(null)).isNull();
    assertThat(SearchResultItemDTO.parseFloatOrNull("")).isNull();
    assertThat(SearchResultItemDTO.parseFloatOrNull("abc")).isNull();
    assertThat(SearchResultItemDTO.parseFloatOrNull("9.1")).isEqualTo(9.1f);
  }
}
