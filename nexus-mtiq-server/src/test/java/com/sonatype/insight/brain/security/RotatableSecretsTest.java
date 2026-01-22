/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Set;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RotatableSecretsTest
    extends AbstractComponentTest
{
  @Inject
  private Set<RotatableSecrets> rotatableSecrets;

  @Test
  public void testRotatableSecretsInterface_extendAbstractOperationalSqlDAO() {
    for (RotatableSecrets rotatableSecret : rotatableSecrets) {
      // RotatableSecrets must inherit from AbstractOperationalSqlDAO to be able to rotate secrets using the
      // DAOSecretRotator, however the methods are protected so we cant enforce the methods with the interface.
      assertThat(rotatableSecret).isInstanceOf(AbstractOperationalSqlDAO.class);
    }
  }
}
