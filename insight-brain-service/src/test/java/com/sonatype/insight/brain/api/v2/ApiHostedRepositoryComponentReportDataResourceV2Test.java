/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.api.v2.dto.ApiDependencyTreeResponseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiDependencyTreeNodeDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportPolicyDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.security.SecurityAspectControl;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApiHostedRepositoryComponentReportDataResourceV2Test
{
  private static final String HRC_ID = "hrc-uuid-123";

  private static final String SCAN_ID = "scan-456";

  @Mock
  private ApiReportDataServiceV2 reportDataService;

  @Mock
  private HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  @InjectMocks
  private ApiHostedRepositoryComponentReportDataResourceV2 resource;

  private HostedRepositoryComponent hrc;

  @Before
  public void setUp() {
    // SystemConfigurationPropertyFeature uses static injection; wire a Mockito-friendly DAO stub
    // so setEnabled(...) below can create a transaction context. Mirrors the pattern used in
    // HostedRepositoryComponentReportResourceTest.
    SystemConfigurationPropertyDAO sysDao = Mockito.mock(SystemConfigurationPropertyDAO.class);
    TransactionContext tx = Mockito.mock(TransactionContext.class);
    Mockito.lenient().when(sysDao.createTransactionContext()).thenReturn(tx);
    SystemConfigurationPropertyFeature.injectDependencies(sysDao);

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
    SystemConfigurationPropertyFeature.injectDependencies(null);
  }

  @Test
  public void getRawData_delegatesToReportDataServiceWithResolvedHrc() throws Exception {
    ApiReportRawDataDTOV2 expected = new ApiReportRawDataDTOV2();
    when(reportDataService.getRawData(hrc, SCAN_ID, false)).thenReturn(expected);

    ApiReportRawDataDTOV2 actual = resource.getRawData(HRC_ID, SCAN_ID, false);

    assertThat(actual).isSameAs(expected);
    verify(hostedRepositoryComponentDAO).getByIdNotNull(HRC_ID);
    verify(reportDataService).getRawData(hrc, SCAN_ID, false);
  }

  @Test
  public void getRawData_withCustomSecurityData_delegatesWithCorrectFlag() throws Exception {
    ApiReportRawDataDTOV2 expected = new ApiReportRawDataDTOV2();
    when(reportDataService.getRawData(hrc, SCAN_ID, true)).thenReturn(expected);

    ApiReportRawDataDTOV2 actual = resource.getRawData(HRC_ID, SCAN_ID, true);

    assertThat(actual).isSameAs(expected);
    verify(hostedRepositoryComponentDAO).getByIdNotNull(HRC_ID);
    verify(reportDataService).getRawData(hrc, SCAN_ID, true);
  }

  @Test
  public void getPolicyViolations_delegatesToReportDataServiceWithResolvedHrc() throws Exception {
    ApiReportPolicyDataDTOV2 expected = new ApiReportPolicyDataDTOV2();
    when(reportDataService.getPolicyViolationsData(hrc, SCAN_ID, false, null, null)).thenReturn(expected);

    ApiReportPolicyDataDTOV2 actual = resource.getPolicyViolations(HRC_ID, SCAN_ID, false, null, null);

    assertThat(actual).isSameAs(expected);
    verify(hostedRepositoryComponentDAO).getByIdNotNull(HRC_ID);
    verify(reportDataService).getPolicyViolationsData(hrc, SCAN_ID, false, null, null);
  }

  @Test
  public void getPolicyViolations_withViolationTimes_delegatesWithCorrectFlag() throws Exception {
    ApiReportPolicyDataDTOV2 expected = new ApiReportPolicyDataDTOV2();
    when(reportDataService.getPolicyViolationsData(hrc, SCAN_ID, true, null, null)).thenReturn(expected);

    ApiReportPolicyDataDTOV2 actual = resource.getPolicyViolations(HRC_ID, SCAN_ID, true, null, null);

    assertThat(actual).isSameAs(expected);
    verify(hostedRepositoryComponentDAO).getByIdNotNull(HRC_ID);
    verify(reportDataService).getPolicyViolationsData(hrc, SCAN_ID, true, null, null);
  }

  @Test
  public void getPolicyViolations_withPagination_delegatesWithCorrectParams() throws Exception {
    ApiReportPolicyDataDTOV2 expected = new ApiReportPolicyDataDTOV2();
    when(reportDataService.getPolicyViolationsData(hrc, SCAN_ID, false, 1, 10)).thenReturn(expected);

    ApiReportPolicyDataDTOV2 actual = resource.getPolicyViolations(HRC_ID, SCAN_ID, false, 1, 10);

    assertThat(actual).isSameAs(expected);
    verify(hostedRepositoryComponentDAO).getByIdNotNull(HRC_ID);
    verify(reportDataService).getPolicyViolationsData(hrc, SCAN_ID, false, 1, 10);
  }

  @Test
  public void getDependencyTree_delegatesToReportDataServiceWithResolvedHrc() throws Exception {
    ApiDependencyTreeResponseDTO expected = new ApiDependencyTreeResponseDTO(new ApiDependencyTreeNodeDTO());
    when(reportDataService.getDependencyTree(hrc, SCAN_ID)).thenReturn(expected);

    ApiDependencyTreeResponseDTO actual = resource.getDependencyTree(HRC_ID, SCAN_ID);

    assertThat(actual).isSameAs(expected);
    verify(hostedRepositoryComponentDAO).getByIdNotNull(HRC_ID);
    verify(reportDataService).getDependencyTree(hrc, SCAN_ID);
  }
}
