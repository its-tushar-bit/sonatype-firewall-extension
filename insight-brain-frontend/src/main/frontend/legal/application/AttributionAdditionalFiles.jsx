/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useRef, useState } from 'react';
import { inc } from 'ramda';
import { NxButton, NxFieldset, NxFontAwesomeIcon, NxModal, NxWarningAlert } from '@sonatype/react-shared-components';
import { faTrash } from '@fortawesome/pro-solid-svg-icons';
import * as PropTypes from 'prop-types';

const NO_FILE_TO_DELETE_INDEX = -1;
const FORM_FIELD_NAME = 'noticeFiles';

/**
 * The file input section of the Attribution Report Form. React makes file inputs difficult, and this
 * component needs to be somewhat complex coordination of a dynamic collection of inputs, as opposed to just one,
 * so it was found to be cleanest to manage the inputs manually outside of react. One major factor was that it
 * is crucial that a given input stay the same instance throughout its life - if it gets removed and recreated, its
 * state is lost
 */
export default function AttributionAdditionalFiles(props) {
  const { onFilesChange } = props;
  // a div which holds all inputs where files have been selected
  const usedInputContainerRef = useRef(),
    // a div which holds an input where a file selection has not yet been made
    freshInputContainerRef = useRef();

  // This is only tracked to trigger react re-renders after handling the file input change event
  const [, setFileInputChangeCount] = useState(0),
    [fileIndexToDelete, setFileIndexToDelete] = useState(NO_FILE_TO_DELETE_INDEX);

  useEffect(createNewInput, []);

  function createNewInput() {
    const newInput = document.createElement('input');

    newInput.accept = 'text/plain';
    newInput.type = 'file';
    newInput.addEventListener('change', onFileInputChange);

    freshInputContainerRef.current.appendChild(newInput);
  }

  function onFileInputChange(evt) {
    evt.target.name = FORM_FIELD_NAME;

    // once a selection is made, move that input to the usedInputContainer and create a new fresh one
    usedInputContainerRef.current.appendChild(evt.target);
    createNewInput();
    setFileInputChangeCount(inc);
    onFilesChange(usedInputContainerRef.current.children);
  }

  function openDeleteFilePrompt(index) {
    setFileIndexToDelete(index);
  }

  function closeDeleteFilePrompt() {
    setFileIndexToDelete(NO_FILE_TO_DELETE_INDEX);
  }

  function openFileExplorer() {
    freshInputContainerRef.current.children[0].click();
  }

  function removeFile(index) {
    usedInputContainerRef.current.children[index].remove();

    // note that aside from closing the modal, this state update is crucial to having the effects of the line above
    // reflected in the UI
    setFileIndexToDelete(NO_FILE_TO_DELETE_INDEX);
    onFilesChange(usedInputContainerRef.current.children);
  }

  const fileInputs = usedInputContainerRef.current ? Array.from(usedInputContainerRef.current.children) : [];

  return (
    <NxFieldset label="Additional Notice Files">
      <ul className="nx-list">
        {fileInputs.map((input, index) => (
          <li className="nx-list__item" key={index}>
            <span className="nx-list__text">{input.files[0].name}</span>
            <div className="nx-list__actions">
              <NxButton
                title="Delete file"
                type="button"
                variant="icon-only"
                onClick={() => openDeleteFilePrompt(index)}
              >
                <NxFontAwesomeIcon icon={faTrash} />
              </NxButton>
            </div>
          </li>
        ))}
      </ul>
      <div ref={usedInputContainerRef} />
      <div ref={freshInputContainerRef} />
      {fileIndexToDelete !== NO_FILE_TO_DELETE_INDEX && (
        <NxModal
          variant="narrow"
          id="nx-modal-narrow-example"
          onCancel={closeDeleteFilePrompt}
          aria-labelledby="modal-narrow-header"
        >
          <header className="nx-modal-header">
            <h2 className="nx-h2" id="modal-narrow-header">
              <NxFontAwesomeIcon icon={faTrash} />
              <span>Delete file</span>
            </h2>
          </header>
          <div className="nx-modal-content">
            <NxWarningAlert>
              Clicking &apos;Delete&apos; will permanently remove this file. Are you sure you want to delete this file?
            </NxWarningAlert>
          </div>
          <footer className="nx-footer">
            <div className="nx-btn-bar">
              <NxButton type="button" onClick={closeDeleteFilePrompt}>
                Close
              </NxButton>
              <NxButton type="button" variant="primary" onClick={() => removeFile(fileIndexToDelete)}>
                Delete
              </NxButton>
            </div>
          </footer>
        </NxModal>
      )}
      <div className="nx-form-row">
        <div className="nx-btn-bar">
          <NxButton
            onClick={openFileExplorer}
            variant="tertiary"
            type="button"
            className="attribution-report-form__nx-button--apparent-file-input"
          >
            Attach Files
          </NxButton>
        </div>
      </div>
    </NxFieldset>
  );
}

AttributionAdditionalFiles.propTypes = {
  onFilesChange: PropTypes.func.isRequired,
};
