/*Har Har Mahadev !!*/
import com.bidirectionalsetup.Config.Config;
import com.bidirectionalsetup.Models.CustomTrade;
import com.bidirectionalsetup.Models.Direction;
import com.bidirectionalsetup.Utils.AccessTokenUtils;
import com.bidirectionalsetup.Utils.TradeUtils;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Margin;

import java.io.IOException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException, KiteException {
        Scanner scanner = new Scanner(System.in);
        KiteConnect kiteSdk = new KiteConnect(Config.apiKey);
        kiteSdk.setUserId(Config.userId);
        AccessTokenUtils accessTokenUtils = new AccessTokenUtils(kiteSdk);

        String accessToken = accessTokenUtils.getAccessToken();
        kiteSdk.setAccessToken(accessToken);

        TradeUtils tradeUtils = new TradeUtils(kiteSdk);
        String stockName = tradeUtils.getStock();
        System.out.println("Trading in Stock : " + stockName);

        boolean isTradeOpen = false;

        Direction direction;
        while (true) {
            System.out.print("Enter direction of the trade (6/9): ");
            int directionInt = scanner.nextInt();


            if (directionInt == 6) {
                direction = Direction.BULLISH;
            } else if (directionInt == 9) {
                direction = Direction.BEARISH;
            } else {
                System.out.println("Enter valid input : ");
                continue;
            }

            System.out.print("Do you want to start the trade? (Y/N): ");
            String confirmTrade = scanner.next();

            if (confirmTrade.toUpperCase().equals("Y")) {
                isTradeOpen = true;
                break;
            }
        }

        Margin capital = kiteSdk.getMargins().get("equity");
        String capitalAvailable = capital.available.cash;

        String[] instruments = {stockName};
        double ltp = kiteSdk.getLTP(instruments).get(stockName).lastPrice;
        int quantity = (int) Math.ceil(Integer.parseInt(capitalAvailable) / ltp);


        CustomTrade trade = new CustomTrade();
        trade.isTradeTaken = false;
        trade.currentDirection = direction;
        trade.tradeCount = 1;
        trade.stockName = stockName;
        trade.quantity = quantity;
        trade.successfulTrade = false;

        while(!trade.successfulTrade && trade.tradeCount < 4 && tradeUtils.isTradeInTime()) {
            tradeUtils.executeStrategy(trade);
        }

        if(!tradeUtils.isTradeInTime() && trade.isTradeTaken){
            if(trade.currentDirection.equals(Direction.BULLISH)){
                tradeUtils.sellAtMarketPriceMIS(trade.stockName, trade.quantity);
                trade.isTradeTaken = false;
            } else {
                tradeUtils.buyAtMarketPriceMIS(trade.stockName, trade.quantity);
                trade.isTradeTaken = false;
            }
            System.out.println("Closed trade due to timeout.....");
        }

    }
}
