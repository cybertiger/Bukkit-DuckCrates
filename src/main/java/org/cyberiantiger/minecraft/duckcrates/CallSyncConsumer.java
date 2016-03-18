/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cyberiantiger.minecraft.duckcrates;

import java.util.function.Consumer;

/**
 *
 * @author antony
 */
public class CallSyncConsumer<T> implements Consumer<T> {
    private final Consumer<T> delegate;
    private final Main outer;

    public CallSyncConsumer(Consumer<T> delegate, final Main outer) {
        this.outer = outer;
        this.delegate = delegate;
    }

    @Override
    public void accept(final T t) {
        outer.getServer().getScheduler().runTask(outer, () -> {
            delegate.accept(t);
        });
    }
    
}
