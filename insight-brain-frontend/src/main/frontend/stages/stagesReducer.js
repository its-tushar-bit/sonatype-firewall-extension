/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { __, fromPairs, map, pair } from 'ramda';

import { createReducerFromActionMap } from '../util/reduxUtil';
import { pathSet, propSet } from '../util/jsUtil';

import {
  FETCH_STAGE_TYPES_REQUESTED,
  FETCH_STAGE_TYPES_FULFILLED,
  FETCH_STAGE_TYPES_FAILED,
  validPurposes,
} from './stagesActions';

const initialPurposeState = Object.freeze({
    loading: false,
    error: null,
    stageTypes: null,
  }),
  // state has a key for each valid purpose, with each key's value initially being `initialPurposeState`
  initialState = Object.freeze(fromPairs(map(pair(__, initialPurposeState), validPurposes)));

const reducerActionMap = {
  [FETCH_STAGE_TYPES_REQUESTED]: fetchStageTypesRequested,
  [FETCH_STAGE_TYPES_FULFILLED]: fetchStageTypesFulfilled,
  [FETCH_STAGE_TYPES_FAILED]: fetchStageTypesFailed,
};

const getShortName = ({ stageTypeId, stageName }) => (stageTypeId === 'stage-release' ? 'Stage' : stageName),
  addShortName = (stageType) =>
    Object.freeze({
      ...stageType,
      shortName: getShortName(stageType),
    });

function fetchStageTypesRequested(payload, state) {
  return pathSet([payload, 'loading'], true, state);
}

function fetchStageTypesFulfilled({ purpose, data }, state) {
  const stageTypes = purpose === 'cli' ? data : map(addShortName, data);

  return propSet(
    purpose,
    {
      ...initialPurposeState,
      stageTypes: Object.freeze(stageTypes),
    },
    state
  );
}

function fetchStageTypesFailed({ purpose, error }, state) {
  return propSet(
    purpose,
    {
      ...initialPurposeState,
      error,
    },
    state
  );
}

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
