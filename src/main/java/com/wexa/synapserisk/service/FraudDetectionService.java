package com.wexa.synapserisk.service;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FraudDetectionService {

    private final Driver driver;

    public FraudDetectionService(Driver driver) {
        this.driver = driver;
    }

    public Map<String, Object> checkHealth() {
        try (Session session = driver.session()) {
            session.run("RETURN 1 AS ping");
            return Map.of("status", "HEALTHY", "database", "CognoDB Connected");
        } catch (Exception e) {
            return Map.of("status", "UNHEALTHY", "error", e.getMessage());
        }
    }

    public List<Map<String, Object>> getAllAccounts() {
        List<Map<String, Object>> accounts = new ArrayList<>();
        try (Session session = driver.session()) {
            String cypher = "MATCH (a:Account) RETURN a.id AS id, a.holderName AS name, a.riskScore AS riskScore, a.balance AS balance, a.status AS status ORDER BY a.riskScore DESC";
            Result result = session.run(cypher);
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> acc = new HashMap<>();
                acc.put("id", record.get("id").asString());
                acc.put("name", record.get("name").asString());
                acc.put("riskScore", record.get("riskScore").asInt());
                acc.put("balance", record.get("balance").asDouble());
                acc.put("status", record.get("status").asString());
                accounts.add(acc);
            }
        }
        return accounts;
    }

    public List<Map<String, Object>> traceMultiHopPaths(String accountId) {
        List<Map<String, Object>> paths = new ArrayList<>();
        try (Session session = driver.session()) {
            String cypher = """
                MATCH path = (start:Account {id: $accountId})-[:TRANSFERRED_TO*1..4]->(target:Account)
                WHERE start <> target
                RETURN 
                  [n in nodes(path) | {id: n.id, name: n.holderName, risk: n.riskScore}] AS nodeChain,
                  [r in relationships(path) | {amount: r.amount, txnId: r.txnId}] AS transfers,
                  length(path) AS hops
                ORDER BY hops DESC
            """;
            Result result = session.run(cypher, Values.parameters("accountId", accountId));
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> map = new HashMap<>();
                map.put("nodeChain", record.get("nodeChain").asList());
                map.put("transfers", record.get("transfers").asList());
                map.put("hops", record.get("hops").asInt());
                paths.add(map);
            }
        }
        return paths;
    }

    public List<Map<String, Object>> detectFraudRings() {
        List<Map<String, Object>> rings = new ArrayList<>();
        try (Session session = driver.session()) {
            String cypher = """
                MATCH path = (a:Account)-[:TRANSFERRED_TO*2..5]->(a:Account)
                RETURN 
                  [n in nodes(path) | n.id] AS ringAccountIds,
                  [n in nodes(path) | n.holderName] AS ringHolders,
                  length(path) AS cycleLength
            """;
            Result result = session.run(cypher);
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> map = new HashMap<>();
                map.put("accounts", record.get("ringAccountIds").asList());
                map.put("holders", record.get("ringHolders").asList());
                map.put("cycleLength", record.get("cycleLength").asInt());
                rings.add(map);
            }
        }
        return rings;
    }

    public List<Map<String, Object>> detectSharedInfrastructure() {
        List<Map<String, Object>> infra = new ArrayList<>();
        try (Session session = driver.session()) {
            String cypher = """
                MATCH (a1:Account)-[r1]->(resource)<-[r2]-(a2:Account)
                WHERE id(a1) < id(a2) AND (resource:Device OR resource:IPAddress)
                RETURN 
                  labels(resource)[0] AS resourceType,
                  coalesce(resource.fingerprint, resource.address) AS resourceValue,
                  collect(DISTINCT a1.id) + collect(DISTINCT a2.id) AS linkedAccounts
            """;
            Result result = session.run(cypher);
            while (result.hasNext()) {
                Record record = result.next();
                Set<String> uniqueAccounts = new HashSet<>(record.get("linkedAccounts").asList(v -> v.asString()));
                Map<String, Object> map = new HashMap<>();
                map.put("type", record.get("resourceType").asString());
                map.put("identifier", record.get("resourceValue").asString());
                map.put("linkedAccounts", uniqueAccounts);
                infra.add(map);
            }
        }
        return infra;
    }
}
