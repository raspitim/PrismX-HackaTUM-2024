# Queries
The Cypher Queries for the given Neo3j Knowledge Graph Database.

## Possible Weaknesses associated with Docker incident

```SQL
OPTIONAL MATCH 
  (d:SoftwareInstallation {product: "Docker Engine"})
WHERE 
  toFloat(d.version) >= 23.0 AND toFloat(d.version) <= 26.1

OPTIONAL MATCH 
  (d_cli:SoftwareInstallation {product: "Docker CLI"})
WHERE 
  toFloat(d_cli.version) >= 23.0 AND toFloat(d_cli.version) <= 26.1

OPTIONAL MATCH 
  (d_desktop:SoftwareInstallation {product: "Docker Desktop"})
WHERE 
  d_desktop.version >= "4.10.1" AND d_desktop.version <= "4.34.2"
MATCH (s:System)-[r]->(:SoftwareInstallation)
WHERE (s)-[]->(d) OR (s)-[]->(d_cli) OR (s)-[]->(d_desktop)
MATCH (s)-[:related_weakness]->(w:Weakness)

RETURN DISTINCT w.title AS Title, w.severity AS Severity, w.status AS Status ORDER BY Severity
```

## Historical Incidents

### EDSS Scores

```SQL
MATCH (i:Incident)-[:related_subject]->(s:System)
OPTIONAL MATCH (s)<-[:runs_on]-(a:Application)
OPTIONAL MATCH (i)-[:related_cve]->(w:Weakness)
OPTIONAL MATCH (rating:EPSS)<-[:has_rating]-(w)
RETURN 
    split(i.friendly_name, " ")[1] AS Incident,
    round(AVG(rating.score)*100) AS Score
```

### Statistics

```SQL
MATCH (i:Incident)-[:`related_subject`]->(s:System)
OPTIONAL MATCH (s)<-[:`runs_on`]-(a:Application)
OPTIONAL MATCH (i)-[:`related_cve`]->(w:Weakness)

RETURN 
    split(i.friendly_name, " ")[1] AS Incident,
    COUNT(DISTINCT s) as Systems,
    COUNT(DISTINCT w) as Weaknesses
```

## Affected Assets Statistics

```SQL
MATCH 
  (d:SoftwareInstallation {product: "Docker Engine"})
WHERE 
  toFloat(d.version) >= 23.0 AND toFloat(d.version) <= 26.1

OPTIONAL MATCH 
  (d_cli:SoftwareInstallation {product: "Docker CLI"})
WHERE 
  toFloat(d_cli.version) >= 23.0 AND toFloat(d_cli.version) <= 26.1

OPTIONAL MATCH 
  (d_desktop:SoftwareInstallation {product: "Docker Desktop"})
WHERE 
  d_desktop.version >= "4.10.1" AND d_desktop.version <= "4.34.2"

OPTIONAL MATCH 
  (s:System)-[r:`related_software`]->(:SoftwareInstallation) //könnte man safe optimieren
WHERE (s)-[]->(d) OR (s)-[]->(d_cli) OR (s)-[]->(d_desktop)

RETURN
  COUNT(DISTINCT CASE WHEN s.critical = 1 AND s.state = "Active" THEN s END) AS Critical_Active,
  COUNT(DISTINCT CASE WHEN s.critical = 1 AND s.state = "Inconsistent" THEN s END) AS Critical_Inconsistent,
  COUNT(DISTINCT CASE WHEN s.critical = 0 AND s.state = "Active" THEN s END) AS Noncritical_Active,
  COUNT(DISTINCT CASE WHEN s.critical = 0 AND s.state = "Inconsistent" THEN s END) AS Noncritical_Inconsistent
```

## Docker Incident Statistics

```SQL
OPTIONAL MATCH 
  (d:SoftwareInstallation {product: "Docker Engine"})
WHERE 
  toFloat(d.version) >= 23.0 AND toFloat(d.version) <= 26.1

OPTIONAL MATCH 
  (d_cli:SoftwareInstallation {product: "Docker CLI"})
WHERE 
  toFloat(d_cli.version) >= 23.0 AND toFloat(d_cli.version) <= 26.1

OPTIONAL MATCH 
  (d_desktop:SoftwareInstallation {product: "Docker Desktop"})
WHERE 
  d_desktop.version >= "4.10.1" AND d_desktop.version <= "4.34.2"
MATCH (s:System)-[r]->(:SoftwareInstallation)
WHERE (s)-[]->(d) OR (s)-[]->(d_cli) OR (s)-[]->(d_desktop)
OPTIONAL MATCH (s)-[:related_weakness]->(w:Weakness)

RETURN "Docker" as Docker, COUNT(DISTINCT s) as Systems, COUNT(DISTINCT w) as Weaknesses
```

## List affected Assets

```SQL
OPTIONAL MATCH 
  (d:SoftwareInstallation {product: "Docker Engine"})
WHERE 
  toFloat(d.version) >= 23.0 AND toFloat(d.version) <= 26.1

OPTIONAL MATCH 
  (d_cli:SoftwareInstallation {product: "Docker CLI"})
WHERE 
  toFloat(d_cli.version) >= 23.0 AND toFloat(d_cli.version) <= 26.1

OPTIONAL MATCH 
  (d_desktop:SoftwareInstallation {product: "Docker Desktop"})
WHERE 
  d_desktop.version >= "4.10.1" AND d_desktop.version <= "4.34.2"
MATCH (s:System)-[r]->(:SoftwareInstallation)
WHERE (s)-[]->(d) OR (s)-[]->(d_cli) OR (s)-[]->(d_desktop)

RETURN DISTINCT s.key as Key, s.state as State, s.critical as Critical, s.provider_name as Providername, s.type as Type, s.sub_type as Subtype ORDER BY Critical DESC, State DESC

```
