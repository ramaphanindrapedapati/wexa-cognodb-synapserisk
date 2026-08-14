package com.wexa.synapserisk.service;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

@Service
public class DataSeederService implements CommandLineRunner {

    private final Driver driver;

    @Value("${app.seed.on-startup:true}")
    private boolean seedOnStartup;

    public DataSeederService(Driver driver) {
        this.driver = driver;
    }

    @Override
    public void run(String... args) {
        if (seedOnStartup) {
            seedDatabase();
        }
    }

    public void seedDatabase() {
        try (Session session = driver.session()) {
            System.out.println("🌱 Seeding CognoDB Graph Database via Java Driver...");
            session.run("MATCH (n) DETACH DELETE n");

            String seedQuery = """
                CREATE (a1:Account {id: "ACC-101", holderName: "Alice Miller", riskScore: 12, balance: 14500.0, status: "ACTIVE"})
                CREATE (a2:Account {id: "ACC-102", holderName: "Bob Vance", riskScore: 88, balance: 250.0, status: "SUSPICIOUS"})
                CREATE (a3:Account {id: "ACC-103", holderName: "Charlie Delta", riskScore: 92, balance: 80.0, status: "FLAGGED"})
                CREATE (a4:Account {id: "ACC-104", holderName: "David Echo", riskScore: 85, balance: 120.0, status: "FLAGGED"})
                CREATE (a5:Account {id: "ACC-105", holderName: "Elena Rostova", riskScore: 95, balance: 50.0, status: "FLAGGED"})
                CREATE (a6:Account {id: "ACC-106", holderName: "Frank Wright", riskScore: 5, balance: 45000.0, status: "ACTIVE"})
                CREATE (a7:Account {id: "ACC-107", holderName: "Grace Hopper", riskScore: 18, balance: 8200.0, status: "ACTIVE"})

                CREATE (d1:Device {fingerprint: "DEV-MAC-88921", os: "Linux", type: "VirtualMachine"})
                CREATE (d2:Device {fingerprint: "DEV-IOS-11029", os: "iOS", type: "Mobile"})
                CREATE (ip1:IPAddress {address: "185.220.101.5", isp: "Tor Exit Node", country: "NL"})
                CREATE (ip2:IPAddress {address: "192.0.2.45", isp: "Comcast Business", country: "US"})

                CREATE (a2)-[:USED_DEVICE {lastSeen: "2026-03-01T10:00:00Z"}]->(d1)
                CREATE (a3)-[:USED_DEVICE {lastSeen: "2026-03-01T10:15:00Z"}]->(d1)
                CREATE (a4)-[:USED_DEVICE {lastSeen: "2026-03-01T10:30:00Z"}]->(d1)
                CREATE (a5)-[:USED_DEVICE {lastSeen: "2026-03-01T11:00:00Z"}]->(d1)

                CREATE (a2)-[:LOGGED_IN_FROM {loginCount: 34}]->(ip1)
                CREATE (a3)-[:LOGGED_IN_FROM {loginCount: 22}]->(ip1)
                CREATE (a4)-[:LOGGED_IN_FROM {loginCount: 19}]->(ip1)
                CREATE (a5)-[:LOGGED_IN_FROM {loginCount: 45}]->(ip1)

                CREATE (a1)-[:LOGGED_IN_FROM {loginCount: 120}]->(ip2)
                CREATE (a6)-[:USED_DEVICE {lastSeen: "2026-03-02T08:00:00Z"}]->(d2)

                CREATE (a2)-[:TRANSFERRED_TO {amount: 9800, timestamp: "2026-03-01T12:00:00Z", txnId: "TX-901"}]->(a3)
                CREATE (a3)-[:TRANSFERRED_TO {amount: 9600, timestamp: "2026-03-01T12:15:00Z", txnId: "TX-902"}]->(a4)
                CREATE (a4)-[:TRANSFERRED_TO {amount: 9400, timestamp: "2026-03-01T12:30:00Z", txnId: "TX-903"}]->(a5)
                CREATE (a5)-[:TRANSFERRED_TO {amount: 9200, timestamp: "2026-03-01T12:45:00Z", txnId: "TX-904"}]->(a2)

                CREATE (a6)-[:TRANSFERRED_TO {amount: 450, timestamp: "2026-02-28T09:00:00Z", txnId: "TX-101"}]->(a7)
                CREATE (a1)-[:TRANSFERRED_TO {amount: 1200, timestamp: "2026-02-28T14:30:00Z", txnId: "TX-102"}]->(a7)
            """;

            session.run(seedQuery);
            System.out.println("✅ CognoDB graph database successfully seeded!");
        } catch (Exception e) {
            System.err.println("❌ Seeding Error: " + e.getMessage());
        }
    }
}
