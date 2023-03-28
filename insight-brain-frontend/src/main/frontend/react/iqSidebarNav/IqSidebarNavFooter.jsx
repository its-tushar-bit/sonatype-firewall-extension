/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';
import { NxGlobalSidebarFooter } from '@sonatype/react-shared-components';
import { SINGLE_TENANT } from 'MainRoot/productFeatures/productFeaturesSelectors';

export default function IqSidebarNavFooter({ productName, releaseNumber, isShowVersionEnabled }) {
  const releaseText = (
    <Fragment>
      <span className="iq-sidebar-nav-footer__product-name visual-testing-ignore">{productName}</span>
      {displayVersion()}
    </Fragment>
  );

  function displayVersion() {
    if (isShowVersionEnabled) {
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
  isShowVersionEnabled: PropTypes.bool,
};
