package no.nav.helse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class EitherTest {

    private val okVal = Either.Right("OK")
    private val feilVal = Either.Left("Left")

    @Test
    fun `høyreverdi kan mappes til ny verdi`() {
        val newVal = 1234
        assertEquals(Either.Right(newVal), okVal.map { newVal })
    }

    @Test
    fun `venstreverdi kan ikke mappes til ny verdi`() {
        val newVal = 1234
        assertEquals(feilVal, feilVal.map { newVal })
    }

    @Test
    fun `høyreverdi kan flatmappes til ny type`() {
        assertEquals(feilVal, okVal.flatMap { feilVal })
    }

    @Test
    fun `venstreverdi kan ikke flatmappes til ny type`() {
        assertEquals(feilVal, feilVal.flatMap { okVal })
    }

    @Test
    fun `fold skal velge riktig callback når verdi er venstreverdi`() {
        val newVal = "Hello, World"
        assertEquals(newVal, feilVal.fold( { newVal }, { "This should not return" }))
    }

    @Test
    fun `fold skal velge riktig callback når verdi er høyreverdi`() {
        val newVal = "Hello, World"
        assertEquals(newVal, okVal.fold( { "This should not return" }, { newVal }))
    }

    @Test
    fun `orElse skal gi høyreverdi når den er tilstede`() {
        assertEquals(okVal.right, okVal.orElse { "This should not return" })
    }

    @Test
    fun `orElse skal gi alternativ verdi når venstreverdi er tilstede`() {
        val newVal = "Hello, World"
        assertEquals(newVal, feilVal.orElse { newVal })
    }

    @Test
    fun `liste av either med bare høyreverdier skal gi høyreverdi`() {
        val list = listOf(
                Either.Right("One"),
                Either.Right("Two")).sequenceU()

        when (list) {
            is Either.Right -> {
                // ok
            }
            is Either.Left -> fail { "Expected Either.Right" }
        }
    }

    @Test
    fun `liste over either med en venstreverdi skal gi venstreverdi`() {
        val list = listOf(
                Either.Right("One"),
                Either.Right("Two"),
                Either.Left("Shit")).sequenceU()

        when (list) {
            is Either.Left -> {
                // ok
            }
            is Either.Right -> fail { "Expected Either.Left" }
        }
    }

    @Test
    fun `skal gi ny venstreverdi om høyreverdi er null`() {
        val str: String? = null
        val either: Either<String, String?> = Either.Right(str)
        val actual = either.leftIfNull { "Left" }

        when (actual) {
            is Either.Left -> assertEquals("Left", actual.left)
            is Either.Right -> fail { "Expected Either.Left" }
        }
    }
}
