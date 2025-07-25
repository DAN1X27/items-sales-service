package danix.app.announcements_service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(url = "${currency_layer_url}", name = "currency-converter")
public interface CurrencyConverterAPI {

	@GetMapping("/api/live")
	Map<String, Object> getCourse(@RequestParam("access_key") String key,
			@RequestParam("currencies") String currencies, @RequestParam("source") String source,
			@RequestParam("format") int format);

}