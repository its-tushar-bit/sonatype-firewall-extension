/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { connect } from 'react-redux';
import { pick } from 'ramda';
import { loadViolation } from './../violation/violationActions';

import RequestWaiverPage from './RequestWaiverPage';

function mapStateToProps({ violation, router }) {
  return {
    ...pick(['loading', 'violationDetailsError', 'violationDetails'], violation),
    ...pick(['violationId'], router.currentParams),
    ...pick(['name'], router.prevState),
    prevParams: router.prevParams,
  };
}

const RequestWaiverPageContainer = connect(mapStateToProps, { loadViolation })(RequestWaiverPage);
export default RequestWaiverPageContainer;
