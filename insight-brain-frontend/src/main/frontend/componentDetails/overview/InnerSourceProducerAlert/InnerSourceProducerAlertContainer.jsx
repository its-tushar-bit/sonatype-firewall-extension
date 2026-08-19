/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { actions } from '../overviewSlice';
import { path } from 'ramda';
import InnerSourceProducerAlert from './InnerSourceProducerAlert';
import { selectInnerSourceProducerData } from '../overviewSelectors';
import { selectSelectedComponent } from '../../../applicationReport/applicationReportSelectors';

function mapStateToProps(state) {
  const { innerSource, innerSourceData } = selectSelectedComponent(state);
  return {
    innerSourceProducerData: selectInnerSourceProducerData(state),
    isInnerSource: !!innerSource,
    ownerApplicationName: path([0, 'ownerApplicationName'], innerSourceData),
  };
}

const mapDispatchToProps = {
  onClick: actions.openInnerSourceProducerReport,
};

export default connect(mapStateToProps, mapDispatchToProps)(InnerSourceProducerAlert);
