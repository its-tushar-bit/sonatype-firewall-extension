/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;

/**
 * This base class is intended to be used when you need a full e2e integration test from the REST endpoint to the DB.
 * Check docs in {@link AbstractBrainServiceIntegrationTest}
 */
public abstract class AbstractResourceTest
    extends AbstractBrainServiceIntegrationTest
{
}
