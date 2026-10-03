package ca.en.solution.common;

/**
 * The one error body every endpoint returns.
 */
public record ApiError(String error, String message) {
}
