package com.sonatype.insight.brain.saas;

import javax.inject.Named;
import javax.ws.rs.Path;

@Path( CIComponentInfoResource.SERVICE_PATH )
@Named
public class CIComponentInfoResource
    extends AbstractComponentInfoResource
{
    public static final String SERVICE_PATH = "rest/ci/component/details";

    @Override
    protected String getToolName()
    {
        return "ci";
    }
}
