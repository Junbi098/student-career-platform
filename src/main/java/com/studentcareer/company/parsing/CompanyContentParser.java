package com.studentcareer.company.parsing;

import com.studentcareer.company.domain.Company;
import java.util.Optional;

public interface CompanyContentParser {
    Optional<Company> parse(String content);
}
