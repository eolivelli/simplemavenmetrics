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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.maven.metrics.Summary;

/**
 * Generic long counter that keep track of min/max/avg. The counter is
 * thread-safe
 */
public class AvgMinMaxCounter extends Summary {

    private final String name;
    private final AtomicLong total = new AtomicLong();
    private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong max = new AtomicLong(Long.MIN_VALUE);
    private final AtomicLong count = new AtomicLong();

    public AvgMinMaxCounter(String name) {
        this.name = name;
    }

    public void addDataPoint(long value) {
        total.addAndGet(value);
        count.incrementAndGet();
        setMin(value);
        setMax(value);
    }

    private void setMax(long value) {
        long current;
        while (value > (current = max.get()) && !max.compareAndSet(current, value)) {
            // no op
        }
    }

    private void setMin(long value) {
        long current;
        while (value < (current = min.get()) && !min.compareAndSet(current, value)) {
            // no op
        }
    }

    public double getAvg() {
        // There is possible race-condition but we don't need the stats to be
        // extremely accurate.
        long currentCount = count.get();
        long currentTotal = total.get();
        if (currentCount > 0) {
            double avgLatency = currentTotal / (double) currentCount;
            BigDecimal bg = new BigDecimal(avgLatency);
            return bg.setScale(4, RoundingMode.HALF_UP).doubleValue();
        }
        return 0;
    }

    public long getCount() {
        return count.get();
    }

    public long getMax() {
        long current = max.get();
        return (current == Long.MIN_VALUE) ? 0 : current;
    }

    public long getMin() {
        long current = min.get();
        return (current == Long.MAX_VALUE) ? 0 : current;
    }

    public long getTotal() {
        return total.get();
    }

    public void resetMax() {
        max.set(getMin());
    }

    public void reset() {
        count.set(0);
        total.set(0);
        min.set(Long.MAX_VALUE);
        max.set(Long.MIN_VALUE);
    }

    public void add(long value) {
        addDataPoint(value);
    }

    public Map<String, Object> values() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put(name + "_avg", this.getAvg());
        m.put(name + "_min", this.getMin());
        m.put(name + "_max", this.getMax());
        m.put(name + "_count", this.getCount());
        m.put(name + "_sum", this.getTotal());
        return m;
    }

}
