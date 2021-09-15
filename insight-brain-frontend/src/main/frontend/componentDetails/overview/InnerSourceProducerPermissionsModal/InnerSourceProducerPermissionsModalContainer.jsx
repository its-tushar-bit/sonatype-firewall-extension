/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { connect } from 'react-redux';
import { actions } from '../overviewSlice';
import InnerSourceProducerPermissionsModal from '../../../applicationReport/results/cipModal/cipTabPanel/innerSourceProducerPermissionsModal/InnerSourceProducerPermissionsModal';
import { selectShowInsufficientPermissionsModal } from '../overviewSelectors';
import { selectSelectedComponent } from '../../../applicationReport/applicationReportSelectors';
import { pathOr } from 'ramda';

function mapStateToProps(state) {
  const applicationName = pathOr(
    'N/A',
    ['innerSourceData', '0', 'ownerApplicationName'],
    selectSelectedComponent(state)
  );
  return {
    showModal: selectShowInsufficientPermissionsModal(state),
    applicationName,
  };
}

const mapDispatchToProps = {
  onClose: actions.toggleInnerSourcePermissionsModal,
};

function InnerSourceProducerPermissionsModalContainer(props) {
  return props.showModal && props.applicationName ? <InnerSourceProducerPermissionsModal {...props} /> : null;
}

export default connect(mapStateToProps, mapDispatchToProps)(InnerSourceProducerPermissionsModalContainer);

InnerSourceProducerPermissionsModalContainer.propTypes = InnerSourceProducerPermissionsModal.propTypes;
