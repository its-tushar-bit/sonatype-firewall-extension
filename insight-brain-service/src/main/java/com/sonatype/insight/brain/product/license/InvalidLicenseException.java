package com.sonatype.insight.brain.product.license;

import com.sonatype.insight.error.HttpStatusCode;

/**
 * Create an HTTP 402 (Payment Required) exception
 */
@HttpStatusCode( 402 )
public class InvalidLicenseException
    extends RuntimeException
{
    private static final long serialVersionUID = 1308434983601088106L;

    public InvalidLicenseException( String msg )
    {
        super( msg );
    }
}