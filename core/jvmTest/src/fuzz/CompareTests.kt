/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.collections.immutable.*
import kotlinx.collections.immutable.implementations.immutableList.MAX_BUFFER_SIZE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import tests.fuzz.HistoryList.Companion.historyList

class CompareTests {
    private fun bubbleSort(historyList: HistoryList<Int>) {
        var done = false
        while (!done) {
            done = true
            val list = historyList.history.last()
            for (i in 0..<list.lastIndex) {
                if (list[i] > list[i + 1]) {
                    done = false
                    historyList.history.add(historyList.history.last().persistentSwap(i, i + 1))
                }
            }
        }
    }

    @FuzzTest(maxDuration = "2h")
    fun bubbleSort(data: FuzzedDataProvider) {
        val size = data.consumeInt(0, initSize)
        val ints = data.forceConsumeInts(size)

        val persistentHistory = historyList(ints.toPersistentList())
        val listHistory = historyList(ints.toMutableList())

        listOf(1, 2, 3).sorted()

        bubbleSort(persistentHistory)
        bubbleSort(listHistory)

        assertTrue(persistentHistory == listHistory)
    }

    @FuzzTest(maxDuration = "2h")
    fun smallPersistentVectorRandomOps(data: FuzzedDataProvider) {
        val first = data.forceConsumeInts(initSize).toList()
        val memorisingList = MemorisingList(mutableListOf(first.toPersistentList()))

        memorisingList.last.iterator()

        val opsNum = data.consumeInt(10, 1000)
        repeat(opsNum) {
            val op = data.consumeListOperation(memorisingList.last)
            memorisingList.applyOperation(op)
            if (memorisingList.last.size <= MAX_BUFFER_SIZE) {
                memorisingList.history.removeLast()
                memorisingList.operations.removeLast()
            }
        }
        memorisingList.validate()
    }

    @FuzzTest(maxDuration = "2h")
    fun persistentVectorRandomOps(data: FuzzedDataProvider) {
        val first = data.forceConsumeInts(2).toList()
        val memorisingList = MemorisingList(mutableListOf(first.toPersistentList()))

        val opsNum = 100 // data.consumeInt(10, 1000)
        repeat(opsNum) {
            val op = data.consumeListOperation(memorisingList.last)
            memorisingList.applyOperation(op)
            if (memorisingList.last.size > MAX_BUFFER_SIZE) {
                memorisingList.history.removeLast()
                memorisingList.operations.removeLast()
            }
        }
        memorisingList.validate()
    }


    @FuzzTest(maxDuration = "2h")
    fun mapRandomOps(data: FuzzedDataProvider) {
        val firstMap = data.consumeInts(initSize)
            .asSequence().chunked(2).filter { it.size == 2 }
            .map { list -> list[0] to list[1] }
            .toMap()

        val memorisingMap = MemorisingMap(mutableListOf(firstMap.toPersistentMap()))

        val opsNum = 10240 // data.consumeInt(10, 1000)
        repeat(opsNum) {
            val op = data.consumeMapOperation(memorisingMap.last)
            memorisingMap.applyOperation(op)
        }

        memorisingMap.validate()
    }


    @FuzzTest(maxDuration = "2h")
    fun hashMapRandomOps(data: FuzzedDataProvider) {
        val firstMap = data.forceConsumeInts(100)
            .asSequence().chunked(2).filter { it.size == 2 }
            .map { list -> list[0] to list[1] }
            .toMap()

        val memorisingMap = MemorisingMap(mutableListOf(firstMap.toPersistentHashMap()))

        val opsNum = data.consumeInt(10, 1000)
        repeat(opsNum) {
            val op = data.consumeMapOperation(memorisingMap.last)
            memorisingMap.applyOperation(op)
        }

        memorisingMap.validate()
    }


    @FuzzTest(maxDuration = "2h")
    fun listBuilderRandomOps(data: FuzzedDataProvider) {
        val initSize = 16 // data.consumeInt(0, Int.MAX_VALUE / 1024)
        val init = data.forceConsumeInts(initSize)
        val opsNum = 1024 //128 //data.consumeInt(0, Int.MAX_VALUE)
        val builder = init.toPersistentList().builder()
        val arrayList = init.toMutableList()
        repeat(opsNum) {
            val op = data.consumeListOperation(builder)
            op.apply(builder)
            op.apply(arrayList)
            assertEquals(arrayList, builder)
        }
    }


    @FuzzTest(maxDuration = "2h")
    fun orderedMapBuilderRandomOps(data: FuzzedDataProvider) {
        val firstMap = data.forceConsumeInts(100)
            .asSequence().chunked(2).filter { it.size == 2 }
            .map { list -> list[0] to list[1] }
            .toMap()

        val builder = firstMap.toPersistentMap().builder()
        val hashMap = firstMap.toMutableMap()

        assertEquals(hashMap, builder)

        val opsNum = data.consumeInt(10, 1000)
        repeat(opsNum) {
            val op = data.consumeMapOperation(builder)
            op.apply(builder)
            op.apply(hashMap)
            assertEquals(hashMap, builder)
        }
    }


    @FuzzTest(maxDuration = "2h")
    fun hashMapBuilderRandomOps(data: FuzzedDataProvider) {
        val firstMap = data.forceConsumeInts(100)
            .asSequence().chunked(2).filter { it.size == 2 }
            .map { list -> list[0] to list[1] }
            .toMap()

        val builder = persistentHashMapOf<Int, Int>().builder().apply { putAll(firstMap) }
        val hashMap = hashMapOf<Int, Int>().apply { putAll(firstMap) }

        assertEquals(hashMap, builder)

        val opsNum = data.consumeInt(10, 1000)
        val ops = mutableListOf<MapOperation>()
        repeat(opsNum) {
            val op = data.consumeMapOperation(builder)
            ops += op
            op.apply(builder)
            op.apply(hashMap)
            assertEquals(hashMap, builder)
        }
    }
}
