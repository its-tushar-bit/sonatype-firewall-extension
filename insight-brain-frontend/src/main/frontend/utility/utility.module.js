/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import utilityServicesModule from './services/utility.services.module';
import commonServicesModule from '../utilAngular/CommonServices';
import formsModule from '../FormsModule';
import utilityDirectivesModule from '../utility/directives/utility.directives.module';
import FuseFilterFactory from './filters/fuzzy.filter';
import CachedServiceFactory from './services/cached.service.factory';
import DeleteModalController from './services/delete.modal.controller';
import DeleteModalReduxController from './services/delete.modal.redux.controller';
import DeleteModalService from './services/delete.modal.service';
import eventNameConstant from './services/event.name.constant';
import FormDataHttpInterceptor from './services/form.data.http.interceptor.factory';
import ownerConstant from './services/owner.constant';
import ageInDaysInput from './widgets/age.in.days.input.directive';
import associationEditor from './widgets/association.editor.directive';
import colorPicker from './widgets/color.picker.directive';
import doubleColumnPicker from './widgets/double.column.picker.directive';
import dropdownSelector from './widgets/dropdown.selector.directive';
import sortColumn from './widgets/sort.column.directive';
import submitWrapper from './widgets/submit.wrapper.directive';
import threatLevelSelector from './widgets/threat.level.selector.directive';

export default angular
  .module('utility', [
    'ui.router.state',
    'ngAria',
    commonServicesModule.name,
    formsModule.name,
    utilityDirectivesModule.name,
    utilityServicesModule.name,
  ])
  .config([
    '$httpProvider',
    function ($httpProvider) {
      $httpProvider.interceptors.push('form.data.http.interceptor');
    },
  ])
  .filter('fuzzy', FuseFilterFactory)
  .service('cached.service.factory', CachedServiceFactory)
  .controller('DeleteModalController', DeleteModalController)
  .controller('DeleteModalReduxController', DeleteModalReduxController)
  .service('DeleteModalService', DeleteModalService)
  .constant('event.name.constant', eventNameConstant)
  .factory('form.data.http.interceptor', FormDataHttpInterceptor)
  .constant('owner.constant', ownerConstant)
  .directive('ageInDaysInput', ageInDaysInput)
  .directive('associationEditor', associationEditor)
  .directive('colorPicker', colorPicker)
  .directive('doubleColumnPicker', doubleColumnPicker)
  .directive('dropdownSelector', dropdownSelector)
  .directive('sortColumn', sortColumn)
  .directive('submitWrapper', submitWrapper)
  .directive('threatLevelSelector', threatLevelSelector);
