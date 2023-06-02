## Install Dependencies for running GGDMiner

1. WS4J and JAWJAW (https://github.com/mhjabreel/ws4j/tree/master/edu.cmu.lti.ws4j)
2. Download the jar files and install to your local repository with Maven
3. mvn install:install-file -Dfile=/path/lib/jawjaw-0.1.0.jar -DgroupId=cmu.edu.lti -DartifactId=jawjaw -Dversion=0.1 -Dpackaging=jar

4. mvn install:install-file -Dfile=/path/lib/ws4j-1.0.2-jar-with-dependencies.jar -DgroupId=cmu.edu.lti -DartifactId=ws4j -Dversion=1.0 -Dpackaging=jar
5. Change the repository configuration in pom.xml to your local repository
```
        <repository>
            <id>local-repo</id>
            <name>Local-Repo</name>
            <url>/path/to/local/rep/</url>
        </repository>
```

## Build the GGDMiner Project

1. run ```mvn install```
2. run ```java -jar target/ggdsminer-1.0-jar-with-dependencies.jar /path/for/config/graph/json /path/for/config/miner/json path/for/output```

---

## Config Graph

1. Information about where the graph is stored and its schema --> see configgraph_example.json

---

## Config Miner

1. Parameters for the miner --> see confiminer_example.json
