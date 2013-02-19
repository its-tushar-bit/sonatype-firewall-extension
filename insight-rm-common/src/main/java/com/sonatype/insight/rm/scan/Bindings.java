/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.rm.scan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.scan.anon.DefaultAnonymizer;
import com.sonatype.insight.scan.client.ClientScanner;
import com.sonatype.insight.scan.client.DefaultClientScanner;
import com.sonatype.insight.scan.file.DefaultFileScanner;
import com.sonatype.insight.scan.file.FileScanner;
import com.sonatype.insight.scan.hash.internal.DefaultDigester;
import com.sonatype.insight.scan.hash.internal.JavaDigester;
import com.sonatype.insight.scan.model.io.DefaultScanWriterFactory;
import com.sonatype.insight.scan.model.io.ScanWriter;
import com.sonatype.insight.scan.model.io.ScanWriterFactory;

final class Bindings
{
    private static Logger logger( Class<?> type )
    {
        return LoggerFactory.getLogger( type );
    }

    static ScanWriterFactory scanWriterFactory()
    {
        return new DefaultScanWriterFactory( logger( ScanWriter.class ) );
    }

    static ClientScanner clientScanner()
    {
        return new DefaultClientScanner( logger( DefaultClientScanner.class ) );
    }

    static FileScanner fileScanner()
    {
        return new DefaultFileScanner( new DefaultDigester( new JavaDigester(), logger( DefaultDigester.class ) ),
                                       new DefaultAnonymizer(), logger( DefaultFileScanner.class ) );
    }
}
