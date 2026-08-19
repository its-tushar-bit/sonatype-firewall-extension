/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;

public class ApiRepositoryComponentEvaluationRequestList
{
  public String format;

  public List<ApiRepositoryComponentEvaluationRequest> components = new ArrayList<>();

  public static class ApiRepositoryComponentEvaluationRequest
  {
    public String pathname;

    @JsonAlias({"sha1", "sonatypeFingerprint"})
    public String hash;

    @JsonAlias({"purl"})
    public String packageUrl;

    public ApiRepositoryComponentEvaluationRequest() {
    }

    public ApiRepositoryComponentEvaluationRequest(final String pathname, final String hash) {
      this.pathname = pathname;
      this.hash = hash;
    }

    public ApiRepositoryComponentEvaluationRequest(final String pathname, final String hash, final String packageUrl) {
      this.pathname = pathname;
      this.hash = hash;
      this.packageUrl = packageUrl;
    }
  }
}
