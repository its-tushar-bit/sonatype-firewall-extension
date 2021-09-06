/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { connect } from 'react-redux';
import { closeInnerSourceProducerPermissionsModal } from '../../../../applicationReportActions';
import InnerSourceProducerPermissionsModal from './InnerSourceProducerPermissionsModal';

function mapStateToProps({ applicationReport }) {
  return {
    showModal: applicationReport.selectedComponent.showInnerSourceProducerPermissionsModal,
  };
}

const mapDispatchToProps = {
  onClose: closeInnerSourceProducerPermissionsModal,
};

function InnerSourceProducerPermissionsModalContainer(props) {
  return props.showModal && props.applicationName ? <InnerSourceProducerPermissionsModal {...props} /> : null;
}

export default connect(mapStateToProps, mapDispatchToProps)(InnerSourceProducerPermissionsModalContainer);

InnerSourceProducerPermissionsModalContainer.propTypes = InnerSourceProducerPermissionsModal.propTypes;
