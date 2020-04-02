/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';

import LoadWrapper from '../react/LoadWrapper';
import MaximizedContainer from '../react/MaximizedContainer';
import BackButton from '../react/BackButton';
import ViolationDetailsTile from './ViolationDetailsTile';

export default function ViolationPage(props) {
  const { $state, loadViolation, fetchStageTypes, loading, violationDetails, stageTypes } = props,
      { id } = $state.params,
      error = props.error || props.stageTypesError;

  useEffect(() => { load(); }, [id]);

  function load() {
    loadViolation(id);
    fetchStageTypes('dashboard');
  }

  return (
    <MaximizedContainer id="violation-page" className="nx-root-container">
      <aside className="nx-page-sidebar">
        <BackButton $state={$state} stateName="dashboard.overview.violations"/>
      </aside>
      <div className="nx-page-main">
        <LoadWrapper error={error} loading={loading || !(violationDetails && stageTypes)}>
          <ViolationDetailsTile { ...({ $state, stageTypes, violationDetails }) } />
        </LoadWrapper>
      </div>
    </MaximizedContainer>
  );
}

ViolationPage.propTypes = {
  $state: PropTypes.shape({
    params: PropTypes.shape({
      id: PropTypes.string.isRequired
    }).isRequired,
    get: PropTypes.func.isRequired,
    href: PropTypes.func.isRequired
  }).isRequired,
  loadViolation: PropTypes.func.isRequired,
  fetchStageTypes: PropTypes.func.isRequired,
  loading: PropTypes.bool.isRequired,
  error: LoadWrapper.propTypes.error,
  stageTypesError: LoadWrapper.propTypes.error,
  violationDetails: ViolationDetailsTile.propTypes.violationDetails,
  stageTypes: ViolationDetailsTile.propTypes.stageTypes
};
