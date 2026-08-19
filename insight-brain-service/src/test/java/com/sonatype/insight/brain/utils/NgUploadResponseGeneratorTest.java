/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.util.concurrent.Callable;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.policy.PolicyImportResult;
import com.sonatype.insight.brain.security.AntiCsrfFilter;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.jaxrs.error.ErrorResponseGenerator;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @since 1.19.0
 */
public class NgUploadResponseGeneratorTest
{
  private final AntiCsrfFilter antiCsrfFilter = mock(AntiCsrfFilter.class);

  private final ErrorResponseGenerator errorResponseGenerator = new ErrorResponseGenerator();

  private final String csrfToken = "csrfToken";

  private final HttpHeaders httpHeaders = mock(HttpHeaders.class);

  private NgUploadResponseGenerator ngUploadResponseGenerator;

  @BeforeEach
  public void setup() {
    ngUploadResponseGenerator = new NgUploadResponseGenerator(errorResponseGenerator, antiCsrfFilter);
  }

  @Test
  public void run_CallsAntiCsrfFilter() throws Exception {
    final Callable<Void> callable = () -> null;
    ngUploadResponseGenerator.run(csrfToken, httpHeaders, false, callable);
    verify(antiCsrfFilter).validate(csrfToken, httpHeaders);

    ngUploadResponseGenerator.run(csrfToken, httpHeaders, true, callable);
    verify(antiCsrfFilter, times(2)).validate(csrfToken, httpHeaders);
  }

  @Test
  public void run_RespondAjaxRequest() throws Exception {
    final String stringResult = "foo";
    final PolicyImportResult pojoResult = new PolicyImportResult();
    pojoResult.ownerName = "foo";

    Callable<Object> callable = new NgUploadResponseResult(stringResult);
    Response response = ngUploadResponseGenerator.run(csrfToken, httpHeaders, false, callable);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat((String) response.getEntity()).isEqualTo(stringResult);

    callable = new NgUploadResponseResult(pojoResult);
    response = ngUploadResponseGenerator.run(csrfToken, httpHeaders, false, callable);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getMetadata().getFirst("Content-Type").toString()).isEqualTo(MediaType.APPLICATION_JSON);
    assertThat((PolicyImportResult) response.getEntity()).isEqualTo(pojoResult);

    callable = new NgUploadResponseResult(null);
    response = ngUploadResponseGenerator.run(csrfToken, httpHeaders, false, callable);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getMetadata().getFirst("Content-Type")).isNull();
    assertThat(response.getEntity()).isNull();
  }

  @Test
  public void run_FailAjaxRequest() {
    final String exceptionMessage = "foo";

    Callable<Object> callable = new NgUploadResponseResult(new BadRequestException(exceptionMessage));
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> ngUploadResponseGenerator.run(csrfToken, httpHeaders, false, callable))
        .withMessage(exceptionMessage);
  }

  @Test
  public void run_RespondIFrameRequest() throws Exception {
    final String stringResult = "foo";
    final PolicyImportResult pojoResult = new PolicyImportResult();
    pojoResult.ownerName = "foo";

    Callable<Object> callable = new NgUploadResponseResult(stringResult);
    Response response = ngUploadResponseGenerator.run(csrfToken, httpHeaders, true, callable);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getMetadata().getFirst("Content-Type").toString()).isEqualTo(MediaType.TEXT_PLAIN);
    assertThat((String) response.getEntity()).isEqualTo(JsonUtils.format(stringResult));

    callable = new NgUploadResponseResult(pojoResult);
    response = ngUploadResponseGenerator.run(csrfToken, httpHeaders, true, callable);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getMetadata().getFirst("Content-Type").toString()).isEqualTo(MediaType.TEXT_PLAIN);
    assertThat((String) response.getEntity()).isEqualTo(JsonUtils.format(pojoResult));
  }

  @Test
  public void run_FailIFrameRequest() throws Exception {
    final String exceptionMessage = "foo";

    Callable<Object> callable = new NgUploadResponseResult(new BadRequestException(exceptionMessage));
    Response response = ngUploadResponseGenerator.run(csrfToken, httpHeaders, true, callable);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getMetadata().getFirst("Content-Type").toString()).isEqualTo(MediaType.TEXT_PLAIN);
    assertThat((String) response.getEntity()).isEqualTo(JsonUtils.format(exceptionMessage));
  }

  private static class NgUploadResponseResult
      implements Callable<Object>
  {
    private final Object result;

    public NgUploadResponseResult(Object result) {
      this.result = result;
    }

    @Override
    public Object call() throws Exception {
      if (result instanceof Exception) {
        throw (Exception) result;
      }
      return result;
    }
  }
}
