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
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ClusterConfigReaderTest
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock
  private MultiTenantInsightConfig mockMultiTenantInsightConfig;

  @Spy
  private ObjectMapper spyObjectMapper;

  @Mock
  private File mockFile;

  private ClusterConfigReader spyClusterConfigReader;

  @Before
  public void before() {
    spyClusterConfigReader = spy(new ClusterConfigReader(mockMultiTenantInsightConfig, spyObjectMapper));
  }

  @Test
  public void testGetClusterConfig_NullPath() {
    when(mockMultiTenantInsightConfig.getClusterConfigFilePath()).thenReturn(null);

    ClusterConfig clusterConfig = spyClusterConfigReader.getClusterConfig();

    assertThat(clusterConfig.getState()).isEqualTo(ClusterState.UNKNOWN);
    assertThat(clusterConfig.getLastModified()).isNull();
  }

  @Test
  public void testGetClusterConfig_DoesNotExist() {
    when(mockMultiTenantInsightConfig.getClusterConfigFilePath()).thenReturn("doesNotExist");

    ClusterConfig clusterConfig = spyClusterConfigReader.getClusterConfig();

    assertThat(clusterConfig.getState()).isEqualTo(ClusterState.UNKNOWN);
    assertThat(clusterConfig.getLastModified()).isNull();
  }

  @Test
  public void testGetClusterConfig_CannotRead() {
    when(mockMultiTenantInsightConfig.getClusterConfigFilePath()).thenReturn("exists");
    when(mockFile.exists()).thenReturn(true);
    when(mockFile.canRead()).thenReturn(false);
    doReturn(mockFile).when(spyClusterConfigReader).newFile(any());

    ClusterConfig clusterConfig = spyClusterConfigReader.getClusterConfig();

    assertThat(clusterConfig.getState()).isEqualTo(ClusterState.UNKNOWN);
    assertThat(clusterConfig.getLastModified()).isNull();
  }

  @Test
  public void testGetClusterConfig_EmptyFile() throws Exception {
    File emptyFile = temporaryFolder.newFile();
    when(mockMultiTenantInsightConfig.getClusterConfigFilePath()).thenReturn(emptyFile.getAbsolutePath());

    ClusterConfig clusterConfig = spyClusterConfigReader.getClusterConfig();

    assertThat(clusterConfig.getState()).isEqualTo(ClusterState.UNKNOWN);
    assertThat(clusterConfig.getLastModified()).isEqualTo(emptyFile.lastModified());
  }

  @Test
  public void testGetClusterConfig_NotJsonFile() throws Exception {
    File notJsonFile = temporaryFolder.newFile();
    FileUtils.writeStringToFile(notJsonFile, "state=inactive\nother=value", StandardCharsets.UTF_8);
    when(mockMultiTenantInsightConfig.getClusterConfigFilePath()).thenReturn(notJsonFile.getAbsolutePath());

    ClusterConfig clusterConfig = spyClusterConfigReader.getClusterConfig();

    assertThat(clusterConfig.getState()).isEqualTo(ClusterState.UNKNOWN);
    assertThat(clusterConfig.getLastModified()).isEqualTo(notJsonFile.lastModified());
  }

  @Test
  public void testGetClusterConfig_MissingStateField() throws Exception {
    File missingStateFile = temporaryFolder.newFile();
    FileUtils.writeStringToFile(missingStateFile, "{\"other\": \"value\"}", StandardCharsets.UTF_8);
    when(mockMultiTenantInsightConfig.getClusterConfigFilePath()).thenReturn(missingStateFile.getAbsolutePath());

    ClusterConfig clusterConfig = spyClusterConfigReader.getClusterConfig();

    assertThat(clusterConfig.getState()).isEqualTo(ClusterState.UNKNOWN);
    assertThat(clusterConfig.getLastModified()).isEqualTo(missingStateFile.lastModified());
  }

  @Test
  public void testGetClusterConfig_Active() throws Exception {
    File clusterConfigFile = createClusterConfigFile(ClusterState.ACTIVE);
    when(mockMultiTenantInsightConfig.getClusterConfigFilePath()).thenReturn(clusterConfigFile.getAbsolutePath());

    ClusterConfig clusterConfig = spyClusterConfigReader.getClusterConfig();

    assertThat(clusterConfig.getState()).isEqualTo(ClusterState.ACTIVE);
    assertThat(clusterConfig.getLastModified()).isEqualTo(clusterConfigFile.lastModified());
  }

  @Test
  public void testGetClusterConfig_Filling() throws Exception {
    File clusterConfigFile = createClusterConfigFile(ClusterState.FILLING);
    when(mockMultiTenantInsightConfig.getClusterConfigFilePath()).thenReturn(clusterConfigFile.getAbsolutePath());

    ClusterConfig clusterConfig = spyClusterConfigReader.getClusterConfig();

    assertThat(clusterConfig.getState()).isEqualTo(ClusterState.FILLING);
    assertThat(clusterConfig.getLastModified()).isEqualTo(clusterConfigFile.lastModified());
  }

  @Test
  public void testGetClusterConfig_Draining() throws Exception {
    File clusterConfigFile = createClusterConfigFile(ClusterState.DRAINING);
    when(mockMultiTenantInsightConfig.getClusterConfigFilePath()).thenReturn(clusterConfigFile.getAbsolutePath());

    ClusterConfig clusterConfig = spyClusterConfigReader.getClusterConfig();

    assertThat(clusterConfig.getState()).isEqualTo(ClusterState.DRAINING);
    assertThat(clusterConfig.getLastModified()).isEqualTo(clusterConfigFile.lastModified());
  }

  @Test
  public void testGetClusterConfig_Inactive() throws Exception {
    File clusterConfigFile = createClusterConfigFile(ClusterState.INACTIVE);
    when(mockMultiTenantInsightConfig.getClusterConfigFilePath()).thenReturn(clusterConfigFile.getAbsolutePath());

    ClusterConfig clusterConfig = spyClusterConfigReader.getClusterConfig();

    assertThat(clusterConfig.getState()).isEqualTo(ClusterState.INACTIVE);
    assertThat(clusterConfig.getLastModified()).isEqualTo(clusterConfigFile.lastModified());
  }

  @Test
  public void testMonitor_FileNotChanged() throws Exception {
    // 1st call - draining state, file first read
    File clusterConfigFile = createClusterConfigFile(ClusterState.DRAINING);
    when(mockMultiTenantInsightConfig.getClusterConfigFilePath()).thenReturn(clusterConfigFile.getAbsolutePath());

    spyClusterConfigReader.getClusterConfig();

    verify(spyObjectMapper).readValue(clusterConfigFile, ClusterConfig.class);

    // 2nd call - draining state, no file change, file not read again
    Mockito.reset(spyObjectMapper);

    spyClusterConfigReader.getClusterConfig();

    verifyNoInteractions(spyObjectMapper);

    // 3rd call - inactive state, file changed, file read again
    writeClusterConfigToFile(ClusterState.INACTIVE, clusterConfigFile);

    spyClusterConfigReader.getClusterConfig();

    verify(spyObjectMapper).readValue(clusterConfigFile, ClusterConfig.class);
  }

  private File createClusterConfigFile(final ClusterState clusterState) throws Exception {
    File clusterConfigFile = temporaryFolder.newFile();
    writeClusterConfigToFile(clusterState, clusterConfigFile);
    return clusterConfigFile;
  }

  private void writeClusterConfigToFile(final ClusterState clusterState, final File clusterConfigFile)
      throws Exception
  {
    new ObjectMapper().writeValue(clusterConfigFile, createClusterConfig(clusterState));
  }

  private ObjectNode createClusterConfig(final ClusterState clusterState) {
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode objectNode = objectMapper.createObjectNode();
    objectNode.put("state", clusterState.name().toLowerCase(Locale.ROOT));
    objectNode.put("other", "value");
    return objectNode;
  }
}
