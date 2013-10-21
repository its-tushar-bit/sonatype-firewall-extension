/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ErrorResponseGeneratorTest
{

  private ErrorResponseGenerator generator = new ErrorResponseGenerator();

  @Test
  public void testGetStatusCode_HandleShiroExceptions() {
    assertThat(generator.getStatusCode(new UnauthorizedException()), is(403));
    assertThat(generator.getStatusCode(new UnauthenticatedException()), is(401));
  }

}
