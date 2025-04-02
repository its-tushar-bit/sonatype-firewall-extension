/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useRef } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { isNil } from 'ramda';
import UnsavedChangesModal from '../../unsavedChangesModal/UnsavedChangesModal';
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
  NxP
}
  from '@sonatype/react-shared-components';
import { selectIsNewScanProcessEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { actions } from './autoWaiverModalSlice';
import { faInfoCircle } from '@fortawesome/pro-solid-svg-icons';
import { selectAutoWaiverModalSlice, selectAutoWaiverDetails } from './autoWaiverModalSelectors';
import ThreatDropdownSelector from 'MainRoot/react/ThreatDropdownSelector';
import AutoWaiverScopeDropdownSelector from './AutoWaiverScopeDropdownSelector';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';
import './_autoWaiverModal.scss';

const UPGRADE_PATH_IS_NOT_AVAILABLE_TEXT = 'Upgrade Path is not available';
const VULNERABILITY_IS_NOT_REACHABLE_TEXT = 'Vulnerability is not reachable';

export default function AutoWaiverModal() {
  const dispatch = useDispatch();

  const {
    isModalOpen,
    isEditMode,
    submitMaskState,
    submitError,
    isDirty,
    isUnsavedChangesModalOpen,
  } = useSelector(selectAutoWaiverModalSlice);
  const isNewScanProcessEnabled = useSelector(selectIsNewScanProcessEnabled);
  const waiverModal = useSelector(selectAutoWaiverDetails);
  const reachability = isNewScanProcessEnabled ? waiverModal?.reachability ?? false : false;
  const pathForward = waiverModal?.pathForward ?? false;
  const threatLevel = waiverModal?.threatLevel ?? 7;
  const scope = waiverModal?.scope ?? 'any';

  const closeModalWithCheck = () => dispatch(actions.closeModal({ isDirty }));
  const closeUnsavedChangesModal = () => dispatch(actions.closeUnsavedChangesModal());
  const closeModal = () => dispatch(actions.closeModal());
  const createAutoWaiver = () => dispatch(actions.createAutoWaiver());
  const saveAutoWaiver = () => dispatch(actions.saveAutoWaiver());
  const setThreatLevel = (val) => dispatch(actions.setThreatLevel(val));
  const setScope = (val) => dispatch(actions.setScope(val));

  const additionalFooterButtons = (
    <NxButton variant="tertiary" type="button" className="nx-form__cancel-btn" onClick={closeModalWithCheck}>
      Cancel
    </NxButton>
  );

  const getHeaderTitle = () => {
    return `${isEditMode ? 'Edit ' : 'New '} Auto-Waiver`
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
    if (isNil(waiverModal?.threatLevel)) {
      setThreatLevel(7);
      dispatch(actions.setIsDirty(false)); // reset dirty flag since this isn't a user-made change
    }
  }, [waiverModal.threatLevel]);

  useEffect(() => {
    return () => {
      closeModal();
    };
  }, []);

  return (
    <>
      {isModalOpen ? (
        <NxModal id="autowaiver-editor" onCancel={closeModalWithCheck}>
          <NxStatefulForm
            onSubmit={isEditMode ? saveAutoWaiver : createAutoWaiver}
            submitMaskState={submitMaskState}
            submitBtnText={isEditMode ? 'Update':'Create'}
            submitError={submitError}
            validationErrors={validationError()}
            additionalFooterBtns={additionalFooterButtons}
          >
            <NxModal.Header>
              <NxH2>
                {getHeaderTitle()}
              </NxH2>
            </NxModal.Header>
            <NxModal.Content>

              <NxP>
                Automatically waive policy violations when the following conditions are met:
              </NxP>

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
                  />
                  {' '}of the below are true:
                </NxH3>
              </div>

              <NxFieldset label="" className="iq-auto-waiver-modal-fieldset">
                <div className="iq-auto-waiver-modal-fieldset__item">
                  <NxH4>{UPGRADE_PATH_IS_NOT_AVAILABLE_TEXT}</NxH4>
                  <NxTooltip title={waiverModal?.isInherited ? 'Inheriting from parent organization' : ''}>
                    <NxCheckbox
                        onChange={() => dispatch(actions.toggleCheckboxNoUpgradePath())}
                        isChecked={pathForward || false}
                        disabled={waiverModal?.isInherited}
                    >
                      No newer, non-violating component version is available
                    </NxCheckbox>
                  </NxTooltip>
                </div>
                {isNewScanProcessEnabled && (
                    <div className="iq-auto-waiver-modal-fieldset__item">
                     <div className="iq-auto-waiver-modal-fieldset__item-reachability-header">
                        <NxH4>{VULNERABILITY_IS_NOT_REACHABLE_TEXT}</NxH4>
                        <NxTooltip title="Reachability Analysis must be enabled via Jenkins or Sonatype CLI">
                          <NxFontAwesomeIcon
                              className="iq-auto-waiver-modal-fieldset__item-icon"
                              data-testid="auto-waiver-modal-reachability-icon"
                              icon={faInfoCircle}
                          />
                        </NxTooltip>
                      </div>
                      <NxTooltip title={waiverModal?.isInherited ? 'Inheriting from parent organization' : ''}>
                        <NxCheckbox
                            onChange={() => dispatch(actions.toggleCheckboxReachability())}
                            isChecked={reachability || false}
                            disabled={waiverModal?.isInherited}
                        >
                          Application does not execute any calls to the vulnerable method
                        </NxCheckbox>
                      </NxTooltip>
                    </div>
                )}
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
