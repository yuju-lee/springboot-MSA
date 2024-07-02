package com.sparta.springmsamember.service;

import com.sparta.springmsamember.dto.MemberDTO;
import com.sparta.springmsamember.dto.UpdatePasswordDTO;
import com.sparta.springmsamember.dto.UpdateProfileDTO;
import com.sparta.springmsamember.entity.MemberEntity;
import com.sparta.springmsamember.repository.JpaMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class MemberService {

    private final JpaMemberRepository jpaMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger log = LoggerFactory.getLogger(MemberService.class);

    @Autowired
    public MemberService(JpaMemberRepository jpaMemberRepository, PasswordEncoder passwordEncoder) {
        this.jpaMemberRepository = jpaMemberRepository;
        this.passwordEncoder = passwordEncoder;
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

        try {
            return jpaMemberRepository.save(memberEntity);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Error. Please try again.");
        }
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


}