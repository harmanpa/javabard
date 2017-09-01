# javabard
Extensions to https://github.com/square/javapoet

To use from Maven, add the following to pom.xml:

```
    <repositories>
        <repository>
            <id>javabard-mvn-repo</id>
            <url>https://raw.github.com/harmanpa/javabard/mvn-repo/</url>
            <snapshots>
                <enabled>true</enabled>
                <updatePolicy>always</updatePolicy>
            </snapshots>
        </repository>
    </repositories>
```

and

```
    <dependency>
            <groupId>tech.cae</groupId>
            <artifactId>javabard</artifactId>
            <version>1.0-SNAPSHOT</version>
    </dependency>
```
