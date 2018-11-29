package no.nav.nare.specifications

import com.google.gson.GsonBuilder
import no.nav.nare.evaluation.Operator

import java.util.Arrays

class RuleDescription {

    private var ruleIdentifcation: String? = null
    private var ruleDescription: String? = null
    private val operator: Operator
    private val children: List<RuleDescription>


    constructor(operator: Operator, ruleIdentifcation: String, ruleDescription: String, vararg children: RuleDescription) {
        this.operator = operator
        this.children = Arrays.asList(*children)
        this.ruleIdentifcation = ruleIdentifcation
        this.ruleDescription = ruleDescription

    }

    constructor(ruleIdentifcation: String, beskrivelse: String) {
        this.operator = Operator.NONE
        this.children = emptyList()
        this.ruleDescription = beskrivelse
        this.ruleIdentifcation = ruleIdentifcation
    }


    override fun toString(): String {
        return GsonBuilder().setPrettyPrinting().create().toJson(this)
    }
}
