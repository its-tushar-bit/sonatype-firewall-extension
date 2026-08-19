/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

/**
 * Interface representing a DAO component that contains secrets which can be rotated.
 * The DAOs that implement this interface must extend the AbstractOperationalSqlDAO this will provide the logic to
 * rotate their encrypted secrets using the DAOSecretRotator and a provided secret rotator function.
 */
public interface RotatableSecrets
{
}
