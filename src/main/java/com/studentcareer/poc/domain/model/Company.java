
package com.studentcareer.poc.domain.model;

public class Company {

    private final String name;
    private final String website;
    private final String industry;
    private final String location;

    public Company(String name, String website, String industry, String location) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Company name is required");
        }

        this.name = name.trim();
        this.website = website;
        this.industry = industry;
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public String getWebsite() {
        return website;
    }

    public String getIndustry() {
        return industry;
    }

    public String getLocation() {
        return location;
    }
}
