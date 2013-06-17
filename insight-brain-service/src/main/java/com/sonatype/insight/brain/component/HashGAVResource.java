/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dataaccess.component.HashGAVDAO;
import com.sonatype.insight.brain.model.component.HashGAV;
import com.sonatype.insight.brain.saas.SaasClient;
import com.sonatype.insight.error.exception.BadRequestException;

@Named
@Path( HashGAVResource.SERVICE_PATH )
public class HashGAVResource
{
    public static final String SERVICE_PATH = "rest/component/identified";

    @Context
    private SaasClient client;

    /**
     * @since 1.4.1
     */
    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public HashGAV setHashGAV( HashGAV hashGAV )
        throws IOException
    {
        Map<String, String> queryParams = new LinkedHashMap<String, String>();
        queryParams.put( "groupId", hashGAV.getGroupId() );
        queryParams.put( "artifactId", hashGAV.getArtifactId() );
        queryParams.put( "version", hashGAV.getVersion() );
        queryParams.put( "extension", hashGAV.getExtension() );
        queryParams.put( "classifier", hashGAV.getClassifier() );
        Boolean isKnown = client.get( Boolean.class, "rest/ide/component/isKnown", queryParams );
        if ( isKnown )
        {
            throw new BadRequestException( "The '" + hashGAV.getGAVECString() + "' coordinates are already in use" );
        }
        
        hashGAV.setId( null );
        new HashGAVDAO().insert( hashGAV );

        return hashGAV;
    }
}
