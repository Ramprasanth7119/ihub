package com.ihub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Self-service profile edit.
 *
 * <p>Only the display name is editable. Email is the login identity and role drives
 * authorization, so neither is accepted here — an admin changes roles through the
 * admin console instead.</p>
 */
public class ProfileUpdateRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
