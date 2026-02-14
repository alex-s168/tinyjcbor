# tinyjcbor
Small Java 21 CBOR decoder & encoder.

Features:
- Supports standard CBOR
- Serialize/Deserialize interface

## Usage
```kt
maven {
    name = "vxcc"
    url = uri("https://maven.vxcc.dev/libs")
}

dependencies {
    implementation("dev.vxcc:tinyjcbor:1.0.0-rc.2")
}
```

```xml
<distributionManagement>
    <repository>
        <id>vxcc</id>
        <url>https://maven.vxcc.dev/libs</url>
    </repository>
</distributionManagement>

<dependencies>
    <dependency>
      <groupId>dev.vxcc</groupId>
      <artifactId>tinyjcbor</artifactId>
      <version>1.0.0-rc.2</version>
    </dependency>
</dependencies>
```
