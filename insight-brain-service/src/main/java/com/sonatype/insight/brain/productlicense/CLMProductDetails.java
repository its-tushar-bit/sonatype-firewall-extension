package com.sonatype.insight.brain.productlicense;

import javax.inject.Named;

import org.sonatype.licensing.ProductDetails;

@Named
public class CLMProductDetails
    implements ProductDetails
{
    @Override
    public String getBrandName()
    {
        return "Sonatype CLM";
    }

    @Override
    public String getProductName()
    {
        return "CLM";
    }
}
