/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.reachability;

import java.io.IOException;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.security.SecurityAspectControl;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ApiHostedRepositoryComponentReachabilityEvidenceResourceTest
{
  private static final String HRC_ID = "hrc-1";

  private static final String REPORT_ID = "scan-1";

  private static final String VULN_ID = "CVE-2023-35116";

  @Mock
  private ApiReachabilityEvidenceService evidenceService;

  @Mock
  private HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  @InjectMocks
  private ApiHostedRepositoryComponentReachabilityEvidenceResource resource;

  private HostedRepositoryComponent hrc;

  @BeforeEach
  public void setUp() {
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

  @AfterEach
  public void tearDown() {
    SecurityAspectControl.enableEnforcement();
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);
    SystemConfigurationPropertyFeature.injectDependencies(null);
  }

  @Test
  public void getReachabilityEvidence_delegatesToServiceWithResolvedHrc() throws IOException {
    ApiReachabilityEvidenceResponse expected = new ApiReachabilityEvidenceResponse(VULN_ID, List.of(), false);
    when(evidenceService.getEvidenceForVulnerability(hrc, REPORT_ID, VULN_ID)).thenReturn(expected);

    ApiReachabilityEvidenceResponse actual = resource.getReachabilityEvidence(HRC_ID, REPORT_ID, VULN_ID);

    assertThat(actual).isSameAs(expected);
    verify(hostedRepositoryComponentDAO).getByIdNotNull(HRC_ID);
    verify(evidenceService).getEvidenceForVulnerability(hrc, REPORT_ID, VULN_ID);
  }

  @Test
  public void getReachabilityEvidence_nullResponse_throwsNotFound() throws IOException {
    when(evidenceService.getEvidenceForVulnerability(hrc, REPORT_ID, VULN_ID)).thenReturn(null);

    assertThatThrownBy(() -> resource.getReachabilityEvidence(HRC_ID, REPORT_ID, VULN_ID))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(VULN_ID)
        .hasMessageContaining(REPORT_ID)
        .hasMessageContaining(HRC_ID);
  }
}
