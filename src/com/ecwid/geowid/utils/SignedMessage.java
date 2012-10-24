/*
 * Copyright (c) 2012, Creative Development LLC
 * Available under the New BSD license
 * see http://github.com/injecto/geowid for details
 */

package com.ecwid.geowid.utils;

import java.io.Serializable;
import java.util.Arrays;

/**
 * подписанное сообщение
 */
public final class SignedMessage implements Serializable {

    /**
     * ctor
     * @param message сообщение
     * @param signature подпись
     */
    public SignedMessage(byte[] message, byte[] signature) {
        this.message = Arrays.copyOf(message, message.length);
        this.signature = Arrays.copyOf(signature, signature.length);
    }

    public byte[] getMessage() {
        return Arrays.copyOf(message, message.length);
    }

    public byte[] getSignature() {
        return Arrays.copyOf(signature, signature.length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SignedMessage that = (SignedMessage) o;

        return Arrays.equals(message, that.message) && Arrays.equals(signature, that.signature);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(message);
        result = 31 * result + Arrays.hashCode(signature);
        return result;
    }

    private final byte[] message;
    private final byte[] signature;
}
