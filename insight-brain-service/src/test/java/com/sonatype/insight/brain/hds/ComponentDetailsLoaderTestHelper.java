/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import com.sonatype.insight.brain.dataaccess.DAOFactory;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;

/**
 * {@link ComponentDetailsLoader} uses static references. At runtime Guice
 * <a href="https://github.com/google/guice/wiki/Injections#static-injections">static-injection</a> is used to
 * populate the references. At test time we use this helper class.
 */
public class ComponentDetailsLoaderTestHelper
{
  public static void inject(final DAOFactory daoFactory) {
    HashComponentIdentifierDAO hashComponentIdentifierDAO = daoFactory.createHashComponentIdentifierDAO();
    MultiLicenseDAO multiLicenseDAO = daoFactory.createMultiLicenseDAO();
    ComponentDetailsLoader.inject(hashComponentIdentifierDAO, multiLicenseDAO);
  }
}
