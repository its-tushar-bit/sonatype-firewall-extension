/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.brain.support.SupportService.SupportFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.support.SupportInfoTestHelper.WORK_DIR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@Ignore("CLM-30888") //https://sonatype.atlassian.net/browse/CLM-30888
public class SupportInfoUtilTest
{
  @Mock
  private MultiTenantInsightConfig insightConfig;

  private SupportInfoUtil supportInfoUtil;

  @Before
  public void setup() {
    SupportInfoUtil.COUNTER.set(0);
    supportInfoUtil = new SupportInfoUtil(insightConfig);
  }

  @AfterClass
  public static void tearDown() throws IOException {
    SupportInfoTestHelper.cleanWorkDir(WORK_DIR);
  }

  @Test
  public void shouldGetWorkDir() {
    // When
    when(insightConfig.getSonatypeWork()).thenReturn(new File("/sonatype-work"));
    File workDir = supportInfoUtil.getWorkDir();

    // Then
    assertThat(workDir)
        .hasName(WORK_DIR)
        .hasParent("/sonatype-work");
  }

  @Test
  public void shouldGenerateUniqueName() {
    // Given
    String now = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
    String nowPrefix = now.substring(0, now.indexOf("-"));

    // When
    String uniqueName = supportInfoUtil.generateUniqueName("tenant-mtiq-support-");

    // Then
    assertThat(uniqueName)
        .startsWith("tenant-mtiq-support-" + nowPrefix)
        .endsWith("-1");
  }

  @Test
  public void shouldWriteTextToFile() throws IOException {
    // Given
    Map<String, Object> tenantInfoParams = new HashMap<>();
    tenantInfoParams.put("tenant_info1", "value1");
    String tenantInfo = new ObjectMapper().writeValueAsString(tenantInfoParams);

    // When
    File outputFile = supportInfoUtil.writeTextToFile(tenantInfo, "file1.json");

    // Then
    assertThat(outputFile)
        .exists()
        .hasName("file1.json");
    String fileContents = new String(Files.readAllBytes(outputFile.toPath()));
    assertThat(fileContents).isEqualTo("{\"tenant_info1\":\"value1\"}");
  }

  @Test
  public void shouldGenerateSupportInfo() throws IOException {
    // Given
    Map<String, Object> tenantInfoParams = new HashMap<>();
    tenantInfoParams.put("tenant_info1", "value1");
    tenantInfoParams.put("tenant_info2", "value2");
    String tenantInfo = new ObjectMapper().writeValueAsString(tenantInfoParams);

    Map<String, Object> systemInfoParams = new HashMap<>();
    tenantInfoParams.put("iq_version", "1.156");
    String systemInfo = new ObjectMapper().writeValueAsString(systemInfoParams);

    File jsonFile1 = supportInfoUtil.writeTextToFile(tenantInfo, "file1.json");
    SupportFile supportFile1 = new SupportFile(SupportFileType.INFO, jsonFile1, true);

    File jsonFile2 = supportInfoUtil.writeTextToFile(systemInfo, "file2.json");
    SupportFile supportFile2 = new SupportFile(SupportFileType.INFO, jsonFile2, true);

    List<SupportFile> filesToZip = new ArrayList<>();
    filesToZip.add(supportFile1);
    filesToZip.add(supportFile2);

    // When
    SupportInfo supportInfo = supportInfoUtil.generateSupportInfo("tenant", filesToZip);

    // Then
    assertThat(supportInfo.getSupportInfoName()).contains("tenant-mtiq-support").endsWith("-1");

    ByteArrayInputStream byteArrayInputStream =
        new ByteArrayInputStream(supportInfo.getSupportInfoOutputStream().toByteArray());
    try (ZipInputStream zipInputStream = new ZipInputStream(byteArrayInputStream)) {
      ZipEntry firstEntry = zipInputStream.getNextEntry();
      assertThat(firstEntry).isNotNull();
      assertThat(firstEntry.getName()).isEqualTo(
          supportInfo.getSupportInfoName() + "/" + SupportFileType.INFO.getDirName() + "/" + "file1.json");

      ZipEntry secondEntry = zipInputStream.getNextEntry();
      assertThat(secondEntry).isNotNull();
      assertThat(secondEntry.getName()).isEqualTo(
          supportInfo.getSupportInfoName() + "/" + SupportFileType.INFO.getDirName() + "/" + "file2.json");

      assertThat(zipInputStream.getNextEntry()).isNull();
    }
  }
}
