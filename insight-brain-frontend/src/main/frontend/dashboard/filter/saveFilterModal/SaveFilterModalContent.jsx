/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';

import {
  NxButton,
  NxFontAwesomeIcon,
  NxModal,
  NxRadio,
  NxSubmitMask,
  NxTextInput,
  NxWarningAlert,
  NxLoadError
} from '@sonatype/react-shared-components';
import { initialState, userInput } from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';
import { faSave } from '@fortawesome/free-solid-svg-icons';
import * as PropTypes from 'prop-types';
import {validateMaxLength, validateNonEmpty, hasValidationErrors} from '../../../util/validationUtil';
import { isNil, reject } from 'ramda';
import { DEFAULT_FILTER_NAME } from '../defaultFilter';

const SAVE_MODE_OVERWRITE = 'overwrite';
const SAVE_MODE_SAVE_AS = 'saveAs';

const WARNING_NAME_IN_USE = 'nameInUseWarning';
const WARNING_OVERWRITE = 'overwriteWarning';

export default function SaveFilterModalContent(props) {

  const {
    appliedFilterName,
    saveError,
    saveFilter,
    saveFilterSaving,
    saveFilterSuccess,
    savedFilters,
    setDisplaySaveFilterModal
  } = props;

  const [saveMode, setSaveMode] = useState(appliedFilterName ? SAVE_MODE_OVERWRITE : SAVE_MODE_SAVE_AS);
  const [warning, setWarning] = useState(undefined);
  const [filterName, setFilterName] = useState(initialState(''));

  const trySave = (e) => {
    e.preventDefault();
    if (!isSaveEnabled()) {
      return;
    }
    else if (warning) {
      // if a warning is already up and the user hit Continue, then go ahead and save
      doSave();
    }
    else {
      if (saveMode === SAVE_MODE_OVERWRITE) {
        setWarning(WARNING_OVERWRITE);
      }
      else {
        const duplicate = savedFilters.some(filter => filterName.value === filter.name);

        if (duplicate) {
          setWarning(WARNING_NAME_IN_USE);
        }
        else {
          // no warning needed when creating a new filter with unused name
          doSave();
        }
      }
    }
  };

  const validateIsNotDefault = val => val === DEFAULT_FILTER_NAME ? 'Can not overwrite Default filter' : null;

  const validateNameChange = (val) => reject(isNil,
      [validateNonEmpty(val), validateMaxLength(60, val), validateIsNotDefault(val)]);

  const filterNameChangeHandler =
      (newValue) => setFilterName(userInput(validateNameChange, newValue));

  const getFilterNameToSave = () => {
    return saveMode === SAVE_MODE_OVERWRITE ? appliedFilterName : filterName.value;
  };

  const doSave = () => {
    saveFilter(getFilterNameToSave());
  };

  const onCancel = () => {
    if (warning === undefined) {
      setDisplaySaveFilterModal(false);
    }
    else {
      setWarning();
    }
  };

  // Save is enabled if we are overwriting the existing filter or if the text box is valid
  const isSaveEnabled = () =>
    saveMode === SAVE_MODE_OVERWRITE || (!filterName.isPristine && !hasValidationErrors(filterName.validationErrors));

  const headerLabel = warning === undefined ? 'Save Filter' :
    warning === WARNING_OVERWRITE ? 'Overwrite Filter' :
      warning === WARNING_NAME_IN_USE ? 'Name in Use' :
        '';

  const warningContentMap = {
    [WARNING_OVERWRITE]: `You are about to permanently overwrite ${appliedFilterName}. This action cannot be undone.`,
    [WARNING_NAME_IN_USE]: '"' + filterName.value + '" is already in use. Continuing will permanently ' +
      'overwrite ' + filterName.value + '. This action cannot be undone.'
  };

  const warningContent =
    <NxWarningAlert id="save-filter-confirmation">
      <span>{ warningContentMap[warning] }</span>
    </NxWarningAlert>;

  const formContent =
    <fieldset className="nx-fieldset">
      <legend className="nx-legend">Choose an Option</legend>
      <NxRadio id="dashboard-filter-overwrite"
               name="saveMode"
               isChecked={saveMode === SAVE_MODE_OVERWRITE}
               onChange={setSaveMode}
               value={SAVE_MODE_OVERWRITE}
               disabled={appliedFilterName == null}>
        save (overwrite{appliedFilterName ? ' ' + appliedFilterName : ''})
      </NxRadio>
      <NxRadio id="dashboard-filter-save-as"
               name="saveMode"
               isChecked={saveMode === SAVE_MODE_SAVE_AS}
               onChange={setSaveMode}
               value={SAVE_MODE_SAVE_AS}>
        save as…
      </NxRadio>
      {
        saveMode === SAVE_MODE_SAVE_AS &&
        <div id="filter-name-section">
          <NxTextInput {...filterName}
                       validatable
                       autoFocus
                       onChange={filterNameChangeHandler}/>
        </div>
      }
    </fieldset>;

  return (
    <NxModal id="save-filter-modal" onClose={onCancel}>
      <form className="nx-form" onSubmit={trySave} noValidate>
        { (saveFilterSaving || saveFilterSuccess) &&
          <NxSubmitMask message="Saving…" success={saveFilterSuccess} /> }
        <header className="nx-modal-header">
          <h2 className="nx-h2">
            <NxFontAwesomeIcon icon={faSave}/>
            <span>{headerLabel}</span>
          </h2>
        </header>
        <div className="nx-modal-content">
          { warning ? warningContent : formContent }
        </div>
        <footer className="nx-footer">
          { saveError &&
            <NxLoadError error={saveError} retryHandler={trySave} titleMessage="An error occurred saving data." />
          }
          <div className="nx-btn-bar">
            <NxButton id="save-filter-modal-cancel-button" type="button" onClick={onCancel}>
              Cancel
            </NxButton>
            { !saveError &&
              <NxButton variant="primary"
                        id="save-filter-modal-continue-button"
                        disabled={!isSaveEnabled()}
                        type="submit">
                { warning ? 'Continue' : 'Save' }
              </NxButton>
            }
          </div>
        </footer>
      </form>
    </NxModal>
  );
}

SaveFilterModalContent.propTypes = {
  appliedFilterName: PropTypes.string,
  saveError: PropTypes.string,
  saveFilter: PropTypes.func,
  saveFilterSaving: PropTypes.bool,
  saveFilterSuccess: PropTypes.bool,
  setDisplaySaveFilterModal: PropTypes.func,
  savedFilters: PropTypes.array
};
