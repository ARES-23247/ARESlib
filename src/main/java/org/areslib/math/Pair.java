package org.areslib.math;
public class Pair<A, B> { public A getFirst() { return null; } public B getSecond() { return null; } public static <A, B> Pair<A, B> of(A a, B b) { return new Pair<>(); } }