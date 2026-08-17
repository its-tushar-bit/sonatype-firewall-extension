/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PaginationResponseBuilderTest
{
  private static final String ABSOLUTE_PATH = "http://localhost/baseUrl/";

  @Test
  public void testPage2WithResultsAnd2QueryParams() {
    // SETUP
    final ApiPageResult<String> apiPageResult = new ApiPageResult<>(10, 2, 3, Arrays.asList("result1", "result2"));
    Map<String, List<String>> queryParams = new HashMap<>();
    queryParams.put("param1", Arrays.asList("value1", "value2"));
    queryParams.put("param2", Arrays.asList("value3", "value4"));

    // EXECUTE
    final PaginationResponseBuilder<String> builder = new PaginationResponseBuilder<>(ABSOLUTE_PATH, 2, 3,
        apiPageResult);
    builder.queryParameters(queryParams);
    final Response response = builder.build();

    // VERIFY
    assertThat(response.getEntity()).isEqualTo(apiPageResult);
    assertThat(response.getLink(PaginationResponseBuilder.FIRST_REL)).hasToString(
        "<http://localhost/baseUrl/?page=1&param1=value1&param1=value2&param2=value3&param2=value4>; rel=\"first\"");
    assertThat(response.getLink(PaginationResponseBuilder.LAST_REL)).hasToString(
        "<http://localhost/baseUrl/?page=4&param1=value1&param1=value2&param2=value3&param2=value4>; rel=\"last\"");
    assertThat(response.getLink(PaginationResponseBuilder.NEXT_REL)).hasToString(
        "<http://localhost/baseUrl/?page=3&param1=value1&param1=value2&param2=value3&param2=value4>; rel=\"next\"");
    assertThat(response.getLink(PaginationResponseBuilder.PREV_REL)).hasToString(
        "<http://localhost/baseUrl/?page=1&param1=value1&param1=value2&param2=value3&param2=value4>; rel=\"prev\"");
  }

  @Test
  public void testPage1WithResultsAnd2QueryParams() {
    // SETUP
    final ApiPageResult<String> apiPageResult = new ApiPageResult<>(10, 1, 3, Arrays.asList("result1", "result2"));
    Map<String, List<String>> queryParams = new HashMap<>();
    queryParams.put("param1", Arrays.asList("value1", "value2"));
    queryParams.put("param2", Arrays.asList("value3", "value4"));

    // EXECUTE
    final PaginationResponseBuilder<String> builder = new PaginationResponseBuilder<>(ABSOLUTE_PATH, 1, 3,
        apiPageResult);
    builder.queryParameters(queryParams);
    final Response response = builder.build();

    // VERIFY
    assertThat(response.getEntity()).isEqualTo(apiPageResult);
    assertThat(response.getLink(PaginationResponseBuilder.FIRST_REL)).hasToString(
        "<http://localhost/baseUrl/?page=1&param1=value1&param1=value2&param2=value3&param2=value4>; rel=\"first\"");
    assertThat(response.getLink(PaginationResponseBuilder.LAST_REL)).hasToString(
        "<http://localhost/baseUrl/?page=4&param1=value1&param1=value2&param2=value3&param2=value4>; rel=\"last\"");
    assertThat(response.getLink(PaginationResponseBuilder.NEXT_REL)).hasToString(
        "<http://localhost/baseUrl/?page=2&param1=value1&param1=value2&param2=value3&param2=value4>; rel=\"next\"");
    assertThat(response.getLink(PaginationResponseBuilder.PREV_REL)).isNull();
  }

  @Test
  public void testPage4WithResultsAnd2QueryParams() {
    // SETUP
    final ApiPageResult<String> apiPageResult = new ApiPageResult<>(10, 4, 3, Arrays.asList("result1", "result2"));
    Map<String, List<String>> queryParams = new HashMap<>();
    queryParams.put("param1", Arrays.asList("value1", "value2"));
    queryParams.put("param2", Arrays.asList("value3", "value4"));

    // EXECUTE
    final PaginationResponseBuilder<String> builder = new PaginationResponseBuilder<>(ABSOLUTE_PATH, 4, 3,
        apiPageResult);
    builder.queryParameters(queryParams);
    final Response response = builder.build();

    // VERIFY
    assertThat(response.getEntity()).isEqualTo(apiPageResult);
    assertThat(response.getLink(PaginationResponseBuilder.FIRST_REL)).hasToString(
        "<http://localhost/baseUrl/?page=1&param1=value1&param1=value2&param2=value3&param2=value4>; rel=\"first\"");
    assertThat(response.getLink(PaginationResponseBuilder.LAST_REL)).hasToString(
        "<http://localhost/baseUrl/?page=4&param1=value1&param1=value2&param2=value3&param2=value4>; rel=\"last\"");
    assertThat(response.getLink(PaginationResponseBuilder.NEXT_REL)).isNull();
    assertThat(response.getLink(PaginationResponseBuilder.PREV_REL)).hasToString(
        "<http://localhost/baseUrl/?page=3&param1=value1&param1=value2&param2=value3&param2=value4>; rel=\"prev\"");
  }

  @Test
  public void testPage4WithResultsAndNoQueryParams() {
    // SETUP
    final ApiPageResult<String> apiPageResult = new ApiPageResult<>(10, 4, 3, Arrays.asList("result1", "result2"));

    // EXECUTE
    final PaginationResponseBuilder<String> builder = new PaginationResponseBuilder<>(ABSOLUTE_PATH, 4, 3,
        apiPageResult);
    final Response response = builder.build();

    // VERIFY
    assertThat(response.getEntity()).isEqualTo(apiPageResult);
    assertThat(response.getLink(PaginationResponseBuilder.FIRST_REL))
        .hasToString("<http://localhost/baseUrl/?page=1>; rel=\"first\"");
    assertThat(response.getLink(PaginationResponseBuilder.LAST_REL))
        .hasToString("<http://localhost/baseUrl/?page=4>; rel=\"last\"");
    assertThat(response.getLink(PaginationResponseBuilder.NEXT_REL)).isNull();
    assertThat(response.getLink(PaginationResponseBuilder.PREV_REL))
        .hasToString("<http://localhost/baseUrl/?page=3>; rel=\"prev\"");
  }

  @Test
  public void testPage2WithResultsAnd2QueryParamsAndResultsIsEmpty() {
    // SETUP
    final ApiPageResult<String> apiPageResult = new ApiPageResult<>(0, 2, 3, Collections.emptyList());
    Map<String, List<String>> queryParams = new HashMap<>();
    queryParams.put("param1", Arrays.asList("value1", "value2"));
    queryParams.put("param2", Arrays.asList("value3", "value4"));

    // EXECUTE
    final PaginationResponseBuilder<String> builder = new PaginationResponseBuilder<>(ABSOLUTE_PATH, 2, 3,
        apiPageResult);
    builder.queryParameters(queryParams);
    final Response response = builder.build();

    // VERIFY
    assertThat(response.getEntity()).isEqualTo(apiPageResult);
    assertThat(response.getLinks()).isEmpty();
  }

  @Test
  public void testPageNumberLessThanMinPageNumber() {
    // SETUP
    final ApiPageResult<String> apiPageResult = new ApiPageResult<>(10, 0, 3, Arrays.asList("result1", "result2"));

    // EXECUTE
    assertThrows(IllegalArgumentException.class,
        () -> new PaginationResponseBuilder<>(ABSOLUTE_PATH, 0, 3, apiPageResult).build());
  }

  @Test
  public void testPageSizeLessThanMinPageSize() {
    // SETUP
    final ApiPageResult<String> apiPageResult = new ApiPageResult<>(10, 1, 0, Arrays.asList("result1", "result2"));

    // EXECUTE
    assertThrows(IllegalArgumentException.class,
        () -> new PaginationResponseBuilder<>(ABSOLUTE_PATH, 1, 0, apiPageResult).build());
  }
}
