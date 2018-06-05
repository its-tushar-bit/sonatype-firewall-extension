const importAll = (requireContext) => { requireContext.keys().forEach(requireContext); };

import '../../main/frontend/lib/jquery-loader';
import 'angular';
import 'angular-aria';
import 'angular-cookies';
import 'angular-route';
import 'angular-sanitize';
import 'angular-ui-validate';
import '@uirouter/angularjs';
import '@uirouter/angularjs/release/stateEvents';
import 'angular-vs-repeat';
import 'angular-xeditable';
import 'ngUpload/ng-upload';
import 'es6-collections';
import '../../main/frontend/lib/bootstrap-loader';
import '../../main/frontend/utility/Polyfills';

import 'angular-mocks/ngMock';

// mocks and spec utils (Globals)
import './SpecUtil';
import './stores/resource.utils';
import './stores/access/access.mock.data';
import './stores/store.utils';
import './stores/policy/policy.mock.data';
import './stores/label/label.mock.data';
import './stores/proprietary/proprietary.mock.data';
import './mock.data/sidebar.resource.mock.data';
import './mock.data/jira.service.mock.data';
import './assets/MockData';

importAll(require.context('./owner.manager', true, /[sS]pec\.js$/));
