package no.nav.helse.vilkaar.regler

import no.nav.helse.Soknad
import no.nav.nare.evaluation.Evaluation
import no.nav.nare.evaluation.Result
import no.nav.nare.evaluation.SingleEvaluation
import no.nav.nare.specifications.AbstractSpecification

class MottarDagpenger : AbstractSpecification<Soknad>() {
    override fun evaluate(t: Soknad): Evaluation {
        return SingleEvaluation(Result.NO, "Mottar dagpenger", "ruleDescription", "Not implemented")
    }
}