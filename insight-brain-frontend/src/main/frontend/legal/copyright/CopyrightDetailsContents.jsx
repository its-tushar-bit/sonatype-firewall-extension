/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import {
  availableScopesPropType,
  componentCopyrightDetailsPropType,
  componentPropType,
} from '../advancedLegalPropTypes';
import * as PropTypes from 'prop-types';
import CopyrightDetailsOverview from './CopyrightDetailsOverview';
import CopyrightFilesTile from './CopyrightFilesTile';

export default function CopyrightDetailsContents(props) {
  const {
    loading,
    error,
    availableScopes,
    componentCopyrightDetails,
    component,

    loadCopyrightContexts,
    unloadCopyrightContext,
    loadFilePathsOnPageUpdate,
  } = props;

  // If we're loading data or in error state than the rendering will be handled by CopyrightDetailsHeader
  // component and this component should not be rendered
  return loading || error ? null : (
    <div className="nx-scrollable nx-viewport-sized__scrollable">
      <CopyrightDetailsOverview
        availableScopes={availableScopes}
        component={component}
        componentCopyrightDetails={componentCopyrightDetails}
      />
      <CopyrightFilesTile
        loadCopyrightContexts={loadCopyrightContexts}
        hideCopyrightContext={unloadCopyrightContext}
        componentCopyrightDetails={componentCopyrightDetails}
        pageChange={loadFilePathsOnPageUpdate}
      />
    </div>
  );
}

CopyrightDetailsContents.propTypes = {
  loading: PropTypes.bool,
  error: PropTypes.string,
  component: componentPropType,
  availableScopes: availableScopesPropType,
  componentCopyrightDetails: componentCopyrightDetailsPropType,
  loadFilePathsOnPageUpdate: PropTypes.func.isRequired,
  loadCopyrightContexts: PropTypes.func.isRequired,
  unloadCopyrightContext: PropTypes.func.isRequired,
};
