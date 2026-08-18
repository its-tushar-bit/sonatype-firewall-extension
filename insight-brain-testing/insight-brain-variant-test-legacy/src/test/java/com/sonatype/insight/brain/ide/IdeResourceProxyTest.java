/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import com.sonatype.insight.brain.variant.LegacyServerTest;

/**
 * Re-runs the full {@link IdeResourceTest} suite with the HDS mock server reachable only via a proxy (selected by the
 * {@code *ProxyTest} class-name suffix), exercising the HDS-proxy code path.
 */
@LegacyServerTest
public class IdeResourceProxyTest
    extends IdeResourceTest
{
}
