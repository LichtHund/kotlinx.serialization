/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("ReplaceArrayOfWithLiteral") // https://youtrack.jetbrains.com/issue/KT-22578

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.test.*
import kotlin.test.*

class JsonAlternativeNamesTest : JsonTestBase() {

    @Serializable
    data class WithNames(@JsonNames("foo", "_foo") val data: String)

    @Serializable
    enum class AlternateEnumNames {
        @JsonNames("someValue", "some_value")
        VALUE_A,
        VALUE_B
    }

    @Serializable
    data class WithEnumNames(
        val enumList: List<AlternateEnumNames>,
        val checkCoercion: AlternateEnumNames = AlternateEnumNames.VALUE_B
    )

    @Serializable
    data class CollisionWithAlternate(
        @JsonNames("_foo") val data: String,
        @JsonNames("_foo") val foo: String
    )

    private val inputString1 = """{"foo":"foo"}"""
    private val inputString2 = """{"_foo":"foo"}"""
    private val json = Json { useAlternativeNames = true }

    @Test
    fun testEnumSupportsAlternativeNames() = noLegacyJs {
        val input = """{"enumList":["VALUE_A", "someValue", "some_value", "VALUE_B"], "checkCoercion":"someValue"}"""
        val expected = WithEnumNames(
            listOf(
                AlternateEnumNames.VALUE_A,
                AlternateEnumNames.VALUE_A,
                AlternateEnumNames.VALUE_A,
                AlternateEnumNames.VALUE_B
            ), AlternateEnumNames.VALUE_A
        )
        for (coercing in listOf(true, false)) {
            val json = Json { coerceInputValues = coercing }
            parametrizedTest { streaming ->
                assertEquals(
                    expected,
                    json.decodeFromString(input, streaming),
                    "Failed test with coercing=$coercing and streaming=$streaming"
                )
            }
        }
    }

    @Test
    fun testParsesAllAlternativeNames() = noLegacyJs {
        for (input in listOf(inputString1, inputString2)) {
            parametrizedTest { streaming ->
                val data = json.decodeFromString(WithNames.serializer(), input, useStreaming = streaming)
                assertEquals("foo", data.data, "Failed to parse input '$input' with streaming=$streaming")
            }
        }
    }

    @Test
    fun testThrowsAnErrorOnDuplicateNames2() = noLegacyJs {
        val serializer = CollisionWithAlternate.serializer()
        parametrizedTest { streaming ->
            assertFailsWithMessage<SerializationException>(
                """The suggested name '_foo' for property foo is already one of the names for property data""",
                "Class ${serializer.descriptor.serialName} did not fail with streaming=$streaming"
            ) {
                json.decodeFromString(
                    serializer, inputString2,
                    useStreaming = streaming
                )
            }
        }
    }
}
