/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.fuzz

import kotlinx.collections.immutable.PersistentList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue


class MemorisingList(val history: MutableList<PersistentList<Int>>) {
    val last
        get() = history.last()

    val operations: MutableList<ListOperation> = mutableListOf()

    fun applyOperation(operation: ListOperation) {
        val nextList = operation.apply(last)
        operations += operation
        history += nextList
    }

    fun validateInvariants() {
        history.asSequence()
            .zipWithNext()
            .zip(operations.asSequence())
            .forEach { (lists, operation) ->
                operation.validateInvariants(
                    lists.first,
                    lists.second
                )
            }
    }

    fun validateArrayList() {
        val list = history[0].toMutableList()
        assertTrue(list == history[0])
        history.asSequence()
            .drop(1)
            .zip(operations.asSequence())
            .forEach { (persList, operation) ->
                operation.apply(list)
                assertEquals(list, persList)
//                assertTrue(list == persList)
            }
    }

    fun validateReverse() {
        history.asSequence()
            .zipWithNext()
            .zip(operations.asSequence())
            .forEach { (pair, operation) ->
                val (preList, postList) = pair
                val reversed = operation.reverse(postList) ?: return@forEach
                assertEquals(preList, reversed)
            }
    }
}