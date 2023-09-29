/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.ArrayList;
import java.util.List;

public class ApiRepositoryComponentEvaluationRequestList
{
  public String format;

  public List<ApiRepositoryComponentEvaluationRequest> components = new ArrayList<>();

  public static class ApiRepositoryComponentEvaluationRequest
  {
    public String pathname;

    public String hash;

    public ApiRepositoryComponentEvaluationRequest() { }

    public ApiRepositoryComponentEvaluationRequest(final String pathname, final String hash) {
      this.pathname = pathname;
      this.hash = hash;
    }
  }
}
