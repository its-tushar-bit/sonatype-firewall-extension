/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.api.v2.dto.ApiReportHistoryDTO;
import com.sonatype.insight.brain.api.v2.service.ApiReportServiceV2;
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
public class ApiHostedRepositoryComponentReportResourceV2Test
{
  private static final String HRC_ID = "hrc-1";

  @Mock
  private ApiReportServiceV2 apiReportServiceV2;

  @Mock
  private HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  @InjectMocks
  private ApiHostedRepositoryComponentReportResourceV2 resource;

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
  public void getReportHistory_delegatesToServiceWithResolvedHrc() {
    ApiReportHistoryDTO expected = new ApiReportHistoryDTO();
    when(apiReportServiceV2.getReportHistoryForOwner(hrc, "build", 10)).thenReturn(expected);

    ApiReportHistoryDTO actual = resource.getReportHistory(HRC_ID, "build", 10);

    assertThat(actual).isSameAs(expected);
    verify(hostedRepositoryComponentDAO).getByIdNotNull(HRC_ID);
    verify(apiReportServiceV2).getReportHistoryForOwner(hrc, "build", 10);
  }

  @Test
  public void getReportHistory_passesThroughNullStageAndLimit() {
    ApiReportHistoryDTO expected = new ApiReportHistoryDTO();
    when(apiReportServiceV2.getReportHistoryForOwner(hrc, null, null)).thenReturn(expected);

    ApiReportHistoryDTO actual = resource.getReportHistory(HRC_ID, null, null);

    assertThat(actual).isSameAs(expected);
    verify(apiReportServiceV2).getReportHistoryForOwner(hrc, null, null);
  }
}
