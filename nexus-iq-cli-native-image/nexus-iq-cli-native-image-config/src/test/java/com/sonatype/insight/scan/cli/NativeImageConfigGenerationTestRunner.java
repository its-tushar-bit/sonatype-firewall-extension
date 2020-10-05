/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.brain.client.RestClientFactory.RestClient;
import com.sonatype.insight.scan.model.ClientScanResult;
import com.sonatype.insight.test.LogOutput;
import com.sonatype.insight.test.NativeImageLogOutputDecorator;
import com.sonatype.insight.test.networking.KeyStoreInfo;
import com.sonatype.insight.test.networking.SslProperties;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.ThrowableAssertAlternative;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import static org.apache.commons.lang3.SystemUtils.JAVA_HOME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Custom test runner to generate the configuration files for the {@code native-image} tooling. Rather than a
 * traditional test execution and result assertion process (see {@link JUnitPolicyEvaluatorTestRunner} for that), this
 * class will execute the actual CLI JAR file with the given parameters, along with the native-image tracing agent, in
 * order to generate the config files. The results of the scan will still be asserted.
 */
public class NativeImageConfigGenerationTestRunner
    extends AbstractPolicyEvaluatorTestRunner
{
  private final List<String> params;

  private final Map<String, String> environment;

  private final Logger log = LoggerFactory.getLogger(getClass());

  private boolean ssl = false;

  public NativeImageConfigGenerationTestRunner(
      final List<String> params,
      final Map<String, String> environment,
      final LogOutput logOutput)
  {
    super(decorateLogOutput(logOutput));
    this.params = params;
    this.environment = environment;
  }

  private static LogOutput decorateLogOutput(final LogOutput logOutput) {
    return new NativeImageLogOutputDecorator(logOutput);
  }

  public AbstractPolicyEvaluatorTestRunner withSsl() {
    this.ssl = true;
    return this;
  }

  @Override
  public void doPolicyEvaluationRun() throws Exception {
    executeTest(() -> {
      run();
      // nothing to return for the `run` method
      return null;
    });
  }

  @Override
  public ClientScanResult doPolicyEvaluationScan(
      final ProprietaryConfig proprietaryConfig, final RestClient restClient) throws Exception
  {
    return executeTest(() -> scan(proprietaryConfig, restClient));
  }

  @Override
  public AbstractPolicyEvaluatorTestRunner expectDebugLog(final String log) {
    debugLogs.addAll(split(log));
    return this;
  }

  @Override
  public AbstractPolicyEvaluatorTestRunner expectInfoLog(final String log) {
    infoLogs.addAll(split(log));
    return this;
  }

  @Override
  public AbstractPolicyEvaluatorTestRunner expectWarnLog(final String log) {
    warnLogs.addAll(split(log));
    return this;
  }

  @Override
  public AbstractPolicyEvaluatorTestRunner expectErrorLog(final String log) {
    errorLogs.addAll(split(log));
    return this;
  }

  @Override
  protected void assertException(final ThrowableAssertAlternative<ExitException> result) {
    // Exceptions occur within the JVM and the process executor has no way to access them asides from any log that is
    // dumped to WARN/ERROR. Even with that, there are cases where the Exception message is squashed in favour of a
    // custom log entry. So there is nothing we can do here. Please add proper log assertions to your tests as well.
  }

  private void run() throws Exception {
    assertThat(JAVA_HOME).isNotEmpty();

    List<String> command = new ArrayList<>(Arrays.asList(
        Paths.get(JAVA_HOME, "bin", "java").toString(),
        "-Dlogback.configurationFile=" + getGraalLogbackConfig(),
        "-agentlib:native-image-agent=config-merge-dir=" + getConfigOutputDirectory(), // native image tracing agent
        "-cp", getIqCliJar().getAbsolutePath(),
        "com.sonatype.insight.scan.cli.GraalPolicyEvaluatorCli"
    ));

    if (ssl) {
      String trustStorePath = SslProperties.SERVER_STORE_FILE.getAbsolutePath();
      String keyStorePath = SslProperties.CLIENT_STORE_FILE.getAbsolutePath();
      command.add(1, addParam(KeyStoreInfo.TRUST_STORE_PATH_PROPERTY, trustStorePath));
      command.add(1, addParam(KeyStoreInfo.TRUST_STORE_PASSWORD_PROPERTY, SslProperties.TRUST_STORE_PASSWORD));
      command.add(1, addParam(KeyStoreInfo.KEY_STORE_PATH_PROPERTY, keyStorePath));
      command.add(1, addParam(KeyStoreInfo.KEY_STORE_PASSWORD_PROPERTY, SslProperties.KEY_STORE_PASSWORD));
    }

    command.addAll(params);

    log.debug("Executing: " + String.join(" ", command));

    ProcessResult processResult = new ProcessExecutor()
        .command(command)
        .environment(environment)
        .timeout(10, TimeUnit.SECONDS)
        .redirectOutput(Slf4jStream.of(AbstractPolicyEvaluatorTest.class).asInfo())
        .redirectError(Slf4jStream.of(AbstractPolicyEvaluatorTest.class).asError())
        .execute();

    assertThat(processResult.getExitValue()).isEqualTo(expectedExitCode);
    if (expectedExitException) {
      throw new ExitException(expectedExitCode);

      // Note: We cannot assert any of the exceptions defined in the runner as that can only happen within the JVM
    }
  }

  private ClientScanResult scan(final ProprietaryConfig proprietaryConfig, final RestClient restClient)
      throws Exception
  {
    run();

    String dir = params.get(params.indexOf("-o") + 1);
    File scanOutputDir = new File(dir);
    assertThat(scanOutputDir.exists())
        .as("Scan output directory not found. '-o' option required for scan tests")
        .isTrue();

    File[] scanFiles = scanOutputDir.listFiles(file -> file.getName().matches("scan-.*\\.xml\\.gz"));

    // currently no third party scan tests in ExpandedCoveragePolicyEvaluatorTest
    boolean hasThirdPartScanContent = false;

    return new ClientScanResult(scanFiles[0], hasThirdPartScanContent);
  }

  private String getGraalLogbackConfig() {
    URL url = getClass().getClassLoader().getResource("logback-graal.xml");
    assertThat(url).as("Could not find 'logback-graal.xml' in the test classpath").isNotNull();
    File graalLogbackConfig = new File(url.getFile());
    assertThat(graalLogbackConfig.exists()).as("Could not find 'logback-graal.xml' in the test classpath").isTrue();
    return graalLogbackConfig.getAbsolutePath();
  }

  /**
   * Get the directory to output the config files for the {@code native-image} tool. This will be
   * /target/native-image-configs and we need to generate empty json files that each particular file expects (empty
   * object or empty array etc...) which allows us to use the 'config-merge-dir' option
   */
  private File getConfigOutputDirectory() throws IOException {
    File configOutputDirectory = Paths.get("target", "native-image-configs").toFile();
    log.info("Storing generated native-image config files in: " + configOutputDirectory.getAbsolutePath());

    createDefaultConfigIfDoesNotExist(configOutputDirectory, "jni-config.json", "[]");
    createDefaultConfigIfDoesNotExist(configOutputDirectory, "proxy-config.json", "[[]]");
    createDefaultConfigIfDoesNotExist(configOutputDirectory, "reflect-config.json", "[]");
    createDefaultConfigIfDoesNotExist(configOutputDirectory, "resource-config.json", "{}");
    return configOutputDirectory;
  }

  private void createDefaultConfigIfDoesNotExist(
      final File configOutputDirectory,
      final String filename,
      final String contents)
      throws IOException
  {
    File configFile = new File(configOutputDirectory, filename);
    if (!configFile.exists()) {
      FileUtils.writeStringToFile(configFile, contents, Charset.defaultCharset());
    }
  }

  private File getIqCliJar() {
    File iqCliJar = Paths.get("target", "native-image-jar", "nexus-iq-cli.jar").toFile();
    assertThat(iqCliJar).isNotEmpty();
    log.info("Using IQ CLI jar for native-image config generation: " + iqCliJar.getAbsolutePath());
    return iqCliJar;
  }

  /**
   * Some of the log assertions have newlines in them. When executed as a process these come out as two fully separate
   * lines so here we split them so they can be asserted individually.
   */
  private List<String> split(final String log) {
    return Arrays.asList(log.split("\\r?\\n"));
  }

  private String addParam(final String name, final String value) {
    return "-D" + name + "=" + value;
  }
}
