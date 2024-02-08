/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useState } from 'react';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxStatefulForm,
  NxFormGroup,
  NxModal,
  NxTextInput,
  nxTextInputStateHelpers,
  NxToggle,
  NxFormSelect,
} from '@sonatype/react-shared-components';
import { availableScopesPropType, componentPropType, licenseObligationPropType } from '../advancedLegalPropTypes';
import * as PropTypes from 'prop-types';
import { faPlus } from '@fortawesome/pro-solid-svg-icons';
import { pathSet } from '../../util/jsUtil';
import ObligationStatusComponent from '../shared/ObligationStatusComponent';
import { createScopeOption } from '../legalUtility';

const { initialState, userInput } = nxTextInputStateHelpers;

export default function OriginalSourcesForm(props) {
  const {
    component,
    availableScopes,
    saveOriginalSourceError,
    existingObligation,
    submitMaskState,

    //actions
    saveOriginalSourcesOverride,
    setDisplayOriginalSourcesOverrideModal,
    setObligationStatus,
    setObligationScope,
  } = props;

  const [sources, setSources] = useState(
    component.licenseLegalData.sourceLinks.map(({ content, ...rest }) => ({
      content: initialState(content),
      ...rest,
    }))
  );

  const defaultScope = () =>
    existingObligation && existingObligation.ownerId !== null ? existingObligation.ownerId : 'ROOT_ORGANIZATION_ID';

  const [scope, setScope] = useState(defaultScope());

  const printSourceTextInput = (index, source) => (
    <td>
      <NxTextInput
        id={'source-' + index}
        {...source.content}
        maxLength="1000"
        onChange={onOriginalSourceContentChange(index)}
        className="legal-modal-override-input-content"
        disabled={source.status === 'disabled'}
      />
    </td>
  );

  const createFormRowItem = (source, index) => (
    <tr key={index}>
      {printSourceTextInput(index, source)}
      <td>
        <NxToggle
          inputId={'source-status-toggle-' + index}
          onChange={() => onOriginalSourceStatusChange(index, source)}
          className="nx-toggle--no-gap original-sources-override-status-toggle"
          isChecked={source.status === 'enabled'}
        >
          {source.status === 'enabled' ? 'Included' : 'Excluded'}
        </NxToggle>
      </td>
    </tr>
  );

  const isSourcePresent = () => sources && sources.length > 0;

  const onOriginalSourceContentChange = (index) => (content) => {
    setSources(pathSet([index, 'content'], userInput(null, content), sources));
  };

  const onOriginalSourceStatusChange = (index, source) =>
    setSources(pathSet([index, 'status'], flipStatus(source.status), sources));

  const setOriginalSourceScope = (event) => setScope(event.target.value);

  const setObligationScopeIfNeeded = (event) => {
    if (existingObligation && existingObligation.status !== existingObligation.originalStatus) {
      setObligationScope({
        name: existingObligation.name,
        value: event.target.value,
      });
    }
  };

  const flipStatus = (status) => (status === 'enabled' ? 'disabled' : 'enabled');

  const addNewSourceLink = () => {
    setSources([
      ...sources,
      {
        id: null,
        content: initialState(''),
        status: 'enabled',
      },
    ]);
  };

  const trySave = () => {
    saveOriginalSourcesOverride({
      sources: sources
        .filter((s) => s.id !== null || s.content.trimmedValue.length !== 0)
        .map(({ content, ...rest }) => ({
          content: content.trimmedValue,
          ...rest,
        })),
      existingObligation,
      areSourcesDirty: areSourcesDirty(),
      isObligationDirty: isObligationDirty(),
    });
  };

  function areSourcesDirty() {
    for (let i = component.licenseLegalData.sourceLinks.length; i < sources.length; i++) {
      if (sources[i].content.trimmedValue.length !== 0) {
        return true;
      }
    }
    if (defaultScope() !== scope) {
      return true;
    }
    for (let i = 0; i < component.licenseLegalData.sourceLinks.length; i++) {
      if (
        component.licenseLegalData.sourceLinks[i].content !== sources[i].content.trimmedValue ||
        component.licenseLegalData.sourceLinks[i].status !== sources[i].status
      ) {
        return true;
      }
    }
    return false;
  }

  function isObligationDirty() {
    return existingObligation && existingObligation.status !== existingObligation.originalStatus;
  }

  const resetExistingObligation = () => {
    if (existingObligation) {
      setObligationStatus({
        name: existingObligation.name,
        value: existingObligation.originalStatus,
      });
      setObligationScope({
        name: existingObligation.name,
        value: existingObligation.originalScope,
      });
    }
  };

  const onObligationChange = (value) => {
    setObligationStatus({ name: existingObligation.name, value });
    if (value === existingObligation.originalStatus) {
      setObligationScope({
        name: existingObligation.name,
        value: existingObligation.originalScope,
      });
    } else {
      setObligationScope({ name: existingObligation.name, value: scope });
    }
  };

  const submitError = saveOriginalSourceError || existingObligation?.error;

  return (
    <NxModal
      id="edit-original-sources-attribution-modal"
      aria-labelledby="original-sources-modal-header"
      onClose={() => {
        setDisplayOriginalSourcesOverrideModal(false);
        resetExistingObligation();
      }}
      variant="wide"
    >
      <NxStatefulForm
        onCancel={() => {
          setDisplayOriginalSourcesOverrideModal(false);
          resetExistingObligation();
        }}
        submitMaskState={submitMaskState}
        submitBtnText="Save"
        submitError={submitError}
        onSubmit={trySave}
        validationErrors={submitError ? null : areSourcesDirty() || isObligationDirty() ? null : 'No modifications'}
      >
        <header className="nx-modal-header">
          <h2 className="nx-h2" id="original-sources-modal-header">
            {isSourcePresent() ? 'Edit Sources' : 'Add Original Sources'}
          </h2>
        </header>
        <div className="nx-modal-content">
          <table className="legal-file-override-table">
            <thead>
              <tr>
                <th id="edit-original-sources-source-text-title">Original Source Link</th>
                <th id="edit-original-sources-source-status-title">Attribution Report status</th>
              </tr>
            </thead>
            <tbody>{sources.map(createFormRowItem)}</tbody>
          </table>

          <div className="nx-form-row">
            <div className="nx-btn-bar">
              <NxButton type="button" id="add-source-link" variant="tertiary" onClick={addNewSourceLink}>
                <NxFontAwesomeIcon icon={faPlus} />
                <span>Add Link</span>
              </NxButton>
            </div>
          </div>
          {existingObligation && (
            <Fragment>
              <ObligationStatusComponent existingObligation={existingObligation} onChange={onObligationChange} />
              <NxFormGroup
                className="legal-modal-scope-selection-group"
                label="Scope"
                sublabel="Apply changes to"
                isRequired
              >
                <NxFormSelect
                  className="nx-form-select--long"
                  id="edit-original-sources-scope-selection"
                  value={scope}
                  onChange={(event) => {
                    setOriginalSourceScope(event);
                    setObligationScopeIfNeeded(event);
                  }}
                >
                  {availableScopes.values.map(createScopeOption)}
                </NxFormSelect>
              </NxFormGroup>
            </Fragment>
          )}
        </div>
      </NxStatefulForm>
    </NxModal>
  );
}

OriginalSourcesForm.propTypes = {
  setObligationStatus: PropTypes.func.isRequired,
  setObligationScope: PropTypes.func.isRequired,
  component: componentPropType,
  availableScopes: availableScopesPropType,
  saveOriginalSourcesOverride: PropTypes.func,
  saveOriginalSourceError: PropTypes.string,
  submitMaskState: PropTypes.bool,
  existingObligation: licenseObligationPropType,
  setDisplayOriginalSourcesOverrideModal: PropTypes.func.isRequired,
};
