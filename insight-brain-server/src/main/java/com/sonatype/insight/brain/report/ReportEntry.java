/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

public final class ReportEntry
{
    public final String name;

    public final long time;

    public final byte[] buf;

    ReportEntry( final String name, final long time, final byte[] buf )
    {
        this.name = name;
        this.time = time;
        this.buf = buf;
    }
}
