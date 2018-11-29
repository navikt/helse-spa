package no.nav.nare.evaluation.booleans

import no.nav.nare.evaluation.AggregatedEvaluation
import no.nav.nare.evaluation.Evaluation
import no.nav.nare.evaluation.Operator
import no.nav.nare.evaluation.Result

class NotEvaluation(id: String, ruleDescription: String, child: Evaluation) : AggregatedEvaluation(Operator.NOT, id, ruleDescription, child) {

    override fun result(): Result {
        return first().result().not()
    }

    override fun reason(): String {
        return if (result().equals(Result.YES)) {
            "Satisfies the inverse of " + first().ruleIdentification()
        } else {
            "Does not satisfy the inverse of " + first().ruleIdentification()
        }


    }

}
