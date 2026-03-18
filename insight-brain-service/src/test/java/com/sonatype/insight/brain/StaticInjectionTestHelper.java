/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import com.sonatype.insight.brain.api.v2.SystemConfigurationPropertyFeatureTestHelper;
import com.sonatype.insight.brain.api.v2.service.ConfigurationUtilsTestHelper;
import com.sonatype.insight.brain.dataaccess.ConditionTypesTestHelper;
import com.sonatype.insight.brain.dataaccess.DAOFactory;
import com.sonatype.insight.brain.hds.ComponentDetailsLoaderTestHelper;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.testing.AbstractBaseIntegrationTest;

/**
 * <p>
 * Helper to manage special static injection cases for tests.
 * </p>
 *
 * <p>
 * Background: In late 2023 the database layer received a large overhaul
 * (<a href="https://sonatype.atlassian.net/browse/CLM-26741">see CLM-26741</a>)
 * with the goal to make the database classes easier to use and extend for the future. Two of the primary goals were to
 * get the DAO classes managed by Guice, and to remove some of the heavy use of statics in the code (statics are by
 * their nature not easily extensible). As part of the effort to remove statics some areas usage was quite extensive
 * and a refactor would have implied a larger change that was out of scope at the time. For those cases it was decided
 * to use Guice <a href="https://github.com/google/guice/wiki/Injections#static-injections">static-injection</a>.
 * <p>
 * <p>
 * At runtime Guice will handle all static injection using the framework feature `requestStaticInjection`. However, for
 * tests that do not leverage Guice the injection must be handled manually. This is where this helper class is meant
 * to assist. It is intended to be used from the various base test classes to ensure consistency. Also note that these
 * injections need to be re-executed for each test as (due to the nature of statics) they may get overwritten from test
 * to test. For example, test order is random and it could execute an integration test (those extending
 * {@link AbstractBaseIntegrationTest}) followed by a component test (those extending {@link AbstractComponentTest},
 * followed by another integration test.
 */
public class StaticInjectionTestHelper
{
  public static void inject(final DAOFactory daoFactory) {
    ConditionTypesTestHelper.initConditionTypes(daoFactory);
    ConditionTypesTestHelper.initConditionValueTypes(daoFactory);
    ComponentDetailsLoaderTestHelper.inject(daoFactory);
    SystemConfigurationPropertyFeatureTestHelper.inject(daoFactory);
    ConfigurationUtilsTestHelper.inject(daoFactory);
  }
}
