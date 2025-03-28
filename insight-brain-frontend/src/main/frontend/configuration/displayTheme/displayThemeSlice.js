/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSlice } from '@reduxjs/toolkit';
import {
  getDisplayTheme as getDisplayThemeOnStorage,
  setDisplayTheme as setDisplayThemeOnStorage,
  onDisplayThemeChange as onDisplayThemeStorageChange,
} from 'MainRoot/util/preferenceStore';

const REDUCER_NAME = 'displayTheme';

export const validDisplayThemes = ['system', 'dark', 'light'];

//The whole state here is just a single string, not an object
const initialState = 'light';

export const displayThemeSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setDisplayThemeState: (state, { payload }) => payload,
  },
});

const { setDisplayThemeState: setDisplayThemeStateAction } = displayThemeSlice.actions;

export const setDisplayTheme = (theme) => (dispatch) => {
  if (!validDisplayThemes.includes(theme)) {
    throw new Error(`Invalid theme: ${theme}`);
  }

  setDisplayThemeOnStorage(theme);
  dispatch(setDisplayThemeStateAction(theme));
};

/**
 * Initialize the display theme state from local storage and listen for changes to it.
 */
const initialize = () => (dispatch) => {
  const storedTheme = getDisplayThemeOnStorage();
  if (storedTheme) {
    dispatch(setDisplayTheme(storedTheme));
  }

  onDisplayThemeStorageChange((theme) => {
    dispatch(setDisplayTheme(theme));
  });
};

export default displayThemeSlice.reducer;

export const actions = { initialize, setDisplayTheme };
