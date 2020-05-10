package org.apache.maven.metrics;


/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.classrealm.ClassRealmManager;
import org.apache.maven.metrics.MetricsContext;
import org.apache.maven.metrics.MetricsProvider;
import org.apache.maven.metrics.MetricsSystem;
import org.apache.maven.metrics.impl.SimpleMetricsProvider;
import org.apache.maven.metrics.impl.NullMetricsProvider;
import org.codehaus.plexus.logging.Logger;

/**
 * Default Implementation of Metrics System Runtime
 * @author Enrico Olivelli
 */
@Named( MetricsSystem.HINT )
@Singleton
public class SimpleMetricsSystem extends MetricsSystem {
 
    private final Logger log;
    private final MetricsProvider metricsProvider;

    @Inject
    public SimpleMetricsSystem(Logger log, ClassRealmManager classRealmManager) throws Exception {
        this.log = log;
        log.debug("Initializing SimpleMetricsSystem");
        metricsProvider = new SimpleMetricsProvider(log);
    }
    
    @Override
    public MetricsContext getMetricsContext() {
        return metricsProvider.getRootContext();
    }

    @Override
    public MetricsProvider getMetricsProvider() {
        return metricsProvider;
    }
    
}
