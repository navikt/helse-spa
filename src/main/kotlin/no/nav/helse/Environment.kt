package no.nav.helse

data class Environment(
        val username: String? = getEnvVar("SERVICEUSER_USERNAME", "foo"),
        val password: String? = getEnvVar("SERVICEUSER_PASSWORD", "bar"),
        val bootstrapServersUrl: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
        val schemaRegistryUrl: String = getEnvVar("KAFKA_SCHEMA_REGISTRY_URL", "localhost:8081"),
        val httpPort: Int? = null,
        val navTruststorePath: String? = getEnvVar("NAV_TRUSTSTORE_PATH"),
        val navTruststorePassword: String? = getEnvVar("NAV_TRUSTSTORE_PASSWORD")
)

private fun getEnvVar(varName: String, defaultValue: String? = null) =
        System.getenv().getOrDefault(varName, defaultValue)

