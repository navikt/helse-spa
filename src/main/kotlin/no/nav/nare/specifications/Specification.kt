package no.nav.nare.specifications


import no.nav.nare.evaluation.Evaluation


interface Specification<T> {


    fun evaluate(t: T): Evaluation

    fun and(specification: Specification<T>): Specification<T>
    fun or(specification: Specification<T>): Specification<T>

    fun identity(): String
    fun description(): String

    fun ruleDescription(): RuleDescription

    fun medBeskrivelse(beskrivelse: String): Specification<*>
    fun medID(id: String): Specification<*>
}
