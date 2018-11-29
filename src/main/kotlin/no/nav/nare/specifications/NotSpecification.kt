package no.nav.nare.specifications


import no.nav.nare.evaluation.Evaluation
import no.nav.nare.evaluation.Operator
import no.nav.nare.evaluation.booleans.NotEvaluation

/**
 * NOT decorator, used to create a new specifcation that is the inverse (NOT) of the given spec.
 */
class NotSpecification<T>(private val spec1: Specification<T>) : AbstractSpecification<T>() {

    override fun evaluate(t: T): Evaluation {
        return NotEvaluation(identity(), description(), spec1.evaluate(t))
    }

    override fun identity(): String {
        return if (id.isEmpty()) {
            "(NOT " + spec1.identity() + ")"
        } else {
            id
        }
    }


    override fun description(): String {
        return if (beskrivelse.isEmpty()) {
            "(NOT " + spec1.description() + ")"
        } else {
            beskrivelse
        }

    }

    override fun ruleDescription(): RuleDescription {
        return RuleDescription(Operator.NOT, identity(), description(), spec1.ruleDescription())
    }

    companion object {

        fun ikke(spec1: Specification<*>): NotSpecification<*> {
            return NotSpecification(spec1)
        }
    }
}
