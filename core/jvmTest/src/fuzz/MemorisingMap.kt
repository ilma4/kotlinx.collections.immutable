/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.fuzz

import kotlinx.collections.immutable.PersistentMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

class MemorisingMap(val history: MutableList<PersistentMap<Int, Int>>) {
    val last
        get() = history.last()

    val operations: MutableList<MapOperation> = mutableListOf()

    fun applyOperation(operation: MapOperation) {
        val nextList = operation.apply(last)
        operations += operation
        history += nextList
    }

    fun validateInvariants() {
        history.asSequence()
            .zipWithNext()
            .zip(operations.asSequence())
            .forEach { (lists, operation) -> operation.validate(lists.first, lists.second) }
    }

    fun validateArrayList() {
        val map = history[0].toMutableMap()
        assertTrue(map == history[0])
        history.asSequence()
            .drop(1)
            .zip(operations.asSequence())
            .forEach { (persList, operation) ->
                operation.apply(map)
                assertEquals(map, persList)
            }
    }
}