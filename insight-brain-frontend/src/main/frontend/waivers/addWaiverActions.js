/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { noPayloadActionCreator } from '../util/reduxUtil';

// ToDo: Add real actions
export const ADD_WAIVER = 'ADD_WAIVER';
export const addWaiver = noPayloadActionCreator(ADD_WAIVER);
