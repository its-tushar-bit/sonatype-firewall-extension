/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.sonatype.insight.brain.model.OwnerType;

import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermInSetQuery;
import org.apache.lucene.util.BytesRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.ORGANIZATION_ID;
import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;

public final class LuceneRbacFilterQueryBuilder
{
  private static final Logger log = LoggerFactory.getLogger(LuceneRbacFilterQueryBuilder.class);

  private LuceneRbacFilterQueryBuilder() {
  }

  /**
   * Builds the legacy Lucene RBAC filter shape. Empty optional means unrestricted/global access;
   * an empty map means restricted but no readable application or organization contexts.
   */
  public static Query build(final Optional<Map<String, OwnerType>> readableContexts) {
    if (readableContexts.isEmpty()) {
      return new MatchAllDocsQuery();
    }

    Map<String, OwnerType> contextIdsWithReadPermissionMap = readableContexts.get();
    if (contextIdsWithReadPermissionMap.isEmpty()) {
      return new MatchNoDocsQuery();
    }

    List<BytesRef> applicationTerms = new ArrayList<>();
    List<BytesRef> organizationTerms = new ArrayList<>();
    contextIdsWithReadPermissionMap.forEach((contextId, type) -> {
      BytesRef term = new BytesRef(contextId.toLowerCase(Locale.ROOT));
      if (OwnerType.APPLICATION.equals(type)) {
        applicationTerms.add(term);
      }
      else if (OwnerType.ORGANIZATION.equals(type)) {
        organizationTerms.add(term);
      }
    });

    if (applicationTerms.isEmpty() && organizationTerms.isEmpty()) {
      // Non-app/non-org owner types (e.g. repository containers) are not indexed for search RBAC;
      // fail closed rather than matching everything.
      log.debug(
          "RBAC context map had {} entries but no APPLICATION/ORGANIZATION types; returning MatchNoDocsQuery",
          contextIdsWithReadPermissionMap.size());
      return new MatchNoDocsQuery();
    }

    BooleanQuery.Builder rbac = new BooleanQuery.Builder();
    if (!applicationTerms.isEmpty()) {
      rbac.add(new TermInSetQuery(APPLICATION_ID.label, applicationTerms), SHOULD);
    }
    if (!organizationTerms.isEmpty()) {
      rbac.add(new TermInSetQuery(ORGANIZATION_ID.label, organizationTerms), SHOULD);
    }
    rbac.setMinimumNumberShouldMatch(1);
    return rbac.build();
  }
}
