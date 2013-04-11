package com.sonatype.insight.brain;

import org.sonatype.licensing.product.ProductLicenseKey;
import org.sonatype.licensing.product.util.LicenseFingerprinter;

public class TestLicenseFingerprinter
    extends LicenseFingerprinter
{
    @Override
    public String calculate()
    {
        return "1234";
    }
    
    @Override
    public String calculate( ProductLicenseKey key )
    {
        return calculate();
    }
}
