package ca.en.solution.currency;

import ca.en.solution.common.BadRequestException;

enum DisplayCurrency {
    CAD, USD;

    static DisplayCurrency parse(String value) {
        if (value == null) {
            return CAD;
        }
        for (DisplayCurrency currency : values()) {
            if (currency.name().equals(value)) {
                return currency;
            }
        }
        throw new BadRequestException("currency must be one of: CAD, USD.");
    }
}
