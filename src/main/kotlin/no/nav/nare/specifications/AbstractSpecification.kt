package no.nav.nare.specifications


import com.google.gson.GsonBuilder
import no.nav.nare.evaluation.Evaluation
import no.nav.nare.evaluation.Result
import no.nav.nare.evaluation.SingleEvaluation

abstract class AbstractSpecification<T> protected constructor() : Specification<T> {
    protected var beskrivelse = ""
    protected var id = ""

    override fun and(specification: Specification<T>): Specification<T> {
        return AndSpecification(this, specification)
    }

    override fun or(specification: Specification<T>): Specification<T> {
        return OrSpecification(this, specification)
    }


    fun ja(reason: String, vararg stringformatArguments: Any): Evaluation {
        return SingleEvaluation(Result.YES, identity(), description(), reason, stringformatArguments)
    }

    fun nei(reason: String, vararg stringformatArguments: Any): Evaluation {
        return SingleEvaluation(Result.NO, identity(), description(), reason, stringformatArguments)
    }


    override fun ruleDescription(): RuleDescription {
        return RuleDescription(identity(), description())
    }

    override fun identity(): String {
        return if (id.isEmpty()) {
            Integer.toString(this.hashCode())
        } else {
            id
        }
    }

    override fun description(): String {
        return beskrivelse
    }

    override fun medBeskrivelse(beskrivelse: String): Specification<*> {
        this.beskrivelse = beskrivelse
        return this
    }

    override fun medID(id: String): Specification<*> {
        this.id = id
        return this
    }

    override fun toString(): String {
        println(this.description())
        return GsonBuilder().setPrettyPrinting().create().toJson(this)
    }
}
