/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { connect } from 'react-redux';
import InnerSourceProducerReportModal from '../../../applicationReport/results/cipModal/cipTabPanel/innerSourceProducerReportModal/InnerSourceProducerReportModal';
import { actions } from '../overviewSlice';
import { selectShowInnerSourceProducerReportModal, selectInnerSourceProducerUrl } from '../overviewSelectors';

function mapStateToProps(state) {
  return {
    showModal: selectShowInnerSourceProducerReportModal(state),
    reportUrl: selectInnerSourceProducerUrl(state),
  };
}

const mapDispatchToProps = {
  onClose: actions.toggleInnerSourceProducerReportModal,
};

function InnerSourceProducerReportModalContainer(props) {
  return props.showModal && props.reportUrl ? <InnerSourceProducerReportModal {...props} /> : null;
}

export default connect(mapStateToProps, mapDispatchToProps)(InnerSourceProducerReportModalContainer);

InnerSourceProducerReportModalContainer.propTypes = InnerSourceProducerReportModal.propTypes;
