package com.demo.payflowai.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

/**
 * Tool methods available to the chat LLM (gpt-oss:20b).
 * In a production integration these would query real data sources;
 * here they return deterministic mock data suitable for sales demos.
 */
@Component
public class PaymentDataTools {

    private static final String[] BANKS = {
        "Albion Bank PLC", "Meridian Bank PLC", "Crestfield Group PLC",
        "Harrington PLC", "Caledonian Bank", "Vantage Bank PLC"
    };

    private static final String[] STATUSES = {"COMPLETED", "COMPLETED", "COMPLETED", "FLAGGED", "PENDING"};
    private static final String[] FLAG_REASONS = {"HIGH_AMOUNT", "RAPID_SUCCESSION", "ROUND_NUMBER", "HIGH_RISK_ACCOUNT"};

    @Tool(description = "Get recent payment transactions. Returns transaction reference, sender, receiver, amount in GBP, status, risk score, and timestamp.")
    public String getRecentTransactions(int limit) {
        var sb = new StringBuilder("Recent transactions:\n");
        var rng = new Random(42);
        for (int i = 1; i <= Math.min(limit, 20); i++) {
            String sender = BANKS[rng.nextInt(BANKS.length)];
            String receiver = BANKS[rng.nextInt(BANKS.length)];
            while (receiver.equals(sender)) receiver = BANKS[rng.nextInt(BANKS.length)];
            double amount = 5_000 + rng.nextInt(95_000);
            String status = STATUSES[rng.nextInt(STATUSES.length)];
            int risk = status.equals("FLAGGED") ? 60 + rng.nextInt(40) : rng.nextInt(40);
            sb.append(String.format(
                "- PF-2026%04d | %s → %s | £%.0f | %s | risk=%d\n",
                1000 + i, sender, receiver, amount, status, risk));
        }
        return sb.toString();
    }

    @Tool(description = "Get payment statistics: total count, GBP volume, fraud flags raised, success rate, and average risk score.")
    public String getPaymentStatistics() {
        return """
                Payment statistics (last 24 hours):
                - Total transactions: 247
                - Settled volume: £4,823,500
                - Fraud flags raised: 18 (7.3%)
                - Success rate: 91.5%
                - Average risk score: 34
                - Peak hour: 14:00–15:00 (42 transactions)
                - Highest single payment: £98,400 (Vantage Bank → Meridian Bank, COMPLETED)
                """;
    }

    @Tool(description = "Get recent fraud alerts with rule that triggered, risk score, sender, amount, and timestamp.")
    public String getFraudAlerts(int limit) {
        var sb = new StringBuilder("Recent fraud alerts:\n");
        var rng = new Random(99);
        var fmt = DateTimeFormatter.ofPattern("HH:mm");
        var now = LocalDateTime.now();
        for (int i = 1; i <= Math.min(limit, 10); i++) {
            String bank = BANKS[rng.nextInt(BANKS.length)];
            String rule = FLAG_REASONS[rng.nextInt(FLAG_REASONS.length)];
            double amount = 10_000 + rng.nextInt(90_000);
            int risk = 60 + rng.nextInt(40);
            String time = now.minusMinutes(i * 7L).format(fmt);
            sb.append(String.format(
                "- [%s] %s | rule=%s | £%.0f | risk=%d\n",
                time, bank, rule, amount, risk));
        }
        return sb.toString();
    }

    @Tool(description = "Explain all fraud detection rules configured in the system, including their thresholds and risk score contributions.")
    public String explainFraudDetectionRules() {
        return """
                Fraud detection rules (applied in sequence, scores are additive):

                1. HIGH_AMOUNT — payment > £50,000 → +50 risk score
                2. RAPID_SUCCESSION — 3+ payments from same account within 60 seconds → +70 risk score
                3. ROUND_NUMBER — payment is an exact multiple of £5,000 → +30 risk score
                4. HIGH_RISK_ACCOUNT — sender account has prior fraud history → +40 risk score

                Thresholds:
                - Risk score ≥ 60 → transaction FLAGGED, alert raised, payment still processed
                - Risk score < 60 → transaction COMPLETED normally

                Redis sliding-window counters track rapid succession per account ID.
                All rules are evaluated asynchronously via RabbitMQ consumer.
                """;
    }

    @Tool(description = "Get account balance summary for all demo banks.")
    public String getAccountBalances() {
        return """
                Current account balances:
                - Albion Bank PLC:      £4,873,200  (↓ £126,800 today)
                - Meridian Bank PLC:    £3,641,500  (↑ £141,500 today)
                - Crestfield Group PLC: £2,698,000  (↓ £52,000 today)
                - Harrington PLC:       £4,315,750  (↑ £115,750 today)
                - Caledonian Bank:      £1,762,300  (↓ £37,700 today)
                - Vantage Bank PLC:     £6,284,100  (↑ £184,100 today)
                """;
    }

    @Tool(description = "Generate a detailed fraud investigation narrative for a specific transaction reference (e.g. PF-20261004).")
    public String generateFraudNarrative(String transactionReference) {
        return String.format("""
                Fraud Investigation Report — %s

                Transaction flagged at 14:32 UTC.
                Sender: Vantage Bank PLC (acc-006)
                Receiver: Meridian Bank PLC (acc-002)
                Amount: £87,500
                Risk Score: 80 (CRITICAL)

                Rules triggered:
                  • HIGH_AMOUNT: £87,500 exceeds £50,000 threshold (+50)
                  • ROUND_NUMBER: exact multiple of £2,500 (+30)

                Contextual signals:
                  • Sender account made 2 other payments in the past 4 hours (£45,000 and £62,000)
                  • Combined 3-payment total: £194,500 — unusually high for this account
                  • Receiver account is new (created 6 days ago)

                Recommended action: Hold for manual compliance review.
                Escalation path: Payments Risk → Senior Compliance Officer.
                """, transactionReference);
    }
}
