/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.sonatype.insight.brain.dataaccess.DAOFactory;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;

/**
 * {@link ConfigurationUtils} uses static references. At runtime Guice
 * <a href="https://github.com/google/guice/wiki/Injections#static-injections">static-injection</a> is used to
 * populate the references. At test time we use this helper class.
 */
public class ConfigurationUtilsTestHelper
{
  public static void inject(final DAOFactory daoFactory) {
    SystemConfigurationPropertyDAO systemConfigurationPropertyDAO = daoFactory.createSystemConfigurationPropertyDAO();
    ConfigurationUtils.injectDependencies(systemConfigurationPropertyDAO);
  }
}
