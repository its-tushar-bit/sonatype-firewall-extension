/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

/**
 * Orchestrates the IQ-local and catalog legs of the Global Search typeahead.
 *
 * <p>
 * Implementations always return an HTTP-200-equivalent result; catalog failures degrade gracefully to
 * an empty catalog group and {@code catalogAvailable: false}, and never fail the whole response.
 */
public interface SuggestService
{
  /**
   * Run the typeahead pipeline for the given query against the given {@link SearchSource}.
   * {@code source=local} covers every entity type; {@code source=catalog} covers only the
   * catalog-served types (COMPONENT and VULNERABILITY). Exactly one source per request; no
   * cross-source fall-through.
   *
   * @param query non-blank, length-validated by the caller
   * @param source the requested source; never {@code null}
   * @return the suggest response; never {@code null}
   */
  SuggestResponse suggest(String query, SearchSource source);
}
