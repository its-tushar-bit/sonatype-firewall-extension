/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import {
  availableScopesPropType,
  componentNoticeDetailsPropType,
  componentPropType,
} from '../../advancedLegalPropTypes';
import * as PropTypes from 'prop-types';
import NoticeDetailsOverview from './NoticeDetailsOverview';

export default function NoticeDetailsContents(props) {
  const { loading, error, availableScopes, componentNoticeDetails, component } = props;

  // If we're loading data or in error state than the rendering will be handled by NoticeDetailsHeader
  // component and this component should not be rendered
  return loading || error ? null : (
    <NoticeDetailsOverview
      availableScopes={availableScopes}
      component={component}
      componentNoticeDetails={componentNoticeDetails}
    />
  );
}

NoticeDetailsContents.propTypes = {
  loading: PropTypes.bool,
  error: PropTypes.string,
  component: componentPropType,
  availableScopes: availableScopesPropType,
  componentNoticeDetails: componentNoticeDetailsPropType,
};
