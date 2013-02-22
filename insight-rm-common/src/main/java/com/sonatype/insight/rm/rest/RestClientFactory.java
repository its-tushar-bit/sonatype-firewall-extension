/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.rm.rest;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.client.PolicyClient;
import com.sonatype.insight.brain.client.ScanClient;
import com.sonatype.insight.brain.client.ValidationClient;
import com.sonatype.insight.brain.model.policy.Stage;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageReleaseStageType;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.rm.rest.RestClient.App;
import com.sonatype.insight.rm.rest.RestClient.Scan;

public class RestClientFactory
{

    public RestClient.Base forConfiguration( final RestClientConfiguration config )
    {
        if ( config == null )
        {
            throw new IllegalArgumentException( "REST client configuration missing" );
        }
        return new BaseClient( config.getConfig() );
    }

    private static class BaseClient
        implements RestClient.Base
    {

        protected final Configuration config;

        public BaseClient( final Configuration config )
        {
            this.config = config;
        }

        @Override
        public void validateConfiguration()
            throws IOException
        {
            new ValidationClient( config ).validateConfiguration();
        }

        @Override
        public App forApplication( final String appId )
        {
            return new AppSpecificClient( config, appId );
        }

    }

    private static class AppSpecificClient
        extends BaseClient
        implements RestClient.App
    {

        protected final String appId;

        public AppSpecificClient( final Configuration config, final String appId )
        {
            super( config );
            this.appId = appId;
        }

        @Override
        public void validateApplicationId()
            throws IOException
        {
            new ValidationClient( config ).validateApplicationId( appId );
        }

        @Override
        public ScanReceipt uploadScan( File scanFile )
            throws IOException
        {
            return new ScanClient( config, appId ).uploadRepoManScan( scanFile );
        }

        @Override
        public Scan forScan( String scanId )
        {
            return new ScanSpecificClient( config, appId, scanId );
        }

    }

    private static class ScanSpecificClient
        extends AppSpecificClient
        implements RestClient.Scan
    {

        protected final String scanId;

        public ScanSpecificClient( final Configuration config, final String appId, final String scanId )
        {
            super( config, appId );
            this.scanId = scanId;
        }

        @Override
        public List<PolicyAlert> evaluatePolicies( com.sonatype.insight.rm.rest.Stage stage )
            throws IOException
        {
            if ( stage == null )
            {
                throw new IllegalArgumentException( "stage missing" );
            }
            Stage st;
            switch ( stage )
            {
                case CLOSE_REPOSITORY:
                    st = new Stage( StageReleaseStageType.ID );
                    break;
                case RELEASE_REPOSITORY:
                    st = new Stage( ReleaseStageType.ID );
                    break;
                default:
                    throw new IllegalStateException( "unsupported stage " + stage );
            }
            return new PolicyClient( config, appId ).evaluate( scanId, st );
        }

    }

}
