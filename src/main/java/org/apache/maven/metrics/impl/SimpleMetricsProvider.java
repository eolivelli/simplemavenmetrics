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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.maven.metrics.impl;

import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import org.apache.maven.metrics.Counter;
import org.apache.maven.metrics.Gauge;
import org.apache.maven.metrics.MetricsContext;
import org.apache.maven.metrics.MetricsProvider;
import org.apache.maven.metrics.MetricsProviderLifeCycleException;
import org.apache.maven.metrics.Summary;
import org.apache.maven.metrics.SummarySet;
import org.codehaus.plexus.logging.Logger;

/**
 * Default implementation of {@link MetricsProvider}.<br>
 * It does not implement a real hierarchy of contexts, but metrics are flattened
 * in a single namespace.<br>
 */
public class SimpleMetricsProvider implements MetricsProvider {

    private final DefaultMetricsContext rootMetricsContext = new DefaultMetricsContext();
    private final Logger log;

    public SimpleMetricsProvider(Logger log) {
        this.log = log;
    }

    @Override
    public void start() throws MetricsProviderLifeCycleException {
    }

    @Override
    public MetricsContext getRootContext() {
        return rootMetricsContext;
    }

    @Override
    public void stop() {
        boolean dumpAtEnd = Boolean.getBoolean("metrics.dumpAtEnd");
        if (dumpAtEnd) {
            log.info("Maven Runtime Execution Metrics");
            rootMetricsContext.dump((k, v) -> {
                log.info(k + ":" + v);
            });
        }
        // release all references to external objects
        rootMetricsContext.gauges.clear();
    }

    @Override
    public void resetAllValues() {
        rootMetricsContext.reset();
    }

    private static final class DefaultMetricsContext implements MetricsContext {

        private final ConcurrentMap<String, Gauge> gauges = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, String> gaugesDescriptions = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, SimpleCounter> counters = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, AvgMinMaxPercentileCounter> summaries = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, AvgMinMaxPercentileCounterSet> summarySets = new ConcurrentHashMap<>();

        @Override
        public MetricsContext getContext(String name) {
            // no hierarchy yet
            return this;
        }

        @Override
        public Counter getCounter(String name, String description) {
            return counters.computeIfAbsent(name, (n) -> {
                return new SimpleCounter(n, description);
            });
        }

        @Override
        public void registerGauge(String name, String description, Gauge gauge) {
            Objects.requireNonNull(gauge, "Cannot register a null Gauge for " + name);
            gauges.put(name, gauge);
            gaugesDescriptions.put(name, description);
        }

        @Override
        public void unregisterGauge(String name) {
            gauges.remove(name);
            gaugesDescriptions.remove(name);
        }

        @Override
        public Summary getSummary(String name, String description) {
            return summaries.computeIfAbsent(name, (n) -> {
                return new AvgMinMaxPercentileCounter(name, description);
            });

        }

        @Override
        public SummarySet getSummarySet(String name, String description) {
            return summarySets.computeIfAbsent(name, (n) -> {
                return new AvgMinMaxPercentileCounterSet(name, description);
            });

        }

        void dump(BiConsumer<String, Object> sink) {
            gauges.forEach((name, metric) -> {
                Number value = metric.get();
                if (value != null) {
                    sink.accept(gaugesDescriptions.get(name) + "(" + name + ")", value);
                }
            });
            counters.values().forEach(metric -> {
                sink.accept(metric.getDescription() + " (" + metric.getName() + ")", metric.get());
            });
            summaries.values().forEach(metric -> {
                metric.values().forEach((k, v) -> {
                    sink.accept(metric.getDescription() + " (" + k + ")", v);

                });
            });
            summarySets.values().forEach(metric -> {
                metric.values().forEach((k, v) -> {
                    sink.accept(metric.getDescription() + " (" + k + ")", v);

                });
            });
        }

        void reset() {
            counters.values().forEach(metric -> {
                metric.reset();
            });
            summaries.values().forEach(metric -> {
                metric.reset();
            });
            summarySets.values().forEach(metric -> {
                metric.reset();
            });
            // no need to reset gauges
        }

    }

}
