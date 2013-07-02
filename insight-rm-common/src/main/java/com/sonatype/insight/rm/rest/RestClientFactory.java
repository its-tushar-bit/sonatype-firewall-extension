/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.rm.rest;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.http.client.HttpResponseException;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.client.ConfigurationClient;
import com.sonatype.insight.brain.client.PolicyClient;
import com.sonatype.insight.brain.client.ScanClient;
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

    ConfigurationClient newConfigurationClient( final Configuration config )
    {
        return new ConfigurationClient( config );
    }

    ScanClient newScanClient( final Configuration config, final String appId )
    {
        return new ScanClient( config, appId );
    }

    private class BaseClient
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
            try
            {
                newConfigurationClient( config ).validateConfiguration();
            }
            catch ( IOException e )
            {
                throw handleError( e );
            }
        }

        @Override
        public Map<String, String> getApplications()
            throws IOException
        {
            try
            {
                return newConfigurationClient( config ).getApplicationIdNameMap();
            }
            catch ( IOException e )
            {
                throw handleError( e );
            }
        }

        @Override
        public ProprietaryConfig getProprietaryConfiguration()
            throws IOException
        {
            try
            {
                return newConfigurationClient( config ).getProprietaryConfiguration();
            }
            catch ( IOException e )
            {
                throw handleError( e );
            }
        }

        @Override
        public App forApplication( final String appId )
        {
            return new AppSpecificClient( config, appId );
        }

        protected IOException handleError( IOException e )
        {
            if ( e instanceof HttpResponseException )
            {
                HttpResponseException re = (HttpResponseException) e;
                return new HttpException( re.getStatusCode(), re.getMessage(), re );
            }
            return e;
        }

    }

    private class AppSpecificClient
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
            try
            {
                newConfigurationClient( config ).validateApplicationId( appId );
            }
            catch ( IOException e )
            {
                throw handleError( e );
            }
        }

        @Override
        public ScanReceipt uploadScan( File scanFile )
            throws IOException
        {
            try
            {
                return newScanClient( config, appId ).uploadRepoManScan( scanFile );
            }
            catch ( IOException e )
            {
                throw handleError( e );
            }
        }

        @Override
        public Scan forScan( String scanId )
        {
            return new ScanSpecificClient( config, appId, scanId );
        }

    }

    private class ScanSpecificClient
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
        public PolicyEvaluationResult evaluatePolicies( com.sonatype.insight.rm.rest.Stage stage )
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
                    st = new Stage( Stage.ID_STAGE_RELEASE );
                    break;
                case RELEASE_REPOSITORY:
                    st = new Stage( Stage.ID_RELEASE );
                    break;
                default:
                    throw new IllegalStateException( "unsupported stage " + stage );
            }
            return new PolicyClient( config, appId ).evaluate( scanId, st );
        }

    }

}
