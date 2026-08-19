/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ClassyBrew from 'MainRoot/dashboard/utils/classybrew.factory';

describe('classybrew.factory.spec', function () {
  let brew;
  let classyBrew;
  let originalClassyBrewDef;

  beforeEach(() => {
    originalClassyBrewDef = window.classyBrew;

    window.classyBrew = jest.fn().mockReturnValue({
      colorSchemes: {},
      setColorCode: jest.fn(),
      getColors: jest.fn(),
      getColorInRange: jest.fn(),
      setSeries: jest.fn(),
      setNumClasses: jest.fn(),
      classify: jest.fn(),
    });

    classyBrew = ClassyBrew(window);
    brew = classyBrew.create([1, 2, 3]);
  });

  afterEach(() => {
    window.classyBrew = originalClassyBrewDef;
  });

  it('sets up the Sonatype color scheme', function () {
    expect(brew.colorSchemes.SonatypeBlues).toBeDefined();
    expect(brew.setColorCode).toHaveBeenCalledWith('SonatypeBlues');
  });

  it('uses white text for dark backgrounds', function () {
    setUpColors(brew, ['light', 'medium', 'dark']);
    expect(brew.isWhiteText(1)).toBe(false);
    expect(brew.isWhiteText(2)).toBe(false);
    expect(brew.isWhiteText(3)).toBe(true);

    setUpColors(brew, ['lightest', 'light', 'lightish', 'medium', 'darkish', 'dark', 'darkest']);
    expect(brew.isWhiteText(1)).toBe(false);
    expect(brew.isWhiteText(2)).toBe(false);
    expect(brew.isWhiteText(3)).toBe(false);
    expect(brew.isWhiteText(4)).toBe(false);
    expect(brew.isWhiteText(5)).toBe(true);
    expect(brew.isWhiteText(6)).toBe(true);
    expect(brew.isWhiteText(7)).toBe(true);
  });

  it('sets series', function () {
    expect(brew.setSeries).toHaveBeenCalledWith([1, 2, 3]);
  });

  it('returns fixed color code for 0', function () {
    expect(brew.getColor(0)).toBe('var(--iq-sonatype-blue-1)');
    expect(brew.getColorInRange).not.toHaveBeenCalled();

    brew.getColorInRange.mockReturnValue('color');
    expect(brew.getColor(1)).toBe('color');
    expect(brew.getColorInRange).toHaveBeenCalledWith(1);
  });

  it('call setNumClasses with 7, if length is more than 7', function () {
    brew = classyBrew.create([1, 2, 3, 4, 5, 6, 7, 8]);
    expect(brew.setNumClasses).toHaveBeenCalledWith(7);
  });

  it('call setNumClasses with length of series, if length is less than 7', function () {
    brew = classyBrew.create([1, 2, 3, 4, 5, 6]);
    expect(brew.setNumClasses).toHaveBeenCalledWith(6);
  });

  it('sets quantile algorithm', function () {
    expect(brew.classify).toHaveBeenCalledWith('quantile');
  });

  function setUpColors(theBrew, colors) {
    theBrew.getColors.mockReturnValue(colors);
    theBrew.getColorInRange.mockImplementation(function (score) {
      return colors[score - 1];
    });
  }
});
