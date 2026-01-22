/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.datastore;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class FileSbomPersistenceServiceTest
    extends AbstractComponentTest
{
  private static final String APP_ID = "test-app";
  
  private static final String PREFIX_ID = "prefix-test";

  private static final String FILE_NAME = "test-sbom.xml";

  private static final String SBOM_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><sbom>Test SBOM Content</sbom>";

  @Inject
  private InsightWork insightWork;

  @Inject
  private FileCleaner fileCleaner;

  private FileSbomPersistenceService service;

  @Before
  public void setup() throws Exception {
    service = new FileSbomPersistenceService(insightWork, fileCleaner);
  }

  @Test
  public void testGetPermanentSbom() throws Exception {
    // Create directories for the SBOM
    Path sbomDir = insightWork.getSbomDir(APP_ID).toPath();
    Files.createDirectories(sbomDir);
    
    // Create the SBOM file
    Path sbomPath = sbomDir.resolve(FILE_NAME);
    Files.write(sbomPath, SBOM_CONTENT.getBytes(StandardCharsets.UTF_8));
    
    // Test getSbom method
    SbomEntity entity = service.getPermanentSbom(APP_ID, FILE_NAME);
    
    // Verify the entity properties
    assertThat(entity).isInstanceOf(FileSbomEntity.class);
    assertThat(entity.getAppId()).isEqualTo(APP_ID);
    assertThat(entity.getName()).isEqualTo(FILE_NAME);
    assertThat(entity.getPath()).exists();
    assertThat(entity.getPath()).isEqualTo(sbomPath);
    
    // Verify the content
    try (InputStream is = entity.getInputStream()) {
      String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      assertThat(content).isEqualTo(SBOM_CONTENT);
    }
  }

  @Test
  public void testGetPermanentSbom_withInvalidAppId() {
    // Test getSbom method with invalid app ID
    assertThatThrownBy(() -> service.getPermanentSbom("invalid/app", FILE_NAME))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Invalid value: " + "invalid/app");
  }

  @Test
  public void testGetTemporarySbom() throws Exception {
    // Create directories for the persistent temp SBOM
    Path sbomDir = insightWork.getSbomPersistentTempDir().toPath();
    Files.createDirectories(sbomDir);
    
    // Create the SBOM file
    Path sbomPath = sbomDir.resolve(FILE_NAME);
    Files.write(sbomPath, SBOM_CONTENT.getBytes(StandardCharsets.UTF_8));
    
    // Test getPersistentTempSbom method
    SbomEntity entity = service.getTemporarySbom(FILE_NAME, null);
    
    // Verify the entity properties
    assertThat(entity).isInstanceOf(FileSbomEntity.class);
    assertThat(entity.getAppId()).isNull();
    assertThat(entity.getName()).isEqualTo(FILE_NAME);
    assertThat(entity.getPath()).exists();
    assertThat(entity.getPath()).isEqualTo(sbomPath);
    
    // Verify the content
    try (InputStream is = entity.getInputStream()) {
      String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      assertThat(content).isEqualTo(SBOM_CONTENT);
    }
  }
  
  @Test
  public void testGetTemporarySbomWithPrefix() throws Exception {
    // Create directories for the persistent temp SBOM with prefix
    Path sbomDir = insightWork.getSbomPersistentTempDir().toPath();
    Path prefixDir = sbomDir.resolve(PREFIX_ID);
    Files.createDirectories(prefixDir);
    
    // Create the SBOM file in the prefix directory
    Path sbomPath = prefixDir.resolve(FILE_NAME);
    Files.write(sbomPath, SBOM_CONTENT.getBytes(StandardCharsets.UTF_8));
    
    // Test getPersistentTempSbom method with prefix
    SbomEntity entity = service.getTemporarySbom(FILE_NAME, PREFIX_ID);
    
    // Verify the entity properties
    assertThat(entity).isInstanceOf(FileSbomEntity.class);
    assertThat(entity.getAppId()).isNull();
    assertThat(entity.getName()).isEqualTo(FILE_NAME);
    assertThat(entity.getPath()).exists();
    assertThat(entity.getPath()).isEqualTo(sbomPath);
    
    // Verify the content
    try (InputStream is = entity.getInputStream()) {
      String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      assertThat(content).isEqualTo(SBOM_CONTENT);
    }
  }

  @Test
  public void testCreatePermanentSbom() throws Exception {
    // Test createPermanentSbom method
    SbomEntity entity = service.doGetSbom(APP_ID, FILE_NAME);

    Path expectedPath = insightWork.getSbomDir(APP_ID).toPath().resolve(FILE_NAME);

    // Verify the entity properties
    assertThat(entity).isInstanceOf(FileSbomEntity.class);
    assertThat(entity.getAppId()).isEqualTo(APP_ID);
    assertThat(entity.getName()).isEqualTo(FILE_NAME);
    assertThat(entity.getPath()).isNotNull();
    assertThat(entity.getPath()).isEqualTo(expectedPath);

    assertThat(entity.exists()).isFalse();
    assertThat(Files.exists(expectedPath)).isFalse();
    
    try (OutputStream os = entity.getOutputStream()) {
      os.write(SBOM_CONTENT.getBytes(StandardCharsets.UTF_8));
    }
    
    assertThat(entity.exists()).isTrue();
    assertThat(Files.readAllBytes(expectedPath)).isEqualTo(SBOM_CONTENT.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  public void testCreatePermanentSbom_fileExists() throws Exception {
    // Create directories for the SBOM
    Path sbomDir = insightWork.getSbomDir(APP_ID).toPath();
    Files.createDirectories(sbomDir);
    
    // Create the SBOM file
    Path sbomPath = sbomDir.resolve(FILE_NAME);
    Files.write(sbomPath, SBOM_CONTENT.getBytes(StandardCharsets.UTF_8));
    
    SbomEntity entity = service.doGetSbom(APP_ID, FILE_NAME);
    assertThat(entity.exists()).isTrue();
  }

  @Test
  public void testCreateTransientSbom() throws Exception {
    SbomEntity entity = service.getTransientSbom(FILE_NAME);
    
    // Verify the entity properties
    assertThat(entity).isInstanceOf(FileSbomEntity.class);
    assertThat(entity.getAppId()).isNull();

    // The name will be a generated UUID with the same extension
    assertThat(entity.getName()).isNotEqualTo(FILE_NAME);
    assertThat(entity.getName()).endsWith(".xml");

    Path transientDir = insightWork.getSbomTransientDir().toPath();
    Path sbomPath = transientDir.resolve(entity.getName());
    assertThat(entity.getPath()).isEqualTo(sbomPath);

    // Verify the file is created
    assertThat(Files.exists(sbomPath)).isTrue();
    
    // Write content to the entity
    try (OutputStream os = entity.getOutputStream()) {
      os.write(SBOM_CONTENT.getBytes(StandardCharsets.UTF_8));
    }
    
    // Verify that the file now exists and the content was written
    assertThat(Files.readAllBytes(sbomPath)).isEqualTo(SBOM_CONTENT.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  public void testCreateTransientSbom_fileExists() throws Exception {
    // Create directories for the transient SBOM
    Path sbomDir = insightWork.getSbomTransientDir().toPath();
    Files.createDirectories(sbomDir);
    
    // Create two transient SBOMs - this should work fine since they get unique names
    SbomEntity entity1 = service.getTransientSbom(FILE_NAME);
    SbomEntity entity2 = service.getTransientSbom(FILE_NAME);
    
    // Verify they have different names but the same extension
    assertThat(entity1.getName()).isNotEqualTo(entity2.getName());
    assertThat(entity1.getName()).endsWith(".xml");
    assertThat(entity2.getName()).endsWith(".xml");
  }

  @Test
  public void testSaveTemporarySbom() throws Exception {
    // Create a source SBOM entity
    Path sourceDir = tempDir.newFolder("source").toPath();
    Path sourcePath = sourceDir.resolve(FILE_NAME);
    Files.write(sourcePath, SBOM_CONTENT.getBytes(StandardCharsets.UTF_8));
    FileSbomEntity sourceSbom = new FileSbomEntity(sourcePath, APP_ID, FILE_NAME);
    
    // Save it as a persistent temp SBOM
    String newFileName = "saved-" + FILE_NAME;
    SbomEntity savedSbom = service.saveTemporarySbom(sourceSbom, newFileName, null);
    
    // Verify the saved entity properties
    assertThat(savedSbom).isInstanceOf(FileSbomEntity.class);
    assertThat(savedSbom.getAppId()).isEqualTo(APP_ID);
    assertThat(savedSbom.getName()).isEqualTo(newFileName);
    
    // Verify the file was created and has the correct content
    Path expectedPath = insightWork.getSbomPersistentTempDir().toPath().resolve(newFileName);
    assertThat(Files.exists(expectedPath)).isTrue();
    assertThat(Files.readAllBytes(expectedPath)).isEqualTo(SBOM_CONTENT.getBytes(StandardCharsets.UTF_8));
  }
  
  @Test
  public void testSaveTemporarySbomWithPrefix() throws Exception {
    // Create a source SBOM entity
    Path sourceDir = tempDir.newFolder("source").toPath();
    Path sourcePath = sourceDir.resolve(FILE_NAME);
    Files.write(sourcePath, SBOM_CONTENT.getBytes(StandardCharsets.UTF_8));
    FileSbomEntity sourceSbom = new FileSbomEntity(sourcePath, APP_ID, FILE_NAME);
    
    // Save it as a persistent temp SBOM with a prefix
    String newFileName = "saved-" + FILE_NAME;
    SbomEntity savedSbom = service.saveTemporarySbom(sourceSbom, newFileName, PREFIX_ID);
    
    // Verify the saved entity properties
    assertThat(savedSbom).isInstanceOf(FileSbomEntity.class);
    assertThat(savedSbom.getAppId()).isEqualTo(APP_ID);
    assertThat(savedSbom.getName()).isEqualTo(newFileName);
    
    // Verify the file was created and has the correct content
    Path expectedPath = insightWork.getSbomPersistentTempDir().toPath()
        .resolve(PREFIX_ID).resolve(newFileName);
    assertThat(Files.exists(expectedPath)).isTrue();
    assertThat(Files.readAllBytes(expectedPath)).isEqualTo(SBOM_CONTENT.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  public void testDeleteSbomEntity() throws Exception {
    // Create a SBOM entity
    Path sbomDir = insightWork.getSbomDir(APP_ID).toPath();
    Files.createDirectories(sbomDir);
    Path sbomPath = sbomDir.resolve(FILE_NAME);
    Files.write(sbomPath, SBOM_CONTENT.getBytes(StandardCharsets.UTF_8));
    FileSbomEntity sbomEntity = new FileSbomEntity(sbomPath, APP_ID, FILE_NAME);
    
    // Delete the SBOM entity
    service.deleteSbom(sbomEntity);
    
    // Verify the file was deleted
    assertThat(Files.exists(sbomPath)).isFalse();
  }

  @Test
  public void testDeleteSbomEntity_deletesEmptyParentDirectory() throws Exception {
    // Create a SBOM entity in a subdirectory
    Path sbomDir = insightWork.getSbomDir(APP_ID).toPath();
    Path subDir = sbomDir.resolve("subdir");
    Files.createDirectories(subDir);
    Path sbomPath = subDir.resolve(FILE_NAME);
    Files.write(sbomPath, SBOM_CONTENT.getBytes(StandardCharsets.UTF_8));
    FileSbomEntity sbomEntity = new FileSbomEntity(sbomPath, APP_ID, "subdir/" + FILE_NAME);
    
    // Delete the SBOM entity
    service.deleteSbom(sbomEntity);
    
    // Verify the file and parent directory were deleted
    assertThat(Files.exists(sbomPath)).isFalse();
    assertThat(Files.exists(subDir)).isFalse();
  }

  @Test
  public void testDeleteSbomEntity_nonExistentFile() throws Exception {
    // Create a SBOM entity
    Path sbomDir = insightWork.getSbomDir(APP_ID).toPath();
    Path sbomPath = sbomDir.resolve(FILE_NAME);
    FileSbomEntity sbomEntity = new FileSbomEntity(sbomPath, APP_ID, FILE_NAME);

    // Delete the SBOM entity
    service.deleteSbom(sbomEntity);

    // Verify that an error is not thrown and the file is still non-existent
    assertThat(Files.exists(sbomPath)).isFalse();
  }

  @Test
  public void testDeleteSbom() throws Exception {
    // Create a SBOM file
    Path sbomDir = insightWork.getSbomDir(APP_ID).toPath();
    Files.createDirectories(sbomDir);
    Path sbomPath = sbomDir.resolve(FILE_NAME);
    Files.write(sbomPath, SBOM_CONTENT.getBytes(StandardCharsets.UTF_8));
    
    // Delete the SBOM
    service.deleteSbom(APP_ID, FILE_NAME);
    
    // Verify the file was deleted
    assertThat(Files.exists(sbomPath)).isFalse();
  }

  @Test
  public void testDeleteSbomsFor() throws Exception {
    // Create a directory for the app's SBOMs
    File sbomDir = insightWork.getSbomDir(APP_ID, true);
    
    // Create a spy on the FileCleaner to verify it's called
    FileCleaner spyFileCleaner = spy(fileCleaner);
    FileSbomPersistenceService spyService = new FileSbomPersistenceService(insightWork, spyFileCleaner);
    
    // Delete all SBOMs for the app
    spyService.deleteSbomsFor(APP_ID);
    
    // Verify the FileCleaner was called with the correct directory
    verify(spyFileCleaner).delete(sbomDir);
  }

  @Test
  public void testDeleteTransientSbomsOlderThan() throws Exception {
    // Create directories for transient SBOMs
    Path transientDir = insightWork.getSbomTransientDir().toPath();
    Files.createDirectories(transientDir);
    
    // Create some old and new SBOM files
    Path oldSbomPath = transientDir.resolve("old-sbom.xml");
    Path newSbomPath = transientDir.resolve("new-sbom.xml");
    
    Files.write(oldSbomPath, "old content".getBytes(StandardCharsets.UTF_8));
    Files.write(newSbomPath, "new content".getBytes(StandardCharsets.UTF_8));
    
    // Set the old file to have a modification time of 2 days ago
    Files.setLastModifiedTime(oldSbomPath, FileTime.from(Instant.now().minus(2, ChronoUnit.DAYS)));
    
    // Set the threshold to 1 day ago
    Instant threshold = Instant.now().minus(1, ChronoUnit.DAYS);
    
    // Delete transient SBOMs older than the threshold
    service.deleteTransientSbomsOlderThan(threshold);
    
    // Verify only the old file was deleted
    assertThat(Files.exists(oldSbomPath)).isFalse();
    assertThat(Files.exists(newSbomPath)).isTrue();
  }

  @Test
  public void testMoveSbomEntity() throws Exception {
    Path sbomDir = insightWork.getSbomDir(APP_ID).toPath();
    Files.createDirectories(sbomDir);
    Path sbomPath = sbomDir.resolve(FILE_NAME);
    Files.write(sbomPath, SBOM_CONTENT.getBytes(StandardCharsets.UTF_8));
    SbomEntity from = service.getPermanentSbom(APP_ID, FILE_NAME);
    SbomEntity to = service.getPermanentSbom(APP_ID + "2", FILE_NAME);
    assertThat(from.exists()).isTrue();
    assertThat(to.exists()).isFalse();

    service.moveSbomEntity(from, to);

    assertThat(from.exists()).isFalse();
    assertThat(to.exists()).isTrue();
    try (InputStream inputStream = to.getInputStream()) {
      assertThat(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(SBOM_CONTENT);
    }
  }
}
