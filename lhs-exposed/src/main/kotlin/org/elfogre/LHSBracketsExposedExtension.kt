package org.elfogre

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.CustomEnumerationColumnType
import org.jetbrains.exposed.sql.DecimalColumnType
import org.jetbrains.exposed.sql.GreaterEqOp
import org.jetbrains.exposed.sql.GreaterOp
import org.jetbrains.exposed.sql.IColumnType
import org.jetbrains.exposed.sql.IntegerColumnType
import org.jetbrains.exposed.sql.LessEqOp
import org.jetbrains.exposed.sql.LessOp
import org.jetbrains.exposed.sql.LongColumnType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.wrap
import org.jetbrains.exposed.sql.StringColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.andWhere

fun Query.addOrderByFromLHSSort(sorts: List<Sort>): Query {
    val exposedSorts = sorts.sortedBy { it.priority }.map { sort ->
        val column = searchColumn(sort.fieldName, this.targets)
        when (sort.sortType) {
            SortType.ASC -> Pair(column, SortOrder.ASC)
            SortType.DESC -> Pair(column, SortOrder.DESC)
        }
    }
    return this.orderBy(*exposedSorts.toTypedArray())
}

fun Query.addWhereExpressionFromLHSFilter(filters: List<Filter>): Query {
    filters.forEach { filter ->
        val column = searchColumn(filter.fieldName, this.targets)
        if (filter.operator in listOf(Operator.IN, Operator.NOTIN)) {
            this.andWhere { column.addFilter(filter.operator, generateIterableValue(filter.value, column.columnType)) }
        } else {
            this.andWhere { column.addFilter(filter.operator, generateValue(filter.value, column.columnType)) }
        }
    }
    return this
}

private fun searchColumn(fieldName: String, targets: List<Table>): Column<out Any?> {
    targets.forEach { table ->
        val column = table.columns.firstOrNull { it.name == fieldName }
        if (column != null) { return column }
    }
    throw UnsupportedOperationException()
}

private fun generateValue(value: String, columnType: IColumnType<out Any?>): Any {
    return when (columnType) {
        is StringColumnType -> value
        is LongColumnType -> value.toLong()
        is CustomEnumerationColumnType -> columnType.fromDb(value)!!
        is IntegerColumnType -> value.toInt()
        is DecimalColumnType -> value.toBigDecimal()
        else -> throw UnsupportedOperationException()
    }
}

private fun generateIterableValue(value: String, columnType: IColumnType<out Any?>): List<Any> {
    return value.split(",").map { generateValue(it, columnType) }
}

private fun <T> Column<T>.addFilter(operator: Operator, value: Any): Op<Boolean> {
    return when (operator) {
        Operator.EQ -> this.eq(value as T)
        Operator.NEQ -> this.neq(value as T)
        Operator.GT -> this.greater(value as T)
        Operator.GTE -> this.greaterEq(value as T)
        Operator.LT -> this.less(value as T)
        Operator.LTE -> this.lessEq(value as T)
        Operator.IN -> this.inList(value as Iterable<T>)
        Operator.NOTIN -> this.notInList(value as Iterable<T>)
    }
}

private fun <T> Column<T>.greater(t: T): Op<Boolean> {
    return GreaterOp(this, wrap(t))
}

private fun <T> Column<T>.less(t: T): Op<Boolean> {
    return LessOp(this, wrap(t))
}

private fun <T> Column<T>.greaterEq(t: T): Op<Boolean> {
    return GreaterEqOp(this, wrap(t))
}

private fun <T> Column<T>.lessEq(t: T): Op<Boolean> {
    return LessEqOp(this, wrap(t))
}