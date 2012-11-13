package com.sonatype.insight.clm.service;

import com.sonatype.insight.clm.data.DataResource;
import com.sonatype.insight.clm.legacy.BCResource;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Environment;

public class InsightService
    extends Service<InsightConfiguration>
{
    public static void main( final String[] args )
        throws Exception
    {
        new InsightService().run( args );
    }

    @Override
    protected void initialize( final InsightConfiguration config, final Environment env )
        throws Exception
    {
        env.addProvider( new InsightWork( config ) );
        env.addProvider( new InsightProxy( config ) );

        env.addResource( DataResource.class );
        env.addResource( BCResource.class );
    }
}
