/**
 * (c) 2014 StreamSets, Inc. All rights reserved. May not
 * be copied, modified, or distributed in whole or part without
 * written consent of StreamSets, Inc.
 */
package com.streamsets.pipeline.main;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.streamsets.pipeline.metrics.MetricsModule;
import com.streamsets.pipeline.util.Configuration;
import dagger.Module;
import dagger.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Module(library = true, injects = {BuildInfo.class, RuntimeInfo.class, Configuration.class},
    includes = MetricsModule.class)
public class RuntimeModule {
  private static final Logger LOG = LoggerFactory.getLogger(RuntimeModule.class);
  public static final String DATA_COLLECTOR_BASE_HTTP_URL = "sdc.base.http.url";
  public static final String SDC_PROPERTY_PREFIX = "sdc";
  public static final String SDC_EXECUTION_MODE_KEY = "sdc.execution.mode";
  private static final String SDC_EXECUTION_MODE_DEFAULT = "standalone";

  private static List<ClassLoader> stageLibraryClassLoaders = ImmutableList.of(RuntimeModule.class.getClassLoader());

  public static synchronized void setStageLibraryClassLoaders(List<? extends ClassLoader> classLoaders) {
    stageLibraryClassLoaders = ImmutableList.copyOf(classLoaders);
  }

  @Provides @Singleton
  public BuildInfo provideBuildInfo() {
    return new BuildInfo();
  }

  @Provides @Singleton
  public RuntimeInfo provideRuntimeInfo(MetricRegistry metrics) {
    RuntimeInfo info = new RuntimeInfo(SDC_PROPERTY_PREFIX, metrics, stageLibraryClassLoaders);
    info.init();
    return info;
  }

  @Provides @Singleton
  public Configuration provideConfiguration(RuntimeInfo runtimeInfo) {
    Configuration.setFileRefsBaseDir(new File(runtimeInfo.getConfigDir()));
    Configuration conf = new Configuration();
    File configFile = new File(runtimeInfo.getConfigDir(), "sdc.properties");
    if (configFile.exists()) {
      try {
        conf.load(new FileReader(configFile));
        runtimeInfo.setBaseHttpUrl(conf.get(DATA_COLLECTOR_BASE_HTTP_URL, runtimeInfo.getBaseHttpUrl()));
        runtimeInfo.setExecutionMode(conf.get(SDC_EXECUTION_MODE_KEY, SDC_EXECUTION_MODE_DEFAULT));
        if (runtimeInfo.getExecutionMode() == RuntimeInfo.ExecutionMode.SLAVE) {
          runtimeInfo.setSDCToken(UUID.randomUUID().toString());
        }
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    } else {
      LOG.error("Error did not find sdc.properties at expected location: {}", configFile);
    }
    return conf;
  }

}
