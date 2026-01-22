/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.service.Configuration;

@Named
@Singleton
public class InvalidRequestFilter
    extends org.apache.shiro.web.filter.InvalidRequestFilter
{
  private final Configuration configuration;

  @Inject
  public InvalidRequestFilter(Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  public boolean isBlockSemicolon() {
    return configuration.isBlockSemicolon();
  }

  @Override
  public void setBlockSemicolon(boolean blockSemicolon) {
    configuration.setBlockSemicolon(blockSemicolon);
  }

  @Override
  public boolean isBlockBackslash() {
    return configuration.isBlockBackslash();
  }

  @Override
  public void setBlockBackslash(boolean blockBackslash) {
    configuration.setBlockBackslash(blockBackslash);
  }

  @Override
  public boolean isBlockNonAscii() {
    return configuration.isBlockNonAscii();
  }

  @Override
  public void setBlockNonAscii(boolean blockNonAscii) {
    configuration.setBlockNonAscii(blockNonAscii);
  }
}
