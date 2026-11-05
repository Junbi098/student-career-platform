package com.studentcareer.opportunity.service;

public class OpportunityNotFoundException extends RuntimeException {
    public OpportunityNotFoundException() { super("Opportunity not found"); }
}
