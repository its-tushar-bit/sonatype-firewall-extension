/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.cluster;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.sonatype.insight.brain.service.MultiTenantInsightConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CloudyClusterConfigReaderTest
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock
  private MultiTenantInsightConfig mockMultiTenantInsightConfig;

  private ObjectMapper spyObjectMapper;

  @Mock
  private File mockFile;

  private CloudyClusterConfigReader spyCloudyClusterConfigReader;

  @Before
  public void before() {
    spyObjectMapper = spy(CloudyClusterConfigReader.createObjectMapper());
    spyCloudyClusterConfigReader = spy(new CloudyClusterConfigReader(mockMultiTenantInsightConfig, spyObjectMapper));
  }

  @Test
  public void testGetClusterConfig_NullPath() {
    when(mockMultiTenantInsightConfig.getCloudyClusterConfigFilePath()).thenReturn(null);

    CloudyClusterConfig cloudyClusterConfig = spyCloudyClusterConfigReader.getClusterConfig();

    assertThat(cloudyClusterConfig.getState()).isEqualTo(CloudyClusterState.UNKNOWN);
    assertThat(cloudyClusterConfig.getLastModified()).isNull();
  }

  @Test
  public void testGetClusterConfig_DoesNotExist() {
    when(mockMultiTenantInsightConfig.getCloudyClusterConfigFilePath()).thenReturn("doesNotExist");

    CloudyClusterConfig cloudyClusterConfig = spyCloudyClusterConfigReader.getClusterConfig();

    assertThat(cloudyClusterConfig.getState()).isEqualTo(CloudyClusterState.UNKNOWN);
    assertThat(cloudyClusterConfig.getLastModified()).isNull();
  }

  @Test
  public void testGetClusterConfig_CannotRead() {
    when(mockMultiTenantInsightConfig.getCloudyClusterConfigFilePath()).thenReturn("exists");
    when(mockFile.exists()).thenReturn(true);
    when(mockFile.canRead()).thenReturn(false);
    doReturn(mockFile).when(spyCloudyClusterConfigReader).newFile(any());

    CloudyClusterConfig cloudyClusterConfig = spyCloudyClusterConfigReader.getClusterConfig();

    assertThat(cloudyClusterConfig.getState()).isEqualTo(CloudyClusterState.UNKNOWN);
    assertThat(cloudyClusterConfig.getLastModified()).isNull();
  }

  @Test
  public void testGetClusterConfig_EmptyFile() throws Exception {
    File emptyFile = temporaryFolder.newFile();
    when(mockMultiTenantInsightConfig.getCloudyClusterConfigFilePath()).thenReturn(emptyFile.getAbsolutePath());

    CloudyClusterConfig cloudyClusterConfig = spyCloudyClusterConfigReader.getClusterConfig();

    assertThat(cloudyClusterConfig.getState()).isEqualTo(CloudyClusterState.UNKNOWN);
    assertThat(cloudyClusterConfig.getLastModified()).isEqualTo(emptyFile.lastModified());
  }

  @Test
  public void testGetClusterConfig_NotJsonFile() throws Exception {
    File notJsonFile = temporaryFolder.newFile();
    FileUtils.writeStringToFile(notJsonFile, "state=inactive\nother=value", StandardCharsets.UTF_8);
    when(mockMultiTenantInsightConfig.getCloudyClusterConfigFilePath()).thenReturn(notJsonFile.getAbsolutePath());

    CloudyClusterConfig clusterConfig = spyCloudyClusterConfigReader.getClusterConfig();

    assertThat(clusterConfig.getState()).isEqualTo(CloudyClusterState.UNKNOWN);
    assertThat(clusterConfig.getLastModified()).isEqualTo(notJsonFile.lastModified());
  }

  @Test
  public void testGetClusterConfig_MissingStateField() throws Exception {
    File missingStateFile = temporaryFolder.newFile();
    FileUtils.writeStringToFile(missingStateFile, "{\"other\": \"value\"}", StandardCharsets.UTF_8);
    when(mockMultiTenantInsightConfig.getCloudyClusterConfigFilePath()).thenReturn(missingStateFile.getAbsolutePath());

    CloudyClusterConfig cloudyClusterConfig = spyCloudyClusterConfigReader.getClusterConfig();

    assertThat(cloudyClusterConfig.getState()).isEqualTo(CloudyClusterState.UNKNOWN);
    assertThat(cloudyClusterConfig.getLastModified()).isEqualTo(missingStateFile.lastModified());
  }

  @Test
  public void testGetClusterConfig_Active() throws Exception {
    File clusterConfigFile = createClusterConfigFile(CloudyClusterState.ACTIVE);
    when(mockMultiTenantInsightConfig.getCloudyClusterConfigFilePath()).thenReturn(clusterConfigFile.getAbsolutePath());

    CloudyClusterConfig cloudyClusterConfig = spyCloudyClusterConfigReader.getClusterConfig();

    assertThat(cloudyClusterConfig.getState()).isEqualTo(CloudyClusterState.ACTIVE);
    assertThat(cloudyClusterConfig.getLastModified()).isEqualTo(clusterConfigFile.lastModified());
  }

  @Test
  public void testGetClusterConfig_Filling() throws Exception {
    File clusterConfigFile = createClusterConfigFile(CloudyClusterState.FILLING);
    when(mockMultiTenantInsightConfig.getCloudyClusterConfigFilePath()).thenReturn(clusterConfigFile.getAbsolutePath());

    CloudyClusterConfig cloudyClusterConfig = spyCloudyClusterConfigReader.getClusterConfig();

    assertThat(cloudyClusterConfig.getState()).isEqualTo(CloudyClusterState.FILLING);
    assertThat(cloudyClusterConfig.getLastModified()).isEqualTo(clusterConfigFile.lastModified());
  }

  @Test
  public void testGetClusterConfig_Draining() throws Exception {
    File clusterConfigFile = createClusterConfigFile(CloudyClusterState.DRAINING);
    when(mockMultiTenantInsightConfig.getCloudyClusterConfigFilePath()).thenReturn(clusterConfigFile.getAbsolutePath());

    CloudyClusterConfig cloudyClusterConfig = spyCloudyClusterConfigReader.getClusterConfig();

    assertThat(cloudyClusterConfig.getState()).isEqualTo(CloudyClusterState.DRAINING);
    assertThat(cloudyClusterConfig.getLastModified()).isEqualTo(clusterConfigFile.lastModified());
  }

  @Test
  public void testGetClusterConfig_Inactive() throws Exception {
    File clusterConfigFile = createClusterConfigFile(CloudyClusterState.INACTIVE);
    when(mockMultiTenantInsightConfig.getCloudyClusterConfigFilePath()).thenReturn(clusterConfigFile.getAbsolutePath());

    CloudyClusterConfig cloudyClusterConfig = spyCloudyClusterConfigReader.getClusterConfig();

    assertThat(cloudyClusterConfig.getState()).isEqualTo(CloudyClusterState.INACTIVE);
    assertThat(cloudyClusterConfig.getLastModified()).isEqualTo(clusterConfigFile.lastModified());
  }

  @Test
  public void testMonitor_FileNotChanged() throws Exception {
    // 1st call - draining state, file first read
    File clusterConfigFile = createClusterConfigFile(CloudyClusterState.DRAINING);
    when(mockMultiTenantInsightConfig.getCloudyClusterConfigFilePath()).thenReturn(clusterConfigFile.getAbsolutePath());

    spyCloudyClusterConfigReader.getClusterConfig();

    verify(spyObjectMapper).readValue(clusterConfigFile, CloudyClusterConfig.class);

    // 2nd call - draining state, no file change, file not read again
    Mockito.reset(spyObjectMapper);

    spyCloudyClusterConfigReader.getClusterConfig();

    verifyNoInteractions(spyObjectMapper);

    // 3rd call - inactive state, file changed, file read again
    writeClusterConfigToFile(CloudyClusterState.INACTIVE, clusterConfigFile);

    spyCloudyClusterConfigReader.getClusterConfig();

    verify(spyObjectMapper).readValue(clusterConfigFile, CloudyClusterConfig.class);
  }

  private File createClusterConfigFile(final CloudyClusterState cloudyClusterState) throws Exception {
    File clusterConfigFile = temporaryFolder.newFile();
    writeClusterConfigToFile(cloudyClusterState, clusterConfigFile);
    return clusterConfigFile;
  }

  private void writeClusterConfigToFile(final CloudyClusterState cloudyClusterState, final File clusterConfigFile)
      throws Exception
  {
    spyObjectMapper.writeValue(clusterConfigFile, createClusterConfig(cloudyClusterState));
  }

  private ObjectNode createClusterConfig(final CloudyClusterState cloudyClusterState) {
    ObjectNode objectNode = spyObjectMapper.createObjectNode();
    objectNode.put("state", cloudyClusterState.name().toLowerCase(Locale.ROOT));
    objectNode.put("other", "value");
    return objectNode;
  }
}
