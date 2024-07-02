package com.sparta.springmsamember.controller;

import com.sparta.springmsamember.dto.*;
import com.sparta.springmsamember.entity.MemberEntity;
import com.sparta.springmsamember.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;

    @Autowired
    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody MemberDTO memberDTO) {
        try {
            memberDTO.setRole("ROLE_USER");
            MemberEntity savedMemberEntity = memberService.registerUser(memberDTO);
            String welcomeMessage = "Welcome, " + savedMemberEntity.getEmail() + "!";
            return ResponseEntity.ok(welcomeMessage);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        try {
            memberService.completeSignup(token);
            return ResponseEntity.ok("Email verification successful. Your account is now activated.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body("Bad Request: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Internal Server Error: " + e.getMessage());
        }
    }

    @PutMapping("/update-password")
    public ResponseEntity<String> updatePassword(@RequestHeader("X-Authenticated-User") String email, @RequestBody UpdatePasswordDTO updatePasswordDTO) {
        try {
            memberService.updatePassword(email, updatePasswordDTO);
            return ResponseEntity.ok("Password updated successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body("Bad Request: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Internal Server Error: " + e.getMessage());
        }
    }

    @PutMapping("/update-profile")
    public ResponseEntity<String> updateProfile(@RequestHeader("X-Authenticated-User") String email, @RequestBody UpdateProfileDTO updateProfileDTO) {
        try {
            memberService.updateProfile(email, updateProfileDTO);
            return ResponseEntity.ok("Profile updated successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body("Bad Request: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Internal Server Error: " + e.getMessage());
        }
    }

    @PostMapping("/find-password")
    public ResponseEntity<String> requestPasswordReset(@RequestBody PasswordResetRequestDTO requestDTO) {
        try {
            memberService.requestPasswordReset(requestDTO);
            return ResponseEntity.ok("Password reset email sent.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body("Bad Request: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Internal Server Error: “+e.getMessage()");
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody PasswordResetDTO resetDTO) {
        try {
            memberService.resetPassword(resetDTO);
            return ResponseEntity.ok("Password reset successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body("Bad Request: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Internal Server Error: " + e.getMessage());
        }
    }
}