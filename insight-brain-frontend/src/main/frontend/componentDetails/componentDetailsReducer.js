/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { curryN } from 'ramda';

import {
  LOAD_COMPONENT_LABELS_REQUESTED,
  LOAD_COMPONENT_LABELS_FULLFILED,
  LOAD_COMPONENT_LABELS_FAILED,
} from './componentDetailsActions';

const initState = Object.freeze({
  pendingLoads: new Set(),
  labels: [],
  loadError: null,
});

export default function applicationReportReducer(state = initState, { type, payload }) {
  switch (type) {
    case LOAD_COMPONENT_LABELS_REQUESTED:
      return setPendingLoads(['labels'], state);

    case LOAD_COMPONENT_LABELS_FULLFILED:
      return unsetPendingLoads(['labels'], {
        ...state,
        labels: payload,
      });

    case LOAD_COMPONENT_LABELS_FAILED:
      return unsetPendingLoads(['labels'], { ...state, loadError: payload });

    default:
      return state;
  }
}

const mutatePendingLoads = curryN(3, function mutatePendingLoads(setMutator, loads, state) {
  const { pendingLoads } = state;
  const newPendingLoads = new Set(pendingLoads);

  loads.forEach(setMutator(newPendingLoads));

  return { ...state, pendingLoads: newPendingLoads };
});

const setPendingLoads = mutatePendingLoads((set) => (val) => set.add(val));
const unsetPendingLoads = mutatePendingLoads((set) => (val) => set.delete(val));
