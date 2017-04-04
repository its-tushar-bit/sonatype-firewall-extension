describe('stageFilter.filter.spec', function () {

  beforeEach(module('dashboard.utils'));
  
  it('empty filter', inject(function ($filter) {
    var stageList = [{ id : 'operate' }, { id : 'build' }, { id : 'release' }, { id : 'stage-release' }],
        result;

    // null filter
    result = $filter('stageFilter')(stageList);
    expect(result).toEqual(stageList);

    // empty filter
    result = $filter('stageFilter')(stageList, { stageTypeFilters : [] });
    expect(result).toEqual(stageList);
  }));

  it('filter', inject(function ($filter) {
    var stageList = [{ id : 'build' }, { id : 'stage-release' }, { id : 'release' }, { id : 'operate' }],
        result;

    result = $filter('stageFilter')(stageList, { stageTypeFilters : ['release', 'build'] });
    expect(result).toEqual([{ id : 'build' }, { id : 'release' }]);
  }));
});
