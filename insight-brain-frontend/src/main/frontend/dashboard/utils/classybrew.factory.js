/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

function getClassyBrew(win) {
  win = win || window;

  return function innerCreateClassyBrew(series) {
    const brew = new win.classyBrew();

    brew.colorSchemes.SonatypeBlues = {
      1: ['var(--iq-sonatype-blue-12)'],
      2: ['var(--iq-sonatype-blue-8)', 'var(--iq-sonatype-blue-16)'],
      3: ['var(--iq-sonatype-blue-6)', 'var(--iq-sonatype-blue-12)', 'var(--iq-sonatype-blue-18)'],
      4: [
        'var(--iq-sonatype-blue-5)',
        'var(--iq-sonatype-blue-10)',
        'var(--iq-sonatype-blue-14)',
        'var(--iq-sonatype-blue-19)',
      ],
      5: [
        'var(--iq-sonatype-blue-4)',
        'var(--iq-sonatype-blue-8)',
        'var(--iq-sonatype-blue-12)',
        'var(--iq-sonatype-blue-16)',
        'var(--iq-sonatype-blue-20)',
      ],
      6: [
        'var(--iq-sonatype-blue-3)',
        'var(--iq-sonatype-blue-7)',
        'var(--iq-sonatype-blue-11)',
        'var(--iq-sonatype-blue-13)',
        'var(--iq-sonatype-blue-17)',
        'var(--iq-sonatype-blue-21)',
      ],
      7: [
        'var(--iq-sonatype-blue-2)',
        'var(--iq-sonatype-blue-6)',
        'var(--iq-sonatype-blue-9)',
        'var(--iq-sonatype-blue-12)',
        'var(--iq-sonatype-blue-15)',
        'var(--iq-sonatype-blue-18)',
        'var(--iq-sonatype-blue-22)',
      ],
      properties: { type: 'seq', blind: [1] },
    };

    brew.setColorCode('SonatypeBlues');
    brew.isWhiteText = function (score) {
      var colors = brew.getColors();
      var index = colors.indexOf(brew.getColorInRange(score));

      if (colors.length === 1) {
        return false;
      } else if (colors.length >= 6) {
        return colors.length - index <= 3;
      } else if (colors.length >= 4) {
        return colors.length - index <= 2;
      } else if (colors.length > 1) {
        return colors.length - index <= 1;
      }
      return false;
    };

    brew.getColor = function (score) {
      if (score === 0) {
        return 'var(--iq-sonatype-blue-1)';
      }
      return brew.getColorInRange(score);
    };

    brew.setSeries(series);

    brew.setNumClasses(Math.min(7, series.length));
    brew.classify('quantile');

    return brew;
  };
}

export const createClassyBrew = getClassyBrew();

export default function ClassyBrew($window) {
  return { create: getClassyBrew($window) };
}
