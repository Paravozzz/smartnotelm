package com.smartnotelm.api.config;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

@Configuration
public class WebConfiguration {

  @Bean
  public WebMvcConfigurer webMvcConfigurer(AppProperties appProperties) {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry
            .addMapping("/api/**")
            .allowedOrigins(appProperties.cors().allowedOrigins())
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowCredentials(true);
      }

      @Override
      public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry
            .addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(true)
            .addResolver(
                new PathResourceResolver() {
                  @Override
                  protected Resource resolveResourceInternal(
                          HttpServletRequest request,
                          @NonNull String requestPath,
                          @NonNull List<? extends Resource> locations,
                          @NonNull ResourceResolverChain chain) {
                    Resource resource = chain.resolveResource(request, requestPath, locations);
                    if (resource != null && resource.exists()) {
                      return resource;
                    }
                    if (requestPath.startsWith("api/")) {
                      return null;
                    }
                    return chain.resolveResource(request, "index.html", locations);
                  }
                });
      }
    };
  }
}
