package org.elfogre

object LHSBracketsParser {

    fun parseSearchQueryParams(searchParams: List<Pair<String, String>>): SearchParams {
        val filters = mutableListOf<Filter>()
        val sorts = mutableListOf<Sort>()
        searchParams.forEach { queryParam ->
            val field = queryParam.first.split("[", "]")
            if (field[0] == "sort") {
                val sortData = queryParam.second.split(":")
                sorts.add(Sort(sortData[0], SortType.fromString(sortData[1]), field[1].toInt()))
            } else {
                filters.add(Filter(field[0], Operator.fromString(field[1]), queryParam.second))
            }
        }
        return SearchParams(filters, sorts.sortedBy { it.priority })
    }
}

data class SearchParams (
    val filters: List<Filter>,
    val sorts: List<Sort>,
)

data class Filter (
    val fieldName: String,
    val operator: Operator,
    val value: String,
)

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