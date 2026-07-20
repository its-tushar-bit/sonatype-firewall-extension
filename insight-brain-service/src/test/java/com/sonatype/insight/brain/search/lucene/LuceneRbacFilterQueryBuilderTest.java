/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.util.Map;
import java.util.Optional;

import com.sonatype.insight.brain.model.OwnerType;

import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LuceneRbacFilterQueryBuilderTest
{
  @Test
  public void build_nonAppOrgOwnerTypesFailClosed() {
    Query filter = LuceneRbacFilterQueryBuilder.build(
        Optional.of(Map.of("repo-1", OwnerType.REPOSITORY_CONTAINER)));
    assertThat(filter).isInstanceOf(MatchNoDocsQuery.class);
  }
}
