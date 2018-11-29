package no.nav.nare.evaluation.booleans

import no.nav.nare.evaluation.AggregatedEvaluation
import no.nav.nare.evaluation.Evaluation
import no.nav.nare.evaluation.Operator
import no.nav.nare.evaluation.Result

class AndEvaluation(id: String, ruleDescription: String, vararg children: Evaluation) : AggregatedEvaluation(Operator.AND, id, ruleDescription, *children) {

    override fun result(): Result {
        return first().result().and(second().result())
    }

    override fun reason(): String {
        return if (result().equals(Result.YES)) {
            "Satisfies both " + first().ruleIdentification() + " and " + second().ruleIdentification()
        } else {
            "Does not satisfy both " + first().ruleIdentification() + " and " + second().ruleIdentification()
        }


    }


}
