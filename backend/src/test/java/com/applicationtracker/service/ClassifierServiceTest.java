package com.applicationtracker.service;

import com.applicationtracker.domain.ApplicationStatus;
import com.applicationtracker.dto.ClassificationResult;
import com.applicationtracker.dto.EmailMessageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClassifierServiceTest {

    private ClassifierService classifier;

    @BeforeEach
    void setUp() {
        classifier = new ClassifierService();
    }

    @Nested
    @DisplayName("APPLIED Status Tests - Realistic Emails")
    class AppliedEmailTests {

        @Test
        @DisplayName("1. Greenhouse application confirmation email")
        void testGreenhouseApplicationConfirmation() {
            EmailMessageDto email = EmailMessageDto.of(
                "msg-app-01",
                "Thank you for applying to Figma",
                "Figma Recruiting <no-reply@greenhouse.io>",
                "Hi Alex,\n\nThank you for applying to Figma! We have received your application for the " +
                "Senior Frontend Engineer position. Our team will review your qualifications and reach out if there's a fit.\n\nBest,\nThe Figma Team"
            );

            ClassificationResult result = classifier.classify(email);

            assertThat(result.status()).isEqualTo(ApplicationStatus.APPLIED);
            assertThat(result.company()).isEqualTo("Figma");
            assertThat(result.role()).isEqualTo("Senior Frontend Engineer");
            assertThat(result.confidence()).isGreaterThan(0.8);
        }

        @Test
        @DisplayName("2. Lever application submission confirmation email")
        void testLeverApplicationReceived() {
            EmailMessageDto email = EmailMessageDto.of(
                "msg-app-02",
                "Application Received: Fullstack Developer at Atlassian",
                "Atlassian Talent <jobs@lever.co>",
                "Hello,\n\nThanks for applying! Your application has been submitted successfully for the " +
                "Fullstack Developer role at Atlassian. You can check your application status anytime via this link."
            );

            ClassificationResult result = classifier.classify(email);

            assertThat(result.status()).isEqualTo(ApplicationStatus.APPLIED);
            assertThat(result.company()).isEqualTo("Atlassian");
            assertThat(result.role()).isEqualTo("Fullstack Developer");
        }

        @Test
        @DisplayName("3. Workday automated application receipt email")
        void testWorkdayApplicationReceipt() {
            EmailMessageDto email = EmailMessageDto.of(
                "msg-app-03",
                "Netflix Careers - Application Confirmation (Backend Engineer)",
                "Netflix Careers <netflix@myworkday.com>",
                "Dear Candidate,\n\nWe have received your application for the Backend Engineer position at Netflix. " +
                "Thank you for your interest in joining our team. Our recruiting team is currently reviewing your resume."
            );

            ClassificationResult result = classifier.classify(email);

            assertThat(result.status()).isEqualTo(ApplicationStatus.APPLIED);
            assertThat(result.company()).isEqualTo("Netflix");
            assertThat(result.role()).isEqualTo("Backend Engineer");
        }

        @Test
        @DisplayName("4. Direct company application confirmation from corporate domain")
        void testDirectCompanyApplicationReceipt() {
            EmailMessageDto email = EmailMessageDto.of(
                "msg-app-04",
                "Thank you for your interest in Stripe!",
                "Stripe Recruiting Team <recruiting@stripe.com>",
                "Hi Jordan,\n\nConfirming receipt of your application for the Software Engineer role at Stripe. " +
                "We appreciate the time you invested in applying to Stripe and will be in touch with next steps."
            );

            ClassificationResult result = classifier.classify(email);

            assertThat(result.status()).isEqualTo(ApplicationStatus.APPLIED);
            assertThat(result.company()).isEqualTo("Stripe");
            assertThat(result.role()).isEqualTo("Software Engineer");
        }

        @Test
        @DisplayName("5. Ashby confirmation for Mobile Engineer role")
        void testAshbyApplicationReceipt() {
            EmailMessageDto email = EmailMessageDto.of(
                "msg-app-05",
                "Notion - We've received your application",
                "Notion <jobs@ashbyhq.com>",
                "Hi Sam,\n\nWe've received your application for the Mobile Engineer position at Notion! " +
                "Thank you for applying. We are reviewing applications on a rolling basis."
            );

            ClassificationResult result = classifier.classify(email);

            assertThat(result.status()).isEqualTo(ApplicationStatus.APPLIED);
            assertThat(result.company()).isEqualTo("Notion");
            assertThat(result.role()).isEqualTo("Mobile Engineer");
        }
    }

    @Nested
    @DisplayName("ASSESSMENT Status Tests - Realistic Emails")
    class AssessmentEmailTests {

        @Test
        @DisplayName("6. HackerRank online assessment invitation")
        void testHackerRankAssessment() {
            EmailMessageDto email = EmailMessageDto.of(
                "msg-ass-01",
                "Stripe invited you to take a HackerRank assessment",
                "HackerRank on behalf of Stripe <assessments@hackerrank.com>",
                "Hello,\n\nStripe has invited you to complete an online assessment for the Software Engineer position. " +
                "Please click the button below to start your timed test. You will have 90 minutes to complete the coding challenge."
            );

            ClassificationResult result = classifier.classify(email);

            assertThat(result.status()).isEqualTo(ApplicationStatus.ASSESSMENT);
            assertThat(result.company()).isEqualTo("Stripe");
            assertThat(result.role()).isEqualTo("Software Engineer");
        }

        @Test
        @DisplayName("7. CodeSignal general coding assessment invitation")
        void testCodeSignalAssessment() {
            EmailMessageDto email = EmailMessageDto.of(
                "msg-ass-02",
                "Uber invites you to complete an assessment on CodeSignal",
                "CodeSignal Team <notifications@codesignal.com>",
                "Hi Taylor,\n\nUber invites you to complete the General Coding Assessment on CodeSignal for the " +
                "Distributed Systems Engineer position. Please complete your test within 7 days."
            );

            ClassificationResult result = classifier.classify(email);

            assertThat(result.status()).isEqualTo(ApplicationStatus.ASSESSMENT);
            assertThat(result.company()).isEqualTo("Uber");
            assertThat(result.role()).isEqualTo("Distributed Systems Engineer");
        }

        @Test
        @DisplayName("8. Codility technical test invitation")
        void testCodilityAssessment() {
            EmailMessageDto email = EmailMessageDto.of(
                "msg-ass-03",
                "Invitation: ByteDance Online Technical Assessment",
                "ByteDance Recruiting <recruiting@bytedance.com>",
                "Dear Candidate,\n\nPlease complete the online coding test on Codility for the Backend Developer position. " +
                "The skills assessment contains 3 algorithmic problems to solve within 120 minutes."
            );

            ClassificationResult result = classifier.classify(email);

            assertThat(result.status()).isEqualTo(ApplicationStatus.ASSESSMENT);
            assertThat(result.company()).isEqualTo("ByteDance");
            assertThat(result.role()).isEqualTo("Backend Developer");
        }

        @Test
        @DisplayName("9. Take-home assignment invitation")
        void testTakeHomeChallenge() {
            EmailMessageDto email = EmailMessageDto.of(
                "msg-ass-04",
                "Next Steps: Take-home challenge for Linear",
                "Linear Team <hiring@linear.app>",
                "Hi,\n\nAs the next step in our process for the Fullstack Engineer role, we would love for you to complete " +
                "this take-home assignment. It should take around 3 to 4 hours. Feel free to reply with any questions!"
            );

            ClassificationResult result = classifier.classify(email);

            assertThat(result.status()).isEqualTo(ApplicationStatus.ASSESSMENT);
            assertThat(result.company()).isEqualTo("Linear");
            assertThat(result.role()).isEqualTo("Fullstack Engineer");
        }

        @Test
        @DisplayName("10. Karat technical screening invitation")
        void testKaratTechnicalAssessment() {
            EmailMessageDto email = EmailMessageDto.of(
                "msg-ass-05",
                "Datadog - Schedule your Karat technical assessment",
                "Datadog Recruiting <talent@datadoghq.com>",
                "Hi Morgan,\n\nThanks for speaking with us! The next step is a Karat technical assessment for the " +
                "Senior Software Engineer role. Please select a time that suits you best using the Karat link."
            );

            ClassificationResult result = classifier.classify(email);

            assertThat(result.status()).isEqualTo(ApplicationStatus.ASSESSMENT);
            assertThat(result.company()).isEqualTo("Datadog");
            assertThat(result.role()).isEqualTo("Senior Software Engineer");
        }
    }

    @Nested
    @DisplayName("INTERVIEW Status Tests - Realistic Emails")
    class InterviewEmailTests {

        @Test
        @DisplayName("11. Recruiter phone screen invitation")
        void testRecruiterPhoneScreen() {
            EmailMessageDto email = EmailMessageDto.of(
                "msg-int-01",
                "Invitation to interview: Phone screen with Snowflake recruiter",
                "Snowflake Talent <recruiting@snowflake.com>",
                "Hi Casey,\n\nWe were impressed by your resume and would like to invite you to interview for a brief 30-minute call " +
                "with our recruiter regarding the Cloud Infrastructure Engineer position at Snowflake. Please share your availability."
            );

            ClassificationResult result = classifier.classify(email);

            assertThat(result.status()).isEqualTo(ApplicationStatus.INTERVIEW);
            assertThat(result.company()).isEqualTo("Snowflake");
            assertThat(result.role()).isEqualTo("Cloud Infrastructure Engineer");
        }

        @Test
        @DisplayName("12. Technical interview invitation")
        void testTechnicalInterviewRound() {
            EmailMessageDto email = EmailMessageDto.of(
                "msg-int-02",
                "Meta - Invitation to Technical Interview",
                "Meta Careers <careers@meta.com>",
                "Hi David,\n\nCongratulations on passing the initial screen! We would like to schedule a technical interview " +
                "with an engineer on the team for the Production Engineer role. Please provide dates and times you are free."
            );

            ClassificationResult result = classifier.classify(email);

            assertThat(result.status()).isEqualTo(ApplicationStatus.INTERVIEW);
            assertThat(result.company()).isEqualTo("Meta");
            assertThat(result.role()).isEqualTo("Production Engineer");
        }

        @Test
        @DisplayName("13. Virtual Onsite schedule confirmation")
        void testVirtualOnsiteSchedule() {
            EmailMessageDto email = EmailMessageDto.of(
                "msg-int-03",
                "Airbnb - Your Virtual Onsite Interview schedule",
                "Airbnb Recruiting <talent@airbnb.com>",
                "Hi Elena,\n\nWe are excited to confirm your virtual onsite interview for the Staff Software Engineer role at Airbnb! " +
                "Here are your interview details and schedule for next Tuesday."
            );

            ClassificationResult result = classifier.classify(email);

            assertThat(result.status()).isEqualTo(ApplicationStatus.INTERVIEW);
            assertThat(result.company()).isEqualTo("Airbnb");
            assertThat(result.role()).isEqualTo("Staff Software Engineer");
        }

        @Test
        @DisplayName("14. Hiring manager 30-minute chat invitation")
        void testHiringManagerChat() {
            EmailMessageDto email = EmailMessageDto.of(
                "msg-int-04",
                "Spotify: Next Steps - Chat with the Engineering Manager",
                "Spotify HR <talent@spotify.com>",
                "Hi Chris,\n\nWe would love to schedule a time to chat with the hiring manager regarding your candidacy for the " +
                "Machine Learning Engineer opening at Spotify. Please use this calendar link to pick a slot."
            );

            ClassificationResult result = classifier.classify(email);

            assertThat(result.status()).isEqualTo(ApplicationStatus.INTERVIEW);
            assertThat(result.company()).isEqualTo("Spotify");
            assertThat(result.role()).isEqualTo("Machine Learning Engineer");
        }

        @Test
        @DisplayName("15. Interview confirmation details")
        void testInterviewConfirmed() {
            EmailMessageDto email = EmailMessageDto.of(
                "msg-int-05",
                "Interview Confirmation - Senior DevOps Engineer at Datadog",
                "Datadog Recruiting Team <recruiting@datadoghq.com>",
                "Hello,\n\nThis email confirms your upcoming interview scheduled for tomorrow at 2:00 PM EST for the " +
                "Senior DevOps Engineer position. You will meet with Sarah from the Infrastructure team."
            );

            ClassificationResult result = classifier.classify(email);

            assertThat(result.status()).isEqualTo(ApplicationStatus.INTERVIEW);
            assertThat(result.company()).isEqualTo("Datadog");
            assertThat(result.role()).isEqualTo("Senior DevOps Engineer");
        }
    }

    @Nested
    @DisplayName("REJECTED Status Tests - Realistic Emails")
    class RejectedEmailTests {

        @Test
        @DisplayName("16. Standard polite rejection email")
        void testPoliteRejection() {
            EmailMessageDto email = EmailMessageDto.of(
                "msg-rej-01",
                "Your application to Stripe",
                "Stripe Recruiting <recruiting@stripe.com>",
                "Hi Jordan,\n\nThank you for your interest in Stripe and for taking the time to apply for the " +
                "Software Engineer position. Unfortunately, we have decided to pursue other candidates whose skills " +
                "more closely align with our current needs. We wish you the best in your job search."
            );

            ClassificationResult result = classifier.classify(email);

            assertThat(result.status()).isEqualTo(ApplicationStatus.REJECTED);
            assertThat(result.company()).isEqualTo("Stripe");
            assertThat(result.role()).isEqualTo("Software Engineer");
        }

        @Test
        @DisplayName("17. Post-assessment rejection email")
        void testPostAssessmentRejection() {
            EmailMessageDto email = EmailMessageDto.of(
                "msg-rej-02",
                "Update regarding your application with Google",
                "Google Careers <careers@google.com>",
                "Dear Candidate,\n\nThank you for completing the technical assessment. Unfortunately, after careful consideration, " +
                "we will not be moving forward with your candidacy for the Site Reliability Engineer role at this time."
            );

            ClassificationResult result = classifier.classify(email);

            assertThat(result.status()).isEqualTo(ApplicationStatus.REJECTED);
            assertThat(result.company()).isEqualTo("Google");
            assertThat(result.role()).isEqualTo("Site Reliability Engineer");
        }

        @Test
        @DisplayName("18. Post-interview rejection email with 'regret to inform'")
        void testPostInterviewRejection() {
            EmailMessageDto email = EmailMessageDto.of(
                "msg-rej-03",
                "Application Update: Apple Software Engineer",
                "Apple Talent <talent@apple.com>",
                "Hi Riley,\n\nThank you for interviewing with our team. We regret to inform you that we have chosen to move forward " +
                "with another candidate for the iOS Engineer role. We were very impressed with your background and encourage you to apply again."
            );

            ClassificationResult result = classifier.classify(email);

            assertThat(result.status()).isEqualTo(ApplicationStatus.REJECTED);
            assertThat(result.company()).isEqualTo("Apple");
            assertThat(result.role()).isEqualTo("iOS Engineer");
        }

        @Test
        @DisplayName("19. Position closed / filled rejection email")
        void testPositionClosedRejection() {
            EmailMessageDto email = EmailMessageDto.of(
                "msg-rej-04",
                "Amazon - Update on your application",
                "Amazon Jobs <no-reply@amazon.com>",
                "Hello,\n\nWe wanted to let you know that the Software Development Engineer position has been filled. " +
                "Therefore, we will not be proceeding with your application at Amazon. Thank you for your interest."
            );

            ClassificationResult result = classifier.classify(email);

            assertThat(result.status()).isEqualTo(ApplicationStatus.REJECTED);
            assertThat(result.company()).isEqualTo("Amazon");
            assertThat(result.role()).isEqualTo("Software Development Engineer");
        }

        @Test
        @DisplayName("20. Direct rejection: unable to offer an interview")
        void testUnableToOfferInterviewRejection() {
            EmailMessageDto email = EmailMessageDto.of(
                "msg-rej-05",
                "Your application to DoorDash",
                "DoorDash Recruiting <recruiting@doordash.com>",
                "Hi Jamie,\n\nThank you for applying to DoorDash. While we were impressed with your experience, " +
                "we are unable to offer you an interview for the Data Engineer position at this time."
            );

            ClassificationResult result = classifier.classify(email);

            assertThat(result.status()).isEqualTo(ApplicationStatus.REJECTED);
            assertThat(result.company()).isEqualTo("DoorDash");
            assertThat(result.role()).isEqualTo("Data Engineer");
        }
    }

    @Nested
    @DisplayName("OFFER Status Tests - Realistic Emails")
    class OfferEmailTests {

        @Test
        @DisplayName("21. Formal offer letter email")
        void testFormalOfferLetter() {
            EmailMessageDto email = EmailMessageDto.of(
                "msg-off-01",
                "Offer of Employment - Senior Software Engineer at Apple",
                "Apple HR <hr@apple.com>",
                "Dear Jesse,\n\nWe are pleased to offer you the position of Senior Software Engineer at Apple! " +
                "Please review the attached offer letter outlining your compensation, equity, and start date. Congratulations!"
            );

            ClassificationResult result = classifier.classify(email);

            assertThat(result.status()).isEqualTo(ApplicationStatus.OFFER);
            assertThat(result.company()).isEqualTo("Apple");
            assertThat(result.role()).isEqualTo("Senior Software Engineer");
            assertThat(result.confidence()).isGreaterThan(0.85);
        }

        @Test
        @DisplayName("22. Job offer with attached offer letter")
        void testStripeOfferLetter() {
            EmailMessageDto email = EmailMessageDto.of(
                "msg-off-02",
                "Congratulations on your offer from Stripe!",
                "Stripe Recruiting <talent@stripe.com>",
                "Hi Jamie,\n\nThe team was blown away by your interviews! We are thrilled to extend an offer to you " +
                "for the Backend Engineer role. Attached is your offer letter with full details."
            );

            ClassificationResult result = classifier.classify(email);

            assertThat(result.status()).isEqualTo(ApplicationStatus.OFFER);
            assertThat(result.company()).isEqualTo("Stripe");
            assertThat(result.role()).isEqualTo("Backend Engineer");
        }

        @Test
        @DisplayName("23. Official employment offer email")
        void testFigmaOfficialOffer() {
            EmailMessageDto email = EmailMessageDto.of(
                "msg-off-03",
                "Figma - Job Offer: Frontend Architect",
                "Figma Talent Acquisition <talent@figma.com>",
                "Hi Jordan,\n\nWe are delighted to offer you employment as Frontend Architect at Figma. " +
                "Please find your formal offer letter attached for digital signature."
            );

            ClassificationResult result = classifier.classify(email);

            assertThat(result.status()).isEqualTo(ApplicationStatus.OFFER);
            assertThat(result.company()).isEqualTo("Figma");
            assertThat(result.role()).isEqualTo("Frontend Architect");
        }

        @Test
        @DisplayName("24. Netflix offer details and next steps")
        void testNetflixOfferDetails() {
            EmailMessageDto email = EmailMessageDto.of(
                "msg-off-04",
                "Netflix: Offer Details and Next Steps",
                "Netflix Talent <careers@netflix.com>",
                "Hi Morgan,\n\nWe would like to offer you the position of Cloud Platform Engineer at Netflix. " +
                "Please see the attached formal job offer and let us know if you have any questions."
            );

            ClassificationResult result = classifier.classify(email);

            assertThat(result.status()).isEqualTo(ApplicationStatus.OFFER);
            assertThat(result.company()).isEqualTo("Netflix");
            assertThat(result.role()).isEqualTo("Cloud Platform Engineer");
        }
    }

    @Nested
    @DisplayName("UNKNOWN / Non-Application Noise Email Tests")
    class UnknownEmailTests {

        @Test
        @DisplayName("25. LinkedIn weekly newsletter / job alert digest")
        void testLinkedInNewsletter() {
            EmailMessageDto email = EmailMessageDto.of(
                "msg-unk-01",
                "Top job picks for you this week",
                "LinkedIn Job Alerts <jobalerts-noreply@linkedin.com>",
                "Alex, see 10 new software engineer jobs matching your preferences. Unsubscribe from this weekly digest anytime."
            );

            ClassificationResult result = classifier.classify(email);

            assertThat(result.status()).isEqualTo(ApplicationStatus.UNKNOWN);
            assertThat(result.status().isActionable()).isFalse();
        }

        @Test
        @DisplayName("26. GitHub security advisory / Dependabot notification")
        void testGitHubNotification() {
            EmailMessageDto email = EmailMessageDto.of(
                "msg-unk-02",
                "[GitHub] Security Alert: 1 vulnerability found in repo",
                "GitHub <notifications@github.com>",
                "Dependabot detected a high severity vulnerability in your project. Check the security alert details in github."
            );

            ClassificationResult result = classifier.classify(email);

            assertThat(result.status()).isEqualTo(ApplicationStatus.UNKNOWN);
        }

        @Test
        @DisplayName("27. Amazon shipping / order confirmation")
        void testAmazonOrderConfirmation() {
            EmailMessageDto email = EmailMessageDto.of(
                "msg-unk-03",
                "Your Amazon.com order confirmation #114-8928374-2938472",
                "Amazon.com <auto-confirm@amazon.com>",
                "Thank you for shopping with us. Your order confirmation is below. Your tracking number will be provided when package delivered."
            );

            ClassificationResult result = classifier.classify(email);

            assertThat(result.status()).isEqualTo(ApplicationStatus.UNKNOWN);
        }

        @Test
        @DisplayName("28. Google security alert / verification code")
        void testSecurityAlert() {
            EmailMessageDto email = EmailMessageDto.of(
                "msg-unk-04",
                "Google Account: Security alert for login",
                "Google <no-reply@accounts.google.com>",
                "New sign-in detected from Windows device. Your verification code is 492810. If this was not you, reset password immediately."
            );

            ClassificationResult result = classifier.classify(email);

            assertThat(result.status()).isEqualTo(ApplicationStatus.UNKNOWN);
        }

        @Test
        @DisplayName("29. Uber ride receipt")
        void testUberReceipt() {
            EmailMessageDto email = EmailMessageDto.of(
                "msg-unk-05",
                "Your receipt from your Sunday morning Uber ride",
                "Uber Receipts <uber.us@uber.com>",
                "Thanks for riding with us. Total charged: $24.50. Check your billing statement for details."
            );

            ClassificationResult result = classifier.classify(email);

            assertThat(result.status()).isEqualTo(ApplicationStatus.UNKNOWN);
        }
    }

    @Nested
    @DisplayName("Company and Role Extractor Edge Cases")
    class ExtractorEdgeCasesTests {

        @Test
        @DisplayName("30. Extract company from display name with 'on behalf of'")
        void testCompanyExtractionOnBehalfOf() {
            String company = classifier.extractCompany(
                "HackerRank on behalf of Datadog <assessments@hackerrank.com>",
                "Coding challenge"
            );
            assertThat(company).isEqualTo("Datadog");
        }

        @Test
        @DisplayName("31. Extract company from display name with 'via Greenhouse'")
        void testCompanyExtractionViaGreenhouse() {
            String company = classifier.extractCompany(
                "Figma via Greenhouse <no-reply@greenhouse.io>",
                "Application confirmation"
            );
            assertThat(company).isEqualTo("Figma");
        }

        @Test
        @DisplayName("32. Extract company from domain when display name is generic 'Recruiting'")
        void testCompanyExtractionFromCorporateDomain() {
            String company = classifier.extractCompany(
                "Recruiting <jobs@datadoghq.com>",
                "Thank you for your interest"
            );
            assertThat(company).isEqualTo("Datadoghq");
        }

        @Test
        @DisplayName("33. Extract role from subject line with hyphen suffix")
        void testRoleExtractionFromSubjectSuffix() {
            String role = classifier.extractRole(
                "Interview Confirmation - Site Reliability Engineer",
                "We look forward to meeting you."
            );
            assertThat(role).isEqualTo("Site Reliability Engineer");
        }

        @Test
        @DisplayName("34. Extract role from body when subject is generic")
        void testRoleExtractionFromBody() {
            String role = classifier.extractRole(
                "Thank you for your application",
                "We received your application for the Principal Systems Architect opening."
            );
            assertThat(role).isEqualTo("Principal Systems Architect");
        }
    }
}
