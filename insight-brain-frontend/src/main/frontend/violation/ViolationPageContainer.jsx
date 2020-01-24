/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {connect} from 'react-redux';
import {pick} from 'ramda';

import { loadViolation } from './violationPageActions';
import ViolationPage from './ViolationPage';

function mapStateToProps({ violationPage }) {
  return pick(['loading', 'error'], violationPage);
}

const mapDispatchToProps = { loadViolation };

const ViolationPageContainer = connect(mapStateToProps, mapDispatchToProps)(ViolationPage);
export default ViolationPageContainer;

ViolationPageContainer.propTypes = pick(['$state'], ViolationPage.propTypes);
