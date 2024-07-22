/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
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

    @FuzzTest(maxDuration = "60s")
    fun bubbleSort(data: FuzzedDataProvider) {
        val size = data.consumeInt(0, 1000)
        val ints = data.forceConsumeInts(size)//.toTypedArray()

        val persistentHistory = historyList(ints.toPersistentList())
        val listHistory = historyList(ints.toMutableList())

        listOf(1, 2, 3).sorted()

        bubbleSort(persistentHistory)
        bubbleSort(listHistory)

        assertTrue(persistentHistory == listHistory)
    }

    @FuzzTest(maxDuration = "60s")
    fun randomOps(data: FuzzedDataProvider) {
        val first = data.consumeInts(1000).toList()
        val memorisingList = MemorisingList(mutableListOf(first.toPersistentList()))

        val opsNum = data.consumeInt(10, 1000)
        repeat(opsNum) {
            val op = data.consumeListOperation(memorisingList.last)
            memorisingList.applyOperation(op)
        }
        memorisingList.validateInvariants()
        memorisingList.validateArrayList()
    }

    @FuzzTest(maxDuration = "60s")
    fun mapRandomOps(data: FuzzedDataProvider) {
        val firstMap = data.consumeInts(1000)
            .asSequence().chunked(2).filter { it.size == 2 }
            .map { list -> list[0] to list[1] }
            .toMap()

        val memorisingMap = MemorisingMap(mutableListOf(firstMap.toPersistentMap()))

        val opsNum = data.consumeInt(10, 1000)
        repeat(opsNum) {
            val op = data.consumeMapOperation(memorisingMap.last)
            memorisingMap.applyOperation(op)
        }
        memorisingMap.validateInvariants()
        memorisingMap.validateArrayList()
    }
}
