/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.scan.model.ScanMetadata;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.StringConverter;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractParameters
{
  static {
    String value = System.getProperty("java.net.useSystemProxies");
    if (value == null) {
      /*
       * System proxies are badly implemented: On some platforms (e.g. Ubuntu 12), unconfigured proxies show up as
       * proxies with empty host and port 0 in Java. Obviously, using such a proxy yields nothing but connection
       * errors. Regardless of a URL's protocol, Java's socket factory will first try a socket: connection to its
       * destination. Now imagine what happens, if the proxy selector returns an invalid proxy for the socket:
       * connection (because the user has only configured a proxy for http:)... Furthermore,
       * java.net.useSystemProxies gets only evaluated once in a static initializer, so we can't just unset it
       * after we grabbed the default proxy. To fix this mess, we install a safe proxy selector, wrapping the
       * original one, to filter out the invalid proxies.
       */
      System.setProperty("java.net.useSystemProxies", "true");
      SafeProxySelector.install();
    }
  }

  private String[] args = {};

  private Exception error;

  @Parameter(names = "-D", description = "Configuration properties, e.g. -D key=value", hidden = true, 
      listConverter = StringConverter.class)
  private List<String> properties = new ArrayList<>();

  @Parameter(names = {"-o", "--output-directory"}, description = "Path to output directory for scan results",
             hidden = true)
  private File outputDirectory = new File(System.getProperty("java.io.tmpdir", ""), "nexus-iq").getAbsoluteFile();

  @Parameter(names = {"-i", "--application-id"}, description = "ID of the application on the IQ Server",
             required = true)
  private String applicationId;

  @Parameter(names = {"-s", "--server-url"},
             description = "URL to the IQ Server to which the scan result should be uploaded", required = true)
  private String serverUrl;

  @Parameter(names = { "-p", "--proxy" }, description = "Proxy to use, format <host[:port]>."
      + " If unspecified, the operating system will be queried for the proxy settings")
  private String proxy;

  @Parameter(names = { "-U", "--proxy-user" }, description = "Credentials to use for proxy, format <username:password>")
  private String proxyUser;

  @Parameter(names = {"-t", "--stage"}, validateValueWith = StageParameterValidator.class,
             converter = StageParameterConverter.class,
             description = "The stage to run analysis against. Accepted values: " + Stage.ID_DEVELOP + "|"
                 + Stage.ID_BUILD + "|" + Stage.ID_STAGE_RELEASE + "|" + Stage.ID_RELEASE + "|" + Stage.ID_OPERATE)
  private Stage stage = new Stage(Stage.ID_BUILD);

  @Parameter(names = { "-X", "--debug" }, description = "Enable debug logs."
      + " WARNING: This may expose sensitive information in the log.")
  private boolean debug;

  @Parameter(names = { "-q", "--quiet" }, description = "Restrict logs to errors", hidden = true)
  private boolean quiet;

  @Parameter(names = {"-e", "--ignore-system-errors"}, description = "Ignore system errors (IO, network, server, etc)")
  private boolean ignoreSystemErrors;

  @Parameter(names = { "-m", "--metadata-file" }, converter = ScanMetadataFileParameterConverter.class,
      description = "Path to a JSON file where meta information about the evaluation request, such as commit hash, " +
          "is located")
  private ScanMetadata scanMetadata;

  @Parameter(names = { "-v", "--version" }, description = "Show the IQ version this CLI was built from")
  private boolean version;

  @Parameter(names = { "-h", "--help" }, description = "Show this help screen")
  private boolean help;

  @Parameter(names = { "-b", "--base-dir" }, description = "Set the Base Directory for paths of components in reports",
      hidden = true)
  private File baseDir = null;

  @Parameter(names = { "-k", "--keep-scan-file" },
      description = "Flag to determine if CLI should keep the scan file. by default scan file is deleted",
      hidden = true)
  private boolean keepScanFile;

  public AbstractParameters() {
  }

  public String createUsageHelp() {

    JCommander jc;
    try {
      // NOTE: Be sure to use a fresh params instance to not have current state spoil default values
      jc = new JCommander(getClass().newInstance());
    }
    catch (Exception e) {
      throw new IllegalStateException(e);
    }
    jc.setProgramName(getProgramName());
    StringBuilder buffer = new StringBuilder();
    jc.usage(buffer);
    return buffer.toString();
  }

  protected abstract String getProgramName();

  protected void parse(String... args) {
    try {
      this.args = args.clone();
      error = null;
      JCommander jc = new JCommander(this);
      jc.parse(args);
    }
    catch (RuntimeException e) {
      error = e;
    }
  }

  public String[] getArgs() {
    return args;
  }

  public Exception getError() {
    return error;
  }

  public abstract List<String> getScanTargets();

  public abstract List<String> getModuleExcludes();

  public List<String> getProperties() {
    return properties;
  }

  public File getOutputDirectory() {
    return outputDirectory;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public String getServerUrl() {
    return serverUrl;
  }

  public abstract String getServerUser();

  public String getProxy() {
    return (proxy != null) ? proxy : selectProxy();
  }

  private String selectProxy() {
    URI uri;
    try {
      uri = new URI(serverUrl);
    }
    catch (URISyntaxException e) {
      // invalid URL, let the actual network request fail and take care of the error reporting
      return null;
    }
    String proxy = selectProxy(uri);
    if (proxy == null && "https".equalsIgnoreCase(uri.getScheme())) {
      uri = URI.create("http:" + uri.getRawSchemeSpecificPart());
      proxy = selectProxy(uri);
    }
    return proxy;
  }

  private static String selectProxy(URI uri) {
    ProxySelector selector = ProxySelector.getDefault();
    List<Proxy> proxies = (selector != null) ? selector.select(uri) : null;
    if (proxies != null) {
      for (Proxy proxy : proxies) {
        if (!Proxy.Type.HTTP.equals(proxy.type())) {
          continue;
        }
        if (!(proxy.address() instanceof InetSocketAddress)) {
          continue;
        }
        InetSocketAddress address = (InetSocketAddress) proxy.address();
        if (address.getHostName() == null || address.getHostName().isEmpty()) {
          continue;
        }
        return address.toString();
      }
    }
    return null;
  }

  public String getProxyUser() {
    return proxyUser;
  }

  public Stage getStage() {
    return stage;
  }

  public boolean isDebug() {
    return debug;
  }

  public boolean isQuiet() {
    return quiet;
  }

  public boolean isVersion() {
    return version;
  }

  public boolean isHelp() {
    return help;
  }

  public boolean isIgnoreSystemErrors() {
    return ignoreSystemErrors;
  }

  public ScanMetadata getScanMetadata() {
    return scanMetadata;
  }

  public File getBaseDir() {
    return baseDir;
  }

  public boolean isKeepScanFile() {
    return keepScanFile;
  }

  /*
   * Validator for the stage parameter
   */
  public static class StageParameterValidator
      implements IValueValidator<Stage>
  {
    @Override
    public void validate(String name, Stage value) throws ParameterException {
      if (!Stage.isValidStageTypeId(value.getStageTypeId())) {
        throw new ParameterException("An invalid stage was specified: " + name + " " + value);
      }
    }
  }

  /*
   * Converter to convert String to Stage object
   */
  public static class StageParameterConverter
      implements IStringConverter<Stage>
  {
    @Override
    public Stage convert(String value) {
      return new Stage(value.toLowerCase(Locale.ENGLISH));
    }
  }

  /*
   * Converter to convert the metadata filename into a Metadata object by loading the contents of the metadata file
   */
  public static class ScanMetadataFileParameterConverter
      implements IStringConverter<ScanMetadata>
  {
    @Override
    public ScanMetadata convert(final String value) {
      try {
        File file = new File(value);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(file, ScanMetadata.class);
      }
      catch (IOException e) {
        throw new ParameterException("The specified metadata file '" + value + "' is invalid due to " + e.getMessage());
      }
    }
  }
}
