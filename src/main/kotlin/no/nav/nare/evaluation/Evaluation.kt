package no.nav.nare.evaluation

interface Evaluation {

    fun result(): Result
    fun reason(): String
    fun ruleDescription(): String
    fun ruleIdentification(): String

}
