/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.File;
import java.util.Base64;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.sonatype.insight.brain.security.FipsTestUtil.insertBouncyCastleFipsProvider;
import static com.sonatype.insight.brain.security.FipsTestUtil.removeBouncyCastleFipsProvider;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class FIPSKeystorePasswordGeneratorTest
{
  @TempDir
  public java.nio.file.Path temporaryFolder;

  @BeforeEach
  public void setUp() {
    insertBouncyCastleFipsProvider();
  }

  @AfterEach
  public void tearDown() {
    removeBouncyCastleFipsProvider();
  }

  @Test
  public void testGenerateDeterministicPassword_returnsValidBase64() throws Exception {
    File workDir = java.nio.file.Files.createDirectories(temporaryFolder.resolve("sonatype-work")).toFile();

    String password = FIPSKeystorePasswordGenerator.generateDeterministicPassword(workDir);

    // Base64 encoded 256-bit key should be 44 characters (32 bytes * 4/3 + padding)
    assertThat(password).hasSize(44);

    // Verify it's valid Base64 by decoding it (throws exception if invalid)
    assertThatCode(() -> Base64.getDecoder().decode(password))
        .doesNotThrowAnyException();

    // Verify decoded length is 32 bytes (256 bits)
    byte[] decoded = Base64.getDecoder().decode(password);
    assertThat(decoded).hasSize(32);
  }

  @Test
  public void testGenerateDeterministicPassword_differentDirectoriesProduceDifferentPasswords() throws Exception {
    File workDir1 = java.nio.file.Files.createDirectories(temporaryFolder.resolve("sonatype-work-1")).toFile();
    File workDir2 = java.nio.file.Files.createDirectories(temporaryFolder.resolve("sonatype-work-2")).toFile();

    String password1 = FIPSKeystorePasswordGenerator.generateDeterministicPassword(workDir1);
    String password2 = FIPSKeystorePasswordGenerator.generateDeterministicPassword(workDir2);

    assertThat(password1).isNotEqualTo(password2);
  }

  @Test
  public void testGenerateDeterministicPassword_handlesNonExistentDirectory() throws Exception {
    File nonExistentDir = new File(temporaryFolder.toFile(), "non-existent");

    // Should not throw exception, should handle gracefully
    String password = FIPSKeystorePasswordGenerator.generateDeterministicPassword(nonExistentDir);

    assertThat(password).isNotEmpty();
  }

  @Test
  public void testGenerateDeterministicPassword_handlesSpecialCharactersInPath() throws Exception {
    // Create directory with spaces and special characters
    File specialDir = java.nio.file.Files.createDirectories(temporaryFolder.resolve("sonatype work & test")).toFile();

    String password = FIPSKeystorePasswordGenerator.generateDeterministicPassword(specialDir);

    assertThat(password).isNotEmpty();
  }

  @Test
  public void testGenerateDeterministicPassword_usesAbsolutePath() throws Exception {
    File workDir = java.nio.file.Files.createDirectories(temporaryFolder.resolve("sonatype-work")).toFile();

    // Test that same directory accessed different ways produces same password
    File sameDirDifferentWay = new File(workDir.getAbsolutePath());

    String password1 = FIPSKeystorePasswordGenerator.generateDeterministicPassword(workDir);
    String password2 = FIPSKeystorePasswordGenerator.generateDeterministicPassword(sameDirDifferentWay);

    assertThat(password1).isEqualTo(password2);
  }

  @Test
  public void testGenerateDeterministicPassword_returnsSamePassword() throws Exception {
    File workDir = java.nio.file.Files.createDirectories(temporaryFolder.resolve("sonatype-work")).toFile();

    // Test multiple calls to ensure stability
    String password1 = FIPSKeystorePasswordGenerator.generateDeterministicPassword(workDir);
    String password2 = FIPSKeystorePasswordGenerator.generateDeterministicPassword(workDir);
    String password3 = FIPSKeystorePasswordGenerator.generateDeterministicPassword(workDir);

    assertThat(password1).isEqualTo(password2);
    assertThat(password2).isEqualTo(password3);
    assertThat(password1).isNotEmpty();
  }
}
