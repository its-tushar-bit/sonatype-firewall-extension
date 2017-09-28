/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export default
function chartUtilsService() {
  return {
    calculateTickInterval: calculateTickInterval
  };
}

function calculateTickInterval(numberOfTicks, maxValue) {

  // prevent generating ticks if there is no data
  // (plottable generates ticks from 0 to 1 if there is no data)
  if (maxValue === 0) {
    return 1;
  }

  // if max Value is less than or equal to 1 - use 2 decimals for multiples
  if (maxValue <= 1) {
    return Math.round((1 / numberOfTicks) * 100) / 100;
  }

  var tickInterval = maxValue / numberOfTicks;

  // if tickInterval is more then 5 - make it multiples of 5
  if (tickInterval > 5) {
    return Math.floor(tickInterval / 5) * 5;
  }

  // if tickInterval is between 1 and 5 - make it multiples of 1
  if (tickInterval > 1) {
    return Math.floor(tickInterval);
  }

  return tickInterval;
}
