/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.InsightWork;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

@Named
@Path( OrganizationResource.SERVICE_PATH )
public class OrganizationResource
    extends AbstractResourceWithIcon
{
    public static final String SERVICE_PATH = "rest/organization";

    public static final String GET_ICON_PATH = ICON_PATH + "/{organizationId}";

    @Context
    private InsightWork work;

    /**
     * @since 1.6
     */
    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public List<Organization> getAll()
    {
        return new OrganizationDAO().getAll();
    }

    /**
     * @since 1.6
     */
    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Organization addOrganization( Organization organization )
    {
        new OrganizationDAO().insert( organization );

        return organization;
    }

    /**
     * @since 1.6
     */
    @PUT
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Organization updateOrganization( Organization organization )
    {
        new OrganizationDAO().update( organization );

        return organization;
    }

    @Override
    protected String getDefaultIconFilename()
    {
        return "defaulticon_organization.png";
    }
    
    /**
     * @since 1.6
     */
    @Override
    @GET
    @Path( GENERATE_ICON_PATH )
    @Produces( "image/png" )
    public Response generateIcon( @PathParam( "hashcode" ) final String hashcode, @Context final HttpServletRequest req )
        throws IOException
    {
        return super.generateIcon( hashcode, req );
    }

    /**
     * @since 1.6
     */
    @GET
    @Path( GET_ICON_PATH )
    @Produces( "image/png" )
    public Response getIcon( @PathParam( "organizationId" ) String organizationId )
        throws IOException
    {
        return super.getIcon( organizationId, work.getOrganizationIconDir() );
    }

    /**
     * This is one of two service methods used for editing and adding icons. This method is used for AJAX calls since
     * its return type is a JSON object.
     * 
     * @since 1.6
     */
    @POST
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    @Path( ICON_PATH )
    public void setIcon( @FormDataParam( "organizationId" ) String organizationId,
                         @FormDataParam( "hasRobotSource" ) boolean hasRobotSource,
                         @FormDataParam( "robotHash" ) String robotHash,
                         @FormDataParam( "file" ) InputStream uploadedInputStream,
                         @FormDataParam( "file" ) FormDataContentDisposition fileDetail )
        throws IOException
    {
        super.setIcon( organizationId, work.getOrganizationIconDir(), hasRobotSource, robotHash, uploadedInputStream,
                       fileDetail );
    }

    /**
     * This is one of two service methods used for editing and adding icons. This method is used by angular ng-upload
     * and returns an empty string for success and the error message otherwise
     * 
     * @since 1.6
     */
    @POST
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    @Path( ICON_PATH_SYNC )
    public String setIconSync( @FormDataParam( "organizationId" ) String organizationId,
                                 @FormDataParam( "hasRobotSource" ) boolean hasRobotSource,
                                 @FormDataParam( "robotHash" ) String robotHash,
                                 @FormDataParam( "file" ) InputStream uploadedInputStream,
                                 @FormDataParam( "file" ) FormDataContentDisposition fileDetail )
    {
        return super.setIconSync( organizationId, work.getOrganizationIconDir(), hasRobotSource, robotHash,
                                  uploadedInputStream, fileDetail );
    }
}
