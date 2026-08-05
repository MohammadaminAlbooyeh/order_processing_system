package com.ecommerce.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {
    @Size(min = 2, max = 100)
    @Pattern(regexp = "^[a-zA-Z\\s'-]{2,100}$", message = "Full name can only contain letters, spaces, hyphens, and apostrophes")
    private String fullName;

    @Size(max = 20)
    @Pattern(regexp = "^\\+?1?\\d{9,15}$", message = "Phone number must be a valid format (9-15 digits, optional + prefix)")
    private String phone;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
