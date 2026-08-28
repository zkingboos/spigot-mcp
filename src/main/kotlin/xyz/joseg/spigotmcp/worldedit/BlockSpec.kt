package xyz.joseg.spigotmcp.worldedit

data class BlockSpec(
    val id: String,
    val properties: Map<String, String> = emptyMap()
) {
    val simpleId: String = id.removePrefix(NAMESPACE_PREFIX).lowercase()

    fun property(name: String): String? = properties[name]

    fun withProperties(extra: Map<String, String>): BlockSpec =
        if (extra.isEmpty()) this else copy(properties = properties + extra.normalised())

    override fun toString(): String =
        if (properties.isEmpty()) id
        else properties.entries.joinToString(",", prefix = "$id[", postfix = "]") { "${it.key}=${it.value}" }

    companion object {
        private const val NAMESPACE_PREFIX = "minecraft:"

        fun parse(raw: String): BlockSpec {
            val input = raw.trim()
            require(input.isNotEmpty()) { "Block material must not be empty" }

            val bracket = input.indexOf('[')
            if (bracket < 0) return BlockSpec(input)

            require(input.endsWith(']')) { "Malformed block state: '$raw' (missing closing ']')" }
            val id = input.substring(0, bracket).trim()
            require(id.isNotEmpty()) { "Malformed block state: '$raw' (missing block id)" }

            return BlockSpec(id, parseProperties(input.substring(bracket + 1, input.length - 1), raw))
        }

        private fun parseProperties(body: String, raw: String): Map<String, String> =
            body.split(',')
                .filter { it.isNotBlank() }
                .associate { entry ->
                    val separator = entry.indexOf('=')
                    require(separator > 0) { "Malformed block property '$entry' in '$raw'" }
                    entry.substring(0, separator).trim() to entry.substring(separator + 1).trim()
                }
                .normalised()

        private fun Map<String, String>.normalised(): Map<String, String> =
            mapKeys { it.key.lowercase() }.mapValues { it.value.lowercase() }
    }
}

object BlockProperties {
    const val FACING = "facing"
    const val HALF = "half"

    const val HALF_LOWER = "lower"
    const val HALF_UPPER = "upper"
}
