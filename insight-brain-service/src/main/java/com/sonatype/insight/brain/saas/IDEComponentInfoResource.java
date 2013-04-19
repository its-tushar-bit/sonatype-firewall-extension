package com.sonatype.insight.brain.saas;

import java.io.IOException;
import java.util.Set;

import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.ide.ComponentDetails;
import com.sonatype.insight.brain.product.license.CLMEnforcementPoint;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;

@Path( IDEComponentInfoResource.SERVICE_PATH )
@ProductLicenseEnforcementPoint( { CLMEnforcementPoint.Develop } )
@Named
public class IDEComponentInfoResource
    extends AbstractComponentInfoResource
{
    public static final String SERVICE_PATH = "rest/ide/component/details";

    @GET
    @Path( "versions/{applicationPublicId}" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getComponentVersionDetails( @PathParam( "applicationPublicId" )
    String applicationPublicId, @QueryParam( "instanceId" )
    String instanceId, @QueryParam( "groupId" )
    String groupId, @QueryParam( "artifactId" )
    String artifactId, @QueryParam( "version" )
    String version )
        throws IOException
    {
        return doGetComponentVersionDetails( applicationPublicId, instanceId, groupId, artifactId, version );
    }

    @GET
    @Path( "{applicationPublicId}" )
    @Produces( MediaType.APPLICATION_JSON )
    public ComponentDetails getComponentDetails( @PathParam( "applicationPublicId" )
    String applicationPublicId, @QueryParam( "instanceId" )
    String instanceId, @QueryParam( "groupId" )
    String groupId, @QueryParam( "artifactId" )
    String artifactId, @QueryParam( "version" )
    String version, @QueryParam( "hash" )
    String hash, @QueryParam( "matchState" )
    String matchState )
        throws IOException
    {
        return doGetComponentDetails( applicationPublicId, instanceId, groupId, artifactId, version, hash, matchState );
    }

    @GET
    @Path( "selectableLicenses/{applicationPublicId}" )
    @Produces( { MediaType.APPLICATION_JSON } )
    public Set<License> getSelectableLicenses( @PathParam( "applicationPublicId" )
    String applicationPublicId, @QueryParam( "instanceId" )
    String instanceId, @QueryParam( "groupId" )
    String groupId, @QueryParam( "artifactId" )
    String artifactId, @QueryParam( "version" )
    String version )
        throws IOException
    {
        return doGetSelectableLicenses( applicationPublicId, instanceId, groupId, artifactId, version );
    }

    @Override
    protected String getToolName()
    {
        return "ide";
    }
}
