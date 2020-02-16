/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.Collections;
import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.pages.RepositoryReportContainerPage;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;

import com.codeborne.selenide.Condition;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class RepositoryReportContainerTest
    extends AbstractFunctionalTest
{
  private Repository repository;

  private RepositoryComponent repositoryComponent;

  private RepositoryComponentDAO repositoryComponentDAO = new RepositoryComponentDAO();

  @BeforeClass
  public static void beforeAll() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @Before
  public void before() {
    repository = tempEntity.newRepository(tempEntity.newRepositoryManager(), "central");

    repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(), DateUtils.addHours(new Date(), -84));

    setupHDSResponse();
  }

  @Test
  public void testReportContainer() throws Exception {
    refreshOrOpen(RepositoryReportContainerPage.url(repository.getId()));

    Date oldest = repositoryComponentDAO.getOldestComponentEvaluationTimeByRepositoryId(repository.getId());
    assertThat(oldest).isEqualTo(repositoryComponent.getLastEvaluationTime());
    RepositoryReportContainerPage.refreshButton().shouldBe(visible);
    RepositoryReportContainerPage.oldestEvalTime().shouldBe(visible)
        .shouldBe(Condition.exactTextCaseSensitive("Oldest evaluation 3 days ago"));

    RepositoryReportContainerPage.refreshButton().click();

    RepositoryReportContainerPage.ReEvaluateModal.root().shouldBe(visible);
    RepositoryReportContainerPage.ReEvaluateModal.cancelButton().click();

    RepositoryReportContainerPage.ReEvaluateModal.root().shouldBe(hidden);

    RepositoryReportContainerPage.refreshButton().click();

    RepositoryReportContainerPage.ReEvaluateModal.root().shouldBe(visible);
    RepositoryReportContainerPage.ReEvaluateModal.submitButton().click();

    RepositoryReportContainerPage.ReEvaluateModal.root().shouldBe(hidden);

    for (int i = 0; i < 100; i++) {
      Thread.sleep(100);
      Date newDate = repositoryComponentDAO.getOldestComponentEvaluationTimeByRepositoryId(repository.getId());
      if (newDate.after(oldest)) {
        return;
      }
    }
    fail("Repository component not updated for evaluation");
  }

  private void setupHDSResponse() {
    ComponentEvaluationData evalData = new ComponentEvaluationData();
    evalData.componentIdentifier = repositoryComponent.getComponentIdentifier();
    evalData.hash = repositoryComponent.getHash();
    evalData.matchState = MatchState.EXACT.getId();
    evalData.catalogDate = new Date().getTime();
    evalData.relativePopularity = 50;
    evalData.requestIndex = 0;
    evalData.declaredLicenses = Collections.emptySet();
    evalData.observedLicenses = Collections.emptySet();
    evalData.securityVulnerabilities = Collections.emptyList();

    ComponentEvaluationDataList dataList = new ComponentEvaluationDataList();
    dataList.components.add(evalData);

    testCLMServer.getHdsServer().respondWith(dataList).atUri("rest/component/details/firewall");
  }
}
