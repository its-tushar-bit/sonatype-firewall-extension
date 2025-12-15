/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.modules;

import com.sonatype.insight.brain.security.DefaultEncryptionKeyStore;
import com.sonatype.insight.brain.security.EncryptionKeyStore;
import com.sonatype.insight.brain.security.SsoUserService;
import com.sonatype.insight.brain.security.UserDirectory;
import com.sonatype.insight.brain.security.UserService;

import com.google.inject.AbstractModule;

/**
 * Guice module providing explicit bindings for auth classes that only apply to IQ and not Multi Tenant IQ
 */
public class IqOnlyAuthModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    // IQ-specific authentication bindings - only the bindings that differ from MTIQ
    bind(UserDirectory.class);
    bind(SsoUserService.class);
    bind(UserService.class);
    bind(EncryptionKeyStore.class).to(DefaultEncryptionKeyStore.class);
  }
}
