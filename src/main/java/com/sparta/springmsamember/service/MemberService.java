package com.sparta.springmsamember.service;

import com.sparta.springmsamember.dto.*;
import com.sparta.springmsamember.entity.MemberEntity;
import com.sparta.springmsamember.repository.JpaMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class MemberService {

    private final JpaMemberRepository jpaMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final EmailService emailService;
    private static final Logger log = LoggerFactory.getLogger(MemberService.class);

    @Autowired
    public MemberService(JpaMemberRepository jpaMemberRepository, PasswordEncoder passwordEncoder, RedisTemplate<String, Object> redisTemplate, EmailService emailService) {
        this.jpaMemberRepository = jpaMemberRepository;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;
    }

    public Optional<MemberEntity> findByEmail(String email) {
        return jpaMemberRepository.findByEmail(email);
    }

    public MemberEntity registerUser(MemberDTO memberDTO) {
        if (memberDTO.getEmail() == null || memberDTO.getEmail().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty.");
        }

        if (memberDTO.getUserName() == null || memberDTO.getUserName().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty.");
        }

        if (memberDTO.getPassword() == null || memberDTO.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }

        if (jpaMemberRepository.findByEmail(memberDTO.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already in use. Please choose another one.");
        }

        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setEmail(memberDTO.getEmail());
        memberEntity.setUserName(passwordEncoder.encode(memberDTO.getUserName()));
        memberEntity.setPassword(passwordEncoder.encode(memberDTO.getPassword()));
        memberEntity.setRole("ROLE_USER");
        memberEntity.setEnabled(false); // 이메일 인증 전까지 계정 비활성화

        try {
            MemberEntity savedMember = jpaMemberRepository.save(memberEntity);
            sendVerificationEmail(savedMember);
            return savedMember;
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Error. Please try again.");
        }
    }

    private void sendVerificationEmail(MemberEntity member) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(token, member.getEmail(), 24, TimeUnit.HOURS);

        String url = "http://localhost:8080/member/verify?token=" + token;
        emailService.sendEmail(member.getEmail(), "Email Verification", "Click the link to verify your email: " + url);
    }

    public void verifyUser(String token) {
        String email = (String) redisTemplate.opsForValue().get(token);
        if (email == null) {
            throw new IllegalArgumentException("Invalid verification token");
        }

        MemberEntity member = jpaMemberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        member.setEnabled(true);
        jpaMemberRepository.save(member);

        redisTemplate.delete(token);
    }

    @Transactional
    public void updatePassword(String email, UpdatePasswordDTO updatePasswordDTO) {
        Optional<MemberEntity> optionalMember = jpaMemberRepository.findByEmail(email);
        if (optionalMember.isEmpty()) {
            throw new IllegalArgumentException("Member not found");
        }

        MemberEntity member = optionalMember.get();
        member.setPassword(passwordEncoder.encode(updatePasswordDTO.getNewPassword()));
        jpaMemberRepository.save(member);
    }

    public void updateProfile(String email, UpdateProfileDTO updateProfileDTO) {
        Optional<MemberEntity> optionalMember = jpaMemberRepository.findByEmail(email);
        if (optionalMember.isEmpty()) {
            throw new IllegalArgumentException("Member not found");
        }

        MemberEntity member = optionalMember.get();
        if (updateProfileDTO.getAddress() != null) {
            member.setAddress(updateProfileDTO.getAddress());
        }
        if (updateProfileDTO.getMobileNo() != null) {
            member.setPhone(updateProfileDTO.getMobileNo());
        }
        jpaMemberRepository.save(member);
    }

    // 비밀번호 재설정 요청
    public void requestPasswordReset(PasswordResetRequestDTO requestDTO) {
        MemberEntity member = jpaMemberRepository.findByEmail(requestDTO.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!member.getPhone().equals(requestDTO.getPhone()) || !passwordEncoder.matches(requestDTO.getUserName(), member.getMemberName())) {
            throw new IllegalArgumentException("User details do not match");
        }

        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(token, member.getEmail(), 1, TimeUnit.HOURS);

        String url = "http://localhost:8080/member/reset-password?token=" + token;
        emailService.sendEmail(member.getEmail(), "Password Reset", "Click the link to reset your password: " + url);
    }

    // 비밀번호 재설정
    public void resetPassword(String token, String newPassword) {
        String email = (String) redisTemplate.opsForValue().get(token);
        if (email == null) {
            throw new IllegalArgumentException("Invalid or expired password reset token");
        }

        MemberEntity member = jpaMemberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        member.setPassword(passwordEncoder.encode(newPassword));
        jpaMemberRepository.save(member);

        redisTemplate.delete(token);
    }
}