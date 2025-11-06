package vcmsa.projects.district49_android

data class TermsSection(
    val title: String,
    val content: String
)

object TermsAndConditionsContent {
    val sections = listOf(
        TermsSection(
            title = "Acceptance of Terms",
            content = "By making a donation to District 49 Child and Youth Care Centre (\"District 49\"), you agree to be bound by these Terms and Conditions. Please read them carefully before proceeding with your donation."
        ),
        TermsSection(
            title = "About District 49",
            content = "District 49 is a registered non-profit organization (NPO) dedicated to providing care, shelter, and support to vulnerable children in Umkomaas, KwaZulu-Natal, South Africa. All donations are used to support our mission of protecting and nurturing children in need."
        ),
        TermsSection(
            title = "Donation Processing",
            content = "All donations are processed securely through PayFast, a registered Payment Service Provider. District 49 does not store or have access to your payment card details. By proceeding, you will be redirected to PayFast's secure payment gateway to complete your transaction."
        ),
        TermsSection(
            title = "Donation Policy",
            content = "All donations are voluntary and non-refundable except in cases of duplicate transactions or processing errors. Donations are processed in South African Rand (ZAR). You will receive a confirmation email upon successful completion of your donation."
        ),
        TermsSection(
            title = "Tax Deductibility",
            content = "As a registered NPO, donations to District 49 may qualify for tax deductions under South African tax law (Section 18A). A tax certificate will be issued for donations of R100 or more upon request. Please contact us at the details provided on our website to request your certificate."
        ),
        TermsSection(
            title = "Use of Donations",
            content = "All donations received are used to support the operations of District 49, including but not limited to: provision of food, clothing, shelter, education, healthcare, and emotional support for children in our care. We reserve the right to allocate funds where they are most needed to fulfill our mission."
        ),
        TermsSection(
            title = "Privacy and Data Protection",
            content = "District 49 is committed to protecting your personal information in accordance with the Protection of Personal Information Act (POPIA). Your payment information is processed by PayFast and is not stored by District 49. We collect only the information necessary to process your donation and issue receipts. We will not share your information with third parties except as required for payment processing or by law."
        ),
        TermsSection(
            title = "Payment Security",
            content = "PayFast employs industry-standard security measures to protect your payment information. All transactions are encrypted using SSL technology. District 49 is not responsible for any issues arising from the PayFast payment gateway, though we will assist in resolving any payment-related queries."
        ),
        TermsSection(
            title = "Limitation of Liability",
            content = "District 49 and its affiliates shall not be liable for any indirect, incidental, or consequential damages arising from your donation or use of our donation platform. Our total liability shall not exceed the amount of your donation."
        ),
        TermsSection(
            title = "Changes to Terms",
            content = "District 49 reserves the right to modify these Terms and Conditions at any time. Changes will be effective immediately upon posting to our website or donation platform. Your continued use of the donation platform constitutes acceptance of any modifications."
        ),
        TermsSection(
            title = "Governing Law",
            content = "These Terms and Conditions are governed by the laws of the Republic of South Africa. Any disputes shall be resolved in accordance with South African law and subject to the exclusive jurisdiction of the South African courts."
        ),
        TermsSection(
            title = "Contact Information",
            content = "If you have any questions about these Terms and Conditions or your donation, please contact us through the contact information provided on our website. Thank you for your generous support of District 49 and the children in our care."
        )
    )

    fun getFullText(): String {
        return sections.joinToString("\n\n") { section ->
            "${section.title}\n${section.content}"
        }
    }
}