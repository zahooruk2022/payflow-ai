package com.demo.payflowai.controller;

import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api")
public class DemoDataController {

    private final AtomicInteger txnCounter = new AtomicInteger(0);

    private static final List<Map<String, Object>> TRANSACTIONS = List.of(
        txn(1,  "PF-20261017-001", "Vantage Bank PLC",    "GB29VNTK60161331926819", "Albion Bank PLC",      "GB39ALBN60161311935516", 125000.00, "FLAGGED",    85, "17:58"),
        txn(2,  "PF-20261017-002", "Meridian Bank PLC",   "GB82MRDNK0161332584066", "Crestfield Group PLC", "GB76CRSF60161334761498", 250000.00, "FLAGGED",    92, "17:57"),
        txn(3,  "PF-20261017-003", "Albion Bank PLC",     "GB39ALBN60161311935516", "Harrington PLC",       "GB72HGTN60161339625862", 42500.00,  "COMPLETED",  28, "17:56"),
        txn(4,  "PF-20261017-004", "Caledonian Bank",     "GB55CLDN60161338291764", "Vantage Bank PLC",     "GB29VNTK60161331926819", 18750.00,  "COMPLETED",  12, "17:55"),
        txn(5,  "PF-20261017-005", "Crestfield Group PLC","GB76CRSF60161334761498", "Meridian Bank PLC",    "GB82MRDNK0161332584066", 75000.00,  "FLAGGED",    71, "17:54"),
        txn(6,  "PF-20261017-006", "Harrington PLC",      "GB72HGTN60161339625862", "Caledonian Bank",      "GB55CLDN60161338291764", 9250.00,   "COMPLETED",  8,  "17:53"),
        txn(7,  "PF-20261017-007", "Vantage Bank PLC",    "GB29VNTK60161331926819", "Meridian Bank PLC",    "GB82MRDNK0161332584066", 50000.00,  "FLAGGED",    68, "17:52"),
        txn(8,  "PF-20261017-008", "Albion Bank PLC",     "GB39ALBN60161311935516", "Crestfield Group PLC", "GB76CRSF60161334761498", 31200.00,  "COMPLETED",  22, "17:51"),
        txn(9,  "PF-20261017-009", "Meridian Bank PLC",   "GB82MRDNK0161332584066", "Harrington PLC",       "GB72HGTN60161339625862", 88000.00,  "FLAGGED",    78, "17:50"),
        txn(10, "PF-20261017-010", "Caledonian Bank",     "GB55CLDN60161338291764", "Albion Bank PLC",      "GB39ALBN60161311935516", 14300.00,  "COMPLETED",  15, "17:49"),
        txn(11, "PF-20261017-011", "Crestfield Group PLC","GB76CRSF60161334761498", "Vantage Bank PLC",     "GB29VNTK60161331926819", 60000.00,  "FLAGGED",    74, "17:48"),
        txn(12, "PF-20261017-012", "Harrington PLC",      "GB72HGTN60161339625862", "Caledonian Bank",      "GB55CLDN60161338291764", 22100.00,  "COMPLETED",  19, "17:47"),
        txn(13, "PF-20261017-013", "Vantage Bank PLC",    "GB29VNTK60161331926819", "Crestfield Group PLC", "GB76CRSF60161334761498", 37500.00,  "PROCESSING", 35, "17:46"),
        txn(14, "PF-20261017-014", "Albion Bank PLC",     "GB39ALBN60161311935516", "Meridian Bank PLC",    "GB82MRDNK0161332584066", 5500.00,   "COMPLETED",  5,  "17:45"),
        txn(15, "PF-20261017-015", "Meridian Bank PLC",   "GB82MRDNK0161332584066", "Albion Bank PLC",      "GB39ALBN60161311935516", 110000.00, "FLAGGED",    88, "17:44"),
        txn(16, "PF-20261017-016", "Caledonian Bank",     "GB55CLDN60161338291764", "Harrington PLC",       "GB72HGTN60161339625862", 28900.00,  "COMPLETED",  24, "17:43"),
        txn(17, "PF-20261017-017", "Crestfield Group PLC","GB76CRSF60161334761498", "Albion Bank PLC",      "GB39ALBN60161311935516", 45000.00,  "PENDING",    42, "17:42"),
        txn(18, "PF-20261017-018", "Harrington PLC",      "GB72HGTN60161339625862", "Vantage Bank PLC",     "GB29VNTK60161331926819", 7800.00,   "COMPLETED",  9,  "17:41"),
        txn(19, "PF-20261017-019", "Vantage Bank PLC",    "GB29VNTK60161331926819", "Caledonian Bank",      "GB55CLDN60161338291764", 95000.00,  "FLAGGED",    82, "17:40"),
        txn(20, "PF-20261017-020", "Albion Bank PLC",     "GB39ALBN60161311935516", "Harrington PLC",       "GB72HGTN60161339625862", 16400.00,  "COMPLETED",  18, "17:39"),
        txn(21, "PF-20261017-021", "Meridian Bank PLC",   "GB82MRDNK0161332584066", "Caledonian Bank",      "GB55CLDN60161338291764", 33000.00,  "COMPLETED",  27, "17:38"),
        txn(22, "PF-20261017-022", "Crestfield Group PLC","GB76CRSF60161334761498", "Harrington PLC",       "GB72HGTN60161339625862", 200000.00, "FLAGGED",    95, "17:37"),
        txn(23, "PF-20261017-023", "Caledonian Bank",     "GB55CLDN60161338291764", "Meridian Bank PLC",    "GB82MRDNK0161332584066", 11200.00,  "COMPLETED",  10, "17:36"),
        txn(24, "PF-20261017-024", "Harrington PLC",      "GB72HGTN60161339625862", "Crestfield Group PLC", "GB76CRSF60161334761498", 52000.00,  "FLAGGED",    63, "17:35"),
        txn(25, "PF-20261017-025", "Vantage Bank PLC",    "GB29VNTK60161331926819", "Albion Bank PLC",      "GB39ALBN60161311935516", 19800.00,  "COMPLETED",  16, "17:34")
    );

    private static final List<Map<String, Object>> ALERTS = List.of(
        alert(1, "Meridian Bank PLC",   "Crestfield Group PLC", 250000.00, 92, "HIGH_AMOUNT",      "Transaction of £250,000 exceeds the high-value threshold of £50,000. Immediate review required.", "17:57"),
        alert(2, "Crestfield Group PLC","Harrington PLC",       200000.00, 95, "HIGH_AMOUNT",      "Critical: £200,000 transfer flagged for manual authorisation. Risk score 95/100.", "17:37"),
        alert(3, "Vantage Bank PLC",    "Albion Bank PLC",      125000.00, 85, "HIGH_AMOUNT",      "Transaction of £125,000 flagged. Exceeds standard high-value threshold.", "17:58"),
        alert(4, "Vantage Bank PLC",    "Meridian Bank PLC",    50000.00,  68, "ROUND_NUMBER",     "Round-number transaction of £50,000 from Vantage Bank PLC. Pattern consistent with structuring.", "17:52"),
        alert(5, "Crestfield Group PLC","Vantage Bank PLC",     60000.00,  74, "RAPID_SUCCESSION", "Third high-value transfer from Crestfield Group within 30 minutes. Rapid succession pattern detected.", "17:48"),
        alert(6, "Meridian Bank PLC",   "Albion Bank PLC",      110000.00, 88, "HIGH_AMOUNT",      "£110,000 wire from Meridian Bank PLC. Account flagged for prior suspicious activity.", "17:44"),
        alert(7, "Vantage Bank PLC",    "Caledonian Bank",      95000.00,  82, "HIGH_RISK_ACCOUNT","Vantage Bank PLC is a high-risk-designated account. All transactions require secondary authorisation.", "17:40")
    );

    private static final Map<String, Object> STATS = Map.of(
        "totalTransactions",     247,
        "totalVolume",           4_823_450.00,
        "flaggedTransactions",   18,
        "fraudRate",             7.3,
        "successRate",           92.7,
        "completedTransactions", 229,
        "pendingTransactions",   12
    );

    private static final List<Map<String, Object>> ACCOUNTS = List.of(
        account(1, "Albion Bank PLC",      "GB39ALBN60161311935516", 5_000_000.00),
        account(2, "Meridian Bank PLC",    "GB82MRDNK0161332584066", 3_500_000.00),
        account(3, "Crestfield Group PLC", "GB76CRSF60161334761498", 2_750_000.00),
        account(4, "Harrington PLC",       "GB72HGTN60161339625862", 4_200_000.00),
        account(5, "Caledonian Bank",      "GB55CLDN60161338291764", 1_800_000.00),
        account(6, "Vantage Bank PLC",     "GB29VNTK60161331926819", 6_100_000.00)
    );

    @GetMapping("/payments")
    public List<Map<String, Object>> payments(@RequestParam(defaultValue = "50") int limit) {
        return TRANSACTIONS.stream().limit(limit).toList();
    }

    @GetMapping("/dashboard/stats")
    public Map<String, Object> stats() {
        return STATS;
    }

    @GetMapping("/dashboard/alerts")
    public List<Map<String, Object>> alerts(@RequestParam(defaultValue = "20") int limit) {
        return ALERTS.stream().limit(limit).toList();
    }

    @GetMapping("/accounts")
    public List<Map<String, Object>> accounts() {
        return ACCOUNTS;
    }

    @PostMapping("/payments")
    public Map<String, Object> submitPayment(@RequestBody Map<String, Object> payload) {
        int id = 25 + txnCounter.incrementAndGet();
        String ref = String.format("PF-20261017-%03d", id);

        int senderId   = Integer.parseInt(payload.get("senderAccountId").toString());
        int receiverId = Integer.parseInt(payload.get("receiverAccountId").toString());

        Map<String, Object> sender   = ACCOUNTS.stream().filter(a -> ((Number)a.get("id")).intValue() == senderId).findFirst().orElse(Map.of("name","Unknown","accountNumber","—"));
        Map<String, Object> receiver = ACCOUNTS.stream().filter(a -> ((Number)a.get("id")).intValue() == receiverId).findFirst().orElse(Map.of("name","Unknown","accountNumber","—"));

        double amount    = Double.parseDouble(payload.get("amount").toString());
        boolean flagged  = amount >= 50_000;
        int riskScore    = flagged ? 65 + (int)(amount % 25) : 10 + (int)(amount % 30);
        String status    = flagged ? "FLAGGED" : "PROCESSING";

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("reference", ref);
        m.put("senderName", sender.get("name"));
        m.put("senderAccountNumber", sender.get("accountNumber"));
        m.put("receiverName", receiver.get("name"));
        m.put("receiverAccountNumber", receiver.get("accountNumber"));
        m.put("amount", amount);
        m.put("currency", payload.getOrDefault("currency", "GBP"));
        m.put("status", status);
        m.put("riskScore", riskScore);
        m.put("timestamp", java.time.Instant.now().toString());
        return m;
    }

    private static Map<String, Object> txn(int id, String ref, String senderName, String senderAccNum,
                                            String receiverName, String receiverAccNum,
                                            double amount, String status, int riskScore, String time) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("reference", ref);
        m.put("senderName", senderName);
        m.put("senderAccountNumber", senderAccNum);
        m.put("receiverName", receiverName);
        m.put("receiverAccountNumber", receiverAccNum);
        m.put("amount", amount);
        m.put("currency", "GBP");
        m.put("status", status);
        m.put("riskScore", riskScore);
        m.put("timestamp", "2026-06-17T" + time + ":00");
        return Collections.unmodifiableMap(m);
    }

    private static Map<String, Object> alert(int id, String sender, String receiver,
                                              double amount, int riskScore, String type,
                                              String description, String time) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("senderName", sender);
        m.put("receiverName", receiver);
        m.put("amount", amount);
        m.put("riskScore", riskScore);
        m.put("alertType", type);
        m.put("description", description);
        m.put("timestamp", "2026-06-17T" + time + ":00");
        return Collections.unmodifiableMap(m);
    }

    private static Map<String, Object> account(int id, String name, String accountNumber, double balance) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("accountNumber", accountNumber);
        m.put("balance", balance);
        return Collections.unmodifiableMap(m);
    }
}
