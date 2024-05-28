package com.yokalona.tree.b;

import com.yokalona.tree.BTree;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

import static com.yokalona.tree.b.Helper.*;

@State(Scope.Benchmark)
public class BTreeInsertPerformanceTest {

    public static final int OPERATIONS = 20000;

    @State(Scope.Benchmark)
    public static class ExecutionPlan {
        @Param({"4", "32", "256", "2048", "16384", "131072"})
        public int capacity;
        public int key = 0;
        public SpyKey[] data;

        public BTree<SpyKey, Integer> bTree;

        @Setup(Level.Trial)
        public void fill() {
            this.data = Helper.formSample(OPERATIONS);
        }

        @Setup(Level.Iteration)
        public void prepareData() {
            Helper.SpyKey.reset();
            this.bTree = new BTree<>(capacity);
            shuffle(data);
            key = 0;
        }

    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @Warmup(iterations = 100, batchSize = OPERATIONS)
    @Measurement(iterations = 50, batchSize = OPERATIONS)
    @OperationsPerInvocation(OPERATIONS)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void btree_insert_base(ExecutionPlan executionPlan, Blackhole blackhole) {
        SpyKey.reset();
        BTree<SpyKey, Integer> bTree = executionPlan.bTree;
        SpyKey[] data = executionPlan.data;
        blackhole.consume(bTree.insert(data[executionPlan.key++], executionPlan.key));
    }

}