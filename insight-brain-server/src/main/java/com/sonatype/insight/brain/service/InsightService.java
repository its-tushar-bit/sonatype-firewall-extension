package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.data.DataResource;
import com.sonatype.insight.brain.legacy.BCResource;
import com.sonatype.insight.brain.report.ReportResource;
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
        env.addResource( ReportResource.class );
        env.addResource( BCResource.class );
    }
}
