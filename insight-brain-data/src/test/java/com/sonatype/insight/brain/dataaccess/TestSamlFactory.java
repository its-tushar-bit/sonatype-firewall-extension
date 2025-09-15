/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlConfigurationAdapter;
import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlPasswordFactory;
import com.sonatype.insight.brain.security.FIPSModeDetector;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.security.TestEncryptionKeyStore;
import com.sonatype.insight.brain.security.TestFipsEncryptionKeyStore;

public class TestSamlFactory
    implements SamlFactory
{
  @Override
  public SamlPasswordFactory createSamlPasswordFactory() {
    return new SamlPasswordFactory(createPasswordHandler());
  }

  @Override
  public PasswordHandler createPasswordHandler() {
    return new PasswordHandler(() -> {
      if (FIPSModeDetector.isEnabled()) {
        return new TestFipsEncryptionKeyStore().getKey();
      }
      else {
        return new TestEncryptionKeyStore().getKey();
      }
    });
  }

  @Override
  public SamlConfigurationAdapter createSamlConfigurationAdapter() {
    return new SamlConfigurationAdapter(createSamlPasswordFactory());
  }
}
