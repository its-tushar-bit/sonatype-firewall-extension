/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

/**
 * Parameter holder for {@code GET /rest/search/results}.
 *
 * <p>
 * Plain value object. The JAX-RS resource constructs it from raw query-string parameters after running
 * its own manual validation (so the feature-flag gate can answer 404 before the request leaks its
 * existence via a 400). Bean-validation annotations are intentionally NOT placed on this class: the
 * controller surface is the validation boundary.
 *
 * <p>
 * Unknown query keys are ignored (JAX-RS default). Filter chips embedded inside {@code q} (e.g.
 * {@code q=react itemType:APPLICATION policyThreatLevel:[7 TO 10]}) are parsed by the AST parser and
 * routed through the {@code FieldMap} vocabulary; there is no separate JSON filter parameter.
 *
 * <p>
 * Contract enforced by the controller and {@link ResultsService}:
 *
 * <ul>
 * <li>{@code q} is non-blank, length 1..{@value #MAX_QUERY_LENGTH}, no control characters.</li>
 * <li>{@code tab} is one of {@link Tab}.</li>
 * <li>{@code page} is 1..10000. The product offset {@code (page-1) * pageSize} returns a {@code long}
 * to avoid silent {@code int} overflow.</li>
 * <li>{@code pageSize} is 1..{@value #MAX_PAGE_SIZE} (default {@value #DEFAULT_PAGE_SIZE}).</li>
 * <li>{@code page+pageSize} is supported for offsets strictly less than
 * {@link #DEEP_PAGINATION_THRESHOLD}; deeper paging (offset &ge; {@code DEEP_PAGINATION_THRESHOLD})
 * requires {@code searchAfter}.</li>
 * <li>When both {@code page} and {@code searchAfter} are provided, {@code searchAfter} wins.</li>
 * </ul>
 */
public final class ResultsRequest
{
  /** Maximum allowed length of {@code q} to limit the size of OpenSearch query strings. */
  public static final int MAX_QUERY_LENGTH = 500;

  /** Default page size when {@code pageSize} is omitted. */
  public static final int DEFAULT_PAGE_SIZE = 25;

  /** Hard cap on {@code pageSize}. */
  public static final int MAX_PAGE_SIZE = 100;

  /** Hard cap on {@code page}. */
  public static final int MAX_PAGE = 10_000;

  /**
   * page+pageSize is supported up to and including this offset; deeper paging requires
   * {@code searchAfter}. The boundary itself is excluded by the {@code >=} gate in
   * {@link ResultsService}.
   */
  public static final int DEEP_PAGINATION_THRESHOLD = 1000;

  private final String q;

  private final Tab tab;

  private final int page;

  private final int pageSize;

  private final String sort;

  private final String searchAfter;

  private final SearchSource source;

  public ResultsRequest(
      String q,
      Tab tab,
      Integer page,
      Integer pageSize,
      String sort,
      String searchAfter)
  {
    this(q, tab, page, pageSize, sort, searchAfter, SearchSource.DEFAULT);
  }

  public ResultsRequest(
      String q,
      Tab tab,
      Integer page,
      Integer pageSize,
      String sort,
      String searchAfter,
      SearchSource source)
  {
    int p = page == null ? 1 : page;
    if (p < 1) {
      throw new IllegalArgumentException("page must be >= 1");
    }
    int ps = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
    if (ps < 1 || ps > MAX_PAGE_SIZE) {
      throw new IllegalArgumentException("pageSize must be in [1, " + MAX_PAGE_SIZE + "]");
    }
    this.q = q;
    this.tab = tab;
    this.page = p;
    this.pageSize = ps;
    this.sort = sort;
    this.searchAfter = searchAfter;
    this.source = source == null ? SearchSource.DEFAULT : source;
  }

  public String getQ() {
    return q;
  }

  public Tab getTab() {
    return tab;
  }

  public int getPage() {
    return page;
  }

  public int getPageSize() {
    return pageSize;
  }

  public String getSort() {
    return sort;
  }

  public String getSearchAfter() {
    return searchAfter;
  }

  public SearchSource getSource() {
    return source;
  }

  /** When {@code searchAfter} is present we treat it as the authoritative cursor and ignore {@code page}. */
  public boolean usesCursor() {
    return searchAfter != null && !searchAfter.isBlank();
  }

  /**
   * Zero-indexed offset implied by {@code page} and {@code pageSize}. Returns {@code long} to avoid
   * silent {@code int} overflow when an upstream caller forwards a {@code page} value close to
   * {@code Integer.MAX_VALUE}.
   */
  public long offset() {
    return (long) (page - 1) * (long) pageSize;
  }
}
