/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.api.v2.service.ApiSpdxService;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApiHostedRepositoryComponentSpdxResourceTest
{
  private static final String HRC_ID = "hrc-1";

  private static final String STAGE_ID = "build";

  private static final String SCAN_ID = "scan-1";

  private static final String FORMAT = "json";

  private static final String SPDX_VERSION = "2.3";

  @Mock
  private ApiSpdxService apiSpdxService;

  @Mock
  private HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  @InjectMocks
  private ApiHostedRepositoryComponentSpdxResource resource;

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
  public void getLatestForStage_delegatesWithResolvedHrcAndAllParams() {
    Response expected = mock(Response.class);
    when(apiSpdxService.getLatestForStage(hrc, STAGE_ID, FORMAT, false, SPDX_VERSION)).thenReturn(expected);

    Response actual = resource.getLatestForStage(HRC_ID, STAGE_ID, FORMAT, false, SPDX_VERSION);

    assertThat(actual).isSameAs(expected);
    verify(hostedRepositoryComponentDAO).getByIdNotNull(HRC_ID);
    verify(apiSpdxService).getLatestForStage(hrc, STAGE_ID, FORMAT, false, SPDX_VERSION);
  }

  @Test
  public void getLatestForStage_passesGenerateCycloneDxTrue() {
    Response expected = mock(Response.class);
    when(apiSpdxService.getLatestForStage(hrc, STAGE_ID, FORMAT, true, SPDX_VERSION)).thenReturn(expected);

    Response actual = resource.getLatestForStage(HRC_ID, STAGE_ID, FORMAT, true, SPDX_VERSION);

    assertThat(actual).isSameAs(expected);
    verify(apiSpdxService).getLatestForStage(hrc, STAGE_ID, FORMAT, true, SPDX_VERSION);
  }

  @Test
  public void getByScanId_delegatesWithResolvedHrcAndAllParams() {
    Response expected = mock(Response.class);
    when(apiSpdxService.getByScanId(hrc, SCAN_ID, FORMAT, false, SPDX_VERSION)).thenReturn(expected);

    Response actual = resource.getByScanId(HRC_ID, SCAN_ID, FORMAT, false, SPDX_VERSION);

    assertThat(actual).isSameAs(expected);
    verify(hostedRepositoryComponentDAO).getByIdNotNull(HRC_ID);
    verify(apiSpdxService).getByScanId(hrc, SCAN_ID, FORMAT, false, SPDX_VERSION);
  }

  @Test
  public void getByScanId_passesGenerateCycloneDxTrue() {
    Response expected = mock(Response.class);
    when(apiSpdxService.getByScanId(hrc, SCAN_ID, "xml", true, "2.2")).thenReturn(expected);

    Response actual = resource.getByScanId(HRC_ID, SCAN_ID, "xml", true, "2.2");

    assertThat(actual).isSameAs(expected);
    verify(apiSpdxService).getByScanId(hrc, SCAN_ID, "xml", true, "2.2");
  }
}
