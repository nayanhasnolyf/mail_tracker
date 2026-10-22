package com.applicationtracker.service;

import com.applicationtracker.domain.ApplicationStatus;
import com.applicationtracker.dto.ClassificationResult;
import com.applicationtracker.dto.EmailMessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rule-based implementation of {@link EmailClassifier}.
 * Uses regex and keyword rules to classify emails into application statuses:
 * APPLIED, ASSESSMENT, INTERVIEW, REJECTED, OFFER, or UNKNOWN.
 * Also extracts company name from sender display name/domain/subject and role from subject/body.
 */
@Service
public class ClassifierService implements EmailClassifier {

    private static final Logger log = LoggerFactory.getLogger(ClassifierService.class);

    // ==========================================
    // STATUS CLASSIFICATION PATTERNS
    // ==========================================

    private static final Pattern OFFER_PATTERN = Pattern.compile(
        "(?i)(?:pleased to offer you|delighted to offer you|thrilled to offer you|happy to offer you|" +
        "offer of employment|formal offer|official offer|extend an offer to you|extending an offer|" +
        "congratulations on your offer|offer letter attached|attached (?:is )?your offer letter|" +
        "we would like to offer you the position|job offer for|formal job offer|offer details|formal employment offer)"
    );

    // Negative guard for offer (e.g. "unable to offer", "cannot offer", "not able to offer")
    private static final Pattern OFFER_NEGATIVE_GUARD = Pattern.compile(
        "(?i)(?:unable to offer|cannot offer|not able to offer|not in a position to offer)"
    );

    private static final Pattern REJECTED_PATTERN = Pattern.compile(
        "(?i)(?:unfortunately(?:,)? (?:we |after )?(?:will not|are unable|have decided|cannot)|" +
        "not (?:be )?moving forward|won't be moving forward|decided to (?:pursue|move forward with) other candidates|" +
        "pursuing other candidates|regret to inform you|impressed with your (?:background|qualifications|experience)(?:,)? but|" +
        "decided not to (?:move|proceed|advance)|unable to offer you (?:an interview|a position|the position|employment)|" +
        "position has been (?:closed|filled)|not selected for|we have chosen to move forward with another|" +
        "will not be proceeding with your application|other candidates whose (?:skills|experience|qualifications)|" +
        "after careful consideration, we have decided|decided not to proceed with your candidacy|" +
        "we've decided to move forward with someone else|not moving forward with your candidacy)"
    );

    private static final Pattern INTERVIEW_PATTERN = Pattern.compile(
        "(?i)(?:invitation to interview|invite you to interview|invited to interview|" +
        "schedule (?:an|your|a) interview|schedule (?:a|a brief|a 30-minute|a 45-minute|a 15-minute) call|" +
        "schedule a time to (?:speak|chat|talk|meet)|schedule time with|" +
        "phone screen|technical interview|onsite interview|virtual onsite|" +
        "next round of interviews|first round interview|round of interviews|" +
        "interview confirmation|interview details|interview scheduled|" +
        "chat with (?:the hiring manager|our team|the team|the recruiter|our recruiter)|" +
        "would like to invite you to a (?:phone|video|technical) interview|" +
        "invitation to meet with|selected for an interview|interview availability)"
    );

    private static final Pattern ASSESSMENT_PATTERN = Pattern.compile(
        "(?i)(?:online assessment|coding (?:challenge|assessment|test)|technical assessment|" +
        "take-home (?:assignment|test|challenge|project)|hackerrank|codesignal|codility|" +
        "karat (?:technical )?interview|karat assessment|byteboard|" +
        "complete (?:the|your) assessment|timed (?:test|assessment)|skills assessment|" +
        "complete the online coding|assessment invitation|take-home challenge)"
    );

    private static final Pattern APPLIED_PATTERN = Pattern.compile(
        "(?i)(?:thank you for applying|thanks for applying|thank you for your application|" +
        "application (?:received|confirmation|submitted|has been submitted)|" +
        "received your application|we(?:'ve| have) received your application|" +
        "successfully submitted your application|confirming receipt of your application|" +
        "submission received|your application to .*? has been received|" +
        "thank you for your interest in (?:working at|a career at|joining)|" +
        "we received your resume|application was submitted)"
    );

    // Non-job application noise patterns
    private static final Pattern NOISE_PATTERN = Pattern.compile(
        "(?i)(?:order confirmation|tracking number|shipping update|package delivered|" +
        "verification code|password reset|security alert|login alert|github notification|" +
        "weekly digest|billing statement|invoice receipt|your receipt from|uber ride|lyft ride|" +
        "newsletter|subscription confirmed|credit score)"
    );

    // ==========================================
    // COMPANY EXTRACTION PATTERNS & SETS
    // ==========================================

    private static final Set<String> GENERIC_DOMAINS = new HashSet<>(Arrays.asList(
        "gmail.com", "googlemail.com", "yahoo.com", "hotmail.com", "outlook.com",
        "icloud.com", "mail.com", "protonmail.com", "aol.com", "zoho.com", "live.com"
    ));

    private static final Set<String> ATS_DOMAINS = new HashSet<>(Arrays.asList(
        "greenhouse.io", "lever.co", "myworkday.com", "workday.com", "smartrecruiters.com",
        "ashbyhq.com", "jobvite.com", "workable.com", "bamboohr.com", "icims.com",
        "rippling.com", "talentlyft.com", "hirevue.com", "applicantstack.com",
        "jazzhr.com", "breezy.hr", "hackerrank.com", "codesignal.com", "codility.com",
        "karat.com", "byteboard.com", "calendly.com", "goodtime.io"
    ));

    private static final Set<String> GENERIC_NAMES = new HashSet<>(Arrays.asList(
        "greenhouse", "lever", "workday", "smartrecruiters", "ashby", "jobvite",
        "workable", "bamboohr", "icims", "hackerrank", "codesignal", "codility",
        "karat", "byteboard", "calendly", "goodtime", "recruiting", "recruitment",
        "careers", "talent", "talent acquisition", "jobs", "hr", "hiring",
        "hiring team", "people team", "team", "no-reply", "noreply", "support",
        "notifications", "notification", "do not reply", "human resources", "admin"
    ));

    private static final Pattern FROM_DISPLAY_NAME_PATTERN = Pattern.compile(
        "^\\s*\"?([^\"<]+?)\"?\\s*(?:<([^>]+)>)?$"
    );

    private static final Pattern VIA_PATTERN = Pattern.compile(
        "(?i)^([A-Za-z0-9&.\\- ]+?)\\s+via\\s+"
    );

    private static final Pattern ON_BEHALF_OF_PATTERN = Pattern.compile(
        "(?i)(?:on behalf of)\\s+([A-Za-z0-9&.\\- ]+)"
    );

    private static final Pattern SENDER_CLEANUP_SUFFIX_PATTERN = Pattern.compile(
        "(?i)\\s+(?:Recruiting(?: Team)?|Careers(?: Team)?|Talent(?: Acquisition)?|Team|Jobs|HR|People Team|Hiring Team|Staffing|Recruitment|Automated Notification|via\\s+.*)$"
    );

    private static final Pattern SENDER_CLEANUP_PREFIX_PATTERN = Pattern.compile(
        "(?i)^(?:The\\s+)"
    );

    private static final Pattern SUBJECT_COMPANY_PATTERNS = Pattern.compile(
        "(?i)(?:Thank you for applying to|Thanks for applying to|application to|applying to|applied to|welcome to|career at|position at|interview with|role at|opportunity at|invitation from)\\s+([A-Z0-9][A-Za-z0-9&.\\- ]*?)(?=(?:\\s+(?:for|as|has|was|to|regarding)|\\s*[-–|:!.]|$))"
    );

    private static final Pattern SUBJECT_INVITES_PATTERN = Pattern.compile(
        "(?i)^([A-Z0-9][A-Za-z0-9&.\\- ]+?)\\s+(?:invites you to|invited you to|invitation)"
    );

    private static final Pattern SUBJECT_PREFIX_COMPANY_PATTERN = Pattern.compile(
        "(?i)^([A-Z0-9][A-Za-z0-9&.\\- ]+?)\\s*[:|\\-–]\\s*(?:Application|Interview|Assessment|Thank you|Update|Offer|Invitation|Next steps|Confirmation)"
    );

    // ==========================================
    // ROLE EXTRACTION PATTERNS
    // ==========================================

    private static final Pattern ROLE_EXPLICIT_HEADER_PATTERN = Pattern.compile(
        "(?i)(?:Role|Position|Job Title|Application for|Applied for):\\s*([A-Za-z0-9/\\- ]+?)(?=(?:\\s+(?:at|with)|\\s*[-–|:!.\n\r]|$))"
    );

    private static final Pattern ROLE_BODY_EXPLICIT_PATTERNS = Pattern.compile(
        "(?i)(?:for the|applied for the|applied for|position of|role of|candidacy for the|opening for|candidacy for|regarding the|regarding your application for(?: the)?)\\s+(?:the\\s+)?([A-Za-z0-9/\\- ]+?(?:Engineer|Developer|Manager|Designer|Scientist|Analyst|Intern|Lead|Architect|Specialist|Associate|Director))(?=(?:\\s+(?:role|position|opening|job|at\\s+[A-Za-z0-9])|\\s*[-–|:!.,\n\r]|$))"
    );

    private static final Pattern ROLE_POSITION_SUFFIX_PATTERN = Pattern.compile(
        "(?i)(?:\\b(?:the|that the|a|an)\\s+)?([A-Z][A-Za-z0-9/\\- ]{1,45}?(?:Engineer|Developer|Manager|Designer|Scientist|Analyst|Intern|Lead|Architect|Specialist|Associate|Director))\\s+(?:position|role|opening|job)"
    );

    private static final Pattern ROLE_SUBJECT_PATTERNS = Pattern.compile(
        "(?i)(?:for the|for|role of|position of)\\s+([A-Za-z0-9/\\- ]+?(?:Engineer|Developer|Manager|Designer|Scientist|Analyst|Intern|Lead|Specialist|Associate|Architect|Consultant|Director))(?=(?:\\s+(?:role|position|opening|job|at\\s+[A-Za-z0-9])|\\s*[-–|:!.]|$))"
    );

    private static final Pattern ROLE_SUFFIX_IN_SUBJECT_PATTERN = Pattern.compile(
        "(?i)[-–|:]\\s*(?!Chat with|Meeting with|Call with)([A-Za-z0-9/\\- ]+?(?:Engineer|Developer|Manager|Designer|Scientist|Analyst|Intern|Lead|Architect|Specialist|Associate|Director))(?=(?:\\s+at\\s+[A-Za-z0-9]|\\s*[-–|:!.]|$))"
    );

    @Override
    public ClassificationResult classify(EmailMessageDto email) {
        if (email == null) {
            return ClassificationResult.unknown("Null email provided");
        }

        String subject = email.subject() != null ? email.subject() : "";
        String from = email.from() != null ? email.from() : "";
        String body = email.body() != null ? email.body() : "";
        String combinedText = subject + "\n" + body;

        // 1. Determine Status
        ApplicationStatus status = determineStatus(subject, body, combinedText);

        // If unknown or strictly noise, return UNKNOWN result
        if (status == ApplicationStatus.UNKNOWN) {
            return ClassificationResult.unknown("No application keywords matched or detected as noise");
        }

        // 2. Extract Company Name
        String company = extractCompany(from, subject);

        // 3. Extract Role
        String role = extractRole(subject, body);

        double confidence = calculateConfidence(status, company, role);
        String reasoning = generateReasoning(status, company, role);

        log.debug("Classified email: status={}, company={}, role={}, confidence={}",
            status, company, role, confidence);

        return new ClassificationResult(status, company, role, confidence, reasoning);
    }

    /**
     * Extracts company name from sender display name, domain, or subject.
     */
    public String extractCompany(String from, String subject) {
        // Step 1: Check "Company via Platform" in From
        if (from != null && !from.isBlank()) {
            Matcher viaMatcher = VIA_PATTERN.matcher(from.trim());
            if (viaMatcher.find()) {
                String candidate = cleanCandidateName(viaMatcher.group(1));
                if (isValidCompanyName(candidate)) {
                    return candidate;
                }
            }

            Matcher behalfMatcher = ON_BEHALF_OF_PATTERN.matcher(from);
            if (behalfMatcher.find()) {
                String candidate = cleanCandidateName(behalfMatcher.group(1));
                if (isValidCompanyName(candidate)) {
                    return candidate;
                }
            }
        }

        // Step 2: Parse display name from From header
        String displayName = "";
        String domain = "";
        if (from != null && !from.isBlank()) {
            Matcher fromMatcher = FROM_DISPLAY_NAME_PATTERN.matcher(from.trim());
            if (fromMatcher.matches()) {
                displayName = fromMatcher.group(1) != null ? fromMatcher.group(1).trim() : "";
                String emailAddr = fromMatcher.group(2) != null ? fromMatcher.group(2).trim() : "";
                if (emailAddr.contains("@")) {
                    domain = emailAddr.substring(emailAddr.indexOf('@') + 1).toLowerCase(Locale.ROOT);
                }
            } else if (from.contains("@")) {
                int atIdx = from.indexOf('@');
                domain = from.substring(atIdx + 1).replace(">", "").trim().toLowerCase(Locale.ROOT);
            }
        }

        // Check display name
        if (!displayName.isBlank()) {
            String cleanedDisplay = cleanDisplayName(displayName);
            if (isValidCompanyName(cleanedDisplay)) {
                return cleanedDisplay;
            }
        }

        // Step 3: Extract company from Subject
        if (subject != null && !subject.isBlank()) {
            Matcher invitesMatcher = SUBJECT_INVITES_PATTERN.matcher(subject.trim());
            if (invitesMatcher.find()) {
                String candidate = cleanCandidateName(invitesMatcher.group(1));
                if (isValidCompanyName(candidate)) {
                    return candidate;
                }
            }

            Matcher subMatcher = SUBJECT_COMPANY_PATTERNS.matcher(subject);
            if (subMatcher.find()) {
                String candidate = cleanCandidateName(subMatcher.group(1));
                if (isValidCompanyName(candidate)) {
                    return candidate;
                }
            }

            Matcher prefixMatcher = SUBJECT_PREFIX_COMPANY_PATTERN.matcher(subject);
            if (prefixMatcher.find()) {
                String candidate = cleanCandidateName(prefixMatcher.group(1));
                if (isValidCompanyName(candidate)) {
                    return candidate;
                }
            }
        }

        // Step 4: Extract from Domain (if not ATS or generic provider)
        if (!domain.isBlank() && !isAtsOrGenericDomain(domain)) {
            String domainCompany = extractCompanyFromDomain(domain);
            if (isValidCompanyName(domainCompany)) {
                return domainCompany;
            }
        }

        return "Unknown Company";
    }

    /**
     * Extracts role from subject and body.
     */
    public String extractRole(String subject, String body) {
        // Step 1: Explicit header in subject or body
        if (subject != null && !subject.isBlank()) {
            Matcher explicitMatcher = ROLE_EXPLICIT_HEADER_PATTERN.matcher(subject);
            if (explicitMatcher.find()) {
                String role = cleanRoleName(explicitMatcher.group(1));
                if (isValidRole(role)) {
                    return role;
                }
            }
        }

        if (body != null && !body.isBlank()) {
            Matcher explicitBodyMatcher = ROLE_EXPLICIT_HEADER_PATTERN.matcher(body);
            if (explicitBodyMatcher.find()) {
                String role = cleanRoleName(explicitBodyMatcher.group(1));
                if (isValidRole(role)) {
                    return role;
                }
            }

            // Step 2: Body patterns like "position of <Role>", "for the <Role> role", "regarding the <Role> position"
            Matcher bodyMatcher = ROLE_BODY_EXPLICIT_PATTERNS.matcher(body);
            if (bodyMatcher.find()) {
                String role = cleanRoleName(bodyMatcher.group(1));
                if (isValidRole(role)) {
                    return role;
                }
            }

            Matcher positionSuffixMatcher = ROLE_POSITION_SUFFIX_PATTERN.matcher(body);
            if (positionSuffixMatcher.find()) {
                String role = cleanRoleName(positionSuffixMatcher.group(1));
                if (isValidRole(role)) {
                    return role;
                }
            }
        }

        // Step 3: Subject patterns
        if (subject != null && !subject.isBlank()) {
            Matcher subjectMatcher = ROLE_SUBJECT_PATTERNS.matcher(subject);
            if (subjectMatcher.find()) {
                String role = cleanRoleName(subjectMatcher.group(1));
                if (isValidRole(role)) {
                    return role;
                }
            }

            Matcher suffixMatcher = ROLE_SUFFIX_IN_SUBJECT_PATTERN.matcher(subject);
            if (suffixMatcher.find()) {
                String role = cleanRoleName(suffixMatcher.group(1));
                if (isValidRole(role)) {
                    return role;
                }
            }
        }

        // Step 4: Fallback heuristic if mentions software engineer
        if ((subject != null && subject.toLowerCase(Locale.ROOT).contains("software engineer")) ||
            (body != null && body.toLowerCase(Locale.ROOT).contains("software engineer"))) {
            return "Software Engineer";
        }

        return "Role Not Specified";
    }

    private ApplicationStatus determineStatus(String subject, String body, String combined) {
        // First check OFFER (with negative guard)
        if (OFFER_PATTERN.matcher(combined).find() && !OFFER_NEGATIVE_GUARD.matcher(combined).find()) {
            return ApplicationStatus.OFFER;
        }

        // Second check REJECTED
        if (REJECTED_PATTERN.matcher(combined).find()) {
            return ApplicationStatus.REJECTED;
        }

        // Third check INTERVIEW
        if (INTERVIEW_PATTERN.matcher(combined).find()) {
            return ApplicationStatus.INTERVIEW;
        }

        // Fourth check ASSESSMENT
        if (ASSESSMENT_PATTERN.matcher(combined).find()) {
            return ApplicationStatus.ASSESSMENT;
        }

        // Fifth check APPLIED
        if (APPLIED_PATTERN.matcher(combined).find()) {
            return ApplicationStatus.APPLIED;
        }

        // Check if pure noise
        if (NOISE_PATTERN.matcher(combined).find()) {
            return ApplicationStatus.UNKNOWN;
        }

        return ApplicationStatus.UNKNOWN;
    }

    private String cleanDisplayName(String displayName) {
        if (displayName == null) return "";
        String cleaned = displayName.trim();
        // Remove "The " prefix
        cleaned = SENDER_CLEANUP_PREFIX_PATTERN.matcher(cleaned).replaceFirst("");
        // Remove recruiting suffixes
        cleaned = SENDER_CLEANUP_SUFFIX_PATTERN.matcher(cleaned).replaceFirst("");
        // Remove trailing punctuation
        cleaned = cleaned.replaceAll("[\\s,.\\-]+$", "").trim();
        return cleaned;
    }

    private String cleanCandidateName(String name) {
        if (name == null) return "";
        String cleaned = name.trim();
        cleaned = SENDER_CLEANUP_PREFIX_PATTERN.matcher(cleaned).replaceFirst("");
        cleaned = SENDER_CLEANUP_SUFFIX_PATTERN.matcher(cleaned).replaceFirst("");
        cleaned = cleaned.replaceAll("[\\s,.\\-]+$", "").trim();
        return cleaned;
    }

    private boolean isValidCompanyName(String name) {
        if (name == null || name.isBlank() || name.length() < 2 || name.length() > 60) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT).trim();
        if (GENERIC_NAMES.contains(lower)) {
            return false;
        }
        // Avoid strings that look like generic sentences or punctuation
        return name.matches(".*[a-zA-Z0-9].*");
    }

    private boolean isAtsOrGenericDomain(String domain) {
        if (domain == null) return true;
        String lower = domain.toLowerCase(Locale.ROOT);
        if (GENERIC_DOMAINS.contains(lower) || ATS_DOMAINS.contains(lower)) {
            return true;
        }
        for (String ats : ATS_DOMAINS) {
            if (lower.endsWith("." + ats)) {
                return true;
            }
        }
        return false;
    }

    private String extractCompanyFromDomain(String domain) {
        // e.g. "mail.stripe.com" -> parts: ["mail", "stripe", "com"]
        String[] parts = domain.split("\\.");
        if (parts.length >= 2) {
            String candidate = parts[parts.length - 2];
            // If subdomain has company name, e.g. "careers.airbnb.com" -> airbnb
            if (candidate.length() >= 2 && !GENERIC_NAMES.contains(candidate.toLowerCase(Locale.ROOT))) {
                return capitalize(candidate);
            }
        }
        return "";
    }

    private String cleanRoleName(String role) {
        if (role == null) return "";
        String cleaned = role.trim();
        cleaned = cleaned.replaceAll("(?i)^.*?\\b(?:that the|for the|for|that|the|a|an)\\s+", "");
        cleaned = cleaned.replaceAll("[\\s,.\\-–|]+$", "").trim();
        return cleaned;
    }

    private boolean isValidRole(String role) {
        if (role == null || role.isBlank() || role.length() < 3 || role.length() > 80) {
            return false;
        }
        String lower = role.toLowerCase(Locale.ROOT);
        return !GENERIC_NAMES.contains(lower);
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) return "";
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private double calculateConfidence(ApplicationStatus status, String company, String role) {
        double score = 0.80;
        if (status == ApplicationStatus.OFFER || status == ApplicationStatus.REJECTED) {
            score += 0.10;
        } else if (status == ApplicationStatus.INTERVIEW || status == ApplicationStatus.ASSESSMENT) {
            score += 0.08;
        }
        if (company != null && !"Unknown Company".equals(company)) {
            score += 0.05;
        }
        if (role != null && !"Role Not Specified".equals(role)) {
            score += 0.05;
        }
        return Math.min(1.0, score);
    }

    private String generateReasoning(ApplicationStatus status, String company, String role) {
        return String.format("Classified as %s for '%s' (%s) based on matching email patterns.",
            status, company != null ? company : "Unknown", role != null ? role : "Role Not Specified");
    }
}
