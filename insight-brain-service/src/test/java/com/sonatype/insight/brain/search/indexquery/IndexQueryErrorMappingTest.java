/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.insight.brain.search.global.FilterValidationException;
import com.sonatype.insight.brain.search.global.FilterValidationExceptionMapper;
import com.sonatype.insight.brain.search.global.StaleCursorException;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import jakarta.ws.rs.core.Response;

import org.junit.Test;

/**
 * Drives the two search exceptions through the real JAX-RS {@link ErrorResponseGenerator} to assert
 * the mapped HTTP status, not merely the thrown type. Locks the 400/410 contract the endpoint promises.
 */
public class IndexQueryErrorMappingTest
{
  private final ErrorResponseGenerator generator = new ErrorResponseGenerator();

  @Test
  public void filterValidationException_mapsTo400() {
    int status = generator.mapExceptionAndLog(
        new FilterValidationException(FilterValidationException.Code.INVALID_FILTER, "unknown filter key"))
        .getStatusCode();
    assertThat(status).isEqualTo(400);
  }

  @Test
  public void filterValidationException_sortNotAllowed_mapsTo400() {
    int status = generator.mapExceptionAndLog(
        new FilterValidationException(FilterValidationException.Code.SORT_NOT_ALLOWED, "sort not allowed"))
        .getStatusCode();
    assertThat(status).isEqualTo(400);
  }

  @Test
  public void compileWaiverType_singleElementArray_isUnwrapped() {
    // A consumer may pass ["AUTO"] by analogy with the array-valued filters; unwrap it silently.
    IndexQueryFilterCompiler.CompiledQuery compiled = IndexQueryFilterCompiler.compileWithClauses(
        IndexQueryType.VIOLATION, Map.of("waiverType", List.of("AUTO")));
    assertThat(compiled.fieldClauses())
        .containsExactly("policyViolationWaiverStatus:\"" + IndexQueryWaiverStatus.AUTO_WAIVED + "\"");
  }

  @Test
  public void compileWaiverType_multiElementArray_givesClearMessage() {
    // The client body stays generic, but the server-log detail must name the real shape problem.
    assertThatThrownBy(() -> IndexQueryFilterCompiler.compileWithClauses(
        IndexQueryType.VIOLATION, Map.of("waiverType", List.of("AUTO", "MANUAL"))))
            .isInstanceOf(FilterValidationException.class)
            .satisfies(e -> {
              FilterValidationException fve = (FilterValidationException) e;
              assertThat(fve.getCode()).isEqualTo(FilterValidationException.Code.INVALID_FILTER);
              assertThat(fve.getDetail())
                  .isEqualTo("filter 'waiverType' takes a single value, not an array of 2");
            });
  }

  @Test
  public void staleCursorException_mapsTo410() {
    int status = generator.mapExceptionAndLog(new StaleCursorException("cursor is stale")).getStatusCode();
    assertThat(status).isEqualTo(410);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void mapper_emitsCodeAndStaticMessage_neverEchoesRawInput() throws Exception {
    // The exception message deliberately carries attacker-controllable input; it must not reach the body.
    FilterValidationException e = new FilterValidationException(
        FilterValidationException.Code.SORT_NOT_ALLOWED, "sort 'evilInjectedValue' is not allowed");

    Response response = new FilterValidationExceptionMapper().toResponse(e);
    assertThat(response.getStatus()).isEqualTo(400);

    String json = new ObjectMapper().writeValueAsString(response.getEntity());
    Map<String, Object> body = new ObjectMapper().readValue(json, Map.class);
    assertThat(body).containsEntry("code", "SORT_NOT_ALLOWED");
    assertThat(body).containsEntry("message", FilterValidationException.Code.SORT_NOT_ALLOWED.clientMessage());
    assertThat(json).doesNotContain("evilInjectedValue");
  }
}
