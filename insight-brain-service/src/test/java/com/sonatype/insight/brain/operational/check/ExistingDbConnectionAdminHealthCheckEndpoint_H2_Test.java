/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;

import org.junit.experimental.categories.Category;

@H2DiskTest
@SuppressWarnings("checkstyle:TypeName")
@Category(SlowTest.class)
public class ExistingDbConnectionAdminHealthCheckEndpoint_H2_Test
    extends AbstractExistingDbConnectionAdminHealthCheckEndpointTest
{
  // All tests are in the super class
}
