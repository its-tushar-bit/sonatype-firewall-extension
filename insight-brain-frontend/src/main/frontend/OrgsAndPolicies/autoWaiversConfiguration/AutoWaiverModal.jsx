/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import UnsavedChangesModal from '../../modals/unsavedChangesModal/UnsavedChangesModal';
import {
  NxModal,
  NxH2,
  NxH4,
  NxH3,
  NxStatefulForm,
  NxButton,
  NxFieldset,
  NxTooltip,
  NxCheckbox,
  NxFontAwesomeIcon,
  NxP,
} from '@sonatype/react-shared-components';
import { actions } from './autoWaiverModalSlice';
import { faInfoCircle } from '@fortawesome/pro-solid-svg-icons';
import { selectAutoWaiverModalSlice, selectAutoWaiverDetails } from './autoWaiverModalSelectors';
import ThreatDropdownSelector from 'MainRoot/react/ThreatDropdownSelector';
import AutoWaiverScopeDropdownSelector from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/AutoWaiverScopeDropdownSelector';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';
import { selectHasAutoWaiverManagement } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { EnterpriseFullWidthBanner } from 'MainRoot/shared/enterpriseTier';
import TierTag from 'MainRoot/react/shared/TierTag';
import './_autoWaiverModal.scss';

const UPGRADE_PATH_IS_NOT_AVAILABLE_TEXT = 'Upgrade Path is not available';
const VULNERABILITY_IS_NOT_REACHABLE_TEXT = 'Vulnerability is not reachable';

export default function AutoWaiverModal() {
  const dispatch = useDispatch();

  const { isModalOpen, isEditMode, submitMaskState, submitError, isDirty, isUnsavedChangesModalOpen } = useSelector(
    selectAutoWaiverModalSlice
  );
  const waiverModal = useSelector(selectAutoWaiverDetails);
  const hasAutoWaiverManagement = useSelector(selectHasAutoWaiverManagement);
  const reachability = waiverModal.reachability;
  const pathForward = waiverModal.pathForward;
  const threatLevel = waiverModal.threatLevel;
  const scope = waiverModal.scope;

  const closeModalWithCheck = () => dispatch(actions.closeModal({ isDirty }));
  const closeUnsavedChangesModal = () => dispatch(actions.closeUnsavedChangesModal());
  const closeModal = () => dispatch(actions.closeModal());
  const createAutoWaiver = () => dispatch(actions.createAutoWaiver());
  const saveAutoWaiver = () => dispatch(actions.saveAutoWaiver());
  const setThreatLevel = (val) => dispatch(actions.setThreatLevel(val));
  const setScope = (val) => dispatch(actions.setScope(val));

  const additionalFooterButtons = (
    <NxButton variant="tertiary" type="button" className="nx-form__cancel-btn" onClick={closeModalWithCheck}>
      {hasAutoWaiverManagement ? 'Cancel' : 'Close'}
    </NxButton>
  );

  const getHeaderTitle = () => {
    return `${isEditMode ? 'Edit ' : 'New '} Auto-Waiver`;
  };

  const validationError = () => {
    if (!reachability && !pathForward) {
      return `Either '${UPGRADE_PATH_IS_NOT_AVAILABLE_TEXT}'
        or '${VULNERABILITY_IS_NOT_REACHABLE_TEXT}' is required to be selected.`;
    }

    if (!isDirty) {
      return MSG_NO_CHANGES_TO_SAVE;
    }

    return undefined;
  };

  useEffect(() => {
    return () => {
      closeModal();
    };
  }, []);

  return (
    <>
      {isModalOpen ? (
        <NxModal
          id="autowaiver-editor"
          onCancel={closeModalWithCheck}
          aria-labelledby="modal-header-text"
          data-testid="iq-auto-waiver-modal"
          className={!hasAutoWaiverManagement ? 'iq-auto-waiver-modal--pro-tier' : ''}
        >
          <NxStatefulForm
            onSubmit={hasAutoWaiverManagement ? (isEditMode ? saveAutoWaiver : createAutoWaiver) : undefined}
            submitMaskState={submitMaskState}
            submitBtnText={isEditMode ? 'Update' : 'Create'}
            submitError={submitError}
            validationErrors={validationError()}
            additionalFooterBtns={additionalFooterButtons}
          >
            <NxModal.Header>
              <NxH2 id="modal-header-text">
                {getHeaderTitle()}
                {!hasAutoWaiverManagement && <TierTag>Enterprise Feature</TierTag>}
              </NxH2>
            </NxModal.Header>
            <NxModal.Content>
              {!hasAutoWaiverManagement && (
                <EnterpriseFullWidthBanner description="Automatically apply waivers to low-risk, non-reachable or known issues so teams can stay unblocked." />
              )}
              <NxP>Automatically waive policy violations when the following conditions are met:</NxP>

              <div className="iq-auto-waiver-modal__container">
                <NxH3>Threat Level is equal to or less than </NxH3>
                <div className="iq-auto-waiver-threat-dropdown__container">
                  <ThreatDropdownSelector
                    threatLevel={threatLevel}
                    onSelectThreatLevel={setThreatLevel}
                    className="edit-auto-waiver-threat-dropdown"
                    id="editor-auto-waiver-threat-level"
                  />
                </div>
              </div>

              <div className="iq-auto-waiver-modal__container">
                <NxH3>
                  And, when{' '}
                  <AutoWaiverScopeDropdownSelector
                    scope={scope}
                    onSelectScope={setScope}
                    id="editor-auto-waiver-scope"
                    disabled={!reachability || !pathForward}
                  />{' '}
                  of the following are true:
                </NxH3>
              </div>

              <NxFieldset label="" className="iq-auto-waiver-modal-fieldset">
                <div className="iq-auto-waiver-modal-fieldset__item">
                  <NxH4>{UPGRADE_PATH_IS_NOT_AVAILABLE_TEXT}</NxH4>
                  <NxTooltip title={waiverModal?.isInherited ? 'Inherited from parent organization' : ''}>
                    <NxCheckbox
                      onChange={() => dispatch(actions.toggleCheckboxNoUpgradePath())}
                      isChecked={pathForward || false}
                      disabled={waiverModal?.isInherited}
                    >
                      No newer, non-violating component version is available
                    </NxCheckbox>
                  </NxTooltip>
                </div>
                <div className="iq-auto-waiver-modal-fieldset__item">
                  <div className="iq-auto-waiver-modal-fieldset__item-reachability-header">
                    <NxH4>{VULNERABILITY_IS_NOT_REACHABLE_TEXT}</NxH4>
                    <NxTooltip title="Reachability Analysis must be enabled (via Sonatype CLI or CI/CD Integration).">
                      <NxFontAwesomeIcon
                        className="iq-auto-waiver-modal-fieldset__item-icon"
                        data-testid="auto-waiver-modal-reachability-icon"
                        icon={faInfoCircle}
                      />
                    </NxTooltip>
                  </div>
                  <NxTooltip title={waiverModal?.isInherited ? 'Inherited from parent organization' : ''}>
                    <NxCheckbox
                      onChange={() => dispatch(actions.toggleCheckboxReachability())}
                      isChecked={reachability || false}
                      disabled={waiverModal?.isInherited}
                    >
                      Application does not execute any calls to the vulnerable method
                    </NxCheckbox>
                  </NxTooltip>
                </div>
              </NxFieldset>
            </NxModal.Content>
          </NxStatefulForm>
        </NxModal>
      ) : null}

      {isUnsavedChangesModalOpen ? (
        <UnsavedChangesModal onContinue={closeModal} onClose={closeUnsavedChangesModal} />
      ) : null}
    </>
  );
}
