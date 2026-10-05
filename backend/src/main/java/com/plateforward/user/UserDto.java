package com.plateforward.user;

public record UserDto(Long id, String email, String name, User.Role role, String phone, String area, boolean verified) {
    public static UserDto of(User u) {
        return new UserDto(u.getId(), u.getEmail(), u.getName(), u.getRole(), u.getPhone(), u.getArea(), u.isVerified());
    }
}
