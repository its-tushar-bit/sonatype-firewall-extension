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
      1: ['rgb(121,165,198)'],
      2: ['rgb(168,197,219)', 'rgb(74,129,170)'],
      3: ['rgb(190,212,228)', 'rgb(121,165,198)', 'rgb(63,108,143)'],
      4: ['rgb(203,220,234)', 'rgb(150,185,212)', 'rgb(91,145,187)', 'rgb(54,93,123)'],
      5: ['rgb(211,226,238)', 'rgb(168,197,219)', 'rgb(121,165,198)', 'rgb(74,129,170)', 'rgb(47,82,109)'],
      6: [
        'rgb(218,231,241)',
        'rgb(181,205,223)',
        'rgb(142,179,208)',
        'rgb(100,151,190)',
        'rgb(67,117,155)',
        'rgb(43,74,98)',
      ],
      7: [
        'rgb(222,233,242)',
        'rgb(190,212,228)',
        'rgb(157,189,214)',
        'rgb(121,165,198)',
        'rgb(83,139,183)',
        'rgb(63,108,143)',
        'rgb(40,69,91)',
      ],
      properties: { type: 'seq', blind: [1] },
    };

    brew.setColorCode('SonatypeBlues');

    brew.isWhiteText = function (score) {
      var colors = brew.getColors();
      var index = colors.indexOf(brew.getColorInRange(score));
      if (colors.length === 7) {
        return index >= 4;
      } else if (colors.length >= 4) {
        return colors.length - index <= 2;
      } else if (colors.length > 1) {
        return colors.length - index <= 1;
      }
      return false;
    };

    brew.getColor = function (score) {
      if (score === 0) {
        return 'rgb(247,251,255)';
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
