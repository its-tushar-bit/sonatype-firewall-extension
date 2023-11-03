/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';
import { NxGlobalSidebarFooter } from '@sonatype/react-shared-components';

export default function IqSidebarNavFooter({ releaseNumber, isShowVersionEnabled }) {
  const releaseText = <Fragment>{displayVersion()}</Fragment>;

  function displayVersion() {
    if (isShowVersionEnabled) {
      return <>Release {releaseNumber}</>;
    }

    return null;
  }

  return (
    <NxGlobalSidebarFooter
      className="iq-sidebar-nav-footer"
      releaseText={releaseText}
      productTagLine="Powered by Sonatype IQ Server"
      showCreatedBy={false}
    />
  );
}

IqSidebarNavFooter.propTypes = {
  releaseNumber: PropTypes.string,
  isShowVersionEnabled: PropTypes.bool,
};
