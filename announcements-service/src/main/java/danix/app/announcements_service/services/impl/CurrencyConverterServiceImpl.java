package danix.app.announcements_service.services.impl;

import danix.app.announcements_service.feign.CurrencyConverterAPI;
import danix.app.announcements_service.services.CurrencyConverterService;
import danix.app.announcements_service.util.CurrencyCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyConverterServiceImpl implements CurrencyConverterService {

    private final CurrencyConverterAPI converterAPI;

    @Value("${currency_layer_api_key}")
    private String apiKey;

    @Override
    public double convertPrice(CurrencyCode currencyCode, Function<Double, Double> operation) {
        if (CurrencyCode.USD != currencyCode) {
            Map<String, Object> response = null;
            try {
                response = converterAPI.getCourse(apiKey, currencyCode.toString(), "USD", 1);
                Map<String, Object> quotes = (Map<String, Object>) response.get("quotes");
                double course = (double) quotes.get("USD" + currencyCode);
                BigDecimal bigDecimal = BigDecimal.valueOf(operation.apply(course)).setScale(2, RoundingMode.HALF_UP);
                return bigDecimal.doubleValue();
            }
            catch (Exception e) {
                assert response != null;
                Map<String, Object> error = (Map<String, Object>) response.get("error");
                String message = (String) error.get("info");
                log.error("Error convert price: {}", message);
                throw e;
            }
        }
        return operation.apply(1.0);
    }

}
