/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export default function () {
  var selected = null,
    coordinates = null,
    format = null,
    identificationSource = null;
  return {
    get: function () {
      return coordinates;
    },
    getFormat: function () {
      return format;
    },
    getIdentificationSource: function () {
      return identificationSource;
    },
    setIdentificationSource: function (is) {
      identificationSource = is;
    },
    set: function (t, c) {
      coordinates = c;
      format = t;
      selected = null;
    },
    getSelected: function () {
      return selected || coordinates;
    },
    setSelected: function (c) {
      if (c && coordinates && c.version === coordinates.version) {
        selected = null;
      } else {
        selected = c;
      }
    },
    isOriginalVersion: function () {
      return (this.get() || {}).version === (this.getSelected() || {}).version;
    },
  };
}
