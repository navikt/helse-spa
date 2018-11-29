package no.nav.nare.evaluation


enum class Result private constructor(private val weight: Int, private val inverse: String) {

    YES(-1, "NO"),
    NO(1, "YES");

    fun and(result: Result): Result {
        return if (this.weight > result.weight) this else result
    }

    fun or(result: Result): Result {
        return if (this.weight < result.weight) this else result
    }

    operator fun not(): Result {
        return Result.valueOf(this.inverse)

    }
}
