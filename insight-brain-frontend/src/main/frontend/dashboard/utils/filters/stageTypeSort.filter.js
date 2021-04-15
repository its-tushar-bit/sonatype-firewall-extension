/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export default function stageTypeSortFilter() {
  function priority(stage) {
    var ordinal = null;
    switch (stage.stageTypeId || stage.id) {
      case 'build':
        ordinal = 0;
        break;
      case 'stage-release':
        ordinal = 1;
        break;
      case 'release':
        ordinal = 2;
        break;
      case 'operate':
        ordinal = 3;
        break;
    }
    return ordinal;
  }

  return function (input) {
    if (input) {
      return input.sort(function (a, b) {
        return priority(a) - priority(b);
      });
    }
  };
}
