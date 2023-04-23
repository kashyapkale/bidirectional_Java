package com.bidirectionalsetup.Utils;

import com.bidirectionalsetup.Models.CustomTrade;
import com.bidirectionalsetup.Models.Direction;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.kiteconnect.utils.Constants;
import com.zerodhatech.models.Order;
import com.zerodhatech.models.OrderParams;
import com.zerodhatech.models.Trade;


import java.io.IOException;
import java.time.LocalTime;
import java.util.Scanner;

public class TradeUtils {
    private final KiteConnect kiteSdk;

    public TradeUtils(KiteConnect kiteSdk) {
        this.kiteSdk = kiteSdk;
    }

    public String getStock() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter Stock Name : ");
        return scanner.nextLine().toUpperCase();
    }

    public Order buyAtMarketPriceMIS(String stockName, Integer quantity) throws IOException, KiteException {
        OrderParams order = createOrder(quantity, stockName, true);
        return kiteSdk.placeOrder(order, Constants.VARIETY_REGULAR);
    }

    public Order sellAtMarketPriceMIS(String stockName, Integer quantity) throws IOException, KiteException {
        OrderParams order = createOrder(quantity, stockName, false);
        return kiteSdk.placeOrder(order, Constants.VARIETY_REGULAR);
    }

    public Double getLTPByStockName(String stockName) throws IOException, KiteException {
        String[] instruments = {stockName};
        return kiteSdk.getLTP(instruments).get(stockName).lastPrice;
    }

    public static OrderParams createOrder(int quantity, String stockName, boolean buyFlag) {
        OrderParams order = new OrderParams();
        order.exchange = Constants.EXCHANGE_NSE;
        order.quantity = quantity;
        order.orderType = Constants.ORDER_TYPE_MARKET;
        order.product = Constants.PRODUCT_MIS;
        order.validity = Constants.VALIDITY_DAY;
        order.disclosedQuantity = 0;
        order.triggerPrice = Double.valueOf(0);
        order.tag = "my_order";
        order.tradingsymbol = stockName;

        if (buyFlag) {
            order.transactionType = Constants.TRANSACTION_TYPE_BUY;
        } else {
            order.transactionType = Constants.TRANSACTION_TYPE_SELL;
        }

        System.out.println("-----------------------Order Created---------------");
        System.out.println(order);
        System.out.println("---------------------------------------------------");

        return order;
    }

    public Order getOrderHistory(String orderId) throws IOException, KiteException {
        /*ReVisit*/
        return kiteSdk.getOrderHistory(orderId).get(0);
    }

    public void executeStrategy(CustomTrade trade)
            throws IOException, KiteException, InterruptedException, NumberFormatException {
        if(!trade.isTradeTaken){
            Order newOrder = null;
            if(trade.currentDirection.equals(Direction.BULLISH)) {
                newOrder = buyAtMarketPriceMIS(trade.stockName,trade.quantity);
            } else {
                newOrder = sellAtMarketPriceMIS(trade.stockName,trade.quantity);
            }

            while(! getOrderHistory(newOrder.orderId).status.equals("COMPLETE")) {
                Thread.sleep(2000);
                System.out.println("Waiting for order completion....");
            }

            Order completedOrder = getOrderHistory(newOrder.orderId);
            //trade.tradeCount++;
            trade.isTradeTaken = true;
            /*ReVisit*/
            trade.threshold = Double.parseDouble(completedOrder.triggerPrice);
            trade.lastExecutedOrder = completedOrder;
        } else {
            Double ltp = getLTPByStockName(trade.stockName);
            if(trade.currentDirection.equals(Direction.BULLISH)) {

                if(ltp >= trade.getTargetPrice()){
                    Order squareOffOrder = sellAtMarketPriceMIS(trade.stockName,trade.quantity);
                    trade.successfulTrade = true;
                    trade.isTradeTaken = false;
                    trade.lastExecutedOrder = squareOffOrder;
                    TextUtils.printInGreen("Target Hit !! ");
                } else if (ltp <= trade.getSLPrice()){
                    Order squareOffOrder = sellAtMarketPriceMIS(trade.stockName,trade.quantity);
                    while(! getOrderHistory(squareOffOrder.orderId).status.equals("COMPLETE")) {
                        Thread.sleep(1000);
                        System.out.println("Waiting for order completion....");
                    }
                    trade.isTradeTaken = false;
                    trade.lastExecutedOrder = squareOffOrder;
                    trade.tradeCount++;
                    trade.currentDirection = Direction.BEARISH;
                    TextUtils.printInRed("SL Hit @ "+ getOrderHistory(squareOffOrder.orderId).triggerPrice);
                } else {
                    System.out.println("In the trade, LTP :: " + ltp);
                }

            } else {

                if(ltp <= trade.getTargetPrice()){
                    Order squareOffOrder = buyAtMarketPriceMIS(trade.stockName,trade.quantity);
                    trade.successfulTrade = true;
                    trade.isTradeTaken = false;
                    trade.lastExecutedOrder = squareOffOrder;
                    TextUtils.printInGreen("Target Hit !! ");
                } else if (ltp >= trade.getSLPrice()){
                    Order squareOffOrder = buyAtMarketPriceMIS(trade.stockName,trade.quantity);
                    while(! getOrderHistory(squareOffOrder.orderId).status.equals("COMPLETE")) {
                        Thread.sleep(1000);
                        System.out.println("Waiting for order completion....");
                    }
                    trade.isTradeTaken = false;
                    trade.lastExecutedOrder = squareOffOrder;
                    trade.tradeCount++;
                    trade.currentDirection = Direction.BULLISH;
                    TextUtils.printInRed("SL Hit @ "+ getOrderHistory(squareOffOrder.orderId).triggerPrice);
                } else {
                    System.out.println("In the trade, LTP :: " + ltp);
                }
            }
        }
    }

    public Boolean isTradeInTime(){
        LocalTime currentTime = LocalTime.now();
        LocalTime cutoffTime = LocalTime.parse("11:30");

        if (currentTime.isBefore(cutoffTime)) {
            return true;
        } else {
            System.out.println("Time is Up");
            return false;
        }
    }


}
