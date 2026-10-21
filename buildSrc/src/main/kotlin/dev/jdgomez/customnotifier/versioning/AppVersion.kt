package dev.jdgomez.customnotifier.versioning

/** The app version, parsed from the generated `version.txt` (strict MAJOR.MINOR.PATCH). */
class AppVersion private constructor(
    val name: String,
    val code: Int,
) {
    companion object {
        private const val MAJOR_FACTOR = 1_000_000
        private const val MINOR_FACTOR = 1_000
        private const val PART_LIMIT = 1_000

        // Play's version code limit is 2_100_000_000.
        private const val MAX_MAJOR = 2_099
        private val FORMAT = Regex("""(0|[1-9]\d{0,3})\.(0|[1-9]\d{0,3})\.(0|[1-9]\d{0,3})""")

        /**
         * Version code = MAJOR * 1_000_000 + MINOR * 1_000 + PATCH, so a later version always has a greater code.
         * Fails on anything but a plain semantic version, when MINOR or PATCH reach 1000 (the code would stop
         * growing) or when MAJOR is above 2099 (the code would pass Play's limit).
         */
        fun parse(text: String): AppVersion {
            val name = text.trim()
            val match =
                requireNotNull(FORMAT.matchEntire(name)) {
                    "version.txt must hold MAJOR.MINOR.PATCH, found '$name'."
                }
            val (major, minor, patch) = match.destructured.toList().map { it.toInt() }
            require(major <= MAX_MAJOR && minor < PART_LIMIT && patch < PART_LIMIT) {
                "'$name' is out of range: MAJOR at most $MAX_MAJOR, MINOR and PATCH below $PART_LIMIT."
            }
            return AppVersion(name, major * MAJOR_FACTOR + minor * MINOR_FACTOR + patch)
        }
    }
}
