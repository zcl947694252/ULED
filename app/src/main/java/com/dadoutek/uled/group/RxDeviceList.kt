package com.dadoutek.uled.group

import com.blankj.utilcode.util.LogUtils
import io.reactivex.Observable
import io.reactivex.ObservableEmitter

class RxDeviceList<T> : ArrayList<T>() {
    data class ObservableListHmValue<T>(val observableEmitter: ObservableEmitter<List<T>>, val predicate: (T) -> Boolean)

    private val observableListHm = hashMapOf<Int, ObservableListHmValue<T>>()

    override fun add(element: T): Boolean {
        val ret = super.add(element)
        emitData()

        return ret
    }

    override fun add(index: Int, element: T) {
        super.add(index, element)
        emitData()
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        val ret = super.addAll(index, elements)
        emitData()
        return ret
    }

    override fun addAll(elements: Collection<T>): Boolean {
        val ret = super.addAll(elements)
        emitData()
        return ret
    }


    override fun remove(element: T): Boolean {
        val ret = super.remove(element)
        emitData()
        return ret
    }

    override fun removeAt(index: Int): T {
        val ret = super.removeAt(index)
        emitData()
        return ret
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        val ret = super.removeAll(elements)
        emitData()
        return ret
    }

    override fun removeRange(fromIndex: Int, toIndex: Int) {
        super.removeRange(fromIndex, toIndex)
        emitData()
    }

    override fun clear() {
        super.clear()
        emitData()
    }

    /**
     * 给所有观察者发送数据
     */
    private fun emitData() {
        for ((_, value) in observableListHm) {
            val list = this.filter(value.predicate) //按照要求过滤数据，得到结果list
            if (!value.observableEmitter.isDisposed)        //如果没有取消订阅才能发送消息
                value.observableEmitter.onNext(list)        //发送list给观察者
            else
                LogUtils.d("index has been disposed")
        }
}

    /**
     * 根据 @param predicate 返回对应的Observable。 订阅该Observable后，只要该List的数据改变了，就会通过该Observable发送数据给观察者。
     * @param index  第几个订阅，一般不用填。
     */
    fun getObservable(index: Int = observableListHm.size, predicate: (T) -> Boolean): Observable<List<T>> {
        return Observable.create<List<T>> {
            observableListHm[index] = ObservableListHmValue(it, predicate)
//            Observable.create { emitter: ObservableEmitter<List<T>> ->
//                observableListHm[index] = ObservableListHmValue(emitter,predicate)
//            }
        }
                .doOnDispose {
                    observableListHm.remove(index)      //如果取消订阅了，直接把对应的index从HashMap中移除
                }
    }

}