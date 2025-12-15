/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.StaticInjectionTestHelper;
import com.sonatype.insight.brain.api.v2.service.ConfigurationUtils;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.DAOFactory;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.TestDAOFactory;
import com.sonatype.insight.brain.db.rule.DatabaseContainerRule;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.ConditionValueTypes;
import com.sonatype.insight.brain.search.SearchIndexRule;
import com.sonatype.insight.brain.search.SearchModule;
import com.sonatype.insight.brain.security.SecurityModule;
import com.sonatype.insight.brain.service.ApplicationLifecycle;
import com.sonatype.insight.brain.service.DbBasedModule;
import com.sonatype.insight.brain.service.DefaultApplicationLifecycle;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.brain.service.MultiBinderModule;
import com.sonatype.insight.brain.service.modules.AuthenticationModule;
import com.sonatype.insight.brain.service.modules.ComponentModule;
import com.sonatype.insight.brain.service.modules.CoreServiceModule;
import com.sonatype.insight.brain.service.modules.DashboardModule;
import com.sonatype.insight.brain.service.modules.DataAccessModule;
import com.sonatype.insight.brain.service.modules.FirewallModule;
import com.sonatype.insight.brain.service.modules.IntegrationModule;
import com.sonatype.insight.brain.service.modules.IqOnlyAuthModule;
import com.sonatype.insight.brain.service.modules.IqOnlyModule;
import com.sonatype.insight.brain.service.modules.MigrationModule;
import com.sonatype.insight.brain.service.modules.OperationalModule;
import com.sonatype.insight.brain.service.modules.OrganizationModule;
import com.sonatype.insight.brain.service.modules.PolicyModule;
import com.sonatype.insight.brain.service.modules.ProductLicenseModule;
import com.sonatype.insight.brain.service.modules.RepositoryModule;
import com.sonatype.insight.brain.service.modules.ScannerModule;
import com.sonatype.insight.brain.service.modules.SonatypeLicensingModule;
import com.sonatype.insight.brain.service.modules.TelemetryModule;
import com.sonatype.insight.brain.utils.DefaultExecutorThreadPools;
import com.sonatype.insight.brain.utils.ExecutorThreadPools;
import com.sonatype.insight.test.GuiceInjectedTest;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.experimental.categories.Category;

/**
 * Handles creation of the four data store classes for tests. The {@link DatabaseContainerRule} is a junit rule
 * to create the instances and inject them into the legacy *Provider classes. The {@link DataStoreTestModule} binds
 * those instances so Guice can inject as needed. Ultimately any test that accesses a datastore needs to extend this
 * base class.
 *
 * <B>IMPORTANT</B> - If you override {@link #configure(Binder)}, make sure to call `super.configure(binder)` to get
 * database support
 */
@Category(SlowTest.class)
public abstract class BrainInjectedTest
    extends GuiceInjectedTest
{
  /**
   * Note: As this will be the child class of the test, the database rule must be executed first. This is very
   * important as we need the data stores initialized first, in particular ahead of other rules like
   * {@link TemporaryEntity}
   */
  @Rule(order = 1)
  public DatabaseContainerRule databaseContainerRule = DatabaseContainerRule.getInstance(BrainInjectedTest.class);

  @Rule(order = 2)
  public SearchIndexRule searchIndexRule = SearchIndexRule.getInstance(BrainInjectedTest.class);

  @Rule(order = 3)
  public TemporaryEntity tempEntity = createTemporaryEntity();

  /** You should only use this `daoFactory` when you override the `configure` method and you need to crate DAOs there.
   * Otherwise, always prefer the use of the `@Inject` annotation to inject the DAOs you need for your test */
  protected DAOFactory daoFactory;

  @BeforeClass
  public static void disableWaitToCloseOldClients() {
    HdsClient.waitToCloseOldClients = false;
  }

  @Before
  @Override
  public void setUp()
      throws Exception
  {
    // Re-inject classes that have static dependencies
    daoFactory = new TestDAOFactory(databaseContainerRule);
    StaticInjectionTestHelper.inject(daoFactory);

    super.setUp();
  }

  /**
   * Get production modules to install in tests. These modules are used for creating a test server that is equivalent
   * to the IQ Production server
   *
   * @return list of production modules
   */
  @Override
  protected List<Module> getProductionModulesForTest() {
    List<Module> modules = new ArrayList<>();

    addTestSpecificModules(modules);
    addProductionModules(modules);

    return modules;
  }

  /**
   * These modules mimic a Production IQ server
   * @param modules - the list of modules
   */
  private void addProductionModules(final List<Module> modules) {
    modules.add(new SearchModule(() -> searchIndexRule.getSearchConfig()));
    modules.add(new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(com.sonatype.insight.jaxrs.error.ErrorResponseGenerator.class).to(ErrorResponseGenerator.class);
        bind(ExecutorThreadPools.class).to(DefaultExecutorThreadPools.class);

        requestStaticInjection(ExecutorThreadPools.class);
        requestStaticInjection(ConditionTypes.class);
        requestStaticInjection(ConditionValueTypes.class);
        requestStaticInjection(ConfigurationUtils.class);
        requestStaticInjection(ComponentDetailsLoader.class);
        requestStaticInjection(SystemConfigurationPropertyFeature.class);

        bind(ApplicationLifecycle.class).to(DefaultApplicationLifecycle.class);

        // This binding is referenced by a class present in sonatype-licensing that we don't actually use.
        // For unclear reasons, since the switch to dropwizard-guicey leaving this binding null has prevented
        // the server from starting. A proper solution cound not be found, so just fill it in with a dummy value
        bind(File.class).annotatedWith(Names.named("licensing.access.file")).toInstance(new File("workaround"));
      }
    });

    // we intentionally don't have binder.install(new SecurityAopModule()); because we want to bypass authz checks in
    // most tests and instead have it in AbstractServiceAuthzTest
    modules.add(new SecurityModule());

    // Import explicit Guice modules at top of file
    modules.add(new ComponentModule());
    modules.add(new CoreServiceModule());
    modules.add(new DashboardModule());
    modules.add(new DbBasedModule(() -> databaseContainerRule.getDatabaseContainer()));  // Database-specific bindings
    modules.add(new DataAccessModule());  // Auto-bind all @Named DAOs from data layer
    modules.add(new FirewallModule());
    modules.add(new IntegrationModule());
    modules.add(new IqOnlyModule());
    modules.add(new IqOnlyAuthModule());
    modules.add(new MigrationModule());
    modules.add(new OperationalModule());
    modules.add(new OrganizationModule());
    modules.add(new PolicyModule());
    modules.add(new SonatypeLicensingModule());
    modules.add(new ProductLicenseModule());
    modules.add(new RepositoryModule());
    modules.add(new ScannerModule());
    modules.add(new AuthenticationModule());
    modules.add(new TelemetryModule());
  }

  /**
   * These modules are specific to testing only
   * @param modules - the list of modules to add to
   */
  private void addTestSpecificModules(final List<Module> modules) {
    modules.add(new DataStoreTestModule(databaseContainerRule));
    modules.add(new TestHelperModule());
    modules.add(new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(File.class).annotatedWith(Names.named("licensing.access.file")).toInstance(new File("workaround"));
      }
    });
  }

  /**
   * Get test-specific module with bindings that override production bindings. This method wraps the configure(Binder)
   * method in an AbstractModule.
   *
   * @return test module with overrides, or null if no overrides needed
   */
  @Override
  protected Module getOverrideModule() {

    // Allow BrainInjectedTest to override injected classes
    AbstractModule brainInjectedTestOverrides = new AbstractModule()
    {
      @Override
      protected void configure() {
        BrainInjectedTest.this.overrideTestBindings(binder());
      }
    };

    // Convenience module/method to allow individual tests to override injections without having to create new modules
    AbstractModule additionalOverrides = new AbstractModule()
    {
      @Override
      protected void configure() {
        BrainInjectedTest.this.configure(binder());
      }
    };

    return Modules.override(brainInjectedTestOverrides).with(additionalOverrides);
  }

  @Override
  protected Module getMultiBinderModule(final Set<Class<?>> extensions) {
    return new MultiBinderModule(extensions);
  }

  /**
   * This needs a better name as it no longer overrides SISU but using "configure" means we don't need to update
   * all the tests that override this / won't miss them.
   *
   * @param binder - guice binder
   */
  protected void configure(Binder binder) {

  }

  /**
   * Configure test-specific bindings. Override this method to provide bindings that will override production bindings.
   * This method is called automatically by the test infrastructure.
   *
   * <p>Example:
   * <pre>
   * {@literal @}Override
   * public void configure(Binder binder) {
   *   binder.bind(SomeService.class).to(MockSomeService.class);
   * }
   * </pre>
   *
   * @param binder the Guice binder for configuring test bindings
   */
  protected void overrideTestBindings(Binder binder) {
    // Default: no test overrides
    // Subclasses can override to provide test-specific bindings
  }

  /**
   * Creates a new instance of {@link TemporaryEntity}. This method is protected to allow subclasses to override the
   * creation logic if needed, for example, to insert any dependencies or configurations used by the
   * {@link TemporaryEntity}
   *
   * @return a new instance of {@link TemporaryEntity}.
   */
  protected TemporaryEntity createTemporaryEntity() {
    return new TemporaryEntity(databaseContainerRule);
  }
}
