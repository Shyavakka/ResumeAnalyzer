package com.navadhiti.resumeanalyzer.service;

/** Thrown for any expected, user-facing failure (bad file, AI call failure, etc). */
public class ResumeProcessingException extends RuntimeException {
    public ResumeProcessingException(String message) {
        super(message);
    }

    public ResumeProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
