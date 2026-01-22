/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.UriBuilder;

public class PaginationResponseBuilder<T>
{
  public static final String PAGE_PARAM = "page";

  public static final String FIRST_REL = "first";

  public static final String LAST_REL = "last";

  public static final String NEXT_REL = "next";

  public static final String PREV_REL = "prev";

  private final String absolutePath;

  private final long page;

  private final int pageSize;

  private final ApiPageResult<T> result;

  private Map<String, List<String>> queryParameters;

  public PaginationResponseBuilder(
      final String absolutePath,
      final long page,
      final int pageSize,
      final ApiPageResult<T> result)
  {
    if (page < 1) {
      throw new IllegalArgumentException("Page number should be greater than 0");
    }

    if (pageSize < 1) {
      throw new IllegalArgumentException("Page size should be greater than 0");
    }

    this.absolutePath = absolutePath;
    this.page = page;
    this.pageSize = pageSize;
    this.result = result;
  }

  public PaginationResponseBuilder<T> queryParameters(Map<String, List<String>> queryParameters) {
    this.queryParameters = queryParameters;
    return this;
  }

  public Response build() {
    // get a path build of the request absolute path
    final UriBuilder pathBuilder = UriBuilder.fromPath(absolutePath);

    // append query params to absolute path
    if (queryParameters != null) {
      for (Entry<String, List<String>> param : queryParameters.entrySet()) {
        pathBuilder.queryParam(param.getKey(), param.getValue().toArray());
      }
    }

    // add pagination Link headers
    final ResponseBuilder responseBuilder = addPaginationLinkHeaders(pathBuilder);

    return responseBuilder.build();
  }

  public static long calculateLastPage(final int pageSize, final double total) {
    return (long) Math.ceil(total / pageSize);
  }

  private ResponseBuilder addPaginationLinkHeaders(final UriBuilder pathBuilder) {
    final ResponseBuilder responseBuilder = Response.ok(result);

    // if total records is zero, no pagination exists so link headers are not needed
    if (result.getTotal() == 0) {
      return responseBuilder;
    }

    List<Link> links = new ArrayList<>();
    final long lastPage = calculateLastPage(pageSize, result.getTotal());

    // add a link header of first page
    links.add(Link.fromUri(pathBuilder.replaceQueryParam(PAGE_PARAM, 1).build()).rel(FIRST_REL).build());
    // add a link header of last page
    links.add(Link.fromUri(pathBuilder.replaceQueryParam(PAGE_PARAM, lastPage).build()).rel(LAST_REL).build());

    // add a link header to next page, if current page is not already the last page
    if (page < lastPage) {
      links.add(Link.fromUri(pathBuilder.replaceQueryParam(PAGE_PARAM, page + 1).build()).rel(NEXT_REL).build());
    }

    // add a link header to previous page, if current page is not already the first page
    if (page > 1) {
      links.add(Link.fromUri(pathBuilder.replaceQueryParam(PAGE_PARAM, page - 1).build()).rel(PREV_REL).build());
    }

    for (Link link : links) {
      responseBuilder.link(link.getUri(), link.getRel());
    }

    return responseBuilder;
  }
}
