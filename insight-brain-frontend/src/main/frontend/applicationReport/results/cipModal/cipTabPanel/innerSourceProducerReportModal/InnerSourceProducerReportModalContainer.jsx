/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { connect } from 'react-redux';
import { closeInnerSourceProducerReportModal } from '../../../../applicationReportActions';
import InnerSourceProducerReportModal from './InnerSourceProducerReportModal';

function mapStateToProps({ applicationReport }) {
  return {
    showModal: applicationReport.selectedComponent.showInnerSourceProducerReportModal,
  };
}

const mapDispatchToProps = {
  onClose: closeInnerSourceProducerReportModal,
};

function InnerSourceProducerReportModalContainer(props) {
  return props.showModal && props.reportUrl ? <InnerSourceProducerReportModal {...props} /> : null;
}

export default connect(mapStateToProps, mapDispatchToProps)(InnerSourceProducerReportModalContainer);

InnerSourceProducerReportModalContainer.propTypes = InnerSourceProducerReportModal.propTypes;
