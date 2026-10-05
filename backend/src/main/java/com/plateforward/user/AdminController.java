package com.plateforward.user;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/organizations")
@RequiredArgsConstructor
public class AdminController {
    private final UserRepository users;

    @GetMapping
    List<UserDto> organizations() {
        return users.findByRoleOrderByVerifiedAscIdDesc(User.Role.ORG).stream().map(UserDto::of).toList();
    }

    @PostMapping("/{id}/verify")
    @Transactional
    UserDto toggleVerified(@PathVariable Long id) {
        var org = users.findById(id).filter(u -> u.getRole() == User.Role.ORG)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        org.setVerified(!org.isVerified());
        return UserDto.of(org);
    }
}
