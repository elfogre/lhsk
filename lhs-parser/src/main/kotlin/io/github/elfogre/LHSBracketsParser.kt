package io.github.elfogre

/**
 * The LHSBracketsParser object provides a utility method for parsing LHS brackets expressions. These expressions are commonly used for filtering and sorting purposes.
 * Example as URL query param: /var/foo?property_type&#091;eq&#093;=PARKING&deposit_amount&#091;lte&#093;=604.75&sort&#091;0&#093;=auction_value:asc
 */
object LHSBracketsParser {

    /**
     * Takes a list of key-value pairs representing LHS expressions and converts them into a [SearchParams] object.
     *
     * LHS expressions for filtering should follow the format Pair("parameter&#091;operator&#093;", "value").
     * Example: Pair("field3&#091;eq&#093;", "whatever3")
     *
     * LHS expressions for sorting should follow the format Pair("sort&#091;index&#093;", "field:sortType").
     * Example: Pair("sort&#091;1&#093;", "field1:asc")
     *
     * @param searchParams a list of key-value pairs representing the LHS expressions. Usually extracted from a request query params.
     * @return the parsed search parameters as a [SearchParams] object
     * @throws InvalidSyntaxException if the syntax of the query parameters is invalid
     * @throws OperatorNotFoundException if invalid filter operators are provided
     */
    fun parseSearchQueryParams(searchParams: List<Pair<String, String>>): SearchParams {
        val filters = mutableListOf<Filter>()
        val sorts = mutableListOf<Sort>()
        searchParams.forEach { queryParam ->
            val field = queryParam.first.split("[", "]")
            if (field.size < 2) {
                throw InvalidSyntaxException()
            }
            if (field[0] == "sort") {
                val sortData = queryParam.second.split(":")
                sorts.add(Sort(sortData[0], SortType.fromString(sortData.getOrNull(1)), field[1].toInt()))
            } else {
                filters.add(Filter(field[0], Operator.fromString(field[1]), queryParam.second))
            }
        }
        return SearchParams(filters, sorts.sortedBy { it.priority })
    }
    
    /**
     * Converts search parameters to LHS brackets query parameters.
     *
     * Given a `SearchParams` object, this function builds a list of key-value pairs representing the LHS brackets query parameters.
     * The query parameters are constructed based on the filters and sorts defined in the `SearchParams` object.
     *
     * @param searchParams the search parameters object containing filters and sorts
     * @return a list of key-value pairs representing the query parameters
     */
    fun searchParamsToLHSBracketsQueryParams(searchParams: SearchParams): List<Pair<String, String>> {
        return buildList {
            searchParams.filters.forEach { filter ->
                add("${filter.fieldName}[${filter.operator.name.lowercase()}]" to filter.value)
            }
            searchParams.sorts.forEach { sort ->
                add("sort[${sort.priority}]" to "${sort.fieldName}:${sort.sortType.name.lowercase()}")
            }
        }
    }
}


/**
 * The `SearchParams` class represents the search parameters used for filtering and sorting.
 * It contains a list of `Filter` objects and a list of `Sort` objects.
 *
 * @param filters the list of filters to apply
 * @param sorts the list of sorts to apply
 */
data class SearchParams (
    val filters: List<Filter>,
    val sorts: List<Sort>,
)

/**
 * The `Filter` class represents a filter with a field name, operator, and value.
 * Filters are used to determine if a condition is true or false when comparing values.
 * The field name represents the property or column to filter on.
 * The operator represents the comparison operator to use when evaluating the condition.
 * The value represents the value to compare against.
 *
 * Example Usage:
 * val filter = Filter("fieldName", Operator.EQ, "value")
 *
 * @param fieldName the name of the field or property to filter on
 * @param operator the comparison operator to use when evaluating the condition
 * @param value the value to compare against
 */
data class Filter (
    val fieldName: String,
    val operator: Operator,
    val value: String,
)

/**
 * The Operator enum represents various comparison operators that can be used in filters.
 * These operators are used to compare values and determine if a condition is true or false.
 *
 * The available operators are:
 * - eq: Equal to
 * - neq: Not equal to
 * - gt: Greater than
 * - gte: Greater than or equal to
 * - lt: Less than
 * - lte: Less than or equal to
 * - in: In a specified list of values  (Use a comma separated list as value)
 * - notin: Not in a specified list of values (Use a comma separated list as value)
 */
enum class Operator {
    EQ,
    NEQ,
    GT,
    GTE,
    LT,
    LTE,
    IN,
    NOTIN,;
    companion object {
        fun fromString(operator: String?): Operator {
            return when(operator) {
                "eq" -> EQ
                "neq" -> NEQ
                "gt" -> GT
                "gte" -> GTE
                "lt" -> LT
                "lte" -> LTE
                "in" -> IN
                "notin" -> NOTIN
                else -> throw OperatorNotFoundException()
            }
        }
    }
}

class OperatorNotFoundException : Throwable()
class InvalidSyntaxException : Throwable()

/**
 * The `Sort` class represents a sorting parameter used for sorting data.
 * It contains the field name to sort on, the sort type (asc or desc), and the priority of the sort. Priority to apply multiple sorts is ordered in ASC mode.
 *
 * @param fieldName the name of the field to sort on
 * @param sortType the type of sorting (ASC for ascending, DESC for descending)
 * @param priority the priority of the sort, used to determine the order of multiple sorts (lower priority value comes first)
 */
data class Sort (
    val fieldName: String,
    val sortType: SortType,
    val priority: Int,
)

enum class SortType {
    ASC,
    DESC,;
    companion object {
        fun fromString(sortType: String?): SortType {
            return when(sortType) {
                "asc" -> ASC
                "desc" -> DESC
                else -> ASC
            }
        }
    }
}