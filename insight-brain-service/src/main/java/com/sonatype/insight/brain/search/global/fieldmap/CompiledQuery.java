/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global.fieldmap;

import java.util.List;

import org.apache.lucene.search.Query;

/**
 * Compilation result: an always-executable Lucene query plus non-fatal warnings (unknown fields,
 * value-shape or enum mismatches).
 */
public record CompiledQuery(Query luceneQuery, List<String> warnings)
{
  public CompiledQuery {
    warnings = List.copyOf(warnings);
  }
}
