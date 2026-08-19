/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';
import {
  loadCopyrightContexts,
  loadFilePathsOnPageUpdate,
  unloadCopyrightContext,
} from './componentCopyrightDetailsActions';
import { connect } from 'react-redux';
import CopyrightDetailsContents from './CopyrightDetailsContents';

function mapStateToProps({ advancedLegal, componentCopyrightDetails }) {
  const component = advancedLegal.component || {};
  const availableScopes = advancedLegal.availableScopes || {};
  return {
    loading: component.loading || availableScopes.loading || componentCopyrightDetails.loadingCopyrightFileCounts,
    error:
      component.error ||
      availableScopes.error ||
      componentCopyrightDetails.errorCopyrightFileCounts ||
      componentCopyrightDetails.errorFilePaths,
    availableScopes,
    componentCopyrightDetails,
    ...pick(['component'], component),
  };
}

const mapDispatchToProps = {
  loadCopyrightContexts,
  unloadCopyrightContext,
  loadFilePathsOnPageUpdate,
};

const CopyrightDetailsContentsContainer = connect(mapStateToProps, mapDispatchToProps)(CopyrightDetailsContents);
export default CopyrightDetailsContentsContainer;
