package com.sonatype.insight.brain.model;

import com.sonatype.insight.error.HttpStatusCode;

@SuppressWarnings( "serial" )
@HttpStatusCode( 400 )
public class InvalidNameException
    extends RuntimeException
{
    public InvalidNameException( String message )
    {
        super( message );
    }
}
