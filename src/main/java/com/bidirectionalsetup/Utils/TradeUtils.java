package com.bidirectionalsetup.Utils;

import com.bidirectionalsetup.Models.CustomTrade;
import com.bidirectionalsetup.Models.Direction;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.kiteconnect.utils.Constants;
import com.zerodhatech.models.Order;
import com.zerodhatech.models.OrderParams;


import java.io.IOException;
import java.time.LocalTime;
import java.util.List;
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

    public Order getOrderInCurrentState(String orderId) throws IOException, KiteException {

        List<Order> orderHistory = kiteSdk.getOrderHistory(orderId);
        for (Order o : orderHistory) {
            if(o.status.equals("COMPLETE") && o.orderId.equals(orderId)){
                return o;
            }
        }
        /*ReVisit*/
    /*
        List<Trade> tradeHistory = kiteSdk.getOrderTrades(orderId);
        List<Order> completeOrderHistory = kiteSdk.getOrders();
        List<Trade> completeTradeHistory = kiteSdk.getOrderTrades(orderId);
        ObjectMapper mapper = new ObjectMapper();
        int index = 0;
        System.out.println("--------------------Order History--------------------------");
        String orderHistoryJson = mapper.writeValueAsString(orderHistory);
        System.out.println(orderHistoryJson);
        System.out.println("----------------------------------------------------------");

        System.out.println("--------------------Trade History--------------------------");
        String tradeHistoryJson = mapper.writeValueAsString(tradeHistory);
        System.out.println(tradeHistoryJson);
        System.out.println("----------------------------------------------------------");

        System.out.println("--------------------Complete Order History--------------------------");
        String completeOrderHistoryJson = mapper.writeValueAsString(completeOrderHistory);
        System.out.println(completeOrderHistoryJson);
        System.out.println("----------------------------------------------------------");

        System.out.println("--------------------Complete Trade History--------------------------");
        String completeTradeHistoryJson = mapper.writeValueAsString(completeTradeHistory);
        System.out.println(completeTradeHistoryJson);
        System.out.println("----------------------------------------------------------");
    */

        //If Order not successful return order in its current state.
        return orderHistory.get(orderHistory.size()-1);
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

            while(! getOrderInCurrentState(newOrder.orderId).status.equals("COMPLETE")) {
                Thread.sleep(2000);
                System.out.println("Waiting for order completion....");
            }

            Order completedOrder = getOrderInCurrentState(newOrder.orderId);
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
                    while(! getOrderInCurrentState(squareOffOrder.orderId).status.equals("COMPLETE")) {
                        Thread.sleep(1000);
                        System.out.println("Waiting for order completion....");
                    }
                    trade.isTradeTaken = false;
                    trade.lastExecutedOrder = squareOffOrder;
                    trade.tradeCount++;
                    trade.currentDirection = Direction.BEARISH;
                    TextUtils.printInRed("SL Hit @ "+ getOrderInCurrentState(squareOffOrder.orderId).triggerPrice);
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
                    while(! getOrderInCurrentState(squareOffOrder.orderId).status.equals("COMPLETE")) {
                        Thread.sleep(1000);
                        System.out.println("Waiting for order completion....");
                    }
                    trade.isTradeTaken = false;
                    trade.lastExecutedOrder = squareOffOrder;
                    trade.tradeCount++;
                    trade.currentDirection = Direction.BULLISH;
                    TextUtils.printInRed("SL Hit @ "+ getOrderInCurrentState(squareOffOrder.orderId).triggerPrice);
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
