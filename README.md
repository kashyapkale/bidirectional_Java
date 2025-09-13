## Bidirectional Trading (Java)

Interactive console tool that places directional intraday equity orders on NSE using Zerodha KiteConnect. It supports a simple bidirectional strategy with a TOTP helper and an optional live-test mode to only fetch LTP.

### Features
- **KiteConnect integration**: Login via request token, session generation, order placement
- **TOTP helper**: Continuously displays current TOTP to aid 2FA login
- **Simple strategy loop**: Enter direction (bullish/bearish), place market order, manage target/SL, flip on SL
- **LTP utility**: Retrieve and print latest traded price for a symbol

## Prerequisites
- **JDK 8+** (project compiles with Java 8)
- **Maven 3.6+**
- Zerodha **KiteConnect API key/secret**, **userId**, and your **TOTP secret** (Base32)

## Project Structure
- `src/main/java/Main.java`: App entrypoint
- `com/bidirectionalsetup/Utils/*`: Access token flow, TOTP, trading helpers, text colors
- `com/bidirectionalsetup/Models/*`: `CustomTrade` and `Direction`
- `pom.xml`: Maven dependencies (Jackson, OkHttp, TOTP, KiteConnect, Lombok)

## Configuration
Create `src/main/java/com/bidirectionalsetup/Config/Config.java` with your credentials and runtime flags.

```java
package com.bidirectionalsetup.Config;

public class Config {
    public static final String apiKey = "your_kite_api_key";         // e.g., "kite123ab"
    public static final String apiSecret = "your_kite_api_secret";   // keep private
    public static final String userId = "your_kite_user_id";         // e.g., "AB1234"
    public static final String totpSecret = "BASE32_TOTP_SECRET";    // from your authenticator setup

    // Set true to run lightweight LTP test (no orders placed)
    public static final boolean testFlag = false;

    // Capital used to size order quantity (very simplified)
    public static final double tradeCapital = 10000.0;
}
```

Notes:
- `totpSecret` is the Base32 secret used by your authenticator app. Do not commit real credentials.
- The console prints a fresh TOTP every ~2s to help with 2FA during login.

## Login flow (KiteConnect)
1. On start, the app prints a Kite login URL.
2. Open the URL in a browser, enter username/password.
3. Use the console TOTP shown by the app to complete 2FA.
4. After login, you are redirected with a `request_token` in the URL. Copy it.
5. Paste the `request_token` back into the console when prompted. The app exchanges it for an access token and proceeds.

## Build
```bash
mvn -q clean package
```

## Run
Run via the Maven Exec plugin (no pom changes needed):

```bash
mvn -q org.codehaus.mojo:exec-maven-plugin:3.5.0:java -Dexec.mainClass=Main
```

### First run (interactive)
- If `testFlag` is `false`:
  - Enter the stock symbol (e.g., `SBIN`).
  - Enter direction with digits: `6` for BULLISH, `9` for BEARISH.
  - Confirm to start the trade. The app places a market MIS order and manages target/SL.
- If `testFlag` is `true`:
  - The app runs a lightweight LTP check for a sample trade and exits.

### Trading window safeguard
`TradeUtils.isTradeInTime()` only allows trading before `11:30` (local time). After that, any open trade is squared off.

## Key components
- `Main.java`: Orchestrates login, prompts, and the strategy loop
- `Utils/AccessTokenUtils.java`: TOTP helper and session generation from `request_token`
- `Utils/TradeUtils.java`: Market MIS buy/sell, LTP fetch, target/SL handling, order-state checks
- `Models/CustomTrade.java`: In-memory trade state (direction, entry threshold, quantity sizing, TP/SL calc)

## Dependencies (from `pom.xml`)
- `com.zerodhatech.kiteconnect:kiteconnect:3.3.2`
- `com.squareup.okhttp3:okhttp:4.10.0`
- `com.fasterxml.jackson.core:jackson-databind:2.13.4.2`
- `dev.samstevens.totp:totp:1.7.1`, `de.taimos:totp:1.0`
- `commons-codec:commons-codec:1.10`
- `org.projectlombok:lombok:1.18.20`

A `kiteconnect.jar` exists in the repo root, but the dependency is already provided via Maven; the local JAR is not required to run.

## Tips
- If Maven cannot find `Main`, ensure it is at `src/main/java/Main.java` and uses `public class Main` with no package.
- If TOTP seems off, verify `totpSecret` and system time sync.
- Use plain NSE symbols (e.g., `SBIN`) when prompted; the code prepends `NSE:` internally for LTP.

## Disclaimer
This project can place live market orders. Use small capital and test with `testFlag = true` first. You are responsible for API usage and any financial losses. Educational use only.
