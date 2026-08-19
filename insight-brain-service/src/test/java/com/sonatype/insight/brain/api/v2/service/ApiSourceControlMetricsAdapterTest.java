/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;
import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiPullRequestResult;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiPullRequestResults;
import com.sonatype.insight.brain.git.EnhancedPullRequestResult;
import com.sonatype.nexus.iq.manager.PullRequestResult;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiSourceControlMetricsAdapterTest
{
  @Test
  public void testConvertToDTO_null() {
    ApiPullRequestResults results = ApiSourceControlMetricsAdapter.convertToDTO(null);
    assertThat(results.results).isEmpty();
  }

  @Test
  public void testConvertToDTO_empty() {
    ApiPullRequestResults results = ApiSourceControlMetricsAdapter.convertToDTO(Collections.emptyList());
    assertThat(results.results).isEmpty();
  }

  @Test
  public void testConvertToDTO() {
    PullRequestResult success = new PullRequestResult();
    success.setCheckoutTime(1L);
    success.setRemediationTime(1L);
    success.setPushTime(1L);
    success.setPullRequestCreationTime(1L);
    success.setSuccessful(true);
    EnhancedPullRequestResult enhancedSuccess = new EnhancedPullRequestResult(success, new Date(),
        ComponentIdentifier.createMavenCoordinates("foo", "bar", "1.0"),
        "Bump bar to 1.1", false);

    ApiPullRequestResults results =
        ApiSourceControlMetricsAdapter.convertToDTO(Collections.singletonList(enhancedSuccess));
    assertThat(results.results).hasSize(1);
    ApiPullRequestResult adapted = results.results.get(0);
    assertThat(adapted.startTime).isEqualTo(enhancedSuccess.getStartTime());
    assertThat(adapted.title).isEqualTo(enhancedSuccess.getTitle());
    assertThat(adapted.reasoning).isEqualTo(enhancedSuccess.getReasoning());
    assertThat(adapted.exceptionThrown).isEqualTo(enhancedSuccess.isExceptionThrown());
    assertThat(adapted.successful).isEqualTo(enhancedSuccess.getTiming().isSuccessful());
    assertThat(adapted.totalTime).isEqualTo(enhancedSuccess.getTiming().getTotalTime());
  }
}
