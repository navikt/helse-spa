package no.nav.nare.specifications

import no.nav.nare.evaluation.Evaluation
import no.nav.nare.evaluation.Operator
import no.nav.nare.evaluation.booleans.OrEvaluation

/**
 * OR specification, used to create a new specifcation that is the OR of two other specifications.
 */
class OrSpecification<T>(private val spec1: Specification<T>, private val spec2: Specification<T>) : AbstractSpecification<T>() {

    override fun evaluate(t: T): Evaluation {
        return OrEvaluation(identity(), description(), spec1.evaluate(t), spec2.evaluate(t))
    }


    override fun identity(): String {
        return if (id.isEmpty()) {
            "(" + spec1.identity() + " OR " + spec2.identity() + ")"
        } else {
            id
        }
    }


    override fun description(): String {
        return if (beskrivelse.isEmpty()) {
            "(" + spec1.description() + " OR " + spec2.description() + ")"
        } else {
            beskrivelse
        }
    }

    override fun ruleDescription(): RuleDescription {
        return RuleDescription(Operator.OR, identity(), description(), spec1.ruleDescription(), spec2.ruleDescription())
    }


}
