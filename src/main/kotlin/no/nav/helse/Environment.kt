package no.nav.helse

data class Environment(
        val username: String? = getEnvVar("SERVICEUSER_USERNAME"),
        val password: String? = getEnvVar("SERVICEUSER_PASSWORD"),
        val bootstrapServersUrl: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS"),
        val schemaRegistryUrl: String = getEnvVar("KAFKA_SCHEMA_REGISTRY_URL"),
        val httpPort: Int? = null,
        val navTruststorePath: String? = getEnvVar("NAV_TRUSTSTORE_PATH"),
        val navTruststorePassword: String? = getEnvVar("NAV_TRUSTSTORE_PASSWORD")
)

private fun getEnvVar(varName: String, defaultValue: String? = null) =
        System.getenv().getOrDefault(varName, defaultValue)

