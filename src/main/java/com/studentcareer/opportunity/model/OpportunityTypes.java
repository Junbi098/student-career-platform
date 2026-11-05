package com.studentcareer.opportunity.model;

public final class OpportunityTypes {
    private OpportunityTypes() { }
    public enum Type { INTERNSHIP, JUNIOR_JOB }
    public enum WorkMode { ON_SITE, HYBRID, REMOTE, UNKNOWN }
    public enum Eligibility { YES, NO, UNKNOWN }
    public enum StudentType { MAURITIAN, INTERNATIONAL }
    public enum Verification { UNVERIFIED, CONFIRMED, REJECTED }
    public enum Status { UNVERIFIED, OPEN, STALE, CLOSED, EXPIRED, REJECTED }
    public enum Origin { MANUAL_ENTRY, HTML_SUBMISSION }
    public enum DeadlinePrecision { DATE, TIME, UNKNOWN }
    public enum ReviewAction { CONFIRM, REJECT, CLOSE, REOPEN, MATERIAL_CHANGE, SUBMITTED }
    public enum Sort { RELEVANCE, NEWEST, CLOSING_SOON }
}
