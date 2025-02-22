/*
01001000 01100001 01110010 00100000 01001000 01100001 01110010 00100000 01001101 01100001 01101000 01100001 01100100 01100101 01110110
*/
import com.bidirectionalsetup.Config.Config;
import com.bidirectionalsetup.Models.CustomTrade;
import com.bidirectionalsetup.Models.Direction;
import com.bidirectionalsetup.Utils.AccessTokenUtils;
import com.bidirectionalsetup.Utils.TradeUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Margin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void liveTest(KiteConnect kiteSdk, TradeUtils tradeUtils)
            throws IOException, KiteException, InterruptedException {
        ObjectMapper mapper = new ObjectMapper();
        CustomTrade trade = new CustomTrade("SBIN", Direction.BULLISH, 0.0, 10000.0);
        tradeUtils.setLTPByStockName(trade);
        LOGGER.log(Level.INFO, "LTP: {0}", trade.ltp);
    }

    public static void main(String[] args) throws IOException, InterruptedException, KiteException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        ObjectMapper mapper = new ObjectMapper();
        KiteConnect kiteSdk = new KiteConnect(Config.apiKey);
        kiteSdk.setUserId(Config.userId);
        AccessTokenUtils accessTokenUtils = new AccessTokenUtils(kiteSdk);

        String accessToken = accessTokenUtils.getAccessToken();
        kiteSdk.setAccessToken(accessToken);

        TradeUtils tradeUtils = new TradeUtils(kiteSdk);
        boolean testFlag = Config.testFlag;

        if (!testFlag) {
            String stockName = tradeUtils.getStock();
            LOGGER.log(Level.INFO, "Trading in Stock: {0}", stockName);
            Direction direction = null;

            while (true) {
                LOGGER.info("Enter direction of the trade (6/9): ");
                int directionInt;
                try {
                    directionInt = Integer.parseInt(reader.readLine().trim());
                } catch (NumberFormatException e) {
                    LOGGER.warning("Invalid input. Please enter a valid number.");
                    continue;
                }

                if (directionInt == 6) {
                    direction = Direction.BULLISH;
                } else if (directionInt == 9) {
                    direction = Direction.BEARISH;
                } else {
                    LOGGER.warning("Enter a valid input (6 or 9).");
                    continue;
                }

                LOGGER.info("Do you want to start the trade? (Y/N): ");
                String confirmTrade = reader.readLine().trim().toUpperCase();
                if ("Y".equals(confirmTrade)) {
                    break;
                }
            }

            Margin capital = kiteSdk.getMargins().get("equity");
            Double tradeCapital = Config.tradeCapital;

            CustomTrade trade = new CustomTrade(stockName, direction, 0.0, tradeCapital);
            tradeUtils.setLTPByStockName(trade);

            while (!trade.successfulTrade && trade.tradeCount < 4) {
                LOGGER.log(Level.INFO, "{0}", mapper.writeValueAsString(trade));
                tradeUtils.executeStrategy(trade);
                Thread.sleep(1500);
            }

            if ((!tradeUtils.isTradeInTime() && trade.isTradeTaken) || trade.isTradeTaken) {
                if (trade.currentDirection.equals(Direction.BULLISH)) {
                    tradeUtils.sellAtMarketPriceMIS(trade.stockName, trade.quantity);
                } else {
                    tradeUtils.buyAtMarketPriceMIS(trade.stockName, trade.quantity);
                }
                trade.isTradeTaken = false;
                LOGGER.warning("Closed trade due to timeout...");
            }
        } else {
            liveTest(kiteSdk, tradeUtils);
        }
    }
}

