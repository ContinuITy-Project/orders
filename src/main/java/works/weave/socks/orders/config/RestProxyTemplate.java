package works.weave.socks.orders.config;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import rocks.inspectit.agent.java.sdk.opentracing.TracerProvider;
import rocks.inspectit.agent.java.sdk.opentracing.internal.impl.TracerImpl;

@Component
public final class RestProxyTemplate {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired RestTemplate restTemplate;

    @Bean
    public RestTemplate restTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		List<ClientHttpRequestInterceptor> interceptors = new ArrayList<ClientHttpRequestInterceptor>();
		interceptors.add(new ClientHttpRequestInterceptor() {

			@Override
			public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
				// Add post body to span. (Dirty hack 2000)
				TracerImpl tracer = (TracerImpl) TracerProvider.get();
				tracer.getCurrentSpan().setTag("body", new String(body));
				return execution.execute(request, body);
			}
		});
		restTemplate.setInterceptors(interceptors);
		return restTemplate;
    }

    @Value("${proxy.host:}")
    private String host;

    @Value("${proxy.port:}")
    private String port;

    @PostConstruct
    public void init() {
        if (host.isEmpty() || port.isEmpty()) {
            return;
        }
        int portNr = -1;
        try {
            portNr = Integer.parseInt(port);
        } catch (NumberFormatException e) {
            logger.error("Unable to parse the proxy port number");
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        InetSocketAddress address = new InetSocketAddress(host, portNr);
        Proxy proxy = new Proxy(Proxy.Type.HTTP, address);
        factory.setProxy(proxy);

        restTemplate.setRequestFactory(factory);
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }
}
