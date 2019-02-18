package no.nav.helse

data class Environment(
        val username: String = getEnvVar("SERVICEUSER_USERNAME"),
        val password: String = getEnvVar("SERVICEUSER_PASSWORD"),
        val bootstrapServersUrl: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS"),
        val httpPort: Int? = null,
        val navTruststorePath: String? = getEnvVar("NAV_TRUSTSTORE_PATH"),
        val navTruststorePassword: String? = getEnvVar("NAV_TRUSTSTORE_PASSWORD"),
        val sparkelBaseUrl: String = getEnvVar("SPARKEL_BASE_URL", "http://localhost:8099"),
        val stsRestUrl: String = getEnvVar("SECURITY_TOKEN_SERVICE_REST_URL")
)

private fun getEnvVar(varName: String, defaultValue: String? = null) =
        System.getenv(varName) ?: defaultValue ?: throw Exception("mangler verdi for $varName")

