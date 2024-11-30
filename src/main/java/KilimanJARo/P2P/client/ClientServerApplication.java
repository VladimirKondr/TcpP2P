package KilimanJARo.P2P.client;

import KilimanJARo.P2P.server.requests.AuthRequest;
import KilimanJARo.P2P.server.requests.LogoutRequest;
import KilimanJARo.P2P.server.requests.RegisterRequest;
import KilimanJARo.P2P.server.responses.AuthResponse;
import KilimanJARo.P2P.server.responses.LogoutResponse;
import KilimanJARo.P2P.server.responses.RegisterResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

@SpringBootApplication
@EnableScheduling
public class ClientServerApplication {

    public static void main(String[] args) {
        if (args != null) {
            String[] customArgs = new String[]{"--spring.config.name=ClientServerApplication", "--spring.profiles.active=client_server"};
            String[] allArgs = new String[args.length + customArgs.length];
            System.arraycopy(args, 0, allArgs, 0, args.length);
            System.arraycopy(customArgs, 0, allArgs, args.length, customArgs.length);
            SpringApplication.run(ClientServerApplication.class, allArgs);
        } else {
            SpringApplication.run(ClientServerApplication.class, "--spring.config.name=ClientServerApplication", "--spring.profiles.active=client_server");
        }

    }

    @Bean(name="ClientServerRestTemplate")
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Value("${central_server_properties}")
    private String centralServerPropertiesPath;

    @Bean(name = "centralServerProperties")
    public PropertiesFactoryBean centralServerProperties() {
        PropertiesFactoryBean bean = new PropertiesFactoryBean();
        bean.setLocation(new ClassPathResource(centralServerPropertiesPath));
        return bean;
    }

    @Value("${spring.config.name}")
    private String serverPropertiesPath;


    @Bean(name = "serverProperties")
    public PropertiesFactoryBean serverProperties() {
        PropertiesFactoryBean bean = new PropertiesFactoryBean();
        bean.setLocation(new ClassPathResource(serverPropertiesPath));
        return bean;
    }
}
