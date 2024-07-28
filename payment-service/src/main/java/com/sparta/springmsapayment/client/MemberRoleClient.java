package com.sparta.springmsapayment.client;

import com.sparta.springmsapayment.dto.MemberRoleResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "member-role-service", url = "${member-service.url}")
public interface MemberRoleClient {
    @GetMapping("/member/{email}")
    MemberRoleResponseDTO getMemberRole(@PathVariable String email);
}
