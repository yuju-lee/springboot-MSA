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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ProductService {

    private final ProductRepository productRepository;
    private final LikeRepository likeRepository;
    private final WishListRepository wishListRepository;
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public ProductService(ProductRepository productRepository, LikeRepository likeRepository, WishListRepository wishListRepository, ObjectMapper objectMapper, KafkaTemplate<String, String> kafkaTemplate) {
        this.productRepository = productRepository;
        this.likeRepository = likeRepository;
        this.wishListRepository = wishListRepository;
        this.objectMapper = objectMapper;

        this.kafkaTemplate = kafkaTemplate;
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
            ProductResponseDTO productResponse = new ProductResponseDTO(
                    product.getProductId(),
                    product.getProductName(),
                    product.getPrice(),
                    product.getQuantity()
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

}