/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;
import java.util.Date;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiPullRequestResult;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiPullRequestResults;
import com.sonatype.insight.brain.git.EnhancedPullRequestResult;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.nexus.iq.manager.PullRequestResult;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiSourceControlMetricsAdapterTest
    extends AbstractComponentTest
{
  @Inject
  private ApiSourceControlMetricsAdapter adapter;

  @Test
  public void testConvertToDTO_null() {
    ApiPullRequestResults results = adapter.convertToDTO(null);
    assertThat(results.results).isEmpty();
  }

  @Test
  public void testConvertToDTO_empty() {
    ApiPullRequestResults results = adapter.convertToDTO(Collections.emptyList());
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

    ApiPullRequestResults results = adapter.convertToDTO(Collections.singletonList(enhancedSuccess));
    assertThat(results.results).hasSize(1);
    ApiPullRequestResult adapted = results.results.get(0);
    assertThat(adapted).extracting("startTime").isEqualTo(enhancedSuccess.getStartTime());
    assertThat(adapted).extracting("title").isEqualTo(enhancedSuccess.getTitle());
    assertThat(adapted).extracting("reasoning").isEqualTo(enhancedSuccess.getReasoning());
    assertThat(adapted).extracting("exceptionThrown").isEqualTo(enhancedSuccess.isExceptionThrown());
    assertThat(adapted).extracting("successful").isEqualTo(enhancedSuccess.getTiming().isSuccessful());
    assertThat(adapted).extracting("totalTime").isEqualTo(enhancedSuccess.getTiming().getTotalTime());
  }
}
