package io.github.elfogre

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class LHSBracketsParserTest : FeatureSpec({

    feature("parseSearchQueryParams") {
        scenario("happy path") {
            val searchParams = LHSBracketsParser.parseSearchQueryParams(
                listOf(
                    Pair("sort[1]", "field1:asc"),
                    Pair("sort[0]", "field2:desc"),
                    Pair("field3[eq]", "whatever3"),
                    Pair("field4[neq]", "whatever4"),
                    Pair("field9[gt]", "whatever9"),
                    Pair("field5[gte]", "whatever5"),
                    Pair("field10[lt]", "whatever10"),
                    Pair("field6[lte]", "whatever6"),
                    Pair("field7[in]", "whatever7"),
                    Pair("field8[notin]", "whatever8"),
                )
            )
            searchParams shouldBe SearchParams(
                sorts = listOf(
                    Sort("field2", SortType.DESC, 0),
                    Sort("field1", SortType.ASC, 1),
                ),
                filters = listOf(
                    Filter("field3", Operator.EQ, "whatever3"),
                    Filter("field4", Operator.NEQ, "whatever4"),
                    Filter("field9", Operator.GT, "whatever9"),
                    Filter("field5", Operator.GTE, "whatever5"),
                    Filter("field10", Operator.LT, "whatever10"),
                    Filter("field6", Operator.LTE, "whatever6"),
                    Filter("field7", Operator.IN, "whatever7"),
                    Filter("field8", Operator.NOTIN, "whatever8"),
                ),
            )
        }
        scenario("no operation exception") {
            shouldThrow<OperatorNotFoundException> {
                LHSBracketsParser.parseSearchQueryParams(
                    listOf(
                        Pair("field8[nottin]", "whatever8"),
                    )
                )
            }
        }
        scenario("invalid syntax exception") {
            shouldThrow<InvalidSyntaxException> {
                LHSBracketsParser.parseSearchQueryParams(
                    listOf(
                        Pair("field8", "whatever8"),
                    )
                )
            }
        }
    }
})