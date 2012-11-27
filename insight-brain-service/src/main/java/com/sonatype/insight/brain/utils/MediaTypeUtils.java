/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil2;
import eu.medsea.mimeutil.detector.ExtensionMimeDetector;

/**
 * Various utility methods for detecting media types.
 */
public final class MediaTypeUtils
{
    private static final MimeUtil2 mimeUtil = new MimeUtil2();

    static
    {
        mimeUtil.registerMimeDetector( ExtensionMimeDetector.class.getName() );
    }

    private MediaTypeUtils()
    {
        // utility class
    }

    /**
     * Attempts to detect the media type based on the given name of the resource.
     * 
     * @param name The resource name
     * @return Detected media type
     */
    public static String byName( final String name )
    {
        final MimeType mimeType = MimeUtil2.getMostSpecificMimeType( mimeUtil.getMimeTypes( name ) );
        if ( mimeType != null )
        {
            final String mediaType = mimeType.toString(); // returns "<mediaType>/<subType>"
            return mediaType.startsWith( "text" ) ? mediaType + ";charset=UTF-8" : mediaType;
        }
        return null;
    }
}
