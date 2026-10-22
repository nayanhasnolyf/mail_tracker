package com.applicationtracker.domain;

/**
 * Status lifecycle values for a job application.
 */
public enum ApplicationStatus {
    APPLIED,
    ASSESSMENT,
    INTERVIEW,
    REJECTED,
    OFFER,
    UNKNOWN;

    /**
     * Checks if this status is an actionable application status (i.e. not UNKNOWN).
     */
    public boolean isActionable() {
        return this != UNKNOWN;
    }

    /**
     * Determines whether transitioning from this status to {@code next} is a sensible forward progression.
     * Rules:
     * - Cannot transition to UNKNOWN or null.
     * - Identical status is not a state progression.
     * - REJECTED is terminal and cannot transition backwards.
     * - OFFER cannot revert to earlier pipeline stages (APPLIED, ASSESSMENT, INTERVIEW), but may transition to REJECTED.
     * - APPLIED can advance to ASSESSMENT, INTERVIEW, OFFER, REJECTED.
     * - ASSESSMENT can advance to INTERVIEW, OFFER, REJECTED.
     * - INTERVIEW can advance to OFFER, REJECTED.
     *
     * @param next the candidate next status
     * @return true if transitioning to {@code next} moves the application forward sensibly
     */
    public boolean canTransitionTo(ApplicationStatus next) {
        if (next == null || next == UNKNOWN || this == next) {
            return false;
        }
        return switch (this) {
            case APPLIED -> next == ASSESSMENT || next == INTERVIEW || next == OFFER || next == REJECTED;
            case ASSESSMENT -> next == INTERVIEW || next == OFFER || next == REJECTED;
            case INTERVIEW -> next == OFFER || next == REJECTED;
            case OFFER -> next == REJECTED;
            case REJECTED -> false;
            case UNKNOWN -> next.isActionable();
        };
    }
}
