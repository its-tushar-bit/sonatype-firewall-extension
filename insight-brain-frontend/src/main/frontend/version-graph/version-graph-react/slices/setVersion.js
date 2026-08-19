/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectCurrentComponentIdentifier,
  selectComponentProperties,
  selectSelectedVersion,
  setSelectedVersion,
} from './componentsSlice';
import { selectSelectedApplication } from './applicationsSlice';
import { fetchComponentDetails } from './componentDetailsSlice';

/**
 * Action creator for selecting a version in the graph. Defined here in a separate file because it relies on multiple
 * other slices and would otherwise create a circular dependency.
 */
export function setVersion(version) {
  return async (dispatch, getState) => {
    const state = getState();
    const applicationId = selectSelectedApplication(state)?.publicId;
    const selectedVersion = selectSelectedVersion(state);

    if (selectedVersion === version || !applicationId) {
      return;
    }

    // Set the selected version in the state
    dispatch(setSelectedVersion(version));

    // Fetch component data for the new version
    await dispatch(
      fetchComponentDetails({
        currentComponentIdentifier: selectCurrentComponentIdentifier(state),
        hash: selectComponentProperties(state)?.hash,
        selectedVersion: version,
        applicationId: applicationId,
      })
    );
  };
}
