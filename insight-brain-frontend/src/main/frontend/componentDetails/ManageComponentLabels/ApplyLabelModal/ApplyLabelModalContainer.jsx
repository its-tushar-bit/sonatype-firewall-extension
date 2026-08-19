/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import {
  selectApplicableLabelScopes,
  selectApplicableLabelScopesLoadError,
  selectLabelModalMaskState,
  selectComponentName,
  selectSaveLabelError,
  selectIsApplyLabelModalLoading,
  selectLabelScopeToSave,
  selectSelectedLabelDetails,
  selectShowApplyLabelModal,
} from '../../componentDetailsSelectors';
import { selectFirewallComponentName } from 'MainRoot/firewall/firewallSelectors';
import { actions } from '../../componentDetailsSlice';
import ApplyLabelModal from './ApplyLabelModal';

function mapStateToProps(state) {
  return {
    applicableLabelScopes: selectApplicableLabelScopes(state),
    componentName: selectComponentName(state) || selectFirewallComponentName(state),
    labelScopeToSave: selectLabelScopeToSave(state),
    loadError: selectApplicableLabelScopesLoadError(state),
    loading: selectIsApplyLabelModalLoading(state),
    saveLabelError: selectSaveLabelError(state),
    selectedLabelDetails: selectSelectedLabelDetails(state),
    showApplyLabelModal: selectShowApplyLabelModal(state),
    submitMaskState: selectLabelModalMaskState(state),
  };
}

const mapDispatchToProps = {
  cancelApplyLabelModal: actions.cancelApplyLabelModal,
  loadApplicableLabelScopes: actions.loadApplicableLabelScopes,
  saveApplyLabelScope: actions.saveApplyLabelScope,
  setLabelScopeToSave: actions.setLabelScopeToSaveAction,
};

export default connect(mapStateToProps, mapDispatchToProps)(ApplyLabelModal);
