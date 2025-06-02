package danix.app.announcements_service.services;

import danix.app.announcements_service.util.CurrencyCode;

import java.util.function.Function;

public interface CurrencyConverterService {

    double convertPrice(CurrencyCode currencyCode, Function<Double, Double> operation);

}
