package com.sonatype.insight.brain.product.license;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;

/**
 * Create an HTTP 402 (Payment Required) exception
 */
public class InvalidLicenseException
    extends WebApplicationException
{
    private static final long serialVersionUID = 1308434983601088106L;

    private static StatusType PAYMENT_REQUIRED_TYPE = new StatusType()
    {
        @Override
        public int getStatusCode()
        {
            return 402;
        }

        @Override
        public Family getFamily()
        {
            return Family.SERVER_ERROR;
        }

        @Override
        public String getReasonPhrase()
        {
            return "Payment Required";
        }
    };

    public InvalidLicenseException()
    {
        super( Response.status( PAYMENT_REQUIRED_TYPE ).build() );
    }

    public InvalidLicenseException( String msg )
    {
        super( Response.status( PAYMENT_REQUIRED_TYPE ).entity( msg ).type( "text/plain" ).build() );
    }
}