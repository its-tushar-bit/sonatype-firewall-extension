/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { Fragment } from 'react';
import { NxTextLink } from '@sonatype/react-shared-components';

function NoProductLicense() {
  return (
    <Fragment>
      <p className="nx-p" id="license-install-guideline">
        No product licenses to display. Please locate your license key and use the “Install License” button below to
        install it. This is a file with the extension “.lic”, which will have been emailed to one or more technical
        contacts in your organization. If you have issues locating or installing your license key, please{' '}
        <NxTextLink href="mailto:support@sonatype.com">contact support</NxTextLink>.
      </p>
      <p className="nx-p" id="license-proxy-guideline">
        If you need to configure a Proxy Server, use either the public REST API or the{' '}
        <NxTextLink href="#proxyConfig">Proxy Server Configuration form</NxTextLink>.
      </p>
    </Fragment>
  );
}

export default NoProductLicense;
