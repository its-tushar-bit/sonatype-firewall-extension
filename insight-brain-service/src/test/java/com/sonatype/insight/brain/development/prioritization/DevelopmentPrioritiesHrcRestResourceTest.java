/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.development.prioritization;

import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.security.SecurityAspectControl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DevelopmentPrioritiesHrcRestResourceTest
{
  private static final String HRC_ID = "hrc-1";

  private static final String SCAN_ID = "scan-abc";

  @Mock
  private DevelopmentPrioritiesService developmentPrioritiesService;

  @Mock
  private HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  @InjectMocks
  private DevelopmentPrioritiesHrcRestResource resource;

  private HostedRepositoryComponent hrc;

  @Before
  public void setUp() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
    SecurityAspectControl.disableEnforcement();
    hrc = new HostedRepositoryComponent("repo-1", "path/lib.jar", "hash-abc");
    hrc.setId(HRC_ID);
    when(hostedRepositoryComponentDAO.getByIdNotNull(HRC_ID)).thenReturn(hrc);
  }

  @After
  public void tearDown() {
    SecurityAspectControl.enableEnforcement();
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);
  }

  @Test
  public void getPriorities_resolvesHrcAndDelegatesToService() {
    DevelopmentPrioritizationResults expected =
        new DevelopmentPrioritizationResults("", false, new ApiPageResult<>(0L, 1, 10, List.of()));
    when(developmentPrioritiesService.getPrioritizedFindings(hrc, SCAN_ID, 1, 10, null, false, true))
        .thenReturn(expected);

    DevelopmentPrioritizationResults actual = resource.getPriorities(HRC_ID, SCAN_ID, 1, 10, null, true);

    assertThat(actual).isSameAs(expected);
    verify(hostedRepositoryComponentDAO).getByIdNotNull(HRC_ID);
    verify(developmentPrioritiesService).getPrioritizedFindings(hrc, SCAN_ID, 1, 10, null, false, true);
  }

  @Test
  public void getPriorities_passesThroughFilterAndPagination() {
    DevelopmentPrioritizationResults expected =
        new DevelopmentPrioritizationResults("", false, new ApiPageResult<>(0L, 2, 25, List.of()));
    when(developmentPrioritiesService.getPrioritizedFindings(hrc, SCAN_ID, 2, 25, "junit", false, false))
        .thenReturn(expected);

    DevelopmentPrioritizationResults actual = resource.getPriorities(HRC_ID, SCAN_ID, 2, 25, "junit", false);

    assertThat(actual).isSameAs(expected);
    verify(developmentPrioritiesService).getPrioritizedFindings(hrc, SCAN_ID, 2, 25, "junit", false, false);
  }
}
