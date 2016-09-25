package dog.woofwoofinc.grace.repository

import rx.Observer

open class ObserverAdapter<T> : Observer<T> {
    override fun onCompleted() { }
    override fun onError(e: Throwable) { }
    override fun onNext(t: T) { }
}

fun <T> emptyObserver(): Observer<T> {
    return ObserverAdapter()
}
