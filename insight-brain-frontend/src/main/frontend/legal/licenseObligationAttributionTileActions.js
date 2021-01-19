/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { payloadParamActionCreator } from '../util/reduxUtil';

export const LICENSE_OBLIGATION_SET_ATTRIBUTION_TEXT = 'LICENSE_OBLIGATION_SET_ATTRIBUTION_TEXT';
export const LICENSE_OBLIGATION_SET_OBLIGATION_FULFILLED = 'LICENSE_OBLIGATION_SET_OBLIGATION_FULFILLED';
export const LICENSE_OBLIGATION_SET_SCOPE = 'LICENSE_OBLIGATION_SET_SCOPE';

export const setAttributionText = payloadParamActionCreator(LICENSE_OBLIGATION_SET_ATTRIBUTION_TEXT);
export const setObligationFulfilled = payloadParamActionCreator(LICENSE_OBLIGATION_SET_OBLIGATION_FULFILLED);
export const setScope = payloadParamActionCreator(LICENSE_OBLIGATION_SET_SCOPE);
