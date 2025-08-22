package com.NewCooks.NewCooks.Config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CloudinaryConfig {

    private final Cloudinary cloudinary;

    public CloudinaryConfig(
            @Value("${cloudinary_username}") String cloudName,
            @Value("${cloudinary_api_key}") String apiKey,
            @Value("${cloudinary_api_secret}") String apiSecret
    ) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));
    }

    public Cloudinary getCloudinary() {
        return cloudinary;
    }
}
