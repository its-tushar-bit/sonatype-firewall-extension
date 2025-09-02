/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';
import { NxButton, NxButtonBar, NxFieldset, NxFooter, NxH2, NxModal, NxRadio } from '@sonatype/react-shared-components';
import { selectDisplayTheme } from './displayThemeSelectors';
import { actions as displayThemeActions } from 'MainRoot/configuration/displayTheme/displayThemeSlice';

export default function DisplayThemeModal({ onClose }) {
  return (
    <NxModal id="iq-display-theme-modal" onCancel={onClose} aria-labelledby="iq-display-theme-modal-header-text">
      <NxModal.Header id="iq-display-theme-modal-header-text">
        <NxH2>Edit Display Theme</NxH2>
      </NxModal.Header>
      <NxModal.Content>
        <DisplayThemeModalContents />
      </NxModal.Content>
      <NxFooter>
        <NxButtonBar>
          <NxButton onClick={onClose}>Close</NxButton>
        </NxButtonBar>
      </NxFooter>
    </NxModal>
  );
}

function DisplayThemeModalContents() {
  const dispatch = useDispatch();
  const displayTheme = useSelector(selectDisplayTheme);

  const handleChange = (val) => {
    dispatch(displayThemeActions.setDisplayTheme(val));
  };

  return (
    <NxFieldset label="Display Theme">
      <NxRadio
        id="iq-system-setting-radio"
        name="theme"
        value="system"
        onChange={handleChange}
        isChecked={displayTheme === 'system'}
      >
        System Setting
      </NxRadio>
      <NxRadio
        id="iq-dark-mode-radio"
        name="theme"
        value="dark"
        onChange={handleChange}
        isChecked={displayTheme === 'dark'}
      >
        Dark Mode
      </NxRadio>
      <NxRadio
        id="iq-light-mode-radio"
        name="theme"
        value="light"
        onChange={handleChange}
        isChecked={displayTheme === 'light'}
      >
        Light Mode
      </NxRadio>
    </NxFieldset>
  );
}

DisplayThemeModal.propTypes = {
  onClose: PropTypes.func.isRequired,
};
