# LHSK (LHS brackets parser and utilities for Kotlin and Exposed)

[![License](https://img.shields.io/badge/License-GNU%20GPL-blue)](LICENSE)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.elfogre/lhs-parser/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.elfogre/lhs-parser)


## Table of Contents

- [Introduction](#introduction)
- [Features](#features)
- [Installation](#installation)
- [Usage](#usage)
- [Contributing](#contributing)
- [License](#license)
- [Acknowledgments](#acknowledgments)

## Introduction

[LHS brackets](https://christiangiacomi.com/posts/rest-design-principles/) is a flexible and powerful tool to add filters and sorts on REST requests as query params.
Example:

```
/api/search/data?property_type[eq]=PARKING&deposit_amount[lte]=604.75&sort[0]=auction_value:asc    
```

This library provides utilities to parse LHS brackets syntax and also to easily apply them to database queries via [Exposed](https://github.com/JetBrains/Exposed) library.

## Features

- Easy integration with Kotlin projects.
- Comprehensive API for database interactions using Exposed.
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
Check for more examples in [integration tests](lhs-exposed/src/test/kotlin/io/github/elfogre/LHSBracketsExposedExtensionTest.kt).

## Contributing

We welcome contributions! Please add issues or submit a pull request.

## License

This project is licensed under the GNU GPL License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Thanks to the Exposed and Kotlin teams for their invaluable libraries.