/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';
import { connect } from 'react-redux';
import * as advancedLegalActions from './advancedLegalActions';
import AdvancedLegalApplicationPage from './AdvancedLegalApplicationPage';

function mapStateToProps({ advancedLegal, router }) {
  return {
    ...pick(['viewStateApplicationReport', 'applicationReport'], advancedLegal),
    ...pick(['publicId'], router.currentParams),
  };
}

const mapDispatchToProps = { ...advancedLegalActions };

const AdvancedLegalApplicationContainer = connect(mapStateToProps, mapDispatchToProps)(AdvancedLegalApplicationPage);
export default AdvancedLegalApplicationContainer;

AdvancedLegalApplicationContainer.propTypes = pick(['$state'], AdvancedLegalApplicationPage.propTypes);
