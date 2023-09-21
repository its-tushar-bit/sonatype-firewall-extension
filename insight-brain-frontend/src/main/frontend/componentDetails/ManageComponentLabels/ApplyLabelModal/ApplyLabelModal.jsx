/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import { find, propEq } from 'ramda';
import ComponentLabelTag from 'MainRoot/react/tag/ComponentLabelTag';
import IqScopeDropdown from 'MainRoot/react/iqScopeDropdown/IqScopeDropdown';
import { NxModal, NxStatefulForm, NxFieldset } from '@sonatype/react-shared-components';

export default function ApplyLabelModal({
  applicableLabelScopes = [],
  componentName,
  cancelApplyLabelModal,
  labelScopeToSave,
  loadApplicableLabelScopes,
  loadError,
  loading,
  saveApplyLabelScope,
  saveLabelError,
  selectedLabelDetails,
  setLabelScopeToSave,
  showApplyLabelModal,
  submitMaskState,
}) {
  useEffect(() => {
    if (showApplyLabelModal) {
      loadApplicableLabelScopes();
    }
  }, [showApplyLabelModal]);

  const hasValidationError =
    labelScopeToSave.labelScopeId === undefined || labelScopeToSave.labelScopeType === undefined
      ? 'Select a scope to apply'
      : undefined;

  if (!showApplyLabelModal) {
    return null;
  }

  const handleScopeChange = (id) => {
    const target = find(propEq('id', id), applicableLabelScopes);
    setLabelScopeToSave({ labelScopeType: target.type, labelScopeId: target.id });
  };

  const extractScopeOptionText = ({ label, name }) => {
    switch (label) {
      case 'Repository_container':
        return name;
      case 'Repository_manager':
        return `Repository Manager - ${name}`;
      default:
        return `${label} - ${name}`;
    }
  };

  return (
    <NxModal id="iq-apply-label-modal" onClose={cancelApplyLabelModal} aria-labelledby="iq-apply-label-modal__heading">
      <NxStatefulForm
        className="nx-form"
        onSubmit={saveApplyLabelScope}
        submitError={saveLabelError}
        onCancel={cancelApplyLabelModal}
        loading={loading}
        loadError={loadError}
        doLoad={loadApplicableLabelScopes}
        validationErrors={hasValidationError}
        submitMaskState={submitMaskState}
        submitMaskMessage="Applying label…"
      >
        <header className="nx-modal-header">
          <h2 className="nx-h2" id="iq-apply-label-modal__heading">
            Apply Label
          </h2>
        </header>
        <div className="nx-modal-content">
          <dl className="nx-read-only">
            <dt className="nx-read-only__label">Component</dt>
            <dd className="nx-read-only__data">{componentName}</dd>
            <dt className="nx-read-only__label">Label</dt>
            <dd className="nx-read-only__data">
              <ComponentLabelTag color={selectedLabelDetails.color}>{selectedLabelDetails.label}</ComponentLabelTag>
            </dd>
          </dl>
          <NxFieldset label="Scope" className="nx-read-only" isRequired>
            <IqScopeDropdown
              id="iq-apply-label-scope"
              onChangeHandler={handleScopeChange}
              availableScopes={applicableLabelScopes}
              getOptionText={extractScopeOptionText}
              currentValue={labelScopeToSave.labelScopeId}
              withHiddenOption
            />
          </NxFieldset>
        </div>
      </NxStatefulForm>
    </NxModal>
  );
}

ApplyLabelModal.propTypes = {
  applicableLabelScopes: PropTypes.array.isRequired,
  cancelApplyLabelModal: PropTypes.func.isRequired,
  componentName: PropTypes.string,
  labelScopeToSave: PropTypes.object,
  loading: PropTypes.bool.isRequired,
  loadError: PropTypes.string,
  loadApplicableLabelScopes: PropTypes.func.isRequired,
  saveApplyLabelScope: PropTypes.func.isRequired,
  saveLabelError: PropTypes.string,
  selectedLabelDetails: PropTypes.object.isRequired,
  setLabelScopeToSave: PropTypes.func.isRequired,
  showApplyLabelModal: PropTypes.bool.isRequired,
  submitMaskState: PropTypes.bool,
};
