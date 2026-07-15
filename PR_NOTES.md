# Pull Request: Fix DB2 Timestamp Compatibility and Clean Up Keystore Properties

This Pull Request contains fixes for DB2 database compatibility when handling date-time fields, and cleans up minor issues in the default configuration files.

## Summary of Changes

### 1. Database Compatibility (DB2 Timestamp support)
* **Problem**: DB2's JDBC driver (JCC) does not natively accept Java 8 `OffsetDateTime` parameters for standard `TIMESTAMP` columns, resulting in `SQLSTATE=22007` (Invalid datetime format) data conversion errors that aborted outbound transaction submissions and archival cleanups.
* **Solution**: Wrapped `OffsetDateTime` statement parameters with the existing `toTS()` utility helper in the following JDBC managers to convert them to `java.sql.Timestamp`:
  * **[OutboundTransactionManagerJdbc.java](file:///c:/workspace/phoss-ap/phoss-ap/phoss-ap-db/src/main/java/com/helger/phoss/ap/db/OutboundTransactionManagerJdbc.java)**:
    * Converted `aCreationTD` in `create`
    * Converted `aNextRetryDT` in `updateStatusAndRetry`
    * Converted `now()` in `updateStatusCompleted`
    * Converted `aMlsReceivedDT` in `updateMlsStatus`
  * **[ArchivalManagerJdbc.java](file:///c:/workspace/phoss-ap/phoss-ap/phoss-ap-db/src/main/java/com/helger/phoss/ap/db/ArchivalManagerJdbc.java)**:
    * Converted `aCutoff` in `cleanupOutbound` and `cleanupInbound`

### 2. Keystore Properties Cleanup
* **Problem**: Trailing spaces after the private key password value in the webapp configuration properties parsed the password incorrectly as `"peppol   "` instead of `"peppol"`, leading to key load failures.
* **Solution**: Removed three trailing spaces from the private password property in **[application.properties](file:///c:/workspace/phoss-ap/phoss-ap/phoss-ap-webapp/src/main/resources/application.properties)**:
  ```properties
  org.apache.wss4j.crypto.merlin.keystore.private.password=peppol
  ```

---

## Local Testing Configuration (Optional / Not for Production)
The following properties were added to `application.properties` for testing the outbound-to-inbound transaction loopback on `localhost`:
```properties
outbound.dev-loopback.enabled=true
phase4.endpoint.address=http://localhost:8080/as4
```
*(Note: These can be reverted before merging if loopback testing is not desired by default in the main branch).*
