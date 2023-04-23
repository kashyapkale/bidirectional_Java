package com.bidirectionalsetup.Models;

import com.zerodhatech.models.Order;

public class CustomTrade {
    public Boolean isTradeTaken;
    public Integer tradeCount;
    public String stockName;
    public Direction currentDirection;
    public double threshold;
    public Integer quantity;
    public Order lastExecutedOrder;
    public Boolean successfulTrade;

    public Double getTargetPrice(){
        if (currentDirection.equals(Direction.BULLISH)){
            return threshold + (0.025 * threshold);
        } else {
            return threshold - (0.025 * threshold);
        }
    }

    public Double getSLPrice(){
        if (currentDirection.equals(Direction.BULLISH)){
            return threshold - (0.0020 * threshold);
        } else {
            return threshold + (0.0020 * threshold);
        }
    }

}
