package com.amie.refactor.analysis;

// Unchecked exception for the CK Tool metrics module.
//
// Used for configuration problems (missing JAR, invalid paths),
// execution failures (non-zero exit code, timeout, missing CSV
// output) and unusable CSV content (missing required columns).
// Every failure carries a message that explains what went wrong,
// so problems are observable instead of being silently ignored.
public class CKToolException extends RuntimeException {

    public CKToolException(String message) {
        super(message);
    }

    public CKToolException(String message, Throwable cause) {
        super(message, cause);
    }
}
