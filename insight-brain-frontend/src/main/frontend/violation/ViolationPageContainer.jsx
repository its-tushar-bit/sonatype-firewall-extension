/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { pick } from 'ramda';

import { loadViolation, loadVulnerabilityDetails } from './violationActions';
import { stateGo } from '../reduxUiRouter/routerActions';
import { fetchStageTypes } from '../stages/stagesActions';
import ViolationPage from './ViolationPage';

function mapStateToProps({ stages, violation }) {
  const stageData = stages.dashboard;

  return {
    ...pick(
      [
        'loading',
        'violationDetailsError',
        'violationDetails',
        'vulnerabilityDetailsLoading',
        'vulnerabilityDetails',
        'vulnerabilityDetailsError',
        'activeWaivers',
      ],
      violation
    ),
    stageTypes: stageData.stageTypes,
    stageTypesError: stageData.error,
  };
}

const mapDispatchToProps = {
  loadViolation,
  loadVulnerabilityDetails,
  fetchStageTypes,
  stateGo,
};

const ViolationPageContainer = connect(mapStateToProps, mapDispatchToProps)(ViolationPage);
export default ViolationPageContainer;

ViolationPageContainer.propTypes = pick(['$state'], ViolationPage.propTypes);
