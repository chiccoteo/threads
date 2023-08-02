package uz.threads.gateway

import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoRestTemplateFactory
import org.springframework.cloud.client.loadbalancer.LoadBalanced
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter

@EnableResourceServer
@Configuration
class ResourceServerConfig : ResourceServerConfigurerAdapter() {

    override fun configure(http: HttpSecurity) {
        http.requestMatchers()
            .and()
            .authorizeRequests()
            .antMatchers("/api/v1/auth/oauth/**").permitAll()
            .antMatchers("/api/v1/file/download").permitAll()
            .antMatchers(HttpMethod.POST,"/api/v1/user").permitAll()
            .antMatchers("/api/v1/*/internal/**").denyAll()
            .antMatchers("/**").authenticated()
    }

    @Bean
    @LoadBalanced
    internal fun oautøh2RestTemplate(userInfoRestTemplateFactory: UserInfoRestTemplateFactory): OAuth2RestTemplate {
        return userInfoRestTemplateFactory.userInfoRestTemplate
    }
}