package no.nav.helse

data class Environment(
        val username: String = getEnvVar("SERVICEUSER_USERNAME"),
        val password: String = getEnvVar("SERVICEUSER_PASSWORD"),
        val kafkaUsername: String? = getEnvVarOptional("SERVICEUSER_USERNAME"),
        val kafkaPassword: String? = getEnvVarOptional("SERVICEUSER_PASSWORD"),
        val bootstrapServersUrl: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS"),
        val httpPort: Int? = null,
        val navTruststorePath: String? = getEnvVarOptional("NAV_TRUSTSTORE_PATH"),
        val navTruststorePassword: String? = getEnvVarOptional("NAV_TRUSTSTORE_PASSWORD"),
        val sparkelBaseUrl: String = getEnvVar("SPARKEL_BASE_URL", "http://sparkel"),
        val stsRestUrl: String = getEnvVar("SECURITY_TOKEN_SERVICE_REST_URL"),
        val plainTextKafka: String? = getEnvVarOptional("PLAIN_TEXT_KAFKA"),

        val sensuHostname: String = getEnvVar("SENSU_HOSTNAME", "probe.nais"),
        val sensuPort: Int = getEnvVar("SENSU_PORT", "3030").toInt()
)

private fun getEnvVar(varName: String, defaultValue: String? = null) =
        getEnvVarOptional(varName, defaultValue) ?: throw Exception("mangler verdi for $varName")

private fun getEnvVarOptional(varName: String, defaultValue: String? = null) =
        System.getenv(varName) ?: defaultValue


