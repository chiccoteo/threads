package uz.threads.auth

import org.apache.commons.logging.LogFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer
import org.springframework.security.oauth2.provider.*
import org.springframework.security.oauth2.provider.token.AbstractTokenGranter
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices
import org.springframework.util.StringUtils
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import org.springframework.web.servlet.i18n.SessionLocaleResolver
import org.springframework.web.servlet.support.RequestContextUtils
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Configuration
class WebMvcConfig : WebMvcConfigurer {
    @Bean
    fun localeResolver() = SessionLocaleResolver().apply { setDefaultLocale(Locale("ru")) }

    @Bean
    fun localeChangeInterceptor() = HeaderLocaleChangeInterceptor().apply { headerName = "hl" }

    @Bean
    fun errorMessageSource() = ResourceBundleMessageSource().apply {
        setDefaultEncoding(Charsets.UTF_8.name())
        setBasename("error")
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(localeChangeInterceptor())
    }
}

@Configuration
class WebSecurityConfig(val authenticationProvider: CustomAuthenticationProvider) : WebSecurityConfigurerAdapter() {

    override fun configure(auth: AuthenticationManagerBuilder) {
        auth.authenticationProvider(authenticationProvider)
    }

    @Bean
    override fun authenticationManagerBean(): AuthenticationManager = super.authenticationManagerBean()

    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder()

    override fun configure(http: HttpSecurity?) {
        http
            ?.sessionManagement()
            ?.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            ?.and()
            ?.csrf()?.disable()
    }
}

@EnableResourceServer
@Configuration
class ResourceServerConfig : ResourceServerConfigurerAdapter() {
    override fun configure(http: HttpSecurity?) {
        http
            ?.requestMatchers()
            ?.and()
            ?.authorizeRequests()
            ?.antMatchers(HttpMethod.DELETE, "/token")?.permitAll()
            ?.antMatchers("/**")?.authenticated()
    }
}

@EnableAuthorizationServer
@Configuration
class AuthorizationConfig(
    val authenticationManager: AuthenticationManager,
    val authClientDetailsService: AuthClientDetailsService,
    val passwordEncoder: BCryptPasswordEncoder,
    val userDetailsService: CustomUserDetailsService,
    val mongoTokenStore: MongoTokenStore
) : AuthorizationServerConfigurerAdapter() {

    override fun configure(clients: ClientDetailsServiceConfigurer) {
        clients.withClientDetails(authClientDetailsService)
    }

    override fun configure(endpoints: AuthorizationServerEndpointsConfigurer) {
        val compositeTokenGranter = CompositeTokenGranter(mutableListOf(endpoints.tokenGranter))

        endpoints
            .tokenStore(mongoTokenStore)
            .userDetailsService(userDetailsService)
            .authenticationManager(authenticationManager)
            .reuseRefreshTokens(false)
            .tokenGranter(compositeTokenGranter)

    }

    override fun configure(oauthServer: AuthorizationServerSecurityConfigurer) {
        oauthServer
            .tokenKeyAccess("permitAll()")
            .checkTokenAccess("isAuthenticated()")
            .passwordEncoder(passwordEncoder)
            .allowFormAuthenticationForClients()
    }
}

class HeaderLocaleChangeInterceptor : HandlerInterceptorAdapter() {

    private val logger = LogFactory.getLog(javaClass)
    private val DEFAULT_HEADER_NAME = "locale"
    var headerName = DEFAULT_HEADER_NAME
    private var httpMethods = mutableListOf<String>()
    private var ignoreInvalidLocale = false

    private fun parseLocaleValue(localeValue: String) = StringUtils.parseLocale(localeValue)

    private fun checkHttpMethod(currentMethod: String): Boolean {
        if (httpMethods.isEmpty())
            return true

        for (method in httpMethods) {
            if (method == currentMethod) {
                return true
            }
        }
        return false
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val newLocale = request.getHeader(headerName)
        if (newLocale != null && checkHttpMethod(request.method)) {
            val localeResolver = RequestContextUtils.getLocaleResolver(request)
                ?: throw IllegalStateException("No LocaleResolver found: not in a DispatcherServlet request?")
            try {
                localeResolver.setLocale(request, response, parseLocaleValue(newLocale))
            } catch (ex: IllegalArgumentException) {
                if (ignoreInvalidLocale) {
                    if (logger.isDebugEnabled)
                        logger.debug("Ignoring invalid locale value [" + newLocale + "]: " + ex.message)
                } else
                    throw ex
            }
        }
        return true
    }
}

class AuthTokenGranter(
    private val authenticationManager: AuthenticationManager,
    authorizationServerTokenServices: AuthorizationServerTokenServices,
    clientDetailsService: ClientDetailsService,
    requestFactory: OAuth2RequestFactory,
    grantType: String
) : AbstractTokenGranter(authorizationServerTokenServices, clientDetailsService, requestFactory, grantType) {

    override fun getOAuth2Authentication(client: ClientDetails?, tokenRequest: TokenRequest?): OAuth2Authentication {
        val value = tokenRequest?.requestParameters?.get(getGrantTypeKey(tokenRequest))
        val userAuthentication = UsernamePasswordAuthenticationToken(value, null)
        userAuthentication.details = tokenRequest?.requestParameters
        return OAuth2Authentication(
            tokenRequest?.createOAuth2Request(client!!),
            authenticationManager.authenticate(userAuthentication)
        )
    }

    private fun getGrantTypeKey(tokenRequest: TokenRequest?): String {
        if (tokenRequest == null) throw Exception("TokenRequest null")
        GrantType.values().forEach {
            if (it.value == tokenRequest.grantType) return it.value
        }
        return tokenRequest.grantType
    }
}