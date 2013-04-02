package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.error.HttpStatusCode;

@SuppressWarnings( "serial" )
@HttpStatusCode( 400 )
public class InvalidApplicationProfileException
    extends RuntimeException
{
    public InvalidApplicationProfileException( String message )
    {
        super( message );
    }
}
