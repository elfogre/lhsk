# LHSK (LHS brackets parser and utilities for Kotlin and Exposed)

[![License](https://img.shields.io/badge/License-GNU%20GPL-blue)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.elfogre/lhs-parser.svg?label=Maven%20Central&logo=apachemaven)](https://central.sonatype.com/artifact/io.github.elfogre/lhs-parser/)

## Table of Contents

- [Introduction](#introduction)
- [Features](#features)
- [Installation](#installation)
- [Usage](#usage)
- [Supported operators](#supported-operators)
- [Valid SQL types](#valid-sql-types)
- [Contributing](#contributing)
- [License](#license)
- [Acknowledgments](#acknowledgments)

## Introduction

[LHS brackets](https://christiangiacomi.com/posts/rest-design-principles/) is a flexible and powerful tool to add filters and sorts on REST requests as query params.
Example:

```
/api/search/data?property_type[eq]=PARKING&deposit_amount[lte]=604.75&sort[0]=auction_value:asc    
```

This library provides utilities to parse LHS brackets syntax, generate it and also to easily apply them to database queries via [Exposed](https://github.com/JetBrains/Exposed) library.

## Features

- Easy integration with Kotlin projects.
- Comprehensive API for database interactions using [Exposed](https://github.com/JetBrains/Exposed).
- Support for join queries in Exposed.
- Supports Java SDK version 11 and Kotlin API version 1.9.

## Installation

### Gradle

Add the following dependencies to your `build.gradle.kts` file:

```kotlin
dependencies {
    implementation("io.github.elfogre:lhs-parser:1.0.0")
    implementation("io.github.elfogre:lhs-exposed:1.0.0")
}
```

## Usage

### Example 1: Basic Parser Usage (usin Ktor request params)

```kotlin
import io.github.elfogre.LHSBracketsParser

fun main() {
    val searchParams = LHSBracketsParser.parseSearchQueryParams(call.request.queryParameters.flattenEntries())
        ...
}
```

### Example 2: Database Interaction with Exposed

Use exact table column names as property types on LHS.

```kotlin
import io.github.elfogre.LHSBracketsExposedExtension
import org.jetbrains.exposed.sql.transactions.transaction

fun main() {
    ...
    transaction {
        CarTable.
        selectAll().
        addWhereExpressionFromLHSFilter(parsedParams.filters).
        addOrderByFromLHSSort(parsedParams.sorts).
        map { Car.fromResultRow(it) }
    }
}
```
Filter and sort operations accepts a list of fields to exclude them from sorting and filtering.

**Why Use Excluded Fields?**

- **Prevent Performance Issues:** Filtering by non-indexed fields can severely impact database performance.
- **Protect Internal Logic Fields:** Avoid exposing and filtering by fields that are used for internal logic and should not be accessible externally.

Check for more examples in [integration tests](lhs-exposed/src/test/kotlin/io/github/elfogre/LHSBracketsExposedExtensionTest.kt).

### Example 3: LHS brackets syntax generation

```kotlin
import io.github.elfogre.LHSBracketsParser

fun main() {
    val queryParams = LHSBracketsParser.searchParamsToLHSBracketsQueryParams(searchParams)
        ...
}
```

## Supported operators

- `eq`: Equal to
- `neq`: Not equal to
- `gt`: Greater than
- `gte`: Greater than or equal to
- `lt`: Less than
- `lte`: Less than or equal to
- `in`: In a specified list of values  (Use a comma separated list as value)
- `notin`: Not in a specified list of values (Use a comma separated list as value)

## Valid SQL types

Supported exposed column types for automatic casting in `lhs-exposed` are:

- `StringColumnType`
- `LongColumnType`
- `CustomEnumerationColumnType`
- `IntegerColumnType`
- `DecimalColumnType`


## Contributing

We welcome contributions! Please add issues or submit a pull request.

## License

This project is licensed under the GNU GPL License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Thanks to the Exposed and Kotlin teams for their invaluable libraries.