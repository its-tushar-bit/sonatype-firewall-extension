/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import SimilarMatchesPopover from './SimilarMatchesPopover';
import { selectComponentSimilarMatches } from '../../componentDetailsSelectors';
import { selectComponentDetailsOverviewSlice } from '../overviewSelectors';
import { actions } from '../overviewSlice';

function mapStateToProps(state) {
  return {
    similarMatches: selectComponentSimilarMatches(state),
    showSimilarMatchesPopover: selectComponentDetailsOverviewSlice(state).showSimilarMatchesPopover,
  };
}

const mapDispatchToProps = {
  onClose: actions.toggleShowSimilarMatches,
};

export default connect(mapStateToProps, mapDispatchToProps)(SimilarMatchesPopover);
