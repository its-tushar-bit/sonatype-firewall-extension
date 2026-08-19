/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { IqPopoverHeaderTitleText } from 'MainRoot/react/IqPopover';
import PropTypes from 'prop-types';
import React from 'react';

export default function VexAnnotationDrawerHeader(props) {
  const { componentPurl, headerSize, headerTitle } = props;

  return (
    <div>
      <div className="iq-popover-header__title">
        <IqPopoverHeaderTitleText headerSize={headerSize} headerTitle={headerTitle} />
      </div>
      <div className="vex-annotation-drawer-header-popover__package-url">{componentPurl}</div>
    </div>
  );
}

VexAnnotationDrawerHeader.propTypes = {
  componentPurl: PropTypes.string,
  headerSize: PropTypes.string,
  headerTitle: PropTypes.string,
};
