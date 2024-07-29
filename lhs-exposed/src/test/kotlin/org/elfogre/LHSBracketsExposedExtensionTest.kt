package org.elfogre

import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.extensions.testcontainers.JdbcDatabaseContainerExtension
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.testcontainers.containers.MySQLContainer
import java.math.BigDecimal
import javax.sql.DataSource

class LHSBracketsExposedExtensionTest : FeatureSpec({

    val mysql = MySQLContainer("mysql:8")
    val dataSource: DataSource = install(JdbcDatabaseContainerExtension(mysql))

    val testUsers = listOf(
        User(1, "John", UserType.COMMON),
        User(2, "Lucas", UserType.COMMON),
        User(3, "David", UserType.COMMON),
        User(4, "Frank", UserType.RARE),
        User(5, "Julio", UserType.EPIC),
        User(6, "Chad", UserType.LEGENDARY),
    )

    val testTransactions = listOf(
        Transaction(1, 1, BigDecimal(1000), 0),
        Transaction(2, 6, BigDecimal(100000.01), 10),
        Transaction(3, 2, BigDecimal(10), 0),
        Transaction(4, 2, BigDecimal(10), 0),
        Transaction(5, 5, BigDecimal(10000), 5),
    )

    beforeSpec {
        Database.connect(dataSource)
        transaction {
            SchemaUtils.createMissingTablesAndColumns(UserTable, TransactionTable)
            UserTable.batchInsert(testUsers) {
                this[UserTable.id] = it.id
                this[UserTable.name] = it.name
                this[UserTable.type] = it.type
            }
            TransactionTable.batchInsert(testTransactions) {
                this[TransactionTable.id] = it.id
                this[TransactionTable.userId] = it.userId
                this[TransactionTable.transactionAmount] = it.transactionAmount
                this[TransactionTable.priority] = it.priority
            }
        }
    }

    feature("addOrderByFromLHSSort") {
        scenario("single order ASC") {
            val lhsSorts = listOf(Sort("name", SortType.ASC, 1))
            val names = transaction {
                UserTable.selectAll().addOrderByFromLHSSort(lhsSorts).map { it[UserTable.name] }
            }
            names shouldBe listOf("Chad", "David", "Frank", "John", "Julio", "Lucas")
        }
        scenario("single order DESC") {
            val lhsSorts = listOf(Sort("name", SortType.DESC, 1))
            val names = transaction {
                UserTable.selectAll().addOrderByFromLHSSort(lhsSorts).map { it[UserTable.name] }
            }
            names shouldBe listOf("Lucas", "Julio", "John", "Frank", "David", "Chad")
        }
        scenario("multiple order with joins") {
            val lhsSorts = listOf(
                Sort("priority", SortType.DESC, 1),
                Sort("name", SortType.DESC, 2),
            )
            val names = transaction {
                UserTable.join(TransactionTable, JoinType.INNER, UserTable.id, TransactionTable.userId).selectAll().addOrderByFromLHSSort(lhsSorts).map { it[UserTable.name] }
            }
            names shouldBe listOf("Chad", "Julio", "Lucas", "Lucas", "John")
        }
    }

    feature("addWhereExpressionFromLHSFilter") {
        scenario("EQ filter") {
            val lhsFilters = listOf(Filter("name", Operator.EQ, "Chad"))
            val user = transaction {
                val result = UserTable.selectAll().addWhereExpressionFromLHSFilter(lhsFilters).single()
                User.fromResultRow(result)
            }
            user shouldBe User(6, "Chad", UserType.LEGENDARY)
        }
        scenario("NEQ filter") {
            val lhsFilters = listOf(Filter("name", Operator.NEQ, "Chad"))
            val users = transaction {
                UserTable.selectAll().addWhereExpressionFromLHSFilter(lhsFilters).map { User.fromResultRow(it) }
            }
            users shouldBe listOf(
                User(1, "John", UserType.COMMON),
                User(2, "Lucas", UserType.COMMON),
                User(3, "David", UserType.COMMON),
                User(4, "Frank", UserType.RARE),
                User(5, "Julio", UserType.EPIC),
            )
        }
        scenario("IN filter") {
            val lhsFilters = listOf(Filter("type", Operator.IN, "RARE,LEGENDARY"))
            val users = transaction {
                UserTable.selectAll().addWhereExpressionFromLHSFilter(lhsFilters).map { User.fromResultRow(it) }
            }
            users shouldBe listOf(
                User(4, "Frank", UserType.RARE),
                User(6, "Chad", UserType.LEGENDARY),
            )
        }
        scenario("NOT IN filter") {
            val lhsFilters = listOf(Filter("type", Operator.NOTIN, "RARE,LEGENDARY"))
            val users = transaction {
                UserTable.selectAll().addWhereExpressionFromLHSFilter(lhsFilters).map { User.fromResultRow(it) }
            }
            users shouldBe listOf(
                User(1, "John", UserType.COMMON),
                User(2, "Lucas", UserType.COMMON),
                User(3, "David", UserType.COMMON),
                User(5, "Julio", UserType.EPIC),
            )
        }
        scenario("GTE filter") {
            val lhsFilters = listOf(Filter("id", Operator.GTE, "3"))
            val users = transaction {
                UserTable.selectAll().addWhereExpressionFromLHSFilter(lhsFilters).map { User.fromResultRow(it) }
            }
            users shouldBe listOf(
                User(3, "David", UserType.COMMON),
                User(4, "Frank", UserType.RARE),
                User(5, "Julio", UserType.EPIC),
                User(6, "Chad", UserType.LEGENDARY),
            )
        }
        scenario("GT filter") {
            val lhsFilters = listOf(Filter("id", Operator.GT, "3"))
            val users = transaction {
                UserTable.selectAll().addWhereExpressionFromLHSFilter(lhsFilters).map { User.fromResultRow(it) }
            }
            users shouldBe listOf(
                User(4, "Frank", UserType.RARE),
                User(5, "Julio", UserType.EPIC),
                User(6, "Chad", UserType.LEGENDARY),
            )
        }
        scenario("LTE filter") {
            val lhsFilters = listOf(Filter("id", Operator.LTE, "3"))
            val users = transaction {
                UserTable.selectAll().addWhereExpressionFromLHSFilter(lhsFilters).map { User.fromResultRow(it) }
            }
            users shouldBe listOf(
                User(1, "John", UserType.COMMON),
                User(2, "Lucas", UserType.COMMON),
                User(3, "David", UserType.COMMON),
            )
        }
        scenario("LT filter") {
            val lhsFilters = listOf(Filter("id", Operator.LT, "3"))
            val users = transaction {
                UserTable.selectAll().addWhereExpressionFromLHSFilter(lhsFilters).map { User.fromResultRow(it) }
            }
            users shouldBe listOf(
                User(1, "John", UserType.COMMON),
                User(2, "Lucas", UserType.COMMON),
            )
        }
        scenario("multiple filters") {
            val lhsFilters = listOf(
                Filter("transaction_amount", Operator.GTE, "1000"),
                Filter("type", Operator.IN, "COMMON,LEGENDARY"),
            )
            val names = transaction {
                UserTable.join(TransactionTable, JoinType.INNER, UserTable.id, TransactionTable.userId).selectAll().addWhereExpressionFromLHSFilter(lhsFilters).map { it[UserTable.name] }
            }
            names shouldBe listOf("John", "Chad")
        }
    }

})