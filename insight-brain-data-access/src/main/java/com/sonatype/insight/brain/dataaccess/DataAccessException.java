package com.sonatype.insight.brain.dataaccess;

public class DataAccessException
    extends RuntimeException
{
    private static final long serialVersionUID = 707805649518539904L;

    public DataAccessException( String message )
    {
        super( message );
    }

    public DataAccessException( Throwable cause )
    {
        super( cause );
    }
}
