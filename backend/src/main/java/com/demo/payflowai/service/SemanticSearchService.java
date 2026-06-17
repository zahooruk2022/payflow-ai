package com.demo.payflowai.service;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SemanticSearchService {

    private final VectorStore vectorStore;

    public SemanticSearchService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    void seedVectorStore() {
        vectorStore.add(List.of(
            doc("TXN-001", "Large corporate treasury settlement between Albion Bank and Vantage Bank, £250,000, scheduled end-of-quarter transfer", "COMPLETED", 12),
            doc("TXN-002", "Five rapid succession payments from Caledonian Bank totalling £95,000 within 60 seconds, flagged by automated fraud system", "FLAGGED", 95),
            doc("TXN-003", "Retail salary payment from Crestfield Group to individual account, £3,200, monthly payroll run", "COMPLETED", 5),
            doc("TXN-004", "Suspicious round-number transfer of £50,000 from new account at Harrington PLC, receiver account created 3 days ago", "FLAGGED", 78),
            doc("TXN-005", "Cross-border SWIFT payment to correspondent bank, £180,000, trade finance letter of credit settlement", "COMPLETED", 22),
            doc("TXN-006", "Late night payment at 02:14 UTC from Meridian Bank, £67,000, no prior activity from this account in 30 days", "FLAGGED", 82),
            doc("TXN-007", "Interbank liquidity adjustment between Albion Bank and Meridian Bank, £500,000, central bank clearing", "COMPLETED", 8),
            doc("TXN-008", "Pension fund disbursement from Vantage Bank, batch of retail transfers totalling £420,000 across 140 beneficiaries", "COMPLETED", 10),
            doc("TXN-009", "Duplicate payment attempt detected: identical amount £15,000 from same sender within 4 minutes", "FLAGGED", 70),
            doc("TXN-010", "Mortgage drawdown payment from Harrington PLC to conveyancing solicitor, £325,000, property transaction reference present", "COMPLETED", 15),
            doc("TXN-011", "Unusually high velocity: Caledonian Bank account submitted 8 payments in 90 seconds, amounts ranging £5,000 to £22,000", "FLAGGED", 88),
            doc("TXN-012", "Regular standing order from Crestfield Group, £8,500 monthly, consistent pattern for 14 months", "COMPLETED", 3),
            doc("TXN-013", "First-time large payment from Vantage Bank account, £75,000, account has no prior transaction history", "FLAGGED", 65),
            doc("TXN-014", "FX conversion payment, Albion Bank selling GBP for EUR, £95,000 equivalent at spot rate", "COMPLETED", 20),
            doc("TXN-015", "Emergency same-day CHAPS payment, Meridian Bank, £210,000, marked urgent by corporate client", "COMPLETED", 18),
            doc("TXN-016", "Split payment pattern: single £100,000 liability split into 5 × £20,000 payments sent 90 seconds apart from Harrington PLC", "FLAGGED", 91),
            doc("TXN-017", "Retail purchase refund from Crestfield Group back to customer, £850, chargeback resolution", "COMPLETED", 2),
            doc("TXN-018", "Investment settlement from Vantage Bank to custody account, £1,200,000, quarterly fund rebalancing", "COMPLETED", 25),
            doc("TXN-019", "Payment to recently-sanctioned entity account, Caledonian Bank, £35,000, compliance hold triggered", "FLAGGED", 99),
            doc("TXN-020", "Overnight batch settlement, Albion Bank, aggregated retail payments £672,000 across 203 transactions", "COMPLETED", 7)
        ));
    }

    private Document doc(String id, String description, String status, int riskScore) {
        return new Document(description, Map.of(
            "txnId", id,
            "status", status,
            "riskScore", String.valueOf(riskScore)
        ));
    }

    public List<SearchResult> search(String query, int topK) {
        return vectorStore.similaritySearch(
            SearchRequest.builder().query(query).topK(topK).build()
        ).stream()
         .map(doc -> new SearchResult(
             (String) doc.getMetadata().get("txnId"),
             doc.getText(),
             (String) doc.getMetadata().get("status"),
             Integer.parseInt((String) doc.getMetadata().get("riskScore")),
             doc.getScore() != null ? doc.getScore() : 0.0
         ))
         .toList();
    }

    public record SearchResult(String txnId, String description, String status, int riskScore, double similarity) {}
}
