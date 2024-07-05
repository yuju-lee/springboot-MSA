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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ProductService {

    private final ProductRepository productRepository;
    private final LikeRepository likeRepository;
    private final WishListRepository wishListRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private static final String REDIS_STOCK_KEY_PREFIX = "product_stock_";

    public ProductService(ProductRepository productRepository, LikeRepository likeRepository, WishListRepository wishListRepository, ObjectMapper objectMapper, KafkaTemplate<String, String> kafkaTemplate, RedisTemplate<String, String> redisTemplate) {
        this.productRepository = productRepository;
        this.likeRepository = likeRepository;
        this.wishListRepository = wishListRepository;
        this.objectMapper = objectMapper;

        this.kafkaTemplate = kafkaTemplate;
        this.redisTemplate = redisTemplate;
    }

    public List<ProductDTO> getAllProducts() {
        List<ProductEntity> products = productRepository.findAll();
        return products.stream()
                .map(product -> new ProductDTO(product.getProductName(), product.getPrice()))
                .collect(Collectors.toList());
    }

    public Optional<ProductEntity> getProductById(Integer productId) {
        return productRepository.findById(productId);
    }

    @Transactional
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

    public void deleteWishProduct(String email, Integer productId) {
        Optional<WishEntity> optionalWish = wishListRepository.findByProductIdAndEmail(productId, email);
        if (optionalWish.isPresent()) {
            WishEntity wishEntity = optionalWish.get();
            wishListRepository.delete(wishEntity);
        } else {
            throw new IllegalArgumentException("Wish product not found for the user");
        }
    }

    public List<WishListRequestDTO> getWishListByEmail(String email) {
        List<WishEntity> wishList = wishListRepository.findByEmail(email);
        return wishList.stream()
                .map(wish -> new WishListRequestDTO(wish.getProductName(), wish.getPrice(), wish.getQuantity()))
                .collect(Collectors.toList());
    }

    @KafkaListener(topics = "product-info-request-topic", groupId = "product")
    public void sendProductInfo(String productOrderJson) throws JsonProcessingException {
        ProductOrderDTO productOrder = objectMapper.readValue(productOrderJson, ProductOrderDTO.class);

        Optional<ProductEntity> productOpt = productRepository.findById(productOrder.getProductNo());
        if (productOpt.isPresent()) {
            ProductEntity product = productOpt.get();
            Integer stock = Integer.valueOf(Objects.requireNonNull(redisTemplate.opsForValue().get(REDIS_STOCK_KEY_PREFIX + product.getProductId())));

            ProductResponseDTO productResponse = new ProductResponseDTO(
                    product.getProductId(),
                    product.getProductName(),
                    product.getPrice(),
                    stock
            );
            String responseJson = objectMapper.writeValueAsString(productResponse);
            kafkaTemplate.send("product-info-response-topic", responseJson);
        }
    }

    @KafkaListener(topics = "deduct-stock-request-topic", groupId = "product")
    @Transactional
    public void deductStock(String productOrderJson) throws JsonProcessingException {
        ProductOrderDTO productOrder = objectMapper.readValue(productOrderJson, ProductOrderDTO.class);

        Optional<ProductEntity> productOpt = productRepository.findById(productOrder.getProductNo());
        if (productOpt.isPresent()) {
            ProductEntity product = productOpt.get();
            product.setQuantity(product.getQuantity() - productOrder.getQty());
            redisTemplate.opsForValue().decrement(REDIS_STOCK_KEY_PREFIX + product.getProductId(), productOrder.getQty());
            productRepository.save(product);
        }
    }

    @KafkaListener(topics = "restore-stock-request-topic", groupId = "product")
    @Transactional
    public void restoreStock(String productOrderJson) throws JsonProcessingException {
        ProductOrderDTO productOrder = objectMapper.readValue(productOrderJson, ProductOrderDTO.class);

        Optional<ProductEntity> productOpt = productRepository.findById(productOrder.getProductNo());
        if (productOpt.isPresent()) {
            ProductEntity product = productOpt.get();
            product.setQuantity(product.getQuantity() + productOrder.getQty());
            redisTemplate.opsForValue().increment(REDIS_STOCK_KEY_PREFIX + product.getProductId(), productOrder.getQty());
            productRepository.save(product);
        }
    }

    // 위시리스트 요청 수신
    @KafkaListener(topics = "wishlist-request-topic", groupId = "product")
    public void listenProductWishlist(String email) {
        log.info("Received wishlist request for email: {}", email);

        // 위시리스트를 가져옴
        List<WishEntity> wishList = wishListRepository.findByEmail(email);

        // DTO로 변환
        List<WishListRequestDTO> wishListDTOs = wishList.stream()
                .map(wish -> new WishListRequestDTO(wish.getProductId(), wish.getProductName(), wish.getPrice(), wish.getQuantity()))
                .collect(Collectors.toList());

        try {
            // DTO를 JSON 배열로 변환
            String wishListJson = objectMapper.writeValueAsString(wishListDTOs);

            // 멤버로 전송
            kafkaTemplate.send("wishlist-topic", wishListJson);
            log.info("Sent wishlist for email: {} to member service", email);
            log.info("Sent wishlist: {}", wishListJson);
        } catch (JsonProcessingException e) {
            log.error("Error serializing wishlist for email: {}", email, e);
        }
    }

    // 위시리스트 요청 보내기
    public void sendWishListRequest(String email) {
        kafkaTemplate.send("wishlist-request-topic", email);
    }

    @PostConstruct
    public void initializeStockInRedis() {
        List<ProductEntity> products = productRepository.findAll();
        for (ProductEntity product : products) {
            String redisKey = REDIS_STOCK_KEY_PREFIX + product.getProductId();
            String redisStock = redisTemplate.opsForValue().get(redisKey);
            if (redisStock == null) {
                redisTemplate.opsForValue().set(redisKey, String.valueOf(product.getQuantity()));
                log.info("Initialized Redis stock for product {} from MySQL: {}", product.getProductId(), product.getQuantity());
            }
        }
    }

    @Scheduled(fixedRate = 3600000) // 1시간마다 실행
    @Transactional
    public void syncStockWithRedis() {
        List<ProductEntity> products = productRepository.findAll();
        for (ProductEntity product : products) {
            String redisKey = REDIS_STOCK_KEY_PREFIX + product.getProductId();
            String redisStock = redisTemplate.opsForValue().get(redisKey);
            if (redisStock != null) {
                Integer redisStockValue = Integer.valueOf(redisStock);
                if (!product.getQuantity().equals(redisStockValue)) {
                    product.setQuantity(redisStockValue);
                    productRepository.save(product);
                    log.info("Synchronized product {} stock from Redis to MySQL: {}", product.getProductId(), redisStockValue);
                }
            } else {
                redisTemplate.opsForValue().set(redisKey, String.valueOf(product.getQuantity()));
                log.info("Initialized Redis stock for product {} from MySQL: {}", product.getProductId(), product.getQuantity());
            }
        }
    }
}