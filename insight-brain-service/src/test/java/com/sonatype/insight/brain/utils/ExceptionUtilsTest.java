/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.RollbackException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ExceptionUtilsTest
{
  @Test
  public void testIsDuplicateKeyException_EntityExistsException() {
    Exception e = new EntityExistsException("Duplicate key");

    boolean result = ExceptionUtils.isDuplicateKeyException(e);

    assertThat(result).isTrue();
  }

  @Test
  public void testIsDuplicateKeyException_RollbackExceptionWrappingEntityExistsException() {
    EntityExistsException cause = new EntityExistsException("Duplicate key");
    Exception e = new RollbackException("Transaction rolled back", cause);

    boolean result = ExceptionUtils.isDuplicateKeyException(e);

    assertThat(result).isTrue();
  }

  @Test
  public void testIsDuplicateKeyException_RollbackExceptionWithDifferentCause() {
    PersistenceException cause = new PersistenceException("Some other error");
    Exception e = new RollbackException("Transaction rolled back", cause);

    boolean result = ExceptionUtils.isDuplicateKeyException(e);

    assertThat(result).isFalse();
  }

  @Test
  public void testIsDuplicateKeyException_RollbackExceptionWithNullCause() {
    Exception e = new RollbackException("Transaction rolled back");

    boolean result = ExceptionUtils.isDuplicateKeyException(e);

    assertThat(result).isFalse();
  }

  @Test
  public void testIsDuplicateKeyException_OtherException() {
    Exception e = new RuntimeException("Some error");

    boolean result = ExceptionUtils.isDuplicateKeyException(e);

    assertThat(result).isFalse();
  }

  @Test
  public void testIsDuplicateKeyException_PersistenceException() {
    Exception e = new PersistenceException("Persistence error");

    boolean result = ExceptionUtils.isDuplicateKeyException(e);

    assertThat(result).isFalse();
  }
}
