package com.utepetshop.service;

import com.cloudinary.Cloudinary;
import com.utepetshop.entity.*;
import com.utepetshop.repository.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CloudinaryImageService {
  private final ObjectProvider<Cloudinary> cloudinary;
  private final ProductRepository products;
  private final ProductImageRepository images;

  public CloudinaryImageService(
      ObjectProvider<Cloudinary> c, ProductRepository p, ProductImageRepository i) {
    cloudinary = c;
    products = p;
    images = i;
  }

  public ProductImage upload(Long productId, MultipartFile file) {
    try {
      Cloudinary c = cloudinary.getIfAvailable();
      if (c == null)
        throw new IllegalStateException(
            "Cloudinary chưa cấu hình CLOUDINARY_CLOUD_NAME/API_KEY/API_SECRET");
      Map r = c.uploader().upload(file.getBytes(), Map.of("folder", "utepet/products"));
      ProductImage image = new ProductImage();
      image.setProduct(
          products
              .findById(productId)
              .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm")));
      image.setImageUrl(String.valueOf(r.get("url")));
      image.setSecureUrl(String.valueOf(r.get("secure_url")));
      image.setPublicId(String.valueOf(r.get("public_id")));
      image.setPrimaryImage(images.findByProductId(productId).isEmpty());
      return images.save(image);
    } catch (Exception e) {
      throw new IllegalArgumentException("Tải ảnh thất bại: " + e.getMessage());
    }
  }
}
