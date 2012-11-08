/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.eclipse.birt.report.engine.internal.util;

/**
 * Patched copy which avoids the below exception when BIRT runs within Hudson/Jenkins whose plugin class loader
 * generates {@code CodeSource} objects with {@code null} fields which BIRT doesn't tolerate.
 * 
 * <pre>
 * Caused by: java.lang.NullPointerException
 *   at org.eclipse.birt.report.engine.internal.util.BundleVersionUtil.getBundleVersion(BundleVersionUtil.java:49)
 *   at org.eclipse.birt.report.engine.emitter.pdf.PDFPageDevice.(PDFPageDevice.java:71)
 * </pre>
 * 
 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=390832
 */
public class BundleVersionUtil
{

    private static String UNKNOWN_VERSION = "UNKNOWN";

    public static String getBundleVersion( final String bundleName )
    {
        return UNKNOWN_VERSION;
    }

}
