package com.foodstore.auth;

import java.util.Optional;

/**
 * Phase 0 chỉ cần tra cứu theo username. Phase 1 (M5) bổ sung findAll, save, update,
 * setActive, countActiveAdmins cho StaffService.
 */
public interface StaffDao {

    Optional<StaffAccount> findByUsername(String username);
}
