/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {curryN} from 'ramda';

export const validateNonEmpty = (val) => val && val.length ? null : 'Must be non-empty';

export const validateMaxLength = curryN(2, function validateMaxLength(maxLength, val) {
  if (!val || !maxLength) {
    return null;
  }
  return val.length <= maxLength ? null : `Please enter less than ${maxLength} characters`;
});

export const hasValidationErrors = (validationErrors) => {
  if (validationErrors == null) {
    return false;
  }
  else if (Array.isArray(validationErrors)) {
    return validationErrors.length !== 0;
  }
  else {
    return true;
  }
};
