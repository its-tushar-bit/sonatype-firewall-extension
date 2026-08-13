/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.variant.AbstractComponentH2Test;

import java.io.File;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentH2Test
public class InsightWorkTest
    extends AbstractComponentH2Test
{
  private static final String VALID_ID = "abcdefghijklmnopqrstuvwxyz_ABCDEFGHIJKLOMOPQUSTUVWXYZ-0123456789";

  private static final String[] INVALID_CHARACTERS = {".", "\\", "/", "%"};

  private static final String WORK_ROOT = new File("target", "insight-work-test-work").getAbsolutePath();

  private static final String CLUSTER_ROOT = new File("target", "insight-work-test-cluster").getAbsolutePath();

  private static final String RELATIVE_CLONE_DIRECTORY = "source-control";

  @Inject
  private InsightWork work;

  @Inject
  private InsightConfig insightConfig;

  @Test
  public void testGetScanDir() {
    File file = work.getScanDir(VALID_ID);
    assertThat(file).isNotNull();
  }

  @Test
  public void testGetScanDir_InvalidAppId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> work.getScanDir(invalidValue))
          .withMessage("Invalid value: " + invalidValue);
    }
  }

  @Test
  public void testGetScanFile() {
    File file = work.getScanFile(VALID_ID, VALID_ID);
    assertThat(file).isNotNull();
  }

  @Test
  public void testGetScanFile_InvalidAppIdValidScanId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> work.getScanFile(invalidValue, VALID_ID))
          .withMessage("Invalid value: " + invalidValue);
    }
  }

  @Test
  public void testGetScanFile_ValidAppIdInvalidScanId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> work.getScanFile(VALID_ID, invalidValue))
          .withMessage("Invalid value: " + invalidValue);
    }
  }

  @Test
  public void testGetAuditDir() {
    File file = work.getAuditDir(VALID_ID);
    assertThat(file).isNotNull();
  }

  @Test
  public void testGetAuditDir_InvalidAppId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> work.getAuditDir(invalidValue))
          .withMessage("Invalid value: " + invalidValue);
    }
  }

  @Test
  public void testGetReportDir() {
    File file = work.getReportDir(VALID_ID);
    assertThat(file).isNotNull();
  }

  @Test
  public void testGetReportDir_InvalidAppId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> work.getReportDir(invalidValue))
          .withMessage("Invalid value: " + invalidValue);
    }
  }

  @Test
  public void testGetReportDir_InvalidAppIdValidScanId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> work.getReportDir(invalidValue, VALID_ID))
          .withMessage("Invalid value: " + invalidValue);
    }
  }

  @Test
  public void testGetReportDir_ValidAppIdInvalidScanId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> work.getReportDir(VALID_ID, invalidValue))
          .withMessage("Invalid value: " + invalidValue);
    }
  }

  @Test
  public void testGetReportFile() {
    File file = work.getReportFile(VALID_ID, VALID_ID);
    assertThat(file).isNotNull();
  }

  @Test
  public void testGetReportFile_InvalidAppIdValidScanId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> work.getReportFile(invalidValue, VALID_ID))
          .withMessage("Invalid value: " + invalidValue);
    }
  }

  @Test
  public void testGetReportFile_ValidAppIdInvalidScanId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> work.getReportFile(VALID_ID, invalidValue))
          .withMessage("Invalid value: " + invalidValue);
    }
  }

  @Test
  public void testComponentDetailsDir() {
    File file = work.getComponentDetailsDir(VALID_ID);
    assertThat(file).isNotNull();
  }

  @Test
  public void testComponentDetailsDir_InvalidAppId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> work.getComponentDetailsDir(invalidValue))
          .withMessage("Invalid value: " + invalidValue);
    }
  }

  @Test
  public void testComponentDetailsFile() {
    File file = work.getComponentDetailsFile(VALID_ID, VALID_ID);
    assertThat(file).isNotNull();
  }

  @Test
  public void testComponentDetailsFile_InvalidAppIdValidResultsId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> work.getComponentDetailsFile(invalidValue, VALID_ID))
          .withMessage("Invalid value: " + invalidValue);
    }
  }

  @Test
  public void testComponentDetailsFile_ValidAppIdInvalidResultsId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> work.getComponentDetailsFile(VALID_ID, invalidValue))
          .withMessage("Invalid value: " + invalidValue);
    }
  }

  @Test
  public void testSourceControlDir() {
    File file = work.getSourceControlDir(VALID_ID);
    assertThat(file).isNotNull();
  }

  @Test
  public void testSourceControlDir_InvalidAppId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> work.getSourceControlDir(invalidValue))
          .withMessage("Invalid value: " + invalidValue);
    }
  }

  @Test
  public void testGetTemporaryDirectory() {
    File file = work.getTemporaryDirectory();
    assertThat(file).isNotNull();
  }

  @Test
  public void testGetCacheDir() {
    assertThat(work.getClusterCacheDir()).isEqualTo(new File(insightConfig.getClusterDirectory(), "cache"));
  }

  @Test
  public void testGetScanDir_WithoutAppId() {
    assertThat(work.getScanDir()).isEqualTo(new File(insightConfig.getClusterDirectory(), "scan"));
  }

  @Test
  public void testGetIerDashboardIconsDirectory() {
    assertThat(work.getIerDashboardIconsDirectory()).isEqualTo(
        new File(work.getNodeCacheDir(), "enterpriseReportingDashboardIcons"));
  }

  @Test
  public void testApplicationSbomDir() {
    File file = work.getSbomDir("appId");
    assertThat(file).isNotNull();
    assertThat(file.exists()).isTrue();
  }

  @Test
  public void testApplicationSbomDirWithCreateFlagTrue() {
    File file = work.getSbomDir("appId", true);
    assertThat(file).isNotNull();
    assertThat(file.exists()).isTrue();
  }

  @Test
  public void testApplicationSbomDirWithCreateFlagFalse() {
    File file = work.getSbomDir("appId", false);
    assertThat(file).isNotNull();
    assertThat(file.exists()).isFalse();
  }

  @Test
  public void testGetResolvedCloneDirectory_relativePathResolvesAgainstSonatypeWorkWhenDisabled() {
    InsightWork underTest = newInsightWork(RELATIVE_CLONE_DIRECTORY, false, CLUSTER_ROOT);

    File resolved = underTest.getResolvedCloneDirectory();

    assertThat(resolved).isEqualTo(new File(WORK_ROOT, RELATIVE_CLONE_DIRECTORY));
  }

  @Test
  public void testGetResolvedCloneDirectory_relativePathResolvesAgainstClusterDirectoryWhenEnabled() {
    InsightWork underTest = newInsightWork(RELATIVE_CLONE_DIRECTORY, true, CLUSTER_ROOT);

    File resolved = underTest.getResolvedCloneDirectory();

    assertThat(resolved).isEqualTo(new File(CLUSTER_ROOT, RELATIVE_CLONE_DIRECTORY));
  }

  @Test
  public void testGetResolvedCloneDirectory_absolutePathIsUsedVerbatimWhenEnabled() {
    String absoluteCloneDirectory = new File("target", "absolute-clone-directory").getAbsolutePath();
    InsightWork underTest = newInsightWork(absoluteCloneDirectory, true, CLUSTER_ROOT);

    File resolved = underTest.getResolvedCloneDirectory();

    assertThat(resolved).isEqualTo(new File(absoluteCloneDirectory));
  }

  @Test
  public void testGetResolvedCloneDirectory_absolutePathIsUsedVerbatimWhenDisabled() {
    String absoluteCloneDirectory = new File("target", "absolute-clone-directory").getAbsolutePath();
    InsightWork underTest = newInsightWork(absoluteCloneDirectory, false, CLUSTER_ROOT);

    File resolved = underTest.getResolvedCloneDirectory();

    assertThat(resolved).isEqualTo(new File(absoluteCloneDirectory));
  }

  @Test
  public void testGetResolvedCloneDirectory_clusterDirectoryUnsetFallsBackToSonatypeWorkWhenEnabled() {
    InsightWork underTest = newInsightWork(RELATIVE_CLONE_DIRECTORY, true, null);

    File resolved = underTest.getResolvedCloneDirectory();

    assertThat(resolved).isEqualTo(new File(WORK_ROOT, RELATIVE_CLONE_DIRECTORY));
  }

  @Test
  public void testGetSourceControlDir_relativePathResolvesAgainstClusterDirectoryWhenEnabled() {
    InsightWork underTest = newInsightWork(RELATIVE_CLONE_DIRECTORY, true, CLUSTER_ROOT);

    File resolved = underTest.getSourceControlDir(VALID_ID);

    assertThat(resolved).isEqualTo(new File(new File(CLUSTER_ROOT, RELATIVE_CLONE_DIRECTORY), VALID_ID));
  }

  private InsightWork newInsightWork(
      final String cloneDirectory,
      final boolean cloneDirectoryOnClusterStorage,
      final String clusterDirectory)
  {
    InsightConfig config = new InsightConfig();
    config.setSonatypeWork(WORK_ROOT);
    config.setClusterDirectory(clusterDirectory);

    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setCloneDirectory(cloneDirectory);

    Configuration mockConfiguration = mock(Configuration.class);
    when(mockConfiguration.getSourceControlConfigurationOrDefault()).thenReturn(sourceControlConfiguration);
    lenient().when(mockConfiguration.isSourceControlCloneDirectoryOnClusterStorage())
        .thenReturn(cloneDirectoryOnClusterStorage);

    return new InsightWork(config, mockConfiguration);
  }
}
