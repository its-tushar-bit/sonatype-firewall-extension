/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.PENDING;
import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.UPLOADED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus;
import com.sonatype.insight.brain.sbom.datastore.SbomEntity;
import com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportSpecification;
import com.sonatype.insight.brain.sbom.utils.SbomDetectionResult;
import com.sonatype.insight.brain.sbom.utils.SbomSummary;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.thirdparty.ThirdPartyPersistenceService;
import com.sonatype.insight.brain.utils.ExistingFilesHelper;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = PendingSbomMetadataCleanerTest.ExistingFilesHelperTestConfig.class)
public class PendingSbomMetadataCleanerTest
    extends AbstractComponentTest
{
  @TestConfiguration
  static class ExistingFilesHelperTestConfig
  {
    @Bean
    ExistingFilesHelper existingFilesHelper() {
      return new ExistingFilesHelper();
    }
  }

  @Inject
  private PendingSbomMetadataCleaner pendingSbomMetadataCleaner;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Inject
  private InsightWork insightWork;

  @Inject
  private ThirdPartyPersistenceService thirdPartyPersistenceService;

  @Inject
  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  @Inject
  private ExistingFilesHelper existingFilesHelper;

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(PendingSbomMetadataCleaner.class).build().isConcurrentExectionDisallowed()).isTrue();
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

    verify(mockTaskScheduler).scheduleDailyTask(eq(pendingSbomMetadataCleaner), any(LocalTime.class));
  }

  @Test
  public void testExecute_AdminTask() throws Exception {
    testExecute(pendingSbomMetadataCleaner -> {
      try {
        pendingSbomMetadataCleaner.execute((Map<String, List<String>>) null,
            new PrintWriter(OutputStream.nullOutputStream()));
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testExecute_QuartzJob() throws Exception {
    JobExecutionContext mockJobExecutionContext = mock(JobExecutionContext.class);
    testExecute(pendingSbomMetadataCleaner -> pendingSbomMetadataCleaner.execute(mockJobExecutionContext));
  }

  private void testExecute(final Consumer<PendingSbomMetadataCleaner> trigger) throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    Instant now = Instant.ofEpochMilli(Duration.ofDays(1).toMillis() + 1);
    List<ImmutablePair<String, File>> sbomData = createSbomData(application, now.toEpochMilli());

    try (MockedStatic<Instant> mockedInstant = mockStatic(Instant.class, Mockito.CALLS_REAL_METHODS)) {
      mockedInstant.when(Instant::now).thenReturn(now);
      trigger.accept(pendingSbomMetadataCleaner);
    }

    assertThat(thirdPartySbomMetadataDAO.getAll()).hasSize(6);
    List<ThirdPartySbomMetadata> sbomMetadata = sbomData.stream()
        .map(ImmutablePair::getLeft)
        .map(thirdPartySbomMetadataDAO::getById)
        .toList();
    // Uploaded SBOMs
    assertThat(sbomMetadata.get(0)).isNull();
    assertThat(sbomMetadata.get(1)).isNull();
    assertThat(sbomMetadata.get(2)).isNotNull();
    // Pending SBOMs
    assertThat(sbomMetadata.get(3)).isNull();
    assertThat(sbomMetadata.get(4)).isNull();
    assertThat(sbomMetadata.get(5)).isNotNull();
    // Active SBOMs
    assertThat(sbomMetadata.get(6)).isNotNull();
    assertThat(sbomMetadata.get(7)).isNotNull();
    assertThat(sbomMetadata.get(8)).isNotNull();
    // Temporary transient SBOMs, which were initialized with no corresponding metadata
    assertThat(sbomMetadata.get(9)).isNull();
    assertThat(sbomMetadata.get(10)).isNull();
    assertThat(sbomMetadata.get(11)).isNull();
    // Temporary persistent binaries
    assertThat(sbomMetadata.get(12)).isNull();
    assertThat(sbomMetadata.get(13)).isNull();
    assertThat(sbomMetadata.get(14)).isNotNull();

    List<File> sbomFiles = sbomData.stream().map(ImmutablePair::getRight).toList();
    // Check the file tree
    existingFilesHelper.assertExistingSbomFiles(
        Stream.of(
            sbomFiles.get(2),
            sbomFiles.get(5),
            sbomFiles.get(6),
            sbomFiles.get(7),
            sbomFiles.get(8),
            sbomFiles.get(11),
            sbomFiles.get(14))
            .map(file -> insightWork.getSbomDir().toPath().relativize(file.toPath()).toString())
            .toArray(String[]::new));
  }

  /**
   * @return a list of pairs where each pair is (the SBOM metadata ID if available, the file associated with the SBOM)
   */
  private List<ImmutablePair<String, File>> createSbomData(
      final Application application,
      final long currentTimeMillis) throws Exception
  {
    long moreThan24HoursOld = currentTimeMillis - Duration.ofHours(24).toMillis() - 1;
    long exactly24HoursOld = currentTimeMillis - Duration.ofHours(24).toMillis();
    long lessThan24HoursOld = currentTimeMillis - Duration.ofHours(24).toMillis() + 1;

    // Uploaded SBOMs
    ImmutablePair<String, File> uploadedSbomMoreThan24HoursOld = createSbom(application, UPLOADED);
    setLastModified(uploadedSbomMoreThan24HoursOld, moreThan24HoursOld);
    ImmutablePair<String, File> uploadedSbomExactly24HoursOld = createSbom(application, UPLOADED);
    setLastModified(uploadedSbomExactly24HoursOld, exactly24HoursOld);
    ImmutablePair<String, File> uploadedSbomLessThan24HoursOld = createSbom(application, UPLOADED);
    setLastModified(uploadedSbomLessThan24HoursOld, lessThan24HoursOld);

    // Pending SBOMs
    ImmutablePair<String, File> pendingSbomMoreThan24HoursOld = createSbom(application, PENDING);
    setLastModified(pendingSbomMoreThan24HoursOld, moreThan24HoursOld);
    ImmutablePair<String, File> pendingSbomExactly24HoursOld = createSbom(application, PENDING);
    setLastModified(pendingSbomExactly24HoursOld, exactly24HoursOld);
    ImmutablePair<String, File> pendingSbomLessThan24HoursOld = createSbom(application, PENDING);
    setLastModified(pendingSbomLessThan24HoursOld, lessThan24HoursOld);

    // Active SBOMs
    ImmutablePair<String, File> activeSbomMoreThan24HoursOld = createSbom(application, ACTIVE);
    setLastModified(activeSbomMoreThan24HoursOld, moreThan24HoursOld);
    ImmutablePair<String, File> activeSbomExactly24HoursOld = createSbom(application, ACTIVE);
    setLastModified(activeSbomExactly24HoursOld, exactly24HoursOld);
    ImmutablePair<String, File> activeSbomLessThan24HoursOld = createSbom(application, ACTIVE);
    setLastModified(activeSbomLessThan24HoursOld, lessThan24HoursOld);

    // Temporary transient SBOMs
    File tempTransientSbomMoreThan24HoursOld = createTemporaryTransientSbom();
    setLastModified(tempTransientSbomMoreThan24HoursOld, moreThan24HoursOld);
    File tempTransientSbomExactly24HoursOld = createTemporaryTransientSbom();
    setLastModified(tempTransientSbomExactly24HoursOld, exactly24HoursOld);
    File tempTransientSbomLessThan24HoursOld = createTemporaryTransientSbom();
    setLastModified(tempTransientSbomLessThan24HoursOld, lessThan24HoursOld);

    // Temporary persistent binaries
    ImmutablePair<String, File> tempPersistentBinaryMoreThan24HoursOld = createTemporaryPersistentBinary(application);
    setLastModified(tempPersistentBinaryMoreThan24HoursOld, moreThan24HoursOld);
    ImmutablePair<String, File> tempPersistentBinaryExactly24HoursOld = createTemporaryPersistentBinary(application);
    setLastModified(tempPersistentBinaryExactly24HoursOld, exactly24HoursOld);
    ImmutablePair<String, File> tempPersistentBinaryLessThan24HoursOld = createTemporaryPersistentBinary(application);
    setLastModified(tempPersistentBinaryLessThan24HoursOld, lessThan24HoursOld);

    return List.of(
        uploadedSbomMoreThan24HoursOld,
        uploadedSbomExactly24HoursOld,
        uploadedSbomLessThan24HoursOld,
        pendingSbomMoreThan24HoursOld,
        pendingSbomExactly24HoursOld,
        pendingSbomLessThan24HoursOld,
        activeSbomMoreThan24HoursOld,
        activeSbomExactly24HoursOld,
        activeSbomLessThan24HoursOld,
        ImmutablePair.of(null, tempTransientSbomMoreThan24HoursOld),
        ImmutablePair.of(null, tempTransientSbomExactly24HoursOld),
        ImmutablePair.of(null, tempTransientSbomLessThan24HoursOld),
        tempPersistentBinaryMoreThan24HoursOld,
        tempPersistentBinaryExactly24HoursOld,
        tempPersistentBinaryLessThan24HoursOld);
  }

  private ImmutablePair<String, File> createSbom(
      final Application application,
      final ThirdPartySbomMetadataStatus thirdPartySbomMetadataStatus) throws Exception
  {
    SbomDetectionResult sbomDetectionResult = new SbomDetectionResult();
    sbomDetectionResult.isSbom = true;
    sbomDetectionResult.isValid = true;
    sbomDetectionResult.mimeType = MediaType.APPLICATION_XML;
    SbomSummary sbomSummary = new SbomSummary();
    sbomSummary.applicationName = "app";
    sbomSummary.applicationVersion = "1.0";
    sbomSummary.specification = SbomSpecification.CYCLONEDX.toString();
    sbomSummary.version = ExportSpecification.CYCLONEDX_16.getVersion();
    sbomSummary.format = sbomDetectionResult.mimeType;
    sbomDetectionResult.summary = sbomSummary;
    ImmutablePair<ThirdPartySbomMetadata, ThirdPartyFile> sbomPersistenceResult;
    SbomEntity tempSbomPath = null;
    try {
      tempSbomPath = thirdPartyPersistenceService.writeToTransientStorage(
          new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8)), "sbom.xml");
      sbomPersistenceResult =
          thirdPartyPersistenceService.saveSbomManagerSbomOrBinary(
              tempSbomPath,
              "sbom.xml",
              application.getId(),
              sbomDetectionResult);
    }
    finally {
      if (tempSbomPath != null) {
        thirdPartyPersistenceService.deleteSbomFromTransientStorage(tempSbomPath);
      }
    }
    File sbom = Path.of(insightWork.getSbomDir().getAbsolutePath(), application.getId(),
        sbomPersistenceResult.getLeft().getFilename()).toFile();
    assertThat(sbom).exists();
    sbomPersistenceResult.getLeft().setStatus(thirdPartySbomMetadataStatus);
    thirdPartySbomMetadataDAO.update(sbomPersistenceResult.getLeft());
    return ImmutablePair.of(sbomPersistenceResult.getLeft().getId(), sbom);
  }

  private File createTemporaryTransientSbom() throws Exception {
    SbomEntity sbomEntity = thirdPartyPersistenceService.writeToTransientStorage(
        new ByteArrayInputStream("content".getBytes()),
        "sbom.xml");
    File tempTransientSbom = sbomEntity.getPath().toFile();
    assertThat(tempTransientSbom).exists();
    return tempTransientSbom;
  }

  private ImmutablePair<String, File> createTemporaryPersistentBinary(final Application application) throws Exception {
    SbomDetectionResult sbomDetectionResult = new SbomDetectionResult();
    sbomDetectionResult.isSbom = false;
    sbomDetectionResult.isValid = true;
    sbomDetectionResult.mimeType = MediaType.APPLICATION_XML;
    SbomSummary sbomSummary = new SbomSummary();
    sbomSummary.applicationName = "app";
    sbomSummary.applicationVersion = "1.0";
    sbomSummary.specification = SbomSpecification.CYCLONEDX.toString();
    sbomSummary.version = ExportSpecification.CYCLONEDX_16.getVersion();
    sbomSummary.format = sbomDetectionResult.mimeType;
    sbomDetectionResult.summary = sbomSummary;
    ImmutablePair<ThirdPartySbomMetadata, ThirdPartyFile> sbomPersistenceResult;
    SbomEntity tempSbomPath = null;
    try {
      tempSbomPath = thirdPartyPersistenceService.writeToTransientStorage(
          new ByteArrayInputStream("binary_content".getBytes(StandardCharsets.UTF_8)), "binary.zip");
      sbomPersistenceResult =
          thirdPartyPersistenceService.saveSbomManagerSbomOrBinary(
              tempSbomPath,
              "binary.zip",
              application.getId(),
              sbomDetectionResult);
    }
    finally {
      if (tempSbomPath != null) {
        thirdPartyPersistenceService.deleteSbomFromTransientStorage(tempSbomPath);
      }
    }

    return ImmutablePair.of(sbomPersistenceResult.getLeft().getId(),
        thirdPartyPersistenceService.getBinaryPersistentTempFilePath(sbomPersistenceResult.getLeft(),
            sbomPersistenceResult.getRight()).getPath().toFile());
  }

  private void setLastModified(final ImmutablePair<String, File> sbomData, final long time) throws IOException {
    setLastModified(sbomData.getLeft(), time);
    setLastModified(sbomData.getRight(), time);
  }

  private void setLastModified(final File file, final long time) throws IOException {
    Files.setLastModifiedTime(file.toPath(), FileTime.fromMillis(time));
  }

  private void setLastModified(final String thirdPartySbomMetadataId, final long time) {
    ThirdPartySbomMetadata thirdPartySbomMetadata = thirdPartySbomMetadataDAO.getById(thirdPartySbomMetadataId);
    thirdPartySbomMetadata.setCreatedAt(new Date(time));
    thirdPartySbomMetadataDAO.update(thirdPartySbomMetadata);
  }
}
