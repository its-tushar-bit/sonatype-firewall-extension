/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import Overview from './Overview';
import { selectSelectedComponent } from '../../applicationReport/applicationReportSelectors';
import { actions } from './overviewSlice';
import { loadReportIfNeeded } from '../../applicationReport/applicationReportActions';
import { occurrencesPopoverActions } from './occurrencesPopover/occurrencesPopoverSlice';
import {
  selectComponentSimilarMatches,
  selectComponentDetailsLoading,
  selectComponentDetailsLoadErrors,
} from '../componentDetailsSelectors';

function mapStateToProps(state) {
  return {
    loading: selectComponentDetailsLoading(state),
    loadError: selectComponentDetailsLoadErrors(state),
    componentInformation: selectSelectedComponent(state),
    similarMatches: selectComponentSimilarMatches(state),
  };
}

const mapDispatchToProps = {
  toggleShowOccurrencesPopover: occurrencesPopoverActions.toggleShowOccurrencesPopover,
  toggleShowSimilarMatches: actions.toggleShowSimilarMatches,
  loadInnerSourceProducerData: actions.loadInnerSourceProducerData,
  loadReport: loadReportIfNeeded,
};

export const OverviewContainer = connect(mapStateToProps, mapDispatchToProps)(Overview);
