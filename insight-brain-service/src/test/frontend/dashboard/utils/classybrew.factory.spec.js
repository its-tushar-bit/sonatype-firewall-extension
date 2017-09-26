describe('classybrew.factory.spec', function() {

  var brew;

  beforeEach(module('dashboard.utils'));

  beforeEach(function() {
    module(function($provide) {
      $provide.value('$window', (function() {
        return {
          classyBrew: function() {
            this.colorSchemes = {};
            this.setColorCode = jasmine.createSpy();
            this.getColors = jasmine.createSpy();
            this.getColorInRange = jasmine.createSpy();
            this.setSeries = jasmine.createSpy();
          }
        };
      }()));
    });
  });

  beforeEach(inject(function(ClassyBrew) {
    brew = ClassyBrew.create();
  }));

  it('sets up the Sonatype color scheme', function() {
    expect(brew.colorSchemes.SonatypeBlues).toBeDefined();
    expect(brew.setColorCode).toHaveBeenCalledWith('SonatypeBlues');
  });

  it('uses white text for dark backgrounds', function() {
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

  it('adds a higher number so that the last entry in supplied series gets included', function() {
    brew.setSeriesInclusive([1, 2, 3]);
    expect(brew.setSeries).toHaveBeenCalledWith([1, 2, 3, Number.MAX_VALUE]);
  });

  it('returns fixed color code for 0', function() {
    expect(brew.getColor(0)).toBe('rgb(247,251,255)');
    expect(brew.getColorInRange).not.toHaveBeenCalled();

    brew.getColorInRange.and.returnValue('color');
    expect(brew.getColor(1)).toBe('color');
    expect(brew.getColorInRange).toHaveBeenCalledWith(1);
  });

  function setUpColors(theBrew, colors) {
    theBrew.getColors.and.returnValue(colors);
    theBrew.getColorInRange.and.callFake(function(score) {
      return colors[score - 1];
    });
  }
});
