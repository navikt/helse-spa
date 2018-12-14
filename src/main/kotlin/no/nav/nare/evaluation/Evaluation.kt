package no.nav.nare.evaluation

import org.json.JSONPropertyName

interface Evaluation {

    @JSONPropertyName("result")
    fun result(): Result
    @JSONPropertyName("reason")
    fun reason(): String
    @JSONPropertyName("ruleDescription")
    fun ruleDescription(): String
    @JSONPropertyName("ruleIdentification")
    fun ruleIdentification(): String

}
