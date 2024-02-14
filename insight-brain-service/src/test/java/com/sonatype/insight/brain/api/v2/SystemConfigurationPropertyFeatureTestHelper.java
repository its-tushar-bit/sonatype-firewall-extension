/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.dataaccess.DAOFactory;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

/**
 * {@link SystemConfigurationPropertyFeature} uses static references. At runtime Guice
 * <a href="https://github.com/google/guice/wiki/Injections#static-injections">static-injection</a> is used to
 * populate the references. At test time we use this helper class.
 */
public class SystemConfigurationPropertyFeatureTestHelper
{
  public static void inject(final DAOFactory daoFactory) {
    SystemConfigurationPropertyDAO systemConfigurationPropertyDAO = daoFactory.createSystemConfigurationPropertyDAO();
    SystemConfigurationPropertyFeature.injectDependencies(systemConfigurationPropertyDAO);
  }
}
