/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import classNames from 'classnames';
import {
  NxFieldset,
  NxFontAwesomeIcon,
  NxModal,
  NxStatefulForm,
  NxRadio,
  NxTextInput,
  NxWarningAlert,
} from '@sonatype/react-shared-components';
import { initialState, userInput } from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';
import { faSave } from '@fortawesome/free-solid-svg-icons';
import * as PropTypes from 'prop-types';
import { validateMaxLength, validateNonEmpty, validateNameCharacters } from '../../../util/validationUtil';
import { isNil, reject } from 'ramda';
import { DEFAULT_FILTER_NAME } from '../defaultFilter';
import { WARNING_OVERWRITE, WARNING_NAME_IN_USE } from '../manageFiltersReducer';
import useEscapeKeyStack from '../../../react/useEscapeKeyStack';

const SAVE_MODE_OVERWRITE = 'overwrite';
const SAVE_MODE_SAVE_AS = 'saveAs';

export default function SaveFilterModalContent(props) {
  const {
    appliedFilterName,
    existingDuplicateFilterName,
    saveError,
    saveFilter,
    saveFilterMaskState,
    saveFilterWarning,
    cancelSaveFilter,
    defaultFilterName = DEFAULT_FILTER_NAME,
    maxNameLength = 60,
    saveAsLabel = 'Save as…',
    additionalFooterBtns,
  } = props;

  const validateIsNotDefault = (val) =>
    val === defaultFilterName ? `Can not overwrite ${defaultFilterName} filter` : null;

  const validateNameChange = (val) =>
    reject(isNil, [
      validateNonEmpty(val),
      validateMaxLength(maxNameLength, val),
      validateIsNotDefault(val),
      validateNameCharacters(val),
    ]);

  const [saveMode, setSaveMode] = useState(appliedFilterName ? SAVE_MODE_OVERWRITE : SAVE_MODE_SAVE_AS);
  const [filterName, setFilterName] = useState(initialState('', validateNameChange));
  useEscapeKeyStack(true, cancelSaveFilter);

  const trySave = (e) => {
    e?.preventDefault();

    saveFilter({
      name: getFilterNameToSave(),
      isOverwriting: saveMode === SAVE_MODE_OVERWRITE,
    });
  };

  const filterNameChangeHandler = (newValue) => setFilterName(userInput(validateNameChange, newValue));

  const getFilterNameToSave = () => {
    return saveMode === SAVE_MODE_OVERWRITE ? appliedFilterName : filterName.trimmedValue;
  };

  const modalClassNames = classNames({
    'enterprise-reporting-save-filter-modal': defaultFilterName !== DEFAULT_FILTER_NAME,
  });

  const headerLabel =
    saveFilterWarning == null
      ? 'Save Filter'
      : saveFilterWarning === WARNING_OVERWRITE
      ? 'Overwrite Filter'
      : saveFilterWarning === WARNING_NAME_IN_USE
      ? 'Name in Use'
      : '';

  const overwritingAs =
    existingDuplicateFilterName !== filterName.trimmedValue ? ` as "${filterName.trimmedValue}"` : '';

  const nameInUseWarning =
    `"${existingDuplicateFilterName}" is already in use. ` +
    `Continuing will permanently overwrite "${existingDuplicateFilterName}"${overwritingAs}. ` +
    'This action cannot be undone.';

  const warningContentMap = {
    [WARNING_OVERWRITE]: `You are about to permanently overwrite ${appliedFilterName}. This action cannot be undone.`,
    [WARNING_NAME_IN_USE]: nameInUseWarning,
  };

  const warningContent = (
    <NxWarningAlert id="save-filter-confirmation">
      <span>{warningContentMap[saveFilterWarning]}</span>
    </NxWarningAlert>
  );

  const formContent = (
    <NxFieldset label="Choose an Option" isRequired>
      <NxRadio
        id="dashboard-filter-overwrite"
        name="saveMode"
        isChecked={saveMode === SAVE_MODE_OVERWRITE}
        onChange={setSaveMode}
        value={SAVE_MODE_OVERWRITE}
        disabled={appliedFilterName == null}
      >
        Save (overwrite{appliedFilterName ? ' ' + appliedFilterName : ''})
      </NxRadio>
      <NxRadio
        id="dashboard-filter-save-as"
        name="saveMode"
        isChecked={saveMode === SAVE_MODE_SAVE_AS}
        onChange={setSaveMode}
        value={SAVE_MODE_SAVE_AS}
      >
        {saveAsLabel}
      </NxRadio>
      {saveMode === SAVE_MODE_SAVE_AS && (
        <div id="filter-name-section" className="iq-filter-name-section">
          <NxTextInput {...filterName} validatable autoFocus onChange={filterNameChangeHandler} />
        </div>
      )}
    </NxFieldset>
  );

  return (
    <NxModal id="save-filter-modal" className={modalClassNames}>
      <NxStatefulForm
        onSubmit={trySave}
        onCancel={cancelSaveFilter}
        submitBtnText={saveFilterWarning ? 'Continue' : 'Save'}
        validationErrors={saveMode === SAVE_MODE_SAVE_AS ? validateNameChange(filterName.value) : null}
        submitError={saveError}
        submitErrorTitleMessage="An error occurred saving data."
        submitMaskState={saveFilterMaskState}
        submitMaskMessage="Saving…"
        additionalFooterBtns={additionalFooterBtns}
      >
        <header className="nx-modal-header">
          <h2 className="nx-h2">
            <NxFontAwesomeIcon icon={faSave} />
            <span>{headerLabel}</span>
          </h2>
        </header>
        <div className="nx-modal-content">{saveFilterWarning ? warningContent : formContent}</div>
      </NxStatefulForm>
    </NxModal>
  );
}

SaveFilterModalContent.propTypes = {
  appliedFilterName: PropTypes.string,
  existingDuplicateFilterName: PropTypes.string,
  saveError: PropTypes.string,
  saveFilter: PropTypes.func,
  saveFilterMaskState: PropTypes.bool,
  saveFilterWarning: PropTypes.string,
  cancelSaveFilter: PropTypes.func,
  defaultFilterName: PropTypes.string,
  maxNameLength: PropTypes.number,
  saveAsLabel: PropTypes.string,
  additionalFooterBtns: PropTypes.element,
};
