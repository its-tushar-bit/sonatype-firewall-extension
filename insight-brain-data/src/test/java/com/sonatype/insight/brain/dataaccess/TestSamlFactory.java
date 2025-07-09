/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlPasswordFactory;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.security.keystore.KeyStoreFactory;

public class TestSamlFactory
    implements SamlFactory
{
  @Override
  public SamlPasswordFactory createSamlPasswordFactory() {
    return new SamlPasswordFactory(createPasswordHandler());
  }

  @Override
  public PasswordHandler createPasswordHandler() {
    return new PasswordHandler(KeyStoreFactory::getDefaultEncryptionKeyStoreKey);
  }

  @Override
  public SamlConfigurationService createSamlConfigurationService() {
    return new SamlConfigurationService(createSamlPasswordFactory());
  }
}
