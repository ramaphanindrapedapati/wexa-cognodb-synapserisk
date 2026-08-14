# SynapseRisk — Graph-Powered Financial Fraud Ring & Mule Account Detector

SynapseRisk is an end-to-end financial crime investigation platform built with **Java 17**, **Spring Boot 3**, and **CognoDB** using the official **Neo4j Java Bolt Driver** (Bolt 5.0+ protocol).

---

## 1. Use Case & "Why a Graph Database?"

### The Problem
Financial criminals and money-laundering syndicates evade traditional transaction monitoring by dispersing stolen capital across multiple intermediary "money mule" accounts, fabricating synthetic identities from shared hardware/IPs, and routing funds through circular paths (*round-tripping*).

### Why Relational Databases (SQL) Fail
* **Exponential Join Complexity:** Finding a 3- to 5-hop mule chain ($A \to B \to C \to D$) requires 4 recursive self-joins on a multi-million-row `transactions` table. Performance degrades exponentially ($O(n^k)$) as path depth increases.
* **Rigid Schema Bottlenecks:** Modeling heterogeneous entities (bank accounts, virtual machines, Tor exit nodes) linked by dynamic relationships requires cluttered foreign key mapping tables and fragile recursive CTEs.

### Why CognoDB (Graph Database) Wins
* **Index-Free Adjacency:** Nodes directly reference adjacent nodes in memory via physical pointers. Multi-hop path traversals execute in constant time ($O(1)$) per hop, regardless of total graph size.
* **Intuitive Pattern Matching:** Complex structural patterns—such as directed transaction cycles ($A \to B \to C \to A$) and shared infrastructure collusion—are expressed in clean, declarative openCypher queries.

---

## 2. Graph Data Model

[:USED_DEVICE]
            ┌────────────────────────────────────┐
            │  - lastSeen: String (ISO 8601)     │
            ▼                                    │
    ┌───────────────┐                            │
    │    Device     │                            │
    ├───────────────┤                            │
    │  fingerprint  │                            │
    │  os           │                            │
    │  type         │                            │
    └───────────────┘                            │
                                                 │
    ┌───────────────┐                            │
    │   IPAddress   │                            │
    ├───────────────┤                            │
    │  address      │                            │
    │  isp          │                            │
    │  country      │                            │
    └───────────────┘                            │
            ▲                                    │
            │                                    │
            └────────────────────┐               │
           [:LOGGED_IN_FROM]     │               │
            - loginCount: Int    │               │
                                 │               │
                          ┌──────────────┐       │
                          │   Account    │───────┘
                          ├──────────────┤
                          │  id          │
                          │  holderName  │
                          │  riskScore   │
                          │  balance     │
                          │  status      │
                          └──────┬───────┘
                                 │
                 [:TRANSFERRED_TO]
                 - amount: Float
                 - timestamp: String
                 - txnId: String
                                 │
                                 ▼
                          ┌──────────────┐
                          │   Account    │
                          └──────────────┘

### Nodes
* **`Account`**: `id` (String), `holderName` (String), `riskScore` (Integer), `balance` (Float), `status` (String)
* **`Device`**: `fingerprint` (String), `os` (String), `type` (String)
* **`IPAddress`**: `address` (String), `isp` (String), `country` (String)

### Relationships
* **`TRANSFERRED_TO`**: Connects `Account` -> `Account` (Properties: `amount`, `timestamp`, `txnId`)
* **`USED_DEVICE`**: Connects `Account` -> `Device` (Properties: `lastSeen`)
* **`LOGGED_IN_FROM`**: Connects `Account` -> `IPAddress` (Properties: `loginCount`)

---

## 3. Core Cypher Queries Explained

### Query 1: Multi-Hop Money Mule Traversal (1 to 4 Hops)
Tracks downstream fund flow chains originating from a specific target account to detect layered disbursement networks.
```cypher
MATCH path = (start:Account {id: $accountId})-[:TRANSFERRED_TO*1..4]->(target:Account)
WHERE start <> target
RETURN 
  [n in nodes(path) | {id: n.id, name: n.holderName, risk: n.riskScore}] AS nodeChain,
  [r in relationships(path) | {amount: r.amount, txnId: r.txnId}] AS transfers,
  length(path) AS hops
ORDER BY hops DESC
Query 2: Circular Money Laundering Ring Detection (Graph Cycles)
Detects round-tripping fund cycles where money circles through intermediary accounts and returns to the originator.

Cypher
MATCH path = (a:Account)-[:TRANSFERRED_TO*2..5]->(a:Account)
RETURN 
  [n in nodes(path) | n.id] AS ringAccountIds,
  [n in nodes(path) | n.holderName] AS ringHolders,
  length(path) AS cycleLength
Query 3: Shared Infrastructure Collusion (Synthetic Identity Detection)
Uncovers collusive accounts operating from identical device fingerprints (VMs) or Tor network nodes.

Cypher
MATCH (a1:Account)-[r1]->(resource)<-[r2]-(a2:Account)
WHERE id(a1) < id(a2) AND (resource:Device OR resource:IPAddress)
RETURN 
  labels(resource)[0] AS resourceType,
  coalesce(resource.fingerprint, resource.address) AS resourceValue,
  collect(DISTINCT a1.id) + collect(DISTINCT a2.id) AS linkedAccounts
4. Setup & Running Instructions
Prerequisites
Java 17 or higher

Maven 3.8+

A free CognoDB Cloud instance (from https://console.cognodb.com)

1. Configure CognoDB Connection
Update src/main/resources/application.properties with your instance credentials:

Properties
server.port=8080
cognodb.uri=bolt+s://db-42acf968.databases.cognodb.com
cognodb.username=cognodb
cognodb.password=a342b03396556430aabb2faad351b2f9
app.seed.on-startup=true
2. Build & Run Application
Bash
mvn clean spring-boot:run
3. Access Dashboard
Open your web browser and navigate to:
http://localhost:8080
