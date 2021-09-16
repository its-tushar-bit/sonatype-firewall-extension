/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
const baseIqConfig = require('./karma.conf');

module.exports = function (config) {
  baseIqConfig(config);

  config.set({
    singleRun: false,
    browsers: ['Chrome'],
    concurrency: 1,
    reporters: ['progress'],
  });
};
