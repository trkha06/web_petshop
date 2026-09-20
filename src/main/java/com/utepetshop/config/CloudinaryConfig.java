package com.utepetshop.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;

@Configuration
public class CloudinaryConfig {
  @Bean
  @ConditionalOnProperty(value = "app.cloudinary.cloud-name")
  Cloudinary cloudinary(
      @Value("${app.cloudinary.cloud-name}") String name,
      @Value("${app.cloudinary.api-key}") String key,
      @Value("${app.cloudinary.api-secret}") String secret) {
    return new Cloudinary(
        ObjectUtils.asMap(
            "cloud_name", name, "api_key", key, "api_secret", secret, "secure", true));
  }
}
