package danix.app.announcements_service.feignClients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(url = "http://apilayer.net", name = "currency-converter")
public interface CurrencyConverterAPI {
    @GetMapping("/api/live")
    Map<String, Object> convertCurrency(@RequestParam("access_key") String key,
                                        @RequestParam("currencies") String currencies,
                                        @RequestParam("source") String source,
                                        @RequestParam(value = "format") int format);
}