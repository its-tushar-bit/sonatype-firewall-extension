/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { applyTo, curryN, isNil, map, reject, test } from 'ramda';
import isValidHostname from 'is-valid-hostname';

export const validateHostname = (value) => (isValidHostname(value) ? null : 'Invalid host name');

export const validateNonEmpty = (val) => (val && val.length ? null : 'Must be non-empty');

export const validateMaxLength = curryN(2, function validateMaxLength(maxLength, val) {
  if (!val || !maxLength) {
    return null;
  }
  return val.length <= maxLength ? null : `Please enter less than ${maxLength} characters`;
});

export const validatePatternMatch = curryN(3, function validatePatternMatch(regex, message, val) {
  return test(regex, val) ? null : message;
});

/**
 * Given a list of validator functions that return either a single string or null, returns a validator that
 * returns a list of validation messages
 */
export const combineValidators = (validators) => (val) => reject(isNil, map(applyTo(val), validators));

export const hasValidationErrors = (validationErrors) => {
  if (validationErrors == null) {
    return false;
  } else if (Array.isArray(validationErrors)) {
    return validationErrors.length !== 0;
  } else {
    return true;
  }
};
