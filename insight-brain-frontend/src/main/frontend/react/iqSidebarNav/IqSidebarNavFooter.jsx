/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';
import { NxGlobalSidebarFooter } from '@sonatype/react-shared-components';

export default function IqSidebarNavFooter({ productName, releaseNumber, tenantMode }) {
  const releaseText = (
    <Fragment>
      <span className="iq-sidebar-nav-footer__product-name visual-testing-ignore">{productName}</span>
      {displayVersion()}
    </Fragment>
  );

  function displayVersion() {
    if (tenantMode === 'single') {
      return (
        <>
          {` `}Release {releaseNumber}
        </>
      );
    }

    return null;
  }

  return (
    <NxGlobalSidebarFooter
      className="iq-sidebar-nav-footer"
      releaseText={releaseText}
      productTagLine="Powered by Nexus IQ Server"
    />
  );
}

IqSidebarNavFooter.propTypes = {
  productName: PropTypes.string,
  releaseNumber: PropTypes.string,
  tenantMode: PropTypes.string,
};
