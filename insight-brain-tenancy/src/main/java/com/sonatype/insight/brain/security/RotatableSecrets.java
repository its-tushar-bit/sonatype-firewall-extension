/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.sql.SQLException;
import java.util.function.Function;

/**
 * Interface representing a component that contains secrets which can be rotated.
 * Implementations of this interface should provide the logic to rotate their encrypted secrets
 * using a provided secret rotator function.
 */
public interface RotatableSecrets
{
  /**
   * Rotates the encrypted secrets using the provided secret rotator function.
   *
   * @param secretRotator a function that takes an encrypted secret and returns the rotated secret
   * @throws SQLException if an SQL error occurs during the rotation process
   */
  void rotateEncryptedSecrets(Function<String, String> secretRotator) throws SQLException;
}
