/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function CurrentLabelData() {
  var currentLabel = null,
    currentError = null;

  return {
    get: function () {
      return currentLabel;
    },
    set: function (label) {
      currentLabel = label;
    },
    getError: function () {
      return currentError;
    },
    setError: function (error) {
      currentError = error;
    },
  };
}
