package io.github.elfogre

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

/**
 * Adds an order by clause to the query based on the provided list of sorts.
 * The sorts are sorted by their priority and mapped to exposed sorts that consist of column and sort order.
 * The exposed sorts are then used to create the order by clause in the query.
 * The method returns the modified query object with the order by clause applied.
 *
 * @param sorts the list of sorts to apply as the order by clause
 * @param excludeColumns columns to exclude in generated sorts
 * @param exceptionOnExcludedColumns default false. If true throw exception if excluded column is requested on sorts. If false sorts on excluded columns are silently discarded
 * @return the modified query object with the order by clause applied
 */
fun Query.addOrderByFromLHSSort(
    sorts: List<Sort>,
    excludeColumns: List<Column<*>> = emptyList(),
    exceptionOnExcludedColumns: Boolean = false,
): Query {
    val exposedSorts = sorts.sortedBy { it.priority }.mapNotNull { sort ->
        val column = searchColumn(sort.fieldName, this.targets, excludeColumns, exceptionOnExcludedColumns)
        if (column == null) {
            null
        } else {
            when (sort.sortType) {
                SortType.ASC -> Pair(column, SortOrder.ASC)
                SortType.DESC -> Pair(column, SortOrder.DESC)
            }
        }
    }
    return this.orderBy(*exposedSorts.toTypedArray())
}

/**
 * Adds where expressions to the Query based on the provided list of filters.
 * Each filter consists of a field name, an operator, and a value.
 * The method applies the filter conditions to the corresponding columns in the Query's target tables.
 * If the operator is IN or NOTIN, the filter value is treated as a comma-separated list and the condition is applied to the column as a list of values.
 * If the operator is any other comparison operator, the condition is applied to the column as a single value.
 * The method returns the modified Query object.
 *
 * @param filters the list of filters to apply as where expressions
 * @param excludeColumns columns to exclude in generated filters
 * @param exceptionOnExcludedColumns default false. If true throw exception if excluded column is requested on filters. If false filters on excluded columns are silently discarded
 * @return the modified Query object
 */
fun Query.addWhereExpressionFromLHSFilter(
    filters: List<Filter>,
    excludeColumns: List<Column<*>> = emptyList(),
    exceptionOnExcludedColumns: Boolean = false,
): Query {
    filters.forEach { filter ->
        val column = searchColumn(filter.fieldName, this.targets, excludeColumns, exceptionOnExcludedColumns)
        if (column != null) {
            if (filter.operator in listOf(Operator.IN, Operator.NOTIN)) {
                this.andWhere {
                    column.addFilter(
                        filter.operator,
                        generateIterableValue(filter.value, column.columnType)
                    )
                }
            } else {
                this.andWhere { column.addFilter(filter.operator, generateValue(filter.value, column.columnType)) }
            }
        }
    }
    return this
}

private fun searchColumn(
    fieldName: String,
    targets: List<Table>,
    excludeColumns: List<Column<*>>,
    exceptionOnExcludedColumns: Boolean,
): Column<out Any?>? {
    targets.forEach { table ->
        val column = table.columns.firstOrNull { it.name == fieldName }
        if (column != null) {
            if (column in excludeColumns) {
                if (exceptionOnExcludedColumns) {
                    throw ExcludedColumnException("Column $fieldName is excluded")
                } else {
                    return null
                }
            }
            return column
        }
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

class ExcludedColumnException(message: String) : Exception(message)
