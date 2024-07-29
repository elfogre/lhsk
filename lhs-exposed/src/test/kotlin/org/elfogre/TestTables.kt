package org.elfogre

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import java.math.BigDecimal

object UserTable: Table("user") {
    val id: Column<Long> = long("id")
    val name: Column<String> = varchar("name", length = 255)
    val type = customEnumeration("type", sql = "ENUM ('COMMON','RARE','EPIC','LEGENDARY') NOT NULL", fromDb = { value -> UserType.valueOf(value as String) }, toDb = { it.name })

    override val primaryKey = PrimaryKey(id)
}

object TransactionTable: Table("transaction") {
    val id: Column<Long> = long("id")
    val userId: Column<Long> = long("user_id")
    val transactionAmount: Column<BigDecimal> = decimal("transaction_amount", 10, 2)
    val priority: Column<Int> = integer("priority")

    override val primaryKey = PrimaryKey(id)
}

enum class UserType {
    COMMON,
    RARE,
    EPIC,
    LEGENDARY,
}

data class User(val id: Long, val name: String, val type: UserType) {
    companion object {
        fun fromResultRow(result: ResultRow): User {
            return User(
                result[UserTable.id],
                result[UserTable.name],
                result[UserTable.type],
            )
        }
    }
}

data class Transaction(val id: Long, val userId: Long, val transactionAmount: BigDecimal, val priority: Int)