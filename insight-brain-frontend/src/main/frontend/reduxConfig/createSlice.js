/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Wrapper around RTK's createSlice that supports the legacy object notation for extraReducers.
 *
 * RTK 2.x removed support for the object notation:
 *   extraReducers: { [thunk.pending]: handler }
 *
 * This wrapper detects object notation and converts it to the builder callback form at runtime,
 * avoiding the need to mechanically update 100+ slice files.
 */
import { createSlice as rtkCreateSlice } from '@reduxjs/toolkit';
import { forEachObjIndexed, is } from 'ramda';

export default function createSlice(options) {
  const { extraReducers } = options;

  if (extraReducers && is(Object, extraReducers) && !is(Function, extraReducers)) {
    return rtkCreateSlice({
      ...options,
      extraReducers: (builder) => {
        forEachObjIndexed((handler, actionType) => {
          builder.addCase(actionType, handler);
        }, extraReducers);
      },
    });
  }

  return rtkCreateSlice(options);
}
