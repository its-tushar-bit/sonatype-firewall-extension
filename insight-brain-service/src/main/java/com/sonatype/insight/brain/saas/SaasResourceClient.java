/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.plexus.util.StringUtils;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.insight.brain.model.component.MavenCoordinates;

/**
 * A resource oriented SaaS client, as opposed to the HTTP based {@link SaasClient}.
 * 
 * TODO Should review the approach used for insight-brian-client. 
 * 
 * @since 1.4.1
 */
@Named
public class SaasResourceClient
{
    private SaasClient client;

    @Inject
    public SaasResourceClient( SaasClient client )
    {
        this.client = client;
    }

    public ComponentSummary getComponentSummary( MavenCoordinates coordinates ) throws IOException
    {
        Map<String, String> queryParams = new LinkedHashMap<String, String>();
        queryParams.put( "groupId", coordinates.getGroupId() );
        queryParams.put( "artifactId", coordinates.getArtifactId() );
        queryParams.put( "version", coordinates.getVersion() );
        
        //optional fields
        if (StringUtils.isNotBlank( coordinates.getExtension() ) )
        {
            queryParams.put( "extension", coordinates.getExtension() );
        }
        
        if (StringUtils.isNotBlank( coordinates.getClassifier() ) )
        {
            queryParams.put( "classifier", coordinates.getClassifier() );
        }
        
        return client.get( ComponentSummary.class, "rest/ide/component", queryParams );
    }
}
