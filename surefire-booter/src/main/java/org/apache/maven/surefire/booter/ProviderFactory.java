package org.apache.maven.surefire.booter;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.lang.reflect.Method;
import java.util.Iterator;
import org.apache.maven.surefire.providerapi.SurefireProvider;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.ReflectionUtils;


/**
 * Creates the surefire provider.
 * <p/>
 *
 * @author Kristian Rosenvold
 */
public class ProviderFactory
{
    private final StartupConfiguration startupConfiguration;

    private final ProviderConfiguration providerConfiguration;

    private final ClassLoader surefireClassLoader;

    private final ClassLoader testsClassLoader;

    private final SurefireReflector surefireReflector;

    private final Object reporterManagerFactory;

    private static final Class[] invokeParamaters = new Class[]{ Object.class };


    public ProviderFactory( StartupConfiguration startupConfiguration, ProviderConfiguration providerConfiguration,
                            ClassLoader surefireClassLoader, ClassLoader testsClassLoader,
                            Object reporterManagerFactory )
    {
        this.providerConfiguration = providerConfiguration;
        this.surefireClassLoader = surefireClassLoader;
        this.startupConfiguration = startupConfiguration;
        this.surefireReflector = new SurefireReflector( surefireClassLoader );
        this.testsClassLoader = testsClassLoader;
        this.reporterManagerFactory = reporterManagerFactory;
    }

    public SurefireProvider createProvider()
    {
        ClassLoader context = java.lang.Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader( surefireClassLoader );

        StartupConfiguration starterConfiguration = startupConfiguration;

        final Object o = surefireReflector.createBooterConfiguration( surefireClassLoader, reporterManagerFactory );
        surefireReflector.setTestSuiteDefinitionAware( o, providerConfiguration.getTestSuiteDefinition() );
        surefireReflector.setProviderPropertiesAware( o, providerConfiguration.getProviderProperties() );
        surefireReflector.setReporterConfigurationAware( o, providerConfiguration.getReporterConfiguration() );
        surefireReflector.setTestClassLoaderAware( o, surefireClassLoader, testsClassLoader );
        surefireReflector.setTestArtifactInfoAware( o, providerConfiguration.getTestArtifact() );
        surefireReflector.setIfDirScannerAware( o, providerConfiguration.getDirScannerParams() );

        Object provider = surefireReflector.instantiateProvider( starterConfiguration.getProviderClassName(), o );
        Thread.currentThread().setContextClassLoader( context );

        return new ProviderProxy( provider );
    }


    private class ProviderProxy
        implements SurefireProvider
    {
        private final Object providerInOtherClassLoader;


        private ProviderProxy( Object providerInOtherClassLoader )
        {
            this.providerInOtherClassLoader = providerInOtherClassLoader;
        }

        public Iterator getSuites()
        {
            return (Iterator) ReflectionUtils.invokeGetter( providerInOtherClassLoader, "getSuites" );
        }

        public RunResult invoke( Object forkTestSet )
            throws TestSetFailedException, ReporterException
        {
            final Method invoke =
                ReflectionUtils.getMethod( providerInOtherClassLoader.getClass(), "invoke", invokeParamaters );

            final Object result = ReflectionUtils.invokeMethodWithArray( providerInOtherClassLoader, invoke,
                                                                         new Object[]{ forkTestSet } );
            return (RunResult) surefireReflector.convertIfRunResult( result );
        }

        public void cancel()
        {
            final Method invoke =
                ReflectionUtils.getMethod( providerInOtherClassLoader.getClass(), "cancel", new Class[]{ } );
            ReflectionUtils.invokeMethodWithArray( providerInOtherClassLoader, invoke, null );
        }
    }
}
