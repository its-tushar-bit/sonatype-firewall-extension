/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { connect } from 'react-redux';
import { pick } from 'ramda';
import { stateGo } from '../../reduxUiRouter/routerActions';
import LegalApplicationDetailsPage from './LegalApplicationDetailsPage';
import * as legalApplicationDetailsActions from './legalApplicationDetailsActions';

function mapStateToProps({ legalApplicationDetails, router }) {
  return {
    ...pick(
      ['application', 'stageType', 'components'],
      legalApplicationDetails
    ),
    ...pick(['applicationPublicId', 'stageTypeId'], router.currentParams),
  };
}

const mapDispatchToProps = {
  ...legalApplicationDetailsActions,
  stateGo,
};

const LegalApplicationDetailsContainer = connect(
  mapStateToProps,
  mapDispatchToProps
)(LegalApplicationDetailsPage);
export default LegalApplicationDetailsContainer;
