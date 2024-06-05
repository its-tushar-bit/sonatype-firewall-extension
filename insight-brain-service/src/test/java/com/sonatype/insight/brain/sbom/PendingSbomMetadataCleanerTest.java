/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Collections;
import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;

import static com.sonatype.insight.brain.sbom.SbomTestHelper.mockOriginalSbom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class PendingSbomMetadataCleanerTest
    extends AbstractComponentTest
{
  @Inject
  private PendingSbomMetadataCleaner pendingSbomMetadataCleaner;

  @Mock
  private ThirdPartySbomMetadataDAO  mockThirdPartySbomMetadataDAO;

  @Mock
  private ThirdPartyFileDAO mockThirdPartyFileDao;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Inject
  private InsightWork insightWork;

  @Override
  public void configure(Binder binder) {
    binder.bind(TaskScheduler.class).toInstance(mockTaskScheduler);
    binder.bind(ThirdPartySbomMetadataDAO.class).toInstance(mockThirdPartySbomMetadataDAO);
    binder.bind(ThirdPartyFileDAO.class).toInstance(mockThirdPartyFileDao);
    super.configure(binder);
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(
        JobBuilder.newJob(PendingSbomMetadataCleaner.class).build().isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testStart_Disabled() {
    pendingSbomMetadataCleaner.disableForTesting = true;

    pendingSbomMetadataCleaner.register();

    verifyNoInteractions(mockTaskScheduler);
  }

  @Test
  public void testStart() {
    pendingSbomMetadataCleaner.register();

    verify(mockTaskScheduler).scheduleDailyTask(eq(pendingSbomMetadataCleaner),
        any(LocalTime.class));
  }

  @Test
  public void testExecute_AdminTask() {
    pendingSbomMetadataCleaner.execute(null, new PrintWriter(new StringWriter()));
    verify(mockThirdPartySbomMetadataDAO).getPendingSbomsOlderThanDuration(Duration.ofHours(24));
  }

  @Test
  public void testExecute_QuartzJob() throws Exception {
    Path zippedBom = mockOriginalSbom(this.getClass(), "third-party-simple-bom.xml",
        insightWork.getSbomDir("appId").toPath());

    ThirdPartySbomMetadata sbomMetadata = new ThirdPartySbomMetadata();
    sbomMetadata.setFilename(zippedBom.getFileName().toString());
    sbomMetadata.setThirdPartyFileId("thirdPartyFileId");
    sbomMetadata.setApplicationId("appId");
    when(mockThirdPartySbomMetadataDAO.getPendingSbomsOlderThanDuration(Duration.ofHours(24)))
        .thenReturn(Collections.singletonList(sbomMetadata));

    TransactionContext mockTransactionContext = mock(TransactionContext.class);
    when(mockThirdPartyFileDao.createTransactionContext()).thenReturn(mockTransactionContext);

    PendingSbomMetadataCleaner pendingSbomMetadataClean =
        spy(pendingSbomMetadataCleaner);
    JobExecutionContext mockJobExecutionContext = mock(JobExecutionContext.class);
    pendingSbomMetadataClean.execute(mockJobExecutionContext);

    verify(mockThirdPartySbomMetadataDAO).getPendingSbomsOlderThanDuration(Duration.ofHours(24));
    verify(mockThirdPartyFileDao).delete(mockTransactionContext, "thirdPartyFileId");
    assertThat(zippedBom).doesNotExist();
  }
}
