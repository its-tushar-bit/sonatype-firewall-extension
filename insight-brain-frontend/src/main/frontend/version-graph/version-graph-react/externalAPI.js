/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { setCurrentCoordinates, fetchComponentData } from './slices/componentsSlice';
import { setError as setGlobalError } from './slices/globalSlice';
import { selectSelectedApplication } from './slices/applicationsSlice';
import store from './store';

/**
 * Sets the component that the graph should be for and the current version of that component
 * @param {string} componentType - Type of component (format)
 * @param {Object} coordinates - Component coordinates
 * @param {Object} properties - Additional properties
 */
async function setCoordinates(componentType, coordinates, properties = {}) {
  try {
    // Dispatch action to update state
    store.dispatch(setCurrentCoordinates({ componentType, coordinates, properties }));

    // Fetch component data if we have a selected application
    const state = store.getState();
    const selectedApplication = selectSelectedApplication(state);

    if (selectedApplication) {
      await store.dispatch(fetchComponentData(selectedApplication.publicId));
    }
  } catch (e) {
    console.error('Error in setCoordinates:', e);
  }
}

/**
 * Deprecated method to set current coordinate by GAV.
 * @param {Object} arg - object containing at least groupId, artifactId, and version properties, and optionally
 * extension, classifier, matchState, proprietary, hash.
 */
function setGav(arg) {
  setCoordinates('maven', arg);
}

/**
 * Sets a global error message for display within the version-graph
 * @param {Object} arg - Error object
 */
function setError(arg) {
  try {
    store.dispatch(setGlobalError(arg.errorMessage || 'Unknown error'));
  } catch (e) {
    console.error('Error in setError:', e);
  }
}

/**
 * This is now a no-op. The intent is to open the viewdetails view in a new tab when the version graph is double
 * clicked, but that can be handled within this bundle without assistance from an externally-specified listener
 * function.
 */
function registerViewDetailsListener() {}

/**
 * This is now a no-op. The intent is to open the viewdetails view in a new tab when the version graph is double
 * clicked, but that can be handled within this bundle without assistance from an externally-specified listener
 * function.
 */
function registerCoordsViewDetailsListener() {}

window.Insight = Object.assign(window.Insight ?? {}, {
  setCoordinates,
  setGav,
  setError,
  registerCoordsViewDetailsListener,
  registerViewDetailsListener,
});
