<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->
# Sonatype Insight Frontend Coding Standards

*Note: This guide applies only to the Insight team which works on the Nexus Lifecycle products.*

At Sonatype we value the stability and maintainability of the code base while stressing the ability of the developers to quickly understand, consume and update the code base. We adopt code practices which facilitate our developers in rapidly creating new features without jeopardizing the existing feature set. Below are a set of code guidelines that we enforce in order to ensure that our values are upheld. The guidelines are short and written with consideration that different developers approach problems differently; they focus on practices that we have found optimize our ability to do our job without stifiling the process of development. This document is maintained in source control and therefore fluid, we encourage pull requests so that our code guidelines improve with our team.

## Technology Stack
* [Angular JS](https://angularjs.org)
  * We use AngularJS as a MVC framework for our frontend code
  * We utilize MVC as it provides a best practice to seperate the concerns of the data, the view and the business logic into separate chunks
  * We picked a framework that was all inclusive as to reduce the need to pick other frameworks and the churn required to maintain their updates
* [AngularUI Router](http://angular-ui.github.io/ui-router/site)
  * We use AngularUI Router as our routing framework
  * We chose AngularUI Router over AngularJS's internal routing as it provides states of MVC beyond a simple routing solution
* [Jasmine](http://jasmine.github.io)
  * We use Jasmine for unit testing
* [Selenide](http://selenide.org)
  * We use Selenide for functional testing

## Core Development
* We prefer native functions over library functions
  * While we have a preferred technological stack, we desire portable code that can be understood by any developer. As such we prefer the use of native functions over those provided by a framework
  * Likewise, we prefer utilizing our core stack over other libraries
  * As an example, we prefer the use of Array.prototype.forEach over angular.forEach over $.each
* We use the IIFE design pattern
  * This allows for a defined execution context for each code block
  * This ensures privacy of code blocks
* We use strict mode for all javascript execution
  * We value failing fast and encourage all errors to be thrown
  * We value security and the enhancements enforced by strict more
* We prefer descriptive names for functions and variables rather than commenting
  * Of course, sometimes that may not be enough, and a comment would aide in understanding, left to developer's discretion

## Component-based application architecture
See "Component-based application architecture" in https://docs.angularjs.org/guide/component

> An application is a tree of components: Ideally, the whole application should be a tree of components that implement clearly defined inputs and outputs, and minimize two-way data binding. That way, it's easier to predict when data changes and what the state of a component is.

We follow "Component-based application architecture" while developing new functionality (and gradually refactoring existing code).

See `frontend/mainHeader` for an example of component-based approach, file structure and naming convention.

## File structure
Since application is a component tree, the directory structure should reflect the component tree, where each component is hosted in its directory with the same name.
Also there are top level directories for reusable components, directives and services (stores)

Here is the desired file structure
```
/directives
/services
/components
../module.js
../checkbox
../../checkbox.js
../../checkbox.html
../../_checkbox.scss
/rootComponent
../module.js
../rootComponent.js
../rootComponent.html
../_rootComponent.scss
../mainHeader
../../module.js
../../mainHeader.js
../../mainHeader.html
../../_mainHeader.scss
```

## AngularJS Naming Standards
* The ultimate goal is to use the same name for 
  * angular entity name (component, directive, service etc)
  * file name
  * directory containing the component
  * component template file
  * component scss partial file (prefixed with `_`)

* Since Angular directives and component names can only be camelCase (lower camel case), we follow this as a lowest common denominator, and use camelCase naming convention for:
  * all angular DI names (modules, directives, components, services, filters)
  * files
  * directories
  * modules

* Angular entities, except for Directives and Components, should be suffixed by their type
  * Appending the component type to its name allows the consumer to easily understand its type and function
  * Directives and Components are not suffixed to prevent the extra HTML markup required to reference them
  * quxService.js
```javascript
  angular.service('quxService', QuxService);
```


## AngularJS Development
* Each Angular Component should exist in it's own file, if it is injectable
  * We value the ability to easily find where code resides
  * We understand that small code files encourages the decomposition of a problem
  * We follow other language practices of encapsolating one object into one file
* Each Angular template and view should exist in it's own file
  * We value the ability to easily access code
  * This allows the view controller and directive to reside in the file system beside its business logic
* Javascript code should not contain HTML
  * This enforces the separation of view and controller
  * Performance concerns can be mitigated by compiling the markup into Javascript during the build
  * HTML in Javascript is difficult to read and maintain
* Component function should exist at the top of the code file
  * This highlights the core logic over the boiler plate code
  * This prevents unnecessary abuse of Javascript's hoisting
```javascript
  function FooController() {
    /* code block */
  }
  angular.controller('foo.controller', FooController);
```
* We utilize an Angular components $inject array to declare dependencies
  * This prevents long lists of dependencies to interfect with code legibility
  * This prevents code churn from how Eclipse and IDEA format these arrays
```javascript
  function FooController($state) {
    /* code block */
  }
  FooController.$inject = ['$state'];
```

### AngularJS Development - Controllers

* **NOTE: as we are migrating towards component-based architecture, there are not going to be any stand-alone Angular controllers.**
As you can see in the example below, route configuration doesn't specify FooBar controller directly, its part of `fooBar` component implementation.
```javascript
  angular.component('fooBar', {
    controller: FooBar
  });
  
  $stateProvider.state('foo', {
    url: '/foo',
    template: '<foo-bar></foo-bar>'
  });
```

* We utilize the controllerAs syntax
  * This isolates the view model to the controller or directive
  * This prevents the temptation to walk through the scope hierarchy
  * This provides a clear view of which properties belong to which objects, especially with name collisions
```javascript
  function FooController() {
    var vm = this;

    vm.bar = 'baz';
  }
  $stateProvider.state('foo', {
    url: '/foo',
    controller: FooController,
    controllerAs: 'vm',
    template: '<div>{{vm.var}}</div>'
  });
```
* We place bindable members on the top of Angular controllers
  * We value digestable code that is easily accessible when reading the files
  * We understand that this requires the (ab)use of Javascripts hoisting
  * We believe that the advantages of clearly indicated bindable members outways the disadvantages of obscured code
```javascript
  function FooController() {
    var vm = this;

    vm.bar = Bar;
    vm.foo = 'foo';

    function Bar() {
      /* code block */
    }
  }
```
* We denote future bindable members at the top of Angular controllers
  * This allows developers to easily locate member variables that will be binded in the future
```javascript
  function FooController() {
    var vm = this;

    vm.bar = Bar;
    vm.error = undefined;
    vm.foo = 'foo';

    function Bar() {
      /* code block */
      vm.error = "error";
    }
  }
```

### AngularJS Development - Directives
* For directives, we use an element restriction when "creating a component that is in control of its template" and an attribute restriction when "decorating an existing element with new functionality".
  * This is inline with the Angular documentation as seen in ["When should I use an attribute versus an element?"](https://docs.angularjs.org/guide/directive)

* We place dependent controllers and links below the directive function
```javascript
  function Foo(...) {
    return {
      controller: FooController,
      link: FooLink
    };

    function FooLink(...) {...}
  }

  Foo.$inject = [...];

  function FooController(...) {...}

  FooController.$inject = [...];
```

# Jasmine Development
* Jasmine root describe should share the name of the containing file
  * This allows developers to easily locate code for failing tests
* There should be a single jasmine file for each Angular component
  * This mirrors the file structure of the source code
  * This enforces smaller test files for easy consumption
  
# HTML guidelines
* If all tag attributes fit in one line - they can be inline. Otherwise each attribute should be in its own line:
```html
<element attr1
         attr2
         attr3>
</element>
```

* If opening tag, HTML content (inner HTML)  and closing tag fit in one line - they can be inline. Otherwise opening and closing tags should be in their own line:
```html
<element>
    really long inner html
</element>
```

# Style guidelines
* One-off styles should be referenced by ID and do not neeed a styleguide example

# Selenide Development
* Typically our page objects contain a root object, in cases where that root selector is known (i.e. #someid) we should use that in subqueries directly
  * Replace root.$(".someclass") with $("#someid .someclass") to save roundtrips
* Rather than query for a list of elements and grab a certain one, tighten up the css selector to get the desired item back directly
  * Replace $$(".someclass").get(0) with $(".someclass:first-child") or $(".someclass:nth-child(7)") for example
* Use `#id` instead of `.class` selectors when possible to minimize dependency on class names. (which means adding new ids to HTML if needed)
