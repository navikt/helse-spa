package no.nav.nare.evaluation

import org.json.JSONPropertyName
import java.util.Arrays


abstract class AggregatedEvaluation protected constructor(private val operator: Operator, private val ruleIdentification: String, private val ruleDescription: String, vararg children: Evaluation) : Evaluation {
    private val result: Result

    private val reason: String

    private val children: List<Evaluation>

    @JSONPropertyName("children")
    fun getChildren(): List<Evaluation> { return children }

    init {
        this.children = Arrays.asList(*children)
        this.result = result()
        this.reason = reason()
    }

    protected fun first(): Evaluation {
        return children[0]
    }

    protected fun second(): Evaluation {
        return children[1]
    }


    override fun ruleDescription(): String {
        return ruleDescription
    }

    override fun ruleIdentification(): String {
        return ruleIdentification
    }

}
