package com.dsanotebook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AppUserTest {
    @Test
    void usersCreatedBeforeRolesExistedAreEnabledUsers() {
        AppUser legacy = new AppUser();
        assertEquals("USER", legacy.effectiveRole());
        assertTrue(legacy.active());
    }

    @Test
    void disabledFlagAndAdminRoleAreRespected() {
        AppUser u = new AppUser();
        u.setRole("ADMIN");
        u.setEnabled(false);
        assertEquals("ADMIN", u.effectiveRole());
        assertFalse(u.active());
    }
}
