package com.trade.algotrade.enums;

public enum CriteriaFilter {
    eq("eq"),gte("gte"),lte("lte");

    private final String operator;

    CriteriaFilter(String operator){
        this.operator = operator;
    }

    @Override
    public String toString() {
        return this.operator;
    }

    public static CriteriaFilter fromString(String operator) {
        for (CriteriaFilter op : CriteriaFilter.values()) {
            if (op.operator.equalsIgnoreCase(operator)) {
                return op;
            }
        }
        return null;
    }
}
