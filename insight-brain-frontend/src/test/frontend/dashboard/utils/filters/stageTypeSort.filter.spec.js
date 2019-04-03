import dashboardUtilsModule from '../../../../../main/frontend/dashboard/utils/dashboard.utils.module';

describe('stageTypeSort.filter.spec', function () {

  beforeEach(angular.mock.module(dashboardUtilsModule.name));

  it('sort by id', inject(function ($filter) {
    var result = $filter('stageTypeSort')([
      { id: 'operate' },
      { id: 'build' },
      { id: 'release' },
      { id: 'stage-release' }
    ]);
    expect(result[0].id).toEqual('build');
    expect(result[1].id).toEqual('stage-release');
    expect(result[2].id).toEqual('release');
    expect(result[3].id).toEqual('operate');
  }));
  it('sort by stageTypeId', inject(function ($filter) {
    var result = $filter('stageTypeSort')([{
      id: 'build',
      stageTypeId: 'operate'
    }, {
      id: 'operate',
      stageTypeId: 'build'
    }, {
      id: 'stage-release',
      stageTypeId: 'release'
    }, {
      id: 'release',
      stageTypeId: 'stage-release'
    }]);
    expect(result[0].stageTypeId).toEqual('build');
    expect(result[1].stageTypeId).toEqual('stage-release');
    expect(result[2].stageTypeId).toEqual('release');
    expect(result[3].stageTypeId).toEqual('operate');
  }));
});
