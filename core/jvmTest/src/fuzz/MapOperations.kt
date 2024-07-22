/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.putAll
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.reflect.full.isSubclassOf

data class Put(val key: Int, val value: Int) : MapOperation, EmptyOperation {
    override fun PersistentMap<Int, Int>.applyInternal(): PersistentMap<Int, Int> = put(key, value)

    override fun MutableMap<Int, Int>.applyInternal() {
        put(key, value)
    }

    override fun validate(preMap: Map<Int, Int>, postMap: Map<Int, Int>) {
        assertTrue(postMap[key] == value)
    }

    override fun reverse(
        preMap: PersistentMap<Int, Int>,
        postMap: PersistentMap<Int, Int>
    ): PersistentMap<Int, Int> {
        if (preMap.containsKey(key)) return postMap.put(key, preMap[key]!!)
        return postMap.remove(key)
    }
}

data class Remove(val key: Int) : MapOperation {
    override fun PersistentMap<Int, Int>.applyInternal(): PersistentMap<Int, Int> = remove(key)

    override fun MutableMap<Int, Int>.applyInternal() {
        remove(key)
    }

    override fun validate(preMap: Map<Int, Int>, postMap: Map<Int, Int>) {
        assertTrue(!postMap.containsKey(key))
        assertTrue(postMap[key] == null)
    }

    override fun reverse(
        preMap: PersistentMap<Int, Int>,
        postMap: PersistentMap<Int, Int>
    ): PersistentMap<Int, Int> {
        if (preMap.containsKey(key)) return postMap.put(key, preMap[key]!!)
        return postMap
    }
}

data class PutAll(val keyValues: List<Pair<Int, Int>>) : MapOperation, EmptyOperation {
    override fun PersistentMap<Int, Int>.applyInternal(): PersistentMap<Int, Int> {
        return putAll(keyValues)
    }

    override fun MutableMap<Int, Int>.applyInternal() {
        putAll(keyValues)
    }

    override fun validate(preMap: Map<Int, Int>, postMap: Map<Int, Int>) {
        assertTrue(keyValues.associate { it }.all { (key, value) -> postMap[key] == value })
    }

    override fun reverse(
        preMap: PersistentMap<Int, Int>,
        postMap: PersistentMap<Int, Int>
    ): PersistentMap<Int, Int> {
        // TODO: can be optimized
        var result = postMap
        for ((key, _) in keyValues) {
            result = if (preMap.contains(key)) {
                result.put(key, preMap[key]!!)
            } else {
                result.remove(key)
            }
        }
        return result
    }
}

private val MAP_OPERATIONS = MapOperation::class.sealedSubclasses

private val EMPTY_MAP_OPERATIONS = MAP_OPERATIONS.filter { it.isSubclassOf(EmptyOperation::class) }

fun FuzzedDataProvider.consumeMapOperation(map: Map<Int, *>): MapOperation {
    val operations = if (map.isEmpty()) EMPTY_MAP_OPERATIONS else MAP_OPERATIONS

    return when (pickValue(operations)) {
        Put::class -> Put(consumeInt(), consumeInt())
        Remove::class -> Remove(consumeInt())
        Clear::class -> Clear
        PutAll::class -> consumePutAll()
        else -> TODO()
    }
}

fun FuzzedDataProvider.consumePutAll(): MapOperation {
    val keyValues = List(consumeInt(0, 100)) { consumeInt() to consumeInt() }
    return PutAll(keyValues)
}
