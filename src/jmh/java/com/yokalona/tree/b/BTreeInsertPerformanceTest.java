package com.yokalona.tree.b;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.JavaFlightRecorderProfiler;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

import static com.yokalona.tree.b.Helper.formSample;
import static com.yokalona.tree.b.Helper.randomKey;

@State(Scope.Benchmark)
public class BTreeInsertPerformanceTest {

    @State(Scope.Benchmark)
    public static class ExecutionPlan {
        @Param({"4", "32", "256", "2048", "16384", "131072"})
        public int capacity;

        @Param({"1000", "10000", "100000", "1000000"})
        public int sampleSize;

        private Integer[] data;

        public BTree<Integer, Integer> bTree;

        @Setup(Level.Trial)
        public void prepareData() {
            this.data = formSample(sampleSize);
            this.bTree = new BTree<>(capacity);
        }

        @Setup(Level.Iteration)
        public void fillData() {
            bTree.clear();
            for (int key : data) {
                bTree.insert(key, key);
            }
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void btree_insert(ExecutionPlan executionPlan, Blackhole blackhole) {
        Integer[] data = executionPlan.data;
        BTree<Integer, Integer> bTree = executionPlan.bTree;
        Integer key = randomKey(data);
        blackhole.consume(bTree.insert(key, randomKey(data)));
    }

}