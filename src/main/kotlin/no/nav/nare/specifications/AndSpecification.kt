package no.nav.nare.specifications


import no.nav.nare.evaluation.*
import no.nav.nare.evaluation.booleans.AndEvaluation

/**
 * AND specification, used to create a new specifcation that is the AND of two other specifications.
 */
class AndSpecification<T>(private val spec1: Specification<T>, private val spec2: Specification<T>) : AbstractSpecification<T>() {

    override fun evaluate(t: T): Evaluation {
        return AndEvaluation(identity(), description(), spec1.evaluate(t), spec2.evaluate(t))
    }

    override fun identity(): String {
        return if (id.isEmpty()) {
            "(" + spec1.identity() + " AND " + spec2.identity() + ")"
        } else {
            id
        }
    }

    override fun description(): String {
        return if (beskrivelse.isEmpty()) {
            "(" + spec1.description() + " AND " + spec2.description() + ")"
        } else {
            beskrivelse
        }
    }

    override fun ruleDescription(): RuleDescription {
        return RuleDescription(Operator.AND, identity(), description(), spec1.ruleDescription(), spec2.ruleDescription())
    }

}
