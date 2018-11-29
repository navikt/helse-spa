package no.nav.nare.evaluation

import java.text.MessageFormat

class SingleEvaluation(private val result: Result, private val ruleIdentification: String, private val ruleDescription: String, reason: String, vararg stringformatArguments: Any) : Evaluation {
    private val reason: String

    init {
        this.reason = MessageFormat.format(reason, *stringformatArguments)
    }

    override fun result(): Result {
        return result
    }

    override fun ruleDescription(): String {
        return ruleDescription
    }

    override fun ruleIdentification(): String {
        return ruleIdentification
    }

    override fun reason(): String {
        return reason
    }


}
