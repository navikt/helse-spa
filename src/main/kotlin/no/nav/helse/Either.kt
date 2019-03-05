package no.nav.helse

sealed class Either<out A, out R> {

    data class Left<out A>(val left: A) : Either<A, Nothing>()

    data class Right<out B>(val right: B) : Either<Nothing, B>()
}

fun <A, B, C> Either<A, B>.map(ifRight: (B) -> C) =
        flatMap {
            Either.Right(ifRight(it))
        }

fun <A, B, C> Either<A, B>.mapLeft(ifLeft: (A) -> C) =
        fold({
            Either.Left(ifLeft(it))
        }) {
            Either.Right(it)
        }

fun <A, B, C> Either<A, B>.flatMap(ifRight: (B) -> Either<A, C>) =
        when (this) {
            is Either.Right -> ifRight(this.right)
            is Either.Left -> this
        }

fun <A, B, C, D> Either<A, B>.bimap(ifLeft: (A) -> C, ifRight: (B) -> D) =
        fold({
            Either.Left(ifLeft(it))
        }) {
            Either.Right(ifRight(it))
        }

fun <A, B, C> Either<A, B>.fold(ifLeft: (A) -> C, ifRight: (B) -> C) =
        when (this) {
            is Either.Right -> ifRight(this.right)
            is Either.Left -> ifLeft(this.left)
        }

fun <B> Either<*, B>.orElse(ifLeft: () -> B) = fold({ ifLeft() }, { it })

fun <A, B> Either<A, B?>.leftIfNull(ifNull: () -> A) =
        flatMap {
            when (it) {
                null -> Either.Left(ifNull())
                else -> Either.Right(it)
            }
        }

// mimicks scala's sequenceU
fun <A, B> List<Either<A, B>>.sequenceU() =
        fold(Either.Right(emptyList<B>()) as Either<A, List<B>>) { acc, either ->
            either.fold({ left ->
                Either.Left(left)
            }, { right ->
                acc.map { list ->
                    list + right
                }
            })
        }