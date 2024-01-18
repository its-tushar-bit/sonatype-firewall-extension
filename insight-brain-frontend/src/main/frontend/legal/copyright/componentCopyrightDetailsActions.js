/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { payloadParamActionCreator } from '../../util/reduxUtil';
import axios from 'axios';
import { getCopyrightContextUrl, getCopyrightFileCountUrl, getCopyrightFilePathsUrl } from '../../util/CLMLocation';
import { Messages } from '../../utilAngular/CommonServices';
import { FILE_PATH_PAGE_SIZE, pageOffset } from './copyrightDetailsUtils';
import { loadAvailableScopes, loadComponent, loadComponentByComponentIdentifier } from '../advancedLegalActions';

export const COPYRIGHT_DETAILS_REQUEST = 'COPYRIGHT_DETAILS_REQUEST';
export const COPYRIGHT_DETAILS_FULFILLED = 'COPYRIGHT_DETAILS_FULFILLED';
export const COPYRIGHT_DETAILS_FAILED = 'COPYRIGHT_DETAILS_FAILED';

export const COPYRIGHT_FILE_PATHS_REQUEST = 'COPYRIGHT_FILE_PATHS_REQUEST';
export const COPYRIGHT_FILE_PATHS_FULFILLED = 'COPYRIGHT_FILE_PATHS_FULFILLED';
export const COPYRIGHT_FILE_PATHS_FAILED = 'COPYRIGHT_FILE_PATHS_FAILED';

export const COPYRIGHT_CONTEXT_REQUEST = 'COPYRIGHT_CONTEXT_REQUEST';
export const COPYRIGHT_CONTEXT_FULFILLED = 'COPYRIGHT_CONTEXT_FULFILLED';
export const COPYRIGHT_CONTEXT_FAILED = 'COPYRIGHT_CONTEXT_FAILED';

export const copyrightDetailsRequest = payloadParamActionCreator(COPYRIGHT_DETAILS_REQUEST);
export const copyrightDetailsFulfilled = payloadParamActionCreator(COPYRIGHT_DETAILS_FULFILLED);
export const copyrightDetailsFailed = payloadParamActionCreator(COPYRIGHT_DETAILS_FAILED);

export const copyrightFilePathsRequest = payloadParamActionCreator(COPYRIGHT_FILE_PATHS_REQUEST);
export const copyrightFilePathsFulfilled = payloadParamActionCreator(COPYRIGHT_FILE_PATHS_FULFILLED);
export const copyrightFilePathsFailed = payloadParamActionCreator(COPYRIGHT_FILE_PATHS_FAILED);

export const copyrightContextsRequest = payloadParamActionCreator(COPYRIGHT_CONTEXT_REQUEST);
export const copyrightContextFulfilled = payloadParamActionCreator(COPYRIGHT_CONTEXT_FULFILLED);
export const copyrightContextFailed = payloadParamActionCreator(COPYRIGHT_CONTEXT_FAILED);

export function refreshCopyrightDetails() {
  return (dispatch, getState) => {
    const currentParams = getState().router && getState().router.currentParams;
    return (
      currentParams &&
      currentParams.copyrightIndex &&
      requestLoadCopyrightDetails(dispatch, getState(), currentParams.copyrightIndex)
    );
  };
}

function getFirstFileCopyrightContexts(getState, dispatch, copyrightIndex) {
  return requestLoadCopyrightDetails(dispatch, getState(), copyrightIndex)
    .then(() => {
      const filePaths = getState().componentCopyrightDetails.filePaths;
      if (filePaths.length) {
        return dispatch(loadCopyrightContexts(filePaths[0].filePath));
      }
    })
    .catch((error) => dispatch(copyrightContextFailed({ value: Messages.getHttpErrorMessage(error) })));
}

export function loadComponentAndCopyrightDetails(ownerType, ownerId, hash, copyrightIndex, componentIdentifier) {
  return (dispatch, getState) => {
    const component = getState().advancedLegal.component.component;
    if (!component) {
      dispatch(loadAvailableScopes(ownerType, ownerId));
      const componentPromise = hash
        ? loadComponent(ownerType, ownerId, hash)
        : loadComponentByComponentIdentifier(componentIdentifier, {
            orgOrApp: ownerType,
            ownerId: ownerId,
          });
      return dispatch(componentPromise).then(() => getFirstFileCopyrightContexts(getState, dispatch, copyrightIndex));
    } else {
      return getFirstFileCopyrightContexts(getState, dispatch, copyrightIndex);
    }
  };
}

export function loadFilePathsOnPageUpdate(filePathsPage) {
  return (dispatch, getState) => {
    const { copyright, component, ownerType, ownerPublicId } = extractRoutingParameters(getState());

    const pageNumber = filePathsPage || 0;
    dispatch(copyrightFilePathsRequest({ filePathsPage: pageNumber }));
    if (copyright.originalContentHash) {
      return loadFilePaths(dispatch, ownerType, ownerPublicId, component, copyright, pageNumber);
    } else {
      return dispatch(
        copyrightFilePathsFulfilled({
          filePaths: { filePaths: [], totalFileMatches: 0 },
        })
      );
    }
  };
}

export function unloadCopyrightContext(filePath) {
  return (dispatch, getState) => {
    const { selectedFilePaths } = getState().componentCopyrightDetails;
    selectedFilePaths.splice(selectedFilePaths.indexOf(filePath), 1);
    dispatch(copyrightContextsRequest({ selectedFilePaths }));
  };
}

export function loadCopyrightContexts(filePath) {
  return (dispatch, getState) => {
    const { selectedFilePaths, copyrightContexts } = getState().componentCopyrightDetails;
    dispatch(copyrightContextsRequest({ selectedFilePaths: [...selectedFilePaths, filePath] }));
    const { copyright, component, ownerType, ownerPublicId } = extractRoutingParameters(getState());
    if (!copyrightContexts.find((context) => context.filePath === filePath)) {
      return loadCopyrightContext(
        dispatch,
        ownerType,
        ownerPublicId,
        component,
        copyright.originalContentHash,
        filePath
      );
    }
  };
}

function onCopyrightDetailsLoaded(dispatch, copyrightIndex, component, results) {
  dispatch(
    copyrightDetailsFulfilled({
      copyrightIndex,
      copyright: component.licenseLegalData.copyrights[copyrightIndex],
      filePaths: results.paths.data,
      copyrightFileCounts: results.copyrightCount.data,
    })
  );
}

function requestLoadCopyrightDetails(dispatch, state, copyrightIndex) {
  const { copyright, component, ownerType, ownerPublicId } = extractRoutingParameters(state, copyrightIndex);

  const details = state.componentCopyrightDetails;

  const copyrightFileCountsNotLoaded = !details || !details.copyrightFileCounts;

  dispatch(
    copyrightDetailsRequest({
      copyrightIndex,
      copyright,
      loadingCopyrightFileCounts: copyrightFileCountsNotLoaded,
      loadingFilePaths: true,
    })
  );

  const copyrightFileCounts = loadCopyrightFileCountPromise(ownerType, ownerPublicId, component);
  const loadPaths = loadFilePathsPromise(ownerType, ownerPublicId, component, copyright, 0);

  return Promise.all([loadPaths, copyrightFileCounts])
    .then(([paths, copyrightCount]) =>
      onCopyrightDetailsLoaded(dispatch, copyrightIndex, component, {
        paths,
        copyrightCount,
      })
    )
    .catch((error) => dispatch(copyrightDetailsFailed({ value: Messages.getHttpErrorMessage(error) })));
}

function loadCopyrightContext(dispatch, ownerType, ownerId, component, copyrightContentHash, filePath) {
  const copyrightContextUrl = getCopyrightContextUrl(
    ownerType,
    ownerId,
    component.hash,
    component.componentIdentifier,
    copyrightContentHash,
    filePath
  );

  return axios
    .get(copyrightContextUrl)
    .then((copyrightContextsPayload) =>
      dispatch(
        copyrightContextFulfilled({
          filePath,
          copyrightContexts: copyrightContextsPayload.data,
        })
      )
    )
    .catch((error) => dispatch(copyrightContextFailed({ value: Messages.getHttpErrorMessage(error) })));
}

function loadFilePaths(dispatch, ownerType, ownerId, component, copyright, pageNumber) {
  return loadFilePathsPromise(ownerType, ownerId, component, copyright, pageNumber)
    .then((filePathsPayload) => dispatch(copyrightFilePathsFulfilled({ filePaths: filePathsPayload.data })))
    .catch((error) => dispatch(copyrightFilePathsFailed({ value: Messages.getHttpErrorMessage(error) })));
}

function loadFilePathsPromise(ownerType, ownerId, component, copyright, pageNumber) {
  if (copyright === undefined) {
    return Promise.reject('Invalid copyright index');
  }
  if (copyright.originalContentHash) {
    const copyrightFilePathsUrl = getCopyrightFilePathsUrl(
      ownerType,
      ownerId,
      component.hash,
      component.componentIdentifier,
      copyright.originalContentHash,
      pageOffset(pageNumber),
      FILE_PATH_PAGE_SIZE
    );
    return axios.get(copyrightFilePathsUrl);
  } else {
    // Manually added copyright, return resolved promise with no file paths
    return Promise.resolve({ data: { filePaths: [], totalFileMatches: 0 } });
  }
}

function loadCopyrightFileCountPromise(ownerType, ownerId, component) {
  const copyrightFileCountUrl = getCopyrightFileCountUrl(
    ownerType,
    ownerId,
    component?.hash,
    component?.componentIdentifier
  );

  return axios.get(copyrightFileCountUrl);
}

function extractRoutingParameters(state, requestedCopyrightIndex) {
  const advancedLegalState = state.advancedLegal;
  const routerParams = state.router.currentParams;

  const copyrightIndex = requestedCopyrightIndex || state.componentCopyrightDetails.copyrightIndex;

  const component = advancedLegalState.component.component;
  const copyright = component?.licenseLegalData.copyrights[copyrightIndex];

  const ownerType = routerParams.ownerType;
  const ownerPublicId = routerParams.ownerId;
  return { copyright, component, ownerType, ownerPublicId };
}
