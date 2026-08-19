/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.mtiq;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * FIPS-mode variant of {@link AbstractMtiqUiTest}: inserts the BouncyCastle FIPS JCE provider
 * and sets {@code FIPS_MODE_ENABLED=true} before each test's tenant provisioning runs, via
 * {@link MtiqFipsSetupExtension}. Subclasses do not need to manage the provider lifecycle.
 */
@ExtendWith(MtiqFipsSetupExtension.class)
public abstract class AbstractMtiqFipsUiTest
    extends AbstractMtiqUiTest
{
}
