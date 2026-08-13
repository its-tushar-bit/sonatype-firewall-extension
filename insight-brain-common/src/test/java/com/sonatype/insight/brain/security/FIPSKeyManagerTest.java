/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_MODE_ENABLED_ENV;
import static com.sonatype.insight.brain.security.FipsTestUtil.insertBouncyCastleFipsProvider;
import static com.sonatype.insight.brain.security.FipsTestUtil.removeBouncyCastleFipsProvider;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FIPSKeyManagerTest
{
  private final TestEnvironmentVariables environmentVariables = new TestEnvironmentVariables();

  private File tempDirectory;

  private FIPSKeyManager keyManager;

  @BeforeEach
  public void setUp() throws IOException {
    insertBouncyCastleFipsProvider();
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");
    tempDirectory = Files.createTempDirectory("fips-key-manager-test").toFile();
    keyManager = new FIPSKeyManager(tempDirectory);
  }

  @AfterEach
  public void restoreEnvironmentVariables() {
    environmentVariables.restore();
  }

  @AfterEach
  public void tearDown() {
    if (tempDirectory != null && tempDirectory.exists()) {
      deleteDirectory(tempDirectory);
    }
    removeBouncyCastleFipsProvider();
  }

  @Test
  public void testGetOrGenerateKey_WhenKeystoreDoesNotExist_GeneratesNewKey() throws Exception {
    String key = keyManager.getOrGenerateKey();
    File keystoreFile = new File(tempDirectory, "fips-encryption.keystore");

    assertThat(keystoreFile).exists().isFile();
    assertThat(key).hasSize(32)
        .matches(k -> k.chars().allMatch(c -> c >= 33 && c <= 126));
  }

  @Test
  public void testGetOrGenerateKey_GeneratesUniqueKeys() throws Exception {
    String firstKey = keyManager.getOrGenerateKey();

    File secondTempDir = Files.createTempDirectory("fips-key-manager-test-2").toFile();
    try {
      FIPSKeyManager secondManager = new FIPSKeyManager(secondTempDir);
      String secondKey = secondManager.getOrGenerateKey();

      assertThat(secondKey).isNotEqualTo(firstKey);
    }
    finally {
      deleteDirectory(secondTempDir);
    }
  }

  @Test
  public void testGetOrGenerateKey_RetrievesExistingKey() throws Exception {
    String originalKey = keyManager.getOrGenerateKey();

    // Test that subsequent calls return the same key
    String secondCall = keyManager.getOrGenerateKey();
    assertThat(secondCall).isEqualTo(originalKey);

    // Test that new manager instance retrieves the same key
    FIPSKeyManager secondManager = new FIPSKeyManager(tempDirectory);
    String retrievedKey = secondManager.getOrGenerateKey();
    assertThat(retrievedKey).isEqualTo(originalKey);
  }

  @Test
  public void testGetOrGenerateKey_ReadOnlyDirectory_ThrowsException() throws Exception {
    File readOnlyDir = Files.createTempDirectory("fips-readonly-test").toFile();
    try {
      readOnlyDir.setWritable(false);

      FIPSKeyManager readOnlyManager = new FIPSKeyManager(readOnlyDir);

      assertThatThrownBy(readOnlyManager::getOrGenerateKey)
          .isInstanceOf(FIPSKeyManager.FIPSKeyException.class)
          .hasMessageContaining("Failed to generate and store FIPS encryption key");
    }
    finally {
      readOnlyDir.setWritable(true);
      deleteDirectory(readOnlyDir);
    }
  }

  @Test
  public void testGetOrGenerateKey_CorruptedKeystoreFile_ThrowsException() throws Exception {
    File keystoreFile = new File(tempDirectory, "fips-encryption.keystore");
    Files.write(keystoreFile.toPath(), "corrupted keystore content".getBytes());

    assertThatThrownBy(() -> keyManager.getOrGenerateKey())
        .isInstanceOf(FIPSKeyManager.FIPSKeyException.class)
        .hasMessageContaining("Failed to retrieve FIPS encryption key from keystore");
  }

  @Test
  public void testConstructor_DirectoryCreationFailure_ThrowsException() throws Exception {
    // Create a file where the directory should be to simulate creation failure
    File conflictingFile = new File(tempDirectory, "subdir");
    Files.write(conflictingFile.toPath(), "blocking file".getBytes());

    File dir = new File(conflictingFile, "nested");

    assertThatThrownBy(() -> new FIPSKeyManager(dir))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to create keystore directory");
  }

  @Test
  public void testGetOrGenerateKey_SetsPosixFilePermissions() throws Exception {
    try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class, Mockito.CALLS_REAL_METHODS)) {
      FileStore mockFileStore = mock(FileStore.class);
      when(mockFileStore.supportsFileAttributeView("posix")).thenReturn(true);
      filesMock.when(() -> Files.getFileStore(any(Path.class))).thenReturn(mockFileStore);

      Set<PosixFilePermission> expectedPermissions = EnumSet.of(
          PosixFilePermission.OWNER_READ,
          PosixFilePermission.OWNER_WRITE);
      filesMock.when(() -> Files.getPosixFilePermissions(any(Path.class)))
          .thenReturn(expectedPermissions);

      keyManager.getOrGenerateKey();

      File keystoreFile = new File(tempDirectory, "fips-encryption.keystore");
      assertThat(keystoreFile).exists();

      Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(keystoreFile.toPath());
      assertThat(permissions).isEqualTo(expectedPermissions);
      filesMock.verify(() -> Files.setPosixFilePermissions(any(Path.class), eq(expectedPermissions)));
    }
  }

  @Test
  public void testGetOrGenerateKey_SetsWindowsAclWhenPosixUnsupported() throws Exception {
    try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class, Mockito.CALLS_REAL_METHODS)) {
      // Mock setPosixFilePermissions to throw UnsupportedOperationException to force Windows ACL path
      filesMock.when(() -> Files.setPosixFilePermissions(any(Path.class), any(Set.class)))
          .thenThrow(new UnsupportedOperationException("POSIX not supported"));

      AclFileAttributeView mockAclView = mock(AclFileAttributeView.class);
      UserPrincipal mockOwner = mock(UserPrincipal.class);

      filesMock.when(() -> Files.getFileAttributeView(any(Path.class), eq(AclFileAttributeView.class)))
          .thenReturn(mockAclView);
      filesMock.when(() -> Files.getOwner(any(Path.class))).thenReturn(mockOwner);

      String key = keyManager.getOrGenerateKey();

      File keystoreFile = new File(tempDirectory, "fips-encryption.keystore");
      assertThat(keystoreFile).exists();
      assertThat(keystoreFile).canRead().canWrite();
      assertThat(key).hasSize(32).matches(k -> k.chars().allMatch(c -> c >= 33 && c <= 126));

      // Verify POSIX was attempted first
      filesMock.verify(() -> Files.setPosixFilePermissions(any(Path.class), any(Set.class)));

      // Verify Windows ACL operations were called
      filesMock.verify(() -> Files.getFileAttributeView(any(Path.class), eq(AclFileAttributeView.class)));
      filesMock.verify(() -> Files.getOwner(any(Path.class)));
      Mockito.verify(mockAclView).setAcl(any(List.class));
    }
  }

  private void deleteDirectory(final File directory) {
    if (!directory.exists()) {
      return;
    }
    try (var paths = Files.walk(directory.toPath())) {
      paths.sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(File::delete);
    }
    catch (IOException e) {
      throw new RuntimeException("Failed to delete directory: " + directory, e);
    }
  }
}
