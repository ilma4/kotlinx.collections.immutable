/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */


package tests.fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor

//@JvmInline
//value class Index(val value: Int)

data class Add(val element: Int) : ListOperation, EmptyOperation {
    override fun MutableList<Int>.applyInternal() {
        this.add(element)
    }

    override fun PersistentList<Int>.applyInternal(): PersistentList<Int> = this.add(element)

    override fun validate(preList: List<Int>, postList: List<Int>) {
    }

    override val reverseOperation get() = RemoveLast
}

data class AddAt(val index: Int, val element: Int) : ListOperation, EmptyOperation {
    override fun MutableList<Int>.applyInternal() {
        this.add(index, element)
    }

    override fun PersistentList<Int>.applyInternal(): PersistentList<Int> =
        this.add(index, element)

    override fun validate(preList: List<Int>, postList: List<Int>) {
        require(postList.size == preList.size + 1)
        require(postList[index] == element)
    }

    override val reverseOperation: ListOperation
        get() = RemoveAt(index)
}

data class AddAll(val elements: Collection<Int>) : ListOperation, EmptyOperation {
    override fun MutableList<Int>.applyInternal() {
        addAll(elements)
    }


    override fun PersistentList<Int>.applyInternal(): PersistentList<Int> =
        this.addAll(elements.toList())


    override fun validate(preList: List<Int>, postList: List<Int>) {
        require(postList.size == preList.size + elements.size)
        require(postList.subList(preList.size, postList.size) == elements.toList())
    }
}

data class AddAllAt(val index: Int, val elements: Collection<Int>) : ListOperation, EmptyOperation {
    override fun MutableList<Int>.applyInternal() {
        addAll(index, elements)
    }

    override fun PersistentList<Int>.applyInternal(): PersistentList<Int> =
        this.addAll(index, elements.toList())

    override fun validate(preList: List<Int>, postList: List<Int>) {
        require(postList.size == preList.size + elements.size)
    }

}

data class RemoveAt(val index: Int) : ListOperation {
    override fun MutableList<Int>.applyInternal() {
        removeAt(index)
    }


    override fun PersistentList<Int>.applyInternal(): PersistentList<Int> =
        this.removeAt(index)


    override fun validate(preList: List<Int>, postList: List<Int>) {
        require(postList.size + 1 == preList.size)
    }
}

data object Clear : ListOperation, EmptyOperation, MapOperation {
    override fun MutableList<Int>.applyInternal() {
        clear()
    }

    override fun PersistentList<Int>.applyInternal(): PersistentList<Int> {
        return persistentListOf()
    }

    override fun validate(preList: List<Int>, postList: List<Int>) {
        require(postList.isEmpty())
    }

    override fun PersistentMap<Int, Int>.applyInternal(): PersistentMap<Int, Int> =
        persistentMapOf()

    override fun MutableMap<Int, Int>.applyInternal() {
        this.clear()
    }

    override fun validate(preMap: Map<Int, Int>, postMap: Map<Int, Int>) {
        assertTrue(postMap.isEmpty())
        assertTrue(postMap.keys.isEmpty())
    }
}

data class Set(val index: Int, val element: Int) : ListOperation {
    override fun MutableList<Int>.applyInternal() {
        this[index] = element
    }

    override fun PersistentList<Int>.applyInternal(): PersistentList<Int> {
        return this.set(index, element)
    }

    override fun validate(preList: List<Int>, postList: List<Int>) {
        require(postList[index] == element)
    }
}

data object RemoveLast : ListOperation {
    override fun MutableList<Int>.applyInternal() {
        this.removeLast()
    }

    override fun PersistentList<Int>.applyInternal(): PersistentList<Int> {
        return this.removeAt(this.lastIndex)
    }

    override fun validate(preList: List<Int>, postList: List<Int>) {
        require(preList.subList(0, preList.lastIndex) == postList)
    }
}


private val LIST_OPERATIONS = ListOperation::class.sealedSubclasses
private val EMPTY_LIST_OPERATIONS =
    LIST_OPERATIONS.filter { it.isSubclassOf(EmptyOperation::class) }

fun FuzzedDataProvider.consumeIntList(): List<Int> {
    val size = consumeInt(0, 10)
    return List(size) { consumeInt() }
}


fun FuzzedDataProvider.consumeRemoveIndex(list: List<*>): Int = consumeInt(0, list.lastIndex)

fun FuzzedDataProvider.consumeAddIndex(list: List<*>): Int = consumeInt(0, list.size)

fun FuzzedDataProvider.consumeListOperation(list: List<Int>): ListOperation {
    val operations = if (list.isEmpty()) EMPTY_LIST_OPERATIONS else LIST_OPERATIONS

    return when (pickValue(operations)) {
        Add::class -> consumeAdd()
        AddAt::class -> consumeAddAt(list)
        AddAll::class -> consumeAddAll()
        AddAllAt::class -> consumeAddAllAt(list)
        RemoveAt::class -> consumeRemoveAt(list)
        Clear::class -> Clear
        Set::class -> consumeSet(list)
        RemoveLast::class -> RemoveLast
        else -> TODO()
    }
}

fun FuzzedDataProvider.consumeOperationRefl(
    klass: KClass<ListOperation>,
//    list: List<*>
): ListOperation {
    val constructor = klass.primaryConstructor!!
    val args = constructor.parameters.map {
        when (it.type) {
            Int::class -> consumeInt()
            Collection::class -> consumeIntList()
            else -> TODO()
        }
    }.toTypedArray()
    return constructor.call(args)
}

private fun FuzzedDataProvider.consumeSet(list: List<Int>) =
    Set(consumeRemoveIndex(list), consumeInt())

private fun FuzzedDataProvider.consumeRemoveAt(list: List<Int>) =
    RemoveAt(consumeRemoveIndex(list))

private fun FuzzedDataProvider.consumeAddAllAt(list: List<Int>) =
    AddAllAt(consumeAddIndex(list), consumeIntList())

private fun FuzzedDataProvider.consumeAddAll() = AddAll(consumeIntList())

private fun FuzzedDataProvider.consumeAddAt(list: List<Int>) =
    AddAt(consumeAddIndex(list), consumeInt())

private fun FuzzedDataProvider.consumeAdd() = Add(consumeInt())