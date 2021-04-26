/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import * as legalApplicationDetailsFilterActions from './legalApplicationDetailsFilterActions';
import LegalApplicationDetailsFilter from './LegalApplicationDetailsFilter';

function mapStateToProps({ legalApplicationDetails }) {
  return {
    ...legalApplicationDetails,
  };
}

const mapDispatchToProps = {
  ...legalApplicationDetailsFilterActions,
};

const LegalApplicationDetailsFilterContainer = connect(
  mapStateToProps,
  mapDispatchToProps
)(LegalApplicationDetailsFilter);
export default LegalApplicationDetailsFilterContainer;
