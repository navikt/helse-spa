package no.nav.nare.evaluation.booleans

import no.nav.nare.evaluation.AggregatedEvaluation
import no.nav.nare.evaluation.Evaluation
import no.nav.nare.evaluation.Operator
import no.nav.nare.evaluation.Result

class OrEvaluation(id: String, ruleDescription: String, vararg children: Evaluation) : AggregatedEvaluation(Operator.OR, id, ruleDescription, *children) {

    override fun result(): Result {
        return first().result().or(second().result())
    }

    override fun reason(): String {
        return if (result().equals(Result.YES)) {
            "Satisfies " + ruleOrIdentification()
        } else {
            "Does not satisfy either " + first().ruleIdentification() + " nor " + second().ruleIdentification()
        }

    }


    private fun ruleOrIdentification(): String {
        val firstID = if (first().result().equals(Result.YES)) first().ruleIdentification() else ""
        val secondID = if (second().result().equals(Result.YES)) second().ruleIdentification() else ""
        if (firstID.isEmpty()) return secondID
        return if (secondID.isEmpty()) firstID else "$firstID OG $secondID"
    }


}
