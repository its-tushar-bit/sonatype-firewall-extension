package com.sonatype.insight.brain.saas;

import javax.inject.Named;
import javax.ws.rs.Path;

import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.CLMEnforcementPoint;

@Path( CIComponentInfoResource.SERVICE_PATH )
@ProductLicenseEnforcementPoint( { CLMEnforcementPoint.Build } )
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
