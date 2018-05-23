package com.dadoutek.uled.intf;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

public class HttpResultFunc<T> implements Function<Throwable, Observable<T>> {
    @Override
    public Observable<T> apply(Throwable throwable) throws Exception {
        return Observable.error(ExceptionEngine.INSTANCE.handleException(throwable));
    }
}
