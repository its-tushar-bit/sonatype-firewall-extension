/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;

import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.report.ApplicationReport;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.search.index.IndexingContext;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.inject.Binder;
import jakarta.inject.Inject;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DocumentBuilderHelperTest
    extends AbstractComponentTest
{
  @Inject
  private DocumentBuilderHelper documentBuilderHelper;

  @Mock
  private PolicyEvaluationDAO policyEvaluationDAOMock;

  @Mock
  private ReportService reportServiceMock;

  @Mock
  private IndexingContext indexingContextMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(PolicyEvaluationDAO.class).toInstance(policyEvaluationDAOMock);
    binder.bind(ReportService.class).toInstance(reportServiceMock);
    super.configure(binder);
  }

  @Test
  public void testBuildApplicationStageSVDocs_IoExceptionIsSwallowed() throws IOException {
    Application app = tempEntity.newApplicationWithParent();
    Organization org = tempEntity.newOrganization();
    PolicyEvaluation eval = new PolicyEvaluation();
    eval.setScanId("scan-id");
    eval.setApplicationId(app.getId());
    when(policyEvaluationDAOMock.getLastByApplicationIdAndStageId(app.getId(), StageTypes.BUILD.getId())).thenReturn(
        eval);
    ApplicationReport mockApplicationReport = mock(ApplicationReport.class);
    doThrow(new IOException("IO error")).when(mockApplicationReport).exists();
    when(reportServiceMock.getReport(anyString(), anyString())).thenReturn(mockApplicationReport);

    assertThat(documentBuilderHelper.buildApplicationStageSVDocs(
        indexingContextMock, org, app, StageTypes.BUILD, Collections.emptyList()
    )).isEmpty();
  }

  @Test
  public void testBuildApplicationStageSVDocs_NotFoundExceptionIsSwallowed() {
    Application app = tempEntity.newApplicationWithParent();
    Organization org = tempEntity.newOrganization();
    PolicyEvaluation eval = new PolicyEvaluation();
    eval.setScanId("scan-id");
    when(policyEvaluationDAOMock.getLastByApplicationIdAndStageId(app.getId(), StageTypes.BUILD.getId()))
        .thenReturn(eval);
    when(reportServiceMock.getReport(anyString(), anyString()))
        .thenThrow(new NotFoundException("Not found"));

    assertThat(documentBuilderHelper.buildApplicationStageSVDocs(
        indexingContextMock, org, app, StageTypes.BUILD, Collections.emptyList()
    )).isEmpty();
  }

  @Test
  public void testBuildApplicationStageSVDocs_UncheckedIoExceptionIsSwallowed() {
    Application app = tempEntity.newApplicationWithParent();
    Organization org = tempEntity.newOrganization();
    PolicyEvaluation eval = new PolicyEvaluation();
    eval.setScanId("scan-id");
    when(policyEvaluationDAOMock.getLastByApplicationIdAndStageId(app.getId(), StageTypes.BUILD.getId()))
        .thenReturn(eval);
    when(reportServiceMock.getReport(anyString(), anyString()))
        .thenThrow(new UncheckedIOException(new IOException("IO error")));

    assertThat(documentBuilderHelper.buildApplicationStageSVDocs(
        indexingContextMock, org, app, StageTypes.BUILD, Collections.emptyList()
    )).isEmpty();
  }

  @Test
  public void testBuildApplicationStageSVDocs_WrappedIoExceptionIsSwallowed() {
    Application app = tempEntity.newApplicationWithParent();
    Organization org = tempEntity.newOrganization();
    PolicyEvaluation eval = new PolicyEvaluation();
    eval.setScanId("scan-id");
    when(policyEvaluationDAOMock.getLastByApplicationIdAndStageId(app.getId(), StageTypes.BUILD.getId()))
        .thenReturn(eval);
    when(reportServiceMock.getReport(anyString(), anyString()))
        .thenThrow(new RuntimeException("Wrapped", new IOException("IO error")));

    assertThat(documentBuilderHelper.buildApplicationStageSVDocs(
        indexingContextMock, org, app, StageTypes.BUILD, Collections.emptyList()
    )).isEmpty();
  }

  @Test
  public void testBuildApplicationStageSVDocs_WrappedNotFoundExceptionIsSwallowed() {
    Application app = tempEntity.newApplicationWithParent();
    Organization org = tempEntity.newOrganization();
    PolicyEvaluation eval = new PolicyEvaluation();
    eval.setScanId("scan-id");
    when(policyEvaluationDAOMock.getLastByApplicationIdAndStageId(app.getId(), StageTypes.BUILD.getId()))
        .thenReturn(eval);
    when(reportServiceMock.getReport(anyString(), anyString()))
        .thenThrow(new RuntimeException("Wrapped", new NotFoundException("Not found")));

    assertThat(documentBuilderHelper.buildApplicationStageSVDocs(
        indexingContextMock, org, app, StageTypes.BUILD, Collections.emptyList()
    )).isEmpty();
  }

  @Test
  public void testBuildApplicationStageSVDocs_NonIoExceptionIsRethrown() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Organization org = tempEntity.newOrganization();
    PolicyEvaluation eval = new PolicyEvaluation();
    eval.setScanId("scan-id");
    when(policyEvaluationDAOMock.getLastByApplicationIdAndStageId(app.getId(), StageTypes.BUILD.getId()))
        .thenReturn(eval);
    when(reportServiceMock.getReport(anyString(), anyString()))
        .thenThrow(new IllegalStateException("Unexpected error"));

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> documentBuilderHelper.buildApplicationStageSVDocs(
            indexingContextMock, org, app, StageTypes.BUILD, Collections.emptyList()
        ))
        .withMessage("Unexpected error");
  }
}
