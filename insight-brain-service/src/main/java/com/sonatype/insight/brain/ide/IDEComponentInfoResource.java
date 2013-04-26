package com.sonatype.insight.brain.ide;

import javax.inject.Named;
import javax.ws.rs.Path;

import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.saas.AbstractComponentInfoResource;
import com.sonatype.insight.license.model.CLMEnforcementPoint;

@Path( IDEComponentInfoResource.SERVICE_PATH )
@ProductLicenseEnforcementPoint( { CLMEnforcementPoint.Develop } )
@Named
public class IDEComponentInfoResource
    extends AbstractComponentInfoResource
{
    public static final String SERVICE_PATH = "rest/ide/component/details";

    @Override
    protected String getToolName()
    {
        return "ide";
    }
}
