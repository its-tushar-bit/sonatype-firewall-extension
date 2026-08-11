/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.node.ContainerNode;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.repository.hosted.HostedComponentScanQueueConsumer;
import com.sonatype.insight.brain.repository.hosted.HostedRepositoryComponentResolver;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The test in this class was written to be hardcoded to the expectations of the AbstractDataTest class, so it was not
 * simple to move it to ReportServiceTest which extends AbstractComponentTest. No new tests should be added here, and
 * future refactoring can rewrite this test. For now, it will remain separate because it doesn't impact the refactoring
 * that made it necessary to merge into the ReportService.
 */
public class ReportServiceDataTest
    extends AbstractDataTest
{
  private FileLifecycleReportPersistenceService lifecycleReportPersistenceService;

  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  private MultiLicenseDAO multiLicenseDAO;

  private LicenseThreatGroupDAO licenseThreatGroupDAO;

  private InsightWork insightWork;

  @Before
  public void setUp() throws Exception {
    multiLicenseDAO = daoFactory.createMultiLicenseDAO();
    licenseThreatGroupDAO = daoFactory.createLicenseThreatGroupDAO();
    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(tempDir.newFolder().getAbsolutePath());
    lifecycleReportPersistenceService = new FileLifecycleReportPersistenceService(insightConfig, new FileCleaner());
    insightWork = new InsightWork(insightConfig, null);
  }

  @Test
  public void testWriteLicenseThreatsToReportFile() throws Exception {
    Organization org = tempEntity.newOrganization("testWriteLicenseThreatsToReportFile");
    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newLicenseThreatGroup(app.getId(), "My group 1", 0, "Apache-2.0", "GPL-2.0");
    tempEntity.newLicenseThreatGroup(org.getId(), "My group 2", 5, "GPL-2.0");
    tempEntity.newLicenseThreatGroup(org.getParentOrganizationId(), "My group 3", 9, "GPL-3.0");
    ReportHelper.saveMockReport(insightWork, app.getId(), "scanId");

    LifecycleReport lifecycleReport = new LifecycleReport(lifecycleReportPersistenceService, app, "scanId");

    createReportService().writeLicenseThreatsToReportFile(app, lifecycleReport);

    ReportEntry licenseThreatsEntry = lifecycleReport.getEntry("licensethreats.json");

    ContainerNode<?> licenseThreats = JsonUtils.parse(licenseThreatsEntry.buf);
    int countNotZero = 0;
    @SuppressWarnings("unchecked")
    Map<String, Integer> threatLevelsByMultiLicenseId =
        JsonUtils.asPojo(licenseThreats.get("aaData"), Map.class);
    for (String multiLicenseShortName : threatLevelsByMultiLicenseId.keySet()) {
      Set<String> simpleLicenseIds = multiLicenseDAO.getLicensesByMultiLicenseIdNotNull(
          multiLicenseDAO.getByNameNotNull(multiLicenseShortName).getId())
          .stream()
          .map(License::getId)
          .collect(Collectors.toSet());
      if (simpleLicenseIds.contains("GPL-3.0")) {
        assertThat(threatLevelsByMultiLicenseId.get(multiLicenseShortName)).isEqualTo(9);
        countNotZero++;
      }
      else if (simpleLicenseIds.contains("GPL-2.0")) {
        assertThat(threatLevelsByMultiLicenseId.get(multiLicenseShortName)).isEqualTo(5);
        countNotZero++;
      }
      else if (simpleLicenseIds.contains("Apache-2.0")) {
        assertThat(threatLevelsByMultiLicenseId.get(multiLicenseShortName)).isEqualTo(0);
        countNotZero++;
      }
      else {
        assertThat(threatLevelsByMultiLicenseId.get(multiLicenseShortName)).as("Threat for multi license %s",
            multiLicenseShortName).isNull();
      }
    }
    assertThat(threatLevelsByMultiLicenseId).hasSize(multiLicenseDAO.getAll().size());
    assertThat(countNotZero).isPositive();
  }

  private ReportService createReportService() {

    return new ReportService(null, null, null, null, null, null, null, null, null, null, null,
        daoFactory.createLicenseDAO(), null, null, licenseThreatGroupDAO, null, null, null, multiLicenseDAO, null, null,
        null, null, null, null, null, null, null, null, null, null, null, null, null, null,
        Mockito.mock(HostedComponentScanQueueConsumer.class),
        Mockito.mock(ClusterLockManager.class),
        Mockito.mock(HostedRepositoryComponentDAO.class),
        Mockito.mock(HostedRepositoryComponentResolver.class),
        // A lambda rather than a Mockito double: the constructor stores the Provider itself, so a
        // stub-free mock and a lambda behave identically here, and the lambda cannot be mistaken
        // for a mock candidate by type-matching injection machinery.
        () -> Mockito.mock(ScanPolicyEvaluator.class));
  }
}
