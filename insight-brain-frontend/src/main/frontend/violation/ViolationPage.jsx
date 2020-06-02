/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';

import LoadWrapper from '../react/LoadWrapper';
import ViolationDetailsTile, { violationDetailsPropTypes } from './ViolationDetailsTile';
import PolicyViolationInfoTile, { constraintViolationsPropType } from './PolicyViolationInfoTile';
import MaximizedContainer from '../react/MaximizedContainer';

export default function ViolationPage(props) {
  const {
    $state,
    loadViolation,
    fetchStageTypes,
    loading,
    violationDetails,
    stageTypes,
    vulnerabilityDetailsLoading,
    vulnerabilityDetails,
    vulnerabilityDetailsError
  } = props;

  const { id } = $state.params,
      error = props.violationDetailsError || props.stageTypesError;

  useEffect(() => { load(); }, [id]);

  function load() {
    loadViolation(id);
    fetchStageTypes('dashboard');
  }

  return (
    <MaximizedContainer id="violation-page">
      <LoadWrapper error={error} loading={loading || !(violationDetails && stageTypes)}>
        <ViolationDetailsTile { ...({ $state, stageTypes, violationDetails }) } />
        <PolicyViolationInfoTile {...({
          violationDetails,
          vulnerabilityDetails,
          vulnerabilityDetailsError,
          vulnerabilityDetailsLoading
        })}/>
      </LoadWrapper>
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
  violationDetailsError: LoadWrapper.propTypes.error,
  stageTypesError: LoadWrapper.propTypes.error,
  violationDetails: PropTypes.shape({
    ...violationDetailsPropTypes,
    constraintViolations: constraintViolationsPropType.isRequired
  }),
  stageTypes: ViolationDetailsTile.propTypes.stageTypes,
  vulnerabilityDetailsLoading: PropTypes.bool.isRequired,
  vulnerabilityDetails: PropTypes.object,
  vulnerabilityDetailsError: LoadWrapper.propTypes.error
};
