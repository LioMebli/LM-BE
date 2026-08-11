package com.vansisto.lmbe;

/**
 * Names {@code application-test.yaml}. A test that loads a Spring context and forgets this
 * profile silently runs without the suite's own settings, which is the failure SC-007 is
 * about — so the name is a constant rather than a string repeated per class.
 */
public final class TestProfile {

    public static final String NAME = "test";

    private TestProfile() {
    }
}
