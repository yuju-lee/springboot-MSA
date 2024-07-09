package com.sparta.springmsaproduct.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.springmsaproduct.dto.*;
import com.sparta.springmsaproduct.entity.LikeEntity;
import com.sparta.springmsaproduct.entity.ProductEntity;
import com.sparta.springmsaproduct.entity.WishEntity;
import com.sparta.springmsaproduct.repository.LikeRepository;
import com.sparta.springmsaproduct.repository.ProductRepository;
import com.sparta.springmsaproduct.repository.WishListRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ProductService {

    private final ProductRepository productRepository;
    private final LikeRepository likeRepository;
    private final WishListRepository wishListRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private static final String REDIS_STOCK_KEY_PREFIX = "product_stock_";

    private final ObjectMapper objectMapper;

    public ProductService(ProductRepository productRepository, LikeRepository likeRepository,
                          WishListRepository wishListRepository, RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.likeRepository = likeRepository;
        this.wishListRepository = wishListRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public List<ProductDTO> getAllProducts() {
        List<ProductEntity> products = productRepository.findAll();
        return products.stream()
                .map(product -> new ProductDTO(product.getProductName(), product.getPrice()))
                .collect(Collectors.toList());
    }


    public ProductResponseDTO getProductById(Integer productId) {
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        ProductResponseDTO responseDTO = new ProductResponseDTO();
        responseDTO.setProductNo(product.getProductId());
        responseDTO.setProductName(product.getProductName());
        responseDTO.setPrice(product.getPrice());
        responseDTO.setQuantity(product.getQuantity());

        return responseDTO;
    }

    public String toggleLikeProduct(String email, Integer productId) {
        Optional<ProductEntity> optionalProduct = productRepository.findById(productId);
        if (optionalProduct.isEmpty()) {
            throw new IllegalArgumentException("Product not found");
        }

        ProductEntity product = optionalProduct.get();
        Optional<LikeEntity> optionalLike = likeRepository.findByProductIdAndEmail(productId, email);

        if (optionalLike.isPresent()) {
            likeRepository.delete(optionalLike.get());
            product.setLikeCount(product.getLikeCount() - 1);
            productRepository.save(product);
            return "Unlike...";
        } else {
            LikeEntity like = new LikeEntity();
            like.setProductId(productId);
            like.setEmail(email);
            likeRepository.save(like);

            product.setLikeCount(product.getLikeCount() + 1);
            productRepository.save(product);
            return "Like!";
        }
    }

    public List<WishListRequestDTO> getWishProducts(String email) {
        List<WishEntity> wishes = wishListRepository.findByEmail(email);
        return wishes.stream()
                .map(wish -> new WishListRequestDTO(wish.getProductId(), wish.getProductName(), wish.getPrice(), wish.getQuantity()))
                .collect(Collectors.toList());
    }

    @Transactional
    public String addWishProduct(String email, WishListRequestDTO wishListRequestDTO) {
        Optional<ProductEntity> optionalProduct = productRepository.findById(wishListRequestDTO.getProductId());
        if (optionalProduct.isPresent()) {
            ProductEntity product = optionalProduct.get();

            // 위시리스트에 제품이 이미 있는지 확인
            Optional<WishEntity> optionalWish = wishListRepository.findByProductIdAndEmail(product.getProductId(), email);
            WishEntity wish;
            if (optionalWish.isPresent()) {
                // 제품이 이미 있는 경우 수량을 추가
                wish = optionalWish.get();
                wish.setQuantity(wish.getQuantity() + wishListRequestDTO.getQuantity());
                wish.setPrice(product.getPrice() * wish.getQuantity());
            } else {
                // 제품이 없는 경우 새로 추가
                wish = new WishEntity();
                wish.setProductId(product.getProductId());
                wish.setProductName(product.getProductName());
                wish.setPrice(product.getPrice() * wishListRequestDTO.getQuantity());
                wish.setQuantity(wishListRequestDTO.getQuantity());
                wish.setEmail(email);
            }
            wishListRepository.save(wish);
            return "Product added to wishlist successfully.";
        } else {
            throw new IllegalArgumentException("Product not found");
        }
    }

    @Transactional
    public void deleteWishProduct(String email, Integer productId) {
        Optional<WishEntity> optionalWish = wishListRepository.findByProductIdAndEmail(productId, email);
        if (optionalWish.isPresent()) {
            WishEntity wishEntity = optionalWish.get();
            wishListRepository.delete(wishEntity);
        } else {
            throw new IllegalArgumentException("Wish product not found for the user");
        }
    }

    @Transactional
    public void restoreStock(ProductOrderDTO productOrderDTO) {
        Optional<ProductEntity> productOpt = productRepository.findById(productOrderDTO.getProductNo());

        if (productOpt.isPresent()) {
            ProductEntity product = productOpt.get();

            product.setQuantity(product.getQuantity() + productOrderDTO.getQty());
            productRepository.save(product);

            redisTemplate.opsForValue().increment(REDIS_STOCK_KEY_PREFIX + product.getProductId(), productOrderDTO.getQty());

            log.info("Restored stock for product {} in SQL and Redis by {}", product.getProductId(), productOrderDTO.getQty());
        } else {
            log.warn("Product not found for ID: {}", productOrderDTO.getProductNo());
        }
    }

    @KafkaListener(topics = "stock-update-topic", groupId = "stock-group")
    public void handleStockUpdate(String stockUpdateDataJson) {
        try {
            Map stockUpdateData = objectMapper.readValue(stockUpdateDataJson, Map.class);
            int productId = Integer.parseInt((String) stockUpdateData.get("productId"));
            int stock = Integer.parseInt((String) stockUpdateData.get("stock"));

            updateProductStock(productId, stock);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateProductStock(int productId, int stock) {
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        product.setQuantity(stock);
        productRepository.save(product);
    }

}