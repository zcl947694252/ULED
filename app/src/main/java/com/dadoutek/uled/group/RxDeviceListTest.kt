package com.dadoutek.uled.group

class RxDeviceListTest {
    val listA = mutableListOf<Int>()
    val listB = mutableListOf<Int>()

    fun subscribe() {
        val deviceList = RxDeviceList<Int>()

        val observable = deviceList.getObservable  { i -> i > 2 }
        val disposableA = observable.subscribe({
                    listA.clear()
                    listA.addAll(it)
                    println("listA onNext result = $it")
                },
                {
                    print(it)
                }
        )

        val observable2 = deviceList.getObservable { i -> i <= 2 }
        val disposableB =  observable2.subscribe(
                {
                    listB.clear()
                    listB.addAll(it)
                    println("listB onNext result = $it")
                },
                {
                    print(it)
                }
        )


        deviceList.addAll(listOf(1, 2, 3, 4, 5))
        Thread.sleep(1000)

        disposableA.dispose()
        deviceList.removeAt(deviceList.lastIndex)
        Thread.sleep(1000)

        deviceList.removeAt(0)
        Thread.sleep(1000)

        disposableB.dispose()
        deviceList.removeAt(deviceList.lastIndex)
    }
}