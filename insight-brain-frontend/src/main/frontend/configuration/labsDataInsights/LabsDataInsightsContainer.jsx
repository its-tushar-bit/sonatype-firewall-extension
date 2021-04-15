/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import * as labsDataInsightsActions from './labsDataInsightsActions';
import LabsDataInsights from './LabsDataInsights';

function mapStateToProps({ labsDataInsights }) {
  return {
    errorMessage: labsDataInsights.viewState.errorMessage,
    loadingLabsDataInsights: labsDataInsights.viewState.loadingLabsDataInsights,
  };
}
const LabsDataInsightsContainer = connect(mapStateToProps, labsDataInsightsActions)(LabsDataInsights);
export default LabsDataInsightsContainer;
