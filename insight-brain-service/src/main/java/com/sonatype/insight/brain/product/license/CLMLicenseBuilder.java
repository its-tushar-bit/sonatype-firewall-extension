package com.sonatype.insight.brain.product.license;

import javax.inject.Named;

import org.sonatype.licensing.product.AbstractLicenseBuilder;

@Named
public class CLMLicenseBuilder
    extends AbstractLicenseBuilder
{

    public CLMLicenseBuilder()
    {
        super( new CLMProductDetails(), "com/sonatype/clm" );
    }

    @Override
    public String getPublicKeyStorePassword()
    {
        return "r5h57cYfggHHDdS";
    }

    @Override
    public String getPublicKeyStorePath()
    {
        return "/productlicense/publicKeyStore";
    }

}
