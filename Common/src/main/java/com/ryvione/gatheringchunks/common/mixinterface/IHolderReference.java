package com.ryvione.gatheringchunks.common.mixinterface;

public interface IHolderReference<T> {
    void gc$setValue(T value);

    T gc$getValue();
}
