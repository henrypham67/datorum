package io.beandev.datorum.data;

public record BigId(long gen, long num) {
    public BigId(long num) {
        this(0, num);
    }
}
