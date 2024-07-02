package com.sparta.springmsamember.controller;


import com.sparta.springmsamember.dto.MemberDTO;
import com.sparta.springmsamember.dto.UpdatePasswordDTO;
import com.sparta.springmsamember.dto.UpdateProfileDTO;
import com.sparta.springmsamember.entity.MemberEntity;
import com.sparta.springmsamember.repository.JpaMemberRepository;
import com.sparta.springmsamember.service.MemberService;
import org.antlr.v4.runtime.misc.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public MemberController(MemberService memberService, PasswordEncoder passwordEncoder) {
        this.memberService = memberService;
        this.passwordEncoder = passwordEncoder;
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
}