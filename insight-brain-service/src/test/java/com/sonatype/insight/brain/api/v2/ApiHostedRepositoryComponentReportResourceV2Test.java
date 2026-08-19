/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.api.v2.dto.ApiReportHistoryDTO;
import com.sonatype.insight.brain.api.v2.service.ApiReportServiceV2;
import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.security.SecurityAspectControl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ApiHostedRepositoryComponentReportResourceV2}. The resource is the
 * HRC-scoped sibling of {@link ApiReportResourceV2}; its lone read handler resolves the HRC via
 * {@link HostedRepositoryComponentDAO#getByIdNotNull(String)} and delegates to
 * {@link ApiReportServiceV2#getReportHistoryForOwner(com.sonatype.insight.brain.model.Owner,
 * String, Integer)}.
 */
@ExtendWith(MockitoExtension.class)
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

  @BeforeEach
  public void setUp() {
    // AspectJ compile-time weaving inserts a @HasFeature aspect on the resource class and an
    // @Authorize aspect on its service-call sites. Both fire during Mockito unit tests that
    // bypass the Spring proxy. Disabling enforcement short-circuits both to the mocked service
    // call — see SecurityAspectControl's javadoc for the intended use. This also covers
    // @HasFeature(HOSTED_REPOSITORY_EVALUATION), so the feature must not be toggled here:
    // SystemConfigurationPropertyFeature.setEnabled reaches for a statically injected
    // SystemConfigurationPropertyDAO that a plain MockitoJUnitRunner never wires.
    SecurityAspectControl.disableEnforcement();
    hrc = new HostedRepositoryComponent("repo-1", "path/lib.jar", "hash-abc");
    hrc.setId(HRC_ID);
    when(hostedRepositoryComponentDAO.getByIdNotNull(HRC_ID)).thenReturn(hrc);
  }

  @AfterEach
  public void tearDown() {
    SecurityAspectControl.enableEnforcement();
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
