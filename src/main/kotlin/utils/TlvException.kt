package utils

/**
 * A custom exception thrown for any errors encountered during TLV parsing or encoding,
 * such as malformed data or unsupported formats.
 *
 * @param message A descriptive error message.
 * @param cause An optional underlying cause for the exception.
 */
class TlvException(message: String, cause: Throwable? = null) : java.lang.RuntimeException(message, cause)