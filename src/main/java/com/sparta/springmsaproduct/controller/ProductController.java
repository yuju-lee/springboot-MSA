package com.sparta.springmsaproduct.controller;

import com.sparta.springmsaproduct.dto.ProductDTO;
import com.sparta.springmsaproduct.dto.ProductOrderDTO;
import com.sparta.springmsaproduct.dto.ProductResponseDTO;
import com.sparta.springmsaproduct.dto.WishListRequestDTO;
import com.sparta.springmsaproduct.entity.ProductEntity;
import com.sparta.springmsaproduct.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/detail/{productId}")
    public ResponseEntity<ProductResponseDTO> getProductById(@PathVariable Integer productId) {
        ProductResponseDTO product = productService.getProductById(productId);
        return ResponseEntity.ok(product);
    }

    @PostMapping("/like/{productId}")
    public ResponseEntity<String> toggleLikeProduct(@RequestHeader("X-Authenticated-User") String email, @PathVariable Integer productId) {
        return ResponseEntity.ok(productService.toggleLikeProduct(email, productId));
    }

    @GetMapping("/wishlist")
    public ResponseEntity<List<WishListRequestDTO>> getWishProducts(@RequestHeader("X-Authenticated-User") String email) {
        return ResponseEntity.ok(productService.getWishProducts(email));
    }

    @PostMapping("/wishlist")
    public ResponseEntity<String> addWishProduct(@RequestHeader("X-Authenticated-User") String email, @RequestBody WishListRequestDTO wishListRequestDTO) {
        String message = productService.addWishProduct(email, wishListRequestDTO);
        return ResponseEntity.ok(message);
    }

    @DeleteMapping("/wishlist/{productId}")
    public ResponseEntity<Void> deleteWishProduct(@RequestHeader("X-Authenticated-User") String email, @PathVariable Integer productId) {
        productService.deleteWishProduct(email, productId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/products/restore-stock")
    public void restoreStock(@RequestBody ProductOrderDTO productOrderDTO) {
        productService.restoreStock(productOrderDTO);
    }

}